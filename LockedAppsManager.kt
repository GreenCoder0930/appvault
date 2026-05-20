package com.dacksec.appvault.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: android.graphics.drawable.Drawable,
    val isLocked: Boolean
)

class LockedAppsManager(context: Context) {

    private val pm = context.packageManager

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "vault_locked_apps",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getInstalledApps(): List<AppInfo> {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        return pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != "com.dacksec.appvault" }
            .map { resolve ->
                val pkg = resolve.activityInfo.packageName
                AppInfo(
                    packageName = pkg,
                    label = resolve.loadLabel(pm).toString(),
                    icon = resolve.loadIcon(pm),
                    isLocked = isLocked(pkg)
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun isLocked(packageName: String): Boolean =
        prefs.getBoolean(packageName, false)

    fun setLocked(packageName: String, locked: Boolean) {
        prefs.edit().putBoolean(packageName, locked).apply()
    }

    fun getLockedPackages(): Set<String> =
        prefs.all.filterValues { it == true }.keys
}
