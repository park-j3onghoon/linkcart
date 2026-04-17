package com.linkcart.application.auth.port

interface RefreshTokenGenerator {
    /** 클라이언트에 전달할 원본 토큰 문자열을 생성한다. */
    fun generate(): String

    /** 원본 토큰을 DB 저장용 해시로 변환한다 (SHA-256 hex). */
    fun hash(rawToken: String): String
}
