package com.linkcart.infrastructure.adapter.parser

import org.junit.jupiter.api.Test
import kotlin.test.assertIs

class ParserFactoryTest {

    private val ogParser = OgParser()
    private val factory = ParserFactory(listOf(ogParser))

    @Test
    fun `returns OgParser for unknown domain`() {
        val parser = factory.getParser("https://example.com/product/123")
        assertIs<OgParser>(parser)
    }

    @Test
    fun `returns OgParser for coupang URL when no dedicated parser exists`() {
        val parser = factory.getParser("https://www.coupang.com/vp/products/123")
        assertIs<OgParser>(parser)
    }

    @Test
    fun `returns OgParser for 11st URL when no dedicated parser exists`() {
        val parser = factory.getParser("https://www.11st.co.kr/products/123")
        assertIs<OgParser>(parser)
    }
}
