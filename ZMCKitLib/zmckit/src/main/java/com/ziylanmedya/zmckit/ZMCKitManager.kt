package com.ziylanmedya.zmckit

import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ziylanmedya.zmckit.camera.Constants.EXTRA_APPLY_LENS_ID
import com.ziylanmedya.zmckit.camera.Constants.EXTRA_CAMERA_FACING_FRONT
import com.ziylanmedya.zmckit.camera.Constants.EXTRA_CAMERA_KIT_API_TOKEN
import com.ziylanmedya.zmckit.camera.Constants.EXTRA_LENS_GROUP_IDS
import com.ziylanmedya.zmckit.camera.ZMCameraActivity

class ZMCKitManager private constructor(private val activity: AppCompatActivity) {

    // This is a callback that will be triggered on lens selection change
    private var lensChangeListener: ((String) -> Unit)? = null

    /**
     * Registers a listener to handle lens changes.
     * @param listener Callback function to be invoked with the selected lens ID.
     */
    fun onLensChange(listener: (String) -> Unit) {
        lensChangeListener = listener
    }

    // Private method to notify listeners
    internal fun notifyLensChange(lensId: String) {
        if (lensChangeListener == null) {
            Log.w("ZMCKitManager", "Lens change listener is not registered!")
        }

        lensChangeListener?.invoke(lensId)
    }

    companion object {
        // Singleton instance to hold the only instance of ZMCKitManager
        @Volatile
        private var INSTANCE: ZMCKitManager? = null

        // This function ensures only one instance of ZMCKitManager is created
        fun getInstance(activity: AppCompatActivity): ZMCKitManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ZMCKitManager(activity).also { INSTANCE = it }
            }
        }
    }

    /**
     * Launches the camera in single product mode to display single lens as Activity.
     *
     * @param snapAPIToken The API token for authentication with Snap Camera Kit.
     * @param partnerGroupId The unique ID for the partner lens group.
     * @param lensId The unique ID for the lens.
     * @param cameraFacingFront Whether the camera should default to front-facing.
     */
    fun showProductActivity(
        snapAPIToken: String,
        partnerGroupId: String,
        lensId: String,
        cameraFacingFront : Boolean = false)
    {
        // Create the camera for a lens
        val lensGroup = arrayOf(partnerGroupId).toSet().toTypedArray()

        // Start CameraActivity
        val cameraIntent = Intent(activity, ZMCameraActivity::class.java).apply {
            putExtra(EXTRA_CAMERA_KIT_API_TOKEN, snapAPIToken)
            putExtra(EXTRA_CAMERA_FACING_FRONT, cameraFacingFront)
            putExtra(EXTRA_LENS_GROUP_IDS, lensGroup)
            putExtra(EXTRA_APPLY_LENS_ID, lensId)

        }
        activity.startActivity(cameraIntent)
    }

    /**
     * Launches the camera in group mode to display a lens group view as Activity.
     *
     * @param snapAPIToken The API token for authentication with Snap Camera Kit.
     * @param partnerGroupId The unique ID for the partner lens group.
     * @param cameraFacingFront Whether the camera should default to front-facing.
     */
    fun showGroupActivity(
        snapAPIToken: String,
        partnerGroupId: String,
        cameraFacingFront : Boolean = false
    ) {
        // Create the camera for a multiple lenses view
        val lensGroup = arrayOf(partnerGroupId).toSet().toTypedArray()

        // Start CameraActivity
        val cameraIntent = Intent(activity, ZMCameraActivity::class.java).apply {
            putExtra(EXTRA_CAMERA_KIT_API_TOKEN, snapAPIToken)
            putExtra(EXTRA_CAMERA_FACING_FRONT, cameraFacingFront)
            putExtra(EXTRA_LENS_GROUP_IDS, lensGroup)
        }
        activity.startActivity(cameraIntent)
    }
}