package com.linkcart.application.usecase

import com.linkcart.application.parser.ParserResolver
import com.linkcart.domain.model.ParseResult
import com.linkcart.domain.port.FallbackProductParser
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class ParseProductUseCase(
    private val parserResolver: ParserResolver,
) {

    @Cacheable(
        cacheNames = ["products"],
        key = "#url",
        unless = "#result instanceof T(com.linkcart.domain.model.ParseResult\$Failure)",
    )
    fun execute(url: String): ParseResult {
        val primaryParser = parserResolver.resolve(url)
        val primaryResult = primaryParser.parse(url)

        // primary가 폴백 자체이면 재시도 의미 없음 — 단락.
        if (primaryResult !is ParseResult.Failure || primaryParser is FallbackProductParser) {
            return primaryResult
        }

        val fallbackResult = parserResolver.fallbackParser.parse(url)
        return when (fallbackResult) {
            is ParseResult.Success -> fallbackResult.copy(fallbackUsed = true)
            is ParseResult.Partial -> fallbackResult.copy(fallbackUsed = true)
            is ParseResult.Failure -> ParseResult.Failure(
                reason = "전용 파서(${primaryResult.parserUsed.code}) 실패: ${primaryResult.reason}; 폴백(${fallbackResult.parserUsed.code}) 실패: ${fallbackResult.reason}",
                parserUsed = fallbackResult.parserUsed,
            )
        }
    }
}
