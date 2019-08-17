package com.kotlinify.cropiwa.config

import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.IntRange

import com.kotlinify.cropiwa.image.CropIwaBitmapManager

/**
 * @author Yaroslav Polyakov https://github.com/polyak01
 * 25.02.2017.
 */
class CropIwaSaveConfig(dstPath: Uri) {

    var compressFormat: Bitmap.CompressFormat? = null
        private set
    var quality: Int = 0
        private set
    var width: Int = 0
        private set
    var height: Int = 0
        private set
    var dstUri: Uri? = null
        private set

    init {
        this.dstUri = dstPath
        this.compressFormat = Bitmap.CompressFormat.PNG
        this.width = CropIwaBitmapManager.SIZE_UNSPECIFIED
        this.height = CropIwaBitmapManager.SIZE_UNSPECIFIED
        this.quality = 90
    }

    class Builder(dstPath: Uri) {

        private val saveConfig: CropIwaSaveConfig

        init {
            saveConfig = CropIwaSaveConfig(dstPath)
        }

        fun setSize(width: Int, height: Int): Builder {
            saveConfig.width = width
            saveConfig.height = height
            return this
        }

        fun setCompressFormat(compressFormat: Bitmap.CompressFormat): Builder {
            saveConfig.compressFormat = compressFormat
            return this
        }

        fun setQuality(@IntRange(from = 0, to = 100) quality: Int): Builder {
            saveConfig.quality = quality
            return this
        }

        fun saveToFile(uri: Uri): Builder {
            saveConfig.dstUri = uri
            return this
        }

        fun build(): CropIwaSaveConfig {
            return saveConfig
        }
    }


}
