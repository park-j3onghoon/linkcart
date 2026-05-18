package com.linkcart.presentation.api

import com.linkcart.domain.port.UserRepository
import com.linkcart.presentation.dto.MeResponse
import com.linkcart.presentation.dto.UserResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * AIP-156 singleton: 현재 인증된 사용자 리소스(`users/me`)는 컬렉션 없이 단독으로 노출한다.
 */
@RestController
class UserController(
    private val userRepository: UserRepository,
) {

    @GetMapping("/api/v1/users/me")
    fun me(@AuthenticationPrincipal userId: Long): ResponseEntity<MeResponse> {
        val user = userRepository.findById(userId)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다")
        return ResponseEntity.ok(MeResponse(user = UserResponse.from(user)))
    }
}
