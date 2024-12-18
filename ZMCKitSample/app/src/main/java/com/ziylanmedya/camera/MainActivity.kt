package com.ziylanmedya.camera

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.ziylanmedya.zmckit.ZMCKitManager

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val customCameraButton: Button = findViewById(R.id.customCameraButton)
        customCameraButton.setOnClickListener {
            val intent = Intent(this, CustomCameraActivity::class.java)
            startActivity(intent)
        }

        setupZMCCamera()
    }

    private fun setupZMCCamera() {

        // Find the button by its ID
        val showProductButton: Button = findViewById(R.id.showProductButton)
        val showGroupButton: Button = findViewById(R.id.showGroupButton)

        // Set up the click listener for Product
        showProductButton.setOnClickListener {
            val snapAPIToken = BuildConfig.CAMERA_KIT_API_TOKEN
            val partnerGroupId = BuildConfig.LENS_GROUP_ID
            val lensId = BuildConfig.LENS_ID

            ZMCKitManager.showProductActivity(
                this,
                snapAPIToken,
                partnerGroupId,
                lensId,
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
        }

        // Set up the click listener for Group
        showGroupButton.setOnClickListener {
            val snapAPIToken = BuildConfig.CAMERA_KIT_API_TOKEN
            val partnerGroupId = BuildConfig.LENS_GROUP_ID

            ZMCKitManager.showGroupActivity(
                this,
                snapAPIToken,
                partnerGroupId,
                cameraListener = object : ZMCKitManager.ZMCameraListener {
                    override fun onLensChange(lensId: String) {
                        // Handle the lens change here
                        println("Lens changed: $lensId")
                    }

                    override fun onImageCaptured(imageUri: Uri) {

                    }
                }
            )
        }
    }
}
