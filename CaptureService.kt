package com.example.carromassistant

import android.app.*
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.DisplayMetrics
import android.view.WindowManager
import java.nio.ByteBuffer

class CaptureService : Service() {
    companion object {
        const val ACTION_ANALYSIS = "com.example.carromassistant.ANALYSIS"
        const val ACTION_STOPPED = "com.example.carromassistant.STOPPED"
        private const val CHANNEL = "carrom_analysis"
        private const val NOTIFICATION_ID = 77
    }

    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var display: VirtualDisplay? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastRun = 0L

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        val notification = Notification.Builder(this, CHANNEL)
            .setContentTitle("Carrom AI Assistant")
            .setContentText("Analysing the shared screen")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setOngoing(true).build()
        startForeground(NOTIFICATION_ID, notification)

        val code = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = intent?.getParcelableExtra<Intent>("captureData")
        if (code != Activity.RESULT_OK || data == null) { stopSelf(); return START_NOT_STICKY }

        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mgr.getMediaProjection(code, data)
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        reader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2)
        display = projection?.createVirtualDisplay(
            "CarromAI", width, height, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader?.surface, null, handler
        )
        reader?.setOnImageAvailableListener({ source ->
            val now = System.currentTimeMillis()
            if (now - lastRun < 1400) { source.acquireLatestImage()?.close(); return@setOnImageAvailableListener }
            lastRun = now
            val image = source.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val bitmap = imageToBitmap(image)
                val report = BoardAnalyzer.analyze(bitmap)
                sendBroadcast(Intent(ACTION_ANALYSIS).setPackage(packageName).putExtra("report", report))
                bitmap.recycle()
            } catch (_: Exception) {
                sendBroadcast(Intent(ACTION_ANALYSIS).setPackage(packageName).putExtra("report", "Could not analyse this frame. Keep the full board visible."))
            } finally { image.close() }
        }, handler)
        return START_NOT_STICKY
    }

    private fun imageToBitmap(image: Image): android.graphics.Bitmap {
        val plane = image.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width
        val padded = android.graphics.Bitmap.createBitmap(
            image.width + rowPadding / pixelStride, image.height, android.graphics.Bitmap.Config.ARGB_8888
        )
        padded.copyPixelsFromBuffer(buffer)
        val cropped = android.graphics.Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
        padded.recycle()
        return cropped
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL, "Carrom analysis", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        reader?.close(); display?.release(); projection?.stop()
        sendBroadcast(Intent(ACTION_STOPPED).setPackage(packageName))
        super.onDestroy()
    }
}
