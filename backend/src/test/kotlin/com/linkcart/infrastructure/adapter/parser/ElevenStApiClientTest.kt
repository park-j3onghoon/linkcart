package com.linkcart.infrastructure.adapter.parser

import com.linkcart.domain.model.ParseResult
import com.linkcart.domain.vo.Mall
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.ResourceAccessException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ElevenStApiClientTest {

    @Test
    fun `normal API response returns Success`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo("https://11st.test/products?key=test-key&apiCode=ProductInfo&productNo=987654"))
            .andRespond(withSuccess(successBody("갤럭시 버즈 프로"), MediaType.APPLICATION_XML))

        val result = parser.parse("https://www.11st.co.kr/products/987654")

        server.verify()
        assertIs<ParseResult.Success>(result)
        assertEquals("갤럭시 버즈 프로", result.product.name)
        assertEquals(129000L, result.product.price.amount)
        assertEquals(Mall.ELEVENST, result.product.mall)
        assertEquals("11st-api", result.parserUsed)
    }

    @Test
    fun `HTTP error returns Failure`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo("https://11st.test/products?key=test-key&apiCode=ProductInfo&productNo=987654"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        val result = parser.parse("https://www.11st.co.kr/products/987654")

        server.verify()
        assertIs<ParseResult.Failure>(result)
        assertTrue(result.reason.contains("500"))
    }

    @Test
    fun `timeout returns Failure`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo("https://11st.test/products?key=test-key&apiCode=ProductInfo&productNo=987654"))
            .andRespond { _ -> throw ResourceAccessException("Read timed out") }

        val result = parser.parse("https://www.11st.co.kr/products/987654")

        server.verify()
        assertIs<ParseResult.Failure>(result)
        assertEquals("11번가 API 타임아웃", result.reason)
    }

    @Test
    fun `missing product returns Failure`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo("https://11st.test/products?key=test-key&apiCode=ProductInfo&productNo=987654"))
            .andRespond(withSuccess("<Products></Products>", MediaType.APPLICATION_XML))

        val result = parser.parse("https://www.11st.co.kr/products/987654")

        server.verify()
        assertIs<ParseResult.Failure>(result)
        assertEquals("11번가 상품 정보가 없습니다", result.reason)
    }

    private fun parser(): ElevenStApiClient =
        ElevenStApiClient(
            apiKey = "test-key",
            baseUrl = "https://11st.test",
            restTemplateBuilder = RestTemplateBuilder(),
        )

    private fun successBody(name: String): String = """
        <ProductResponse>
          <resultCode>200</resultCode>
          <Product>
            <ProductName>$name</ProductName>
            <ProductPrice>129000</ProductPrice>
            <ProductImage>https://cdn.11st.test/product.jpg</ProductImage>
            <ProductStatus>ON_SALE</ProductStatus>
          </Product>
        </ProductResponse>
    """.trimIndent()
}
