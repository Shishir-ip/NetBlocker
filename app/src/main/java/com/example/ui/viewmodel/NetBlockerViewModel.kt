package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.NetBlockerApp
import com.example.data.model.AppItem
import com.example.data.model.BlockedRule
import com.example.data.model.CommandLog
import com.example.data.model.FilterType
import com.example.data.model.FirewallMode
import com.example.data.repository.AppRepository
import com.example.data.repository.FirewallRepository
import com.example.engine.CapabilityLevel
import com.example.engine.ProbeResult
import com.example.executor.ShizukuState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FirewallStats(
    val totalApps: Int = 0,
    val blockedApps: Int = 0,
    val blockedWifi: Int = 0,
    val blockedMobile: Int = 0,
    val blockedLan: Int = 0
)

class NetBlockerViewModel(
    val appRepository: AppRepository,
    val firewallRepository: FirewallRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val selectedFilter = MutableStateFlow(FilterType.ALL)
    val firewallMode = MutableStateFlow(FirewallMode.AUTO)

    val activeExecutorName: StateFlow<String> = firewallRepository.activeExecutorName
    val probeResult: StateFlow<ProbeResult> = firewallRepository.currentProbe

    val isRootAvailable = MutableStateFlow(false)
    val shizukuState = MutableStateFlow(ShizukuState.NOT_INSTALLED)
    val isAdbHelperRunning = MutableStateFlow(false)

    val isExecuting = MutableStateFlow(false)
    val pingStatus = MutableStateFlow<Boolean?>(null)
    val isPinging = MutableStateFlow(false)
    val bannerMessage = MutableStateFlow<String?>(null)

    val selectedAppForDetail = MutableStateFlow<AppItem?>(null)
    val showSetupScreen = MutableStateFlow(false)
    val showAdbScriptDialog = MutableStateFlow(false)
    val showLogsDialog = MutableStateFlow(false)
    val showModeDialog = MutableStateFlow(false)

    val recentLogs: StateFlow<List<CommandLog>> = firewallRepository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val rawAppsFlow = appRepository.observeAppItems()

    val filteredApps: StateFlow<List<AppItem>> = combine(
        rawAppsFlow,
        searchQuery,
        selectedFilter
    ) { apps, query, filter ->
        apps.filter { app ->
            val matchesFilter = when (filter) {
                FilterType.ALL -> true
                FilterType.USER -> !app.isSystemApp
                FilterType.SYSTEM -> app.isSystemApp
                FilterType.BLOCKED -> app.isBlocked
            }

            val matchesQuery = query.isBlank() ||
                    app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true) ||
                    app.uid.toString().contains(query)

            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<FirewallStats> = rawAppsFlow.combine(MutableStateFlow(Unit)) { apps, _ ->
        val blockedList = apps.filter { it.isBlocked }
        FirewallStats(
            totalApps = apps.size,
            blockedApps = blockedList.size,
            blockedWifi = blockedList.count { it.blockWifi },
            blockedMobile = blockedList.count { it.blockMobile },
            blockedLan = blockedList.count { it.blockLan }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FirewallStats())

    init {
        refreshExecutorsAndProbe()
    }

    fun refreshExecutorsAndProbe() {
        viewModelScope.launch {
            isRootAvailable.value = firewallRepository.rootExecutor.isAvailable()
            shizukuState.value = firewallRepository.shizukuExecutor.getShizukuState()
            isAdbHelperRunning.value = firewallRepository.adbHelperExecutor.isAvailable()

            val resolved = firewallRepository.resolveExecutor(firewallMode.value)
            if (resolved == null && firewallMode.value != FirewallMode.SIMULATION) {
                // If nothing set up, suggest opening setup screen
                if (!isRootAvailable.value && shizukuState.value != ShizukuState.READY && !isAdbHelperRunning.value) {
                    bannerMessage.value = "Setup required: select Shizuku, PC ADB, or Root."
                }
            } else {
                val capability = firewallRepository.currentProbe.value.capabilityLevel
                if (capability == CapabilityLevel.PARTIAL) {
                    bannerMessage.value = "Notice: Running in partial mode (restricts background data only)."
                } else if (capability == CapabilityLevel.UNSUPPORTED) {
                    bannerMessage.value = "Warning: Current device does not support native non-root commands."
                }
            }
        }
    }

    fun toggleAppBlock(app: AppItem, isBlocked: Boolean) {
        viewModelScope.launch {
            isExecuting.value = true
            try {
                val verified = appRepository.toggleAppBlock(app, isBlocked, firewallMode.value)
                if (verified == isBlocked) {
                    bannerMessage.value = "${app.appName}: Verified ${if (verified) "Blocked" else "Allowed"}"
                } else {
                    bannerMessage.value = "${app.appName}: Rule mismatch (Requested: $isBlocked, Verified: $verified)"
                }
            } catch (e: Exception) {
                bannerMessage.value = "Error: ${e.message}"
            } finally {
                isExecuting.value = false
            }
        }
    }

    fun updateAppNetworkToggles(
        app: AppItem,
        blockWifi: Boolean,
        blockMobile: Boolean,
        blockLan: Boolean
    ) {
        viewModelScope.launch {
            isExecuting.value = true
            try {
                appRepository.updateAppNetworkToggles(app, blockWifi, blockMobile, blockLan, firewallMode.value)
                bannerMessage.value = "${app.appName}: Interface settings updated"
            } catch (e: Exception) {
                bannerMessage.value = "Error: ${e.message}"
            } finally {
                isExecuting.value = false
            }
        }
    }

    fun blockAllFiltered() {
        viewModelScope.launch {
            isExecuting.value = true
            val currentList = filteredApps.value
            if (currentList.isNotEmpty()) {
                appRepository.blockAllApps(currentList, firewallMode.value)
                bannerMessage.value = "Applied block to ${currentList.size} apps."
            }
            isExecuting.value = false
        }
    }

    fun unblockAll() {
        viewModelScope.launch {
            isExecuting.value = true
            appRepository.unblockAllApps(firewallMode.value)
            bannerMessage.value = "All applications unblocked."
            isExecuting.value = false
        }
    }

    fun syncActiveRules() {
        viewModelScope.launch {
            isExecuting.value = true
            try {
                val blockedApps = filteredApps.value.filter { it.isBlocked }
                firewallRepository.syncAllRules(blockedApps, firewallMode.value)
                bannerMessage.value = "Firewall rules synced and verified (${blockedApps.size} apps)."
            } catch (e: Exception) {
                bannerMessage.value = "Failed to sync: ${e.message}"
            } finally {
                isExecuting.value = false
            }
        }
    }

    fun requestShizukuPermission() {
        firewallRepository.shizukuExecutor.requestPermission()
        refreshExecutorsAndProbe()
    }

    fun restartAdbHelperViaShizuku() {
        viewModelScope.launch {
            val result = firewallRepository.adbHelperExecutor.restartViaShizuku(firewallRepository.shizukuExecutor)
            if (result.isSuccess) {
                bannerMessage.value = "Helper started via Shizuku!"
            } else {
                bannerMessage.value = "Helper start note: ${result.output}"
            }
            refreshExecutorsAndProbe()
        }
    }

    fun checkAdbHelper() {
        viewModelScope.launch {
            isAdbHelperRunning.value = firewallRepository.adbHelperExecutor.isAvailable()
            refreshExecutorsAndProbe()
        }
    }

    fun testConnectivity() {
        viewModelScope.launch {
            isPinging.value = true
            val success = firewallRepository.testGlobalConnectivity()
            pingStatus.value = success
            isPinging.value = false
        }
    }

    fun clearBanner() {
        bannerMessage.value = null
    }

    fun clearLogs() {
        viewModelScope.launch {
            firewallRepository.clearLogs()
        }
    }

    fun getFallbackIptablesScript(): String {
        return NetBlockerApp.instance.firewallEngine.buildFullScript(
            filteredApps.value.map {
                BlockedRule(it.packageName, it.appName, it.uid, it.isBlocked, it.blockWifi, it.blockMobile, it.blockLan)
            },
            firewallRepository.currentProbe.value.strategy
        )
    }

    companion object {
        fun provideFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = NetBlockerApp.instance
                return NetBlockerViewModel(
                    appRepository = app.appRepository,
                    firewallRepository = app.firewallRepository
                ) as T
            }
        }
    }
}
