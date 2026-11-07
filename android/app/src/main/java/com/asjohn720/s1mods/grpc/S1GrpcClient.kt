package com.asjohn720.s1mods.grpc

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import io.grpc.Attributes
import io.grpc.CallOptions
import io.grpc.ClientCall
import io.grpc.EquivalentAddressGroup
import io.grpc.ManagedChannel
import io.grpc.MethodDescriptor
import io.grpc.NameResolver
import io.grpc.NameResolverProvider
import io.grpc.NameResolverRegistry
import io.grpc.StatusRuntimeException
import io.grpc.okhttp.OkHttpChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.concurrent.TimeUnit

/**
 * Direct gRPC client for MoveOS Root Diagnostics Service (TCP Port 50005).
 * Weakly-typed root execution without authentication or pairing.
 * Supports multi-method fallback and link-local scoped IPv6 address resolution.
 */
class S1GrpcClient(
    val host: String,
    val port: Int = 50005,
    val scope: Int? = null
) {
    companion object {
        private const val SCHEME = "s1mods"
        private var resolverInstalled = false

        fun installResolver() {
            if (resolverInstalled) return
            synchronized(this) {
                if (resolverInstalled) return
                NameResolverRegistry.getDefaultRegistry().register(ScopedResolverProvider())
                resolverInstalled = true
            }
        }

        private fun hexEncode(text: String): String {
            val bytes = text.toByteArray(StandardCharsets.UTF_8)
            val hexChars = "0123456789abcdef"
            val sb = StringBuilder(bytes.size * 2)
            for (b in bytes) {
                val i = b.toInt() and 0xFF
                sb.append(hexChars[i ushr 4])
                sb.append(hexChars[i and 0x0F])
            }
            return sb.toString()
        }

        private fun hexDecode(hex: String): String {
            val bytes = ByteArray(hex.length / 2)
            for (i in 0 until hex.length step 2) {
                bytes[i / 2] = hex.substring(i, i + 2).toInt(16).toByte()
            }
            return String(bytes, StandardCharsets.UTF_8)
        }
    }

    private var channel: ManagedChannel? = null

    var activeServiceName: String = "wifican.WifiCanRpc"
        private set

    var activeMethodName: String = "ExecuteADBCommand"
        private set

    var detectedSoftwareVersion: String? = null
        private set

    val candidateExecutionMethods = listOf(
        "ExecuteADBCommand",
        "ExecuteAdbCommand",
        "executeADBCommand",
        "executeAdbCommand",
        "ExecuteAdbCmd",
        "executeAdbCmd",
        "ExecuteCommand",
        "executeCommand",
        "ExecCommand",
        "execCommand",
        "RunCmd",
        "runCmd",
        "RunCommand",
        "runCommand",
        "RunShellCommand",
        "runShellCommand",
        "ShellCommand",
        "shellCommand",
        "RunAdbCommand",
        "runAdbCommand",
        "RunADBCommand",
        "runADBCommand",
        "AdbCommand",
        "adbCommand",
        "AdbCmd",
        "adbCmd",
        "AdbExec",
        "adbExec",
        "Execute",
        "execute",
        "Exec",
        "exec",
        "RunSystemTestCmd",
        "RemoteRunTestCmd"
    )

    private fun buildMethodDescriptor(
        methodName: String,
        serviceName: String = activeServiceName
    ): MethodDescriptor<ByteArray, ByteArray> {
        return MethodDescriptor.newBuilder<ByteArray, ByteArray>()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName(MethodDescriptor.generateFullMethodName(serviceName, methodName))
            .setRequestMarshaller(ByteArrayMarshaller)
            .setResponseMarshaller(ByteArrayMarshaller)
            .build()
    }

    private object ByteArrayMarshaller : MethodDescriptor.Marshaller<ByteArray> {
        override fun stream(value: ByteArray): InputStream = ByteArrayInputStream(value)
        override fun parse(stream: InputStream): ByteArray = stream.readBytes()
    }

    /**
     * Formats target host as an RFC-3986 compliant URI parsed safely by ScopedResolver.
     * Prevents java.net.URI crashes on scoped link-local IPv6 addresses (e.g. fe80::...%109).
     */
    fun targetUri(): String {
        var h = host.trim()
        if (h.lowercase().startsWith("ipv6:")) {
            h = h.substring(5).trim()
        }
        h = h.trim('[', ']')
        if (h.contains("]")) {
            h = h.split("]")[0].trimStart('[')
        }

        if (h.count { it == ':' } >= 2) {
            // IPv6 Address
            if (h.lowercase().startsWith("fe80") && !h.contains("%")) {
                if (scope != null) {
                    h = "$h%$scope"
                }
            }
        }
        return "$SCHEME:///${hexEncode(h)}/$port"
    }

    /**
     * Connects and verifies channel responsiveness with multi-method auto-discovery.
     */
    suspend fun connect(timeoutSeconds: Long = 6L, onLog: (String) -> Unit = {}) = withContext(Dispatchers.IO) {
        installResolver()
        val target = targetUri()
        channel?.shutdownNow()
        val ch = OkHttpChannelBuilder.forTarget(target)
            .usePlaintext()
            .maxInboundMessageSize(64 * 1024 * 1024)
            .build()
        channel = ch

        var connected = false
        var lastError: Exception? = null

        // 1. Fast try top execution method candidates directly
        val quickCandidates = listOf("ExecuteADBCommand", "ExecuteAdbCommand", "ExecuteAdbCmd", "ExecuteCommand", "RunCmd", "RunSystemTestCmd")
        for (method in quickCandidates) {
            try {
                val (status, out) = executeViaMethod(method, "id -u", 2L)
                val trimmed = out.trim()
                if (status in 0..1 || trimmed == "0" || trimmed.contains("uid=0")) {
                    activeMethodName = method
                    connected = true
                    onLog("[+] Connected via execution method: $method (uid: $trimmed)")
                    break
                }
            } catch (e: StatusRuntimeException) {
                lastError = e
                if (e.status.code == io.grpc.Status.Code.UNIMPLEMENTED) {
                    continue
                } else {
                    throw e
                }
            } catch (e: Exception) {
                lastError = e
                break
            }
        }

        // 2. If direct candidates fail, run deep method discovery audit
        if (!connected) {
            onLog("[*] Direct method match failed. Starting auto-discovery audit on $host...")
            val discovered = auditMethodsAndServices(onLog)
            if (discovered != null) {
                activeMethodName = discovered
                connected = true
            }
        }

        if (!connected) {
            throw IllegalStateException("No compatible execution RPC method found on $host: ${lastError?.message ?: "Unimplemented"}")
        }
    }

    /**
     * Audits gRPC services and methods on the scooter without modifying vehicle state.
     * Discovers which service names are registered, queries vehicle info, and tests execution candidates.
     */
    suspend fun auditMethodsAndServices(onLog: (String) -> Unit): String? = withContext(Dispatchers.IO) {
        installResolver()
        if (channel == null) {
            val target = targetUri()
            channel = OkHttpChannelBuilder.forTarget(target)
                .usePlaintext()
                .maxInboundMessageSize(64 * 1024 * 1024)
                .build()
        }
        val ch = channel ?: return@withContext null

        onLog("--------------------------------------------------")
        onLog("[*] AUDITING GRPC DAEMON ON $host:$port")
        onLog("--------------------------------------------------")

        // Step A: Inspect registered services
        val candidateServices = listOf(
            "wifican.WifiCanRpc",
            "wifican.WifiCan",
            "wifican.Diagnostics",
            "diagnostics.DiagnosticsService",
            "diagnostics.Diagnostics",
            "ola.Diagnostics",
            "ola.WifiCanRpc",
            "grpc.reflection.v1alpha.ServerReflection",
            "grpc.reflection.v1.ServerReflection"
        )
        val activeServices = mutableListOf<String>()

        for (srv in candidateServices) {
            try {
                val desc = MethodDescriptor.newBuilder<ByteArray, ByteArray>()
                    .setType(MethodDescriptor.MethodType.UNARY)
                    .setFullMethodName(MethodDescriptor.generateFullMethodName(srv, "_probe_"))
                    .setRequestMarshaller(ByteArrayMarshaller)
                    .setResponseMarshaller(ByteArrayMarshaller)
                    .build()
                val call = ch.newCall(desc, CallOptions.DEFAULT.withDeadlineAfter(800, TimeUnit.MILLISECONDS))
                val resultHolder = CompletableResult<ByteArray>()
                call.start(object : ClientCall.Listener<ByteArray>() {
                    override fun onMessage(message: ByteArray) { resultHolder.complete(message) }
                    override fun onClose(status: io.grpc.Status, trailers: io.grpc.Metadata) {
                        if (!status.isOk && !resultHolder.isDone()) {
                            resultHolder.completeExceptionally(status.asRuntimeException())
                        }
                    }
                }, io.grpc.Metadata())
                call.request(1)
                call.sendMessage(ByteArray(0))
                call.halfClose()
                try {
                    resultHolder.await()
                    activeServices.add(srv)
                    onLog("  [+] Service REGISTERED: $srv")
                } catch (e: StatusRuntimeException) {
                    val descStr = e.status.description ?: ""
                    if (descStr.contains("unknown method", ignoreCase = true)) {
                        activeServices.add(srv)
                        onLog("  [+] Service REGISTERED: $srv")
                    } else if (e.status.code != io.grpc.Status.Code.UNIMPLEMENTED) {
                        activeServices.add(srv)
                        onLog("  [+] Service ACTIVE: $srv (${e.status.code})")
                    }
                }
            } catch (_: Exception) {}
        }

        if (!activeServices.contains("wifican.WifiCanRpc")) {
            activeServices.add(0, "wifican.WifiCanRpc")
        }

        // Step B: Query safe vehicle metadata
        onLog("\n[*] Probing diagnostic query RPCs:")
        val queryMethods = listOf(
            "GetVehicleOperationMode",
            "GetVehicleIdentifiers",
            "GetSwVersion",
            "GetAospVersion",
            "GetEcosDynoFlags",
            "CheckManifestFile",
            "InitConnection"
        )

        for (method in queryMethods) {
            try {
                val desc = buildMethodDescriptor(method, "wifican.WifiCanRpc")
                val call = ch.newCall(desc, CallOptions.DEFAULT.withDeadlineAfter(1200, TimeUnit.MILLISECONDS))
                val resultHolder = CompletableResult<ByteArray>()
                call.start(object : ClientCall.Listener<ByteArray>() {
                    override fun onMessage(message: ByteArray) { resultHolder.complete(message) }
                    override fun onClose(status: io.grpc.Status, trailers: io.grpc.Metadata) {
                        if (!status.isOk && !resultHolder.isDone()) {
                            resultHolder.completeExceptionally(status.asRuntimeException())
                        }
                    }
                }, io.grpc.Metadata())
                call.request(1)
                val payload = if (method == "InitConnection") ProtoUtils.buildCommandPayload("s1mods") else ByteArray(0)
                call.sendMessage(payload)
                call.halfClose()

                val resp = resultHolder.await()
                val (status, msg) = ProtoUtils.decodeResponse(resp)
                val cleanPreview = msg.filter { it.isLetterOrDigit() || it in " {}:,\"-_.=/" }.take(100)
                if (method == "GetVehicleIdentifiers" && msg.isNotBlank()) {
                    onLog("  • $method: AVAILABLE -> $cleanPreview")
                } else if (method == "GetSwVersion" && msg.isNotBlank()) {
                    detectedSoftwareVersion = msg
                    onLog("  • $method: AVAILABLE -> MoveOS: $msg")
                } else {
                    onLog("  • $method: AVAILABLE (status=$status${if (cleanPreview.isNotEmpty()) ", msg='$cleanPreview'" else ""})")
                }
            } catch (e: StatusRuntimeException) {
                if (e.status.code == io.grpc.Status.Code.UNIMPLEMENTED) {
                    onLog("  • $method: Not registered")
                } else {
                    onLog("  • $method: Responded (${e.status.code}: ${e.status.description})")
                }
            } catch (e: Exception) {
                onLog("  • $method: error: ${e.message}")
            }
        }

        // Step C: Test execution candidates across all active services
        onLog("\n[*] Scanning candidate execution RPC methods:")
        for (srv in activeServices) {
            for (method in candidateExecutionMethods) {
                try {
                    val (status, out) = executeViaMethod(method, "id -u", 1200L, srv)
                    val trimmed = out.trim()
                    if (status in 0..1 || trimmed == "0" || trimmed.contains("uid=0")) {
                        onLog("  [★] FOUND WORKING METHOD: $srv/$method (output: '$trimmed')")
                        activeServiceName = srv
                        activeMethodName = method
                        return@withContext method
                    } else if (out.isNotBlank()) {
                        onLog("  • $srv/$method: responded (status=$status, out='$trimmed')")
                    }
                } catch (e: StatusRuntimeException) {
                    if (e.status.code != io.grpc.Status.Code.UNIMPLEMENTED) {
                        onLog("  • $srv/$method: present (error: ${e.status.code})")
                    }
                } catch (_: Exception) {}
            }
        }

        // Step D: Try handshake with InitConnection then re-check top candidates
        onLog("\n[*] Testing session handshake (InitConnection)...")
        val handshakeOk = initConnection("s1mods", 2L)
        if (handshakeOk) {
            onLog("  [+] InitConnection handshake accepted! Re-testing execution candidates...")
            for (method in candidateExecutionMethods) {
                try {
                    val (status, out) = executeViaMethod(method, "id -u", 1200L)
                    val trimmed = out.trim()
                    if (status in 0..1 || trimmed == "0" || trimmed.contains("uid=0")) {
                        onLog("  [★] FOUND WORKING METHOD after handshake: $method (output: '$trimmed')")
                        activeMethodName = method
                        return@withContext method
                    }
                } catch (_: Exception) {}
            }
        } else {
            onLog("  • InitConnection not required or not present.")
        }

        onLog("\n[!] Audit complete. No execution method produced uid=0.")
        onLog("Tip: Tap 'Copy Log' and share this audit to reveal available methods.")
        null
    }

    /**
     * Sends an InitConnection handshake to wake/register the session (required on some MoveOS 3 builds).
     */
    private suspend fun initConnection(clientName: String = "s1mods", timeoutSeconds: Long = 3L): Boolean = withContext(Dispatchers.IO) {
        val ch = channel ?: return@withContext false
        try {
            val desc = buildMethodDescriptor("InitConnection")
            val call = ch.newCall(desc, CallOptions.DEFAULT.withDeadlineAfter(timeoutSeconds, TimeUnit.SECONDS))
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
            call.sendMessage(ProtoUtils.buildCommandPayload(clientName))
            call.halfClose()
            val resp = resultHolder.await()
            val (status, _) = ProtoUtils.decodeResponse(resp)
            status in 0..1
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Executes raw command string using a specific gRPC method name.
     */
    private suspend fun executeViaMethod(
        methodName: String,
        command: String,
        timeoutSeconds: Long = 30L,
        serviceName: String = activeServiceName
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val ch = channel ?: throw IllegalStateException("Channel not connected")
        val payload = ProtoUtils.buildCommandPayload(command)
        val desc = buildMethodDescriptor(methodName, serviceName)

        val call: ClientCall<ByteArray, ByteArray> = ch.newCall(
            desc,
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

        val responseBytes = resultHolder.await()
        ProtoUtils.decodeResponse(responseBytes)
    }

    /**
     * Executes raw command string on the scooter's daemon using the active method.
     */
    suspend fun executeRaw(command: String, timeoutSeconds: Long = 30L): Pair<Int, String> = withContext(Dispatchers.IO) {
        try {
            executeViaMethod(activeMethodName, command, timeoutSeconds)
        } catch (e: StatusRuntimeException) {
            if (e.status.code == io.grpc.Status.Code.UNIMPLEMENTED) {
                for (fallback in candidateExecutionMethods.filter { it != activeMethodName }) {
                    try {
                        val res = executeViaMethod(fallback, command, timeoutSeconds)
                        activeMethodName = fallback
                        return@withContext res
                    } catch (_: Exception) {}
                }
            }
            throw e
        }
    }

    /**
     * Executes arbitrary shell commands by wrapping in sh -c and replacing spaces with ${IFS}.
     */
    suspend fun shell(command: String, timeoutSeconds: Long = 30L): Pair<Int, String> {
        val wrapped = "sh -c " + command.replace(" ", "\${IFS}")
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
     * Wakes the display screen safely.
     */
    suspend fun wakeScreen() {
        shell("input keyevent 224", 10L)
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

    private class ScopedResolverProvider : NameResolverProvider() {
        override fun newNameResolver(targetUri: URI, args: NameResolver.Args): NameResolver? {
            if (SCHEME != targetUri.scheme) return null
            return ScopedResolver(targetUri)
        }

        override fun getDefaultScheme(): String = SCHEME
        override fun isAvailable(): Boolean = true
        override fun priority(): Int = 5
    }

    private class ScopedResolver(private val uri: URI) : NameResolver() {
        override fun getServiceAuthority(): String = "localhost"

        override fun start(listener: Listener2) {
            val path = uri.path?.replaceFirst("^/+".toRegex(), "") ?: ""
            val parts = path.split("/")
            if (parts.size != 2) {
                listener.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription("Invalid target: $uri"))
                return
            }
            try {
                val host = hexDecode(parts[0])
                val port = parts[1].toInt()
                val cleanHost = host.split("%")[0].trim('[', ']')
                val scope = if (host.contains("%")) {
                    host.split("%")[1].toIntOrNull()
                } else null

                val address = if (cleanHost.contains(":") && scope != null) {
                    Inet6Address.getByAddress(null, InetAddress.getByName(cleanHost).address, scope)
                } else {
                    InetAddress.getByName(cleanHost)
                }
                val socket = InetSocketAddress(address, port)
                val result = ResolutionResult.newBuilder()
                    .setAddresses(Collections.singletonList(EquivalentAddressGroup(socket)))
                    .setAttributes(Attributes.EMPTY)
                    .build()
                listener.onResult(result)
            } catch (e: Exception) {
                listener.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription("Cannot resolve $uri: ${e.message}"))
            }
        }

        override fun shutdown() {}
    }
}
