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
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CoupangApiClientTest {

    @Test
    fun `normal API response returns Success`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo("https://coupang.test/v2/providers/seller_api/apis/api/v1/marketplace/seller-products/123456"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(
                header(
                    HttpHeaders.AUTHORIZATION,
                    "CEA algorithm=HmacSHA256, access-key=test-access, signed-date=240102T030405Z, signature=585cc27a40a213b6dbcc6786ddd544dd788ba14e3412f1166fcce3380d9043e9",
                ),
            )
            .andRespond(withSuccess(successBody(name = "아이패드 에어", price = 899000L), MediaType.APPLICATION_JSON))

        val result = parser.parse("https://www.coupang.com/vp/products/123456")

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
        server.expect(requestTo("https://coupang.test/v2/providers/seller_api/apis/api/v1/marketplace/seller-products/123456"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND))

        val result = parser.parse("https://www.coupang.com/vp/products/123456")

        server.verify()
        assertIs<ParseResult.Failure>(result)
        assertTrue(result.reason.contains("404"))
    }

    @Test
    fun `timeout returns Failure`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo("https://coupang.test/v2/providers/seller_api/apis/api/v1/marketplace/seller-products/123456"))
            .andRespond { _ -> throw ResourceAccessException("Read timed out") }

        val result = parser.parse("https://www.coupang.com/vp/products/123456")

        server.verify()
        assertIs<ParseResult.Failure>(result)
        assertEquals("쿠팡 API 타임아웃", result.reason)
    }

    @Test
    fun `deleted product returns Failure`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo("https://coupang.test/v2/providers/seller_api/apis/api/v1/marketplace/seller-products/123456"))
            .andRespond(withSuccess(successBody(name = "삭제된 상품", deleted = true), MediaType.APPLICATION_JSON))

        val result = parser.parse("https://www.coupang.com/vp/products/123456")

        server.verify()
        assertIs<ParseResult.Failure>(result)
        assertEquals("쿠팡 상품이 삭제되었습니다", result.reason)
    }

    @Test
    fun `sold out product returns Failure`() {
        val parser = parser()
        val server = MockRestServiceServer.createServer(parser.restTemplate)
        server.expect(requestTo("https://coupang.test/v2/providers/seller_api/apis/api/v1/marketplace/seller-products/123456"))
            .andRespond(withSuccess(successBody(name = "품절 상품", soldOut = true), MediaType.APPLICATION_JSON))

        val result = parser.parse("https://www.coupang.com/vp/products/123456")

        server.verify()
        assertIs<ParseResult.Failure>(result)
        assertEquals("쿠팡 상품이 품절되었습니다", result.reason)
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

    private fun successBody(
        name: String,
        price: Long = 123000L,
        soldOut: Boolean = false,
        deleted: Boolean = false,
    ): String = """
        {
          "code": "SUCCESS",
          "data": {
            "displayProductName": "$name",
            "status": "${if (deleted) "DELETED" else "APPROVED"}",
            "statusName": "${if (deleted) "상품삭제" else "승인완료"}",
            "items": [
              {
                "salePrice": $price,
                "maximumBuyCount": ${if (soldOut) 0 else 10},
                "images": [
                  {
                    "imageType": "REPRESENTATION",
                    "cdnPath": "https://image.coupangcdn.com/test.jpg"
                  }
                ]
              }
            ]
          }
        }
    """.trimIndent()
}
