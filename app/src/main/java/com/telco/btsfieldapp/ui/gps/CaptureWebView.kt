package com.telco.btsfieldapp.ui.gps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import android.webkit.WebView

/**
 * Custom WebView that can capture its content as a Bitmap.
 * Uses a drawing cache + bitmap recreation for reliable capture.
 */
class CaptureWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.webViewStyle
) : WebView(context, attrs, defStyleAttr) {

    /**
     * Capture the WebView's current content as a Bitmap.
     * Returns null if the view has zero dimensions.
     */
    fun captureBitmap(): Bitmap? {
        if (width <= 0 || height <= 0) return null

        // Create a bitmap matching the view dimensions
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        draw(canvas)
        return bitmap
    }
}
