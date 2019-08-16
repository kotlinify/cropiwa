package com.kotlinify.cropiwa.shape

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

import com.kotlinify.cropiwa.config.CropIwaOverlayConfig

/**
 * Created by yarolegovich on 04.02.2017.
 * https://github.com/yarolegovich
 */
class CropIwaRectShape(config: CropIwaOverlayConfig) : CropIwaShape(config) {
    override val mask: CropIwaShapeMask
        get() = RectShapeMask()

    override fun clearArea(canvas: Canvas, cropBounds: RectF, clearPaint: Paint) {
        canvas.drawRect(cropBounds, clearPaint)
    }

    override fun drawBorders(canvas: Canvas, cropBounds: RectF, paint: Paint) {
        canvas.drawRect(cropBounds, paint)
    }

    private class RectShapeMask : CropIwaShapeMask {
        override fun applyMaskTo(croppedRegion: Bitmap): Bitmap {
            //Nothing to do
            return croppedRegion
        }
    }
}

