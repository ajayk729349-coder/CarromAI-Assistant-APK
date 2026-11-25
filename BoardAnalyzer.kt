package com.example.carromassistant

import android.graphics.Bitmap
import kotlin.math.*

/**
 * Lightweight colour heuristic for a first prototype.
 * It deliberately reports observations, not guaranteed shot instructions.
 * Screen themes, animations, shadows and compression can cause false detections.
 */
object BoardAnalyzer {
    data class Counts(val blue: Int, val black: Int, val red: Int)

    fun analyze(bitmap: Bitmap): String {
        // Downsample for speed. Ignore the top HUD and bottom controls.
        val w = bitmap.width
        val h = bitmap.height
        val sx = max(1, w / 180)
        val sy = max(1, h / 300)
        var blue = 0; var black = 0; var red = 0
        var bluePixels = 0; var blackPixels = 0; var redPixels = 0

        var y = (h * 0.27).toInt()
        val yEnd = (h * 0.78).toInt()
        while (y < yEnd) {
            var x = (w * 0.02).toInt()
            val xEnd = (w * 0.98).toInt()
            while (x < xEnd) {
                val c = bitmap.getPixel(x, y)
                val r = android.graphics.Color.red(c)
                val g = android.graphics.Color.green(c)
                val b = android.graphics.Color.blue(c)
                val maxC = max(r, max(g, b))
                val minC = min(r, min(g, b))
                val saturation = maxC - minC
                if (b > r * 1.18 && b > g * 1.04 && b > 95) bluePixels++
                if (r > 155 && r > g * 1.45 && r > b * 1.35) redPixels++
                if (maxC < 78 && saturation < 42) blackPixels++
                x += sx
            }
            y += sy
        }

        // Pixel totals are intentionally labelled rough colour evidence, not coin counts.
        val boardVisible = bluePixels + blackPixels + redPixels > 20
        val guidance = when {
            !boardVisible -> "Board not confidently detected. Keep the complete wooden board visible and avoid menus or pop-ups."
            bluePixels > 30 && redPixels > 3 -> "Blue and red pieces detected. Check the board visually before choosing a shot."
            bluePixels > 30 -> "Blue-coloured pieces detected. Check opponent pieces and the striker position."
            else -> "Some board colours detected, but piece classification is uncertain."
        }
        return "LIVE SCREEN ANALYSIS\n\n" +
            "Blue-colour pixels: $bluePixels\n" +
            "Black/dark pixels: $blackPixels\n" +
            "Red-colour pixels: $redPixels\n\n" +
            "$guidance\n\n" +
            "Prototype limitation: these are colour-pixel indicators, not verified coin counts or a reliable best-shot calculation. Use the on-screen game controls yourself."
    }
}
