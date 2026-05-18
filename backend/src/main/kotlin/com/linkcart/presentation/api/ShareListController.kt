package com.linkcart.presentation.api

import com.linkcart.application.share.usecase.CopyShareListUsecase
import com.linkcart.application.share.usecase.CreateShareListUsecase
import com.linkcart.application.share.usecase.DeleteShareListUsecase
import com.linkcart.application.share.usecase.EmptyShareListException
import com.linkcart.application.share.usecase.GetShareListByIdUsecase
import com.linkcart.application.share.usecase.LookupShareListByTokenUsecase
import com.linkcart.application.share.usecase.ShareListNotFoundException
import com.linkcart.application.user.usecase.UserProductNotFoundException
import com.linkcart.presentation.dto.CopyShareListResponse
import com.linkcart.presentation.dto.CreateShareListRequest
import com.linkcart.presentation.dto.LookupShareListRequest
import com.linkcart.presentation.dto.ShareListResponse
import com.linkcart.presentation.dto.UserProductResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class ShareListController(
    private val createShareListUsecase: CreateShareListUsecase,
    private val getShareListByIdUsecase: GetShareListByIdUsecase,
    private val lookupShareListByTokenUsecase: LookupShareListByTokenUsecase,
    private val copyShareListUsecase: CopyShareListUsecase,
    private val deleteShareListUsecase: DeleteShareListUsecase,
) {

    @PostMapping("/api/v1/shareLists")
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

    @GetMapping("/api/v1/shareLists/{id}")
    fun get(@PathVariable id: Long): ResponseEntity<ShareListResponse> {
        val shareList = try {
            getShareListByIdUsecase.execute(id)
        } catch (e: ShareListNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message, e)
        }
        return ResponseEntity.ok(ShareListResponse.from(shareList))
    }

    /**
     * 비로그인 공유 페이지가 사용하는 토큰 → 리소스 조회.
     * 토큰은 secret이므로 URL이 아닌 request body로 받는다.
     */
    @PostMapping("/api/v1/shareLists:lookup")
    fun lookupByToken(
        @Valid @RequestBody request: LookupShareListRequest,
    ): ResponseEntity<ShareListResponse> {
        val shareList = try {
            lookupShareListByTokenUsecase.execute(request.token)
        } catch (e: ShareListNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message, e)
        }
        return ResponseEntity.ok(ShareListResponse.from(shareList))
    }

    @PostMapping("/api/v1/shareLists/{id}:copy")
    fun copy(
        @AuthenticationPrincipal viewerId: Long,
        @PathVariable id: Long,
    ): ResponseEntity<CopyShareListResponse> {
        val result = try {
            copyShareListUsecase.execute(viewerId = viewerId, shareListId = id)
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

    @DeleteMapping("/api/v1/shareLists/{id}")
    fun delete(
        @AuthenticationPrincipal ownerId: Long,
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        try {
            deleteShareListUsecase.execute(ownerId = ownerId, shareListId = id)
        } catch (e: ShareListNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message, e)
        }
        return ResponseEntity.noContent().build()
    }
}
