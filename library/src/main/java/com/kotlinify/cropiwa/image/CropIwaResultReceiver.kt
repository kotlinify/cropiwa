package com.kotlinify.cropiwa.image

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable

/**
 * @author yarolegovich
 * 25.02.2017.
 */
class CropIwaResultReceiver : BroadcastReceiver() {

    private var listener: Listener? = null

    override fun onReceive(context: Context, intent: Intent) {
        val extras = intent.extras
        if (listener != null) {
            if (extras!!.containsKey(EXTRA_ERROR)) {
                listener!!.onCropFailed(extras.getSerializable(EXTRA_ERROR) as Throwable)
            } else if (extras.containsKey(EXTRA_URI)) {
                listener!!.onCropSuccess(extras.getParcelable<Parcelable>(EXTRA_URI) as Uri)
            }
        }
    }

    fun register(context: Context) {
        val filter = IntentFilter(ACTION_CROP_COMPLETED)
        context.registerReceiver(this, filter)
    }

    fun unregister(context: Context) {
        context.unregisterReceiver(this)
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    interface Listener {
        fun onCropSuccess(croppedUri: Uri)

        fun onCropFailed(e: Throwable)
    }

    companion object {

        private val ACTION_CROP_COMPLETED = "cropIwa_action_crop_completed"
        private val EXTRA_ERROR = "extra_error"
        private val EXTRA_URI = "extra_uri"

        fun onCropCompleted(context: Context, croppedImageUri: Uri) {
            val intent = Intent(ACTION_CROP_COMPLETED)
            intent.putExtra(EXTRA_URI, croppedImageUri)
            context.sendBroadcast(intent)
        }

        fun onCropFailed(context: Context, e: Throwable) {
            val intent = Intent(ACTION_CROP_COMPLETED)
            intent.putExtra(EXTRA_ERROR, e)
            context.sendBroadcast(intent)
        }
    }
}
