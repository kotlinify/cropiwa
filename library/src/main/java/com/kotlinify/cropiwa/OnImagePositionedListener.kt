package com.kotlinify.cropiwa

import android.graphics.RectF

/**
 * @author Yaroslav Polyakov
 * 25.02.2017.
 */

internal interface OnImagePositionedListener {
    fun onImagePositioned(imageRect: RectF)
}
