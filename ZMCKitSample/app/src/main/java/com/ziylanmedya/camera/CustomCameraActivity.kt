package com.ziylanmedya.camera

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.ziylanmedya.zmckit.ZMCKitManager
import com.ziylanmedya.zmckit.widgets.ZMCameraLayout

class CustomCameraActivity : AppCompatActivity() {

    private lateinit var cameraContainer: FrameLayout  // Parent container

    private var zmcCameraLayout: ZMCameraLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the content view
        setContentView(R.layout.activity_custom_layout)

        // Initialize the parent container where the camera layout will be added
        cameraContainer = findViewById(R.id.cameraContainer)

        // Retrieve tokens and IDs (e.g., from BuildConfig or any source)
        val snapAPIToken = BuildConfig.CAMERA_KIT_API_TOKEN
        val partnerGroupId = BuildConfig.LENS_GROUP_ID
        val lensId = BuildConfig.LENS_ID

        // Add it dynamically to the parent container

        // Set up the buttons
        val showProductButton: Button = findViewById(R.id.showProductButton)
        val showGroupButton: Button = findViewById(R.id.showGroupButton)

        // Set up button click listeners to configure the camera layout
        showProductButton.setOnClickListener {
            // Remove the previous camera layout if it exists
            removeCameraLayout()

            // Call the method from ZMCKitManager to create the camera layout
            zmcCameraLayout = ZMCKitManager.createProductCameraLayout(
                context = this,
                snapAPIToken = snapAPIToken,
                partnerGroupId = partnerGroupId,
                lensId = lensId,
                cameraListener = object : ZMCKitManager.ZMCameraListener {
                    override fun onLensChange(lensId: String) {
                        // Handle the lens change here
                        println("Lens changed: $lensId")
                    }

                    override fun onImageCaptured(imageUri: Uri) {
                        println("Capture Image: $imageUri")
                    }
                }
            )
            // Add the newly created camera layout to the container
            cameraContainer.addView(zmcCameraLayout)
        }

        showGroupButton.setOnClickListener {
            // Remove the previous camera layout if it exists
            removeCameraLayout()

            // Call the method from ZMCKitManager to create the camera layout
            zmcCameraLayout = ZMCKitManager.createGroupCameraLayout(
                context = this,
                snapAPIToken = snapAPIToken,
                partnerGroupId = partnerGroupId,
                cameraListener = object : ZMCKitManager.ZMCameraListener {
                    override fun onLensChange(lensId: String) {
                        // Handle the lens change here
                        println("Lens changed: $lensId")
                    }

                    override fun onImageCaptured(imageUri: Uri) {

                    }
                }
            )
            // Add the newly created camera layout to the container
            cameraContainer.addView(zmcCameraLayout)
        }
    }

    // Remove the layout when needed
    private fun removeCameraLayout() {
        cameraContainer.removeView(zmcCameraLayout)
    }
}
