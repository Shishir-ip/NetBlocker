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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FirewallStats(
    val totalApps: Int = 0,
    val blockedApps: Int = 0,
    val blockedWifi: Int = 0,
    val blockedMobile: Int = 0,
    val blockedLan: Int = 0
)

class NetBlockerViewModel(
    private val appRepository: AppRepository,
    private val firewallRepository: FirewallRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val selectedFilter = MutableStateFlow(FilterType.ALL)
    val firewallMode = MutableStateFlow(FirewallMode.AUTO)
    val isRootAvailable = MutableStateFlow(false)
    val isExecuting = MutableStateFlow(false)
    val pingStatus = MutableStateFlow<Boolean?>(null)
    val isPinging = MutableStateFlow(false)
    val bannerMessage = MutableStateFlow<String?>(null)

    val selectedAppForDetail = MutableStateFlow<AppItem?>(null)
    val showAdbScriptDialog = MutableStateFlow(false)
    val showLogsDialog = MutableStateFlow(false)
    val showModeDialog = MutableStateFlow(false)

    val recentLogs: StateFlow<List<CommandLog>> = firewallRepository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Raw stream of all apps combined with database blocked rules
    private val rawAppsFlow = appRepository.observeAppItems()

    // Filtered list based on search query and category tab
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

    // Stats calculated from all installed apps
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
        checkRootStatus()
    }

    fun checkRootStatus() {
        viewModelScope.launch {
            val root = firewallRepository.checkRoot()
            isRootAvailable.value = root
        }
    }

    fun toggleAppBlock(app: AppItem, isBlocked: Boolean) {
        viewModelScope.launch {
            appRepository.toggleAppBlock(app, isBlocked)
            // Auto re-apply rules
            applyActiveRules()
        }
    }

    fun updateAppNetworkToggles(
        app: AppItem,
        blockWifi: Boolean,
        blockMobile: Boolean,
        blockLan: Boolean
    ) {
        viewModelScope.launch {
            appRepository.updateAppNetworkToggles(app, blockWifi, blockMobile, blockLan)
            applyActiveRules()
        }
    }

    fun blockAllFiltered() {
        viewModelScope.launch {
            val currentList = filteredApps.value
            if (currentList.isNotEmpty()) {
                appRepository.blockAllApps(currentList)
                applyActiveRules()
                bannerMessage.value = "Blocked network access for ${currentList.size} apps."
            }
        }
    }

    fun unblockAll() {
        viewModelScope.launch {
            appRepository.unblockAllApps()
            firewallRepository.flushRules(asRoot = isRootAvailable.value)
            bannerMessage.value = "All applications unblocked."
        }
    }

    fun applyActiveRules() {
        viewModelScope.launch {
            isExecuting.value = true
            try {
                val rules = appRepository.getActiveBlockedRules()
                val result = firewallRepository.applyRules(rules, firewallMode.value)
                if (result.isSuccess) {
                    bannerMessage.value = "Firewall rules synced successfully."
                } else {
                    bannerMessage.value = "Rule sync note: ${result.output.take(80)}"
                }
            } catch (e: Exception) {
                bannerMessage.value = "Failed to sync: ${e.message}"
            } finally {
                isExecuting.value = false
            }
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

    fun getIptablesScript(): String {
        val rules = filteredApps.value.map { app ->
            BlockedRule(
                packageName = app.packageName,
                appName = app.appName,
                uid = app.uid,
                isBlocked = app.isBlocked,
                blockWifi = app.blockWifi,
                blockMobile = app.blockMobile,
                blockLan = app.blockLan
            )
        }
        return firewallRepository.getIptablesScript(rules)
    }

    fun getAdbScript(): String {
        val rules = filteredApps.value.map { app ->
            BlockedRule(
                packageName = app.packageName,
                appName = app.appName,
                uid = app.uid,
                isBlocked = app.isBlocked,
                blockWifi = app.blockWifi,
                blockMobile = app.blockMobile,
                blockLan = app.blockLan
            )
        }
        return firewallRepository.getAdbScript(rules)
    }

    fun getWindowsBatScript(): String {
        val rules = filteredApps.value.map { app ->
            BlockedRule(
                packageName = app.packageName,
                appName = app.appName,
                uid = app.uid,
                isBlocked = app.isBlocked,
                blockWifi = app.blockWifi,
                blockMobile = app.blockMobile,
                blockLan = app.blockLan
            )
        }
        return firewallRepository.getWindowsBatScript(rules)
    }

    fun getSingleAppAdbCommand(app: AppItem): String {
        return firewallRepository.getSingleAppAdbCommand(
            BlockedRule(
                packageName = app.packageName,
                appName = app.appName,
                uid = app.uid,
                isBlocked = app.isBlocked,
                blockWifi = app.blockWifi,
                blockMobile = app.blockMobile,
                blockLan = app.blockLan
            )
        )
    }

    fun getSingleAppRootCommand(app: AppItem): String {
        return firewallRepository.getSingleAppRootCommand(
            BlockedRule(
                packageName = app.packageName,
                appName = app.appName,
                uid = app.uid,
                isBlocked = app.isBlocked,
                blockWifi = app.blockWifi,
                blockMobile = app.blockMobile,
                blockLan = app.blockLan
            )
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
