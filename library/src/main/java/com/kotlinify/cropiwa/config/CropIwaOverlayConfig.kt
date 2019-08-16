package com.kotlinify.cropiwa.config

import android.content.Context
import android.content.res.TypedArray
import androidx.annotation.FloatRange
import android.util.AttributeSet

import com.kotlinify.cropiwa.AspectRatio
import com.kotlinify.cropiwa.R
import com.kotlinify.cropiwa.shape.CropIwaOvalShape
import com.kotlinify.cropiwa.shape.CropIwaRectShape
import com.kotlinify.cropiwa.shape.CropIwaShape
import com.kotlinify.cropiwa.util.ResUtil

import java.util.ArrayList

/**
 * @author yarolegovich https://github.com/yarolegovich
 * 04.02.2017.
 */
class CropIwaOverlayConfig {

    internal var overlayColor: Int = 0

    private var borderColor: Int = 0
    private var cornerColor: Int = 0
    private var gridColor: Int = 0
    private var borderStrokeWidth: Int = 0

    private var cornerStrokeWidth: Int = 0
    private var gridStrokeWidth: Int = 0

    internal var minHeight: Int = 0
    internal var minWidth: Int = 0

    internal var aspectRatio: AspectRatio? = null

    internal var cropScale: Float = 0.toFloat()

    internal var isDynamicCrop: Boolean = false
    private var shouldDrawGrid: Boolean = false
    internal var cropShape: CropIwaShape? = null

    private val listeners: MutableList<ConfigChangeListener>
    private val iterationList: MutableList<ConfigChangeListener>

    init {
        listeners = ArrayList()
        iterationList = ArrayList()
    }

    fun getOverlayColor(): Int {
        return overlayColor
    }

    fun getBorderColor(): Int {
        return borderColor
    }

    fun getCornerColor(): Int {
        return cornerColor
    }

    fun getBorderStrokeWidth(): Int {
        return borderStrokeWidth
    }

    fun getCornerStrokeWidth(): Int {
        return cornerStrokeWidth
    }

    fun getMinHeight(): Int {
        return minHeight
    }

    fun getMinWidth(): Int {
        return minWidth
    }

    fun getGridColor(): Int {
        return gridColor
    }

    fun getGridStrokeWidth(): Int {
        return gridStrokeWidth
    }

    fun shouldDrawGrid(): Boolean {
        return shouldDrawGrid
    }

    fun getCropShape(): CropIwaShape? {
        return cropShape
    }

    fun isDynamicCrop(): Boolean {
        return isDynamicCrop
    }

    fun getCropScale(): Float {
        return cropScale
    }

    fun getAspectRatio(): AspectRatio? {
        return aspectRatio
    }

    fun setOverlayColor(overlayColor: Int): CropIwaOverlayConfig {
        this.overlayColor = overlayColor
        return this
    }

    fun setBorderColor(borderColor: Int): CropIwaOverlayConfig {
        this.borderColor = borderColor
        return this
    }

    fun setCornerColor(cornerColor: Int): CropIwaOverlayConfig {
        this.cornerColor = cornerColor
        return this
    }

    fun setGridColor(gridColor: Int): CropIwaOverlayConfig {
        this.gridColor = gridColor
        return this
    }

    fun setBorderStrokeWidth(borderStrokeWidth: Int): CropIwaOverlayConfig {
        this.borderStrokeWidth = borderStrokeWidth
        return this
    }

    fun setCornerStrokeWidth(cornerStrokeWidth: Int): CropIwaOverlayConfig {
        this.cornerStrokeWidth = cornerStrokeWidth
        return this
    }

    fun setGridStrokeWidth(gridStrokeWidth: Int): CropIwaOverlayConfig {
        this.gridStrokeWidth = gridStrokeWidth
        return this
    }

    fun setCropScale(@FloatRange(from = 0.01, to = 1.0) cropScale: Float): CropIwaOverlayConfig {
        this.cropScale = cropScale
        return this
    }

    fun setMinHeight(minHeight: Int): CropIwaOverlayConfig {
        this.minHeight = minHeight
        return this
    }

    fun setMinWidth(minWidth: Int): CropIwaOverlayConfig {
        this.minWidth = minWidth
        return this
    }

    fun setAspectRatio(ratio: AspectRatio): CropIwaOverlayConfig {
        this.aspectRatio = ratio
        return this
    }

    fun setShouldDrawGrid(shouldDrawGrid: Boolean): CropIwaOverlayConfig {
        this.shouldDrawGrid = shouldDrawGrid
        return this
    }

    fun setCropShape(cropShape: CropIwaShape): CropIwaOverlayConfig {
        this.cropShape?.let { removeConfigChangeListener(it) }
        this.cropShape = cropShape
        return this
    }

    fun setDynamicCrop(enabled: Boolean): CropIwaOverlayConfig {
        this.isDynamicCrop = enabled
        return this
    }

    fun addConfigChangeListener(listener: ConfigChangeListener?) {
        if (listener != null) {
            listeners.add(listener)
        }
    }

    fun removeConfigChangeListener(listener: ConfigChangeListener) {
        listeners.remove(listener)
    }

    fun apply() {
        iterationList.addAll(listeners)
        for (listener in iterationList) {
            listener.onConfigChanged()
        }
        iterationList.clear()
    }

    companion object {

        private val DEFAULT_CROP_SCALE = 0.8f

        fun createDefault(context: Context): CropIwaOverlayConfig {
            val r = ResUtil(context)
            val config = CropIwaOverlayConfig()
                    .setBorderColor(r.color(R.color.cropiwa_default_border_color))
                    .setCornerColor(r.color(R.color.cropiwa_default_corner_color))
                    .setGridColor(r.color(R.color.cropiwa_default_grid_color))
                    .setOverlayColor(r.color(R.color.cropiwa_default_overlay_color))
                    .setBorderStrokeWidth(r.dimen(R.dimen.cropiwa_default_border_stroke_width))
                    .setCornerStrokeWidth(r.dimen(R.dimen.cropiwa_default_corner_stroke_width))
                    .setCropScale(DEFAULT_CROP_SCALE)
                    .setGridStrokeWidth(r.dimen(R.dimen.cropiwa_default_grid_stroke_width))
                    .setMinWidth(r.dimen(R.dimen.cropiwa_default_min_width))
                    .setMinHeight(r.dimen(R.dimen.cropiwa_default_min_height))
                    .setAspectRatio(AspectRatio(2, 1))
                    .setShouldDrawGrid(true)
                    .setDynamicCrop(true)
            val shape = CropIwaRectShape(config)
            config.setCropShape(shape)
            return config
        }

        fun createFromAttributes(context: Context, attrs: AttributeSet?): CropIwaOverlayConfig {
            val c = CropIwaOverlayConfig.createDefault(context)
            if (attrs == null) {
                return c
            }
            val ta = context.obtainStyledAttributes(attrs, R.styleable.CropIwaView)
            try {
                c.setMinWidth(ta.getDimensionPixelSize(
                        R.styleable.CropIwaView_ci_min_crop_width,
                        c.getMinWidth()))
                c.setMinHeight(ta.getDimensionPixelSize(
                        R.styleable.CropIwaView_ci_min_crop_height,
                        c.getMinHeight()))
                c.setAspectRatio(AspectRatio(
                        ta.getInteger(R.styleable.CropIwaView_ci_aspect_ratio_w, 1),
                        ta.getInteger(R.styleable.CropIwaView_ci_aspect_ratio_h, 1)))
                c.setCropScale(ta.getFloat(
                        R.styleable.CropIwaView_ci_crop_scale,
                        c.getCropScale()))
                c.setBorderColor(ta.getColor(
                        R.styleable.CropIwaView_ci_border_color,
                        c.getBorderColor()))
                c.setBorderStrokeWidth(ta.getDimensionPixelSize(
                        R.styleable.CropIwaView_ci_border_width,
                        c.getBorderStrokeWidth()))
                c.setCornerColor(ta.getColor(
                        R.styleable.CropIwaView_ci_corner_color,
                        c.getCornerColor()))
                c.setCornerStrokeWidth(ta.getDimensionPixelSize(
                        R.styleable.CropIwaView_ci_corner_width,
                        c.getCornerStrokeWidth()))
                c.setGridColor(ta.getColor(
                        R.styleable.CropIwaView_ci_grid_color,
                        c.getGridColor()))
                c.setGridStrokeWidth(ta.getDimensionPixelSize(
                        R.styleable.CropIwaView_ci_grid_width,
                        c.getGridStrokeWidth()))
                c.setShouldDrawGrid(ta.getBoolean(
                        R.styleable.CropIwaView_ci_draw_grid,
                        c.shouldDrawGrid()))
                c.setOverlayColor(ta.getColor(
                        R.styleable.CropIwaView_ci_overlay_color,
                        c.getOverlayColor()))
                c.setCropShape(if (ta.getInt(R.styleable.CropIwaView_ci_crop_shape, 0) == 0)
                    CropIwaRectShape(c)
                else
                    CropIwaOvalShape(c))
                c.setDynamicCrop(ta.getBoolean(
                        R.styleable.CropIwaView_ci_dynamic_aspect_ratio,
                        c.isDynamicCrop()))
            } finally {
                ta.recycle()
            }
            return c
        }
    }
}
