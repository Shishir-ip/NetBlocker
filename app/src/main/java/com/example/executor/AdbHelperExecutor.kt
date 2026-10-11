package com.example.executor

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.Socket
import java.util.UUID

class AdbHelperExecutor(private val context: Context) : PrivilegedExecutor {

    override val name: String = "ADB Helper Server"

    companion object {
        const val HELPER_PORT = 19527
        private const val PREFS_NAME = "netblocker_helper_prefs"
        private const val KEY_TOKEN = "secret_token"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getToken(): String {
        var token = prefs.getString(KEY_TOKEN, null)
        if (token.isNullOrEmpty()) {
            token = UUID.randomUUID().toString().replace("-", "")
            prefs.edit().putString(KEY_TOKEN, token).apply()
        }
        return token
    }

    fun getStartScriptFile(): File {
        val filesDir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(filesDir, "start.sh")
    }

    fun getStartCommand(): String {
        val script = generateStartScript()
        // Provide standard /sdcard/Android/data/... path for easy adb execution
        val relativePath = "/sdcard/Android/data/${context.packageName}/files/start.sh"
        return "adb shell sh $relativePath"
    }

    fun generateStartScript(): File {
        val scriptFile = getStartScriptFile()
        val apkPath = context.applicationInfo.sourceDir
        val token = getToken()

        val content = """
            #!/system/bin/sh
            # NetBlocker One-Time Privileged Helper Server Starter
            PORT=$HELPER_PORT
            TOKEN="$token"
            APK_PATH="$apkPath"

            echo "[NetBlocker] Stopping any previous helper instances..."
            pkill -f "netblocker_server" 2>/dev/null || true
            sleep 1

            echo "[NetBlocker] Launching NetBlocker privileged helper in background..."
            export CLASSPATH="${'$'}APK_PATH"
            nohup app_process /system/bin --nice-name=netblocker_server com.example.server.AdbHelperServer "${'$'}PORT" "${'$'}TOKEN" >/dev/null 2>&1 &
            sleep 1

            echo "[NetBlocker] Helper initialized successfully! You can now close this terminal."
        """.trimIndent()

        try {
            scriptFile.writeText(content)
            scriptFile.setReadable(true, false)
            scriptFile.setExecutable(true, false)
        } catch (_: Exception) {}

        return scriptFile
    }

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        val token = getToken()
        try {
            Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(InetAddress.getByName("127.0.0.1"), HELPER_PORT), 800)
                socket.soTimeout = 1500
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                writer.println("TOKEN $token")
                val authRes = reader.readLine()
                if (authRes != "AUTH_OK") return@withContext false

                writer.println("PING")
                val pongRes = reader.readLine()
                return@withContext (pongRes == "PONG")
            }
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun exec(cmd: String): ExecResult = withContext(Dispatchers.IO) {
        val token = getToken()
        try {
            Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(InetAddress.getByName("127.0.0.1"), HELPER_PORT), 1500)
                socket.soTimeout = 8000
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                writer.println("TOKEN $token")
                val authRes = reader.readLine()
                if (authRes != "AUTH_OK") {
                    return@withContext ExecResult(-1, "Helper authentication failed: $authRes")
                }

                writer.println("EXEC $cmd")

                // Read output protocol:
                // EXIT <codeLines>
                // BEGIN_OUTPUT
                // <lines>
                // END_OUTPUT
                val exitLine = reader.readLine() ?: return@withContext ExecResult(-1, "No response from helper")
                val exitCode = if (exitLine.startsWith("EXIT ")) {
                    exitLine.removePrefix("EXIT ").trim().toIntOrNull() ?: -1
                } else -1

                val beginLine = reader.readLine()
                val outputBuilder = StringBuilder()
                if (beginLine == "BEGIN_OUTPUT") {
                    var outLine: String?
                    while (reader.readLine().also { outLine = it } != null) {
                        if (outLine == "END_OUTPUT") break
                        outputBuilder.appendLine(outLine)
                    }
                }

                writer.println("QUIT")
                ExecResult(exitCode = exitCode, output = outputBuilder.toString().trim())
            }
        } catch (e: Exception) {
            ExecResult(-1, "Failed to connect to helper server on port $HELPER_PORT: ${e.message}")
        }
    }

    suspend fun restartViaShizuku(shizukuExecutor: ShizukuExecutor): ExecResult = withContext(Dispatchers.IO) {
        val script = generateStartScript()
        val cmd = "sh ${script.absolutePath}"
        shizukuExecutor.exec(cmd)
    }
}
