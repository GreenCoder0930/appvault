package com.dacksec.appvault.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.dacksec.appvault.AppLockActivity
import com.dacksec.appvault.data.LockedAppsManager

class AppLockerService : AccessibilityService() {

    private lateinit var lockedAppsManager: LockedAppsManager
    private var lastLockedPackage: String? = null

    override fun onServiceConnected() {
        lockedAppsManager = LockedAppsManager(this)
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Ignore our own app and system UI
        if (packageName == "com.dacksec.appvault") {
            lastLockedPackage = null
            return
        }
        if (packageName == "com.android.systemui") return

        // Only show lock screen once per app session, not repeatedly
        if (packageName == lastLockedPackage) return

        if (lockedAppsManager.isLocked(packageName)) {
            lastLockedPackage = packageName
            val lockIntent = Intent(this, AppLockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(AppLockActivity.EXTRA_LOCKED_PACKAGE, packageName)
            }
            startActivity(lockIntent)
        }
    }

    override fun onInterrupt() {}
}
