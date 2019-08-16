package com.kotlinify.cropiwa.image

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask

import com.kotlinify.cropiwa.config.CropIwaSaveConfig
import com.kotlinify.cropiwa.shape.CropIwaShapeMask
import com.kotlinify.cropiwa.util.CropIwaUtils

import java.io.IOException
import java.io.OutputStream

/**
 * Created by Yaroslav Polyakov on 22.03.2017.
 * https://github.com/polyak01
 */

internal class CropImageTask(
        private val context: Context, private val cropArea: CropArea, private val mask: CropIwaShapeMask,
        private val srcUri: Uri, private val saveConfig: CropIwaSaveConfig) : AsyncTask<Void, Void, Throwable>() {

    override fun doInBackground(vararg params: Void): Throwable? {
        try {
            val bitmap = CropIwaBitmapManager.get().loadToMemory(
                    context, srcUri, saveConfig.width,
                    saveConfig.height) ?: return NullPointerException("Failed to load bitmap")

            var cropped: Bitmap? = cropArea.applyCropTo(bitmap)
                    ?: return Exception("Coordinates must not negative")

            cropped = cropped?.let { mask.applyMaskTo(it) }

            val dst = saveConfig.dstUri
            val os = context.contentResolver.openOutputStream(dst!!)
            cropped!!.compress(saveConfig.compressFormat, saveConfig.quality, os)
            CropIwaUtils.closeSilently(os)

            bitmap.recycle()
            cropped.recycle()
        } catch (e: IOException) {
            return e
        }

        return null
    }

    override fun onPostExecute(throwable: Throwable?) {
        if (throwable == null) {
            CropIwaResultReceiver.onCropCompleted(context, saveConfig.dstUri!!)
        } else {
            CropIwaResultReceiver.onCropFailed(context, throwable)
        }
    }
}