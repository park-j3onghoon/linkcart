package com.linkcart.infrastructure.adapter.parser

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SafeUrlCheckerTest {

    private val checker = SafeUrlChecker()

    @Test
    fun `valid external url returns true`() {
        assertTrue(checker.isSafe(VALID_EXTERNAL_URL))
    }

    @Test
    fun `javascript scheme returns false`() {
        assertFalse(checker.isSafe(JAVASCRIPT_URL))
    }

    @Test
    fun `uppercase scheme returns false`() {
        assertFalse(checker.isSafe(UPPERCASE_URL))
    }

    @Test
    fun `IPv4 loopback returns false`() {
        assertFalse(checker.isSafe(LOOPBACK_V4))
    }

    @Test
    fun `IPv6 loopback returns false`() {
        assertFalse(checker.isSafe(LOOPBACK_V6))
    }

    @Test
    fun `any local address returns false`() {
        assertFalse(checker.isSafe(ANY_LOCAL))
    }

    @Test
    fun `link local address returns false`() {
        assertFalse(checker.isSafe(LINK_LOCAL))
    }

    @Test
    fun `private IP returns false`() {
        assertFalse(checker.isSafe(PRIVATE_IP))
    }

    @Test
    fun `malformed URL returns false`() {
        assertFalse(checker.isSafe(MALFORMED))
    }

    companion object {
        private const val VALID_EXTERNAL_URL = "https://8.8.8.8"
        private const val JAVASCRIPT_URL = "javascript:alert(1)"
        private const val UPPERCASE_URL = "HTTP://example.com"
        private const val LOOPBACK_V4 = "http://127.0.0.1"
        private const val LOOPBACK_V6 = "http://[::1]"
        private const val ANY_LOCAL = "http://0.0.0.0"
        private const val LINK_LOCAL = "http://169.254.1.1"
        private const val PRIVATE_IP = "http://10.0.0.1"
        private const val MALFORMED = "not a url"
    }
}
