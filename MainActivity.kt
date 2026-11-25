package com.example.carromassistant

import android.app.Activity
import android.content.*
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.*
import android.view.Gravity

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var details: TextView
    private val requestCapture = 4102

    private val resultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == CaptureService.ACTION_ANALYSIS) {
                status.text = "Live analysis running"
                details.text = intent.getStringExtra("report") ?: "Waiting for a clear board view…"
            } else if (intent?.action == CaptureService.ACTION_STOPPED) {
                status.text = "Capture stopped"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 36, 28, 24)
        }
        val heading = TextView(this).apply {
            text = "Carrom AI Assistant"
            textSize = 25f
        }
        status = TextView(this).apply { text = "Not connected"; textSize = 16f; setPadding(0, 18, 0, 18) }
        details = TextView(this).apply {
            text = "Start screen analysis, then switch to Carrom Disc Pool. Android will show a screen-sharing permission prompt."
            textSize = 16f
            setPadding(0, 12, 0, 12)
        }
        val start = Button(this).apply {
            text = "Start live screen analysis"
            setOnClickListener {
                val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                startActivityForResult(mgr.createScreenCaptureIntent(), requestCapture)
            }
        }
        val stop = Button(this).apply {
            text = "Stop analysis"
            setOnClickListener { stopService(Intent(this@MainActivity, CaptureService::class.java)); status.text = "Stopped" }
        }
        layout.addView(heading); layout.addView(status); layout.addView(details)
        layout.addView(start); layout.addView(stop)
        setContentView(layout)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCapture && resultCode == RESULT_OK && data != null) {
            val service = Intent(this, CaptureService::class.java).apply {
                putExtra("resultCode", resultCode)
                putExtra("captureData", data)
            }
            startForegroundService(service)
            status.text = "Starting live analysis…"
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(CaptureService.ACTION_ANALYSIS)
            addAction(CaptureService.ACTION_STOPPED)
        }
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(resultReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(resultReceiver, filter)
        }
    }

    override fun onStop() {
        try { unregisterReceiver(resultReceiver) } catch (_: Exception) {}
        super.onStop()
    }
}
