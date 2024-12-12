package com.ziylanmedya.zmckit

import android.content.Context
import android.content.Intent
import com.ziylanmedya.zmckit.camera.Constants.EXTRA_APPLY_LENS_ID
import com.ziylanmedya.zmckit.camera.Constants.EXTRA_CAMERA_FACING_FRONT
import com.ziylanmedya.zmckit.camera.Constants.EXTRA_CAMERA_KIT_API_TOKEN
import com.ziylanmedya.zmckit.camera.Constants.EXTRA_LENS_CHANGE_LISTENER
import com.ziylanmedya.zmckit.camera.Constants.EXTRA_LENS_GROUP_IDS
import com.ziylanmedya.zmckit.camera.ZMCameraActivity
import com.ziylanmedya.zmckit.widgets.ZMCCameraLayout

class ZMCKitManager private constructor() {

    // Notify listeners when lens changes
    interface LensChangeListener : java.io.Serializable {
        fun onLensChange(lensId: String)
    }

    // ---- Full-Screen Activity Methods ----
    companion object {
        /**
         * Launches the camera in single product mode as a full-screen activity.
         */
        fun showProductActivity(
            context: Context,
            snapAPIToken: String,
            partnerGroupId: String,
            lensId: String,
            onLensChange: LensChangeListener,
            cameraFacingFront: Boolean = false
        ) {
            val intent = Intent(context, ZMCameraActivity::class.java).apply {
                putExtra(EXTRA_CAMERA_KIT_API_TOKEN, snapAPIToken)
                putExtra(EXTRA_CAMERA_FACING_FRONT, cameraFacingFront)
                putExtra(EXTRA_LENS_GROUP_IDS, arrayOf(partnerGroupId))
                putExtra(EXTRA_APPLY_LENS_ID, lensId)
                putExtra(EXTRA_LENS_CHANGE_LISTENER, onLensChange)
            }
            context.startActivity(intent)
        }

        /**
         * Launches the camera in group mode as a full-screen activity.
         */
        fun showGroupActivity(
            context: Context,
            snapAPIToken: String,
            partnerGroupId: String,
            onLensChange: LensChangeListener?,
            cameraFacingFront: Boolean = false
        ) {
            val intent = Intent(context, ZMCameraActivity::class.java).apply {
                putExtra(EXTRA_CAMERA_KIT_API_TOKEN, snapAPIToken)
                putExtra(EXTRA_CAMERA_FACING_FRONT, cameraFacingFront)
                putExtra(EXTRA_LENS_GROUP_IDS, arrayOf(partnerGroupId))
                putExtra(EXTRA_LENS_CHANGE_LISTENER, onLensChange)
            }
            context.startActivity(intent)
        }

        /**
         * Initializes and configures a ZMCCameraLayout for single product view.
         *
         * @param context the context to be used for creating the ZMCCameraLayout.
         * @param snapAPIToken the Snap API token for configuration.
         * @param partnerGroupId the group ID for the partner.
         * @param lensId the lens ID to be applied.
         * @param listener the lens change listener to be triggered when lens changes.
         * @return the configured ZMCCameraLayout.
         */
        fun createProductCameraLayout(
            context: Context,
            snapAPIToken: String,
            partnerGroupId: String,
            lensId: String,
            listener: LensChangeListener
        ): ZMCCameraLayout {
            // Initialize the ZMCCameraLayout programmatically
            val zmcCameraLayout = ZMCCameraLayout(context).apply {
                // Configure it for Single Product
                configureProductViewLayout(
                    snapAPIToken,
                    partnerGroupId,
                    lensId,
                    listener = listener
                )
            }
            return zmcCameraLayout
        }

        /**
         * Initializes and configures a ZMCCameraLayout for group view.
         *
         * @param context the context to be used for creating the ZMCCameraLayout.
         * @param snapAPIToken the Snap API token for configuration.
         * @param partnerGroupId the group ID for the partner.
         * @param listener the lens change listener to be triggered when lens changes.
         * @return the configured ZMCCameraLayout.
         */
        fun createGroupCameraLayout(
            context: Context,
            snapAPIToken: String,
            partnerGroupId: String,
            listener: LensChangeListener
        ): ZMCCameraLayout {
            // Initialize the ZMCCameraLayout programmatically
            val zmcCameraLayout = ZMCCameraLayout(context).apply {
                // Configure it for Group View
                configureGroupViewLayout(
                    snapAPIToken,
                    partnerGroupId,
                    listener = listener
                )
            }
            return zmcCameraLayout
        }
    }
}
