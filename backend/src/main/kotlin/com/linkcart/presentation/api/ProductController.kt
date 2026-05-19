package com.linkcart.presentation.api

import com.linkcart.application.usecase.ParseProductUseCase
import com.linkcart.domain.model.ParseResult
import com.linkcart.application.parser.port.SafeUrlChecker
import com.linkcart.presentation.dto.ParseRequest
import com.linkcart.presentation.dto.ParseResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class ProductController(
    private val parseProductUseCase: ParseProductUseCase,
    private val safeUrlChecker: SafeUrlChecker,
) {

    @PostMapping("/api/v1/products:parse")
    fun parse(
        @Valid @RequestBody request: ParseRequest,
    ): ParseResponse {
        if (!safeUrlChecker.isSafe(request.url)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "허용되지 않는 URL입니다")
        }

        return when (val result = parseProductUseCase.execute(request.url)) {
            is ParseResult.Failure -> throw ResponseStatusException(HttpStatus.BAD_GATEWAY, result.reason)
            else -> ParseResponse.from(result, request.url)
        }
    }
}
