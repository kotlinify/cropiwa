package com.kotlinify.cropiwa.shape

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF

import com.kotlinify.cropiwa.CropIwaView
import com.kotlinify.cropiwa.config.ConfigChangeListener
import com.kotlinify.cropiwa.config.CropIwaOverlayConfig

/**
 * @author yarolegovich https://github.com/yarolegovich
 * 06.02.2017.
 */
abstract class CropIwaShape(protected var overlayConfig: CropIwaOverlayConfig?) : ConfigChangeListener {
    private val clearPaint: Paint
    val cornerPaint: Paint
    val gridPaint: Paint
    val borderPaint: Paint

    abstract val mask: CropIwaShapeMask

    constructor(cropIwaView: CropIwaView) : this(cropIwaView.configureOverlay()) {}

    init {

        clearPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        clearPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

        gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        gridPaint.style = Paint.Style.STROKE
        gridPaint.strokeCap = Paint.Cap.SQUARE

        borderPaint = Paint(gridPaint)

        cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        cornerPaint.style = Paint.Style.STROKE
        cornerPaint.strokeCap = Paint.Cap.ROUND

        updatePaintObjectsFromConfig()
    }

    fun draw(canvas: Canvas, cropBounds: RectF) {
        clearArea(canvas, cropBounds, clearPaint)
        if (overlayConfig!!.shouldDrawGrid()) {
            drawGrid(canvas, cropBounds, gridPaint)
        }
        drawBorders(canvas, cropBounds, borderPaint)
    }

    fun drawCorner(canvas: Canvas, x: Float, y: Float, deltaX: Float, deltaY: Float) {
        canvas.drawLine(x, y, x + deltaX, y, cornerPaint)
        canvas.drawLine(x, y, x, y + deltaY, cornerPaint)
    }

    protected abstract fun clearArea(canvas: Canvas, cropBounds: RectF, clearPaint: Paint)

    protected abstract fun drawBorders(canvas: Canvas, cropBounds: RectF, paint: Paint)

    protected open fun drawGrid(canvas: Canvas, cropBounds: RectF, paint: Paint) {
        val stepX = cropBounds.width() * 0.333f
        val stepY = cropBounds.height() * 0.333f
        var x = cropBounds.left
        var y = cropBounds.top
        for (i in 0..1) {
            x += stepX
            y += stepY
            canvas.drawLine(x, cropBounds.top, x, cropBounds.bottom, paint)
            canvas.drawLine(cropBounds.left, y, cropBounds.right, y, paint)
        }
    }

    override fun onConfigChanged() {
        updatePaintObjectsFromConfig()
    }

    private fun updatePaintObjectsFromConfig() {
        cornerPaint.strokeWidth = overlayConfig!!.getCornerStrokeWidth().toFloat()
        cornerPaint.color = overlayConfig!!.getCornerColor()
        gridPaint.color = overlayConfig!!.getGridColor()
        gridPaint.strokeWidth = overlayConfig!!.getGridStrokeWidth().toFloat()
        borderPaint.color = overlayConfig!!.getBorderColor()
        borderPaint.strokeWidth = overlayConfig!!.getBorderStrokeWidth().toFloat()
    }
}