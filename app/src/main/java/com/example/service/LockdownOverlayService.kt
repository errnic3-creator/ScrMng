package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.example.R
import com.example.ScreenTimeApplication
import com.example.ui.lockdown.LockdownActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LockdownOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val repository by lazy { (application as ScreenTimeApplication).repository }

    private var currentPackage: String = ""
    private var currentAppName: String = ""
    private var currentReason: String = ""
    private var currentLockUntil: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        if (intent.action == ACTION_DISMISS) {
            removeOverlay()
            stopSelf()
            return START_NOT_STICKY
        }

        currentPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        currentAppName = intent.getStringExtra(EXTRA_APP_NAME) ?: "App"
        currentReason = intent.getStringExtra(EXTRA_REASON) ?: "Limit Exceeded"
        currentLockUntil = intent.getLongExtra(EXTRA_LOCK_UNTIL, 0L)

        showOverlay()

        return START_NOT_STICKY
    }

    private fun showOverlay() {
        if (windowManager == null) {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }

        if (overlayView != null) {
            updateOverlayContent()
            return
        }

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        try {
            // Launch LockdownActivity directly for seamless full-fidelity compose experience
            val activityIntent = Intent(this, LockdownActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(LockdownActivity.EXTRA_PACKAGE_NAME, currentPackage)
                putExtra(LockdownActivity.EXTRA_APP_NAME, currentAppName)
                putExtra(LockdownActivity.EXTRA_REASON, currentReason)
                putExtra(LockdownActivity.EXTRA_LOCK_UNTIL, currentLockUntil)
            }
            startActivity(activityIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateOverlayContent() {
        // Content refresh if view is persistent
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_REASON = "extra_reason"
        const val EXTRA_LOCK_UNTIL = "extra_lock_until"
        const val ACTION_DISMISS = "com.example.action.DISMISS_OVERLAY"

        fun show(context: Context, packageName: String, appName: String, reason: String, lockUntil: Long) {
            val intent = Intent(context, LockdownOverlayService::class.java).apply {
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_REASON, reason)
                putExtra(EXTRA_LOCK_UNTIL, lockUntil)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun dismiss(context: Context) {
            val intent = Intent(context, LockdownOverlayService::class.java).apply {
                action = ACTION_DISMISS
            }
            context.startService(intent)
        }
    }
}
