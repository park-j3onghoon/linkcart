package com.linkcart.application.port

/** SSRF/내부망 접근 차단 정책. 정책은 application 포트, 구현은 infrastructure. */
interface SafeUrlChecker {
    fun isSafe(url: String): Boolean
}
