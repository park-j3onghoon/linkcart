package com.linkcart.application.user.usecase

import com.linkcart.domain.model.User
import com.linkcart.domain.port.UserRepository
import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Service

class UnauthenticatedException(message: String) : AuthenticationException(message)

/**
 * AIP-156 singleton: 인증된 사용자 본인의 리소스(`users/me`) 조회 유스케이스.
 * 컨트롤러가 Repository를 직접 참조하지 않도록 application 레이어에 둔다.
 */
@Service
class GetMeUsecase(
    private val userRepository: UserRepository,
) {
    fun execute(userId: Long): User =
        userRepository.findById(userId)
            ?: throw UnauthenticatedException("인증이 필요합니다")
}
