package com.linkcart.presentation.api

import com.linkcart.application.image.usecase.ImageFetchFailedException
import com.linkcart.application.image.usecase.ProxyImageUsecase
import com.linkcart.application.image.usecase.UnsafeImageUrlException
import com.linkcart.application.image.usecase.UnsupportedImageFormatException
import com.linkcart.infrastructure.config.WebConfig
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.ComponentScan.Filter
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    controllers = [ImageProxyController::class],
    excludeFilters = [Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [WebConfig::class])],
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
class ImageProxyControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var proxyImageUsecase: ProxyImageUsecase

    @Test
    fun `success returns image bytes with content-type and cache header`() {
        val imageBytes = byteArrayOf(1, 2, 3, 4)
        given(proxyImageUsecase.execute("https://images.example.com/test.jpg")).willReturn(
            ProxyImageUsecase.ProxiedImage(bytes = imageBytes, contentType = MediaType.IMAGE_JPEG),
        )

        mockMvc.perform(get("/api/v1/images:proxy").param("url", "https://images.example.com/test.jpg"))
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE))
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=300"))
            .andExpect(content().bytes(imageBytes))
    }

    @Test
    fun `blank url returns 400`() {
        mockMvc.perform(get("/api/v1/images:proxy").param("url", ""))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
    }

    @Test
    fun `UnsafeImageUrlException maps to 400 INVALID_ARGUMENT`() {
        given(proxyImageUsecase.execute("http://127.0.0.1/internal.png"))
            .willThrow(UnsafeImageUrlException("허용되지 않는 URL입니다"))

        mockMvc.perform(get("/api/v1/images:proxy").param("url", "http://127.0.0.1/internal.png"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
    }

    @Test
    fun `ImageFetchFailedException maps to 503 UNAVAILABLE`() {
        given(proxyImageUsecase.execute("https://images.example.com/error.jpg"))
            .willThrow(ImageFetchFailedException("이미지를 가져올 수 없습니다"))

        mockMvc.perform(get("/api/v1/images:proxy").param("url", "https://images.example.com/error.jpg"))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.code").value("UNAVAILABLE"))
    }

    @Test
    fun `UnsupportedImageFormatException maps to 503 UNAVAILABLE`() {
        given(proxyImageUsecase.execute("https://images.example.com/icon.svg"))
            .willThrow(UnsupportedImageFormatException("지원하지 않는 이미지 형식입니다"))

        mockMvc.perform(get("/api/v1/images:proxy").param("url", "https://images.example.com/icon.svg"))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.code").value("UNAVAILABLE"))
            .andExpect(jsonPath("$.message").value("지원하지 않는 이미지 형식입니다"))
    }
}
