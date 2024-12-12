package com.ziylanmedya.zmckit.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.snap.camerakit.ImageProcessor
import com.snap.camerakit.UnauthorizedApplicationException
import com.snap.camerakit.lenses.LensesComponent
import com.snap.camerakit.lenses.whenHasSome
import com.snap.camerakit.support.widget.CameraLayout
import com.ziylanmedya.zmckit.R
import com.ziylanmedya.zmckit.ZMCKitManager
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

class ZMCCameraLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {


    private lateinit var cameraLayout: CameraLayout
    private var cameraSession: com.snap.camerakit.Session? = null
    private val closeOnDestroy = mutableListOf<Closeable>()

    // Private initialize method
    private fun initialize(
        apiToken: String?,
        cameraFacingFront: Boolean = false,
        lensGroupIds: Set<String>,
        applyLensById: String?,
        listener: ZMCKitManager.LensChangeListener?
    ) {
        inflate(context, R.layout.camera_kit_activity_camerakit_camera, this)

        cameraLayout = findViewById<CameraLayout>(R.id.snap_camera_layout).apply {
            configureSession { apiToken(apiToken) }

            configureLensesCarousel {
                observedGroupIds = lensGroupIds
                closeButtonEnabled = false
                disableIdle = true

                applyLensById?.let {
                    configureEachItem { item ->
                        item.enabled = item.lens.id == applyLensById
                    }
                    enabled = false
                }
            }

            toggleFlashButton.visibility = View.GONE
            captureButton.visibility = View.GONE
            tapToFocusView.visibility = View.GONE
            flipFacingButton.visibility = View.GONE

            enabledAdjustments = emptySet()

            onSessionAvailable { session ->
                cameraSession = session
                handleSessionAvailable(session, lensGroupIds, applyLensById, cameraFacingFront, listener)
            }

            onError { error ->
                val message = mapErrorToException(error).toString()
                println("ZMCCameraLayout Error $message")
            }
        }
    }

    private fun handleSessionAvailable(
        session: com.snap.camerakit.Session,
        lensGroupIds: Set<String>,
        applyLensById: String?,
        cameraFacingFront: Boolean,
        listener: ZMCKitManager.LensChangeListener?
    ) {
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

        // Observe carousel events and notify when the lens is selected
        closeOnDestroy.add(
            session.lenses.carousel.observe { event ->
                when (event) {
                    is LensesComponent.Carousel.Event.Activated.WithLens -> {
                        listener?.onLensChange(event.lens.id) // Notify about the selected lens
                    }
                    else -> {
                        // Handle other carousel events if needed
                    }
                }
            }
        )
        cameraLayout.startPreview(facingFront = cameraFacingFront)
    }

    private fun mapErrorToException(error: Throwable): Exception {
        return when (error) {
            is UnauthorizedApplicationException ->
                IllegalStateException("Application is not authorized to use CameraKit", error)
            is CameraLayout.Failure.DeviceNotSupported ->
                UnsupportedOperationException("Device not supported by CameraKit", error)
            is CameraLayout.Failure.MissingPermissions ->
                SecurityException("Permissions required for CameraKit were not granted", error)
            is ImageProcessor.Failure.Graphics ->
                RuntimeException("Graphics processing failure in CameraKit", error)
            is LensesComponent.Processor.Failure ->
                RuntimeException("Lenses processing failure in CameraKit", error)
            else -> Exception("Unexpected error in CameraKit", error)
        }
    }

    // Public function to launch the camera in product view mode (single lens mode)
    fun configureProductViewLayout(
        snapAPIToken: String,
        partnerGroupId: String,
        lensId: String,
        cameraFacingFront: Boolean = false,
        listener: ZMCKitManager.LensChangeListener?
    ) {
        initialize(
            apiToken = snapAPIToken,
            cameraFacingFront = cameraFacingFront,
            lensGroupIds = setOf(partnerGroupId),
            applyLensById = lensId,
            listener = listener
        )
    }

    // Public function to launch the camera in group view mode (lens group mode)
    fun configureGroupViewLayout(
        snapAPIToken: String,
        partnerGroupId: String,
        cameraFacingFront: Boolean = false,
        listener: ZMCKitManager.LensChangeListener?
    ) {
        initialize(
            apiToken = snapAPIToken,
            cameraFacingFront = cameraFacingFront,
            lensGroupIds = setOf(partnerGroupId),
            applyLensById = null,
            listener = listener
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        closeOnDestroy.forEach { closeable ->
            closeable.close()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return if (cameraLayout.dispatchKeyEvent(event)) {
            true
        } else {
            super.dispatchKeyEvent(event)
        }
    }
}