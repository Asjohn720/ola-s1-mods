package com.asjohn720.s1mods.grpc

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * Lightweight protobuf varint and wire-format serializer/deserializer.
 * Matches ola.py's weakly-typed wire format for /wifican.WifiCanRpc/ExecuteADBCommand.
 */
object ProtoUtils {

    /**
     * Encodes an integer/long as a protobuf varint.
     */
    fun encodeVarint(value: Long): ByteArray {
        val out = ByteArrayOutputStream()
        var v = value
        while (true) {
            val b = (v and 0x7F).toInt()
            v = v ushr 7
            if (v != 0L) {
                out.write(b or 0x80)
            } else {
                out.write(b)
                break
            }
        }
        return out.toByteArray()
    }

    /**
     * Builds the request payload for ExecuteADBCommand:
     * Field 1 (tag = (1 shl 3) | 2 = 10, wire type 2 = length-delimited),
     * followed by varint length, followed by utf-8 bytes.
     */
    fun buildCommandPayload(command: String): ByteArray {
        val raw = command.toByteArray(StandardCharsets.UTF_8)
        val tag = encodeVarint(((1 shl 3) or 2).toLong())
        val len = encodeVarint(raw.size.toLong())
        return tag + len + raw
    }

    /**
     * Reads a varint from buffer starting at index [start].
     * Returns Pair(value, nextIndex).
     */
    fun readVarint(buf: ByteArray, start: Int): Pair<Long, Int> {
        var value = 0L
        var shift = 0
        var i = start
        while (i < buf.size) {
            val b = buf[i++].toLong()
            value = value or ((b and 0x7F) shl shift)
            if ((b and 0x80L) == 0L) {
                return Pair(value, i)
            }
            shift += 7
        }
        return Pair(value, i)
    }

    /**
     * Decodes the response buffer from gRPC calls.
     * Response fields typically:
     * Field 1 = status code (varint, wire type 0) OR string payload
     * Field 2 = message string (length-delimited, wire type 2)
     */
    fun decodeResponse(buf: ByteArray): Pair<Int, String> {
        var status = 0
        var message = ""
        var fallbackStr = ""
        var i = 0

        while (i < buf.size) {
            val (key, nextI) = readVarint(buf, i)
            i = nextI
            val field = (key ushr 3).toInt()
            val wire = (key and 0x7).toInt()

            when (wire) {
                0 -> {
                    val (valVarint, endI) = readVarint(buf, i)
                    i = endI
                    if (field == 1) status = valVarint.toInt()
                }
                2 -> {
                    val (len, endI) = readVarint(buf, i)
                    i = endI
                    val length = len.toInt()
                    if (i + length <= buf.size) {
                        val str = String(buf, i, length, StandardCharsets.UTF_8)
                        if (field == 2) {
                            message = str
                        } else if (fallbackStr.isEmpty()) {
                            fallbackStr = str
                        }
                        i += length
                    } else {
                        break
                    }
                }
                else -> break
            }
        }
        if (message.isEmpty() && fallbackStr.isNotEmpty()) {
            message = fallbackStr
        }
        return Pair(status, message)
    }

    /**
     * Extracts all wire fields from a protobuf buffer for introspection.
     */
    fun decodeProtoFields(buf: ByteArray): Map<Int, Any> {
        val result = mutableMapOf<Int, Any>()
        var i = 0
        while (i < buf.size) {
            val (key, nextI) = readVarint(buf, i)
            i = nextI
            val field = (key ushr 3).toInt()
            val wire = (key and 0x7).toInt()

            when (wire) {
                0 -> {
                    val (valVarint, endI) = readVarint(buf, i)
                    i = endI
                    result[field] = valVarint
                }
                2 -> {
                    val (len, endI) = readVarint(buf, i)
                    i = endI
                    val length = len.toInt()
                    if (i + length <= buf.size) {
                        val str = String(buf, i, length, StandardCharsets.UTF_8)
                        result[field] = str
                        i += length
                    } else {
                        break
                    }
                }
                else -> break
            }
        }
        return result
    }
}

