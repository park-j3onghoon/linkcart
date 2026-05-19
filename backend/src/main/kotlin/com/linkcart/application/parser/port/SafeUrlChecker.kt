package com.linkcart.application.parser.port

/**
 * SSRF/내부망 접근 차단을 위한 URL 검증 정책.
 * 도메인 정책이므로 application 포트로 정의하고 infrastructure에서 구현한다.
 */
interface SafeUrlChecker {
    fun isSafe(url: String): Boolean
}
