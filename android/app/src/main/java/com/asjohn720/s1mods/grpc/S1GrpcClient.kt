package com.asjohn720.s1mods.grpc

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import io.grpc.CallOptions
import io.grpc.ClientCall
import io.grpc.ManagedChannel
import io.grpc.MethodDescriptor
import io.grpc.okhttp.OkHttpChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Direct gRPC client for MoveOS Root Diagnostics Service (TCP Port 50005).
 * Weakly-typed root execution without authentication or pairing.
 */
class S1GrpcClient(
    val host: String,
    val port: Int = 50005,
    val scope: Int? = null
) {
    private var channel: ManagedChannel? = null

    private fun descriptor(method: String): MethodDescriptor<ByteArray, ByteArray> =
        MethodDescriptor.newBuilder<ByteArray, ByteArray>()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName(MethodDescriptor.generateFullMethodName("wifican.WifiCanRpc", method))
            .setRequestMarshaller(ByteArrayMarshaller)
            .setResponseMarshaller(ByteArrayMarshaller)
            .build()

    private val methodDescriptor = descriptor("ExecuteADBCommand")

    private object ByteArrayMarshaller : MethodDescriptor.Marshaller<ByteArray> {
        override fun stream(value: ByteArray): InputStream = ByteArrayInputStream(value)
        override fun parse(stream: InputStream): ByteArray = stream.readBytes()
    }

    /**
     * Resolves and normalizes the target host for OkHttpChannelBuilder.forAddress.
     */
    fun resolveTargetHost(): String {
        var h = com.asjohn720.s1mods.discovery.NetworkScanner.normalizeHost(host)

        // If fe80 link-local and missing scope, append scope if available
        if (h.startsWith("fe80", ignoreCase = true) && !h.contains("%") && scope != null) {
            h = "$h%$scope"
        }

        return try {
            InetAddress.getByName(h).hostAddress ?: h
        } catch (_: Exception) {
            h
        }
    }

    /**
     * Connects and verifies channel responsiveness using OkHttpChannelBuilder.forAddress.
     */
    suspend fun connect(timeoutSeconds: Long = 6L) = withContext(Dispatchers.IO) {
        val targetHost = resolveTargetHost()
        channel?.shutdownNow()
        val ch = OkHttpChannelBuilder.forAddress(targetHost, port)
            .usePlaintext()
            .maxInboundMessageSize(64 * 1024 * 1024)
            .build()
        channel = ch

        // Quick connectivity check using simple id -u
        val (status, out) = executeRaw("id -u", timeoutSeconds)
        if (status !in 0..1 && out.isEmpty()) {
            throw IllegalStateException("No answer from scooter port $port at $targetHost")
        }
    }

    /**
     * Executes raw command string on the scooter's daemon.
     */
    suspend fun executeRaw(command: String, timeoutSeconds: Long = 30L): Pair<Int, String> =
        unary(methodDescriptor, ProtoUtils.buildCommandPayload(command), timeoutSeconds)

    /**
     * MoveOS software version via the daemon's dedicated GetSwVersion RPC
     * (request is google.protobuf.Empty = empty bytes; response is the same
     * status+message shape as ExecuteADBCommand). Returns the message string.
     */
    suspend fun getSwVersion(timeoutSeconds: Long = 15L): String =
        unary(descriptor("GetSwVersion"), ByteArray(0), timeoutSeconds).second

    private suspend fun unary(
        method: MethodDescriptor<ByteArray, ByteArray>,
        payload: ByteArray,
        timeoutSeconds: Long
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val ch = channel ?: throw IllegalStateException("Channel not connected")

        val call: ClientCall<ByteArray, ByteArray> = ch.newCall(
            method,
            CallOptions.DEFAULT.withDeadlineAfter(timeoutSeconds, TimeUnit.SECONDS)
        )

        val resultHolder = CompletableResult<ByteArray>()
        call.start(object : ClientCall.Listener<ByteArray>() {
            override fun onMessage(message: ByteArray) {
                resultHolder.complete(message)
            }
            override fun onClose(status: io.grpc.Status, trailers: io.grpc.Metadata) {
                if (!status.isOk && !resultHolder.isDone()) {
                    resultHolder.completeExceptionally(status.asRuntimeException())
                }
            }
        }, io.grpc.Metadata())

        call.request(1)
        call.sendMessage(payload)
        call.halfClose()

        ProtoUtils.decodeResponse(resultHolder.await())
    }

    /**
     * Executes arbitrary shell commands. The command is base64-encoded and piped
     * into sh, so spaces inside quotes and filenames survive the daemon's
     * whitespace split (plain ${IFS} replacement broke `echo "a b"` and spaced paths).
     */
    suspend fun shell(command: String, timeoutSeconds: Long = 30L): Pair<Int, String> {
        val b64 = Base64.encodeToString(command.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        val wrapped = "sh -c echo\${IFS}$b64|base64\${IFS}-d|sh"
        return executeRaw(wrapped, timeoutSeconds)
    }

    /**
     * Pushes binary data to remote file path on the scooter using base64 chunking.
     */
    suspend fun push(data: ByteArray, remotePath: String, onProgress: (Float) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        val encoded = Base64.encodeToString(data, Base64.NO_WRAP)
        val chunkSize = 50_000
        val totalChunks = (encoded.length + chunkSize - 1) / chunkSize
        var chunkIndex = 0

        for (start in 0 until encoded.length step chunkSize) {
            val end = (start + chunkSize).coerceAtMost(encoded.length)
            val part = encoded.substring(start, end)
            val redirect = if (start == 0) ">" else ">>"
            val cmd = "sh -c echo\${IFS}$part|base64\${IFS}-d$redirect$remotePath"
            val (status, msg) = executeRaw(cmd, 60L)
            if (status !in 0..1) {
                throw IllegalStateException("Push failed at byte $start: $msg")
            }
            chunkIndex++
            onProgress(chunkIndex.toFloat() / totalChunks.toFloat())
        }
        true
    }

    /**
     * Pulls binary data from remote file path on the scooter using base64.
     */
    suspend fun pull(remotePath: String, timeoutSeconds: Long = 120L): ByteArray = withContext(Dispatchers.IO) {
        val (status, encoded) = shell("base64 $remotePath", timeoutSeconds)
        if (status !in 0..1) {
            throw IllegalStateException("Pull failed: $encoded")
        }
        Base64.decode(encoded.trim(), Base64.DEFAULT)
    }

    /**
     * Captures a screenshot from the scooter and returns it as a Bitmap.
     */
    suspend fun takeScreenshot(): Bitmap = withContext(Dispatchers.IO) {
        val tmp = "/data/local/tmp/_dash_shot.png"
        val (status, out) = executeRaw("screencap -p $tmp", 60L)
        if (status !in 0..1) {
            throw IllegalStateException("Screencap failed: $out")
        }
        val bytes = pull(tmp, 120L)
        shell("rm -f $tmp", 20L)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalStateException("Failed to decode screenshot bitmap")
    }

    /**
     * Unlocks ADB by pushing the public key and bind-mounting it in RAM.
     */
    suspend fun unlockAdb(adbPublicKeyContent: String): Boolean {
        // 1. Upload public key to scooter
        val keyBytes = adbPublicKeyContent.trim().plus("\n").toByteArray(StandardCharsets.UTF_8)
        push(keyBytes, "/data/local/tmp/adb_keys_mine")
        shell("chmod 644 /data/local/tmp/adb_keys_mine", 15L)

        // 2. Set scootermode to serv
        shell("setprop persist.ola.security.scootermode serv", 15L)

        // 3. Prepare and run bind mount script
        val bindScript = """#!/system/bin/sh
if [ -f /data/misc/adb/adbkeymoveos3_pub_prod ]; then
    T=/data/misc/adb/adbkeymoveos3_pub_prod
elif [ -f /data/misc/adb/adb_keys ]; then
    T=/data/misc/adb/adb_keys
else
    T=/adbkeymoveos3_pub_prod
fi
mount | grep -q " ${'$'}T " && umount "${'$'}T"
mount --bind /data/local/tmp/adb_keys_mine "${'$'}T" || exit 1
echo "bound ${'$'}T"
""".trimIndent()

        push(bindScript.toByteArray(StandardCharsets.UTF_8), "/data/local/tmp/bind_key.sh")
        shell("chmod 755 /data/local/tmp/bind_key.sh", 15L)
        val (status, out) = shell("sh /data/local/tmp/bind_key.sh", 20L)
        return (status in 0..1 && out.contains("bound"))
    }

    fun close() {
        try {
            channel?.shutdownNow()
        } catch (_: Exception) {}
        channel = null
    }

    private class CompletableResult<T> {
        private val lock = Object()
        private var result: T? = null
        private var exception: Throwable? = null
        private var done = false

        fun complete(value: T) {
            synchronized(lock) {
                result = value
                done = true
                lock.notifyAll()
            }
        }

        fun completeExceptionally(e: Throwable) {
            synchronized(lock) {
                exception = e
                done = true
                lock.notifyAll()
            }
        }

        fun isDone(): Boolean = synchronized(lock) { done }

        fun await(): T {
            synchronized(lock) {
                while (!done) {
                    lock.wait()
                }
                exception?.let { throw it }
                return result ?: throw IllegalStateException("Result is null")
            }
        }
    }
}

