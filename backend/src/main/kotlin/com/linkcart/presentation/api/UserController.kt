package com.linkcart.presentation.api

import com.linkcart.application.user.usecase.GetMeUsecase
import com.linkcart.presentation.dto.MeResponse
import com.linkcart.presentation.dto.UserResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** AIP-156 singleton: `users/me`는 컬렉션 없이 단독 리소스로 노출. */
@RestController
class UserController(
    private val getMeUsecase: GetMeUsecase,
) {

    @GetMapping("/api/v1/users/me")
    fun me(@AuthenticationPrincipal userId: Long): ResponseEntity<MeResponse> =
        ResponseEntity.ok(MeResponse(user = UserResponse.from(getMeUsecase.execute(userId))))
}
