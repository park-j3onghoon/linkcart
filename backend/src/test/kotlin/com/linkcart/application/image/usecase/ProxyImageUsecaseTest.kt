package com.linkcart.application.image.usecase

import com.linkcart.application.image.port.ImageFetcher
import com.linkcart.application.port.SafeUrlChecker
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willThrow
import org.mockito.Mockito.mock
import org.springframework.http.MediaType
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProxyImageUsecaseTest {

    private val safeUrlChecker = mock(SafeUrlChecker::class.java)
    private val imageFetcher = mock(ImageFetcher::class.java)
    private val sut = ProxyImageUsecase(safeUrlChecker, imageFetcher)

    @Test
    fun `returns image bytes when url is safe and content-type is allowed`() {
        val bytes = byteArrayOf(1, 2, 3)
        given(safeUrlChecker.isSafe("https://cdn/img.jpg")).willReturn(true)
        given(imageFetcher.fetch("https://cdn/img.jpg")).willReturn(
            ImageFetcher.Fetched(bytes = bytes, contentType = MediaType.IMAGE_JPEG),
        )

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
    fun `throws ImageFetchFailedException when fetcher fails`() {
        given(safeUrlChecker.isSafe("https://cdn/timeout.jpg")).willReturn(true)
        willThrow(ImageFetcher.FetchFailed("timeout")).given(imageFetcher).fetch("https://cdn/timeout.jpg")

        assertThrows<ImageFetchFailedException> { sut.execute("https://cdn/timeout.jpg") }
    }

    @Test
    fun `throws UnsupportedImageFormatException when content-type is missing`() {
        given(safeUrlChecker.isSafe("https://cdn/blob")).willReturn(true)
        given(imageFetcher.fetch("https://cdn/blob")).willReturn(
            ImageFetcher.Fetched(bytes = ByteArray(0), contentType = null),
        )

        assertThrows<UnsupportedImageFormatException> { sut.execute("https://cdn/blob") }
    }

    @Test
    fun `throws UnsupportedImageFormatException for non-image content-type`() {
        given(safeUrlChecker.isSafe("https://cdn/page")).willReturn(true)
        given(imageFetcher.fetch("https://cdn/page")).willReturn(
            ImageFetcher.Fetched(bytes = "html".toByteArray(), contentType = MediaType.TEXT_HTML),
        )

        assertThrows<UnsupportedImageFormatException> { sut.execute("https://cdn/page") }
    }

    @Test
    fun `throws UnsupportedImageFormatException for svg (XSS prevention)`() {
        given(safeUrlChecker.isSafe("https://cdn/icon.svg")).willReturn(true)
        given(imageFetcher.fetch("https://cdn/icon.svg")).willReturn(
            ImageFetcher.Fetched(bytes = "<svg/>".toByteArray(), contentType = MediaType.valueOf("image/svg+xml")),
        )

        assertThrows<UnsupportedImageFormatException> { sut.execute("https://cdn/icon.svg") }
    }

    @Test
    fun `throws UnsupportedImageFormatException for bmp (not in allowlist)`() {
        given(safeUrlChecker.isSafe("https://cdn/old.bmp")).willReturn(true)
        given(imageFetcher.fetch("https://cdn/old.bmp")).willReturn(
            ImageFetcher.Fetched(bytes = ByteArray(0), contentType = MediaType.valueOf("image/bmp")),
        )

        assertThrows<UnsupportedImageFormatException> { sut.execute("https://cdn/old.bmp") }
    }

    @Test
    fun `allows png content-type`() {
        val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
        given(safeUrlChecker.isSafe("https://cdn/img.png")).willReturn(true)
        given(imageFetcher.fetch("https://cdn/img.png")).willReturn(
            ImageFetcher.Fetched(bytes = bytes, contentType = MediaType.IMAGE_PNG),
        )

        val result = sut.execute("https://cdn/img.png")
        assertEquals(MediaType.IMAGE_PNG, result.contentType)
    }
}
