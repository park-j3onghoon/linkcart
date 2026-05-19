package com.linkcart.infrastructure.adapter.parser

import com.linkcart.application.parser.port.SafeUrlChecker
import org.springframework.stereotype.Component
import java.net.InetAddress
import java.net.URI

@Component
class DefaultSafeUrlChecker : SafeUrlChecker {

    override fun isSafe(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (uri.scheme !in ALLOWED_SCHEMES) {
            return false
        }

        val host = uri.host ?: return false

        // URL-encoded bypass 방어 (defense-in-depth): Java URI가 대부분의 encoded host를
        // null 처리하지만, 일부 JDK/조합에서 raw encoded host가 통과할 가능성에 대비.
        if (host.contains('%')) {
            return false
        }

        // 짧은 IP 표기 (decimal/hex/octal) 거부: 127.0.0.1 우회 벡터
        if (isSuspiciousNumericHost(host)) {
            return false
        }

        val address = runCatching { InetAddress.getByName(host) }.getOrNull() ?: return false

        return !address.isAnyLocalAddress &&
            !address.isLoopbackAddress &&
            !address.isLinkLocalAddress &&
            !address.isSiteLocalAddress
    }

    private fun isSuspiciousNumericHost(host: String): Boolean {
        // 32-bit decimal 표기 (예: "2130706433" = 127.0.0.1)
        if (host.all { it.isDigit() }) return true
        // hex 표기 (예: "0x7f000001")
        if (host.startsWith("0x", ignoreCase = true)) return true
        // octal dotted 표기 (예: "0177.0.0.1")
        if (host.matches(OCTAL_DOTTED_PATTERN)) return true
        return false
    }

    companion object {
        private val ALLOWED_SCHEMES = setOf("http", "https")
        private val OCTAL_DOTTED_PATTERN = Regex("^0\\d+(?:\\.\\d+){3}$")

        // NOT defended: DNS rebinding (TOCTOU).
        // 호스트 해석 시점과 실제 요청 시점 간 DNS 응답 변조 공격은 별도 대응 필요.
    }
}
