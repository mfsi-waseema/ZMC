package com.ziylanmedya.zmckit

import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.ziylanmedya.zmckit.camera.ZMCameraActivity

class ZMCKitManager private constructor(private val activity: AppCompatActivity) {

    // Activity result launcher for handling camera activity result
    private lateinit var captureLauncher: ActivityResultLauncher<ZMCameraActivity.Configuration>

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


//    companion object {
//        private var lensChangeListener: ((String) -> Unit)? = null
//
//        /**
//         * Registers a listener to handle lens changes.
//         * @param listener Callback function to be invoked with the selected lens ID.
//         */
//        fun onLensChange(listener: (String) -> Unit) {
//            lensChangeListener = listener
//        }
//
//        /**
//         * Notifies the registered listener about the selected lens ID.
//         * @param lensId The ID of the selected lens.
//         */
//        internal fun notifyLensChange(lensId: String) {
//            println(lensId)
//            lensChangeListener?.invoke(lensId)
//        }
//    }

    // Callback interface for result handling
    interface CaptureCallback {
        fun onImageCaptured(uri: String)
        fun onVideoCaptured(uri: String)
        fun onCaptureCancelled()
        fun onCaptureFailure(exception: Exception)
    }

    // Initialize the capture launcher
    fun initCaptureLauncher(callback: CaptureCallback) {
        captureLauncher = activity.registerForActivityResult(ZMCameraActivity.Capture) { result ->
            when (result) {
                is ZMCameraActivity.Capture.Result.Success.Video -> {
                    callback.onVideoCaptured(result.uri.toString())
                }

                is ZMCameraActivity.Capture.Result.Success.Image -> {
                    callback.onImageCaptured(result.uri.toString())
                }

                is ZMCameraActivity.Capture.Result.Cancelled -> {
                    callback.onCaptureCancelled()
                }

                is ZMCameraActivity.Capture.Result.Failure -> {
                    callback.onCaptureFailure(result.exception)
                }
            }
        }
    }

    /**
     * Launches the camera in single product mode to display single lens.
     *
     * @param snapAPIToken The API token for authentication with Snap Camera Kit.
     * @param partnerGroupId The unique ID for the partner lens group.
     * @param lensId The unique ID for the lens.
     * @param cameraFacingFront Whether the camera should default to front-facing.
     * @param cameraFacingFlipEnabled Whether the camera flip functionality should be enabled.
     */
    fun showProductView(
        snapAPIToken: String,
        partnerGroupId: String,
        lensId: String,
        cameraFacingFront : Boolean = false,
        cameraFacingFlipEnabled: Boolean = false)
    {
        // Create the camera configuration for a single product view
        val lensGroup = arrayOf(partnerGroupId) // Use the lensId for a single product

        val configuration = ZMCameraActivity.Configuration.WithLens(
            cameraKitApiToken = snapAPIToken,  // Using the passed API token
            lensGroupId = lensGroup.first(),  // Set of lens IDs for single or multiple lenses
            lensId = lensId,
            displayLensIcon = false,
            cameraAdjustmentsConfiguration = ZMCameraActivity.AdjustmentsConfiguration(
                toneAdjustmentEnabled = false,
                portraitAdjustmentEnabled = false
            ),
            cameraFacingFront = cameraFacingFront,
            cameraFacingFlipEnabled = cameraFacingFlipEnabled,
            cameraFacingBasedOnLens = false,
            cameraFlashEnabled = false
        )

        captureLauncher.launch(configuration)

        // Start CameraActivity to trigger it directly
        val cameraIntent = Intent(activity, ZMCameraActivity::class.java)
        activity.startActivity(cameraIntent)
    }

    /**
     * Launches the camera in group mode to display a lens group view.
     *
     * @param snapAPIToken The API token for authentication with Snap Camera Kit.
     * @param partnerGroupId The unique ID for the partner lens group.
     * @param cameraFacingFront Whether the camera should default to front-facing.
     * @param cameraFacingFlipEnabled Whether the camera flip functionality should be enabled.
     */
    fun showGroupView(
        snapAPIToken: String,
        partnerGroupId: String,
        cameraFacingFront : Boolean = false,
        cameraFacingFlipEnabled: Boolean = false
    ) {
        val lensGroup = arrayOf(partnerGroupId) // Use the lensId for a single product
        // Use lensGroupIds for a group view (multiple lenses)
        val configuration = ZMCameraActivity.Configuration.WithLenses(
            cameraKitApiToken = snapAPIToken,  // Using the passed API token
            lensGroupIds = lensGroup.toSet(),  // Set of lens IDs for single or multiple lenses
            cameraAdjustmentsConfiguration = ZMCameraActivity.AdjustmentsConfiguration(
                toneAdjustmentEnabled = false,
                portraitAdjustmentEnabled = false
            ),
            cameraFacingFront = cameraFacingFront,
            cameraFacingFlipEnabled = cameraFacingFlipEnabled,
            cameraFacingBasedOnLens = false,
            cameraFlashEnabled = false
        )

        captureLauncher.launch(configuration)

        // Start CameraActivity to trigger it directly
        val cameraIntent = Intent(activity, ZMCameraActivity::class.java)
        activity.startActivity(cameraIntent)
    }
}