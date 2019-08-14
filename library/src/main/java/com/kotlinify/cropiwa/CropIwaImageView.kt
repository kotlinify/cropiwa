package com.kotlinify.cropiwa

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Handler
import androidx.annotation.FloatRange
import androidx.appcompat.widget.AppCompatImageView
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView

import com.kotlinify.cropiwa.config.ConfigChangeListener
import com.kotlinify.cropiwa.config.CropIwaImageViewConfig
import com.kotlinify.cropiwa.config.InitialPosition
import com.kotlinify.cropiwa.util.CropIwaUtils
import com.kotlinify.cropiwa.util.MatrixUtils
import com.kotlinify.cropiwa.util.TensionInterpolator

/**
 * @author Yaroslav Polyakov https://github.com/polyak01
 * 03.02.2017.
 */
@SuppressLint("ViewConstructor")
internal class CropIwaImageView(context: Context, config: CropIwaImageViewConfig) : AppCompatImageView(context), OnNewBoundsListener, ConfigChangeListener {

    private var iMatrix: Matrix? = null
    private var matrixUtils: MatrixUtils? = null
    var imageTransformGestureDetector: GestureProcessor? = null
        private set

    private var allowedBounds: RectF? = null
    private var imageBounds: RectF? = null
    private var realImageBounds: RectF? = null

    private var imagePositionedListener: OnImagePositionedListener? = null

    private var config: CropIwaImageViewConfig? = null

    private val realImageWidth: Int
        get() {
            val image = drawable
            return image?.intrinsicWidth ?: -1
        }

    private val realImageHeight: Int
        get() {
            val image = drawable
            return image?.intrinsicHeight ?: -1
        }

    val imageWidth: Int
        get() = imageBounds!!.width().toInt()

    val imageHeight: Int
        get() = imageBounds!!.height().toInt()

    val imageRect: RectF
        get() {
            updateImageBounds()
            return RectF(imageBounds)
        }

    private val currentScalePercent: Float
        get() = CropIwaUtils.boundValue(
                0.01f + (matrixUtils!!.getScaleX(iMatrix) - config!!.minScale) / config!!.maxScale,
                0.01f, 1f)

    init {
        initWith(config)
    }

    private fun initWith(c: CropIwaImageViewConfig) {
        config = c
        config!!.addConfigChangeListener(this)

        imageBounds = RectF()
        allowedBounds = RectF()
        realImageBounds = RectF()

        matrixUtils = MatrixUtils()

        iMatrix = Matrix()
        scaleType = ImageView.ScaleType.MATRIX

        imageTransformGestureDetector = GestureProcessor()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (hasImageSize()) {
            placeImageToInitialPosition()
        }
    }

    private fun placeImageToInitialPosition() {
        updateImageBounds()
        moveImageToTheCenter()
        if (config!!.scale == CropIwaImageViewConfig.SCALE_UNSPECIFIED.toFloat()) {
            when (config!!.getImageInitialPosition()) {
                InitialPosition.CENTER_CROP -> resizeImageToFillTheView()
                InitialPosition.CENTER_INSIDE -> resizeImageToBeInsideTheView()
            }
            config!!.setScale(currentScalePercent).apply()
        } else {
            setScalePercent(config!!.scale)
        }
        notifyImagePositioned()
    }

    private fun resizeImageToFillTheView() {
        val scale: Float
        if (width < height) {
            scale = height.toFloat() / imageHeight
        } else {
            scale = width.toFloat() / imageWidth
        }
        scaleImage(scale + 10)
    }

    private fun resizeImageToBeInsideTheView() {
        val scale: Float
        if (imageWidth < imageHeight) {
            scale = height.toFloat() / imageHeight
        } else {
            scale = width.toFloat() / imageWidth
        }
        scaleImage(scale + 10)
    }

    private fun moveImageToTheCenter() {
        updateImageBounds()
        val deltaX = width / 2f - imageBounds!!.centerX()
        val deltaY = height / 2f - imageBounds!!.centerY()
        translateImage(deltaX, deltaY)
    }

    private fun calculateMinScale(): Float {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (realImageWidth <= viewWidth && realImageHeight <= viewHeight) {
            return config!!.minScale
        }
        val scaleFactor = if (viewWidth < viewHeight)
            viewWidth / realImageWidth
        else
            viewHeight / realImageHeight
        return scaleFactor * 0.8f
    }

    fun hasImageSize(): Boolean {
        return realImageWidth != -1 && realImageHeight != -1
    }

    override fun onNewBounds(bounds: RectF) {
        //        updateImageBounds();
        allowedBounds!!.set(bounds)
        if (hasImageSize()) {
            val handler = Handler()
            val thread = Thread(Runnable { handler.post { animateToAllowedBounds() } })
            thread.priority = Thread.MAX_PRIORITY
            thread.start()
            //            post(new Runnable() {
            //                @Override
            //                public void run() {
            //                    animateToAllowedBounds();
            //                }
            //            });

        }
    }

    private fun animateToAllowedBounds() {

        val endMatrix = MatrixUtils.findTransformToAllowedBounds(
                realImageBounds, iMatrix,
                allowedBounds!!)
        //        MatrixAnimator animator = new MatrixAnimator();
        //        animator.animate(iMatrix, endMatrix, new ValueAnimator.AnimatorUpdateListener() {
        //            @Override
        //            public void onAnimationUpdate(ValueAnimator animation) {
        //                iMatrix.set((Matrix) animation.getAnimatedValue());
        //                setImageMatrix(iMatrix);
        //                updateImageBounds();
        //                invalidate();
        //            }
        //        });
        iMatrix!!.set(endMatrix)
        setImageMatrix(iMatrix)
        updateImageBounds()
        invalidate()
    }

    private fun setScalePercent(@FloatRange(from = 0.01, to = 1.0) percent: Float) {
        var percent = percent
        percent = Math.min(Math.max(0.01f, percent), 1f)
        val desiredScale = config!!.minScale + config!!.maxScale * percent
        val currentScale = matrixUtils!!.getScaleX(iMatrix)
        val factor = desiredScale / currentScale
        scaleImage(factor)
        invalidate()
    }

    private fun scaleImage(factor: Float) {
        updateImageBounds()
        scaleImage(factor, imageBounds!!.centerX(), imageBounds!!.centerY())
    }

    private fun scaleImage(factor: Float, pivotX: Float, pivotY: Float) {
        iMatrix!!.postScale(factor, factor, pivotX, pivotY)
        setImageMatrix(iMatrix)
        updateImageBounds()
    }

    private fun translateImage(deltaX: Float, deltaY: Float) {
        iMatrix!!.postTranslate(deltaX, deltaY)
        setImageMatrix(iMatrix)
        if (deltaX > 0.01f || deltaY > 0.01f) {
            updateImageBounds()
        }
    }

    private fun updateImageBounds() {
        realImageBounds!!.set(0f, 0f, realImageWidth.toFloat(), realImageHeight.toFloat())
        imageBounds!!.set(realImageBounds!!)
        iMatrix!!.mapRect(imageBounds)
    }

    override fun onConfigChanged() {
        if (Math.abs(currentScalePercent - config!!.scale) > 0.001f) {
            setScalePercent(config!!.scale)
            animateToAllowedBounds()
        }
    }

    fun setImagePositionedListener(imagePositionedListener: OnImagePositionedListener) {
        this.imagePositionedListener = imagePositionedListener
        if (hasImageSize()) {
            updateImageBounds()
            notifyImagePositioned()
        }
    }

    fun notifyImagePositioned() {
        if (imagePositionedListener != null) {
            val imageRect = RectF(imageBounds)
            CropIwaUtils.constrainRectTo(0, 0, width, height, imageRect)
            imagePositionedListener!!.onImagePositioned(imageRect)
        }
    }

    private inner class ScaleGestureListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val newScale = matrixUtils!!.getScaleX(iMatrix) * scaleFactor
            if (isValidScale(newScale)) {
                scaleImage(scaleFactor, detector.focusX, detector.focusY)
                config!!.setScale(currentScalePercent).apply()
            }
            return true
        }

        private fun isValidScale(newScale: Float): Boolean {
            return newScale >= config!!.minScale && newScale <= config!!.minScale + config!!.maxScale
        }
    }

    private inner class TranslationGestureListener {

        private var prevX: Float = 0.toFloat()
        private var prevY: Float = 0.toFloat()
        private var id: Int = 0

        private val interpolator = TensionInterpolator()

        fun onDown(e: MotionEvent) {
            onDown(e.x, e.y, e.getPointerId(0))
        }

        private fun onDown(x: Float, y: Float, id: Int) {
            updateImageBounds()
            interpolator.onDown(x, y, imageBounds, allowedBounds)
            saveCoordinates(x, y, id)
        }

        fun onTouchEvent(e: MotionEvent, canHandle: Boolean) {
            when (e.actionMasked) {
                MotionEvent.ACTION_POINTER_UP -> {
                    onPointerUp(e)
                    return
                }
                MotionEvent.ACTION_MOVE -> {
                }
                else -> return
            }

            val index = e.findPointerIndex(id)

            updateImageBounds()

            val currentX = interpolator.interpolateX(e.getX(index))
            val currentY = interpolator.interpolateY(e.getY(index))

            if (canHandle) {
                translateImage(currentX - prevX, currentY - prevY)
            }

            saveCoordinates(currentX, currentY)
        }

        private fun onPointerUp(e: MotionEvent) {
            //If user lifted finger that we used to calculate translation, we need to find a new one
            if (e.getPointerId(e.actionIndex) == id) {
                var index = 0
                while (index < e.pointerCount && index == e.actionIndex) {
                    index++
                }
                onDown(e.getX(index), e.getY(index), e.getPointerId(index))
            }
        }

        private fun saveCoordinates(x: Float, y: Float, id: Int = 0) {
            this.prevX = x
            this.prevY = y
            this.id = id
        }
    }

    inner class GestureProcessor {

        private val scaleDetector: ScaleGestureDetector
        private val translationGestureListener: TranslationGestureListener

        init {
            scaleDetector = ScaleGestureDetector(context, ScaleGestureListener())
            translationGestureListener = TranslationGestureListener()
        }

        fun onDown(event: MotionEvent) {
            translationGestureListener.onDown(event)
        }

        fun onTouchEvent(event: MotionEvent) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> return
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                    animateToAllowedBounds()
                    return
                }
            }
            if (config!!.isImageScaleEnabled()) {
                scaleDetector.onTouchEvent(event)
            }

            if (config!!.isImageTranslationEnabled()) {
                //We don't want image translation while scaling gesture is in progress
                //so - canHandle if scaleDetector.isNotInProgress
                translationGestureListener.onTouchEvent(event, !scaleDetector.isInProgress)
            }
        }
    }

}
