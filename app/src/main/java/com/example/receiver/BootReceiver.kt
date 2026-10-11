package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.NetBlockerApp
import com.example.R
import com.example.data.model.AppItem
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
                        val executor = app.firewallRepository.resolveExecutor(FirewallMode.AUTO)
                        if (executor != null) {
                            // Executor available (e.g. Root or background-running service)
                            val appsToSync = activeRules.map { rule ->
                                AppItem(
                                    packageName = rule.packageName,
                                    appName = rule.appName,
                                    uid = rule.uid,
                                    isSystemApp = false,
                                    hasInternetPermission = true,
                                    isBlocked = rule.isBlocked,
                                    blockWifi = rule.blockWifi,
                                    blockMobile = rule.blockMobile,
                                    blockLan = rule.blockLan
                                )
                            }
                            app.firewallRepository.syncAllRules(appsToSync, FirewallMode.AUTO)
                        } else {
                            // No executor available yet -> post notification asking user to open NetBlocker
                            postRestoreNotification(context, activeRules.size)
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun postRestoreNotification(context: Context, blockedCount: Int) {
        val channelId = "netblocker_alerts"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "NetBlocker Firewall Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when firewall rules need re-application"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("NetBlocker: Action Required")
            .setContentText("Open NetBlocker to restore rules ($blockedCount apps blocked).")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(1001, notification)
    }
}
