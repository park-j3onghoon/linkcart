package com.linkcart.application.parser

import com.linkcart.domain.port.ProductParser
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class ParserFactory(
    private val parsers: List<ProductParser>,
    @Qualifier("ogParser")
    private val fallbackParser: ProductParser,
) {

    fun getParser(url: String): ProductParser {
        return parsers
            .filter { it !== fallbackParser }
            .firstOrNull { it.canParse(url) }
            ?: fallbackParser
    }

    fun getFallback(): ProductParser = fallbackParser
}
