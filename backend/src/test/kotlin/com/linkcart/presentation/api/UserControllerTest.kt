package com.linkcart.presentation.api

import com.linkcart.application.user.usecase.GetMeUsecase
import com.linkcart.application.user.usecase.UnauthenticatedException
import com.linkcart.domain.model.AuthProvider
import com.linkcart.domain.model.User
import com.linkcart.infrastructure.config.WebConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.ComponentScan.Filter
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * AIP-156 singleton `/users/me`의 컨트롤러 계약을 고정한다.
 */
@WebMvcTest(
    controllers = [UserController::class],
    excludeFilters = [Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [WebConfig::class])],
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
class UserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var getMeUsecase: GetMeUsecase

    @BeforeEach
    fun setUpAuthentication() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(USER_ID, null, emptyList())
    }

    @AfterEach
    fun clearAuthentication() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `me returns 200 with AIP-148 resource name and standard fields`() {
        given(getMeUsecase.execute(USER_ID)).willReturn(
            User(
                id = USER_ID,
                email = "teddy@example.com",
                displayName = "Teddy",
                avatarUrl = "https://example.com/avatar.png",
                provider = AuthProvider.GOOGLE,
                providerUserId = "google-sub-1",
            ),
        )

        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.name").value("users/$USER_ID"))
            .andExpect(jsonPath("$.user.email").value("teddy@example.com"))
            .andExpect(jsonPath("$.user.displayName").value("Teddy"))
            .andExpect(jsonPath("$.user.avatarUrl").value("https://example.com/avatar.png"))
            .andExpect(jsonPath("$.user.provider").value("google"))
    }

    @Test
    fun `me returns 401 when user is missing`() {
        willThrow(UnauthenticatedException("인증이 필요합니다"))
            .given(getMeUsecase).execute(USER_ID)

        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
    }

    companion object {
        private const val USER_ID = 7L
    }
}
