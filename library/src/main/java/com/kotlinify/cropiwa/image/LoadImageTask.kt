package com.kotlinify.cropiwa.image

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask

/**
 * Created by Yaroslav Polyakov on 22.03.2017.
 * https://github.com/polyak01
 */
internal class LoadImageTask(private val context: Context, private val uri: Uri, private val desiredWidth: Int, private val desiredHeight: Int) : AsyncTask<Void, Void, Throwable>() {

    private var result: Bitmap? = null

    override fun doInBackground(vararg params: Void): Throwable? {
        try {
            result = CropIwaBitmapManager.get().loadToMemory(
                    context, uri, desiredWidth,
                    desiredHeight)

            if (result == null) {
                return NullPointerException("Failed to load bitmap")
            }
        } catch (e: Exception) {
            return e
        }

        return null
    }

    override fun onPostExecute(e: Throwable?) {
        result?.let { CropIwaBitmapManager.get().notifyListener(uri, it, e) }
    }
}