package com.linkcart.infrastructure.adapter.auth

import com.linkcart.application.auth.port.InvalidAccessTokenException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class JwtAccessTokenIssuerTest {

    private fun issuer(clock: Clock = Clock.systemUTC()): JwtAccessTokenIssuer = JwtAccessTokenIssuer(
        secret = "test-jwt-secret-at-least-32-bytes-long-for-hmac-sha256",
        ttlSeconds = 900,
        issuer = "linkcart-test",
        audience = "linkcart-api-test",
    ).also { it.clock = clock }

    @Test
    fun `issue then verify returns the same userId`() {
        val sut = issuer()

        val issued = sut.issue(userId = 42L)
        val userId = sut.verify(issued.token)

        assertEquals(42L, userId)
    }

    @Test
    fun `expired token throws InvalidAccessTokenException`() {
        val issuedAt = Instant.parse("2026-01-01T00:00:00Z")
        val issuingClock = Clock.fixed(issuedAt, ZoneOffset.UTC)
        val issued = issuer(issuingClock).issue(userId = 7L)

        val afterExpiryClock = Clock.fixed(issuedAt.plusSeconds(901), ZoneOffset.UTC)
        val verifyingIssuer = issuer(afterExpiryClock)

        assertThrows<InvalidAccessTokenException> { verifyingIssuer.verify(issued.token) }
    }

    @Test
    fun `tampered token throws InvalidAccessTokenException`() {
        val sut = issuer()
        val issued = sut.issue(userId = 1L)
        val tampered = issued.token.dropLast(4) + "AAAA"

        assertThrows<InvalidAccessTokenException> { sut.verify(tampered) }
    }
}
