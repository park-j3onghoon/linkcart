package com.linkcart.infrastructure.adapter.auth

import com.linkcart.application.auth.port.RefreshTokenGenerator
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

@Component
class SecureRandomRefreshTokenGenerator : RefreshTokenGenerator {

    private val random = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    override fun generate(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        random.nextBytes(bytes)
        return encoder.encodeToString(bytes)
    }

    override fun hash(rawToken: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(rawToken.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString(separator = "") { "%02x".format(it) }
    }

    companion object {
        private const val TOKEN_BYTES = 32  // 256-bit
    }
}
