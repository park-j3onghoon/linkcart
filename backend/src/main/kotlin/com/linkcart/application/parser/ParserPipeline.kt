package com.linkcart.application.parser

import com.linkcart.domain.model.ParseResult
import com.linkcart.domain.port.FallbackProductParser
import org.springframework.stereotype.Component

@Component
class ParserPipeline(
    private val parserResolver: ParserResolver,
) {

    fun parseWithFallback(url: String): ParseResult {
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
