package com.linkcart.application.auth.port

import com.linkcart.domain.vo.GoogleIdentity

interface GoogleOAuthClient {
    /** 실패 시 [GoogleOAuthException]을 던진다. */
    fun exchangeCodeForIdentity(code: String, redirectUri: String): GoogleIdentity
}

class GoogleOAuthException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
