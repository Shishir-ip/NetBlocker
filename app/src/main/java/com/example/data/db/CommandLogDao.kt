package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.data.model.CommandLog
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandLogDao {

    @Query("SELECT * FROM command_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<CommandLog>>

    @Insert
    suspend fun insertLog(log: CommandLog)

    @Query("DELETE FROM command_logs")
    suspend fun clearLogs()
}
