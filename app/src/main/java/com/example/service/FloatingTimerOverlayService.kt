package com.example.service

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.ScreenTimeApplication
import com.example.data.util.UsageStatsHelper

class FloatingTimerOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var timerTextView: TextView? = null
    private var iconImageView: ImageView? = null
    private var lastAnimatedMinute: Long = -1L
    private var pulseAnimator: ValueAnimator? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundWithNotification()
    }

    private fun startForegroundWithNotification() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, ScreenTimeApplication.CHANNEL_MONITOR_SERVICE)
            .setContentTitle("ScrMngr Floating Timer")
            .setContentText("Active screen time countdown indicator")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_HIDE) {
            removeFloatingView()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        if (!UsageStatsHelper.hasOverlayPermission(this)) {
            removeFloatingView()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val appName = intent?.getStringExtra(EXTRA_APP_NAME) ?: "App"
        val packageName = intent?.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val remainingSeconds = intent?.getLongExtra(EXTRA_REMAINING_SECONDS, -1L) ?: -1L
        val remainingMinutes = intent?.getIntExtra(EXTRA_REMAINING_MINUTES, 0) ?: 0
        val isWarning = intent?.getBooleanExtra(EXTRA_IS_WARNING, false) ?: false

        val effectiveSeconds = if (remainingSeconds >= 0L) remainingSeconds else (remainingMinutes * 60L)

        if (floatingView == null) {
            createFloatingView()
        }

        // Format dynamic countdown timer: HH:MM:SS for >= 60m, MM:SS for < 60m
        val formattedTime = if (effectiveSeconds >= 3600L) {
            val hours = effectiveSeconds / 3600L
            val minutes = (effectiveSeconds % 3600L) / 60L
            val seconds = effectiveSeconds % 60L
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            val minutes = effectiveSeconds / 60L
            val seconds = effectiveSeconds % 60L
            String.format("%02d:%02d", minutes, seconds)
        }

        val prefix = if (isWarning || effectiveSeconds <= 300L) "⚠️ " else "⏳ "
        timerTextView?.text = "$prefix$formattedTime"

        val baseColor = if (effectiveSeconds <= 300L) {
            Color.parseColor("#E11D48") // Crimson / Red for <= 5m
        } else if (effectiveSeconds <= 900L || isWarning) {
            Color.parseColor("#D97706") // Amber for <= 15m
        } else {
            Color.parseColor("#0F172A") // Dark Slate
        }

        val pillBg = GradientDrawable().apply {
            cornerRadius = 40f
            setColor(baseColor)
            setStroke(2, Color.parseColor("#334155"))
        }
        floatingView?.background = pillBg

        // Minute-interval visual red pulse animation every 60 seconds
        val currentMinute = effectiveSeconds / 60L
        if (effectiveSeconds % 60L in 0L..1L && lastAnimatedMinute != currentMinute && effectiveSeconds > 0L) {
            lastAnimatedMinute = currentMinute
            triggerMinutePulseAnimation(baseColor)
        }

        // Load app icon
        try {
            val iconDrawable = packageManager.getApplicationIcon(packageName)
            iconImageView?.setImageDrawable(iconDrawable)
        } catch (e: Exception) {
            iconImageView?.setImageResource(android.R.drawable.ic_lock_idle_lock)
        }

        return START_STICKY
    }

    private fun triggerMinutePulseAnimation(baseColor: Int) {
        val view = floatingView ?: return
        pulseAnimator?.cancel()

        val pulseBg = GradientDrawable().apply {
            cornerRadius = 40f
            setColor(Color.parseColor("#EF4444")) // Bright Red Pulse
            setStroke(3, Color.parseColor("#FCA5A5"))
        }

        // Pulse scale & red flash
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.12f, 1f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.12f, 1f)
        val animator = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY).apply {
            duration = 650L
            repeatCount = 1
            repeatMode = ValueAnimator.REVERSE
        }

        val colorAnimator = ValueAnimator.ofObject(
            ArgbEvaluator(),
            Color.parseColor("#EF4444"),
            baseColor
        ).apply {
            duration = 1000L
            addUpdateListener { anim ->
                val color = anim.animatedValue as Int
                val animatedBg = GradientDrawable().apply {
                    cornerRadius = 40f
                    setColor(color)
                    setStroke(2, Color.parseColor("#334155"))
                }
                view.background = animatedBg
            }
        }

        pulseAnimator = colorAnimator
        animator.start()
        colorAnimator.start()
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
            textSize = 13f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.MONOSPACE
            text = "⏳ 00:00"
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
        pulseAnimator?.cancel()
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
        private const val NOTIFICATION_ID = 2002

        const val ACTION_SHOW_OR_UPDATE = "action_show_or_update"
        const val ACTION_HIDE = "action_hide"

        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_REMAINING_MINUTES = "extra_remaining_minutes"
        const val EXTRA_REMAINING_SECONDS = "extra_remaining_seconds"
        const val EXTRA_IS_WARNING = "extra_is_warning"

        fun update(
            context: Context,
            appName: String,
            packageName: String,
            remainingSeconds: Long,
            isWarning: Boolean
        ) {
            val intent = Intent(context, FloatingTimerOverlayService::class.java).apply {
                action = ACTION_SHOW_OR_UPDATE
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_REMAINING_SECONDS, remainingSeconds)
                putExtra(EXTRA_REMAINING_MINUTES, (remainingSeconds / 60).toInt())
                putExtra(EXTRA_IS_WARNING, isWarning)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
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
