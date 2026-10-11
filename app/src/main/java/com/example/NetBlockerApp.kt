package com.example

import android.app.Application
import com.example.data.db.NetBlockerDatabase
import com.example.data.repository.AppRepository
import com.example.data.repository.FirewallRepository
import com.example.engine.NativeFirewallEngine

class NetBlockerApp : Application() {

    lateinit var database: NetBlockerDatabase
        private set

    lateinit var firewallEngine: NativeFirewallEngine
        private set

    lateinit var firewallRepository: FirewallRepository
        private set

    lateinit var appRepository: AppRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = NetBlockerDatabase.getInstance(this)
        firewallEngine = NativeFirewallEngine(this)
        firewallRepository = FirewallRepository(database.commandLogDao(), firewallEngine)
        appRepository = AppRepository(
            context = this,
            blockedRuleDao = database.blockedRuleDao(),
            firewallRepository = firewallRepository
        )
    }

    companion object {
        lateinit var instance: NetBlockerApp
            private set
    }
}
