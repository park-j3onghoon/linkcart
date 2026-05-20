package com.linkcart.infrastructure.adapter.auth

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.linkcart.application.auth.port.GoogleOAuthException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willThrow
import org.mockito.Mockito.mock
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import kotlin.test.assertEquals

class GoogleOAuthAdapterTest {

    private val tokenUrl = "https://oauth2.googleapis.com/token"

    private fun newAdapter(): GoogleOAuthAdapter = GoogleOAuthAdapter(
        clientId = "client-id",
        clientSecret = "client-secret",
        tokenUrl = tokenUrl,
        restTemplateBuilder = RestTemplateBuilder(),
    )

    @Test
    fun `exchangeCodeForIdentity returns GoogleIdentity on happy path`() {
        val adapter = newAdapter()
        val server = MockRestServiceServer.createServer(adapter.restTemplate)
        server.expect(requestTo(tokenUrl))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withSuccess(
                    """{"access_token":"AT","id_token":"ID-TOKEN","token_type":"Bearer","expires_in":3600}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val mockIdToken = mock(GoogleIdToken::class.java)
        val payload = GoogleIdToken.Payload().apply {
            subject = "google-sub-123"
            email = "teddy@example.com"
            set("name", "Teddy")
            set("picture", "https://example.com/p.png")
        }
        given(mockIdToken.payload).willReturn(payload)
        val mockVerifier = mock(GoogleIdTokenVerifier::class.java)
        given(mockVerifier.verify("ID-TOKEN")).willReturn(mockIdToken)
        adapter.idTokenVerifier = mockVerifier

        val identity = adapter.exchangeCodeForIdentity(code = "code-abc", redirectUri = "https://app/cb")

        assertEquals("google-sub-123", identity.subject)
        assertEquals("teddy@example.com", identity.email)
        assertEquals("Teddy", identity.displayName)
        assertEquals("https://example.com/p.png", identity.avatarUrl)
        server.verify()
    }

    @Test
    fun `exchangeCodeForIdentity allows missing optional displayName and avatarUrl`() {
        val adapter = newAdapter()
        val server = MockRestServiceServer.createServer(adapter.restTemplate)
        server.expect(requestTo(tokenUrl)).andRespond(
            withSuccess("""{"id_token":"ID","token_type":"Bearer"}""", MediaType.APPLICATION_JSON),
        )
        val mockIdToken = mock(GoogleIdToken::class.java)
        given(mockIdToken.payload).willReturn(
            GoogleIdToken.Payload().apply {
                subject = "sub"
                email = "u@e.com"
            },
        )
        val mockVerifier = mock(GoogleIdTokenVerifier::class.java)
        given(mockVerifier.verify("ID")).willReturn(mockIdToken)
        adapter.idTokenVerifier = mockVerifier

        val identity = adapter.exchangeCodeForIdentity("c", "https://app/cb")

        assertEquals(null, identity.displayName)
        assertEquals(null, identity.avatarUrl)
    }

    @Test
    fun `throws GoogleOAuthException when token endpoint returns error`() {
        val adapter = newAdapter()
        val server = MockRestServiceServer.createServer(adapter.restTemplate)
        server.expect(requestTo(tokenUrl)).andRespond(withServerError())

        assertThrows<GoogleOAuthException> {
            adapter.exchangeCodeForIdentity("bad", "https://app/cb")
        }
    }

    @Test
    fun `throws GoogleOAuthException when response omits id_token`() {
        val adapter = newAdapter()
        val server = MockRestServiceServer.createServer(adapter.restTemplate)
        server.expect(requestTo(tokenUrl)).andRespond(
            withSuccess("""{"access_token":"AT"}""", MediaType.APPLICATION_JSON),
        )

        assertThrows<GoogleOAuthException> {
            adapter.exchangeCodeForIdentity("c", "https://app/cb")
        }
    }

    @Test
    fun `throws GoogleOAuthException when id_token verification returns null`() {
        val adapter = newAdapter()
        val server = MockRestServiceServer.createServer(adapter.restTemplate)
        server.expect(requestTo(tokenUrl)).andRespond(
            withSuccess("""{"id_token":"TAMPERED"}""", MediaType.APPLICATION_JSON),
        )
        val mockVerifier = mock(GoogleIdTokenVerifier::class.java)
        given(mockVerifier.verify("TAMPERED")).willReturn(null)
        adapter.idTokenVerifier = mockVerifier

        assertThrows<GoogleOAuthException> {
            adapter.exchangeCodeForIdentity("c", "https://app/cb")
        }
    }

    @Test
    fun `throws GoogleOAuthException when verifier throws`() {
        val adapter = newAdapter()
        val server = MockRestServiceServer.createServer(adapter.restTemplate)
        server.expect(requestTo(tokenUrl)).andRespond(
            withSuccess("""{"id_token":"BAD"}""", MediaType.APPLICATION_JSON),
        )
        val mockVerifier = mock(GoogleIdTokenVerifier::class.java)
        willThrow(RuntimeException("verifier blew up")).given(mockVerifier).verify("BAD")
        adapter.idTokenVerifier = mockVerifier

        assertThrows<GoogleOAuthException> {
            adapter.exchangeCodeForIdentity("c", "https://app/cb")
        }
    }

    @Test
    fun `throws GoogleOAuthException when id_token payload lacks subject`() {
        val adapter = newAdapter()
        val server = MockRestServiceServer.createServer(adapter.restTemplate)
        server.expect(requestTo(tokenUrl)).andRespond(
            withSuccess("""{"id_token":"ID"}""", MediaType.APPLICATION_JSON),
        )
        val mockIdToken = mock(GoogleIdToken::class.java)
        given(mockIdToken.payload).willReturn(GoogleIdToken.Payload().apply { email = "x@x.com" })
        val mockVerifier = mock(GoogleIdTokenVerifier::class.java)
        given(mockVerifier.verify("ID")).willReturn(mockIdToken)
        adapter.idTokenVerifier = mockVerifier

        assertThrows<GoogleOAuthException> {
            adapter.exchangeCodeForIdentity("c", "https://app/cb")
        }
    }

    @Test
    fun `throws GoogleOAuthException when id_token payload lacks email`() {
        val adapter = newAdapter()
        val server = MockRestServiceServer.createServer(adapter.restTemplate)
        server.expect(requestTo(tokenUrl)).andRespond(
            withSuccess("""{"id_token":"ID"}""", MediaType.APPLICATION_JSON),
        )
        val mockIdToken = mock(GoogleIdToken::class.java)
        given(mockIdToken.payload).willReturn(GoogleIdToken.Payload().apply { subject = "sub" })
        val mockVerifier = mock(GoogleIdTokenVerifier::class.java)
        given(mockVerifier.verify("ID")).willReturn(mockIdToken)
        adapter.idTokenVerifier = mockVerifier

        assertThrows<GoogleOAuthException> {
            adapter.exchangeCodeForIdentity("c", "https://app/cb")
        }
    }
}
