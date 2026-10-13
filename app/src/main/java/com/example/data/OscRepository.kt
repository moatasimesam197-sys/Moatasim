package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ExecutorAction
import com.example.model.ExecutorButton
import com.example.model.FaderItem
import com.example.model.LayoutConfiguration
import com.example.model.OscConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Repository responsible for persisting OSC connection settings,
 * user layout configurations, and loading default console configurations.
 */
class OscRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ma2_remote_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TARGET_IP = "target_ip"
        private const val KEY_OSC_IN_PORT = "osc_in_port"
        private const val KEY_OSC_OUT_PORT = "osc_out_port"
        private const val KEY_OSC_PREFIX = "osc_prefix"
        private const val KEY_CMD_ADDRESS = "cmd_address"
        private const val KEY_AT_SYMBOL = "at_symbol"
        private const val KEY_RATE_LIMIT = "rate_limit"
        private const val KEY_AUTO_RECONNECT = "auto_reconnect"
        private const val KEY_HAPTIC = "haptic"
        private const val KEY_CURRENT_PAGE = "current_page"
        private const val KEY_SELECTED_EXEC = "selected_exec"
        private const val KEY_BLACKOUT = "blackout_active"
        private const val KEY_SAVED_LAYOUT = "saved_layout_json"
    }

    fun loadConfig(): OscConfig {
        return OscConfig(
            targetIp = prefs.getString(KEY_TARGET_IP, "192.168.1.100") ?: "192.168.1.100",
            oscReceivePort = prefs.getInt(KEY_OSC_IN_PORT, 8000),
            oscSendPort = prefs.getInt(KEY_OSC_OUT_PORT, 8001),
            prefix = prefs.getString(KEY_OSC_PREFIX, "/gma2") ?: "/gma2",
            cmdAddress = prefs.getString(KEY_CMD_ADDRESS, "/gma2/cmd") ?: "/gma2/cmd",
            atSymbol = prefs.getString(KEY_AT_SYMBOL, "@") ?: "@",
            faderRateLimitHz = prefs.getInt(KEY_RATE_LIMIT, 30),
            autoReconnect = prefs.getBoolean(KEY_AUTO_RECONNECT, true),
            hapticEnabled = prefs.getBoolean(KEY_HAPTIC, true)
        )
    }

    fun saveConfig(config: OscConfig) {
        prefs.edit()
            .putString(KEY_TARGET_IP, config.targetIp)
            .putInt(KEY_OSC_IN_PORT, config.oscReceivePort)
            .putInt(KEY_OSC_OUT_PORT, config.oscSendPort)
            .putString(KEY_OSC_PREFIX, config.prefix)
            .putString(KEY_CMD_ADDRESS, config.cmdAddress)
            .putString(KEY_AT_SYMBOL, config.atSymbol)
            .putInt(KEY_RATE_LIMIT, config.faderRateLimitHz)
            .putBoolean(KEY_AUTO_RECONNECT, config.autoReconnect)
            .putBoolean(KEY_HAPTIC, config.hapticEnabled)
            .apply()
    }

    fun getBlackoutState(): Boolean = prefs.getBoolean(KEY_BLACKOUT, false)
    fun setBlackoutState(active: Boolean) {
        prefs.edit().putBoolean(KEY_BLACKOUT, active).apply()
    }

    fun getCurrentPage(): Int = prefs.getInt(KEY_CURRENT_PAGE, 1)
    fun setCurrentPage(page: Int) {
        prefs.edit().putInt(KEY_CURRENT_PAGE, page.coerceAtLeast(1)).apply()
    }

    fun getSelectedExec(): String = prefs.getString(KEY_SELECTED_EXEC, "1.1") ?: "1.1"
    fun setSelectedExec(exec: String) {
        prefs.edit().putString(KEY_SELECTED_EXEC, exec).apply()
    }

    /**
     * Reads raw JSON configuration bundled in assets.
     */
    fun loadRawAssetConfigJson(): String {
        return try {
            context.assets.open("osc_config.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "{}"
        }
    }

    /**
     * Loads the active user layout, falling back to default Concert Busking preset.
     */
    fun loadLayout(): LayoutConfiguration {
        val savedJson = prefs.getString(KEY_SAVED_LAYOUT, null)
        if (!savedJson.isNullOrBlank()) {
            val parsed = parseLayoutFromJson(savedJson)
            if (parsed != null) return parsed
        }
        return getDefaultConcertLayout()
    }

    fun saveLayout(layout: LayoutConfiguration) {
        val json = serializeLayoutToJson(layout)
        prefs.edit().putString(KEY_SAVED_LAYOUT, json).apply()
    }

    fun getDefaultConcertLayout(): LayoutConfiguration {
        return LayoutConfiguration(
            name = "Concert Live Busking",
            description = "High-energy festival and concert busking layout",
            executorButtons = listOf(
                ExecutorButton("b1", 1, 201, "Strobe Blast", ExecutorAction.FLASH, "#FF1744", gridCol = 0, gridRow = 2, colSpan = 1, rowSpan = 1),
                ExecutorButton("b2", 1, 202, "Audience Blinders", ExecutorAction.TEMP, "#FFD600", gridCol = 1, gridRow = 2, colSpan = 1, rowSpan = 1),
                ExecutorButton("b3", 1, 203, "Chorus Washes", ExecutorAction.GO, "#00E5FF", gridCol = 2, gridRow = 2, colSpan = 1, rowSpan = 1),
                ExecutorButton("b4", 1, 204, "Verse Mood", ExecutorAction.GO, "#7C4DFF", gridCol = 3, gridRow = 2, colSpan = 1, rowSpan = 1),
                ExecutorButton("b5", 1, 205, "Solo Followspot", ExecutorAction.TEMP, "#FF9100", gridCol = 0, gridRow = 3, colSpan = 1, rowSpan = 1),
                ExecutorButton("b6", 1, 206, "Crowd Sweep FX", ExecutorAction.GO, "#00E676", gridCol = 1, gridRow = 3, colSpan = 1, rowSpan = 1),
                ExecutorButton("b7", 1, 207, "Ballyhoo FX", ExecutorAction.GO, "#E040FB", gridCol = 2, gridRow = 3, colSpan = 1, rowSpan = 1),
                ExecutorButton("b8", 1, 208, "Haze Burst", ExecutorAction.TEMP, "#1DE9B6", gridCol = 3, gridRow = 3, colSpan = 1, rowSpan = 1),
                ExecutorButton("b9", 1, 209, "Front Key Light", ExecutorAction.TOGGLE, "#FFAB00", gridCol = 0, gridRow = 4, colSpan = 2, rowSpan = 1),
                ExecutorButton("b10", 1, 210, "All Off / Park", ExecutorAction.OFF, "#D50000", gridCol = 2, gridRow = 4, colSpan = 2, rowSpan = 1)
            ),
            faders = listOf(
                FaderItem("f_master", 1, 0, "Grand Master", 1.0f, isMaster = true, colorHex = "#FF1744", gridCol = 0, gridRow = 0, colSpan = 1, rowSpan = 2),
                FaderItem("f1", 1, 1, "Front Warm", 0.85f, colorHex = "#FFAB00", gridCol = 1, gridRow = 0, colSpan = 1, rowSpan = 2),
                FaderItem("f2", 1, 2, "Color Washes", 0.70f, colorHex = "#00E5FF", gridCol = 2, gridRow = 0, colSpan = 1, rowSpan = 2),
                FaderItem("f3", 1, 3, "Spot Profiles", 0.60f, colorHex = "#7C4DFF", gridCol = 3, gridRow = 0, colSpan = 1, rowSpan = 2)
            ),
            macros = listOf(
                com.example.model.MacroButton("m1", 1, "Circle Pan/Tilt FX", "Macro 1", "#D500F9", 2000L),
                com.example.model.MacroButton("m2", 2, "All Washes Red", "Macro 2", "#FF1744", 1500L),
                com.example.model.MacroButton("m3", 3, "All Washes Cyan", "Macro 3", "#00E5FF", 1500L),
                com.example.model.MacroButton("m4", 4, "Strobe 120 BPM", "Macro 4", "#FFD600", 1200L),
                com.example.model.MacroButton("m5", 5, "Park House Lights", "Macro 5", "#FF9100", 2500L),
                com.example.model.MacroButton("m6", 6, "Calibrate Fixtures", "Macro 6", "#00E676", 3000L)
            )
        )
    }

    fun getDefaultTheaterLayout(): LayoutConfiguration {
        return LayoutConfiguration(
            name = "Theater Cue Playback",
            description = "Cue-stack driven theater performance layout",
            executorButtons = listOf(
                ExecutorButton("tb1", 1, 1, "Main Cue Stack", ExecutorAction.GO, "#00E5FF", gridCol = 0, gridRow = 2, colSpan = 2, rowSpan = 1),
                ExecutorButton("tb2", 1, 2, "House Lights", ExecutorAction.TOGGLE, "#FFAB00", gridCol = 2, gridRow = 2, colSpan = 2, rowSpan = 1),
                ExecutorButton("tb3", 1, 3, "Intermission Mood", ExecutorAction.GO, "#7C4DFF", gridCol = 0, gridRow = 3, colSpan = 2, rowSpan = 1),
                ExecutorButton("tb4", 1, 4, "Curtain Call Cue", ExecutorAction.GO, "#00E676", gridCol = 2, gridRow = 3, colSpan = 2, rowSpan = 1),
                ExecutorButton("tb5", 1, 5, "Orchestra Work", ExecutorAction.TOGGLE, "#FFD600", gridCol = 0, gridRow = 4, colSpan = 2, rowSpan = 1),
                ExecutorButton("tb6", 1, 6, "Emergency Park", ExecutorAction.OFF, "#FF1744", gridCol = 2, gridRow = 4, colSpan = 2, rowSpan = 1)
            ),
            faders = listOf(
                FaderItem("tf_master", 1, 0, "Grand Master", 1.0f, isMaster = true, colorHex = "#FF1744", gridCol = 0, gridRow = 0, colSpan = 1, rowSpan = 2),
                FaderItem("tf1", 1, 1, "Main Playback", 1.0f, colorHex = "#00E5FF", gridCol = 1, gridRow = 0, colSpan = 1, rowSpan = 2),
                FaderItem("tf2", 1, 2, "House Preset", 0.0f, colorHex = "#FFAB00", gridCol = 2, gridRow = 0, colSpan = 1, rowSpan = 2),
                FaderItem("tf3", 1, 3, "Stage Blue Night", 0.65f, colorHex = "#3D5AFE", gridCol = 3, gridRow = 0, colSpan = 1, rowSpan = 2)
            ),
            macros = listOf(
                com.example.model.MacroButton("tm1", 10, "Preset House 50%", "Macro 10", "#FFAB00", 2000L),
                com.example.model.MacroButton("tm2", 11, "Blackout Work Lights", "Macro 11", "#FF1744", 1500L),
                com.example.model.MacroButton("tm3", 12, "Auditorium Chime", "Macro 12", "#00E5FF", 2200L),
                com.example.model.MacroButton("tm4", 13, "Stage Blue Preset", "Macro 13", "#3D5AFE", 1800L)
            )
        )
    }

    fun serializeLayoutToJson(layout: LayoutConfiguration): String {
        val root = JSONObject()
        root.put("name", layout.name)
        root.put("description", layout.description)

        val btnArray = JSONArray()
        for (btn in layout.executorButtons) {
            val bObj = JSONObject()
            bObj.put("id", btn.id)
            bObj.put("page", btn.page)
            bObj.put("execNumber", btn.execNumber)
            bObj.put("label", btn.label)
            bObj.put("action", btn.action.name)
            bObj.put("colorHex", btn.colorHex)
            bObj.put("gridCol", btn.gridCol)
            bObj.put("gridRow", btn.gridRow)
            bObj.put("colSpan", btn.colSpan)
            bObj.put("rowSpan", btn.rowSpan)
            btnArray.put(bObj)
        }
        root.put("executorButtons", btnArray)

        val faderArray = JSONArray()
        for (fader in layout.faders) {
            val fObj = JSONObject()
            fObj.put("id", fader.id)
            fObj.put("page", fader.page)
            fObj.put("execNumber", fader.execNumber)
            fObj.put("label", fader.label)
            fObj.put("level", fader.level.toDouble())
            fObj.put("isMaster", fader.isMaster)
            fObj.put("colorHex", fader.colorHex)
            fObj.put("gridCol", fader.gridCol)
            fObj.put("gridRow", fader.gridRow)
            fObj.put("colSpan", fader.colSpan)
            fObj.put("rowSpan", fader.rowSpan)
            faderArray.put(fObj)
        }
        root.put("faders", faderArray)

        val macroArray = JSONArray()
        for (m in layout.macros) {
            val mObj = JSONObject()
            mObj.put("id", m.id)
            mObj.put("macroNumber", m.macroNumber)
            mObj.put("label", m.label)
            mObj.put("command", m.command)
            mObj.put("colorHex", m.colorHex)
            mObj.put("executionDurationMs", m.executionDurationMs)
            macroArray.put(mObj)
        }
        root.put("macros", macroArray)

        return root.toString(2)
    }

    fun parseLayoutFromJson(jsonStr: String): LayoutConfiguration? {
        return try {
            val root = JSONObject(jsonStr)
            val name = root.optString("name", "Custom Layout")
            val desc = root.optString("description", "")

            val buttons = mutableListOf<ExecutorButton>()
            val btnArray = root.optJSONArray("executorButtons")
            if (btnArray != null) {
                for (i in 0 until btnArray.length()) {
                    val obj = btnArray.getJSONObject(i)
                    val actionStr = obj.optString("action", "GO")
                    val action = try { ExecutorAction.valueOf(actionStr) } catch (e: Exception) { ExecutorAction.GO }
                    buttons.add(
                        ExecutorButton(
                            id = obj.optString("id", "btn_$i"),
                            page = obj.optInt("page", 1),
                            execNumber = obj.optInt("execNumber", 200 + i),
                            label = obj.optString("label", "Exec $i"),
                            action = action,
                            colorHex = obj.optString("colorHex", "#FF9F1C"),
                            gridCol = obj.optInt("gridCol", i % 4),
                            gridRow = obj.optInt("gridRow", 2 + (i / 4)),
                            colSpan = obj.optInt("colSpan", 1),
                            rowSpan = obj.optInt("rowSpan", 1)
                        )
                    )
                }
            }

            val faders = mutableListOf<FaderItem>()
            val faderArray = root.optJSONArray("faders")
            if (faderArray != null) {
                for (i in 0 until faderArray.length()) {
                    val obj = faderArray.getJSONObject(i)
                    faders.add(
                        FaderItem(
                            id = obj.optString("id", "fader_$i"),
                            page = obj.optInt("page", 1),
                            execNumber = obj.optInt("execNumber", i),
                            label = obj.optString("label", if (obj.optBoolean("isMaster")) "Grand Master" else "Exec $i"),
                            level = obj.optDouble("level", 0.0).toFloat().coerceIn(0f, 1f),
                            isMaster = obj.optBoolean("isMaster", false),
                            colorHex = obj.optString("colorHex", "#00F0FF"),
                            gridCol = obj.optInt("gridCol", i % 4),
                            gridRow = obj.optInt("gridRow", (i / 4) * 2),
                            colSpan = obj.optInt("colSpan", 1),
                            rowSpan = obj.optInt("rowSpan", 2)
                        )
                    )
                }
            }

            val macros = mutableListOf<com.example.model.MacroButton>()
            val macroArray = root.optJSONArray("macros")
            if (macroArray != null) {
                for (i in 0 until macroArray.length()) {
                    val obj = macroArray.getJSONObject(i)
                    val mNum = obj.optInt("macroNumber", i + 1)
                    macros.add(
                        com.example.model.MacroButton(
                            id = obj.optString("id", "m_$i"),
                            macroNumber = mNum,
                            label = obj.optString("label", "Macro $mNum"),
                            command = obj.optString("command", "Macro $mNum"),
                            colorHex = obj.optString("colorHex", "#D500F9"),
                            executionDurationMs = obj.optLong("executionDurationMs", 1800L)
                        )
                    )
                }
            }

            LayoutConfiguration(name, desc, buttons, faders, macros)
        } catch (e: Exception) {
            null
        }
    }
}
