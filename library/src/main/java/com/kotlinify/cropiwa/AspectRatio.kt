package com.kotlinify.cropiwa

import androidx.annotation.IntRange

/**
 * Created by yarolegovich https://github.com/yarolegovich
 * on 06.02.2017.
 */

class AspectRatio(@param:IntRange(from = -1) val width: Int, @param:IntRange(from = -1) val height: Int) {

    val isSquare: Boolean
        get() = width == height

    val ratio: Float
        get() = width.toFloat() / height

    companion object {

        val IMG_SRC = AspectRatio(-1, -1)
    }
}
