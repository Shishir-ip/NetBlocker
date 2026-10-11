package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.NetBlockerApp
import com.example.data.model.FirewallMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val app = context.applicationContext as? NetBlockerApp ?: return
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val activeRules = app.database.blockedRuleDao().getActiveBlockedRulesList()
                    if (activeRules.isNotEmpty()) {
                        app.firewallRepository.applyRules(activeRules, FirewallMode.AUTO)
                    }
                } catch (_: Exception) {
                    // Fail gracefully on boot
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
