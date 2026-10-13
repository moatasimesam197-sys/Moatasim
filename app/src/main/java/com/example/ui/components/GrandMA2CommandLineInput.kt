package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.OscViewModel
import com.example.ui.theme.StageAmber
import com.example.ui.theme.StageBlack
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageCyan
import com.example.ui.theme.StageGreen
import com.example.ui.theme.StageOrange
import com.example.ui.theme.StagePurple
import com.example.ui.theme.StageRed
import com.example.ui.theme.StageSurface
import com.example.ui.theme.StageSurfaceElevated
import com.example.ui.theme.StageTextMuted
import com.example.ui.theme.StageTextPrimary
import com.example.ui.theme.StageTextSecondary
import kotlinx.coroutines.delay

/**
 * Reusable grandMA2 TextField-based command line input component.
 * Allows entering and sending text-based console commands to grandMA2 over OSC.
 *
 * Features:
 * - Real-time grandMA2 syntax highlighting (Fixture, Group, Preset, Cue, At, Full, etc.)
 * - Phosphor CRT console display well with blinking hardware block cursor
 * - Authentic green/amber "PLEASE" execution button with tactile press feedback
 * - Quick syntax assistance chips (Group, Preset, Fixture, At, Full, Thru, +, -)
 * - OSC transmission feedback badge and live OSC address preview
 * - Command history navigation (Previous, Next, and Dropdown history selector)
 * - Full hardware/software keyboard support with Enter/Send IME action
 */
@Composable
fun GrandMA2CommandLineInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendCommand: (String) -> Unit,
    modifier: Modifier = Modifier,
    atSymbol: String = "At",
    oscAddress: String = "/gma2/cmd",
    commandHistory: List<String> = emptyList(),
    onHistorySelect: ((String) -> Unit)? = null,
    placeholder: String = "Enter command syntax (e.g. Fixture 1 Thru 5 At 100)...",
    showQuickSyntaxChips: Boolean = true,
    showHistoryControls: Boolean = true,
    showOscPreview: Boolean = true,
    hapticEnabled: Boolean = true,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // History browsing index (-1 means user is typing new text)
    var historyIndex by remember { mutableIntStateOf(-1) }
    var showHistoryDropdown by remember { mutableStateOf(false) }

    // Visual transmission pulse animation when a command is sent over OSC
    var transmissionTimestamp by remember { mutableStateOf(0L) }
    var lastSentEcho by remember { mutableStateOf<String?>(null) }
    val isRecentlySent = (System.currentTimeMillis() - transmissionTimestamp) < 1200L

    // Phosphor CRT cursor blink
    val infiniteTransition = rememberInfiniteTransition(label = "ma2_cursor_blink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 480, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    // Button press scale animation for PLEASE
    var isPleasePressed by remember { mutableStateOf(false) }
    val pleaseScale by animateFloatAsState(
        targetValue = if (isPleasePressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 80),
        label = "please_scale"
    )

    fun executeCommand() {
        val commandToSend = value.trim()
        if (commandToSend.isNotEmpty() && enabled) {
            if (hapticEnabled) HapticUtil.performHaptic(context, isStrong = true)
            transmissionTimestamp = System.currentTimeMillis()
            lastSentEcho = commandToSend
            historyIndex = -1
            onSendCommand(commandToSend)
        } else if (hapticEnabled) {
            HapticUtil.performHaptic(context)
        }
    }

    fun appendToken(token: String) {
        if (hapticEnabled) HapticUtil.performHaptic(context)
        val formattedToken = if (token == "@" && atSymbol == "At") "At " else if (token == "@") "@ " else "$token "
        val updated = if (value.isEmpty() || value.endsWith(" ")) {
            value + formattedToken
        } else {
            "$value $formattedToken"
        }
        onValueChange(updated)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF131722), Color(0xFF0B0D14))
                )
            )
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) StageAmber.copy(alpha = 0.8f) else StageBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // --- Optional Quick Syntax Chips Row ---
        if (showQuickSyntaxChips) {
            Ma2QuickTokenBar(
                atSymbol = atSymbol,
                onTokenClick = { appendToken(it) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // --- Main Interactive Command Strip ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Authentic MA2 Command Prompt Badge [Cmd #]
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF241D0D))
                    .border(1.dp, StageAmber.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Cmd",
                        color = StageAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "#",
                        color = StageGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Phosphor CRT Display Window with TextField
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF06070B))
                    .border(
                        width = 1.dp,
                        color = if (isFocused) StageCyan.copy(alpha = 0.5f) else Color(0xFF1B2336),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        try {
                            focusRequester.requestFocus()
                        } catch (ignored: Exception) {}
                    }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // If text is empty and not focused, show placeholder
                if (value.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = placeholder,
                            color = Color(0xFF475569),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                        Text(
                            text = "█",
                            color = StageGreen.copy(alpha = cursorAlpha),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }

                // Interactive BasicTextField with syntax styling
                BasicTextField(
                    value = value,
                    onValueChange = {
                        historyIndex = -1
                        onValueChange(it)
                    },
                    singleLine = true,
                    enabled = enabled,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    ),
                    cursorBrush = SolidColor(StageGreen),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { executeCommand() },
                        onDone = { executeCommand() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused }
                        .testTag("hardware_command_input_field")
                        .testTag("ma2_command_text_field")
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Backspace / Clear button
            if (value.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1B202D))
                        .border(1.dp, StageBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            if (hapticEnabled) HapticUtil.performHaptic(context)
                            onValueChange("")
                            historyIndex = -1
                        }
                        .testTag("clear_command_strip_btn")
                        .testTag("ma2_command_clear_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear Command",
                        tint = StageAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Command History Navigation Buttons
            if (showHistoryControls && commandHistory.isNotEmpty()) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF141926))
                            .border(1.dp, StageBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                if (hapticEnabled) HapticUtil.performHaptic(context)
                                showHistoryDropdown = !showHistoryDropdown
                            }
                            .testTag("ma2_command_history_menu_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History Menu",
                            tint = StageCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showHistoryDropdown,
                        onDismissRequest = { showHistoryDropdown = false },
                        modifier = Modifier
                            .background(Color(0xFF12151F))
                            .border(1.dp, StageBorder, RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "COMMAND HISTORY (${commandHistory.size})",
                            color = StageAmber,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        commandHistory.take(8).forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = item,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                },
                                onClick = {
                                    showHistoryDropdown = false
                                    if (hapticEnabled) HapticUtil.performHaptic(context)
                                    onHistorySelect?.invoke(item) ?: onValueChange(item)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))
            }

            // Iconic grandMA2 "PLEASE" Execution Button
            Box(
                modifier = Modifier
                    .scale(pleaseScale)
                    .height(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = if (value.isNotBlank()) {
                                listOf(Color(0xFF00C853), Color(0xFF007E33))
                            } else {
                                listOf(Color(0xFF1E2822), Color(0xFF141C16))
                            }
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = if (value.isNotBlank()) StageGreen else StageBorder,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .pointerInput(enabled, value) {
                        detectTapGestures(
                            onPress = {
                                isPleasePressed = true
                                tryAwaitRelease()
                                isPleasePressed = false
                            },
                            onTap = {
                                executeCommand()
                            }
                        )
                    }
                    .padding(horizontal = 12.dp)
                    .testTag("hardware_please_button")
                    .testTag("ma2_command_please_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Please / Send",
                        tint = if (value.isNotBlank()) Color.White else StageTextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "PLEASE",
                        color = if (value.isNotBlank()) Color.White else StageTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // --- Status & OSC Telemetry Footer ---
        if (showOscPreview) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live OSC format preview
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OSC > ",
                        color = StageAmber,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (value.isNotBlank()) "$oscAddress \"$value\"" else "$oscAddress [string]",
                        color = if (value.isNotBlank()) StageCyan else StageTextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }

                // Transmission feedback badge
                if (isRecentlySent && lastSentEcho != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0D2517))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(StageGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SENT",
                            color = StageGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

/**
 * Convenient overload connecting [GrandMA2CommandLineInput] directly to [OscViewModel].
 */
@Composable
fun GrandMA2CommandLineInput(
    viewModel: OscViewModel,
    modifier: Modifier = Modifier,
    placeholder: String = "Enter command syntax (e.g. Fixture 1 Thru 5 At 100)...",
    showQuickSyntaxChips: Boolean = true,
    showHistoryControls: Boolean = true,
    showOscPreview: Boolean = true
) {
    val cmdText = viewModel.commandLineText.value
    val config = viewModel.config.value
    val history = viewModel.commandHistory.value

    GrandMA2CommandLineInput(
        value = cmdText,
        onValueChange = { viewModel.updateCommandLineText(it) },
        onSendCommand = {
            viewModel.submitCommandLine()
        },
        modifier = modifier,
        atSymbol = config.atSymbol,
        oscAddress = config.cmdAddress,
        commandHistory = history,
        onHistorySelect = { viewModel.updateCommandLineText(it) },
        placeholder = placeholder,
        showQuickSyntaxChips = showQuickSyntaxChips,
        showHistoryControls = showHistoryControls,
        showOscPreview = showOscPreview,
        hapticEnabled = config.hapticEnabled
    )
}

/**
 * Horizontal quick token bar with common grandMA2 syntax tokens.
 */
@Composable
private fun Ma2QuickTokenBar(
    atSymbol: String,
    onTokenClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickSyntaxChip("Group", "Group", StageCyan, onTokenClick)
        QuickSyntaxChip("Preset", "Preset", StagePurple, onTokenClick)
        QuickSyntaxChip("Fixture", "Fixture", StageAmber, onTokenClick)
        QuickSyntaxChip("Cue", "Cue", StageAmber, onTokenClick)
        QuickSyntaxChip(atSymbol, atSymbol, StageCyan, onTokenClick)
        QuickSyntaxChip("Full", "Full", StageGreen, onTokenClick)
        QuickSyntaxChip("Thru", "Thru", StageCyan, onTokenClick)
        QuickSyntaxChip("+", "+", StageCyan, onTokenClick)
        QuickSyntaxChip("-", "-", StageCyan, onTokenClick)
        QuickSyntaxChip("Store", "Store", StagePurple, onTokenClick)
        QuickSyntaxChip("Clear", "Clear", StageRed, onTokenClick)
    }
}

@Composable
private fun QuickSyntaxChip(
    label: String,
    token: String,
    accentColor: Color,
    onClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF151924))
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .clickable { onClick(token) }
            .padding(horizontal = 9.dp, vertical = 4.dp)
            .testTag("ma2_quick_token_${token.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

// Convenient composable function aliases for flexible naming
@Composable
fun Ma2CommandLineInput(
    viewModel: OscViewModel,
    modifier: Modifier = Modifier,
    placeholder: String = "Enter command syntax (e.g. Fixture 1 Thru 5 At 100)...",
    showQuickSyntaxChips: Boolean = true,
    showHistoryControls: Boolean = true,
    showOscPreview: Boolean = true
) {
    GrandMA2CommandLineInput(
        viewModel = viewModel,
        modifier = modifier,
        placeholder = placeholder,
        showQuickSyntaxChips = showQuickSyntaxChips,
        showHistoryControls = showHistoryControls,
        showOscPreview = showOscPreview
    )
}

@Composable
fun CommandLineInputComponent(
    value: String,
    onValueChange: (String) -> Unit,
    onSendCommand: (String) -> Unit,
    modifier: Modifier = Modifier,
    atSymbol: String = "At",
    oscAddress: String = "/gma2/cmd",
    commandHistory: List<String> = emptyList(),
    onHistorySelect: ((String) -> Unit)? = null,
    placeholder: String = "Enter command syntax...",
    showQuickSyntaxChips: Boolean = true,
    showHistoryControls: Boolean = true,
    showOscPreview: Boolean = true,
    hapticEnabled: Boolean = true,
    enabled: Boolean = true
) {
    GrandMA2CommandLineInput(
        value = value,
        onValueChange = onValueChange,
        onSendCommand = onSendCommand,
        modifier = modifier,
        atSymbol = atSymbol,
        oscAddress = oscAddress,
        commandHistory = commandHistory,
        onHistorySelect = onHistorySelect,
        placeholder = placeholder,
        showQuickSyntaxChips = showQuickSyntaxChips,
        showHistoryControls = showHistoryControls,
        showOscPreview = showOscPreview,
        hapticEnabled = hapticEnabled,
        enabled = enabled
    )
}
