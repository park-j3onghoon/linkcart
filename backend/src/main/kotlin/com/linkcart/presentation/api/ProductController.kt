package com.linkcart.presentation.api

import com.linkcart.application.usecase.ParseProductUseCase
import com.linkcart.domain.model.ParseResult
import com.linkcart.presentation.dto.ParseRequest
import com.linkcart.presentation.dto.ParseResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/v1/products")
class ProductController(
    private val parseProductUseCase: ParseProductUseCase,
) {

    @PostMapping("/parse")
    fun parse(
        @Valid @RequestBody request: ParseRequest,
    ): ParseResponse {
        return when (val result = parseProductUseCase.execute(request.url)) {
            is ParseResult.Failure -> throw ResponseStatusException(HttpStatus.BAD_GATEWAY, result.reason)
            else -> ParseResponse.from(result, request.url)
        }
    }
}
