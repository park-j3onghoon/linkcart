package com.linkcart.application.user.usecase

import com.linkcart.domain.model.User
import com.linkcart.domain.port.UserRepository
import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Service

class UnauthenticatedException(message: String) : AuthenticationException(message)

/** AIP-156 singleton `users/me` 조회. */
@Service
class GetMeUsecase(
    private val userRepository: UserRepository,
) {
    fun execute(userId: Long): User =
        userRepository.findById(userId)
            ?: throw UnauthenticatedException("인증이 필요합니다")
}
