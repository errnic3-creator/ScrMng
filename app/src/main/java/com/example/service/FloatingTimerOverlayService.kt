package com.example.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.data.util.UsageStatsHelper

class FloatingTimerOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var timerTextView: TextView? = null
    private var iconImageView: ImageView? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_HIDE) {
            removeFloatingView()
            stopSelf()
            return START_NOT_STICKY
        }

        if (!UsageStatsHelper.hasOverlayPermission(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        val appName = intent?.getStringExtra(EXTRA_APP_NAME) ?: "App"
        val packageName = intent?.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val remainingMinutes = intent?.getIntExtra(EXTRA_REMAINING_MINUTES, 0) ?: 0
        val isWarning = intent?.getBooleanExtra(EXTRA_IS_WARNING, false) ?: false

        if (floatingView == null) {
            createFloatingView()
        }

        // Update timer pill content
        val prefix = if (isWarning) "⚠️ " else "⏳ "
        timerTextView?.text = "$prefix$remainingMinutes min left"
        val pillBg = GradientDrawable().apply {
            cornerRadius = 40f
            if (remainingMinutes <= 5) {
                setColor(Color.parseColor("#E11D48")) // Crimson / Red
            } else if (remainingMinutes <= 15 || isWarning) {
                setColor(Color.parseColor("#D97706")) // Amber
            } else {
                setColor(Color.parseColor("#0F172A")) // Dark Slate
            }
            setStroke(2, Color.parseColor("#334155"))
        }
        floatingView?.background = pillBg

        // Load app icon
        try {
            val iconDrawable = packageManager.getApplicationIcon(packageName)
            iconImageView?.setImageDrawable(iconDrawable)
        } catch (e: Exception) {
            iconImageView?.setImageResource(android.R.drawable.ic_lock_idle_lock)
        }

        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingView() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 180
        }

        val linearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 16, 28, 16)
            elevation = 16f
        }

        val iconView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(48, 48).apply {
                marginEnd = 16
            }
        }
        iconImageView = iconView

        val textView = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            text = "⏳ Screen Time"
        }
        timerTextView = textView

        linearLayout.addView(iconView)
        linearLayout.addView(textView)

        // Make draggable
        linearLayout.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    try {
                        windowManager?.updateViewLayout(view, params)
                    } catch (e: Exception) {
                        // ignore
                    }
                    true
                }
                else -> false
            }
        }

        floatingView = linearLayout

        try {
            windowManager?.addView(floatingView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeFloatingView() {
        floatingView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            floatingView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatingView()
    }

    companion object {
        const val ACTION_SHOW_OR_UPDATE = "action_show_or_update"
        const val ACTION_HIDE = "action_hide"

        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_REMAINING_MINUTES = "extra_remaining_minutes"
        const val EXTRA_IS_WARNING = "extra_is_warning"

        fun update(context: Context, appName: String, packageName: String, remainingMinutes: Int, isWarning: Boolean) {
            val intent = Intent(context, FloatingTimerOverlayService::class.java).apply {
                action = ACTION_SHOW_OR_UPDATE
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_REMAINING_MINUTES, remainingMinutes)
                putExtra(EXTRA_IS_WARNING, isWarning)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun hide(context: Context) {
            val intent = Intent(context, FloatingTimerOverlayService::class.java).apply {
                action = ACTION_HIDE
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
