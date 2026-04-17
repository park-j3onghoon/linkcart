package com.linkcart.application.auth.usecase

import com.linkcart.application.auth.port.AccessTokenIssuer
import com.linkcart.application.auth.port.GoogleOAuthClient
import com.linkcart.domain.entity.User
import com.linkcart.domain.model.AccessToken
import com.linkcart.domain.model.AuthProvider
import com.linkcart.domain.port.UserRepository
import org.springframework.stereotype.Service

@Service
class LoginWithGoogleUsecase(
    private val googleOAuthClient: GoogleOAuthClient,
    private val userRepository: UserRepository,
    private val accessTokenIssuer: AccessTokenIssuer,
) {

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

        val accessToken = accessTokenIssuer.issue(
            userId = requireNotNull(user.id) { "User id must be populated after save" },
        )

        return LoginResult(user = user, accessToken = accessToken)
    }

    data class LoginResult(val user: User, val accessToken: AccessToken)
}
