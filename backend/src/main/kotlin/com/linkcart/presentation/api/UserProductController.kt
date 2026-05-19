package com.linkcart.presentation.api

import com.linkcart.application.user.usecase.DeleteUserProductUsecase
import com.linkcart.application.user.usecase.ListUserProductsUsecase
import com.linkcart.application.user.usecase.SaveUserProductUsecase
import com.linkcart.presentation.dto.SaveUserProductRequest
import com.linkcart.presentation.dto.UserProductResponse
import com.linkcart.presentation.dto.UserProductsResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/me/products")
class UserProductController(
    private val saveUserProductUsecase: SaveUserProductUsecase,
    private val listUserProductsUsecase: ListUserProductsUsecase,
    private val deleteUserProductUsecase: DeleteUserProductUsecase,
) {

    @PostMapping
    fun save(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: SaveUserProductRequest,
    ): ResponseEntity<UserProductResponse> {
        val saved = saveUserProductUsecase.execute(request.toDomain(userId))
        return ResponseEntity.status(HttpStatus.CREATED).body(UserProductResponse.from(saved))
    }

    @GetMapping
    fun list(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(defaultValue = "50") pageSize: Int,
        @RequestParam(required = false) pageToken: String?,
    ): ResponseEntity<UserProductsResponse> {
        val page = listUserProductsUsecase.execute(userId = userId, pageSize = pageSize, pageToken = pageToken)
        return ResponseEntity.ok(
            UserProductsResponse(
                products = page.products.map(UserProductResponse::from),
                nextPageToken = page.nextPageToken,
            ),
        )
    }

    @DeleteMapping("/{productId}")
    fun delete(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
    ): ResponseEntity<Void> {
        deleteUserProductUsecase.execute(userId = userId, productId = productId)
        return ResponseEntity.noContent().build()
    }
}
