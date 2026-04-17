package com.linkcart.presentation.api

import com.linkcart.infrastructure.adapter.parser.SafeUrlChecker
import com.linkcart.infrastructure.config.WebConfig
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.ComponentScan.Filter
import org.springframework.context.annotation.FilterType
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

@WebMvcTest(
    controllers = [ImageProxyController::class],
    excludeFilters = [Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [WebConfig::class])],
)
@Import(GlobalExceptionHandler::class)
class ImageProxyControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var safeUrlChecker: SafeUrlChecker

    @MockBean(name = "imageProxyRestTemplate")
    @Qualifier("imageProxyRestTemplate")
    private lateinit var restTemplate: RestTemplate

    @Test
    fun `successful image proxy returns image bytes`() {
        val imageBytes = byteArrayOf(1, 2, 3, 4)
        val headers = HttpHeaders().apply { contentType = MediaType.IMAGE_JPEG }
        given(safeUrlChecker.isSafe("https://images.example.com/test.jpg")).willReturn(true)
        given(
            restTemplate.exchange(
                "https://images.example.com/test.jpg",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                ByteArray::class.java,
            ),
        ).willReturn(ResponseEntity(imageBytes, headers, HttpStatus.OK))

        mockMvc.perform(get("/api/v1/images/proxy").param("url", "https://images.example.com/test.jpg"))
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE))
            .andExpect(content().bytes(imageBytes))
    }

    @Test
    fun `blank url returns 400 response`() {
        mockMvc.perform(get("/api/v1/images/proxy").param("url", ""))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("validation_error"))
    }

    @Test
    fun `unsafe url returns 400 response`() {
        given(safeUrlChecker.isSafe("http://127.0.0.1/internal.png")).willReturn(false)

        mockMvc.perform(get("/api/v1/images/proxy").param("url", "http://127.0.0.1/internal.png"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("invalid_request"))
    }

    @Test
    fun `image fetch failure returns 502 response`() {
        given(safeUrlChecker.isSafe("https://images.example.com/error.jpg")).willReturn(true)
        given(
            restTemplate.exchange(
                "https://images.example.com/error.jpg",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                ByteArray::class.java,
            ),
        ).willThrow(RestClientException("timeout"))

        mockMvc.perform(get("/api/v1/images/proxy").param("url", "https://images.example.com/error.jpg"))
            .andExpect(status().isBadGateway)
            .andExpect(jsonPath("$.code").value("upstream_error"))
    }

    @Test
    fun `non image response returns 502 response`() {
        val headers = HttpHeaders().apply { contentType = MediaType.TEXT_HTML }
        given(safeUrlChecker.isSafe("https://images.example.com/not-image")).willReturn(true)
        given(
            restTemplate.exchange(
                "https://images.example.com/not-image",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                ByteArray::class.java,
            ),
        ).willReturn(ResponseEntity("not image".toByteArray(), headers, HttpStatus.OK))

        mockMvc.perform(get("/api/v1/images/proxy").param("url", "https://images.example.com/not-image"))
            .andExpect(status().isBadGateway)
            .andExpect(jsonPath("$.code").value("upstream_error"))
            .andExpect(jsonPath("$.message").value("이미지 응답이 아닙니다"))
    }
}
