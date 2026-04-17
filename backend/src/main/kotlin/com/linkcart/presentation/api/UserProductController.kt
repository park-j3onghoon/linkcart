package com.linkcart.presentation.api

import com.linkcart.application.user.usecase.DeleteUserProductUsecase
import com.linkcart.application.user.usecase.DuplicateUserProductException
import com.linkcart.application.user.usecase.ListUserProductsUsecase
import com.linkcart.application.user.usecase.SaveUserProductUsecase
import com.linkcart.application.user.usecase.UserProductNotFoundException
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
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

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
        val saved = try {
            saveUserProductUsecase.execute(request.toDomain(userId))
        } catch (e: DuplicateUserProductException) {
            throw ResponseStatusException(HttpStatus.CONFLICT, e.message, e)
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(UserProductResponse.from(saved))
    }

    @GetMapping
    fun list(@AuthenticationPrincipal userId: Long): ResponseEntity<UserProductsResponse> {
        val products = listUserProductsUsecase.execute(userId).map(UserProductResponse::from)
        return ResponseEntity.ok(UserProductsResponse(products = products))
    }

    @DeleteMapping("/{productId}")
    fun delete(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
    ): ResponseEntity<Void> {
        try {
            deleteUserProductUsecase.execute(userId = userId, productId = productId)
        } catch (e: UserProductNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message, e)
        }
        return ResponseEntity.noContent().build()
    }
}
