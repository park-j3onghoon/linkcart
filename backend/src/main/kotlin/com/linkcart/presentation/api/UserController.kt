package com.linkcart.presentation.api

import com.linkcart.application.user.usecase.GetMeUsecase
import com.linkcart.presentation.dto.MeResponse
import com.linkcart.presentation.dto.UserResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * AIP-156 singleton: 현재 인증된 사용자 리소스(`users/me`)는 컬렉션 없이 단독으로 노출한다.
 */
@RestController
class UserController(
    private val getMeUsecase: GetMeUsecase,
) {

    @GetMapping("/api/v1/users/me")
    fun me(@AuthenticationPrincipal userId: Long): ResponseEntity<MeResponse> =
        ResponseEntity.ok(MeResponse(user = UserResponse.from(getMeUsecase.execute(userId))))
}
