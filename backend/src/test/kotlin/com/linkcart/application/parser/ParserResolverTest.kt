package com.linkcart.application.parser

import com.linkcart.domain.model.ParseResult
import com.linkcart.domain.vo.ParserName
import com.linkcart.infrastructure.adapter.parser.DefaultSafeUrlChecker
import com.linkcart.infrastructure.adapter.parser.OgParser
import org.junit.jupiter.api.Test
import kotlin.test.assertSame

class ParserResolverTest {

    private val ogParser = OgParser(DefaultSafeUrlChecker())

    @Test
    fun `returns fallback when no dedicated parser is registered`() {
        val resolver = ParserResolver(dedicatedParsers = emptyList(), fallbackParser = ogParser)

        val parser = resolver.resolve("https://example.com/product/123")

        assertSame(ogParser, parser)
    }

    @Test
    fun `dedicated parser takes priority over fallback`() {
        val dedicated = dedicatedStub(
            canParse = { it.contains("coupang.com") },
            parse = { ParseResult.Failure("stub", ParserName.OG) },
        )
        val resolver = ParserResolver(dedicatedParsers = listOf(dedicated), fallbackParser = ogParser)

        val parser = resolver.resolve("https://www.coupang.com/vp/products/123")

        assertSame(dedicated, parser)
    }

    @Test
    fun `falls back when dedicated parser cannot parse URL`() {
        val dedicated = dedicatedStub(
            canParse = { it.contains("coupang.com") },
            parse = { ParseResult.Failure("stub", ParserName.OG) },
        )
        val resolver = ParserResolver(dedicatedParsers = listOf(dedicated), fallbackParser = ogParser)

        val parser = resolver.resolve("https://example.com/product/123")

        assertSame(ogParser, parser)
    }

    @Test
    fun `fallbackParser property exposes configured fallback`() {
        val resolver = ParserResolver(dedicatedParsers = emptyList(), fallbackParser = ogParser)

        assertSame(ogParser, resolver.fallbackParser)
    }
}
