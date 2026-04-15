package com.linkcart.infrastructure.adapter.parser

import com.fasterxml.jackson.databind.ObjectMapper
import com.linkcart.domain.entity.Product
import com.linkcart.domain.model.ParseResult
import com.linkcart.domain.port.ProductParser
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientException
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Duration

@Component
class CoupangApiClient(
    @Value("\${linkcart.coupang.api-key:}")
    private val apiKey: String,
    @Value("\${linkcart.coupang.base-url:https://api-gateway.coupang.com}")
    private val baseUrl: String,
    restTemplateBuilder: RestTemplateBuilder,
    private val objectMapper: ObjectMapper,
) : ProductParser {

    internal val restTemplate = restTemplateBuilder
        .connectTimeout(Duration.ofSeconds(3))
        .readTimeout(Duration.ofSeconds(3))
        .additionalMessageConverters(StringHttpMessageConverter(StandardCharsets.UTF_8))
        .build()

    override fun canParse(url: String): Boolean = hostMatches(url, "coupang.com")

    override fun parse(url: String): ParseResult {
        if (apiKey.isBlank()) {
            return failure("쿠팡 API 키가 설정되지 않았습니다")
        }

        val productId = extractProductId(url)
            ?: return failure("쿠팡 상품 ID를 추출할 수 없습니다")

        val requestUrl = UriComponentsBuilder.fromUriString(baseUrl)
            .path("/v1/affiliate/products/{productId}")
            .buildAndExpand(productId)
            .toUriString()

        val headers = HttpHeaders().apply {
            accept = listOf(MediaType.APPLICATION_JSON)
            setBearerAuth(apiKey)
        }

        return try {
            val response = restTemplate.exchange(
                requestUrl,
                HttpMethod.GET,
                HttpEntity<Void>(headers),
                String::class.java,
            )
            parseResponse(response.body.orEmpty(), url)
        } catch (_: ResourceAccessException) {
            failure("쿠팡 API 타임아웃")
        } catch (e: HttpStatusCodeException) {
            failure("쿠팡 API 호출 실패: ${e.statusCode.value()}")
        } catch (_: RestClientException) {
            failure("쿠팡 API 호출 실패")
        }
    }

    internal fun parseResponse(body: String, sourceUrl: String): ParseResult {
        if (body.isBlank()) {
            return failure("쿠팡 API 응답이 비어 있습니다")
        }

        val response = runCatching {
            objectMapper.readValue(body, CoupangApiResponse::class.java)
        }.getOrElse {
            return failure("쿠팡 API 응답 파싱 실패")
        }

        if (response.code != null && response.code != "SUCCESS" && response.data == null) {
            return failure(response.message ?: "쿠팡 API 호출 실패")
        }

        val data = response.data ?: return failure(response.message ?: "쿠팡 API 응답에 상품 정보가 없습니다")

        when {
            data.deleted == true -> return failure("쿠팡 상품이 삭제되었습니다")
            data.soldOut == true -> return failure("쿠팡 상품이 품절되었습니다")
        }

        val name = data.name?.takeIf { it.isNotBlank() }
            ?: return failure("쿠팡 상품명이 없습니다")
        val imageUrl = data.imageUrl?.takeIf { it.isNotBlank() }
            ?: return failure("쿠팡 상품 이미지가 없습니다")
        val price = data.price ?: return failure("쿠팡 상품 가격이 없습니다")

        return ParseResult.Success(
            product = Product(
                name = name,
                price = Money(amount = price, currency = data.currency ?: "KRW"),
                imageUrl = imageUrl,
                sourceUrl = sourceUrl,
                mall = Mall.COUPANG,
            ),
            parserUsed = PARSER_NAME,
        )
    }

    private fun extractProductId(url: String): String? {
        val path = parseUri(url)?.path ?: return null
        return Regex("/products/(\\d+)").find(path)?.groupValues?.get(1)
    }

    private fun hostMatches(url: String, domain: String): Boolean {
        val host = parseUri(url)?.host?.lowercase() ?: return false
        return host == domain || host.endsWith(".$domain")
    }

    private fun parseUri(url: String): URI? = runCatching { URI(url) }.getOrNull()

    private fun failure(reason: String): ParseResult.Failure =
        ParseResult.Failure(reason = reason, parserUsed = PARSER_NAME)

    private data class CoupangApiResponse(
        val code: String? = null,
        val message: String? = null,
        val data: CoupangProductData? = null,
    )

    private data class CoupangProductData(
        val name: String? = null,
        val price: Long? = null,
        val currency: String? = null,
        val imageUrl: String? = null,
        val soldOut: Boolean? = null,
        val deleted: Boolean? = null,
    )

    companion object {
        const val PARSER_NAME = "coupang-api"
    }
}
