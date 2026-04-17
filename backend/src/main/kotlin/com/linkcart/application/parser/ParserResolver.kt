package com.linkcart.application.parser

import com.linkcart.domain.port.DedicatedProductParser
import com.linkcart.domain.port.FallbackProductParser
import com.linkcart.domain.port.ProductParser
import org.springframework.stereotype.Component

@Component
class ParserResolver(
    private val dedicatedParsers: List<DedicatedProductParser>,
    val fallbackParser: FallbackProductParser,
) {

    fun resolve(url: String): ProductParser =
        dedicatedParsers.firstOrNull { it.canParse(url) } ?: fallbackParser
}
