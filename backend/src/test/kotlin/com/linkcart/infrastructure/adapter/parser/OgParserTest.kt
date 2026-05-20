package com.linkcart.infrastructure.adapter.parser

import com.linkcart.application.port.SafeUrlChecker
import com.linkcart.domain.model.ParseResult
import com.linkcart.domain.vo.ParserName
import com.linkcart.domain.vo.Mall
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class OgParserTest {

    private val parser = OgParser(DefaultSafeUrlChecker())

    @Test
    fun `parse returns Failure when URL is unsafe`() {
        val unsafeChecker: SafeUrlChecker = mock(SafeUrlChecker::class.java)
        given(unsafeChecker.isSafe("http://169.254.169.254/internal")).willReturn(false)
        val sut = OgParser(unsafeChecker)

        val result = sut.parse("http://169.254.169.254/internal")

        assertIs<ParseResult.Failure>(result)
        assertEquals("허용되지 않는 URL입니다", result.reason)
        assertEquals(ParserName.OG, result.parserUsed)
    }

    @Test
    fun `parse returns Failure when upstream fetch fails`() {
        val safeChecker: SafeUrlChecker = mock(SafeUrlChecker::class.java)
        given(safeChecker.isSafe(org.mockito.ArgumentMatchers.anyString())).willReturn(true)
        val sut = OgParser(safeChecker)

        // 127.0.0.1:1 → 닫힌 포트라 Jsoup이 IOException으로 즉시 실패
        val result = sut.parse("http://127.0.0.1:1/nope")

        assertIs<ParseResult.Failure>(result)
        assertEquals(ParserName.OG, result.parserUsed)
    }

    @Test
    fun `parses full OG tags into Success`() {
        val html = """
            <html><head>
                <meta property="og:title" content="iPhone 16 Pro 256GB">
                <meta property="og:image" content="https://example.com/image.jpg">
                <meta property="product:price:amount" content="1550000">
                <meta property="product:price:currency" content="KRW">
            </head><body></body></html>
        """.trimIndent()

        val result = parser.parseHtml(html, "https://example.com/product/1")

        assertIs<ParseResult.Success>(result)
        assertEquals("iPhone 16 Pro 256GB", result.product.name)
        assertEquals(1550000L, result.product.price.amount)
        assertEquals("https://example.com/image.jpg", result.product.imageUrl)
        assertEquals("https://example.com/product/1", result.product.sourceUrl)
        assertEquals(Mall.GENERIC, result.product.mall)
        assertEquals(ParserName.OG, result.parserUsed)
    }

    @Test
    fun `partial OG tags with only title returns Partial`() {
        val html = """
            <html><head>
                <meta property="og:title" content="Some Product">
            </head><body></body></html>
        """.trimIndent()

        val result = parser.parseHtml(html, "https://example.com/product/2")

        assertIs<ParseResult.Partial>(result)
        assertEquals("Some Product", result.name)
        assertEquals(ParserName.OG, result.parserUsed)
    }

    @Test
    fun `no OG tags falls back to title tag`() {
        val html = """
            <html><head>
                <title>상품 상세 - 쇼핑몰</title>
            </head><body></body></html>
        """.trimIndent()

        val result = parser.parseHtml(html, "https://example.com/product/3")

        assertIs<ParseResult.Partial>(result)
        assertEquals("상품 상세 - 쇼핑몰", result.name)
    }

    @Test
    fun `empty HTML returns Failure`() {
        val result = parser.parseHtml("", "https://example.com/empty")

        assertIs<ParseResult.Failure>(result)
        assertEquals(ParserName.OG, result.parserUsed)
    }

    @Test
    fun `price without currency returns Success with KRW`() {
        val html = """
            <html><head>
                <meta property="og:title" content="통화 생략 상품">
                <meta property="og:image" content="https://example.com/image.jpg">
                <meta property="product:price:amount" content="99000">
            </head><body></body></html>
        """.trimIndent()

        val result = parser.parseHtml(html, "https://example.com/product/no-currency")

        assertIs<ParseResult.Success>(result)
        assertEquals(99000L, result.product.price.amount)
        assertEquals("KRW", result.product.price.currency)
    }

    @Test
    fun `non-numeric price returns Partial`() {
        val html = """
            <html><head>
                <meta property="og:title" content="가격 깨진 상품">
                <meta property="og:image" content="https://example.com/image.jpg">
                <meta property="product:price:amount" content="문의가격">
            </head><body></body></html>
        """.trimIndent()

        val result = parser.parseHtml(html, "https://example.com/product/broken-price")

        assertIs<ParseResult.Partial>(result)
        assertEquals("가격 깨진 상품", result.name)
        assertEquals("https://example.com/image.jpg", result.imageUrl)
        assertFalse(result.price != null)
    }

    @Test
    fun `image-only content returns Partial with imageUrl`() {
        val html = """
            <html><head>
                <meta property="og:image" content="https://example.com/only-image.jpg">
            </head><body></body></html>
        """.trimIndent()

        val result = parser.parseHtml(html, "https://example.com/product/image-only")

        assertIs<ParseResult.Partial>(result)
        assertEquals("https://example.com/only-image.jpg", result.imageUrl)
        assertFalse(result.name != null)
        assertFalse(result.price != null)
    }

    @Test
    fun `html without parseable content returns Failure`() {
        val html = "<html><body><div>no meta tags here</div></body></html>"

        val result = parser.parseHtml(html, "https://example.com/product/empty-meta")

        assertIs<ParseResult.Failure>(result)
        assertEquals("파싱 가능한 정보가 없습니다", result.reason)
        assertEquals(ParserName.OG, result.parserUsed)
    }
}
