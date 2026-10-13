package com.example.osc

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Open Sound Control (OSC 1.0) message data structure.
 */
data class OscMessage(
    val address: String,
    val arguments: List<Any> = emptyList()
) {
    constructor(address: String, vararg args: Any) : this(address, args.toList())

    override fun toString(): String {
        return "$address [${arguments.joinToString(", ")}]"
    }
}

/**
 * Serializes OscMessage instances into RFC-compliant OSC 1.0 byte arrays.
 * Handles 4-byte word boundary alignment and big-endian numerical encoding.
 */
object OscEncoder {

    fun encode(message: OscMessage): ByteArray {
        val out = ByteArrayOutputStream()

        // 1. Encode Address pattern (padded to 4-byte boundary)
        writePaddedString(out, message.address)

        // 2. Encode Type Tag string
        val typeTagBuilder = StringBuilder(",")
        for (arg in message.arguments) {
            when (arg) {
                is String -> typeTagBuilder.append('s')
                is Float, is Double -> typeTagBuilder.append('f')
                is Int, is Long, is Short, is Byte -> typeTagBuilder.append('i')
                is Boolean -> if (arg) typeTagBuilder.append('T') else typeTagBuilder.append('F')
                else -> typeTagBuilder.append('s')
            }
        }
        writePaddedString(out, typeTagBuilder.toString())

        // 3. Encode Argument values
        for (arg in message.arguments) {
            when (arg) {
                is String -> writePaddedString(out, arg)
                is Float -> writeFloat(out, arg)
                is Double -> writeFloat(out, arg.toFloat())
                is Int -> writeInt(out, arg)
                is Long -> writeInt(out, arg.toInt())
                is Short -> writeInt(out, arg.toInt())
                is Byte -> writeInt(out, arg.toInt())
                is Boolean -> {
                    // In OSC 1.0 boolean flags 'T' and 'F' carry no payload in argument section
                }
                else -> writePaddedString(out, arg.toString())
            }
        }

        return out.toByteArray()
    }

    private fun writePaddedString(out: ByteArrayOutputStream, str: String) {
        val bytes = str.toByteArray(Charsets.UTF_8)
        out.write(bytes)
        // Must contain at least one null byte and pad to 4-byte boundary
        val padLength = 4 - (bytes.size % 4)
        for (i in 0 until padLength) {
            out.write(0)
        }
    }

    private fun writeFloat(out: ByteArrayOutputStream, value: Float) {
        val buffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
        buffer.putFloat(value)
        out.write(buffer.array())
    }

    private fun writeInt(out: ByteArrayOutputStream, value: Int) {
        val buffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
        buffer.putInt(value)
        out.write(buffer.array())
    }
}

/**
 * Deserializes UDP datagram bytes into OscMessage instances.
 */
object OscDecoder {

    fun decode(bytes: ByteArray, length: Int = bytes.size): OscMessage? {
        if (length < 4) return null
        val buffer = ByteBuffer.wrap(bytes, 0, length).order(ByteOrder.BIG_ENDIAN)

        try {
            val address = readPaddedString(buffer) ?: return null
            if (!address.startsWith("/")) return null

            if (!buffer.hasRemaining()) {
                return OscMessage(address)
            }

            val typeTags = readPaddedString(buffer) ?: ""
            if (!typeTags.startsWith(",")) {
                return OscMessage(address)
            }

            val args = mutableListOf<Any>()
            for (i in 1 until typeTags.length) {
                val tag = typeTags[i]
                when (tag) {
                    's' -> {
                        val s = readPaddedString(buffer) ?: ""
                        args.add(s)
                    }
                    'f' -> {
                        if (buffer.remaining() >= 4) {
                            args.add(buffer.float)
                        }
                    }
                    'i' -> {
                        if (buffer.remaining() >= 4) {
                            args.add(buffer.int)
                        }
                    }
                    'T' -> args.add(true)
                    'F' -> args.add(false)
                    else -> {
                        // Skip unknown 4-byte aligned tag if remaining
                        if (buffer.remaining() >= 4) {
                            buffer.int
                        }
                    }
                }
            }

            return OscMessage(address, args)
        } catch (e: Exception) {
            return null
        }
    }

    private fun readPaddedString(buffer: ByteBuffer): String? {
        val startPos = buffer.position()
        var nullIndex = -1
        while (buffer.hasRemaining()) {
            val b = buffer.get()
            if (b == 0.toByte()) {
                nullIndex = buffer.position() - 1
                break
            }
        }
        if (nullIndex == -1) return null

        val strLength = nullIndex - startPos
        val bytes = ByteArray(strLength)
        buffer.position(startPos)
        buffer.get(bytes)

        // Advance to 4-byte boundary
        val pad = 4 - (bytes.size % 4)
        buffer.position(startPos + bytes.size + pad)

        return String(bytes, Charsets.UTF_8)
    }
}
