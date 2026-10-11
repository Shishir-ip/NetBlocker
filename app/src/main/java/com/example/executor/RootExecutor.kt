package com.example.executor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class RootExecutor : PrivilegedExecutor {

    override val name: String = "Root (su)"

    private var cachedAvailable: Boolean? = null

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        val suPaths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su"
        )
        val suBinaryExists = suPaths.any { File(it).exists() }
        if (!suBinaryExists) {
            cachedAvailable = false
            return@withContext false
        }

        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val exitCode = process.waitFor()
            val available = (exitCode == 0)
            cachedAvailable = available
            available
        } catch (_: Exception) {
            cachedAvailable = false
            false
        }
    }

    override suspend fun exec(cmd: String): ExecResult = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errReader = BufferedReader(InputStreamReader(process.errorStream))
            val outputBuilder = StringBuilder()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                outputBuilder.appendLine(line)
            }
            while (errReader.readLine().also { line = it } != null) {
                outputBuilder.appendLine("ERR: $line")
            }

            val exitCode = process.waitFor()
            ExecResult(
                exitCode = exitCode,
                output = outputBuilder.toString().trim()
            )
        } catch (e: Exception) {
            ExecResult(
                exitCode = -1,
                output = "Root execution error: ${e.message ?: "Unknown error"}"
            )
        }
    }
}
