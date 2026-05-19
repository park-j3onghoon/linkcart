package com.linkcart.application.image.usecase

import com.linkcart.application.parser.port.SafeUrlChecker
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProxyImageUsecaseTest {

    private val safeUrlChecker = mock(SafeUrlChecker::class.java)
    private val restTemplate = mock(RestTemplate::class.java)
    private val sut = ProxyImageUsecase(safeUrlChecker, restTemplate)

    @Test
    fun `returns image bytes when url is safe and content-type is allowed`() {
        val bytes = byteArrayOf(1, 2, 3)
        val headers = HttpHeaders().apply { contentType = MediaType.IMAGE_JPEG }
        given(safeUrlChecker.isSafe("https://cdn/img.jpg")).willReturn(true)
        given(
            restTemplate.exchange("https://cdn/img.jpg", HttpMethod.GET, HttpEntity.EMPTY, ByteArray::class.java),
        ).willReturn(ResponseEntity(bytes, headers, HttpStatus.OK))

        val result = sut.execute("https://cdn/img.jpg")

        assertEquals(MediaType.IMAGE_JPEG, result.contentType)
        assertTrue(bytes.contentEquals(result.bytes))
    }

    @Test
    fun `throws UnsafeImageUrlException when url is not safe`() {
        given(safeUrlChecker.isSafe("http://127.0.0.1/x.png")).willReturn(false)

        assertThrows<UnsafeImageUrlException> { sut.execute("http://127.0.0.1/x.png") }
    }

    @Test
    fun `throws ImageFetchFailedException when upstream fails`() {
        given(safeUrlChecker.isSafe("https://cdn/timeout.jpg")).willReturn(true)
        given(
            restTemplate.exchange("https://cdn/timeout.jpg", HttpMethod.GET, HttpEntity.EMPTY, ByteArray::class.java),
        ).willThrow(RestClientException("timeout"))

        assertThrows<ImageFetchFailedException> { sut.execute("https://cdn/timeout.jpg") }
    }

    @Test
    fun `throws UnsupportedImageFormatException when content-type is missing`() {
        given(safeUrlChecker.isSafe("https://cdn/blob")).willReturn(true)
        given(
            restTemplate.exchange("https://cdn/blob", HttpMethod.GET, HttpEntity.EMPTY, ByteArray::class.java),
        ).willReturn(ResponseEntity(ByteArray(0), HttpHeaders(), HttpStatus.OK))

        assertThrows<UnsupportedImageFormatException> { sut.execute("https://cdn/blob") }
    }

    @Test
    fun `throws UnsupportedImageFormatException for non-image content-type`() {
        val headers = HttpHeaders().apply { contentType = MediaType.TEXT_HTML }
        given(safeUrlChecker.isSafe("https://cdn/page")).willReturn(true)
        given(
            restTemplate.exchange("https://cdn/page", HttpMethod.GET, HttpEntity.EMPTY, ByteArray::class.java),
        ).willReturn(ResponseEntity("html".toByteArray(), headers, HttpStatus.OK))

        assertThrows<UnsupportedImageFormatException> { sut.execute("https://cdn/page") }
    }

    @Test
    fun `throws UnsupportedImageFormatException for svg (XSS prevention)`() {
        val headers = HttpHeaders().apply { contentType = MediaType.valueOf("image/svg+xml") }
        given(safeUrlChecker.isSafe("https://cdn/icon.svg")).willReturn(true)
        given(
            restTemplate.exchange("https://cdn/icon.svg", HttpMethod.GET, HttpEntity.EMPTY, ByteArray::class.java),
        ).willReturn(ResponseEntity("<svg/>".toByteArray(), headers, HttpStatus.OK))

        assertThrows<UnsupportedImageFormatException> { sut.execute("https://cdn/icon.svg") }
    }

    @Test
    fun `throws UnsupportedImageFormatException for bmp (not in allowlist)`() {
        val headers = HttpHeaders().apply { contentType = MediaType.valueOf("image/bmp") }
        given(safeUrlChecker.isSafe("https://cdn/old.bmp")).willReturn(true)
        given(
            restTemplate.exchange("https://cdn/old.bmp", HttpMethod.GET, HttpEntity.EMPTY, ByteArray::class.java),
        ).willReturn(ResponseEntity(ByteArray(0), headers, HttpStatus.OK))

        assertThrows<UnsupportedImageFormatException> { sut.execute("https://cdn/old.bmp") }
    }

    @Test
    fun `allows png content-type`() {
        val headers = HttpHeaders().apply { contentType = MediaType.IMAGE_PNG }
        val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
        given(safeUrlChecker.isSafe("https://cdn/img.png")).willReturn(true)
        given(
            restTemplate.exchange("https://cdn/img.png", HttpMethod.GET, HttpEntity.EMPTY, ByteArray::class.java),
        ).willReturn(ResponseEntity(bytes, headers, HttpStatus.OK))

        val result = sut.execute("https://cdn/img.png")
        assertEquals(MediaType.IMAGE_PNG, result.contentType)
    }
}
