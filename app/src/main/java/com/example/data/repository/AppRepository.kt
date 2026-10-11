package com.example.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import com.example.data.db.BlockedRuleDao
import com.example.data.model.AppItem
import com.example.data.model.BlockedRule
import com.example.data.model.FirewallMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class AppRepository(
    private val context: Context,
    private val blockedRuleDao: BlockedRuleDao,
    private val firewallRepository: FirewallRepository
) {

    private val packageManager: PackageManager = context.packageManager

    // LRU cache for app icons to achieve 120fps smooth scrolling and low memory usage
    private val iconCache = LruCache<String, Drawable>(150)

    // In-memory cache of basic installed app metadata
    private var cachedInstalledApps: List<AppItem>? = null

    /**
     * Loads installed apps and continuously combines them with database BlockedRules.
     */
    fun observeAppItems(): Flow<List<AppItem>> {
        return blockedRuleDao.getAllRules().combine(getInstalledAppsFlow()) { rules, installedApps ->
            val ruleMap = rules.associateBy { it.packageName }
            installedApps.map { app ->
                val rule = ruleMap[app.packageName]
                if (rule != null) {
                    app.copy(
                        isBlocked = rule.isBlocked,
                        blockWifi = rule.blockWifi,
                        blockMobile = rule.blockMobile,
                        blockLan = rule.blockLan
                    )
                } else {
                    app
                }
            }
        }.flowOn(Dispatchers.Default)
    }

    private fun getInstalledAppsFlow(): Flow<List<AppItem>> = kotlinx.coroutines.flow.flow {
        val apps = getOrLoadInstalledApps()
        emit(apps)
    }

    suspend fun getOrLoadInstalledApps(forceRefresh: Boolean = false): List<AppItem> =
        withContext(Dispatchers.IO) {
            if (!forceRefresh && cachedInstalledApps != null) {
                return@withContext cachedInstalledApps!!
            }

            val installedPackages = try {
                packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            } catch (_: Exception) {
                emptyList()
            }

            val resultList = mutableListOf<AppItem>()

            for (pkg in installedPackages) {
                val appInfo = pkg.applicationInfo ?: continue
                // Exclude this NetBlocker app itself so user doesn't lock themselves out!
                if (pkg.packageName == context.packageName) continue

                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val hasInternet = pkg.requestedPermissions?.contains(android.Manifest.permission.INTERNET) == true

                val label = try {
                    appInfo.loadLabel(packageManager).toString()
                } catch (_: Exception) {
                    pkg.packageName
                }

                val icon = iconCache.get(pkg.packageName) ?: try {
                    val loaded = appInfo.loadIcon(packageManager)
                    iconCache.put(pkg.packageName, loaded)
                    loaded
                } catch (_: Exception) {
                    null
                }

                resultList.add(
                    AppItem(
                        packageName = pkg.packageName,
                        appName = label,
                        uid = appInfo.uid,
                        isSystemApp = isSystem,
                        hasInternetPermission = hasInternet,
                        icon = icon
                    )
                )
            }

            // Sort alphabetically by app name
            val sorted = resultList.sortedBy { it.appName.lowercase() }
            cachedInstalledApps = sorted
            sorted
        }

    /**
     * Toggles an app's block state, verifies real OS rule, and stores verified state.
     */
    suspend fun toggleAppBlock(app: AppItem, isBlocked: Boolean, mode: FirewallMode): Boolean =
        withContext(Dispatchers.IO) {
            val verifiedState = firewallRepository.applyAppBlockWithVerification(app, isBlocked, mode)

            val existingRule = blockedRuleDao.getRuleByPackage(app.packageName)
            val newRule = BlockedRule(
                packageName = app.packageName,
                appName = app.appName,
                uid = app.uid,
                isBlocked = verifiedState,
                blockWifi = existingRule?.blockWifi ?: true,
                blockMobile = existingRule?.blockMobile ?: true,
                blockLan = existingRule?.blockLan ?: true,
                lastUpdated = System.currentTimeMillis()
            )
            blockedRuleDao.upsertRule(newRule)
            verifiedState
        }

    suspend fun updateAppNetworkToggles(
        app: AppItem,
        blockWifi: Boolean,
        blockMobile: Boolean,
        blockLan: Boolean,
        mode: FirewallMode
    ) = withContext(Dispatchers.IO) {
        val updatedApp = app.copy(
            blockWifi = blockWifi,
            blockMobile = blockMobile,
            blockLan = blockLan
        )

        // If the app is currently blocked, re-apply the updated interface rules
        val verifiedState = if (app.isBlocked) {
            firewallRepository.applyAppBlockWithVerification(updatedApp, true, mode)
        } else {
            false
        }

        val newRule = BlockedRule(
            packageName = app.packageName,
            appName = app.appName,
            uid = app.uid,
            isBlocked = if (app.isBlocked) verifiedState else false,
            blockWifi = blockWifi,
            blockMobile = blockMobile,
            blockLan = blockLan,
            lastUpdated = System.currentTimeMillis()
        )
        blockedRuleDao.upsertRule(newRule)
    }

    suspend fun blockAllApps(apps: List<AppItem>, mode: FirewallMode) = withContext(Dispatchers.IO) {
        for (app in apps) {
            toggleAppBlock(app, true, mode)
        }
    }

    suspend fun unblockAllApps(mode: FirewallMode) = withContext(Dispatchers.IO) {
        val activeRules = blockedRuleDao.getActiveBlockedRulesList()
        for (rule in activeRules) {
            val dummyApp = AppItem(
                packageName = rule.packageName,
                appName = rule.appName,
                uid = rule.uid,
                isSystemApp = false,
                hasInternetPermission = true,
                isBlocked = true
            )
            firewallRepository.applyAppBlockWithVerification(dummyApp, false, mode)
        }
        blockedRuleDao.unblockAll()
    }

    suspend fun getActiveBlockedRules(): List<BlockedRule> = withContext(Dispatchers.IO) {
        blockedRuleDao.getActiveBlockedRulesList()
    }

    suspend fun clearIconCache() {
        iconCache.evictAll()
    }
}
