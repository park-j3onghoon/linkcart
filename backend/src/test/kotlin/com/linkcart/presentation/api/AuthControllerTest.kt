package com.linkcart.presentation.api

import com.linkcart.application.auth.port.GoogleOAuthException
import com.linkcart.application.auth.usecase.InvalidRefreshTokenException
import com.linkcart.application.auth.usecase.IssueTokensUsecase
import com.linkcart.application.auth.usecase.LoginWithGoogleUsecase
import com.linkcart.application.auth.usecase.LogoutUsecase
import com.linkcart.application.auth.usecase.RefreshTokensUsecase
import com.linkcart.domain.model.AuthProvider
import com.linkcart.domain.model.User
import com.linkcart.domain.vo.AccessToken
import com.linkcart.infrastructure.config.WebConfig
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willThrow
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.ComponentScan.Filter
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

@WebMvcTest(
    controllers = [AuthController::class],
    excludeFilters = [Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [WebConfig::class])],
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var loginWithGoogleUsecase: LoginWithGoogleUsecase

    @MockBean
    private lateinit var refreshTokensUsecase: RefreshTokensUsecase

    @MockBean
    private lateinit var logoutUsecase: LogoutUsecase

    @Test
    fun `loginWithGoogle returns 200 with tokens and AIP-148 user resource`() {
        val user = User(
            id = 42L,
            email = "teddy@example.com",
            displayName = "Teddy",
            avatarUrl = "https://example.com/p.png",
            provider = AuthProvider.GOOGLE,
            providerUserId = "google-sub",
        )
        val tokens = IssueTokensUsecase.IssueResult(
            accessToken = AccessToken(token = "AT-123", expiresAt = Instant.now().plusSeconds(900)),
            rawRefreshToken = "RT-RAW",
            refreshTokenId = UUID.randomUUID(),
        )
        given(loginWithGoogleUsecase.execute(code = "abc", redirectUri = "https://app/cb"))
            .willReturn(LoginWithGoogleUsecase.LoginResult(user = user, tokens = tokens))

        mockMvc.perform(
            post("/api/v1/auth/oauth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"code":"abc","redirectUri":"https://app/cb"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("AT-123"))
            .andExpect(jsonPath("$.refreshToken").value("RT-RAW"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").isNumber)
            .andExpect(jsonPath("$.user.name").value("users/42"))
            .andExpect(jsonPath("$.user.email").value("teddy@example.com"))
            .andExpect(jsonPath("$.user.provider").value("google"))
    }

    @Test
    fun `loginWithGoogle returns 401 when Google rejects code`() {
        willThrow(GoogleOAuthException("invalid grant"))
            .given(loginWithGoogleUsecase).execute(code = "bad", redirectUri = "https://app/cb")

        mockMvc.perform(
            post("/api/v1/auth/oauth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"code":"bad","redirectUri":"https://app/cb"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
            .andExpect(jsonPath("$.message").value("Google 인증에 실패했습니다"))
    }

    @Test
    fun `loginWithGoogle returns 400 when code is blank`() {
        mockMvc.perform(
            post("/api/v1/auth/oauth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"code":"","redirectUri":"https://app/cb"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
    }

    @Test
    fun `refresh returns 200 with new tokens`() {
        val tokens = IssueTokensUsecase.IssueResult(
            accessToken = AccessToken(token = "AT-new", expiresAt = Instant.now().plusSeconds(900)),
            rawRefreshToken = "RT-new",
            refreshTokenId = UUID.randomUUID(),
        )
        given(refreshTokensUsecase.execute("RT-old")).willReturn(tokens)

        mockMvc.perform(
            post("/api/v1/auth/tokens:refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"RT-old"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("AT-new"))
            .andExpect(jsonPath("$.refreshToken").value("RT-new"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
    }

    @Test
    fun `refresh returns 401 when token is invalid`() {
        willThrow(InvalidRefreshTokenException("expired"))
            .given(refreshTokensUsecase).execute("RT-stale")

        mockMvc.perform(
            post("/api/v1/auth/tokens:refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"RT-stale"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
    }

    @Test
    fun `logout returns 204 and delegates to usecase`() {
        mockMvc.perform(
            post("/api/v1/auth/tokens:revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"RT-bye"}"""),
        )
            .andExpect(status().isNoContent)

        verify(logoutUsecase).execute("RT-bye")
    }

    @Test
    fun `logout returns 400 when refreshToken is blank`() {
        mockMvc.perform(
            post("/api/v1/auth/tokens:revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":""}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
    }
}
