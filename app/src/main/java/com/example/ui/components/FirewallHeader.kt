package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FirewallMode
import com.example.engine.CapabilityLevel
import com.example.engine.ProbeResult
import com.example.ui.theme.AllowedGreen
import com.example.ui.theme.BlockedRed
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.LanPurple
import com.example.ui.theme.MobileOrange
import com.example.ui.theme.WifiBlue
import com.example.ui.viewmodel.FirewallStats

@Composable
fun FirewallHeader(
    stats: FirewallStats,
    mode: FirewallMode,
    activeExecutorName: String,
    probeResult: ProbeResult,
    isExecuting: Boolean,
    isPinging: Boolean,
    pingStatus: Boolean?,
    onApplyRules: () -> Unit,
    onBlockAll: () -> Unit,
    onUnblockAll: () -> Unit,
    onTestPing: () -> Unit,
    onOpenSetup: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenModeSelector: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val isConfigured = (activeExecutorName != "None")
    val capability = probeResult.capabilityLevel

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("firewall_header_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (stats.blockedApps > 0) BlockedRed.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Shield Icon, Title, and Mode Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (stats.blockedApps > 0) BlockedRed.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security Shield",
                        tint = if (stats.blockedApps > 0) BlockedRed else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (stats.blockedApps > 0) "Firewall Active" else "NetBlocker Engine",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .alpha(pulseAlpha)
                                .background(if (stats.blockedApps > 0) BlockedRed else if (isConfigured) AllowedGreen else Color(0xFFFBBF24))
                        )
                    }

                    // Status and capability chip (Clickable -> Opens Setup)
                    Row(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onOpenSetup)
                            .testTag("status_capability_chip"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isConfigured) activeExecutorName else "Not set up (Tap to config)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isConfigured) CyanPrimary else BlockedRed
                        )
                        Text(
                            text = " • ",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = capability.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (capability) {
                                CapabilityLevel.FULL -> AllowedGreen
                                CapabilityLevel.PARTIAL -> Color(0xFFFBBF24)
                                CapabilityLevel.UNSUPPORTED -> BlockedRed
                            }
                        )
                    }
                }

                // Mode Selector Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onOpenModeSelector)
                        .testTag("button_mode_selector")
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SettingsSuggest,
                            contentDescription = "Mode",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = mode.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stat counters: Total Blocked, WiFi Blocked, Mobile Blocked, LAN Blocked
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatPill(
                    count = stats.blockedApps,
                    label = "Blocked",
                    color = BlockedRed,
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    count = stats.blockedWifi,
                    label = "Wi-Fi",
                    color = WifiBlue,
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    count = stats.blockedMobile,
                    label = "Mobile",
                    color = MobileOrange,
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    count = stats.blockedLan,
                    label = "LAN",
                    color = LanPurple,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Horizontal quick action pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionPill(
                    label = "Setup Wizard",
                    icon = Icons.Default.Build,
                    color = CyanPrimary,
                    onClick = onOpenSetup,
                    testTag = "pill_setup_wizard"
                )

                ActionPill(
                    label = "Sync & Verify",
                    icon = Icons.Default.ElectricBolt,
                    color = CyanPrimary,
                    isLoading = isExecuting,
                    onClick = onApplyRules,
                    testTag = "pill_apply_rules"
                )

                ActionPill(
                    label = "Block All",
                    icon = Icons.Default.Security,
                    color = BlockedRed,
                    onClick = onBlockAll,
                    testTag = "pill_block_all"
                )

                ActionPill(
                    label = "Unblock All",
                    icon = Icons.Default.RestartAlt,
                    color = AllowedGreen,
                    onClick = onUnblockAll,
                    testTag = "pill_unblock_all"
                )

                ActionPill(
                    label = "Command Logs",
                    icon = Icons.Default.Terminal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onOpenLogs,
                    testTag = "pill_command_logs"
                )

                ActionPill(
                    label = when {
                        isPinging -> "Testing..."
                        pingStatus == true -> "Connected"
                        pingStatus == false -> "Blocked / Off"
                        else -> "Test Ping"
                    },
                    icon = Icons.Default.WifiTethering,
                    color = if (pingStatus == true) AllowedGreen else if (pingStatus == false) BlockedRed else MaterialTheme.colorScheme.primary,
                    isLoading = isPinging,
                    onClick = onTestPing,
                    testTag = "pill_test_ping"
                )
            }
        }
    }
}

@Composable
private fun StatPill(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count.toString(),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionPill(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    testTag: String,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .clickable(enabled = !isLoading, onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(13.dp),
                    strokeWidth = 2.dp,
                    color = color
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}
