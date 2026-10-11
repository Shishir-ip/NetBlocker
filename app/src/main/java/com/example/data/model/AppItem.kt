package com.example.data.model

import android.graphics.drawable.Drawable

data class AppItem(
    val packageName: String,
    val appName: String,
    val uid: Int,
    val isSystemApp: Boolean,
    val hasInternetPermission: Boolean,
    val isBlocked: Boolean = false,
    val blockWifi: Boolean = true,
    val blockMobile: Boolean = true,
    val blockLan: Boolean = true,
    val icon: Drawable? = null
) {
    val isAnyNetworkBlocked: Boolean
        get() = isBlocked && (blockWifi || blockMobile || blockLan)

    val activeBlockCount: Int
        get() = if (!isBlocked) 0 else {
            var count = 0
            if (blockWifi) count++
            if (blockMobile) count++
            if (blockLan) count++
            count
        }
}
