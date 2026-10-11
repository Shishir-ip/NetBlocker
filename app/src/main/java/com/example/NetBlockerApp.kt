package com.example

import android.app.Application
import com.example.data.db.NetBlockerDatabase
import com.example.data.repository.AppRepository
import com.example.data.repository.FirewallRepository
import com.example.engine.NativeFirewallEngine
import com.example.executor.AdbHelperExecutor
import com.example.executor.RootExecutor
import com.example.executor.ShizukuExecutor

class NetBlockerApp : Application() {

    lateinit var database: NetBlockerDatabase
        private set

    lateinit var firewallEngine: NativeFirewallEngine
        private set

    lateinit var rootExecutor: RootExecutor
        private set

    lateinit var shizukuExecutor: ShizukuExecutor
        private set

    lateinit var adbHelperExecutor: AdbHelperExecutor
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
        rootExecutor = RootExecutor()
        shizukuExecutor = ShizukuExecutor(this)
        adbHelperExecutor = AdbHelperExecutor(this)

        // Ensure helper script is written to external files dir at startup
        adbHelperExecutor.generateStartScript()

        firewallRepository = FirewallRepository(
            commandLogDao = database.commandLogDao(),
            firewallEngine = firewallEngine,
            rootExecutor = rootExecutor,
            shizukuExecutor = shizukuExecutor,
            adbHelperExecutor = adbHelperExecutor
        )

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
