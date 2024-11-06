// AM (REMOVE_LIBRARIES) -->
package eu.kanade.tachiyomi.util

import android.content.Context
import android.graphics.PointF
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.CallSuper
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.os.postDelayed
import androidx.core.view.isVisible
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.EASE_IN_OUT_QUAD
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE
import eu.kanade.tachiyomi.util.system.GLUtil
import eu.kanade.tachiyomi.util.system.animatorDurationScale
import eu.kanade.tachiyomi.util.view.isVisibleOnScreen
import java.nio.ByteBuffer

/**
 * A wrapper view for showing page image.
 *
 * Animated image will be drawn while [SubsamplingScaleImageView] will take non-animated image.
 *
 */
open class ReaderPageImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttrs: Int = 0,
    @StyleRes defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleAttrs, defStyleRes) {

    private var pageView: View? = null

    private var config: Config? = null

    private var onImageLoaded: (() -> Unit)? = null
    private var onImageLoadError: (() -> Unit)? = null
    private var onScaleChanged: ((newScale: Float) -> Unit)? = null
    var onViewClicked: (() -> Unit)? = null

    /**
     * For automatic background. Will be set as background color when [onImageLoaded] is called.
     */
    private var pageBackground: Drawable? = null

    @CallSuper
    open fun onImageLoaded() {
        onImageLoaded?.invoke()
        background = pageBackground
    }

    @CallSuper
    open fun onImageLoadError() {
        onImageLoadError?.invoke()
    }

    @CallSuper
    open fun onScaleChanged(newScale: Float) {
        onScaleChanged?.invoke(newScale)
    }

    @CallSuper
    open fun onViewClicked() {
        onViewClicked?.invoke()
    }

    fun setImage(drawable: Drawable, config: Config) {
        this.config = config
        if (drawable is Animatable) {
            prepareImageView()
            setAnimatedImage(drawable)
        } else {
            prepareSubsamplingImageView()
            setNonAnimatedImage(drawable, config)
        }
    }

    private fun prepareSubsamplingImageView() {
        if (pageView is SubsamplingScaleImageView) return
        removeView(pageView)

        pageView = SubsamplingScaleImageView(context)
            .apply {
                setMaxTileSize(GLUtil.maxTextureSize)
                setDoubleTapZoomStyle(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER)
                setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_INSIDE)
                setMinimumTileDpi(180)
                setOnStateChangedListener(
                    object : SubsamplingScaleImageView.OnStateChangedListener {
                        override fun onScaleChanged(newScale: Float, origin: Int) {
                            this@ReaderPageImageView.onScaleChanged(newScale)
                        }

                        override fun onCenterChanged(newCenter: PointF?, origin: Int) {
                            // Not used
                        }
                    },
                )
                setOnClickListener { this@ReaderPageImageView.onViewClicked() }
            }
        addView(pageView, MATCH_PARENT, MATCH_PARENT)
    }

    private fun setNonAnimatedImage(
        image: Drawable,
        config: Config,
    ) = (pageView as? SubsamplingScaleImageView)?.apply {
        setDoubleTapZoomDuration(config.zoomDuration.getSystemScaledDuration())
        setMinimumScaleType(config.minimumScaleType)
        setMinimumDpi(1) // Just so that very small image will be fit for initial load
        setCropBorders(config.cropBorders)
        setOnImageEventListener(
            object : SubsamplingScaleImageView.DefaultOnImageEventListener() {
                override fun onReady() {
                    setupZoom(config)
                    if (isVisibleOnScreen()) landscapeZoom(true)
                    this@ReaderPageImageView.onImageLoaded()
                }

                override fun onImageLoadError(e: Exception) {
                    this@ReaderPageImageView.onImageLoadError()
                }
            },
        )

        setImage(ImageSource.bitmap((image as BitmapDrawable).bitmap))
        isVisible = true
    }

    private fun setupZoom(config: Config?) {
        val scaleImageView = pageView as? SubsamplingScaleImageView ?: return
        scaleImageView.maxScale = scaleImageView.scale * MAX_ZOOM_SCALE
        scaleImageView.setDoubleTapZoomScale(scaleImageView.scale * 2)

        when (config?.zoomStartPosition) {
            ZoomStartPosition.LEFT -> scaleImageView.setScaleAndCenter(scaleImageView.scale, PointF(0F, 0F))
            ZoomStartPosition.RIGHT -> scaleImageView.setScaleAndCenter(
                scaleImageView.scale,
                PointF(scaleImageView.sWidth.toFloat(), 0F),
            )
            ZoomStartPosition.CENTER -> scaleImageView.setScaleAndCenter(scaleImageView.scale, scaleImageView.center)
            null -> {}
        }
    }

    private fun SubsamplingScaleImageView.landscapeZoom(forward: Boolean) {
        val config = config
        if (config != null &&
            config.landscapeZoom &&
            config.minimumScaleType == SCALE_TYPE_CENTER_INSIDE &&
            sWidth > sHeight &&
            scale == minScale
        ) {
            handler?.postDelayed(500) {
                val point = when (config.zoomStartPosition) {
                    ZoomStartPosition.LEFT -> if (forward) {
                        PointF(0F, 0F)
                    } else {
                        PointF(
                            sWidth.toFloat(),
                            0F,
                        )
                    }
                    ZoomStartPosition.RIGHT -> if (forward) {
                        PointF(sWidth.toFloat(), 0F)
                    } else {
                        PointF(
                            0F,
                            0F,
                        )
                    }
                    ZoomStartPosition.CENTER -> center
                }

                val targetScale = height.toFloat() / sHeight.toFloat()
                animateScaleAndCenter(targetScale, point)!!
                    .withDuration(500)
                    .withEasing(EASE_IN_OUT_QUAD)
                    .withInterruptible(true)
                    .start()
            }
        }
    }

    private fun prepareImageView() {
        if (pageView is AppCompatImageView) return
        removeView(pageView)

        pageView = AppCompatImageView(context).apply {
            adjustViewBounds = true
        }
        addView(pageView, MATCH_PARENT, MATCH_PARENT)
    }

    private fun setAnimatedImage(image: Drawable) = (pageView as? AppCompatImageView)?.apply {
        val bitmap = (image as BitmapDrawable).bitmap
        val byteBuffer = ByteBuffer.allocate(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(byteBuffer)
        byteBuffer.rewind()
        val byteArray = byteBuffer.array()
        val data = ByteBuffer.wrap(byteArray)
        val request = ImageRequest.Builder(context)
            .data(data)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .target(
                onSuccess = { result ->
                    val drawable = result.asDrawable(context.resources)
                    setImageDrawable(drawable)
                    (result as? Animatable)?.start()
                    isVisible = true
                    this@ReaderPageImageView.onImageLoaded()
                },
                onError = {
                    this@ReaderPageImageView.onImageLoadError()
                },
            )
            .crossfade(false)
            .build()
        context.imageLoader.enqueue(request)
    }

    private fun Int.getSystemScaledDuration(): Int {
        return (this * context.animatorDurationScale).toInt().coerceAtLeast(1)
    }

    data class Config(
        val zoomDuration: Int,
        val minimumScaleType: Int = SCALE_TYPE_CENTER_INSIDE,
        val cropBorders: Boolean = false,
        val zoomStartPosition: ZoomStartPosition = ZoomStartPosition.CENTER,
        val landscapeZoom: Boolean = false,
    )

    enum class ZoomStartPosition {
        LEFT,
        CENTER,
        RIGHT,
    }
}

private const val MAX_ZOOM_SCALE = 5F
// <-- AM (REMOVE_LIBRARIES)
