package com.linkcart.presentation.api

import com.linkcart.application.share.usecase.CopyShareListUsecase
import com.linkcart.application.share.usecase.CreateShareListUsecase
import com.linkcart.application.share.usecase.DeleteShareListUsecase
import com.linkcart.application.share.usecase.LookupShareListByTokenUsecase
import com.linkcart.presentation.dto.CopyShareListRequest
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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ShareListController(
    private val createShareListUsecase: CreateShareListUsecase,
    private val lookupShareListByTokenUsecase: LookupShareListByTokenUsecase,
    private val copyShareListUsecase: CopyShareListUsecase,
    private val deleteShareListUsecase: DeleteShareListUsecase,
) {

    @PostMapping("/api/v1/shareLists")
    fun create(
        @AuthenticationPrincipal ownerId: Long,
        @Valid @RequestBody request: CreateShareListRequest,
    ): ResponseEntity<ShareListResponse> {
        val shareList = createShareListUsecase.execute(
            ownerId = ownerId,
            productIds = request.productIds,
            title = request.title,
            expiresAt = request.expireTime,
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(ShareListResponse.from(shareList))
    }

    /**
     * 비로그인 공유 페이지가 사용하는 토큰 → 리소스 조회.
     * 토큰은 secret이므로 URL이 아닌 request body로 받는다.
     * ID 기반 GET endpoint는 일부러 노출하지 않는다 (enumeration 방지).
     */
    @PostMapping("/api/v1/shareLists:lookup")
    fun lookupByToken(
        @Valid @RequestBody request: LookupShareListRequest,
    ): ResponseEntity<ShareListResponse> {
        val shareList = lookupShareListByTokenUsecase.execute(request.token)
        return ResponseEntity.ok(ShareListResponse.from(shareList))
    }

    @PostMapping("/api/v1/shareLists/{id}:copy")
    fun copy(
        @AuthenticationPrincipal viewerId: Long,
        @PathVariable id: Long,
        @Valid @RequestBody request: CopyShareListRequest,
    ): ResponseEntity<CopyShareListResponse> {
        val result = copyShareListUsecase.execute(
            viewerId = viewerId,
            shareListId = id,
            token = request.token,
        )
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
        deleteShareListUsecase.execute(ownerId = ownerId, shareListId = id)
        return ResponseEntity.noContent().build()
    }
}
