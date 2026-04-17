package com.linkcart.infrastructure.adapter.parser

import com.linkcart.domain.model.ParseResult
import com.linkcart.domain.vo.Mall
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OgParserTest {

    private val parser = OgParser(SafeUrlChecker())

    @Test
    fun `canParse is always true for any URL`() {
        assertTrue(parser.canParse("https://example.com"))
        assertTrue(parser.canParse("https://anything.kr/product/123"))
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
        assertEquals("og", result.parserUsed)
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
        assertEquals("Some Product", result.fields["name"])
        assertEquals("og", result.parserUsed)
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
        assertEquals("상품 상세 - 쇼핑몰", result.fields["name"])
    }

    @Test
    fun `empty HTML returns Failure`() {
        val result = parser.parseHtml("", "https://example.com/empty")

        assertIs<ParseResult.Failure>(result)
        assertEquals("og", result.parserUsed)
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
        assertEquals("가격 깨진 상품", result.fields["name"])
        assertEquals("https://example.com/image.jpg", result.fields["imageUrl"])
        assertFalse(result.fields.containsKey("price"))
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
        assertEquals("https://example.com/only-image.jpg", result.fields["imageUrl"])
        assertFalse(result.fields.containsKey("name"))
        assertFalse(result.fields.containsKey("price"))
    }

    @Test
    fun `html without parseable content returns Failure`() {
        val html = "<html><body><div>no meta tags here</div></body></html>"

        val result = parser.parseHtml(html, "https://example.com/product/empty-meta")

        assertIs<ParseResult.Failure>(result)
        assertEquals("파싱 가능한 정보가 없습니다", result.reason)
        assertEquals("og", result.parserUsed)
    }
}
