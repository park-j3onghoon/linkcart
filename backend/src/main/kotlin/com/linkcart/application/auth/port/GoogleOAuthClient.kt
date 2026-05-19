package com.linkcart.application.auth.port

import com.linkcart.domain.vo.GoogleIdentity

interface GoogleOAuthClient {
    /**
     * Google Authorization Code를 교환하고 ID Token을 검증하여 사용자 정보를 반환한다.
     * 실패 시 [GoogleOAuthException]을 던진다.
     */
    fun exchangeCodeForIdentity(code: String, redirectUri: String): GoogleIdentity
}

class GoogleOAuthException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
