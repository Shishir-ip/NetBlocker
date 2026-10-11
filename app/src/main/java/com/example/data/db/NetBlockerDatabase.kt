package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.BlockedRule
import com.example.data.model.CommandLog

@Database(
    entities = [BlockedRule::class, CommandLog::class],
    version = 1,
    exportSchema = false
)
abstract class NetBlockerDatabase : RoomDatabase() {

    abstract fun blockedRuleDao(): BlockedRuleDao
    abstract fun commandLogDao(): CommandLogDao

    companion object {
        @Volatile
        private var INSTANCE: NetBlockerDatabase? = null

        fun getInstance(context: Context): NetBlockerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NetBlockerDatabase::class.java,
                    "netblocker_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
