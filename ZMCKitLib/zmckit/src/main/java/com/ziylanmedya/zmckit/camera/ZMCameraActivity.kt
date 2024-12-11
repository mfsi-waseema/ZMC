package com.ziylanmedya.zmckit.camera

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.snap.camerakit.ImageProcessor
import com.snap.camerakit.UnauthorizedApplicationException
import com.snap.camerakit.lenses.LensesComponent
import com.snap.camerakit.lenses.whenHasSome
import com.snap.camerakit.support.widget.CameraLayout
import com.ziylanmedya.zmckit.R
import com.ziylanmedya.zmckit.ZMCKitManager
import com.ziylanmedya.zmckit.camera.Constants.EXTRA_APPLY_LENS_ID
import com.ziylanmedya.zmckit.camera.Constants.EXTRA_CAMERA_FACING_FRONT
import com.ziylanmedya.zmckit.camera.Constants.EXTRA_CAMERA_KIT_API_TOKEN
import com.ziylanmedya.zmckit.camera.Constants.EXTRA_EXCEPTION
import com.ziylanmedya.zmckit.camera.Constants.EXTRA_LENS_GROUP_IDS
import com.ziylanmedya.zmckit.camera.Constants.RESULT_CODE_FAILURE
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An [Activity] which presents a camera preview with CameraKit features such as lenses applied on top of it.
 * This class is provided as a convenience method to integrate CameraKit driven camera preview and capture flow
 * into an existing application.
 */
internal open class ZMCameraActivity : AppCompatActivity(), LifecycleOwner {

        private val activityTag = "ZMCameraActivity"

        @Suppress("MemberVisibilityCanBePrivate") // left accessible for sub-classing
        protected lateinit var cameraLayout: CameraLayout

        private val closeOnDestroy = mutableListOf<Closeable>()

        override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)

                val apiToken = intent.getStringExtra(EXTRA_CAMERA_KIT_API_TOKEN)
                val cameraFacingFront = intent.getBooleanExtra(EXTRA_CAMERA_FACING_FRONT, false)
                val lensGroupIds = intent.getStringArrayExtra(EXTRA_LENS_GROUP_IDS)?.toSet() ?: emptySet()
                val applyLensById = intent.getStringExtra(EXTRA_APPLY_LENS_ID)

                setContentView(R.layout.camera_kit_activity_camerakit_camera)

                cameraLayout = findViewById<CameraLayout>(R.id.camera_layout).apply {
                        configureSession {
                                apiToken(apiToken)
                        }

                        configureLensesCarousel {
                                observedGroupIds = lensGroupIds

                                // The sole purpose of this Activity is to provide a camera capture interface
                                // with lenses always enabled therefore the close button is not needed.
                                closeButtonEnabled = false

                                disableIdle = true

                                applyLensById?.let {
                                        configureEachItem { item ->
                                                item.enabled = item.lens.id == applyLensById
                                        }
                                        enabled = false
                                }
                        }

                        captureButton.visibility = View.GONE
                        toggleFlashButton.visibility = View.GONE
                        flipFacingButton.visibility = View.GONE
                        tapToFocusView.visibility = View.GONE

                        enabledAdjustments = emptySet()

                        onImageTaken { bitmap ->
                                val imageFile = this@ZMCameraActivity.cacheJpegOf(bitmap)
                                val intent = Intent().apply {
                                        setDataAndType(Uri.fromFile(imageFile), "image/jpeg")
                                }
                                setResult(Activity.RESULT_OK, intent)
                                finish()
                        }

                        onVideoTaken { file ->
                                val intent = Intent().apply {
                                        setDataAndType(Uri.fromFile(file), "video/mp4")
                                }
                                setResult(Activity.RESULT_OK, intent)
                                finish()
                        }

                        onSessionAvailable { session ->
                                val appliedLensById = AtomicBoolean()
                                closeOnDestroy.add(
                                        session.lenses.repository.observe(
                                                LensesComponent.Repository.QueryCriteria.Available(lensGroupIds)
                                        ) { result ->
                                                result.whenHasSome { lenses ->
                                                        if (!applyLensById.isNullOrEmpty()) {
                                                                lenses.find { lens -> lens.id == applyLensById }?.let { lens ->
                                                                        if (appliedLensById.compareAndSet(false, true)) {
                                                                                session.lenses.processor.apply(
                                                                                        lens, LensesComponent.Lens.LaunchData.Empty
                                                                                )
                                                                        }
                                                                }
                                                        }

                                                }
                                        }
                                )

                                // Observe carousel events
                                closeOnDestroy.add(
                                        session.lenses.carousel.observe { event ->
                                                when (event) {
                                                        is LensesComponent.Carousel.Event.Activated.WithLens -> {
                                                                val selectedLensId = event.lens.id
                                                                val activity = (cameraLayout.context as? AppCompatActivity)
                                                                activity?.runOnUiThread {
                                                                        ZMCKitManager.getInstance(activity).notifyLensChange(selectedLensId)
                                                                }
                                                        }
                                                        else -> {
                                                                // Handle other carousel events if needed
                                                        }
                                                }
                                        }
                                )

                                startPreview(facingFront = cameraFacingFront)
                        }

                        onError { error ->
                                Log.e(activityTag, "Encountered an error, finishing with details provided in the result Intent", error)
                                val exception = when (error) {
                                        is UnauthorizedApplicationException ->
                                                Exception.Unauthorized("Application is not authorized to use CameraKit", error)
                                        is CameraLayout.Failure.DeviceNotSupported ->
                                                Exception.DeviceNotSupported("CameraKit does not support this device", error)
                                        is CameraLayout.Failure.MissingPermissions ->
                                                Exception.MissingPermissions("Permissions required to run CameraKit were not granted", error)
                                        is ImageProcessor.Failure.Graphics ->
                                                Exception.GraphicsProcessing(
                                                        "CameraKit encountered a failure in the graphics processing pipeline", error
                                                )
                                        is LensesComponent.Processor.Failure ->
                                                Exception.LensesProcessing(
                                                        "CameraKit encountered a failure in the lenses processing pipeline", error
                                                )
                                        else ->
                                                Exception.Unexpected("CameraKit encountered an unexpected failure", error)
                                }
                                val intent = Intent().apply {
                                        putExtra(EXTRA_EXCEPTION, exception)
                                }
                                setResult(RESULT_CODE_FAILURE, intent)
                                finish()
                        }
                }
        }

        override fun onDestroy() {
                closeOnDestroy.forEach { closeable ->
                        closeable.close()
                }
                super.onDestroy()
        }

        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                return if (cameraLayout.dispatchKeyEvent(event)) {
                        true
                } else {
                        super.dispatchKeyEvent(event)
                }
        }

        /**
         * Indicates a failure which may occur during the use of a [ZMCameraActivity].
         */
        sealed class Exception(
                override val message: String,
                override val cause: Throwable?
        ) : RuntimeException(message, cause) {

                /**
                 * Unexpected failure that has not been accounted for, most likely a bug in the CameraKit implementation.
                 */
                data class Unexpected(
                        override val message: String,
                        override val cause: Throwable?
                ) : Exception(message, cause)

                /**
                 * The application that is attempting to use CameraKit is unauthorized to do so, caused by unrecognized
                 * application package name, incorrect device clock settings or other reasons.
                 */
                data class Unauthorized(
                        override val message: String,
                        override val cause: Throwable?
                ) : Exception(message, cause)

                /**
                 * Device is not supported by the CameraKit due to insufficient graphics or other capabilities.
                 */
                data class DeviceNotSupported(
                        override val message: String,
                        override val cause: Throwable?
                ) : Exception(message, cause)

                /**
                 * User has not granted the permissions required for [ZMCameraActivity] to operate normally.
                 */
                data class MissingPermissions(
                        override val message: String,
                        override val cause: Throwable?
                ) : Exception(message, cause)

                /**
                 * A failure in the CameraKit graphics processing pipeline, most likely an OpenGL related issue which might
                 * be recoverable.
                 */
                data class GraphicsProcessing(
                        override val message: String,
                        override val cause: Throwable?
                ) : Exception(message, cause)

                /**
                 * A failure in the CameraKit lenses processing pipeline, caused by an error in a lens script, exhausted
                 * resources or other reasons that might be recovered from by re-trying or applying a different lens.
                 */
                data class LensesProcessing(
                        override val message: String,
                        override val cause: Throwable?
                ) : Exception(message, cause)
        }
}