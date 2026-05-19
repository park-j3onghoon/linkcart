package com.linkcart.infrastructure.adapter.auth

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.linkcart.application.auth.port.GoogleOAuthClient
import com.linkcart.application.auth.port.GoogleOAuthException
import com.linkcart.domain.vo.GoogleIdentity
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClientException
import java.time.Duration
import java.util.Collections

@Component
class GoogleOAuthAdapter(
    @Value("\${linkcart.google.client-id}")
    private val clientId: String,
    @Value("\${linkcart.google.client-secret}")
    private val clientSecret: String,
    @Value("\${linkcart.google.token-url}")
    private val tokenUrl: String,
    restTemplateBuilder: RestTemplateBuilder,
) : GoogleOAuthClient {

    private val log = LoggerFactory.getLogger(javaClass)

    internal val restTemplate = restTemplateBuilder
        .connectTimeout(Duration.ofSeconds(3))
        .readTimeout(Duration.ofSeconds(5))
        .build()

    internal val idTokenVerifier: GoogleIdTokenVerifier = GoogleIdTokenVerifier.Builder(
        NetHttpTransport(),
        GsonFactory.getDefaultInstance(),
    ).setAudience(Collections.singletonList(clientId)).build()

    override fun exchangeCodeForIdentity(code: String, redirectUri: String): GoogleIdentity {
        val idTokenString = exchangeCode(code, redirectUri)
        return verifyAndExtract(idTokenString)
    }

    private fun exchangeCode(code: String, redirectUri: String): String {
        val form = LinkedMultiValueMap<String, String>().apply {
            add("code", code)
            add("client_id", clientId)
            add("client_secret", clientSecret)
            add("redirect_uri", redirectUri)
            add("grant_type", "authorization_code")
        }
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_FORM_URLENCODED }

        return try {
            val response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                HttpEntity(form, headers),
                GoogleTokenResponse::class.java,
            )
            response.body?.idToken ?: run {
                log.warn("Google 토큰 응답에 id_token 누락")
                throw GoogleOAuthException("Google 인증에 실패했습니다")
            }
        } catch (e: RestClientException) {
            log.warn("Google 토큰 교환 실패", e)
            throw GoogleOAuthException("Google 인증에 실패했습니다", e)
        }
    }

    private fun verifyAndExtract(idTokenString: String): GoogleIdentity {
        val idToken = try {
            idTokenVerifier.verify(idTokenString)
        } catch (e: Exception) {
            log.warn("Google ID 토큰 검증 실패", e)
            throw GoogleOAuthException("Google 인증에 실패했습니다", e)
        } ?: throw GoogleOAuthException("Google 인증에 실패했습니다")

        val payload = idToken.payload
        val subject = payload.subject
            ?: throw GoogleOAuthException("Google 인증에 실패했습니다")
        val email = payload.email
            ?: throw GoogleOAuthException("Google 인증에 실패했습니다")

        return GoogleIdentity(
            subject = subject,
            email = email,
            displayName = payload["name"] as? String,
            avatarUrl = payload["picture"] as? String,
        )
    }

    internal data class GoogleTokenResponse(
        val access_token: String? = null,
        val expires_in: Long? = null,
        val id_token: String? = null,
        val scope: String? = null,
        val token_type: String? = null,
        val refresh_token: String? = null,
    ) {
        val idToken: String? get() = id_token
    }
}
