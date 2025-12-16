package com.appriyo.newsvoiceassistant.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.*
import android.view.*
import androidx.core.app.NotificationCompat
import com.appriyo.newsvoiceassistant.MainActivity
import com.appriyo.newsvoiceassistant.R
import com.appriyo.newsvoiceassistant.data.repository.NewsRepository
import com.appriyo.newsvoiceassistant.tts.TTSManager
import com.appriyo.newsvoiceassistant.util.NotificationHelper
import com.appriyo.newsvoiceassistant.util.OverlayPermissionHelper
import kotlin.math.abs

class NewsVoiceService : Service() {

    companion object {
        const val ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_START = "ACTION_START"
        const val ACTION_HIDE_BUBBLE = "ACTION_HIDE_BUBBLE"
        const val ACTION_SHOW_BUBBLE = "ACTION_SHOW_BUBBLE"
    }

    private lateinit var ttsManager: TTSManager
    private val newsRepository = NewsRepository()
    private var isSpeaking = false
    private var isAppInForeground = false
    private var shouldShowBubble = false

    // Overlay bubble
    private var bubbleView: View? = null
    private var windowManager: WindowManager? = null
    private var bubbleParams: WindowManager.LayoutParams? = null

    private val handler = Handler(Looper.getMainLooper())
    private val activityCheckRunnable = object : Runnable {
        override fun run() {
            updateActivityState()
            handler.postDelayed(this, 1000) // Check every second
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize TTS Manager
        ttsManager = TTSManager(this) {
            // When utterance completes, speak next headline
            speakNext()
        }

        // Create notification channel
        NotificationHelper.createChannel(this)

        // Start as foreground service
        startForeground(1, buildNotification())

        // Start speaking
        speakCurrent()

        // Start activity state monitoring
        handler.post(activityCheckRunnable)

        // Check if we should show bubble initially
        shouldShowBubble = OverlayPermissionHelper.hasPermission(this) && !isAppInForeground
        if (shouldShowBubble) {
            showFloatingBubble()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (!isSpeaking) {
                    speakCurrent()
                }
            }
            ACTION_PLAY_PAUSE -> toggleSpeak()
            ACTION_NEXT -> nextNews()
            ACTION_STOP -> stopService()
            ACTION_HIDE_BUBBLE -> {
                isAppInForeground = true
                removeFloatingBubble()
            }
            ACTION_SHOW_BUBBLE -> {
                isAppInForeground = false
                if (OverlayPermissionHelper.hasPermission(this)) {
                    showFloatingBubble()
                }
            }
        }
        return START_STICKY
    }

    private fun updateActivityState() {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningTasks = activityManager.getRunningTasks(1)
        val currentActivity = runningTasks.firstOrNull()?.topActivity?.className

        val wasInForeground = isAppInForeground
        isAppInForeground = currentActivity == MainActivity::class.java.name

        if (wasInForeground != isAppInForeground) {
            if (isAppInForeground) {
                // App came to foreground, hide bubble
                removeFloatingBubble()
            } else {
                // App went to background, show bubble if permission granted
                if (OverlayPermissionHelper.hasPermission(this)) {
                    showFloatingBubble()
                }
            }
        }
    }

    private fun toggleSpeak() {
        if (isSpeaking) {
            pauseSpeaking()
        } else {
            resumeSpeaking()
        }
        updateNotification()
        updateBubbleIcon()
    }

    private fun pauseSpeaking() {
        ttsManager.stop()
        isSpeaking = false
    }

    private fun resumeSpeaking() {
        speakCurrent()
    }

    private fun speakCurrent() {
        newsRepository.getCurrentHeadline()?.let { headline ->
            ttsManager.speak(headline)
            isSpeaking = true
            updateNotification()
            updateBubbleIcon()
        }
    }

    private fun speakNext() {
        // Automatically called when TTS completes
        if (isSpeaking) {
            nextNews()
        }
    }

    private fun nextNews() {
        pauseSpeaking()
        newsRepository.nextHeadline()?.let { headline ->
            ttsManager.speak(headline)
            isSpeaking = true
            updateNotification()
            updateBubbleIcon()
        }
    }

    private fun stopService() {
        pauseSpeaking()
        removeFloatingBubble()
        handler.removeCallbacks(activityCheckRunnable)
        stopForeground(true)
        stopSelf()
    }

    // ==================== Floating Bubble Logic ====================
    private fun showFloatingBubble() {
        if (!OverlayPermissionHelper.hasPermission(this)) return
        if (bubbleView != null) return
        if (isAppInForeground) return // Don't show bubble when app is in foreground

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        bubbleView = inflater.inflate(R.layout.bubble_layout, null)

        val screenWidth = resources.displayMetrics.widthPixels

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - 200 // Start from right side
            y = 300 // Start from top
        }

        setupBubbleTouchListener()
        windowManager?.addView(bubbleView, bubbleParams)
        updateBubbleIcon()
    }

    private fun setupBubbleTouchListener() {
        bubbleView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = false
            private var lastClickTime = 0L

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                event?.let {
                    // Check for clicks first
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = bubbleParams?.x ?: 0
                            initialY = bubbleParams?.y ?: 0
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            isClick = true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val xDiff = abs(event.rawX - initialTouchX)
                            val yDiff = abs(event.rawY - initialTouchY)

                            // If moved more than 10 pixels, it's not a click
                            if (xDiff > 10 || yDiff > 10) {
                                isClick = false
                            }

                            bubbleParams?.x = initialX + (event.rawX - initialTouchX).toInt()
                            bubbleParams?.y = initialY + (event.rawY - initialTouchY).toInt()
                            windowManager?.updateViewLayout(bubbleView, bubbleParams)
                        }
                        MotionEvent.ACTION_UP -> {
                            if (isClick) {
                                handleBubbleClick()
                            }
                        }
                    }
                }
                return true
            }

            private fun handleBubbleClick() {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime < 300) {
                    // Double tap
                    nextNews()
                    lastClickTime = 0
                } else {
                    // Single tap
                    toggleSpeak()
                    lastClickTime = currentTime
                }
            }
        })

        // Long press to stop
        bubbleView?.setOnLongClickListener {
            stopService()
            true
        }
    }

    private fun updateBubbleIcon() {
        bubbleView?.findViewById<android.widget.ImageView>(R.id.bubble_icon)?.setImageResource(
            if (isSpeaking) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun removeFloatingBubble() {
        bubbleView?.let {
            windowManager?.removeView(it)
            bubbleView = null
        }
    }

    // ==================== Notification Logic ====================
    private fun buildNotification(): Notification {
        val playIntent = Intent(this, NewsVoiceService::class.java).apply {
            action = ACTION_PLAY_PAUSE
        }
        val nextIntent = Intent(this, NewsVoiceService::class.java).apply {
            action = ACTION_NEXT
        }
        val stopIntent = Intent(this, NewsVoiceService::class.java).apply {
            action = ACTION_STOP
        }
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val playPending = PendingIntent.getService(
            this, 0, playIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val nextPending = PendingIntent.getService(
            this, 1, nextIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopPending = PendingIntent.getService(
            this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val openAppPending = PendingIntent.getActivity(
            this, 3, openAppIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val currentHeadline = newsRepository.getCurrentHeadline() ?: "No headlines available"
        val statusText = if (isSpeaking) "Reading: ${currentHeadline.take(30)}..." else "Paused"

        val playPauseIcon = if (isSpeaking) {
            R.drawable.ic_pause
        } else {
            R.drawable.ic_play
        }

        val bubbleStatus = if (OverlayPermissionHelper.hasPermission(this)) {
            if (bubbleView != null) "• Bubble Visible" else "• Bubble Hidden (App in foreground)"
        } else {
            "• Notification Mode"
        }

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setContentTitle("News Voice Assistant")
            .setContentText(statusText)
            .setContentIntent(openAppPending)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$currentHeadline\n\n$bubbleStatus")
                    .setSummaryText("Tap to open app")
            )
            .addAction(playPauseIcon, "Play/Pause", playPending)
            .addAction(R.drawable.ic_next, "Next", nextPending)
            .addAction(R.drawable.ic_stop, "Stop", stopPending)
            .build()
    }

    private fun updateNotification() {
        val notification = buildNotification()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }

    override fun onDestroy() {
        ttsManager.shutdown()
        removeFloatingBubble()
        handler.removeCallbacks(activityCheckRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}