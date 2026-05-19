package com.linkcart.presentation.api

import com.linkcart.application.image.usecase.ProxyImageUsecase
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class ImageProxyController(
    private val proxyImageUsecase: ProxyImageUsecase,
) {

    @GetMapping("/api/v1/images:proxy")
    fun proxy(
        @RequestParam @NotBlank(message = "이미지 URL은 비어 있을 수 없습니다") url: String,
    ): ResponseEntity<ByteArray> {
        val image = proxyImageUsecase.execute(url)
        return ResponseEntity
            .ok()
            .contentType(image.contentType)
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300")
            .body(image.bytes)
    }
}
