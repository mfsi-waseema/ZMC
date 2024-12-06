package com.ziylanmedya.camera

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.ziylanmedya.zmckit.ZMCKitManager

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Access the ZMCKitManager singleton and pass the current activity to it
        val zmckitManager = ZMCKitManager.getInstance(this)

        // Initialize the capture launcher
        zmckitManager.initCaptureLauncher(object : ZMCKitManager.CaptureCallback {
            override fun onImageCaptured(uri: String) {
                // Handle image capture result here
                val imageView: ImageView = findViewById(R.id.capturedImageView)
                imageView.visibility = View.VISIBLE
                imageView.setImageURI(Uri.parse(uri))            }

            override fun onVideoCaptured(uri: String) {
                // Handle video capture result here
                val videoView: VideoView = findViewById(R.id.capturedVideoView)
                videoView.visibility = View.VISIBLE
                videoView.setVideoURI(Uri.parse(uri))
                videoView.start()            }

            override fun onCaptureCancelled() {
                // Handle capture cancellation here
                println("Capture was cancelled.")
            }

            override fun onCaptureFailure(exception: Exception) {
                // Handle capture failure here
                println("Capture failed.")
            }
        })

        // Register the listener to handle lens changes
        zmckitManager.onLensChange { lensId ->
            // Handle the lens change here
            println("Lens changed: $lensId")
        }

        // Find the button by its ID
        val showProductButton: Button = findViewById(R.id.showProductButton)
        val showGroupButton: Button = findViewById(R.id.showGroupButton)

        // Set up the click listener for Product
        showProductButton.setOnClickListener {
            hideCaptureControl()

            val snapAPIToken = BuildConfig.CAMERA_KIT_API_TOKEN
            val partnerGroupId = BuildConfig.LENS_GROUP_ID
            val lensId = BuildConfig.LENS_ID

            zmckitManager.showProductView(
                snapAPIToken,
                partnerGroupId,
                lensId
            )
        }

        // Set up the click listener for Group
        showGroupButton.setOnClickListener {
            hideCaptureControl()

            val snapAPIToken = BuildConfig.CAMERA_KIT_API_TOKEN
            val partnerGroupId = BuildConfig.LENS_GROUP_ID

            zmckitManager.showGroupView(
                snapAPIToken,
                partnerGroupId
            )
        }
    }

    private fun hideCaptureControl() {
        val imageView: ImageView = findViewById(R.id.capturedImageView)
        val videoView: VideoView = findViewById(R.id.capturedVideoView)
        videoView.visibility = View.INVISIBLE
        imageView.visibility = View.INVISIBLE
    }
}