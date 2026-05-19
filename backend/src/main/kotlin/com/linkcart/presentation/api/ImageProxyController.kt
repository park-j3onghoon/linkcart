package com.linkcart.presentation.api

import com.linkcart.application.parser.port.SafeUrlChecker
import jakarta.validation.constraints.NotBlank
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException

@Validated
@RestController
class ImageProxyController(
    private val safeUrlChecker: SafeUrlChecker,
    @Qualifier("imageProxyRestTemplate")
    private val restTemplate: RestTemplate,
) {

    @GetMapping("/api/v1/images:proxy")
    fun proxy(
        @RequestParam @NotBlank(message = "이미지 URL은 비어 있을 수 없습니다") url: String,
    ): ResponseEntity<ByteArray> {
        if (!safeUrlChecker.isSafe(url)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "허용되지 않는 URL입니다")
        }

        val upstreamResponse = try {
            restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, ByteArray::class.java)
        } catch (_: RestClientException) {
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "이미지를 가져올 수 없습니다")
        }

        val contentType = upstreamResponse.headers.contentType
        if (contentType == null || contentType.type != "image" ||
            contentType.subtype !in ALLOWED_IMAGE_SUBTYPES
        ) {
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "지원하지 않는 이미지 형식입니다")
        }

        return ResponseEntity
            .ok()
            .contentType(contentType)
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300")
            .body(upstreamResponse.body ?: ByteArray(0))
    }

    companion object {
        // SVG는 <script> 태그 등 XSS 벡터를 포함할 수 있어 제외.
        private val ALLOWED_IMAGE_SUBTYPES = setOf("png", "jpeg", "gif", "webp")
    }
}
