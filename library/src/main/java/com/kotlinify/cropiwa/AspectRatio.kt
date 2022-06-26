package com.kotlinify.cropiwa

import androidx.annotation.IntRange

/**
 * Created by yarolegovich https://github.com/yarolegovich
 * on 06.02.2017.
 */

open class AspectRatio(@param:IntRange(from = -1) var width: Int, @param:IntRange(from = -1) var height: Int) {

    val isSquare: Boolean
        get() = width == height

    val ratio: Float
        get() = width.toFloat() / height

    companion object {
        var IMG_SRC = AspectRatio(-1, -1)
    }
}
