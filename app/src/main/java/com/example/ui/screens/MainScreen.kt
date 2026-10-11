package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FilterType
import com.example.data.model.FirewallMode
import com.example.ui.components.AdbScriptDialog
import com.example.ui.components.AppDetailsBottomSheet
import com.example.ui.components.AppRowItem
import com.example.ui.components.CommandLogsDialog
import com.example.ui.components.FirewallHeader
import com.example.ui.components.ModeSelectorDialog
import com.example.ui.theme.BlockedRed
import com.example.ui.theme.CyanPrimary
import com.example.ui.viewmodel.NetBlockerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: NetBlockerViewModel,
    modifier: Modifier = Modifier
) {
    val showSetupScreen by viewModel.showSetupScreen.collectAsState()

    if (showSetupScreen) {
        SetupScreen(
            viewModel = viewModel,
            onBack = { viewModel.showSetupScreen.value = false },
            modifier = modifier
        )
        return
    }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val firewallMode by viewModel.firewallMode.collectAsState()
    val activeExecutorName by viewModel.activeExecutorName.collectAsState()
    val probeResult by viewModel.probeResult.collectAsState()

    val isExecuting by viewModel.isExecuting.collectAsState()
    val isPinging by viewModel.isPinging.collectAsState()
    val pingStatus by viewModel.pingStatus.collectAsState()
    val bannerMessage by viewModel.bannerMessage.collectAsState()

    val apps by viewModel.filteredApps.collectAsState()
    val stats by viewModel.stats.collectAsState()

    val selectedAppForDetail by viewModel.selectedAppForDetail.collectAsState()
    val showAdbScriptDialog by viewModel.showAdbScriptDialog.collectAsState()
    val showLogsDialog by viewModel.showLogsDialog.collectAsState()
    val showModeDialog by viewModel.showModeDialog.collectAsState()

    val listState = rememberLazyListState()
    val isPerInterfaceSupported = (activeExecutorName.contains("Root", ignoreCase = true) || firewallMode == FirewallMode.ROOT)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyanPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "NetBlocker Logo",
                                tint = CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "NetBlocker",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Native Non-VPN Firewall",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.showSetupScreen.value = true },
                        modifier = Modifier.testTag("topbar_setup_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Setup Privileged Access",
                            tint = CyanPrimary
                        )
                    }

                    if (stats.blockedApps > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(BlockedRed.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${stats.blockedApps} Blocked",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlockedRed
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.syncActiveRules() },
                icon = {
                    if (isExecuting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = "Sync & Verify Rules"
                        )
                    }
                },
                text = { Text("Sync Rules") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .testTag("fab_sync_rules")
                    .navigationBarsPadding()
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Animated Banner Message
            AnimatedVisibility(
                visible = bannerMessage != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                bannerMessage?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyanPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = msg,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = CyanPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearBanner() },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Dismiss",
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar & Filter Chips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search by name, package, or UID...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_text_field"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Category Chips (All, User Apps, System, Blocked Only)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterType.entries.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectedFilter.value = filter },
                            label = {
                                Text(
                                    text = filter.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = CyanPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                selectedBorderColor = CyanPrimary
                            ),
                            modifier = Modifier.testTag("filter_chip_${filter.name}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Lazy Column with Header and App List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("apps_lazy_column"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Firewall Header at the top of scroll list
                item(key = "firewall_header") {
                    FirewallHeader(
                        stats = stats,
                        mode = firewallMode,
                        activeExecutorName = activeExecutorName,
                        probeResult = probeResult,
                        isExecuting = isExecuting,
                        isPinging = isPinging,
                        pingStatus = pingStatus,
                        onApplyRules = { viewModel.syncActiveRules() },
                        onBlockAll = { viewModel.blockAllFiltered() },
                        onUnblockAll = { viewModel.unblockAll() },
                        onTestPing = { viewModel.testConnectivity() },
                        onOpenSetup = { viewModel.showSetupScreen.value = true },
                        onOpenLogs = { viewModel.showLogsDialog.value = true },
                        onOpenModeSelector = { viewModel.showModeDialog.value = true }
                    )
                }

                if (apps.isEmpty()) {
                    item(key = "empty_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Empty",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "No matching apps found." else "Loading installed apps...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = apps,
                        key = { it.packageName }
                    ) { app ->
                        AppRowItem(
                            app = app,
                            isPerInterfaceSupported = isPerInterfaceSupported,
                            onToggleBlock = { isBlocked ->
                                viewModel.toggleAppBlock(app, isBlocked)
                            },
                            onToggleWifi = { wifiBlocked ->
                                viewModel.updateAppNetworkToggles(
                                    app = app,
                                    blockWifi = wifiBlocked,
                                    blockMobile = app.blockMobile,
                                    blockLan = app.blockLan
                                )
                            },
                            onToggleMobile = { mobileBlocked ->
                                viewModel.updateAppNetworkToggles(
                                    app = app,
                                    blockWifi = app.blockWifi,
                                    blockMobile = mobileBlocked,
                                    blockLan = app.blockLan
                                )
                            },
                            onToggleLan = { lanBlocked ->
                                viewModel.updateAppNetworkToggles(
                                    app = app,
                                    blockWifi = app.blockWifi,
                                    blockMobile = app.blockMobile,
                                    blockLan = lanBlocked
                                )
                            },
                            onClickDetail = {
                                viewModel.selectedAppForDetail.value = app
                            }
                        )
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    // App Details Bottom Sheet
    selectedAppForDetail?.let { app ->
        AppDetailsBottomSheet(
            app = app,
            isPerInterfaceSupported = isPerInterfaceSupported,
            activeStrategyDescription = probeResult.description,
            onDismiss = { viewModel.selectedAppForDetail.value = null },
            onToggleBlock = { isBlocked ->
                viewModel.toggleAppBlock(app, isBlocked)
                viewModel.selectedAppForDetail.value = app.copy(isBlocked = isBlocked)
            },
            onToggleWifi = { wifiBlocked ->
                viewModel.updateAppNetworkToggles(app, wifiBlocked, app.blockMobile, app.blockLan)
                viewModel.selectedAppForDetail.value = app.copy(blockWifi = wifiBlocked)
            },
            onToggleMobile = { mobileBlocked ->
                viewModel.updateAppNetworkToggles(app, app.blockWifi, mobileBlocked, app.blockLan)
                viewModel.selectedAppForDetail.value = app.copy(blockMobile = mobileBlocked)
            },
            onToggleLan = { lanBlocked ->
                viewModel.updateAppNetworkToggles(app, app.blockWifi, app.blockMobile, lanBlocked)
                viewModel.selectedAppForDetail.value = app.copy(blockLan = lanBlocked)
            },
            singleAppNativeCommand = com.example.NetBlockerApp.instance.firewallEngine.buildBlockCommand(
                app,
                probeResult.strategy
            )
        )
    }

    // Fallback ADB Script Export Dialog (kept only as last resort)
    if (showAdbScriptDialog) {
        val script = viewModel.getFallbackIptablesScript()
        AdbScriptDialog(
            bashScript = script,
            windowsScript = script,
            iptablesScript = script,
            onDismiss = { viewModel.showAdbScriptDialog.value = false }
        )
    }

    // Command Logs Audit Dialog
    if (showLogsDialog) {
        val logs by viewModel.recentLogs.collectAsState()
        CommandLogsDialog(
            logs = logs,
            onClearLogs = { viewModel.clearLogs() },
            onDismiss = { viewModel.showLogsDialog.value = false }
        )
    }

    // Mode Selector Dialog
    if (showModeDialog) {
        ModeSelectorDialog(
            currentMode = firewallMode,
            isRootAvailable = viewModel.isRootAvailable.collectAsState().value,
            onSelectMode = { newMode ->
                viewModel.firewallMode.value = newMode
                viewModel.refreshExecutorsAndProbe()
            },
            onDismiss = { viewModel.showModeDialog.value = false }
        )
    }
}
