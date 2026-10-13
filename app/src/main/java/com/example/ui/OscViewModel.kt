package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.OscRepository
import com.example.model.ConnectionState
import com.example.model.ExecutorAction
import com.example.model.ExecutorButton
import com.example.model.FaderItem
import com.example.model.LayoutConfiguration
import com.example.model.OscConfig
import com.example.model.OscLogItem
import com.example.osc.OscConnectionService
import com.example.osc.OscMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class DestructiveActionRequest(
    val title: String,
    val message: String,
    val confirmButtonText: String = "Proceed",
    val onConfirm: () -> Unit
)

class OscViewModel(application: Application) : AndroidViewModel(application) {

    val repository = OscRepository(application)
    val oscService = OscConnectionService(viewModelScope)

    val connectionState: StateFlow<ConnectionState> = oscService.connectionState
    val logs: StateFlow<List<OscLogItem>> = oscService.logs

    private val _config = MutableStateFlow(repository.loadConfig())
    val config: StateFlow<OscConfig> = _config.asStateFlow()

    private val _layout = MutableStateFlow(repository.loadLayout())
    val layout: StateFlow<LayoutConfiguration> = _layout.asStateFlow()

    private val _blackoutActive = MutableStateFlow(repository.getBlackoutState())
    val blackoutActive: StateFlow<Boolean> = _blackoutActive.asStateFlow()

    private val _currentPage = MutableStateFlow(repository.getCurrentPage())
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _selectedExec = MutableStateFlow(repository.getSelectedExec())
    val selectedExec: StateFlow<String> = _selectedExec.asStateFlow()

    private val _commandLineText = MutableStateFlow("")
    val commandLineText: StateFlow<String> = _commandLineText.asStateFlow()

    private val _commandHistory = MutableStateFlow<List<String>>(listOf(
        "Group 1 @ 100",
        "Fixture 1 Thru 12 At 80",
        "Preset 4.1",
        "Highlight",
        "ClearAll"
    ))
    val commandHistory: StateFlow<List<String>> = _commandHistory.asStateFlow()

    private val _pendingDestructiveAction = MutableStateFlow<DestructiveActionRequest?>(null)
    val pendingDestructiveAction: StateFlow<DestructiveActionRequest?> = _pendingDestructiveAction.asStateFlow()

    private val _activeExecutorsMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val activeExecutorsMap: StateFlow<Map<String, Boolean>> = _activeExecutorsMap.asStateFlow()

    init {
        // Auto-connect on launch if enabled
        if (_config.value.autoReconnect) {
            oscService.connect(_config.value)
        }

        // Listen for incoming OSC feedback from grandMA2 console
        viewModelScope.launch {
            oscService.incomingOscMessages.collectLatest { msg ->
                handleIncomingFeedback(msg)
            }
        }
    }

    private fun handleIncomingFeedback(msg: OscMessage) {
        val addr = msg.address
        // Handle executor state feedback, e.g. /gma2/exec/1/201 or /gma2/feedback
        if (addr.contains("/exec/")) {
            val parts = addr.split("/")
            if (parts.size >= 5) {
                val page = parts[3]
                val exec = parts[4]
                val key = "$page.$exec"
                val isActive = when (val first = msg.arguments.firstOrNull()) {
                    is Float -> first > 0.01f
                    is Int -> first > 0
                    is Boolean -> first
                    is String -> first.equals("on", true) || first.equals("active", true) || first.equals("1", true)
                    else -> false
                }
                _activeExecutorsMap.value = _activeExecutorsMap.value + (key to isActive)
            }
        } else if (addr.contains("/fader/")) {
            // Echo back fader position from console
            val firstArg = msg.arguments.firstOrNull()
            if (firstArg is Float) {
                // Update corresponding fader in layout
                updateFaderLevelFromOsc(addr, firstArg)
            }
        }
    }

    private fun updateFaderLevelFromOsc(address: String, level: Float) {
        val currentFaders = _layout.value.faders.map { fader ->
            val matchKey = "${fader.page}/${fader.execNumber}"
            if (address.endsWith(matchKey) || (fader.isMaster && address.contains("master"))) {
                fader.copy(level = level.coerceIn(0f, 1f))
            } else {
                fader
            }
        }
        _layout.value = _layout.value.copy(faders = currentFaders)
    }

    // --- Connection Controls ---
    fun connect() {
        oscService.connect(_config.value)
    }

    fun disconnect() {
        oscService.disconnect()
    }

    fun updateConfig(newConfig: OscConfig) {
        _config.value = newConfig
        repository.saveConfig(newConfig)
        oscService.updateConfig(newConfig)
    }

    // --- Blackout & Safety ---
    fun toggleBlackout() {
        val newState = !_blackoutActive.value
        _blackoutActive.value = newState
        repository.setBlackoutState(newState)
        if (newState) {
            // Activate blackout
            oscService.sendCommand("Blackout On")
        } else {
            // Release blackout
            oscService.sendCommand("Blackout Off")
        }
    }

    fun requestClearAll() {
        _pendingDestructiveAction.value = DestructiveActionRequest(
            title = "Confirm Clear All",
            message = "This will clear all programmer values and active fixture selections across all universes. Proceed?",
            confirmButtonText = "Clear All"
        ) {
            oscService.sendCommand("ClearAll")
        }
    }

    fun requestStopAll() {
        _pendingDestructiveAction.value = DestructiveActionRequest(
            title = "Confirm Off All / Stop Playback",
            message = "This will send an 'Off Exec Thru' command, turning off all currently running sequences and chasers. Continue?",
            confirmButtonText = "Stop All"
        ) {
            oscService.sendCommand("Off Exec Thru")
        }
    }

    fun requestDeleteSelected() {
        _pendingDestructiveAction.value = DestructiveActionRequest(
            title = "Confirm Delete Executor",
            message = "Are you sure you want to delete Executor ${_selectedExec.value} from the console?",
            confirmButtonText = "Delete"
        ) {
            oscService.sendCommand("Delete Exec ${_selectedExec.value} /nc")
        }
    }

    fun dismissDestructiveAction() {
        _pendingDestructiveAction.value = null
    }

    // --- Page Management ---
    fun setPage(page: Int) {
        val newPage = page.coerceAtLeast(1)
        _currentPage.value = newPage
        repository.setCurrentPage(newPage)
        oscService.sendCommand("Page $newPage")
    }

    fun nextPage() = setPage(_currentPage.value + 1)
    fun prevPage() = setPage((_currentPage.value - 1).coerceAtLeast(1))

    // --- Executor Grid Triggers ---
    fun triggerExecutorButton(button: ExecutorButton, isDown: Boolean = true) {
        val page = if (button.page > 0) button.page else _currentPage.value
        val execKey = "$page.${button.execNumber}"

        when (button.action) {
            ExecutorAction.GO -> {
                if (isDown) {
                    oscService.sendCommand("Go Exec $execKey")
                    toggleSimulatedExec(execKey, true)
                }
            }
            ExecutorAction.FLASH -> {
                if (isDown) {
                    oscService.sendCommand("Flash On Exec $execKey")
                    toggleSimulatedExec(execKey, true)
                } else {
                    oscService.sendCommand("Flash Off Exec $execKey")
                    toggleSimulatedExec(execKey, false)
                }
            }
            ExecutorAction.TEMP -> {
                if (isDown) {
                    oscService.sendCommand("Temp On Exec $execKey")
                    toggleSimulatedExec(execKey, true)
                } else {
                    oscService.sendCommand("Temp Off Exec $execKey")
                    toggleSimulatedExec(execKey, false)
                }
            }
            ExecutorAction.TOGGLE -> {
                if (isDown) {
                    val current = _activeExecutorsMap.value[execKey] ?: false
                    val next = !current
                    if (next) {
                        oscService.sendCommand("Go Exec $execKey")
                    } else {
                        oscService.sendCommand("Off Exec $execKey")
                    }
                    toggleSimulatedExec(execKey, next)
                }
            }
            ExecutorAction.OFF -> {
                if (isDown) {
                    oscService.sendCommand("Off Exec $execKey")
                    toggleSimulatedExec(execKey, false)
                }
            }
        }
    }

    fun togglePlaybackState(button: ExecutorButton) {
        val page = if (button.page > 0) button.page else _currentPage.value
        val execKey = "$page.${button.execNumber}"
        val isCurrentlyActive = _activeExecutorsMap.value[execKey] ?: false
        val nextState = !isCurrentlyActive

        if (nextState) {
            oscService.sendCommand("Go Exec $execKey")
        } else {
            oscService.sendCommand("Off Exec $execKey")
        }
        toggleSimulatedExec(execKey, nextState)
    }

    private fun toggleSimulatedExec(key: String, active: Boolean) {
        _activeExecutorsMap.value = _activeExecutorsMap.value + (key to active)
    }

    // --- Fader Panel Controls ---
    fun setFaderLevel(fader: FaderItem, newLevel: Float) {
        val clamped = newLevel.coerceIn(0f, 1f)
        val percent = (clamped * 100f).roundToInt()

        // Update UI state immediately for fluid dragging
        val updatedFaders = _layout.value.faders.map {
            if (it.id == fader.id) it.copy(level = clamped) else it
        }
        _layout.value = _layout.value.copy(faders = updatedFaders)

        // Transmit OSC with rate limiting (~30 updates/sec)
        val page = if (fader.page > 0) fader.page else _currentPage.value
        if (fader.isMaster) {
            val cmd = "SpecialMaster 2.1 At $percent"
            oscService.sendMessage(
                OscMessage(_config.value.cmdAddress, cmd),
                rateLimitKey = "master_fader"
            )
        } else {
            val cmd = "Fader $page.${fader.execNumber} At $percent"
            oscService.sendMessage(
                OscMessage(_config.value.cmdAddress, cmd),
                rateLimitKey = "fader_${page}_${fader.execNumber}"
            )
        }
    }

    // --- Selected Playback Controls ---
    fun setSelectedExec(execStr: String) {
        _selectedExec.value = execStr
        repository.setSelectedExec(execStr)
    }

    fun playbackGo() {
        oscService.sendCommand("Go Exec ${_selectedExec.value}")
    }

    fun playbackBack() {
        oscService.sendCommand("GoBack Exec ${_selectedExec.value}")
    }

    fun playbackPause() {
        oscService.sendCommand("Pause Exec ${_selectedExec.value}")
    }

    fun playbackFlash(isDown: Boolean) {
        if (isDown) {
            oscService.sendCommand("Flash On Exec ${_selectedExec.value}")
        } else {
            oscService.sendCommand("Flash Off Exec ${_selectedExec.value}")
        }
    }

    fun playbackOff() {
        oscService.sendCommand("Off Exec ${_selectedExec.value}")
    }

    fun playbackTop() {
        oscService.sendCommand("Goto Cue Top Exec ${_selectedExec.value}")
    }

    fun playbackRate(multiplier: String) {
        oscService.sendCommand("Rate $multiplier Exec ${_selectedExec.value}")
    }

    // --- Command Line ---
    fun updateCommandLineText(text: String) {
        _commandLineText.value = text
    }

    fun appendToCommandLine(token: String) {
        val current = _commandLineText.value
        val syntaxToken = if (token == "@" && _config.value.atSymbol == "At") "At " else if (token == "@") "@ " else "$token "
        _commandLineText.value = if (current.isEmpty() || current.endsWith(" ")) {
            current + syntaxToken
        } else {
            "$current $syntaxToken"
        }
    }

    fun clearCommandLine() {
        _commandLineText.value = ""
    }

    fun submitCommandLine() {
        val cmd = _commandLineText.value.trim()
        if (cmd.isNotEmpty()) {
            oscService.sendCommand(cmd)
            val currentHistory = _commandHistory.value.toMutableList()
            currentHistory.remove(cmd)
            currentHistory.add(0, cmd)
            if (currentHistory.size > 25) {
                currentHistory.removeAt(currentHistory.size - 1)
            }
            _commandHistory.value = currentHistory
            _commandLineText.value = ""
        }
    }

    // --- Layout Customization ---
    fun updateLayout(newLayout: LayoutConfiguration) {
        _layout.value = newLayout
        repository.saveLayout(newLayout)
    }

    fun addExecutorButton(button: ExecutorButton) {
        val list = _layout.value.executorButtons.toMutableList()
        list.add(button)
        updateLayout(_layout.value.copy(executorButtons = list))
    }

    fun updateExecutorButton(button: ExecutorButton) {
        val list = _layout.value.executorButtons.map {
            if (it.id == button.id) button else it
        }
        updateLayout(_layout.value.copy(executorButtons = list))
    }

    fun removeExecutorButton(buttonId: String) {
        val list = _layout.value.executorButtons.filterNot { it.id == buttonId }
        updateLayout(_layout.value.copy(executorButtons = list))
    }

    fun addFader(fader: FaderItem) {
        val list = _layout.value.faders.toMutableList()
        list.add(fader)
        updateLayout(_layout.value.copy(faders = list))
    }

    fun updateFader(fader: FaderItem) {
        val list = _layout.value.faders.map {
            if (it.id == fader.id) fader else it
        }
        updateLayout(_layout.value.copy(faders = list))
    }

    fun removeFader(faderId: String) {
        val list = _layout.value.faders.filterNot { it.id == faderId }
        updateLayout(_layout.value.copy(faders = list))
    }

    // --- MA2 Macro Triggers & Management ---
    private val macroTimeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)

    fun triggerMacro(macro: com.example.model.MacroButton) {
        val now = macroTimeFormat.format(java.util.Date())
        val updatedMacros = _layout.value.macros.map {
            if (it.id == macro.id) it.copy(isExecuting = true, lastExecutedTime = now) else it
        }
        _layout.value = _layout.value.copy(macros = updatedMacros)

        val cmd = macro.command.ifBlank { "Macro ${macro.macroNumber}" }
        oscService.sendCommand(cmd)

        viewModelScope.launch {
            kotlinx.coroutines.delay(macro.executionDurationMs)
            val resetMacros = _layout.value.macros.map {
                if (it.id == macro.id) it.copy(isExecuting = false) else it
            }
            _layout.value = _layout.value.copy(macros = resetMacros)
        }
    }

    fun addMacro(macro: com.example.model.MacroButton) {
        val list = _layout.value.macros.toMutableList()
        list.add(macro)
        updateLayout(_layout.value.copy(macros = list))
    }

    fun updateMacro(macro: com.example.model.MacroButton) {
        val list = _layout.value.macros.map {
            if (it.id == macro.id) macro else it
        }
        updateLayout(_layout.value.copy(macros = list))
    }

    fun removeMacro(macroId: String) {
        val list = _layout.value.macros.filterNot { it.id == macroId }
        updateLayout(_layout.value.copy(macros = list))
    }

    fun loadPreset(name: String) {
        val preset = if (name.contains("Theater", ignoreCase = true)) {
            repository.getDefaultTheaterLayout()
        } else {
            repository.getDefaultConcertLayout()
        }
        updateLayout(preset)
    }

    fun importLayoutJson(jsonStr: String): Boolean {
        val parsed = repository.parseLayoutFromJson(jsonStr)
        return if (parsed != null) {
            updateLayout(parsed)
            true
        } else {
            false
        }
    }

    fun exportLayoutJson(): String {
        return repository.serializeLayoutToJson(_layout.value)
    }

    fun clearLogs() {
        oscService.clearLogs()
    }
}
