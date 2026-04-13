package com.linkcart.infrastructure.adapter.parser

import com.linkcart.domain.model.ParseResult
import com.linkcart.domain.vo.Mall
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OgParserTest {

    private val parser = OgParser()

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
}
