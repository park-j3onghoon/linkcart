package com.linkcart.infrastructure.adapter.auth

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SecureRandomRefreshTokenGeneratorTest {

    private val sut = SecureRandomRefreshTokenGenerator()

    @Test
    fun `generated tokens are unique across calls`() {
        val tokens = (1..100).map { sut.generate() }.toSet()
        assertEquals(100, tokens.size)
    }

    @Test
    fun `generated token has adequate length`() {
        val token = sut.generate()
        assertTrue(token.length >= 40, "expected >=40 chars, got ${token.length}")
    }

    @Test
    fun `hash is deterministic and produces 64-char hex`() {
        val h1 = sut.hash("some-raw-token")
        val h2 = sut.hash("some-raw-token")
        assertEquals(h1, h2)
        assertEquals(64, h1.length)
        assertTrue(h1.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `hash differs for different inputs`() {
        assertNotEquals(sut.hash("a"), sut.hash("b"))
    }
}
