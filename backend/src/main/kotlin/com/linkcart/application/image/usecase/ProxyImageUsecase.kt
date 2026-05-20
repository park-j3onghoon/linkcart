package com.linkcart.application.image.usecase

import com.linkcart.application.image.port.ImageFetcher
import com.linkcart.application.port.SafeUrlChecker
import org.springframework.http.MediaType
import org.springframework.stereotype.Service

class UnsafeImageUrlException(message: String) : RuntimeException(message)
class ImageFetchFailedException(message: String) : RuntimeException(message)
class UnsupportedImageFormatException(message: String) : RuntimeException(message)

@Service
class ProxyImageUsecase(
    private val safeUrlChecker: SafeUrlChecker,
    private val imageFetcher: ImageFetcher,
) {

    data class ProxiedImage(val bytes: ByteArray, val contentType: MediaType)

    fun execute(url: String): ProxiedImage {
        if (!safeUrlChecker.isSafe(url)) {
            throw UnsafeImageUrlException("허용되지 않는 URL입니다")
        }

        val fetched = try {
            imageFetcher.fetch(url)
        } catch (_: ImageFetcher.FetchFailed) {
            throw ImageFetchFailedException("이미지를 가져올 수 없습니다")
        }

        val contentType = fetched.contentType
        if (contentType == null || contentType.type != "image" ||
            contentType.subtype !in ALLOWED_IMAGE_SUBTYPES
        ) {
            throw UnsupportedImageFormatException("지원하지 않는 이미지 형식입니다")
        }

        return ProxiedImage(bytes = fetched.bytes, contentType = contentType)
    }

    companion object {
        // SVG는 <script> 태그 등 XSS 벡터를 포함할 수 있어 제외.
        private val ALLOWED_IMAGE_SUBTYPES = setOf("png", "jpeg", "gif", "webp")
    }
}
