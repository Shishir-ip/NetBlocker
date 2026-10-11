package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_rules")
data class BlockedRule(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val uid: Int,
    val isBlocked: Boolean = false,
    val blockWifi: Boolean = true,
    val blockMobile: Boolean = true,
    val blockLan: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)
