package com.linkcart.infrastructure.adapter.parser

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.linkcart.domain.model.ParseResult
import com.linkcart.domain.vo.Mall
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.ResourceAccessException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CoupangApiClientTest {

    @Test
    fun `normal API response returns Success`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo(PRODUCT_URL))
            .andExpect(method(HttpMethod.GET))
            .andExpect(
                header(
                    HttpHeaders.AUTHORIZATION,
                    "CEA algorithm=HmacSHA256, access-key=test-access, signed-date=240102T030405Z, signature=585cc27a40a213b6dbcc6786ddd544dd788ba14e3412f1166fcce3380d9043e9",
                ),
            )
            .andRespond(withSuccess(successBody(name = "아이패드 에어", price = 899000L), MediaType.APPLICATION_JSON))

        val result = parser.parse(REQUEST_URL)

        server.verify()
        assertIs<ParseResult.Success>(result)
        assertEquals("아이패드 에어", result.product.name)
        assertEquals(899000L, result.product.price.amount)
        assertEquals(Mall.COUPANG, result.product.mall)
        assertEquals("coupang-api", result.parserUsed)
    }

    @Test
    fun `HTTP error returns Failure`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo(PRODUCT_URL)).andRespond(withStatus(HttpStatus.NOT_FOUND))

        val result = parser.parse(REQUEST_URL)

        server.verify()
        assertIs<ParseResult.Failure>(result)
        assertContains(result.reason, "404")
    }

    @Test
    fun `timeout returns Failure`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo(PRODUCT_URL)).andRespond { _ -> throw ResourceAccessException("Read timed out") }

        val result = parser.parse(REQUEST_URL)

        server.verify()
        assertIs<ParseResult.Failure>(result)
        assertEquals("쿠팡 API 타임아웃", result.reason)
    }

    @Test
    fun `deleted product returns Failure`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo(PRODUCT_URL))
            .andRespond(withSuccess(deletedBody("삭제된 상품"), MediaType.APPLICATION_JSON))

        val result = parser.parse(REQUEST_URL)

        server.verify()
        assertIs<ParseResult.Failure>(result)
        assertEquals("쿠팡 상품이 삭제되었습니다", result.reason)
    }

    @Test
    fun `sold out product returns Failure`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo(PRODUCT_URL))
            .andRespond(withSuccess(soldOutBody("품절 상품"), MediaType.APPLICATION_JSON))

        val result = parser.parse(REQUEST_URL)

        server.verify()
        assertIs<ParseResult.Failure>(result)
        assertEquals("쿠팡 상품이 품절되었습니다", result.reason)
    }

    @Test
    fun `empty response body returns Failure`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo(PRODUCT_URL))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON))

        val result = parser.parse(REQUEST_URL)

        server.verify()
        assertIs<ParseResult.Failure>(result)
        assertEquals("쿠팡 API 응답이 비어 있습니다", result.reason)
    }

    @Test
    fun `invalid JSON returns Failure`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo(PRODUCT_URL))
            .andRespond(withSuccess("{not valid json", MediaType.APPLICATION_JSON))

        val result = parser.parse(REQUEST_URL)

        server.verify()
        assertIs<ParseResult.Failure>(result)
        assertEquals("쿠팡 API 응답 파싱 실패", result.reason)
    }

    @Test
    fun `non-SUCCESS code returns Failure with server message`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo(PRODUCT_URL))
            .andRespond(
                withSuccess(
                    failureCodeBody(code = "ERROR", message = "인증 실패"),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val result = parser.parse(REQUEST_URL)

        server.verify()
        assertIs<ParseResult.Failure>(result)
        assertContains(result.reason, "인증 실패")
    }

    @Test
    fun `missing price returns Failure`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo(PRODUCT_URL))
            .andRespond(withSuccess(missingPriceBody("가격 없는 상품"), MediaType.APPLICATION_JSON))

        val result = parser.parse(REQUEST_URL)

        server.verify()
        assertIs<ParseResult.Failure>(result)
        assertEquals("쿠팡 상품 가격이 없습니다", result.reason)
    }

    @Test
    fun `empty data returns Failure`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo(PRODUCT_URL))
            .andRespond(withSuccess(emptyDataBody(), MediaType.APPLICATION_JSON))

        val result = parser.parse(REQUEST_URL)

        server.verify()
        assertIs<ParseResult.Failure>(result)
        assertEquals("쿠팡 API 응답에 상품 정보가 없습니다", result.reason)
    }

    private fun parser(): CoupangApiClient =
        CoupangApiClient(
            accessKey = "test-access",
            secretKey = "test-secret",
            baseUrl = "https://coupang.test",
            restTemplateBuilder = RestTemplateBuilder(),
            objectMapper = jacksonObjectMapper(),
        ).also {
            it.clock = Clock.fixed(Instant.parse("2024-01-02T03:04:05Z"), ZoneOffset.UTC)
        }

    private fun successBody(name: String, price: Long = 123000L): String = """
        {
          "code": "SUCCESS",
          "data": {
            "displayProductName": "$name",
            "status": "APPROVED",
            "statusName": "승인완료",
            "items": [
              {
                "salePrice": $price,
                "maximumBuyCount": 10,
                "images": [
                  { "imageType": "REPRESENTATION", "cdnPath": "https://image.coupangcdn.com/test.jpg" }
                ]
              }
            ]
          }
        }
    """.trimIndent()

    private fun deletedBody(name: String): String = """
        {
          "code": "SUCCESS",
          "data": {
            "displayProductName": "$name",
            "status": "DELETED",
            "statusName": "상품삭제",
            "items": [
              {
                "salePrice": 10000,
                "maximumBuyCount": 10,
                "images": [
                  { "imageType": "REPRESENTATION", "cdnPath": "https://image.coupangcdn.com/test.jpg" }
                ]
              }
            ]
          }
        }
    """.trimIndent()

    private fun soldOutBody(name: String): String = """
        {
          "code": "SUCCESS",
          "data": {
            "displayProductName": "$name",
            "status": "APPROVED",
            "statusName": "승인완료",
            "items": [
              {
                "salePrice": 10000,
                "maximumBuyCount": 0,
                "images": [
                  { "imageType": "REPRESENTATION", "cdnPath": "https://image.coupangcdn.com/test.jpg" }
                ]
              }
            ]
          }
        }
    """.trimIndent()

    private fun missingPriceBody(name: String): String = """
        {
          "code": "SUCCESS",
          "data": {
            "displayProductName": "$name",
            "status": "APPROVED",
            "statusName": "승인완료",
            "items": [
              {
                "maximumBuyCount": 10,
                "images": [
                  { "imageType": "REPRESENTATION", "cdnPath": "https://image.coupangcdn.com/test.jpg" }
                ]
              }
            ]
          }
        }
    """.trimIndent()

    private fun failureCodeBody(code: String, message: String): String = """
        { "code": "$code", "message": "$message" }
    """.trimIndent()

    private fun emptyDataBody(): String = """
        { "code": "SUCCESS" }
    """.trimIndent()

    companion object {
        private const val REQUEST_URL = "https://www.coupang.com/vp/products/123456"
        private const val PRODUCT_URL =
            "https://coupang.test/v2/providers/seller_api/apis/api/v1/marketplace/seller-products/123456"
    }
}
