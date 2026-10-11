package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.executor.ShizukuState
import com.example.ui.theme.AllowedGreen
import com.example.ui.theme.BlockedRed
import com.example.ui.theme.CyanPrimary
import com.example.ui.viewmodel.NetBlockerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    viewModel: NetBlockerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val isRootAvailable by viewModel.isRootAvailable.collectAsState()
    val shizukuState by viewModel.shizukuState.collectAsState()
    val isAdbHelperRunning by viewModel.isAdbHelperRunning.collectAsState()
    val probeResult by viewModel.probeResult.collectAsState()

    val adbStartCommand = viewModel.firewallRepository.adbHelperExecutor.getStartCommand()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Privileged Setup",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Enable native non-root or root firewall controls",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("setup_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshExecutorsAndProbe() },
                        modifier = Modifier.testTag("setup_refresh_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh status")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Introduction banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyanPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Shield",
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Zero VPN Overhead",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "NetBlocker blocks network traffic natively at the OS level. Choose any ONE of the methods below to activate native permissions.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // CARD 1: I HAVE SHIZUKU (No PC needed)
            // ==========================================
            SetupCard(
                title = "1. I have Shizuku",
                subtitle = "On-device privileged shell service (No PC needed)",
                icon = Icons.Default.Smartphone,
                accentColor = CyanPrimary,
                isReady = (shizukuState == ShizukuState.READY),
                testTag = "card_setup_shizuku"
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    when (shizukuState) {
                        ShizukuState.NOT_INSTALLED -> {
                            StatusBadge(
                                status = "Shizuku Not Installed",
                                isGood = false,
                                message = "Install the free Shizuku app to enable on-device shell access without needing a PC."
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api")
                                    )
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("button_install_shizuku")
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Get Shizuku on Google Play")
                            }
                        }
                        ShizukuState.NOT_RUNNING -> {
                            StatusBadge(
                                status = "Shizuku Not Running",
                                isGood = false,
                                message = "Open Shizuku and start it via 'Wireless Debugging' (Android 11+) or connecting to a PC."
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val launchIntent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                    if (launchIntent != null) context.startActivity(launchIntent)
                                    else Toast.makeText(context, "Please open Shizuku app", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("button_open_shizuku")
                            ) {
                                Text("Open Shizuku App")
                            }
                        }
                        ShizukuState.PERMISSION_NOT_GRANTED -> {
                            StatusBadge(
                                status = "Permission Required",
                                isGood = false,
                                message = "Shizuku is running! Grant permission to let NetBlocker toggle network access."
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { viewModel.requestShizukuPermission() },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("button_grant_shizuku")
                            ) {
                                Text("Grant Shizuku Permission")
                            }
                        }
                        ShizukuState.READY -> {
                            StatusBadge(
                                status = "Shizuku Ready & Connected",
                                isGood = true,
                                message = "NetBlocker is connected to Shizuku UserService. Per-app controls apply instantly on device!"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // CARD 2: I HAVE A PC (ONE-TIME ADB)
            // ==========================================
            SetupCard(
                title = "2. I have a PC (One-Time ADB)",
                subtitle = "Launches local helper server with app_process (lasts until reboot)",
                icon = Icons.Default.Computer,
                accentColor = Color(0xFF60A5FA),
                isReady = isAdbHelperRunning,
                testTag = "card_setup_adb_helper"
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isAdbHelperRunning) {
                        StatusBadge(
                            status = "Helper Running",
                            isGood = true,
                            message = "The NetBlocker privileged helper is running in background. You can toggle apps without a PC until device reboots."
                        )
                    } else {
                        StatusBadge(
                            status = "Helper Stopped",
                            isGood = false,
                            message = "Run this ONE command once from your computer terminal. The helper will run until reboot."
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Single command box with copy button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = adbStartCommand,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = CyanPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                            .clickable {
                                                val clip = ClipData.newPlainText("ADB Command", adbStartCommand)
                                                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                                                Toast.makeText(context, "Command copied!", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                            .testTag("button_copy_adb_command")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copy Command", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CyanPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.checkAdbHelper() },
                            modifier = Modifier.weight(1f).testTag("button_check_helper"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Check Status")
                        }

                        if (shizukuState == ShizukuState.READY) {
                            Button(
                                onClick = { viewModel.restartAdbHelperViaShizuku() },
                                modifier = Modifier.weight(1f).testTag("button_start_helper_shizuku"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Start via Shizuku")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // CARD 3: I HAVE ROOT
            // ==========================================
            SetupCard(
                title = "3. I have Root",
                subtitle = "Direct kernel-level iptables control (Full + Per-interface switches)",
                icon = Icons.Default.Terminal,
                accentColor = Color(0xFFA855F7),
                isReady = isRootAvailable,
                testTag = "card_setup_root"
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isRootAvailable) {
                        StatusBadge(
                            status = "Root Privileges Active",
                            isGood = true,
                            message = "Superuser access granted. NetBlocker can block Wi-Fi, Mobile Data, and LAN separately."
                        )
                    } else {
                        StatusBadge(
                            status = "Root Not Detected",
                            isGood = false,
                            message = "Superuser binary was not found or permission was denied. If rooted, ensure NetBlocker is granted in Magisk/KernelSU."
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.refreshExecutorsAndProbe() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("button_request_root")
                        ) {
                            Text("Check Root Status")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Device Capability summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Device Capability Probe",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Current Level: ${probeResult.capabilityLevel.displayName}",
                        fontWeight = FontWeight.SemiBold,
                        color = if (probeResult.capabilityLevel == com.example.engine.CapabilityLevel.FULL) AllowedGreen
                        else if (probeResult.capabilityLevel == com.example.engine.CapabilityLevel.PARTIAL) Color(0xFFFBBF24)
                        else BlockedRed,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = probeResult.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SetupCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    isReady: Boolean,
    testTag: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isReady) AllowedGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        if (isReady) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Active",
                                tint = AllowedGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun StatusBadge(status: String, isGood: Boolean, message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isGood) AllowedGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .border(
                1.dp,
                if (isGood) AllowedGreen.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                RoundedCornerShape(10.dp)
            )
            .padding(10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isGood) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isGood) AllowedGreen else Color(0xFFFBBF24),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = status,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isGood) AllowedGreen else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
