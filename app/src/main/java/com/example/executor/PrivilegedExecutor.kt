package com.example.executor

data class ExecResult(
    val exitCode: Int,
    val output: String,
    val isSuccess: Boolean = (exitCode == 0)
)

interface PrivilegedExecutor {
    val name: String
    suspend fun isAvailable(): Boolean
    suspend fun exec(cmd: String): ExecResult
}
