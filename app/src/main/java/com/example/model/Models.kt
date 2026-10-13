package com.example.model

/**
 * Connection states for grandMA2 onPC network status.
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    data class Connecting(val attempt: Int = 1, val message: String = "Connecting...") : ConnectionState()
    data class Connected(val targetHost: String, val targetPort: Int) : ConnectionState()
    data class Error(val message: String, val canRetry: Boolean = true) : ConnectionState()
}

/**
 * Actions supported by Executor buttons.
 */
enum class ExecutorAction {
    GO,
    FLASH,
    TOGGLE,
    TEMP,
    OFF
}

/**
 * Representation of a single button in the executor grid with drag-and-drop placement coordinates.
 */
data class ExecutorButton(
    val id: String,
    val page: Int = 1,
    val execNumber: Int = 201,
    val label: String = "Exec",
    val action: ExecutorAction = ExecutorAction.GO,
    val colorHex: String = "#FF9F1C", // Default Amber
    val isActive: Boolean = false,
    val gridCol: Int = 0,
    val gridRow: Int = 0,
    val colSpan: Int = 1,
    val rowSpan: Int = 1
)

/**
 * Representation of an executor or grand master fader with drag-and-drop placement coordinates.
 */
data class FaderItem(
    val id: String,
    val page: Int = 1,
    val execNumber: Int = 1,
    val label: String = "Exec 1",
    val level: Float = 0.0f, // 0.0 to 1.0 (0% to 100%)
    val isMaster: Boolean = false,
    val colorHex: String = "#00F0FF",
    val gridCol: Int = 0,
    val gridRow: Int = 0,
    val colSpan: Int = 1,
    val rowSpan: Int = 2
)

/**
 * Direction of an OSC network packet in diagnostics.
 */
enum class LogDirection {
    TX, // Transmitted to grandMA2
    RX, // Received from grandMA2
    SYS // System event / connection change
}

/**
 * Item in the live OSC telemetry log.
 */
data class OscLogItem(
    val id: Long = System.currentTimeMillis(),
    val timestamp: String,
    val direction: LogDirection,
    val address: String,
    val payload: String,
    val isError: Boolean = false
)

/**
 * Representation of an MA2 Macro button with execution state tracking.
 */
data class MacroButton(
    val id: String,
    val macroNumber: Int = 1,
    val label: String = "Macro 1",
    val command: String = "Macro 1",
    val colorHex: String = "#D500F9", // Default Laser Violet
    val executionDurationMs: Long = 1800L,
    val isExecuting: Boolean = false,
    val lastExecutedTime: String? = null
)

/**
 * Layout configuration model for JSON import / export.
 */
data class LayoutConfiguration(
    val name: String,
    val description: String = "",
    val executorButtons: List<ExecutorButton> = emptyList(),
    val faders: List<FaderItem> = emptyList(),
    val macros: List<MacroButton> = emptyList()
)

/**
 * Network and OSC mapping configuration.
 */
data class OscConfig(
    val targetIp: String = "192.168.1.100",
    val oscReceivePort: Int = 8000, // In Port on grandMA2
    val oscSendPort: Int = 8001,    // Out Port on grandMA2
    val prefix: String = "/gma2",
    val cmdAddress: String = "/gma2/cmd",
    val faderAddressTemplate: String = "/gma2/fader/{page}/{exec}",
    val masterFaderAddress: String = "/gma2/master",
    val atSymbol: String = "@",     // "@" or "At"
    val faderRateLimitHz: Int = 30, // max updates per second
    val autoReconnect: Boolean = true,
    val hapticEnabled: Boolean = true
)
