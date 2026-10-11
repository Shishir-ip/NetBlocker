package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BlockedRule
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedRuleDao {

    @Query("SELECT * FROM blocked_rules")
    fun getAllRules(): Flow<List<BlockedRule>>

    @Query("SELECT * FROM blocked_rules WHERE isBlocked = 1")
    fun getActiveBlockedRules(): Flow<List<BlockedRule>>

    @Query("SELECT * FROM blocked_rules WHERE isBlocked = 1")
    suspend fun getActiveBlockedRulesList(): List<BlockedRule>

    @Query("SELECT * FROM blocked_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun getRuleByPackage(packageName: String): BlockedRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRule(rule: BlockedRule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRules(rules: List<BlockedRule>)

    @Query("DELETE FROM blocked_rules WHERE packageName = :packageName")
    suspend fun deleteRule(packageName: String)

    @Query("UPDATE blocked_rules SET isBlocked = 0")
    suspend fun unblockAll()

    @Query("UPDATE blocked_rules SET isBlocked = 1")
    suspend fun blockAll()
}
