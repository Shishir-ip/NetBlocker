package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_logs")
data class CommandLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val command: String,
    val exitCode: Int,
    val output: String,
    val mode: String,
    val success: Boolean = (exitCode == 0)
)
