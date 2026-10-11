package com.example.shizuku

import androidx.annotation.Keep
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

@Keep
class ShizukuUserService : IShizukuService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun execute(cmd: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errReader = BufferedReader(InputStreamReader(process.errorStream))
            val sb = StringBuilder()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.appendLine(line)
            }
            while (errReader.readLine().also { line = it } != null) {
                sb.appendLine("ERR: $line")
            }

            val exitCode = process.waitFor()
            "$exitCode\n${sb.toString().trim()}"
        } catch (e: Exception) {
            "-1\nExecution error: ${e.message ?: "Unknown"}"
        }
    }
}
