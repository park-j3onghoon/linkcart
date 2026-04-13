package com.linkcart.infrastructure.adapter.parser

import com.linkcart.domain.port.ProductParser
import org.springframework.stereotype.Component
import java.net.URI

@Component
class ParserFactory(
    private val parsers: List<ProductParser>,
) {

    fun getParser(url: String): ProductParser {
        val host = try {
            URI(url).host?.lowercase() ?: ""
        } catch (e: Exception) {
            ""
        }

        return parsers
            .filter { it !is OgParser }
            .firstOrNull { it.canParse(url) }
            ?: parsers.first { it is OgParser }
    }
}
