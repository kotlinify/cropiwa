package com.kotlinify.cropiwa.shape

import android.graphics.Bitmap

import java.io.Serializable

/**
 * Created by yarolegovich on 22.03.2017
 * https://github.com/yarolegovich
 */
interface CropIwaShapeMask : Serializable {
    fun applyMaskTo(croppedRegion: Bitmap): Bitmap
}
