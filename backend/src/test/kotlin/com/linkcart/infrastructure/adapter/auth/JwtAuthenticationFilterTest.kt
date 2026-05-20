package com.linkcart.infrastructure.adapter.auth

import com.linkcart.application.auth.port.AccessTokenIssuer
import com.linkcart.application.auth.port.InvalidAccessTokenException
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JwtAuthenticationFilterTest {

    private val issuer: AccessTokenIssuer = mock(AccessTokenIssuer::class.java)
    private val sut = JwtAuthenticationFilter(issuer)

    @AfterEach
    fun clearContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `valid Bearer token sets authentication`() {
        given(issuer.verify("good-token")).willReturn(42L)
        val request = MockHttpServletRequest().apply { addHeader("Authorization", "Bearer good-token") }
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        sut.doFilter(request, response, chain)

        val auth = SecurityContextHolder.getContext().authentication
        assertEquals(42L, auth.principal)
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `invalid token leaves SecurityContext empty so entry point can reject`() {
        willThrow(InvalidAccessTokenException("bad")).given(issuer).verify("bad-token")
        val request = MockHttpServletRequest().apply { addHeader("Authorization", "Bearer bad-token") }
        val chain = mock(FilterChain::class.java)

        sut.doFilter(request, MockHttpServletResponse(), chain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(chain).doFilter(org.mockito.ArgumentMatchers.eq(request), org.mockito.ArgumentMatchers.any())
    }

    @Test
    fun `missing Authorization header is passed through untouched`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        sut.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(issuer, never()).verify(org.mockito.ArgumentMatchers.anyString())
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `non-Bearer Authorization header is ignored`() {
        val request = MockHttpServletRequest().apply { addHeader("Authorization", "Basic abc") }

        sut.doFilter(request, MockHttpServletResponse(), mock(FilterChain::class.java))

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(issuer, never()).verify(org.mockito.ArgumentMatchers.anyString())
    }

    @Test
    fun `blank Bearer token is treated as missing`() {
        val request = MockHttpServletRequest().apply { addHeader("Authorization", "Bearer    ") }

        sut.doFilter(request, MockHttpServletResponse(), mock(FilterChain::class.java))

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(issuer, never()).verify(org.mockito.ArgumentMatchers.anyString())
    }

    @Test
    fun `pre-existing authentication is not overwritten`() {
        val preAuth = UsernamePasswordAuthenticationToken("existing", null, emptyList())
        SecurityContextHolder.getContext().authentication = preAuth
        val request = MockHttpServletRequest().apply { addHeader("Authorization", "Bearer ignore-me") }

        sut.doFilter(request, MockHttpServletResponse(), mock(FilterChain::class.java))

        assertEquals(preAuth, SecurityContextHolder.getContext().authentication)
        verify(issuer, never()).verify(org.mockito.ArgumentMatchers.anyString())
    }
}
