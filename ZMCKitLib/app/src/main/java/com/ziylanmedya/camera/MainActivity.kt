package com.ziylanmedya.camera

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.ziylanmedya.zmckit.ZMCKitManager

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Access the ZMCKitManager singleton and pass the current activity to it
        val zmckitManager = ZMCKitManager.getInstance(this)
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
            val snapAPIToken = BuildConfig.CAMERA_KIT_API_TOKEN
            val partnerGroupId = BuildConfig.LENS_GROUP_ID
            val lensId = BuildConfig.LENS_ID

            zmckitManager.showProductActivity(
                snapAPIToken,
                partnerGroupId,
                lensId
            )
        }

        // Set up the click listener for Group
        showGroupButton.setOnClickListener {
            val snapAPIToken = BuildConfig.CAMERA_KIT_API_TOKEN
            val partnerGroupId = BuildConfig.LENS_GROUP_ID

            zmckitManager.showGroupActivity(
                snapAPIToken,
                partnerGroupId
            )
        }
    }
}