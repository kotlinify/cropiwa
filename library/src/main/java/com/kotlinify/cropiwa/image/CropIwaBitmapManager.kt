package com.kotlinify.cropiwa.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.AsyncTask

import com.kotlinify.cropiwa.config.CropIwaSaveConfig
import com.kotlinify.cropiwa.shape.CropIwaShapeMask
import com.kotlinify.cropiwa.util.CropIwaLog
import com.kotlinify.cropiwa.util.CropIwaUtils
import com.kotlinify.cropiwa.util.ImageHeaderParser

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.HashMap

import com.kotlinify.cropiwa.util.CropIwaUtils.*

/**
 * @author Yaroslav Polyakov https://github.com/polyak01
 * on 25.02.2017.
 */
class CropIwaBitmapManager private constructor() {

    private val loadRequestLock = Any()

    private val requestResultListeners: MutableMap<Uri, BitmapLoadListener?>?
    private val localCache: MutableMap<Uri, File>

    init {
        requestResultListeners = HashMap()
        localCache = HashMap()
    }

    fun load(context: Context, uri: Uri, width: Int, height: Int, listener: BitmapLoadListener) {
        synchronized(loadRequestLock) {
            val requestInProgress = requestResultListeners?.containsKey(uri)
            requestResultListeners?.set(uri, listener)
            if (requestInProgress==true) {
                CropIwaLog.d("request for {%s} is already in progress", uri.toString())
                return
            }
        }
        CropIwaLog.d("load bitmap request for {%s}", uri.toString())

        val task = LoadImageTask(
                context.applicationContext, uri,
                width, height)
        task.execute()
    }

    fun crop(
            context: Context, cropArea: CropArea, mask: CropIwaShapeMask,
            uri: Uri, saveConfig: CropIwaSaveConfig) {
        val cropTask = CropImageTask(
                context.applicationContext,
                cropArea, mask, uri, saveConfig)
        cropTask.execute()
    }

    fun unregisterLoadListenerFor(uri: Uri) {
        synchronized(loadRequestLock) {
            if (requestResultListeners?.containsKey(uri)==true) {
                CropIwaLog.d("listener for {%s} loading unsubscribed", uri.toString())
                requestResultListeners[uri] = null
            }
        }
    }

    fun removeIfCached(uri: Uri) {
        CropIwaUtils.delete(localCache.remove(uri))
    }

    internal fun notifyListener(uri: Uri, result: Bitmap, e: Throwable?=null) {
        val listener: CropIwaBitmapManager.BitmapLoadListener?
        synchronized(loadRequestLock) {
            listener = requestResultListeners?.remove(uri)
        }
        if (listener != null) {
            if (e != null) {
                listener.onLoadFailed(e)
            } else {
                listener.onBitmapLoaded(uri, result)
            }

            CropIwaLog.d("{%s} loading completed, listener got the result", uri.toString())
        } else {
            //There is no listener interested in this request, so nobody will take care of
            //cached image.
            removeIfCached(uri)

            CropIwaLog.d("{%s} loading completed, but there was no listeners", uri.toString())
        }
    }

    @Throws(IOException::class)
    internal fun loadToMemory(context: Context, uri: Uri, width: Int, height: Int): Bitmap? {
        val localResUri = toLocalUri(context, uri)
        val options = getBitmapFactoryOptions(context, localResUri, width, height)

        val result = tryLoadBitmap(context, localResUri, options)

        if (result != null) {
            CropIwaLog.d("loaded image with dimensions {width=%d, height=%d}",
                    result.width,
                    result.height)
        }

        return result
    }

    @Throws(FileNotFoundException::class)
    private fun tryLoadBitmap(context: Context, uri: Uri, options: BitmapFactory.Options): Bitmap? {
        var result: Bitmap?
        while (true) {
            val `is` = context.contentResolver.openInputStream(uri)
            try {
                result = BitmapFactory.decodeStream(`is`, null, options)
            } catch (error: OutOfMemoryError) {
                if (options.inSampleSize < 64) {
                    options.inSampleSize *= 2
                    continue
                } else {
                    return null
                }
            }

            return ensureCorrectRotation(context, uri, result)
        }
    }

    @Throws(IOException::class)
    private fun toLocalUri(context: Context, uri: Uri): Uri {
        if (isWebUri(uri)) {
            var cached: File? = localCache[uri]
            if (cached == null) {
                cached = cacheLocally(context, uri)
                localCache[uri] = cached
            }
            return Uri.fromFile(cached)
        } else {
            return uri
        }
    }

    @Throws(IOException::class)
    private fun cacheLocally(context: Context, input: Uri): File {
        val local = File(context.externalCacheDir, generateLocalTempFileName(input))
        val url = URL(input.toString())
        var bis: BufferedInputStream? = null
        var bos: BufferedOutputStream? = null
        try {
            val buffer = ByteArray(1024)
            bis = BufferedInputStream(url.openStream())
            bos = BufferedOutputStream(FileOutputStream(local))
            val read: Int = bis.read(buffer)
            while (read != -1) {
                bos.write(buffer, 0, read)
            }
            bos.flush()
        } finally {
            closeSilently(bis)
            closeSilently(bos)
        }
        CropIwaLog.d("cached {%s} as {%s}", input.toString(), local.absolutePath)
        return local
    }

    @Throws(FileNotFoundException::class)
    private fun getBitmapFactoryOptions(c: Context, uri: Uri, width: Int, height: Int): BitmapFactory.Options {
        if (width != SIZE_UNSPECIFIED && height != SIZE_UNSPECIFIED) {
            return getOptimalSizeOptions(c, uri, width, height)
        } else {
            val options = BitmapFactory.Options()
            options.inSampleSize = 1
            return options
        }
    }

    private fun isWebUri(uri: Uri): Boolean {
        val scheme = uri.scheme
        return "http" == scheme || "https" == scheme
    }

    private fun generateLocalTempFileName(uri: Uri): String {
        return "temp_" + uri.lastPathSegment + "_" + System.currentTimeMillis()
    }

    interface BitmapLoadListener {
        fun onBitmapLoaded(uri: Uri, bitmap: Bitmap)

        fun onLoadFailed(e: Throwable)
    }

    companion object {

        private val INSTANCE = CropIwaBitmapManager()

        val SIZE_UNSPECIFIED = -1

        fun get(): CropIwaBitmapManager {
            return INSTANCE
        }

        @Throws(FileNotFoundException::class)
        private fun getOptimalSizeOptions(
                context: Context, bitmapUri: Uri,
                reqWidth: Int, reqHeight: Int): BitmapFactory.Options {
            val `is` = context.contentResolver.openInputStream(bitmapUri)
            val result = BitmapFactory.Options()
            result.inJustDecodeBounds = true
            BitmapFactory.decodeStream(`is`, null, result)
            result.inJustDecodeBounds = false
            result.inSampleSize = calculateInSampleSize(result, reqWidth, reqHeight)
            return result
        }

        private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }

        private fun ensureCorrectRotation(context: Context, uri: Uri, bitmap: Bitmap?): Bitmap? {
            val degrees = exifToDegrees(extractExifOrientation(context, uri))
            if (degrees != 0) {
                val matrix = Matrix()
                matrix.preRotate(degrees.toFloat())
                return transformBitmap(bitmap!!, matrix)
            }
            return bitmap
        }

        private fun transformBitmap(bitmap: Bitmap, transformMatrix: Matrix): Bitmap {
            var result = bitmap
            try {
                val converted = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.width, bitmap.height,
                        transformMatrix,
                        true)
                if (!bitmap.sameAs(converted)) {
                    result = converted
                    bitmap.recycle()
                }
            } catch (error: OutOfMemoryError) {
                CropIwaLog.e(error.message, error)
            }

            return result
        }

        private fun extractExifOrientation(context: Context, imageUri: Uri): Int {
            var `is`: InputStream? = null
            try {
                `is` = context.contentResolver.openInputStream(imageUri)
                return if (`is` == null) {
                    ExifInterface.ORIENTATION_UNDEFINED
                } else ImageHeaderParser(`is`).orientation
            } catch (e: IOException) {
                CropIwaLog.e(e.message, e)
                return ExifInterface.ORIENTATION_UNDEFINED
            } finally {
                closeSilently(`is`)
            }
        }

        private fun exifToDegrees(exifOrientation: Int): Int {
            val rotation: Int
            when (exifOrientation) {
                ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSPOSE -> rotation = 90
                ExifInterface.ORIENTATION_ROTATE_180, ExifInterface.ORIENTATION_FLIP_VERTICAL -> rotation = 180
                ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSVERSE -> rotation = 270
                else -> rotation = 0
            }
            return rotation
        }
    }
}
