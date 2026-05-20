package com.linkcart.application.auth.port

interface RefreshTokenGenerator {
    fun generate(): String

    /** SHA-256 hex. DB에는 원본이 아니라 해시만 저장한다. */
    fun hash(rawToken: String): String
}
