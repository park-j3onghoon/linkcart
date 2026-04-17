package com.linkcart.application.auth.port

import com.linkcart.domain.model.AccessToken

interface AccessTokenIssuer {
    fun issue(userId: Long): AccessToken
    fun verify(token: String): Long
}

class InvalidAccessTokenException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
