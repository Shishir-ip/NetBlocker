package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.data.model.AppItem
import com.example.ui.theme.AllowedGreen
import com.example.ui.theme.BlockedRed
import com.example.ui.theme.LanPurple
import com.example.ui.theme.MobileOrange
import com.example.ui.theme.WifiBlue

@Composable
fun AppRowItem(
    app: AppItem,
    onToggleBlock: (Boolean) -> Unit,
    onToggleWifi: (Boolean) -> Unit,
    onToggleMobile: (Boolean) -> Unit,
    onToggleLan: (Boolean) -> Unit,
    onClickDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg by animateColorAsState(
        targetValue = if (app.isBlocked) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(250),
        label = "cardBg"
    )

    val borderStrokeColor by animateColorAsState(
        targetValue = if (app.isBlocked) {
            BlockedRed.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        },
        animationSpec = tween(250),
        label = "borderColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_item_${app.packageName}")
            .clickable(onClick = onClickDetail),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Icon, Name, UID badge, and Master Toggle Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon
                val bitmap = remember(app.icon) {
                    try {
                        app.icon?.toBitmap(84, 84, Bitmap.Config.ARGB_8888)
                    } catch (_: Exception) {
                        null
                    }
                }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = app.appName,
                            modifier = Modifier.size(38.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "App Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // App Info (Name, Package, UID badge)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (app.isSystemApp) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SYS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "UID: ${app.uid}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = app.packageName,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Master Block Toggle Switch
                Switch(
                    checked = app.isBlocked,
                    onCheckedChange = onToggleBlock,
                    modifier = Modifier.testTag("switch_block_${app.packageName}"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BlockedRed,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = AllowedGreen.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-row: Network Interface Granular Toggles (Wi-Fi, Mobile Data, LAN)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wi-Fi Chip
                NetworkToggleChip(
                    label = "Wi-Fi",
                    icon = Icons.Default.Wifi,
                    isBlocked = app.isBlocked && app.blockWifi,
                    tintColor = WifiBlue,
                    enabled = app.isBlocked,
                    onClick = { onToggleWifi(!app.blockWifi) },
                    testTag = "chip_wifi_${app.packageName}"
                )

                // Mobile Data Chip
                NetworkToggleChip(
                    label = "Mobile",
                    icon = Icons.Default.NetworkCell,
                    isBlocked = app.isBlocked && app.blockMobile,
                    tintColor = MobileOrange,
                    enabled = app.isBlocked,
                    onClick = { onToggleMobile(!app.blockMobile) },
                    testTag = "chip_mobile_${app.packageName}"
                )

                // LAN / Ethernet Chip
                NetworkToggleChip(
                    label = "LAN",
                    icon = Icons.Default.Lan,
                    isBlocked = app.isBlocked && app.blockLan,
                    tintColor = LanPurple,
                    enabled = app.isBlocked,
                    onClick = { onToggleLan(!app.blockLan) },
                    testTag = "chip_lan_${app.packageName}"
                )
            }
        }
    }
}

@Composable
private fun NetworkToggleChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isBlocked: Boolean,
    tintColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val bgColor = if (!enabled) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    } else if (isBlocked) {
        BlockedRed.copy(alpha = 0.18f)
    } else {
        tintColor.copy(alpha = 0.12f)
    }

    val activeColor = if (!enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    } else if (isBlocked) {
        BlockedRed
    } else {
        tintColor
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = if (isBlocked && enabled) BlockedRed.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isBlocked && enabled) Icons.Default.Block else icon,
                contentDescription = label,
                tint = activeColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isBlocked && enabled) "$label BLOCKED" else label,
                fontSize = 11.sp,
                fontWeight = if (isBlocked && enabled) FontWeight.Bold else FontWeight.Medium,
                color = activeColor
            )
        }
    }
}
