package com.kotlinify.cropiwa.image

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.roundToInt

/**
 * @author yarolegovich
 * 25.02.2017.
 */
class CropArea(private val imageRect: Rect, private val cropRect: Rect) {

    fun applyCropTo(bitmap: Bitmap): Bitmap? {
        val x = findRealCoordinate(bitmap.width, cropRect.left, imageRect.width().toFloat())
        val y = findRealCoordinate(bitmap.height, cropRect.top, imageRect.height().toFloat())
        if (x < 0 || y < 0 || imageRect.bottom - cropRect.bottom < 0 || imageRect.left - cropRect.left > 0 || imageRect.right - cropRect.right < 0 || imageRect.top - cropRect.top > 0) {
            return null
        } else {
            val immutableCropped = Bitmap.createBitmap(  //TODO: crash Caused by java.lang.IllegalArgumentException y + height must be <= bitmap.height()
                    bitmap,
                    x, y,
                    findRealCoordinate(bitmap.width, cropRect.width(), imageRect.width().toFloat()),
                    findRealCoordinate(bitmap.height, cropRect.height(), imageRect.height().toFloat()))
            return immutableCropped.copy(immutableCropped.config, true) //TODO: crash outofmemory

        }
    }


    private fun findRealCoordinate(imageRealSize: Int, cropCoordinate: Int, cropImageSize: Float): Int {
        return Math.round(imageRealSize * cropCoordinate / cropImageSize)
    }

    companion object {

        fun create(coordinateSystem: RectF, imageRect: RectF, cropRect: RectF): CropArea {
            return CropArea(
                    moveRectToCoordinateSystem(coordinateSystem, imageRect),
                    moveRectToCoordinateSystem(coordinateSystem, cropRect))
        }

        private fun moveRectToCoordinateSystem(system: RectF, rect: RectF): Rect {
            val originX = system.left
            val originY = system.top
            return Rect(
                    (rect.left - originX).roundToInt(), (rect.top - originY).roundToInt(),
                    (rect.right - originX).roundToInt(), (rect.bottom - originY).roundToInt())
        }
    }

}
