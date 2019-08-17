package com.kotlinify.cropiwa

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

import com.kotlinify.cropiwa.config.ConfigChangeListener
import com.kotlinify.cropiwa.config.CropIwaImageViewConfig
import com.kotlinify.cropiwa.config.CropIwaOverlayConfig
import com.kotlinify.cropiwa.config.CropIwaSaveConfig
import com.kotlinify.cropiwa.image.CropArea
import com.kotlinify.cropiwa.image.CropIwaBitmapManager
import com.kotlinify.cropiwa.image.CropIwaResultReceiver
import com.kotlinify.cropiwa.util.LoadBitmapCommand
import com.kotlinify.cropiwa.shape.CropIwaShapeMask
import com.kotlinify.cropiwa.util.CropIwaLog

import androidx.core.content.res.ResourcesCompat

/**
 * Created by yarolegovich on 02.02.2017.
 */
class CropIwaView : FrameLayout {

    private var imageView: CropIwaImageView? = null
    private var overlayView: CropIwaOverlayView? = null

    private var overlayConfig: CropIwaOverlayConfig? = null
    private var imageConfig: CropIwaImageViewConfig? = null

    private var gestureDetector: CropIwaImageView.GestureProcessor? = null

    private var imageUri: Uri? = null
    private var loadBitmapCommand: LoadBitmapCommand? = null

    private var errorListener: ErrorListener? = null
    private var cropSaveCompleteListener: CropSaveCompleteListener? = null

    private var cropIwaResultReceiver: CropIwaResultReceiver? = null

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        imageConfig = CropIwaImageViewConfig.createFromAttributes(context, attrs)
        initImageView()

        overlayConfig = CropIwaOverlayConfig.createFromAttributes(context, attrs)
        overlayConfig!!.addConfigChangeListener(ReInitOverlayOnResizeModeChange())
        initOverlayView()

        cropIwaResultReceiver = CropIwaResultReceiver()
        cropIwaResultReceiver!!.register(context)
        cropIwaResultReceiver!!.setListener(CropResultRouter())
    }

    private fun initImageView() {
        if (imageConfig == null) {
            throw IllegalStateException("imageConfig must be initialized before calling this method")
        }
        imageView = CropIwaImageView(context, imageConfig!!)
        imageView!!.setBackgroundColor(Color.WHITE)
        gestureDetector = imageView!!.imageTransformGestureDetector
        addView(imageView)
    }

    private fun initOverlayView() {
        if (imageView == null || overlayConfig == null) {
            throw IllegalStateException("imageView and overlayConfig must be initialized before calling this method")
        }
        overlayView = if (overlayConfig!!.isDynamicCrop)
            CropIwaDynamicOverlayView(context, overlayConfig!!)
        else
            CropIwaOverlayView(context, overlayConfig!!)
        overlayView?.setNewBoundsListener(imageView!!)
        imageView!!.setImagePositionedListener(overlayView!!)
        addView(overlayView)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (loadBitmapCommand != null) {
            loadBitmapCommand!!.setDimensions(w, h)
            loadBitmapCommand!!.tryExecute(context)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        //I think this "redundant" if statements improve code readability
        try {
            if (ev.action == MotionEvent.ACTION_DOWN) {
                gestureDetector!!.onDown(ev)
                return false
            }
            return !(overlayView!!.isResizing || overlayView!!.isDraggingCropArea)
        } catch (e: IllegalArgumentException) {
            //e.printStackTrace();
            return false
        }

    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return try {
            gestureDetector!!.onTouchEvent(event)
            super.onTouchEvent(event)
        } catch (e: IllegalArgumentException) {
            //e.printStackTrace();
            false
        }

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        imageView!!.measure(widthMeasureSpec, heightMeasureSpec)
        overlayView!!.measure(
                imageView!!.measuredWidthAndState,
                imageView!!.measuredHeightAndState)
        imageView!!.notifyImagePositioned()
        setMeasuredDimension(
                imageView!!.measuredWidthAndState,
                imageView!!.measuredHeightAndState)
    }

    override fun invalidate() {
        imageView!!.invalidate()
        overlayView!!.invalidate()
    }

    fun configureOverlay(): CropIwaOverlayConfig? {
        return overlayConfig
    }

    fun configureImage(): CropIwaImageViewConfig? {
        return imageConfig
    }

    fun setImageUri(uri: Uri) {
        imageUri = uri
        loadBitmapCommand = LoadBitmapCommand(
                uri, width, height,
                BitmapLoadListener())
        loadBitmapCommand!!.tryExecute(context)
    }

    fun setImage(bitmap: Bitmap) {
        imageView!!.setImageBitmap(bitmap)
        overlayView!!.setDrawOverlay(true)
    }

    fun crop(saveConfig: CropIwaSaveConfig) {
        val cropArea = CropArea.create(
                imageView!!.imageRect,
                imageView!!.imageRect,
                overlayView!!.cropRect)
        val mask = overlayConfig!!.cropShape?.mask
        if (mask != null) {
            imageUri?.let {
                CropIwaBitmapManager.get().crop(
                        context, cropArea, mask,
                        it, saveConfig)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (imageUri != null) {
            val loader = CropIwaBitmapManager.get()
            loader.unregisterLoadListenerFor(imageUri!!)
            loader.removeIfCached(imageUri!!)
        }
        if (cropIwaResultReceiver != null) {
            cropIwaResultReceiver!!.unregister(context)
        }
    }

    fun setErrorListener(errorListener: ErrorListener) {
        this.errorListener = errorListener
    }

    fun setCropSaveCompleteListener(cropSaveCompleteListener: CropSaveCompleteListener) {
        this.cropSaveCompleteListener = cropSaveCompleteListener
    }

    private inner class BitmapLoadListener : CropIwaBitmapManager.BitmapLoadListener {

        override fun onBitmapLoaded(imageUri: Uri, bitmap: Bitmap) {
            setImage(bitmap)
        }

        override fun onLoadFailed(e: Throwable) {
            CropIwaLog.e("CropIwa Image loading from [$imageUri] failed", e)
            overlayView!!.setDrawOverlay(false)
            if (errorListener != null) {
                errorListener!!.onError(e)
            }
        }
    }

    private inner class CropResultRouter : CropIwaResultReceiver.Listener {

        override fun onCropSuccess(croppedUri: Uri) {
            if (cropSaveCompleteListener != null) {
                cropSaveCompleteListener!!.onCroppedRegionSaved(croppedUri)
            }
        }

        override fun onCropFailed(e: Throwable) {
            if (errorListener != null) {
                errorListener!!.onError(e)
            }
        }
    }

    private inner class ReInitOverlayOnResizeModeChange : ConfigChangeListener {

        override fun onConfigChanged() {
            if (shouldReInit()) {
                overlayView?.let { overlayConfig!!.removeConfigChangeListener(it) }
                val shouldDrawOverlay = overlayView!!.isDrawn
                removeView(overlayView)

                initOverlayView()
                overlayView!!.setDrawOverlay(shouldDrawOverlay)

                invalidate()
            }
        }

        private fun shouldReInit(): Boolean {
            return overlayConfig!!.isDynamicCrop != overlayView is CropIwaDynamicOverlayView
        }
    }

    interface CropSaveCompleteListener {
        fun onCroppedRegionSaved(bitmapUri: Uri)
    }

    interface ErrorListener {
        fun onError(e: Throwable)
    }
}
