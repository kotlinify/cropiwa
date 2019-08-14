package com.kotlinify.cropiwa.config

import android.content.Context
import android.content.res.TypedArray
import androidx.annotation.FloatRange
import android.util.AttributeSet

import com.kotlinify.cropiwa.R

import java.util.ArrayList

/**
 * @author yarolegovich https://github.com/yarolegovich
 * 04.02.2017.
 */
class CropIwaImageViewConfig {

    internal var maxScale: Float = 0.toFloat()
    internal var minScale: Float = 0.toFloat()
    private var isScaleEnabled: Boolean = false
    private var isTranslationEnabled: Boolean = false
    internal var scale: Float = 0.toFloat()

    private var initialPosition: InitialPosition? = null

    private val configChangeListeners: MutableList<ConfigChangeListener>

    init {
        configChangeListeners = ArrayList()
    }

    fun getMaxScale(): Float {
        return maxScale
    }

    fun getMinScale(): Float {
        return minScale
    }

    fun isImageScaleEnabled(): Boolean {
        return isScaleEnabled
    }

    fun isImageTranslationEnabled(): Boolean {
        return isTranslationEnabled
    }

    fun getImageInitialPosition(): InitialPosition? {
        return initialPosition
    }

    fun getScale(): Float {
        return scale
    }

    fun setMinScale(@FloatRange(from = 0.001) minScale: Float): CropIwaImageViewConfig {
        this.minScale = minScale
        return this
    }

    fun setMaxScale(@FloatRange(from = 0.001) maxScale: Float): CropIwaImageViewConfig {

        this.maxScale = maxScale
        return this
    }

    fun setImageScaleEnabled(scaleEnabled: Boolean): CropIwaImageViewConfig {
        this.isScaleEnabled = scaleEnabled
        return this
    }

    fun setImageTranslationEnabled(imageTranslationEnabled: Boolean): CropIwaImageViewConfig {
        this.isTranslationEnabled = imageTranslationEnabled
        return this
    }

    fun setImageInitialPosition(initialPosition: InitialPosition): CropIwaImageViewConfig {
        this.initialPosition = initialPosition
        return this
    }

    fun setScale(@FloatRange(from = -1.0, to = 1.0) scale: Float): CropIwaImageViewConfig {
        this.scale = scale
        return this
    }

    fun addConfigChangeListener(configChangeListener: ConfigChangeListener?) {
        if (configChangeListener != null) {
            configChangeListeners.add(configChangeListener)
        }
    }

    fun removeConfigChangeListener(configChangeListener: ConfigChangeListener) {
        configChangeListeners.remove(configChangeListener)
    }

    fun apply() {
        for (listener in configChangeListeners) {
            listener.onConfigChanged()
        }
    }

    companion object {

        private val DEFAULT_MIN_SCALE = 0.7f
        private val DEFAULT_MAX_SCALE = 3f

        val SCALE_UNSPECIFIED = -1

        fun createDefault(): CropIwaImageViewConfig {
            return CropIwaImageViewConfig()
                    .setMaxScale(DEFAULT_MAX_SCALE)
                    .setMinScale(DEFAULT_MIN_SCALE)
                    .setImageTranslationEnabled(true)
                    .setImageScaleEnabled(true)
                    .setScale(SCALE_UNSPECIFIED.toFloat())
        }

        fun createFromAttributes(c: Context, attrs: AttributeSet?): CropIwaImageViewConfig {
            val config = createDefault()
            if (attrs == null) {
                return config
            }
            val ta = c.obtainStyledAttributes(attrs, R.styleable.CropIwaView)
            try {
                config.setMaxScale(ta.getFloat(
                        R.styleable.CropIwaView_ci_max_scale,
                        config.getMaxScale()))
                config.setImageTranslationEnabled(ta.getBoolean(
                        R.styleable.CropIwaView_ci_translation_enabled,
                        config.isImageTranslationEnabled()))
                config.setImageScaleEnabled(ta.getBoolean(
                        R.styleable.CropIwaView_ci_scale_enabled,
                        config.isImageScaleEnabled()))
                config.setImageInitialPosition(InitialPosition.values()[ta.getInt(R.styleable.CropIwaView_ci_initial_position, 0)])
            } finally {
                ta.recycle()
            }
            return config
        }
    }

}
