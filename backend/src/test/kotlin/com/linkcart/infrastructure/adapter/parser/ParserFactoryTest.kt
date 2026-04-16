package com.linkcart.infrastructure.adapter.parser

import com.linkcart.application.parser.ParserFactory
import com.linkcart.domain.model.ParseResult
import com.linkcart.domain.port.ProductParser
import org.junit.jupiter.api.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

class ParserFactoryTest {

    private val ogParser = OgParser()
    private val factory = ParserFactory(listOf(ogParser), ogParser)

    @Test
    fun `returns fallback parser for unknown domain`() {
        val parser = factory.getParser("https://example.com/product/123")
        assertSame(ogParser, parser)
    }

    @Test
    fun `returns fallback for coupang URL when no dedicated parser exists`() {
        val parser = factory.getParser("https://www.coupang.com/vp/products/123")
        assertSame(ogParser, parser)
    }

    @Test
    fun `dedicated parser takes priority over fallback`() {
        val dedicated = object : ProductParser {
            override fun canParse(url: String) = url.contains("coupang.com")
            override fun parse(url: String) = ParseResult.Failure("stub", "test")
        }
        val factoryWithDedicated = ParserFactory(listOf(dedicated, ogParser), ogParser)

        val parser = factoryWithDedicated.getParser("https://www.coupang.com/vp/products/123")
        assertSame(dedicated, parser)
    }

    @Test
    fun `falls back when dedicated parser cannot parse URL`() {
        val dedicated = object : ProductParser {
            override fun canParse(url: String) = url.contains("coupang.com")
            override fun parse(url: String) = ParseResult.Failure("stub", "test")
        }
        val factoryWithDedicated = ParserFactory(listOf(dedicated, ogParser), ogParser)

        val parser = factoryWithDedicated.getParser("https://example.com/product/123")
        assertSame(ogParser, parser)
    }

    @Test
    fun `getFallback returns configured fallback parser`() {
        assertSame(ogParser, factory.getFallback())
    }
}
