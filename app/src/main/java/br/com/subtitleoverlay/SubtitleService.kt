package br.com.subtitleoverlay

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.PixelFormat
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView

class SubtitleService : Service() {
    companion object {
        const val ACTION_START = "br.com.subtitleoverlay.START"
        const val ACTION_SHIFT = "br.com.subtitleoverlay.SHIFT"
        const val ACTION_PAUSE = "br.com.subtitleoverlay.PAUSE"
        const val ACTION_STOP = "br.com.subtitleoverlay.STOP"
        const val EXTRA_URI = "subtitle_uri"
        const val EXTRA_DELAY = "delay_seconds"
        const val EXTRA_SHIFT = "shift_ms"
        const val PREFS = "session"
        const val KEY_OFFSET = "offset_ms"
        private const val CHANNEL = "subtitle_overlay"
        private const val NOTIFICATION = 101
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var wm: WindowManager
    private var subtitleView: TextView? = null
    private var cues: List<Cue> = emptyList()
    private var startAt = 0L
    private var offset = 0L
    private var paused = false
    private var pausedAt = 0L
    private var previousText = ""

    private val ticker = object : Runnable {
        override fun run() {
            if (!paused && subtitleView != null) {
                val time = SystemClock.elapsedRealtime() - startAt + offset
                val line = if (time < 0) "" else findCue(time)
                if (line != previousText) {
                    subtitleView?.text = line
                    previousText = line
                }
            }
            handler.postDelayed(this, 100L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val channel = NotificationChannel(CHANNEL, "Legendas em execução", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
                val file = intent.getStringExtra(EXTRA_URI) ?: return START_NOT_STICKY
                try {
                    val stream = contentResolver.openInputStream(Uri.parse(file)) ?: error("Arquivo inacessível")
                    val raw = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    cues = Srt.parse(raw)
                    if (cues.isEmpty()) error("Nenhuma legenda SRT válida")
                    offset = getSharedPreferences(PREFS, MODE_PRIVATE).getLong(KEY_OFFSET, 0L)
                    startAt = SystemClock.elapsedRealtime() + intent.getIntExtra(EXTRA_DELAY, 15) * 1000L
                    paused = false
                    removeOverlay()
                    showOverlay()
                    startForeground(NOTIFICATION, notification())
                    handler.removeCallbacks(ticker)
                    handler.post(ticker)
                } catch (e: Exception) {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("error", e.message).apply()
                    stopSelf()
                }
            }
            ACTION_SHIFT -> {
                offset += intent.getLongExtra(EXTRA_SHIFT, 0L)
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putLong(KEY_OFFSET, offset).apply()
                previousText = "" // forces next update
                updateNotification()
            }
            ACTION_PAUSE -> {
                if (!paused) {
                    paused = true
                    pausedAt = SystemClock.elapsedRealtime()
                } else {
                    startAt += SystemClock.elapsedRealtime() - pausedAt
                    paused = false
                }
                updateNotification()
            }
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun findCue(ms: Long): String {
        // Binary search; choose the latest cue starting before playback timestamp.
        var lo = 0
        var hi = cues.lastIndex
        var found = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (cues[mid].fromMs <= ms) { found = mid; lo = mid + 1 } else hi = mid - 1
        }
        if (found < 0) return ""
        val cue = cues[found]
        return if (ms < cue.toMs) cue.text else ""
    }

    private fun showOverlay() {
        val text = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 23f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
            setShadowLayer(5f, 2f, 2f, Color.BLACK)
            setPadding(45, 12, 45, 12)
            maxLines = 3
            setBackgroundColor(Color.TRANSPARENT)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; y = 85 }
        wm.addView(text, params)
        subtitleView = text
    }

    private fun removeOverlay() {
        subtitleView?.let { runCatching { wm.removeView(it) } }
        subtitleView = null
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        fun action(label: String, cmd: String, requestCode: Int, shift: Long = 0): Notification.Action {
            val i = Intent(this, SubtitleService::class.java).setAction(cmd)
                .putExtra(EXTRA_SHIFT, shift)
            val pi = PendingIntent.getService(this, requestCode, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            return Notification.Action.Builder(android.R.drawable.ic_media_pause, label, pi).build()
        }
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Subtitle Overlay TV")
            .setContentText("Legendas ativas · ajuste ${offset / 1000.0}s")
            .setContentIntent(open)
            .addAction(action("Pausar / seguir", ACTION_PAUSE, 1))
            .addAction(action("Adiantar 1s", ACTION_SHIFT, 2, 1000))
            .addAction(action("Atrasar 1s", ACTION_SHIFT, 3, -1000))
            .addAction(action("Parar", ACTION_STOP, 4))
            .build()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION, notification())
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        removeOverlay()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?) = null
}
