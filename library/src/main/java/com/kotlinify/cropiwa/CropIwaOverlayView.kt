package com.kotlinify.cropiwa

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

import com.kotlinify.cropiwa.config.ConfigChangeListener
import com.kotlinify.cropiwa.config.CropIwaOverlayConfig
import com.kotlinify.cropiwa.shape.CropIwaShape

/**
 * @author Yaroslav Polyakov https://github.com/polyak01
 * 03.02.2017.
 */
@SuppressLint("ViewConstructor")
internal open class CropIwaOverlayView(context: Context, config: CropIwaOverlayConfig) : View(context), ConfigChangeListener, OnImagePositionedListener {

    private var overlayPaint: Paint? = null
    private var newBoundsListener: OnNewBoundsListener? = null
    private var cropShape: CropIwaShape? = null

    private var cropScale: Float = 0.toFloat()

    private var imageBounds: RectF? = null

    lateinit var cropRect: RectF

    lateinit var config: CropIwaOverlayConfig


    var isDrawn: Boolean = false

    private val isValidCrop: Boolean
        get() = cropRect.width() >= config.minWidth && cropRect.height() >= config.minHeight

    open val isResizing: Boolean
        get() = false

    open val isDraggingCropArea: Boolean
        get() = false

    private val aspectRatio: AspectRatio?
        get() {
            var aspectRatio = config.aspectRatio
            if (aspectRatio === AspectRatio.IMG_SRC) {
                if (imageBounds!!.width() == 0f || imageBounds!!.height() == 0f) {
                    return null
                }
                aspectRatio = AspectRatio(
                        Math.round(imageBounds!!.width()),
                        Math.round(imageBounds!!.height()))
            }
            return aspectRatio
        }

    init {
        initWith(config)
    }

    protected open fun initWith(c: CropIwaOverlayConfig) {
        config = c
        config.addConfigChangeListener(this)

        imageBounds = RectF()
        cropScale = config.cropScale
        cropShape = c.cropShape

        cropRect = RectF()

        overlayPaint = Paint()
        overlayPaint!!.style = Paint.Style.FILL
        overlayPaint!!.color = c.overlayColor

        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    override fun onImagePositioned(imageRect: RectF) {
        imageBounds!!.set(imageRect)
        setCropRectAccordingToAspectRatio()
        notifyNewBounds()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        //We will get here measured dimensions of an ImageView
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return true
    }

    override fun onDraw(canvas: Canvas) {
        if (isDrawn) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint!!)
            if (isValidCrop) {
                cropShape!!.draw(canvas, cropRect)
            }
        }
    }

    protected fun notifyNewBounds() {
        if (newBoundsListener != null) {
            //Do not allow client code to modify our cropRect!
            val rect = RectF(cropRect)
            newBoundsListener!!.onNewBounds(rect)
        }
    }



    fun setDrawOverlay(shouldDraw: Boolean) {
        isDrawn = shouldDraw
        invalidate()
    }

    fun setNewBoundsListener(newBoundsListener: OnNewBoundsListener) {
        this.newBoundsListener = newBoundsListener
    }

    override fun onConfigChanged() {
        overlayPaint!!.color = config.overlayColor
        cropShape = config.cropShape
        cropScale = config.cropScale
        cropShape!!.onConfigChanged()
        setCropRectAccordingToAspectRatio()
        notifyNewBounds()
        invalidate()
    }

    private fun setCropRectAccordingToAspectRatio() {
        val viewWidth = measuredWidth.toFloat()
        val viewHeight = measuredHeight.toFloat()
        if (viewWidth == 0f || viewHeight == 0f) {
            return
        }

        val aspectRatio = aspectRatio ?: return

        if (cropRect.width() != 0f && cropRect.height() != 0f) {
            val currentRatio = cropRect.width() / cropRect.height()
            if (Math.abs(currentRatio - aspectRatio.ratio) < 0.001) {
                return
            }
        }

        val centerX = viewWidth * 0.5f
        val centerY = viewHeight * 0.5f
        val halfWidth: Float
        val halfHeight: Float

        val calculateFromWidth = aspectRatio.height < aspectRatio.width || aspectRatio.isSquare && viewWidth < viewHeight

        if (calculateFromWidth) {
            halfWidth = viewWidth * cropScale * 0.5f
            halfHeight = halfWidth / aspectRatio.ratio
        } else {
            halfHeight = viewHeight * cropScale * 0.5f
            halfWidth = halfHeight * aspectRatio.ratio
        }

        cropRect.set(
                centerX - halfWidth, centerY - halfHeight,
                centerX + halfWidth, centerY + halfHeight)
    }

}
