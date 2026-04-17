package com.linkcart.infrastructure.adapter.parser

import org.springframework.stereotype.Component
import java.net.InetAddress
import java.net.URI

@Component
class SafeUrlChecker {

    fun isSafe(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (uri.scheme !in ALLOWED_SCHEMES) {
            return false
        }

        val host = uri.host ?: return false
        val address = runCatching { InetAddress.getByName(host) }.getOrNull() ?: return false

        return !address.isAnyLocalAddress &&
            !address.isLoopbackAddress &&
            !address.isLinkLocalAddress &&
            !address.isSiteLocalAddress
    }

    companion object {
        private val ALLOWED_SCHEMES = setOf("http", "https")
    }
}
