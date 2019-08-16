package com.kotlinify.cropiwa.shape

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF

import com.kotlinify.cropiwa.config.CropIwaOverlayConfig

/**
 * Created by yarolegovich on 04.02.2017.
 * https://github.com/yarolegovich
 */
class CropIwaOvalShape(config: CropIwaOverlayConfig) : CropIwaShape(config) {
    override val mask: CropIwaShapeMask
        get() = OvalShapeMask()

    private val clipPath: Path

    init {
        clipPath = Path()
    }

    override fun clearArea(canvas: Canvas, cropBounds: RectF, clearPaint: Paint) {
        canvas.drawOval(cropBounds, clearPaint)
    }


    override fun drawBorders(canvas: Canvas, cropBounds: RectF, paint: Paint) {
        canvas.drawOval(cropBounds, paint)
        if (overlayConfig?.isDynamicCrop() == true) {
            canvas.drawRect(cropBounds, paint)
        }
    }

    override fun drawGrid(canvas: Canvas, cropBounds: RectF, paint: Paint) {
        clipPath.rewind()
        clipPath.addOval(cropBounds, Path.Direction.CW)

        canvas.save()
        canvas.clipPath(clipPath)
        super.drawGrid(canvas, cropBounds, paint)
        canvas.restore()
    }

    private class OvalShapeMask : CropIwaShapeMask {
        override fun applyMaskTo(croppedRegion: Bitmap): Bitmap {
            croppedRegion.setHasAlpha(true)

            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

            val ovalRect = RectF(0f, 0f, croppedRegion.width.toFloat(), croppedRegion.height.toFloat())
            val maskShape = Path()
            //This is similar to ImageRect\Oval
            maskShape.addRect(ovalRect, Path.Direction.CW)
            maskShape.addOval(ovalRect, Path.Direction.CCW)

            val canvas = Canvas(croppedRegion)
            canvas.drawPath(maskShape, maskPaint)
            return croppedRegion
        }
    }
}

