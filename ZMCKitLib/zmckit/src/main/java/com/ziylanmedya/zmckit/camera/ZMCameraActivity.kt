package com.ziylanmedya.zmckit.camera

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.LifecycleOwner
import com.snap.camerakit.ImageProcessor
import com.snap.camerakit.UnauthorizedApplicationException
import com.snap.camerakit.adjustments.AdaptiveToneMappingAdjustment
import com.snap.camerakit.adjustments.AdjustmentsComponent
import com.snap.camerakit.adjustments.PortraitAdjustment
import com.snap.camerakit.lenses.LensesComponent
import com.snap.camerakit.lenses.whenHasSome
import com.snap.camerakit.support.widget.CameraLayout
import com.snap.camerakit.support.widget.FlashBehavior
import com.ziylanmedya.zmckit.R
import com.ziylanmedya.zmckit.ZMCKitManager
import com.ziylanmedya.zmckit.camera.ZMCameraActivity.Capture.Result
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An [Activity] which presents a camera preview with CameraKit features such as lenses applied on top of it.
 * This class is provided as a convenience method to integrate CameraKit driven camera preview and capture flow
 * into an existing application. By utilizing the AndroidX [ActivityResultContract] it is possible to get a camera
 * capture result in just a few lines of code:
 *
 * ```
 *  val captureLauncher = registerForActivityResult(ZMCameraActivity.Capture) { result ->
 *      if (result is ZMCameraActivity.Capture.Result.Success) {
 *          // ...
 *      }
 *  }
 *  findViewById<Button>(R.id.some_button).setOnClickListener {
 *      captureLauncher.launch(ZMCameraActivity.Configuration.WithLenses(lensGroupIds = "12459"))
 *  }
 * ```
 *
 * All options to customize the appearance as well as the mode of operation of the [ZMCameraActivity] are passed as
 * the [ZMCameraActivity.Configuration] to the [androidx.activity.result.ActivityResultLauncher] obtained by registering
 * one of the [ZMCameraActivity.Capture] or [ZMCameraActivity.Play] contracts via
 * the [androidx.activity.result.ActivityResultCaller.registerForActivityResult] method.
 *
 * @since 1.7.0
 */
open class ZMCameraActivity : AppCompatActivity(), LifecycleOwner {
        /**
         * Defines all the possible ways to start a [ZMCameraActivity] with custom parameters.
         * @param cameraFacingFront True if CameraKit should attempt to open the front facing camera by default.
         * @param cameraFacingFlipEnabled True if the camera facing flip button and gesture control should be enabled.
         * @param cameraFacingBasedOnLens True if the camera facing should be based on a lens facing preference.
         * @param cameraFlashConfiguration The [FlashConfiguration] for the camera. @see [FlashConfiguration].
         * @param cameraAdjustmentsConfiguration The [AdjustmentsConfiguration] for the camera.
         * @see [AdjustmentsConfiguration]
         * @param cameraFocusEnabled True if the camera tap-to-focus gesture should be enabled.
         * @param cameraZoomEnabled True if the camera zoom by scale gesture should be enabled.
         * @param cameraKitApplicationId An optional ID of the CameraKit application, default is null which means that
         * the CameraKit will attempt to extract it from the current application manifest metadata.
         * @param cameraKitApiToken An optional value of the CameraKit API token, default is null which means that
         * the CameraKit will attempt to extract it from the current application manifest metadata.
         */
        sealed class Configuration(
                open val cameraFacingFront: Boolean,
                open val cameraFacingFlipEnabled: Boolean,
                open val cameraFacingBasedOnLens: Boolean,
                open val cameraFlashConfiguration: FlashConfiguration,
                open val cameraAdjustmentsConfiguration: AdjustmentsConfiguration,
                open val cameraFocusEnabled: Boolean,
                open val cameraZoomEnabled: Boolean,
                @Deprecated("Application ID does not need to be provided anymore, to be removed in 1.23.0.")
                open val cameraKitApplicationId: String?,
                open val cameraKitApiToken: String?
        ) {

                constructor(
                        cameraFacingFront: Boolean = true,
                        cameraFacingFlipEnabled: Boolean = true,
                        cameraFacingBasedOnLens: Boolean = false,
                        cameraFlashEnabled: Boolean = true,
                        cameraAdjustmentsConfiguration: AdjustmentsConfiguration = AdjustmentsConfiguration(),
                        cameraFocusEnabled: Boolean = true,
                        cameraZoomEnabled: Boolean = true,
                        cameraKitApplicationId: String? = null,
                        cameraKitApiToken: String? = null
                ) : this (
                        cameraFacingFront,
                        cameraFacingFlipEnabled,
                        cameraFacingBasedOnLens,
                        if (cameraFlashEnabled) FlashConfiguration.Enabled() else FlashConfiguration.Disabled,
                        cameraAdjustmentsConfiguration,
                        cameraFocusEnabled,
                        cameraZoomEnabled,
                        cameraKitApplicationId,
                        cameraKitApiToken
                )

                constructor(
                        cameraFacingFront: Boolean = true,
                        cameraFacingFlipEnabled: Boolean = true,
                        cameraFacingBasedOnLens: Boolean = false,
                        cameraAdjustmentsConfiguration: AdjustmentsConfiguration = AdjustmentsConfiguration(),
                        cameraFocusEnabled: Boolean = true,
                        cameraZoomEnabled: Boolean = true,
                        cameraKitApplicationId: String? = null,
                        cameraKitApiToken: String? = null
                ) : this (
                        cameraFacingFront,
                        cameraFacingFlipEnabled,
                        cameraFacingBasedOnLens,
                        FlashConfiguration.Enabled(),
                        cameraAdjustmentsConfiguration,
                        cameraFocusEnabled,
                        cameraZoomEnabled,
                        cameraKitApplicationId,
                        cameraKitApiToken
                )

                /**
                 * Defines the parameters to start a [ZMCameraActivity] with a lenses carousel presented on top of a camera
                 * preview.
                 *
                 * @param lensGroupIds The IDs of lens groups presented in the lenses carousel, cannot be empty.
                 * @param applyLensById An optional ID of a lens to pre-select and apply once the [ZMCameraActivity] starts.
                 * @param prefetchLensByIdPattern An optional pattern evaluated as [Regex] for IDs of lenses which content
                 * should be pre-fetched once the [ZMCameraActivity] starts.
                 * @param disableIdleState Boolean indicating whether an idle state of lenses carousel should be disabled.
                 * @param cameraFacingFront @see [Configuration.cameraFacingFront].
                 * @param cameraFacingFlipEnabled @see [Configuration.cameraFacingFlipEnabled].
                 * @param cameraFacingBasedOnLens @see [Configuration.cameraFacingBasedOnLens].
                 * @param cameraFlashConfiguration @see [Configuration.cameraFlashConfiguration].
                 * @param cameraAdjustmentsConfiguration @see [Configuration.cameraAdjustmentsConfiguration].
                 * @param cameraFocusEnabled @see [Configuration.cameraFocusEnabled].
                 * @param cameraZoomEnabled @see [Configuration.cameraZoomEnabled].
                 * @param cameraKitApplicationId @see [Configuration.cameraKitApplicationId].
                 */
                data class WithLenses(
                        val lensGroupIds: Set<String>,
                        val applyLensById: String? = null,
                        val prefetchLensByIdPattern: String? = null,
                        val disableIdleState: Boolean = true,
                        override val cameraFacingFront: Boolean = true,
                        override val cameraFacingFlipEnabled: Boolean = true,
                        override val cameraFacingBasedOnLens: Boolean = false,
                        override val cameraFlashConfiguration: FlashConfiguration = FlashConfiguration.Enabled(),
                        override val cameraAdjustmentsConfiguration: AdjustmentsConfiguration = AdjustmentsConfiguration(),
                        override val cameraFocusEnabled: Boolean = true,
                        override val cameraZoomEnabled: Boolean = true,
                        @Deprecated("Application ID does not need to be provided anymore, to be removed in 1.23.0.")
                        override val cameraKitApplicationId: String? = null,
                        override val cameraKitApiToken: String? = null
                ) : Configuration(
                        cameraFacingFront,
                        cameraFacingFlipEnabled,
                        cameraFacingBasedOnLens,
                        cameraFlashConfiguration,
                        cameraAdjustmentsConfiguration,
                        cameraFocusEnabled,
                        cameraZoomEnabled,
                        cameraKitApplicationId,
                        cameraKitApiToken
                ) {

                        constructor(
                                lensGroupIds: Set<String>,
                                applyLensById: String? = null,
                                prefetchLensByIdPattern: String? = null,
                                disableIdleState: Boolean = true,
                                cameraFacingFront: Boolean = true,
                                cameraFacingFlipEnabled: Boolean = true,
                                cameraFacingBasedOnLens: Boolean = false,
                                cameraFlashEnabled: Boolean = true,
                                cameraAdjustmentsConfiguration: AdjustmentsConfiguration = AdjustmentsConfiguration(),
                                cameraFocusEnabled: Boolean = true,
                                cameraZoomEnabled: Boolean = true,
                                cameraKitApplicationId: String? = null,
                                cameraKitApiToken: String? = null
                        ) : this(
                                lensGroupIds,
                                applyLensById,
                                prefetchLensByIdPattern,
                                disableIdleState,
                                cameraFacingFront,
                                cameraFacingFlipEnabled,
                                cameraFacingBasedOnLens,
                                if (cameraFlashEnabled) FlashConfiguration.Enabled() else FlashConfiguration.Disabled,
                                cameraAdjustmentsConfiguration,
                                cameraFocusEnabled,
                                cameraZoomEnabled,
                                cameraKitApplicationId,
                                cameraKitApiToken
                        )

                        constructor(
                                lensGroupIds: Set<String>,
                                applyLensById: String? = null,
                                prefetchLensByIdPattern: String? = null,
                                disableIdleState: Boolean = true,
                                cameraFacingFront: Boolean = true,
                                cameraFacingFlipEnabled: Boolean = true,
                                cameraFacingBasedOnLens: Boolean = false,
                                cameraAdjustmentsConfiguration: AdjustmentsConfiguration = AdjustmentsConfiguration(),
                                cameraFocusEnabled: Boolean = true,
                                cameraZoomEnabled: Boolean = true,
                                cameraKitApplicationId: String? = null,
                                cameraKitApiToken: String? = null
                        ) : this(
                                lensGroupIds,
                                applyLensById,
                                prefetchLensByIdPattern,
                                disableIdleState,
                                cameraFacingFront,
                                cameraFacingFlipEnabled,
                                cameraFacingBasedOnLens,
                                FlashConfiguration.Enabled(),
                                cameraAdjustmentsConfiguration,
                                cameraFocusEnabled,
                                cameraZoomEnabled,
                                cameraKitApplicationId,
                                cameraKitApiToken
                        )

                        constructor(
                                lensGroupIds: Array<String>,
                                applyLensById: String? = null,
                                prefetchLensByIdPattern: String? = null,
                                disableIdleState: Boolean = true,
                                cameraFacingFront: Boolean = true,
                                cameraFacingFlipEnabled: Boolean = true,
                                cameraFacingBasedOnLens: Boolean = false,
                                cameraFlashConfiguration: FlashConfiguration = FlashConfiguration.Enabled(),
                                cameraAdjustmentsConfiguration: AdjustmentsConfiguration = AdjustmentsConfiguration(),
                                cameraFocusEnabled: Boolean = true,
                                cameraZoomEnabled: Boolean = true,
                                cameraKitApplicationId: String? = null,
                                cameraKitApiToken: String? = null
                        ) : this(
                                lensGroupIds.toSet(),
                                applyLensById,
                                prefetchLensByIdPattern,
                                disableIdleState,
                                cameraFacingFront,
                                cameraFacingFlipEnabled,
                                cameraFacingBasedOnLens,
                                cameraFlashConfiguration,
                                cameraAdjustmentsConfiguration,
                                cameraFocusEnabled,
                                cameraZoomEnabled,
                                cameraKitApplicationId,
                                cameraKitApiToken
                        )

                        constructor(
                                lensGroupIds: Array<String>,
                                applyLensById: String? = null,
                                prefetchLensByIdPattern: String? = null,
                                disableIdleState: Boolean = true,
                                cameraFacingFront: Boolean = true,
                                cameraFacingFlipEnabled: Boolean = true,
                                cameraFacingBasedOnLens: Boolean = false,
                                cameraFlashEnabled: Boolean = true,
                                cameraAdjustmentsConfiguration: AdjustmentsConfiguration = AdjustmentsConfiguration(),
                                cameraFocusEnabled: Boolean = true,
                                cameraZoomEnabled: Boolean = true,
                                cameraKitApplicationId: String? = null,
                                cameraKitApiToken: String? = null
                        ) : this(
                                lensGroupIds.toSet(),
                                applyLensById,
                                prefetchLensByIdPattern,
                                disableIdleState,
                                cameraFacingFront,
                                cameraFacingFlipEnabled,
                                cameraFacingBasedOnLens,
                                if (cameraFlashEnabled) FlashConfiguration.Enabled() else FlashConfiguration.Disabled,
                                cameraAdjustmentsConfiguration,
                                cameraFocusEnabled,
                                cameraZoomEnabled,
                                cameraKitApplicationId,
                                cameraKitApiToken
                        )

                        constructor(
                                lensGroupIds: Array<String>,
                                applyLensById: String? = null,
                                prefetchLensByIdPattern: String? = null,
                                disableIdleState: Boolean = true,
                                cameraFacingFront: Boolean = true,
                                cameraFacingFlipEnabled: Boolean = true,
                                cameraFacingBasedOnLens: Boolean = false,
                                cameraAdjustmentsConfiguration: AdjustmentsConfiguration = AdjustmentsConfiguration(),
                                cameraFocusEnabled: Boolean = true,
                                cameraZoomEnabled: Boolean = true,
                                cameraKitApplicationId: String? = null,
                                cameraKitApiToken: String? = null
                        ) : this(
                                lensGroupIds.toSet(),
                                applyLensById,
                                prefetchLensByIdPattern,
                                disableIdleState,
                                cameraFacingFront,
                                cameraFacingFlipEnabled,
                                cameraFacingBasedOnLens,
                                FlashConfiguration.Enabled(),
                                cameraAdjustmentsConfiguration,
                                cameraFocusEnabled,
                                cameraZoomEnabled,
                                cameraKitApplicationId,
                                cameraKitApiToken
                        )

                        init {
                                assert(lensGroupIds.isNotEmpty())
                        }
                }

                /**
                 * Defines the parameters to start a [ZMCameraActivity] with a single lens presented on top of a camera preview.
                 *
                 * @param lensId The ID of a lens which will be applied once a [ZMCameraActivity] starts.
                 * @param lensGroupId The ID of a group that the lens specified by the [lensId] belongs to.
                 * @param displayLensIcon Boolean indicating whether an icon should be displayed to represent the lens
                 * specified by the [lensId].
                 * @param withLaunchData @see [LensesComponent.Lens.LaunchData.Builder].
                 * @param cameraFacingFront @see [Configuration.cameraFacingFront].
                 * @param cameraFacingFlipEnabled @see [Configuration.cameraFacingFlipEnabled].
                 * @param cameraFacingBasedOnLens @see [Configuration.cameraFacingBasedOnLens].
                 * @param cameraFlashConfiguration @see [Configuration.cameraFlashConfiguration].
                 * @param cameraAdjustmentsConfiguration @see [Configuration.cameraAdjustmentsConfiguration].
                 * @param cameraFocusEnabled @see [Configuration.cameraFocusEnabled].
                 * @param cameraZoomEnabled @see [Configuration.cameraZoomEnabled].
                 * @param cameraKitApplicationId @see [Configuration.cameraKitApplicationId].
                 * @param cameraKitApiToken @see [Configuration.cameraKitApiToken].
                 */
                data class WithLens(
                        val lensId: String,
                        val lensGroupId: String,
                        val displayLensIcon: Boolean = true,
                        val withLaunchData: LensesComponent.Lens.LaunchData.Builder.() -> Unit = {},
                        override val cameraFacingFront: Boolean = true,
                        override val cameraFacingFlipEnabled: Boolean = true,
                        override val cameraFacingBasedOnLens: Boolean = false,
                        override val cameraFlashConfiguration: FlashConfiguration = FlashConfiguration.Enabled(),
                        override val cameraAdjustmentsConfiguration: AdjustmentsConfiguration = AdjustmentsConfiguration(),
                        override val cameraFocusEnabled: Boolean = true,
                        override val cameraZoomEnabled: Boolean = true,
                        @Deprecated("Application ID does not need to be provided anymore, to be removed in 1.23.0.")
                        override val cameraKitApplicationId: String? = null,
                        override val cameraKitApiToken: String? = null
                ) : Configuration(
                        cameraFacingFront,
                        cameraFacingFlipEnabled,
                        cameraFacingBasedOnLens,
                        cameraFlashConfiguration,
                        cameraAdjustmentsConfiguration,
                        cameraFocusEnabled,
                        cameraZoomEnabled,
                        cameraKitApplicationId,
                        cameraKitApiToken
                ) {

                        constructor(
                                lensId: String,
                                lensGroupId: String,
                                displayLensIcon: Boolean = true,
                                withLaunchData: LensesComponent.Lens.LaunchData.Builder.() -> Unit = {},
                                cameraFacingFront: Boolean = true,
                                cameraFacingFlipEnabled: Boolean = true,
                                cameraFacingBasedOnLens: Boolean = false,
                                cameraFlashEnabled: Boolean = true,
                                cameraAdjustmentsConfiguration: AdjustmentsConfiguration = AdjustmentsConfiguration(),
                                cameraFocusEnabled: Boolean = true,
                                cameraZoomEnabled: Boolean = true,
                                cameraKitApplicationId: String? = null,
                                cameraKitApiToken: String? = null
                        ) : this (
                                lensId,
                                lensGroupId,
                                displayLensIcon,
                                withLaunchData,
                                cameraFacingFront,
                                cameraFacingFlipEnabled,
                                cameraFacingBasedOnLens,
                                if (cameraFlashEnabled) FlashConfiguration.Enabled() else FlashConfiguration.Disabled,
                                cameraAdjustmentsConfiguration,
                                cameraFocusEnabled,
                                cameraZoomEnabled,
                                cameraKitApplicationId,
                                cameraKitApiToken
                        )

                        constructor(
                                lensId: String,
                                lensGroupId: String,
                                displayLensIcon: Boolean = true,
                                withLaunchData: LensesComponent.Lens.LaunchData.Builder.() -> Unit = {},
                                cameraFacingFront: Boolean = true,
                                cameraFacingFlipEnabled: Boolean = true,
                                cameraFacingBasedOnLens: Boolean = false,
                                cameraAdjustmentsConfiguration: AdjustmentsConfiguration = AdjustmentsConfiguration(),
                                cameraFocusEnabled: Boolean = true,
                                cameraZoomEnabled: Boolean = true,
                                cameraKitApplicationId: String? = null,
                                cameraKitApiToken: String? = null
                        ) : this (
                                lensId,
                                lensGroupId,
                                displayLensIcon,
                                withLaunchData,
                                cameraFacingFront,
                                cameraFacingFlipEnabled,
                                cameraFacingBasedOnLens,
                                FlashConfiguration.Enabled(),
                                cameraAdjustmentsConfiguration,
                                cameraFocusEnabled,
                                cameraZoomEnabled,
                                cameraKitApplicationId,
                                cameraKitApiToken
                        )
                }
        }

        /**
         * Defines ways to configure the camera flash when starting a [ZMCameraActivity].
         *
         * @since 1.13.0
         */
        sealed class FlashConfiguration {

                protected abstract val configurationType: String

                /**
                 * Camera flash is disabled.
                 */
                object Disabled : FlashConfiguration() {

                        override val configurationType: String = TYPE_DISABLED
                        override fun toString(): String = "Disabled"
                }

                /**
                 * Camera flash is enabled. Ring flash is used for front flash when [useRingFlash] is true.
                 * System navigation and status bars change colors to be similar to the front flash color
                 * for added illumination when [changeSystemBarColors] is true and front flash is active.
                 */
                data class Enabled(
                        val useRingFlash: Boolean = false,
                        val changeSystemBarColors: Boolean = false
                ) : FlashConfiguration() {

                        override val configurationType: String = TYPE_ENABLED

                        override fun toString(): String {
                                return "Enabled(useRingFlash='$useRingFlash', changeSystemBarColors='$changeSystemBarColors')"
                        }
                }

                fun toBundle(extras: Bundle?): Bundle {
                        val bundle = extras ?: Bundle()
                        bundle.putString(FLASH_CONFIGURATION_KEY, configurationType)
                        return if (this is Enabled) {
                                bundle.apply {
                                        putInt(USE_RING_FLASH_KEY, if (useRingFlash) 1 else 0)
                                        putInt(CHANGE_SYSTEM_BAR_COLORS_KEY, if (changeSystemBarColors) 1 else 0)
                                }
                        } else {
                                bundle
                        }
                }

                companion object {
                        private const val TYPE_DISABLED = "FlashConfiguration.Disabled"
                        private const val TYPE_ENABLED = "FlashConfiguration.Enabled"
                        private const val FLASH_CONFIGURATION_KEY = "flash_configuration"
                        private const val USE_RING_FLASH_KEY = "use_ring_flash"
                        private const val CHANGE_SYSTEM_BAR_COLORS_KEY = "change_system_bar_colors"

                        @JvmStatic
                        fun fromBundle(bundle: Bundle): FlashConfiguration {
                                return when (bundle.getString(FLASH_CONFIGURATION_KEY)) {
                                        TYPE_DISABLED -> Disabled
                                        TYPE_ENABLED -> {
                                                val useRingFlash = bundle.getInt(USE_RING_FLASH_KEY, 0) != 0
                                                val changeSystemBarColors = bundle.getInt(CHANGE_SYSTEM_BAR_COLORS_KEY, 0) != 0
                                                Enabled(useRingFlash, changeSystemBarColors)
                                        }
                                        else -> Enabled()
                                }
                        }
                }
        }

        /**
         * Defines ways to configure adjustments.
         *
         * @param toneAdjustmentEnabled True if the tone mapping adjustment mode should be enabled.
         * @param portraitAdjustmentEnabled True if the portrait adjustment mode should be enabled.
         *
         * @since 1.13.0
         */
        data class AdjustmentsConfiguration(
                val toneAdjustmentEnabled: Boolean = true,
                val portraitAdjustmentEnabled: Boolean = true
        ) {

                fun toBundle(extras: Bundle?): Bundle {
                        val bundle = extras ?: Bundle()
                        return bundle.apply {
                                bundle.apply {
                                        putInt(TONE_ADJUSTMENT_KEY, if (toneAdjustmentEnabled) 1 else 0)
                                        putInt(PORTRAIT_ADJUSTMENT_KEY, if (portraitAdjustmentEnabled) 1 else 0)
                                }
                        }
                }

                override fun toString(): String {
                        return "AdjustmentsConfiguration(" +
                                "toneAdjustmentEnabled='$toneAdjustmentEnabled', " +
                                "portraitAdjustmentEnabled='$portraitAdjustmentEnabled'" +
                                ")"
                }

                companion object {
                        private const val TONE_ADJUSTMENT_KEY = "tone_adjustment_enabled"
                        private const val PORTRAIT_ADJUSTMENT_KEY = "portrait_adjustment_enabled"

                        @JvmStatic
                        fun fromBundle(bundle: Bundle): AdjustmentsConfiguration {
                                val toneAdjustmentEnabled = bundle.getInt(TONE_ADJUSTMENT_KEY) != 0
                                val portraitAdjustmentEnabled = bundle.getInt(PORTRAIT_ADJUSTMENT_KEY) != 0
                                return AdjustmentsConfiguration(toneAdjustmentEnabled, portraitAdjustmentEnabled)
                        }
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

        /**
         * Defines a contract to start a capture flow in the [ZMCameraActivity] with custom parameters expressed as the
         * [ZMCameraActivity.Configuration] and to receive results in the form of the [ZMCameraActivity.Capture.Result] using
         * the [androidx.activity.result.ActivityResultCaller.registerForActivityResult] method.
         */
        object Capture : ActivityResultContract<Configuration, Result>() {

                /**
                 * Defines all the possible capture result states that a [ZMCameraActivity] can produce.
                 * A [Result] can be obtained from the [ZMCameraActivity] which was started via the
                 * [androidx.activity.result.ActivityResultCaller.registerForActivityResult] method that accepts the [Capture]
                 * contract.
                 */
                sealed class Result {

                        /**
                         * Indicates a successful capture flow with a result media [type] saved to a [uri].
                         */
                        sealed class Success(open val uri: Uri, open val type: String) : Result() {

                                /**
                                 * Successful capture of a video media [type], typically an mp4, saved to a [uri].
                                 */
                                data class Video(override val uri: Uri, override val type: String) : Success(uri, type)

                                /**
                                 * Successful capture of an image media [type], typically a jpeg, saved to a [uri].
                                 */
                                data class Image(override val uri: Uri, override val type: String) : Success(uri, type)
                        }

                        /**
                         * Indicates a failure which occurred during the use of a [ZMCameraActivity].
                         */
                        data class Failure(val exception: Exception) : Result()

                        /**
                         * Indicates a cancelled capture flow - either due to a user abandoning a [ZMCameraActivity] or other causes
                         * such as missing or incomplete arguments passed when starting a [ZMCameraActivity].
                         */
                        object Cancelled : Result()
                }

                override fun createIntent(context: Context, configuration: Configuration): Intent {
                        return intentForCaptureWith(context, configuration)
                }

                override fun parseResult(resultCode: Int, intent: Intent?): Result {
                        return if (intent != null) {
                                if (resultCode == Activity.RESULT_OK) {
                                        val uri = intent.data
                                        val type = intent.type
                                        if (uri != null && type != null) {
                                                when {
                                                        type.startsWith("image/") -> {
                                                                Result.Success.Image(uri, type)
                                                        }
                                                        type.startsWith("video/") -> {
                                                                Result.Success.Video(uri, type)
                                                        }
                                                        else -> {
                                                                Result.Cancelled
                                                        }
                                                }
                                        } else {
                                                Result.Cancelled
                                        }
                                } else if (resultCode == Activity.RESULT_CANCELED) {
                                        Result.Cancelled
                                } else if (resultCode == RESULT_CODE_FAILURE) {
                                        val exception = intent.getSerializableExtra(EXTRA_EXCEPTION) as? Exception
                                        if (exception != null) {
                                                Result.Failure(exception)
                                        } else {
                                                Result.Cancelled
                                        }
                                } else {
                                        Result.Cancelled
                                }
                        } else {
                                Result.Cancelled
                        }
                }
        }

        /**
         * Defines a contract to start a play session in the [ZMCameraActivity] with custom parameters expressed as the
         * [ZMCameraActivity.Configuration] and to receive results in the form of the [ZMCameraActivity.Play.Result] using
         * the [androidx.activity.result.ActivityResultCaller.registerForActivityResult] method.
         */
        object Play : ActivityResultContract<Configuration, Play.Result>() {

                /**
                 * Defines all the possible play result states that a [ZMCameraActivity] can produce.
                 * A [Result] can be obtained from the [ZMCameraActivity] which was started via the
                 * [androidx.activity.result.ActivityResultCaller.registerForActivityResult] method that accepts the [Play]
                 * contract.
                 */
                sealed class Result {

                        /**
                         * Indicates a completed play session. This typically happens when a user navigates back.
                         */
                        object Completed : Result()

                        /**
                         * Indicates a failure which occurred during the use of a [ZMCameraActivity].
                         */
                        data class Failure(val exception: Exception) : Result()
                }

                override fun createIntent(context: Context, configuration: Configuration): Intent {
                        return intentForPlayWith(context, configuration)
                }

                override fun parseResult(resultCode: Int, intent: Intent?): Result {
                        return if (intent != null) {
                                if (resultCode == Activity.RESULT_OK) {
                                        Result.Completed
                                } else if (resultCode == Activity.RESULT_CANCELED) {
                                        Result.Completed
                                } else if (resultCode == RESULT_CODE_FAILURE) {
                                        val exception = intent.getSerializableExtra(EXTRA_EXCEPTION) as? Exception
                                        if (exception != null) {
                                                Result.Failure(exception)
                                        } else {
                                                Result.Completed
                                        }
                                } else {
                                        Result.Completed
                                }
                        } else {
                                Result.Completed
                        }
                }
        }

        companion object {

                private const val TAG = "ZMCameraActivity"
                private const val ACTION_ROOT = "com.snap.camerakit.support"

                const val ACTION_CAPTURE_WITH_LENSES = "$ACTION_ROOT.CAPTURE_WITH_LENSES"
                const val ACTION_CAPTURE_WITH_LENS = "$ACTION_ROOT.CAPTURE_WITH_LENS"
                const val ACTION_PLAY_WITH_LENSES = "$ACTION_ROOT.PLAY_WITH_LENSES"
                const val ACTION_PLAY_WITH_LENS = "$ACTION_ROOT.PLAY_WITH_LENS"

                const val EXTRA_CAMERAKIT_API_TOKEN = "camerakit_api_token"
                const val EXTRA_CAMERA_FACING_FRONT = "camera_facing_front"
                const val EXTRA_CAMERA_FACING_FLIP_ENABLED = "camera_facing_flip_enabled"
                const val EXTRA_CAMERA_FACING_BASED_ON_LENS = "camera_facing_based_on_lens"
                const val EXTRA_CAMERA_FLASH_CONFIGURATION = "camera_flash_configuration"
                const val EXTRA_CAMERA_ADJUSTMENTS_CONFIGURATION = "camera_adjustments_configuration"
                const val EXTRA_CAMERA_FOCUS_ENABLED = "camera_focus_enabled"
                const val EXTRA_CAMERA_ZOOM_ENABLED = "camera_zoom_enabled"
                const val EXTRA_LENS_GROUP_IDS = "lens_group_ids"
                const val EXTRA_LENS_LAUNCH_DATA = "lens_launch_data"
                const val EXTRA_APPLY_LENS_ID = "apply_lens_id"
                const val EXTRA_PREFETCH_LENS_ID_PATTERN = "prefetch_lens_id_pattern"
                const val EXTRA_DISABLE_LENSES_CAROUSEL = "disable_lenses_carousel"
                const val EXTRA_DISABLE_LENSES_CAROUSEL_IDLE = "disable_lenses_carousel_idle"
                const val EXTRA_EXCEPTION = "failure"
                const val EXTRA_LENS_SELECTION_CALLBACK = "EXTRA_LENS_SELECTION_CALLBACK"

                const val RESULT_CODE_FAILURE = 100

                @JvmStatic
                fun intentForCaptureWith(context: Context, configuration: Configuration): Intent {
                        return intentFor(context, configuration)
                                .apply {
                                        action = when (configuration) {
                                                is Configuration.WithLenses -> {
                                                        ACTION_CAPTURE_WITH_LENSES
                                                }
                                                is Configuration.WithLens -> {
                                                        ACTION_CAPTURE_WITH_LENS
                                                }
                                        }
                                }
                }

                @JvmStatic
                fun intentForPlayWith(context: Context, configuration: Configuration): Intent {
                        return intentFor(context, configuration)
                                .apply {
                                        action = when (configuration) {
                                                is Configuration.WithLenses -> {
                                                        ACTION_PLAY_WITH_LENSES
                                                }
                                                is Configuration.WithLens -> {
                                                        ACTION_PLAY_WITH_LENS
                                                }
                                        }
                                }
                }

                @JvmStatic
                fun intentFor(context: Context, configuration: Configuration): Intent {
                        return Intent(context, ZMCameraActivity::class.java).apply {
                                putExtra(EXTRA_CAMERAKIT_API_TOKEN, configuration.cameraKitApiToken)
                                putExtra(EXTRA_CAMERA_FACING_FRONT, configuration.cameraFacingFront)
                                putExtra(EXTRA_CAMERA_FACING_FLIP_ENABLED, configuration.cameraFacingFlipEnabled)
                                putExtra(EXTRA_CAMERA_FACING_BASED_ON_LENS, configuration.cameraFacingBasedOnLens)
                                putExtra(EXTRA_CAMERA_FLASH_CONFIGURATION, configuration.cameraFlashConfiguration.toBundle(extras))
                                putExtra(
                                        EXTRA_CAMERA_ADJUSTMENTS_CONFIGURATION,
                                        configuration.cameraAdjustmentsConfiguration.toBundle(extras)
                                )
                                putExtra(EXTRA_CAMERA_FOCUS_ENABLED, configuration.cameraFocusEnabled)
                                putExtra(EXTRA_CAMERA_ZOOM_ENABLED, configuration.cameraZoomEnabled)
                                when (configuration) {
                                        is Configuration.WithLenses -> {
                                                putExtra(EXTRA_LENS_GROUP_IDS, configuration.lensGroupIds.toTypedArray())
                                                putExtra(EXTRA_APPLY_LENS_ID, configuration.applyLensById)
                                                putExtra(EXTRA_PREFETCH_LENS_ID_PATTERN, configuration.prefetchLensByIdPattern)
                                                putExtra(EXTRA_DISABLE_LENSES_CAROUSEL_IDLE, configuration.disableIdleState)
                                        }
                                        is Configuration.WithLens -> {
                                                putExtra(EXTRA_LENS_GROUP_IDS, arrayOf(configuration.lensGroupId))
                                                val launchData = BundleLaunchDataBuilder().apply(configuration.withLaunchData).toBundle()
                                                if (!launchData.isEmpty) putExtra(EXTRA_LENS_LAUNCH_DATA, launchData)
                                                putExtra(EXTRA_APPLY_LENS_ID, configuration.lensId)
                                                putExtra(EXTRA_DISABLE_LENSES_CAROUSEL, !configuration.displayLensIcon)
                                        }
                                }
                        }
                }
        }

        @Suppress("MemberVisibilityCanBePrivate") // left accessible for sub-classing
        protected lateinit var cameraLayout: CameraLayout

        private val closeOnDestroy = mutableListOf<Closeable>()

        override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)

                val action = intent.action
                val apiToken = intent.getStringExtra(EXTRA_CAMERAKIT_API_TOKEN)
                val cameraFacingFront = intent.getBooleanExtra(EXTRA_CAMERA_FACING_FRONT, true)
                val cameraFacingFlipEnabled = intent.getBooleanExtra(EXTRA_CAMERA_FACING_FLIP_ENABLED, true)
                val cameraFacingBasedOnLens = intent.getBooleanExtra(EXTRA_CAMERA_FACING_BASED_ON_LENS, false)
                val cameraFlashConfiguration = intent.getBundleExtra(EXTRA_CAMERA_FLASH_CONFIGURATION)?.let {
                        FlashConfiguration.fromBundle(it)
                }
                val cameraAdjustmentsConfiguration = intent.getBundleExtra(EXTRA_CAMERA_ADJUSTMENTS_CONFIGURATION)?.let {
                        AdjustmentsConfiguration.fromBundle(it)
                }
                val cameraFocusEnabled = intent.getBooleanExtra(EXTRA_CAMERA_FOCUS_ENABLED, true)
                val cameraZoomEnabled = intent.getBooleanExtra(EXTRA_CAMERA_ZOOM_ENABLED, true)
                val lensGroupIds = intent.getStringArrayExtra(EXTRA_LENS_GROUP_IDS)?.toSet() ?: emptySet()
                val launchData = intent.getBundleExtra(EXTRA_LENS_LAUNCH_DATA)?.let { BundleLaunchDataBuilder(it) }?.build()
                val applyLensById = intent.getStringExtra(EXTRA_APPLY_LENS_ID)
                val prefetchLensByIdPattern = intent.getStringExtra(EXTRA_PREFETCH_LENS_ID_PATTERN)
                val disableIdleState = intent.getBooleanExtra(EXTRA_DISABLE_LENSES_CAROUSEL_IDLE, true)
                val disableLensesCarousel = intent.getBooleanExtra(EXTRA_DISABLE_LENSES_CAROUSEL, false)
                val singleLensMode = action == ACTION_CAPTURE_WITH_LENS || action == ACTION_PLAY_WITH_LENS
                val disableCapture = action == ACTION_PLAY_WITH_LENS || action == ACTION_PLAY_WITH_LENSES

                Log.d(
                        TAG,
                        "Created with parameters: " +
                                "action=$action, " +
                                "apiToken=$apiToken, " +
                                "cameraFacingFront=$cameraFacingFront, " +
                                "cameraFacingFlipEnabled=$cameraFacingFlipEnabled, " +
                                "cameraFacingBasedOnLens=$cameraFacingBasedOnLens, " +
                                "cameraFlashConfiguration=$cameraFlashConfiguration, " +
                                "cameraAdjustmentsConfiguration=$cameraAdjustmentsConfiguration, " +
                                "cameraFocusEnabled=$cameraFocusEnabled, " +
                                "cameraZoomEnabled=$cameraZoomEnabled, " +
                                "lensGroupIds=$lensGroupIds, " +
                                "applyLensById=$applyLensById, " +
                                "launchData=$launchData, " +
                                "prefetchLensByIdPattern=$prefetchLensByIdPattern, " +
                                "disableIdleState=$disableIdleState, " +
                                "disableLensesCarousel=$disableLensesCarousel, " +
                                "singleLensMode=$singleLensMode, " +
                                "disableCapture=$disableCapture"
                )

                val activityNavBarColor = window.navigationBarColor
                val activityStatusBarColor = window.statusBarColor
                if (cameraFlashConfiguration is FlashConfiguration.Enabled && cameraFlashConfiguration.changeSystemBarColors) {
                        window.apply {
                                val navBarFlag = attributes.flags and WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                                val statusBarFlag = attributes.flags and WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS

                                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                                clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                                clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

                                closeOnDestroy.add(
                                        Closeable {
                                                clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                                                setFlags(navBarFlag, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                                                setFlags(statusBarFlag, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                                        }
                                )
                        }
                }

                setContentView(R.layout.camera_kit_activity_camerakit_camera)

                cameraLayout = findViewById<CameraLayout>(R.id.camera_layout).apply {
                        configureSession {
                                // Typically, CameraKit apiToken is provided through the manifest metadata,
                                // however we keep this as an option to provide it directly through the intent.
                                apiToken(apiToken)
                        }

                        configureLensesCarousel {
                                observedGroupIds = lensGroupIds

                                // The sole purpose of this Activity is to provide a camera capture interface
                                // with lenses always enabled therefore the close button is not needed.
                                closeButtonEnabled = false

                                disableIdle = disableIdleState

                                if (singleLensMode) {
                                        configureEachItem { item ->
                                                item.enabled = item.lens.id == applyLensById
                                        }
                                }

                                enabled = !disableLensesCarousel
                        }

                        if (disableCapture) {
                                captureButton.visibility = View.GONE
                                toggleFlashButton.visibility = View.GONE
                        }

                        if (cameraFlashConfiguration is FlashConfiguration.Enabled) {
                                flashBehavior.apply {
                                        closeOnDestroy.add(
                                                attachOnFlashChangedListener(object : FlashBehavior.OnFlashChangedListener {

                                                        private var activeFlashColor: Int = Color.TRANSPARENT
                                                        private var isFrontFlashActive: Boolean = false
                                                        private var flashType: FlashBehavior.Flash = FlashBehavior.Flash.FRONT_SOLID

                                                        override fun onFlashChanged(isFlashEnabled: Boolean) { }

                                                        override fun onFlashTypeChanged(flashType: FlashBehavior.Flash) {
                                                                this.flashType = flashType
                                                        }

                                                        override fun onFlashActivated(flashType: FlashBehavior.Flash): Closeable {
                                                                isFrontFlashActive = true
                                                                if (cameraFlashConfiguration.changeSystemBarColors) {
                                                                        window.apply {
                                                                                statusBarColor = activeFlashColor
                                                                                navigationBarColor = activeFlashColor
                                                                        }
                                                                }

                                                                return Closeable {
                                                                        isFrontFlashActive = false
                                                                        if (cameraFlashConfiguration.changeSystemBarColors) {
                                                                                window.apply {
                                                                                        statusBarColor = activityStatusBarColor
                                                                                        navigationBarColor = activityNavBarColor
                                                                                }
                                                                        }
                                                                }
                                                        }

                                                        override fun onFrontFlashColorChanged(frontFlashColor: Int) {
                                                                activeFlashColor = if (flashType == FlashBehavior.Flash.FRONT_RING) {
                                                                        ColorUtils.compositeColors(
                                                                                ContextCompat.getColor(
                                                                                        context,
                                                                                        R.color.camera_kit_system_bar_transparent_overlay
                                                                                ),
                                                                                frontFlashColor
                                                                        )
                                                                } else {
                                                                        frontFlashColor
                                                                }

                                                                if (cameraFlashConfiguration.changeSystemBarColors && isFrontFlashActive) {
                                                                        window.apply {
                                                                                statusBarColor = activeFlashColor
                                                                                navigationBarColor = activeFlashColor
                                                                        }
                                                                }
                                                        }
                                                })
                                        )
                                        shouldUseRingFlash = cameraFlashConfiguration.useRingFlash
                                }
                        } else {
                                toggleFlashButton.visibility = View.GONE
                        }

                        val adjustments = mutableSetOf<AdjustmentsComponent.Adjustment>()
                        cameraAdjustmentsConfiguration?.let {
                                if (it.toneAdjustmentEnabled) {
                                        adjustments.add(AdaptiveToneMappingAdjustment)
                                }

                                if (it.portraitAdjustmentEnabled) {
                                        adjustments.add(PortraitAdjustment)
                                }
                        }
                        enabledAdjustments = adjustments

                        val gestureControls = mutableSetOf<CameraLayout.GestureControl>()
                        if (cameraFocusEnabled) {
                                gestureControls.add(CameraLayout.GestureControl.CAMERA_FOCUS_BY_SINGLE_TAP)
                        } else {
                                tapToFocusView.visibility = View.GONE
                        }
                        if (cameraFacingFlipEnabled) {
                                gestureControls.add(CameraLayout.GestureControl.CAMERA_FACING_FLIP_BY_DOUBLE_TAP)
                        } else {
                                flipFacingButton.visibility = View.GONE
                        }
                        if (cameraZoomEnabled) {
                                gestureControls.add(CameraLayout.GestureControl.CAMERA_ZOOM_BY_SCALING)
                        }
                        activeGestureControls = gestureControls

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
                                                                                        lens, launchData ?: LensesComponent.Lens.LaunchData.Empty
                                                                                )
                                                                        }
                                                                }
                                                        }

                                                        if (!prefetchLensByIdPattern.isNullOrEmpty()) {
                                                                val regex = prefetchLensByIdPattern.toRegex()
                                                                val lensesToPrefetch = lenses.filter { lens ->
                                                                        regex.matches(lens.id)
                                                                }
                                                                if (lensesToPrefetch.isNotEmpty()) {
                                                                        closeOnDestroy.add(session.lenses.prefetcher.run(lensesToPrefetch))
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
                                Log.e(TAG, "Encountered an error, finishing with details provided in the result Intent", error)
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

                        if (cameraFacingBasedOnLens) {
                                onChooseFacingForLens { lens ->
                                        lens.facingPreference
                                }
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
}