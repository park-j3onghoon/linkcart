package com.linkcart.infrastructure.adapter.share

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SecureRandomShareListTokenGeneratorTest {

    private val sut = SecureRandomShareListTokenGenerator()

    @Test
    fun `generates 22-char base64url token without padding`() {
        val token = sut.generate()

        // 128bit(16바이트) → base64url 22자, padding 없음
        assertEquals(22, token.length)
        assertTrue(token.all { it.isLetterOrDigit() || it == '-' || it == '_' })
        assertTrue('=' !in token)
    }

    @Test
    fun `generates unique tokens across invocations`() {
        val tokens = (1..200).map { sut.generate() }.toSet()

        // collision 없음 → SecureRandom이 실제로 동작하는지 검증
        assertEquals(200, tokens.size)
    }

    @Test
    fun `two generators produce independent token streams`() {
        val other = SecureRandomShareListTokenGenerator()

        assertNotEquals(sut.generate(), other.generate())
    }
}
