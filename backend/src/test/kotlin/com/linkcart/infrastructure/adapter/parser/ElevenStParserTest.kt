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
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ElevenStParserTest {

    @Test
    fun `normal API response returns Success`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo(PRODUCT_URL))
            .andRespond(withSuccess(successBody("갤럭시 버즈 프로"), MediaType.APPLICATION_XML))

        val result = parser.parse(REQUEST_URL)

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
        server.expect(requestTo(PRODUCT_URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        val result = parser.parse(REQUEST_URL)

        server.verify()
        assertIs<ParseResult.Failure>(result)
        assertContains(result.reason, "500")
    }

    @Test
    fun `timeout returns Failure`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo(PRODUCT_URL)).andRespond { _ -> throw ResourceAccessException("Read timed out") }

        val result = parser.parse(REQUEST_URL)

        server.verify()
        assertIs<ParseResult.Failure>(result)
        assertEquals("11번가 API 타임아웃", result.reason)
    }

    @Test
    fun `missing product returns Failure`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo(PRODUCT_URL))
            .andRespond(withSuccess("<Products></Products>", MediaType.APPLICATION_XML))

        val result = parser.parse(REQUEST_URL)

        server.verify()
        assertIs<ParseResult.Failure>(result)
        assertEquals("11번가 상품 정보가 없습니다", result.reason)
    }

    @Test
    fun `empty XML body returns Failure`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo(PRODUCT_URL))
            .andRespond(withSuccess("", MediaType.APPLICATION_XML))

        val result = parser.parse(REQUEST_URL)

        server.verify()
        assertIs<ParseResult.Failure>(result)
        assertEquals("11번가 API 응답이 비어 있습니다", result.reason)
    }

    @Test
    fun `deleted product returns Failure`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo(PRODUCT_URL))
            .andRespond(withSuccess(statusBody("삭제된 상품", "DELETED"), MediaType.APPLICATION_XML))

        val result = parser.parse(REQUEST_URL)

        server.verify()
        assertIs<ParseResult.Failure>(result)
        assertEquals("11번가 상품이 삭제되었습니다", result.reason)
    }

    @Test
    fun `sold out product returns Failure`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo(PRODUCT_URL))
            .andRespond(withSuccess(statusBody("품절 상품", "SOLDOUT"), MediaType.APPLICATION_XML))

        val result = parser.parse(REQUEST_URL)

        server.verify()
        assertIs<ParseResult.Failure>(result)
        assertEquals("11번가 상품이 품절되었습니다", result.reason)
    }

    @Test
    fun `non-200 resultCode returns Failure with result message`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo(PRODUCT_URL))
            .andRespond(
                withSuccess(
                    resultErrorBody(resultCode = "-1", resultMessage = "인증 키가 유효하지 않습니다"),
                    MediaType.APPLICATION_XML,
                ),
            )

        val result = parser.parse(REQUEST_URL)

        server.verify()
        assertIs<ParseResult.Failure>(result)
        assertContains(result.reason, "인증 키가 유효하지 않습니다")
    }

    private fun parser(): ElevenStParser =
        ElevenStParser(
            apiKey = "test-key",
            baseUrl = "https://11st.test",
            restTemplateBuilder = RestTemplateBuilder(),
        )

    private fun successBody(name: String): String = statusBody(name, "ON_SALE")

    private fun statusBody(name: String, status: String): String = """
        <ProductResponse>
          <resultCode>200</resultCode>
          <Product>
            <ProductName>$name</ProductName>
            <ProductPrice>129000</ProductPrice>
            <ProductImage>https://cdn.11st.test/product.jpg</ProductImage>
            <ProductStatus>$status</ProductStatus>
          </Product>
        </ProductResponse>
    """.trimIndent()

    private fun resultErrorBody(resultCode: String, resultMessage: String): String = """
        <ProductResponse>
          <resultCode>$resultCode</resultCode>
          <resultMessage>$resultMessage</resultMessage>
        </ProductResponse>
    """.trimIndent()

    companion object {
        private const val REQUEST_URL = "https://www.11st.co.kr/products/987654"
        private const val PRODUCT_URL =
            "https://11st.test/products?key=test-key&apiCode=ProductInfo&productNo=987654"
    }
}
