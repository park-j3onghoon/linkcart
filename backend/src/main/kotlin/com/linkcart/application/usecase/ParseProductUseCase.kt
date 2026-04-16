package com.linkcart.application.usecase

import com.linkcart.application.parser.ParserFactory
import com.linkcart.domain.model.ParseResult
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class ParseProductUseCase(
    private val parserFactory: ParserFactory,
) {

    @Cacheable(
        cacheNames = ["products"],
        key = "#url",
        unless = "#result instanceof T(com.linkcart.domain.model.ParseResult\$Failure)",
    )
    fun execute(url: String): ParseResult {
        val primaryParser = parserFactory.getParser(url)
        val primaryResult = primaryParser.parse(url)
        val fallbackParser = parserFactory.getFallback()

        if (primaryResult !is ParseResult.Failure || primaryParser === fallbackParser) {
            return primaryResult
        }

        val fallbackResult = fallbackParser.parse(url)
        return when (fallbackResult) {
            is ParseResult.Success -> fallbackResult.copy(fallbackUsed = true)
            is ParseResult.Partial -> fallbackResult.copy(fallbackUsed = true)
            is ParseResult.Failure -> ParseResult.Failure(
                reason = "전용 파서(${primaryResult.parserUsed}) 실패: ${primaryResult.reason}; 폴백(${fallbackResult.parserUsed}) 실패: ${fallbackResult.reason}",
                parserUsed = fallbackResult.parserUsed,
            )
        }
    }
}
