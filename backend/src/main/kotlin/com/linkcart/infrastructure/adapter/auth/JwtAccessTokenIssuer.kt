package com.linkcart.infrastructure.adapter.auth

import com.linkcart.application.auth.port.AccessTokenIssuer
import com.linkcart.application.auth.port.InvalidAccessTokenException
import com.linkcart.domain.model.AccessToken
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtAccessTokenIssuer(
    @Value("\${linkcart.jwt.secret}")
    private val secret: String,
    @Value("\${linkcart.jwt.access-token-ttl-seconds}")
    private val ttlSeconds: Long,
    @Value("\${linkcart.jwt.issuer}")
    private val issuer: String,
    @Value("\${linkcart.jwt.audience}")
    private val audience: String,
) : AccessTokenIssuer {

    internal var clock: Clock = Clock.systemUTC()

    private val signingKey: SecretKey = run {
        val bytes = secret.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size >= 32) { "JWT secret must be at least 32 bytes for HMAC-SHA256" }
        Keys.hmacShaKeyFor(bytes)
    }

    override fun issue(userId: Long): AccessToken {
        val issuedAt = clock.instant()
        val expiresAt = issuedAt.plus(Duration.ofSeconds(ttlSeconds))

        val token = Jwts.builder()
            .subject(userId.toString())
            .issuer(issuer)
            .audience().add(audience).and()
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact()

        return AccessToken(token = token, expiresAt = expiresAt)
    }

    override fun verify(token: String): Long {
        val claims = try {
            Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .clock { Date.from(clock.instant()) }
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: JwtException) {
            throw InvalidAccessTokenException("Access token 검증 실패: ${e.message}", e)
        }

        val subject = claims.subject
            ?: throw InvalidAccessTokenException("Access token에 sub 주장이 없습니다")
        return subject.toLongOrNull()
            ?: throw InvalidAccessTokenException("Access token sub가 숫자가 아닙니다: $subject")
    }
}
