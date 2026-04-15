package com.linkcart.infrastructure.adapter.parser

import com.linkcart.domain.port.ProductParser
import org.springframework.stereotype.Component

@Component
class ParserFactory(
    private val parsers: List<ProductParser>,
) {

    private val fallbackParser: ProductParser

    init {
        fallbackParser = parsers.firstOrNull { it.canParse("") && it.canParse("https://any.com") }
            ?: throw IllegalStateException("폴백 파서가 등록되어 있지 않습니다")
    }

    fun getParser(url: String): ProductParser {
        return parsers
            .filter { it !== fallbackParser }
            .firstOrNull { it.canParse(url) }
            ?: fallbackParser
    }
}
