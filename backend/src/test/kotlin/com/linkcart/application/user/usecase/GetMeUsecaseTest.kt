package com.linkcart.application.user.usecase

import com.linkcart.domain.model.AuthProvider
import com.linkcart.domain.model.User
import com.linkcart.domain.port.UserRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import kotlin.test.assertEquals

class GetMeUsecaseTest {

    private val userRepository: UserRepository = mock(UserRepository::class.java)
    private val sut = GetMeUsecase(userRepository)

    @Test
    fun `returns user when repository finds it`() {
        val user = User(
            id = 5L,
            provider = AuthProvider.GOOGLE,
            providerUserId = "g-5",
            email = "u@e.com",
        )
        given(userRepository.findById(5L)).willReturn(user)

        assertEquals(user, sut.execute(5L))
    }

    @Test
    fun `throws UnauthenticatedException when repository returns null`() {
        given(userRepository.findById(99L)).willReturn(null)

        assertThrows<UnauthenticatedException> { sut.execute(99L) }
    }
}
