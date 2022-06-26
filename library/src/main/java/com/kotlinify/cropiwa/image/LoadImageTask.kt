package com.kotlinify.cropiwa.image

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors


internal class LoadImageTask(private val context: Context, private val uri: Uri, private val desiredWidth: Int, private val desiredHeight: Int)  {

    fun execute() {
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            try {
                result = CropIwaBitmapManager.get().loadToMemory(
                    context, uri, desiredWidth,
                    desiredHeight
                )

                if (result == null) {
                    throw NullPointerException("Failed to load bitmap")
                }
            } catch (e: Exception) {
                result?.let { CropIwaBitmapManager.get().notifyListener(uri, it, e) }
            }
            handler.post {
                result?.let { CropIwaBitmapManager.get().notifyListener(uri, it, null) }
            }
        }


    }

    private var result: Bitmap? = null
}