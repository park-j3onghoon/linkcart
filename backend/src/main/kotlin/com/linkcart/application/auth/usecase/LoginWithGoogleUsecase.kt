package com.linkcart.application.auth.usecase

import com.linkcart.application.auth.port.GoogleOAuthClient
import com.linkcart.domain.model.User
import com.linkcart.domain.model.AuthProvider
import com.linkcart.domain.port.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LoginWithGoogleUsecase(
    private val googleOAuthClient: GoogleOAuthClient,
    private val userRepository: UserRepository,
    private val issueTokensUsecase: IssueTokensUsecase,
) {

    @Transactional
    fun execute(code: String, redirectUri: String): LoginResult {
        val identity = googleOAuthClient.exchangeCodeForIdentity(code, redirectUri)

        val user = userRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, identity.subject)
            ?: userRepository.save(
                User(
                    provider = AuthProvider.GOOGLE,
                    providerUserId = identity.subject,
                    email = identity.email,
                    displayName = identity.displayName,
                    avatarUrl = identity.avatarUrl,
                ),
            )

        val tokens = issueTokensUsecase.execute(userId = requireNotNull(user.id))

        return LoginResult(user = user, tokens = tokens)
    }

    data class LoginResult(val user: User, val tokens: IssueTokensUsecase.IssueResult)
}
