package com.linkcart.application.parser.usecase

import com.linkcart.application.parser.ParserPipeline
import com.linkcart.domain.model.ParseResult
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class ParseProductUsecase(
    private val parserPipeline: ParserPipeline,
) {

    @Cacheable(
        cacheNames = ["products"],
        key = "#url",
        unless = "#result instanceof T(com.linkcart.domain.model.ParseResult\$Failure)",
    )
    fun execute(url: String): ParseResult = parserPipeline.parseWithFallback(url)
}
