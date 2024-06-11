package tachiyomi.core.util.system

import android.graphics.BitmapFactory
import java.io.InputStream
import java.net.URLConnection

object ImageUtil {

    fun isImage(name: String?, openStream: (() -> InputStream)? = null): Boolean {
        if (name == null) return false

        val contentType = try {
            URLConnection.guessContentTypeFromName(name)
        } catch (e: Exception) {
            null
        } ?: openStream?.let { findImageType(it)?.mime }
        return contentType?.startsWith("image/") ?: false
    }

    fun findImageType(openStream: () -> InputStream): ImageType? {
        return openStream().use { findImageType(it) }
    }

    private fun findImageType(stream: InputStream): ImageType? {
        return try {
            when (getImageType(stream)) {
                "image/avif" -> ImageType.AVIF
                "image/gif" -> ImageType.GIF
                "image/heif" -> ImageType.HEIF
                "image/jpeg" -> ImageType.JPEG
                "image/jxl" -> ImageType.JXL
                "image/png" -> ImageType.PNG
                "image/webp" -> ImageType.WEBP
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getImageType(stream: InputStream): String? {
        val mimeType = URLConnection.guessContentTypeFromStream(stream)
        if (mimeType != null && mimeType.startsWith("image/")) {
            return mimeType
        }

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(stream, null, options)
        return options.outMimeType
    }

    enum class ImageType(val mime: String, val extension: String) {
        AVIF("image/avif", "avif"),
        GIF("image/gif", "gif"),
        HEIF("image/heif", "heif"),
        JPEG("image/jpeg", "jpg"),
        JXL("image/jxl", "jxl"),
        PNG("image/png", "png"),
        WEBP("image/webp", "webp"),
    }
}
