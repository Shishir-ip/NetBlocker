package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppItem
import com.example.ui.theme.AllowedGreen
import com.example.ui.theme.BlockedRed
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.LanPurple
import com.example.ui.theme.MobileOrange
import com.example.ui.theme.WifiBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsBottomSheet(
    app: AppItem,
    onDismiss: () -> Unit,
    onToggleBlock: (Boolean) -> Unit,
    onToggleWifi: (Boolean) -> Unit,
    onToggleMobile: (Boolean) -> Unit,
    onToggleLan: (Boolean) -> Unit,
    singleAppAdbCommand: String,
    singleAppRootCommand: String,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("app_details_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // App Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "App Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = app.packageName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BadgePill("UID: ${app.uid}", CyanPrimary)
                        if (app.isSystemApp) {
                            BadgePill("System App", MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            BadgePill("User App", AllowedGreen)
                        }
                        if (app.hasInternetPermission) {
                            BadgePill("Internet Permission", WifiBlue)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            // Master Block Toggle Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (app.isBlocked) BlockedRed.copy(alpha = 0.12f)
                    else AllowedGreen.copy(alpha = 0.12f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (app.isBlocked) BlockedRed.copy(alpha = 0.4f) else AllowedGreen.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (app.isBlocked) "Network Blocked" else "Network Allowed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (app.isBlocked) BlockedRed else AllowedGreen
                        )
                        Text(
                            text = if (app.isBlocked) "Firewall is dropping outbound traffic" else "Normal internet connectivity enabled",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = app.isBlocked,
                        onCheckedChange = onToggleBlock,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BlockedRed,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = AllowedGreen.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("modal_switch_block")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Granular Interface Controls
            Text(
                text = "Network Interface Controls",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose specifically which connection types to drop for this application:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            InterfaceToggleRow(
                title = "Wi-Fi Network (wlan+)",
                subtitle = "Blocks wireless router / hotspot connections",
                icon = Icons.Default.Wifi,
                accentColor = WifiBlue,
                isBlocked = app.blockWifi,
                enabled = app.isBlocked,
                onToggle = { onToggleWifi(!app.blockWifi) },
                testTag = "detail_toggle_wifi"
            )

            Spacer(modifier = Modifier.height(8.dp))

            InterfaceToggleRow(
                title = "Mobile Cellular Data (rmnet+)",
                subtitle = "Blocks LTE / 5G / 3G mobile operator data",
                icon = Icons.Default.NetworkCell,
                accentColor = MobileOrange,
                isBlocked = app.blockMobile,
                enabled = app.isBlocked,
                onToggle = { onToggleMobile(!app.blockMobile) },
                testTag = "detail_toggle_mobile"
            )

            Spacer(modifier = Modifier.height(8.dp))

            InterfaceToggleRow(
                title = "LAN / Ethernet (eth+)",
                subtitle = "Blocks USB ethernet / docking station interfaces",
                icon = Icons.Default.Lan,
                accentColor = LanPurple,
                isBlocked = app.blockLan,
                enabled = app.isBlocked,
                onToggle = { onToggleLan(!app.blockLan) },
                testTag = "detail_toggle_lan"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Direct ADB Command Box for this app
            Text(
                text = "Native ADB Shell Command",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Non-root ADB execution command to apply policy directly via terminal:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            CommandCopyBox(
                command = singleAppAdbCommand,
                label = "Copy ADB Command",
                onCopy = {
                    copyToClipboard(context, singleAppAdbCommand, "ADB Command copied")
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Direct Root iptables Command Box
            Text(
                text = "Root iptables Command",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            CommandCopyBox(
                command = singleAppRootCommand,
                label = "Copy iptables Command",
                onCopy = {
                    copyToClipboard(context, singleAppRootCommand, "iptables Command copied")
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("button_done_bottom_sheet"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InterfaceToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isBlocked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onToggle)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (!enabled) MaterialTheme.colorScheme.surfaceVariant
                        else if (isBlocked) BlockedRed.copy(alpha = 0.2f)
                        else AllowedGreen.copy(alpha = 0.2f)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (!enabled) "Inactive" else if (isBlocked) "BLOCKED" else "ALLOWED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (!enabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else if (isBlocked) BlockedRed
                    else AllowedGreen
                )
            }
        }
    }
}

@Composable
private fun CommandCopyBox(
    command: String,
    label: String,
    onCopy: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = command,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = CyanPrimary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .clickable(onClick = onCopy)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = label,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgePill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

private fun copyToClipboard(context: Context, text: String, message: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("NetBlocker Command", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
