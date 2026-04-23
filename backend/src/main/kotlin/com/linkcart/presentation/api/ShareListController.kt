package com.linkcart.presentation.api

import com.linkcart.application.share.usecase.CopyShareListUsecase
import com.linkcart.application.share.usecase.CreateShareListUsecase
import com.linkcart.application.share.usecase.EmptyShareListException
import com.linkcart.application.share.usecase.GetShareListByTokenUsecase
import com.linkcart.application.share.usecase.ShareListNotFoundException
import com.linkcart.application.user.usecase.UserProductNotFoundException
import com.linkcart.presentation.dto.CopyShareListResponse
import com.linkcart.presentation.dto.CreateShareListRequest
import com.linkcart.presentation.dto.ShareListResponse
import com.linkcart.presentation.dto.UserProductResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/v1/share-lists")
class ShareListController(
    private val createShareListUsecase: CreateShareListUsecase,
    private val getShareListByTokenUsecase: GetShareListByTokenUsecase,
    private val copyShareListUsecase: CopyShareListUsecase,
) {

    @PostMapping
    fun create(
        @AuthenticationPrincipal ownerId: Long,
        @Valid @RequestBody request: CreateShareListRequest,
    ): ResponseEntity<ShareListResponse> {
        val shareList = try {
            createShareListUsecase.execute(
                ownerId = ownerId,
                productIds = request.productIds,
                title = request.title,
                expiresAt = request.expiresAt,
            )
        } catch (e: EmptyShareListException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
        } catch (e: UserProductNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message, e)
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ShareListResponse.from(shareList))
    }

    @GetMapping("/{token}")
    fun findByToken(@PathVariable token: String): ResponseEntity<ShareListResponse> {
        val shareList = try {
            getShareListByTokenUsecase.execute(token)
        } catch (e: ShareListNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message, e)
        }
        return ResponseEntity.ok(ShareListResponse.from(shareList))
    }

    @PostMapping("/{token}/copy")
    fun copy(
        @AuthenticationPrincipal viewerId: Long,
        @PathVariable token: String,
    ): ResponseEntity<CopyShareListResponse> {
        val result = try {
            copyShareListUsecase.execute(viewerId = viewerId, token = token)
        } catch (e: ShareListNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message, e)
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(
            CopyShareListResponse(
                copiedCount = result.copiedCount,
                skippedCount = result.skippedCount,
                products = result.products.map(UserProductResponse::from),
            ),
        )
    }
}
