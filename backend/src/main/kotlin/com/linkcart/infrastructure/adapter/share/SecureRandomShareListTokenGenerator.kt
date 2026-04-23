package com.linkcart.infrastructure.adapter.share

import com.linkcart.application.share.port.ShareListTokenGenerator
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64

@Component
class SecureRandomShareListTokenGenerator : ShareListTokenGenerator {

    private val random = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    override fun generate(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        random.nextBytes(bytes)
        return encoder.encodeToString(bytes)
    }

    companion object {
        private const val TOKEN_BYTES = 16  // 128-bit → 22자 base64url
    }
}
