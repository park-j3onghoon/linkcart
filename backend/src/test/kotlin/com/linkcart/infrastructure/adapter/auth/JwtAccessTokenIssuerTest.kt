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

    @Test
    fun `rejects secret shorter than 32 bytes at construction`() {
        assertThrows<IllegalArgumentException> {
            JwtAccessTokenIssuer(
                secret = "too-short-secret",
                ttlSeconds = 900,
                issuer = "linkcart-test",
                audience = "linkcart-api-test",
            )
        }
    }

    @Test
    fun `verify throws on token signed with different secret`() {
        val sut = issuer()
        val foreignToken = JwtAccessTokenIssuer(
            secret = "different-jwt-secret-at-least-32-bytes-long!!!",
            ttlSeconds = 900,
            issuer = "linkcart-test",
            audience = "linkcart-api-test",
        ).issue(userId = 1L).token

        assertThrows<InvalidAccessTokenException> { sut.verify(foreignToken) }
    }

    @Test
    fun `verify throws when issuer claim does not match`() {
        val foreign = JwtAccessTokenIssuer(
            secret = "test-jwt-secret-at-least-32-bytes-long-for-hmac-sha256",
            ttlSeconds = 900,
            issuer = "other-issuer",
            audience = "linkcart-api-test",
        ).issue(userId = 1L).token
        val sut = issuer()

        assertThrows<InvalidAccessTokenException> { sut.verify(foreign) }
    }

    @Test
    fun `verify throws when audience claim does not match`() {
        val foreign = JwtAccessTokenIssuer(
            secret = "test-jwt-secret-at-least-32-bytes-long-for-hmac-sha256",
            ttlSeconds = 900,
            issuer = "linkcart-test",
            audience = "other-audience",
        ).issue(userId = 1L).token
        val sut = issuer()

        assertThrows<InvalidAccessTokenException> { sut.verify(foreign) }
    }
}
