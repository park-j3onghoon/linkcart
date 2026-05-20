package com.linkcart.infrastructure.adapter.parser

import com.fasterxml.jackson.databind.ObjectMapper
import com.linkcart.domain.model.Product
import com.linkcart.domain.model.ParseResult
import com.linkcart.domain.vo.ParserName
import com.linkcart.domain.port.DedicatedProductParser
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
import java.time.Clock
import java.time.Duration
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class CoupangParser(
    @Value("\${linkcart.coupang.access-key:}")
    private val accessKey: String,
    @Value("\${linkcart.coupang.secret-key:}")
    private val secretKey: String,
    @Value("\${linkcart.coupang.base-url:https://api-gateway.coupang.com}")
    private val baseUrl: String,
    restTemplateBuilder: RestTemplateBuilder,
    private val objectMapper: ObjectMapper,
) : DedicatedProductParser {

    internal val restTemplate = restTemplateBuilder
        .connectTimeout(Duration.ofSeconds(3))
        .readTimeout(Duration.ofSeconds(3))
        .additionalMessageConverters(StringHttpMessageConverter(StandardCharsets.UTF_8))
        .build()

    internal var clock: Clock = Clock.systemUTC()

    override fun canParse(url: String): Boolean = hostMatches(url, "coupang.com")

    override fun parse(url: String): ParseResult {
        if (accessKey.isBlank() || secretKey.isBlank()) {
            return failure("쿠팡 access key/secret key가 설정되지 않았습니다")
        }

        val sellerProductId = extractProductId(url)
            ?: return failure("쿠팡 상품 ID를 추출할 수 없습니다")

        val requestPath = PRODUCT_QUERY_PATH_TEMPLATE.replace("{sellerProductId}", sellerProductId)
        val requestUrl = UriComponentsBuilder.fromUriString(baseUrl)
            .path(requestPath)
            .build(true)
            .toUriString()

        val headers = HttpHeaders().apply {
            accept = listOf(MediaType.APPLICATION_JSON)
            set(HttpHeaders.AUTHORIZATION, buildAuthorizationHeader(requestPath))
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

    internal fun buildAuthorizationHeader(path: String, query: String = ""): String {
        val signedDate = SIGNED_DATE_FORMATTER.format(clock.instant())
        val message = "$signedDate${HttpMethod.GET.name()}$path$query"
        val signature = hmacSha256(message)

        return "CEA algorithm=HmacSHA256, access-key=$accessKey, signed-date=$signedDate, signature=$signature"
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
        if (data.isDeleted()) {
            return failure("쿠팡 상품이 삭제되었습니다")
        }

        val primaryItem = data.selectPrimaryItem()
            ?: return failure("쿠팡 API 응답에 상품 정보가 없습니다")
        if (primaryItem.maximumBuyCount == 0L) {
            return failure("쿠팡 상품이 품절되었습니다")
        }

        val name = data.displayProductName?.takeIf { it.isNotBlank() }
            ?: data.generalProductName?.takeIf { it.isNotBlank() }
            ?: return failure("쿠팡 상품명이 없습니다")
        val imageUrl = primaryItem.imageUrl()
            ?: return failure("쿠팡 상품 이미지가 없습니다")
        val price = primaryItem.salePrice ?: return failure("쿠팡 상품 가격이 없습니다")

        return ParseResult.Success(
            product = Product(
                name = name,
                price = Money(amount = price, currency = data.currency ?: Money.DEFAULT_CURRENCY),
                imageUrl = imageUrl,
                sourceUrl = sourceUrl,
                mall = Mall.COUPANG,
            ),
            parserUsed = ParserName.COUPANG,
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
        ParseResult.Failure(reason = reason, parserUsed = ParserName.COUPANG)

    private fun hmacSha256(message: String): String {
        val keySpec = SecretKeySpec(secretKey.toByteArray(StandardCharsets.UTF_8), HMAC_SHA_256)
        val mac = Mac.getInstance(HMAC_SHA_256).apply { init(keySpec) }
        return mac.doFinal(message.toByteArray(StandardCharsets.UTF_8))
            .joinToString(separator = "") { "%02x".format(it) }
    }

    private data class CoupangApiResponse(
        val code: String? = null,
        val message: String? = null,
        val data: CoupangProductData? = null,
    )

    private data class CoupangProductData(
        val displayProductName: String? = null,
        val generalProductName: String? = null,
        val currency: String? = null,
        val status: String? = null,
        val statusName: String? = null,
        val items: List<CoupangItem> = emptyList(),
    ) {
        fun isDeleted(): Boolean {
            val normalizedStatus = status?.uppercase()
            val normalizedStatusName = statusName?.uppercase()
            return normalizedStatus == "DELETED" ||
                normalizedStatusName?.contains("DELETE") == true ||
                statusName?.contains("삭제") == true
        }

        fun selectPrimaryItem(): CoupangItem? =
            items.firstOrNull { it.salePrice != null && it.imageUrl() != null }
                ?: items.firstOrNull()
    }

    private data class CoupangItem(
        val salePrice: Long? = null,
        val maximumBuyCount: Long? = null,
        val images: List<CoupangImage> = emptyList(),
    ) {
        fun imageUrl(): String? {
            val representationImage = images.firstOrNull { it.imageType?.uppercase() == "REPRESENTATION" }
            return representationImage?.url() ?: images.firstOrNull()?.url()
        }
    }

    private data class CoupangImage(
        val cdnPath: String? = null,
        val vendorPath: String? = null,
        val imageType: String? = null,
    ) {
        fun url(): String? = cdnPath?.takeIf { it.isNotBlank() } ?: vendorPath?.takeIf { it.isNotBlank() }
    }

    companion object {
        private const val HMAC_SHA_256 = "HmacSHA256"
        private const val PRODUCT_QUERY_PATH_TEMPLATE =
            "/v2/providers/seller_api/apis/api/v1/marketplace/seller-products/{sellerProductId}"
        private val SIGNED_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
    }
}
