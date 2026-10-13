package com.example.osc

import android.util.Log
import com.example.model.ConnectionState
import com.example.model.LogDirection
import com.example.model.OscConfig
import com.example.model.OscLogItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Service managing UDP socket communications with grandMA2 onPC.
 * Features rate-limiting, exponential backoff, background listener, and live diagnostics logging.
 */
class OscConnectionService(
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "OscConnectionService"
        private const val MAX_LOG_ENTRIES = 200
        private const val RECEIVE_BUFFER_SIZE = 4096
    }

    private var socket: DatagramSocket? = null
    private var receiveJob: Job? = null
    private var reconnectJob: Job? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingOscMessages = MutableSharedFlow<OscMessage>(extraBufferCapacity = 64)
    val incomingOscMessages: SharedFlow<OscMessage> = _incomingOscMessages.asSharedFlow()

    private val _logs = MutableStateFlow<List<OscLogItem>>(emptyList())
    val logs: StateFlow<List<OscLogItem>> = _logs.asStateFlow()

    private val logQueue = ConcurrentLinkedDeque<OscLogItem>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    // Rate limiting tracking for faders (key: fader address or ID -> last send timestamp ms)
    private val lastFaderSendTimes = ConcurrentHashMap<String, Long>()
    private val pendingFaderJobs = ConcurrentHashMap<String, Job>()
    private val pendingFaderMessages = ConcurrentHashMap<String, OscMessage>()

    private var currentConfig: OscConfig = OscConfig()
    private var reconnectAttempt = 0
    private var isUserInitiatedDisconnect = false

    fun updateConfig(config: OscConfig) {
        val wasConnected = _connectionState.value is ConnectionState.Connected
        currentConfig = config
        if (wasConnected) {
            connect(config)
        }
    }

    fun connect(config: OscConfig = currentConfig) {
        currentConfig = config
        isUserInitiatedDisconnect = false
        reconnectJob?.cancel()

        scope.launch(Dispatchers.IO) {
            try {
                _connectionState.value = ConnectionState.Connecting(attempt = reconnectAttempt + 1)
                logSystem("Initializing UDP connection to ${config.targetIp}:${config.oscReceivePort}...")

                // Validate IP
                val targetAddress = InetAddress.getByName(config.targetIp.trim())

                // Close any existing socket
                closeSocket()

                // Bind receive socket on specified OSC send/feedback port (fall back to wildcard port if bound)
                val newSocket = try {
                    DatagramSocket(config.oscSendPort)
                } catch (e: Exception) {
                    Log.w(TAG, "Port ${config.oscSendPort} unavailable, binding to ephemeral port: ${e.message}")
                    DatagramSocket()
                }
                newSocket.broadcast = true
                socket = newSocket

                _connectionState.value = ConnectionState.Connected(
                    targetHost = config.targetIp,
                    targetPort = config.oscReceivePort
                )
                reconnectAttempt = 0
                logSystem("UDP socket active on local port ${newSocket.localPort}. Target set to ${config.targetIp}:${config.oscReceivePort}.")

                // Start background receive loop
                startReceiveLoop(newSocket)

            } catch (e: Exception) {
                Log.e(TAG, "Connection failed: ${e.message}", e)
                _connectionState.value = ConnectionState.Error(e.localizedMessage ?: "Failed to resolve host or bind UDP socket")
                logSystem("Connection error: ${e.message}", isError = true)
                triggerReconnectWithBackoff()
            }
        }
    }

    fun disconnect() {
        isUserInitiatedDisconnect = true
        reconnectJob?.cancel()
        reconnectAttempt = 0
        closeSocket()
        _connectionState.value = ConnectionState.Disconnected
        logSystem("Disconnected from console by user.")
    }

    private fun closeSocket() {
        receiveJob?.cancel()
        receiveJob = null
        try {
            socket?.close()
        } catch (ignored: Exception) {}
        socket = null
    }

    private fun triggerReconnectWithBackoff() {
        if (isUserInitiatedDisconnect || !currentConfig.autoReconnect) return

        reconnectJob?.cancel()
        reconnectJob = scope.launch(Dispatchers.IO) {
            reconnectAttempt++
            // Exponential backoff: 2s, 4s, 8s, 16s... up to 30s
            val delayMs = (2000L * (1 shl minOf(reconnectAttempt - 1, 4))).coerceAtMost(30000L)
            logSystem("Reconnection attempt #$reconnectAttempt scheduled in ${delayMs / 1000}s...")
            _connectionState.value = ConnectionState.Connecting(
                attempt = reconnectAttempt,
                message = "Reconnecting in ${delayMs / 1000}s..."
            )
            delay(delayMs)
            if (isActive && !isUserInitiatedDisconnect) {
                connect(currentConfig)
            }
        }
    }

    private fun startReceiveLoop(ds: DatagramSocket) {
        receiveJob?.cancel()
        receiveJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(RECEIVE_BUFFER_SIZE)
            val packet = DatagramPacket(buffer, buffer.size)

            while (isActive && !ds.isClosed) {
                try {
                    ds.receive(packet)
                    val receivedBytes = packet.data.copyOf(packet.length)
                    val oscMsg = OscDecoder.decode(receivedBytes, packet.length)

                    if (oscMsg != null) {
                        logMessage(LogDirection.RX, oscMsg.address, oscMsg.arguments.joinToString(", "))
                        _incomingOscMessages.emit(oscMsg)
                    } else {
                        val preview = String(receivedBytes, 0, minOf(packet.length, 32))
                        logMessage(LogDirection.RX, "(raw)", preview)
                    }
                } catch (e: Exception) {
                    if (!isActive || ds.isClosed) break
                    Log.w(TAG, "Socket receive error: ${e.message}")
                    triggerReconnectWithBackoff()
                    break
                }
            }
        }
    }

    /**
     * Sends an OSC command string to the grandMA2 command line address (e.g. /gma2/cmd).
     */
    fun sendCommand(commandText: String): Boolean {
        val address = currentConfig.cmdAddress
        return sendMessage(OscMessage(address, commandText))
    }

    /**
     * Sends an OSC Message. Handles rate-limiting (max 30 Hz / 33ms) if rateLimitKey is specified.
     * Implements trailing-edge dispatch to guarantee the final fader position is always delivered.
     */
    fun sendMessage(message: OscMessage, rateLimitKey: String? = null): Boolean {
        if (rateLimitKey != null) {
            val now = System.currentTimeMillis()
            val minIntervalMs = (1000.0 / currentConfig.faderRateLimitHz.coerceIn(5, 60)).toLong()
            val lastTime = lastFaderSendTimes[rateLimitKey] ?: 0L
            val elapsed = now - lastTime

            if (elapsed < minIntervalMs) {
                // Buffer the latest message for trailing-edge dispatch when the time slot arrives
                pendingFaderMessages[rateLimitKey] = message
                if (pendingFaderJobs[rateLimitKey]?.isActive != true) {
                    val remainingDelay = minIntervalMs - elapsed
                    pendingFaderJobs[rateLimitKey] = scope.launch(Dispatchers.IO) {
                        delay(remainingDelay)
                        val trailingMsg = pendingFaderMessages.remove(rateLimitKey)
                        if (trailingMsg != null) {
                            lastFaderSendTimes[rateLimitKey] = System.currentTimeMillis()
                            sendRawOsc(trailingMsg)
                        }
                    }
                }
                return true
            }

            // Immediately send and cancel previous trailing job for this fader
            pendingFaderJobs[rateLimitKey]?.cancel()
            pendingFaderMessages.remove(rateLimitKey)
            lastFaderSendTimes[rateLimitKey] = now
        }

        return sendRawOsc(message)
    }

    private fun sendRawOsc(message: OscMessage): Boolean {
        return try {
            val bytes = OscEncoder.encode(message)
            val activeSocket = socket ?: DatagramSocket().also { socket = it }
            val targetAddr = InetAddress.getByName(currentConfig.targetIp.trim())
            val packet = DatagramPacket(bytes, bytes.size, targetAddr, currentConfig.oscReceivePort)

            scope.launch(Dispatchers.IO) {
                try {
                    activeSocket.send(packet)
                    logMessage(
                        LogDirection.TX,
                        message.address,
                        message.arguments.joinToString(", ")
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error transmitting UDP packet: ${e.message}", e)
                    logMessage(LogDirection.TX, message.address, "ERR: ${e.message}", isError = true)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encode/send OSC message: ${e.message}", e)
            logSystem("Send failure: ${e.message}", isError = true)
            false
        }
    }

    private fun logMessage(direction: LogDirection, address: String, payload: String, isError: Boolean = false) {
        val item = OscLogItem(
            timestamp = timeFormat.format(Date()),
            direction = direction,
            address = address,
            payload = payload,
            isError = isError
        )
        addLog(item)
    }

    private fun logSystem(message: String, isError: Boolean = false) {
        val item = OscLogItem(
            timestamp = timeFormat.format(Date()),
            direction = LogDirection.SYS,
            address = "[SYSTEM]",
            payload = message,
            isError = isError
        )
        addLog(item)
    }

    private fun addLog(item: OscLogItem) {
        logQueue.addFirst(item)
        while (logQueue.size > MAX_LOG_ENTRIES) {
            logQueue.removeLast()
        }
        _logs.value = logQueue.toList()
    }

    fun clearLogs() {
        logQueue.clear()
        _logs.value = emptyList()
    }
}
