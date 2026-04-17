package com.linkcart.infrastructure.adapter.parser

import com.linkcart.application.parser.ParserNames
import com.linkcart.domain.entity.Product
import com.linkcart.domain.model.ParseResult
import com.linkcart.domain.port.DedicatedProductParser
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
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
class ElevenStParser(
    @Value("\${linkcart.elevenst.api-key:}")
    private val apiKey: String,
    @Value("\${linkcart.elevenst.base-url:https://openapi.11st.co.kr/openapi}")
    private val baseUrl: String,
    restTemplateBuilder: RestTemplateBuilder,
) : DedicatedProductParser {

    internal val restTemplate = restTemplateBuilder
        .connectTimeout(Duration.ofSeconds(3))
        .readTimeout(Duration.ofSeconds(3))
        .additionalMessageConverters(StringHttpMessageConverter(StandardCharsets.UTF_8))
        .build()

    override fun canParse(url: String): Boolean = hostMatches(url, "11st.co.kr")

    override fun parse(url: String): ParseResult {
        if (apiKey.isBlank()) {
            return failure("11번가 API 키가 설정되지 않았습니다")
        }

        val productId = extractProductId(url)
            ?: return failure("11번가 상품 ID를 추출할 수 없습니다")

        val requestUrl = UriComponentsBuilder.fromUriString(baseUrl)
            .path("/products")
            .queryParam("key", apiKey)
            .queryParam("apiCode", "ProductInfo")
            .queryParam("productNo", productId)
            .build(true)
            .toUriString()

        return try {
            val body = restTemplate.getForObject(requestUrl, String::class.java).orEmpty()
            parseResponse(body, url)
        } catch (_: ResourceAccessException) {
            failure("11번가 API 타임아웃")
        } catch (e: HttpStatusCodeException) {
            failure("11번가 API 호출 실패: ${e.statusCode.value()}")
        } catch (_: RestClientException) {
            failure("11번가 API 호출 실패")
        }
    }

    internal fun parseResponse(body: String, sourceUrl: String): ParseResult {
        if (body.isBlank()) {
            return failure("11번가 API 응답이 비어 있습니다")
        }

        val doc = Jsoup.parse(body, "", Parser.xmlParser())
        val resultCode = doc.selectFirst("resultCode")?.text()?.trim()
        if (resultCode != null && resultCode != "200") {
            val message = doc.selectFirst("resultMessage")?.text()?.trim()
            return failure(message ?: "11번가 API 호출 실패")
        }

        val productNode = doc.selectFirst("Product")
            ?: return failure("11번가 상품 정보가 없습니다")

        val status = textOf(productNode, "ProductStatus")?.uppercase()
        when (status) {
            "DELETE", "DELETED" -> return failure("11번가 상품이 삭제되었습니다")
            "SOLDOUT", "SOLD_OUT" -> return failure("11번가 상품이 품절되었습니다")
        }

        val name = textOf(productNode, "ProductName")
            ?: return failure("11번가 상품명이 없습니다")
        val imageUrl = textOf(productNode, "ProductImage")
            ?: textOf(productNode, "ProductImage100")
            ?: return failure("11번가 상품 이미지가 없습니다")
        val price = textOf(productNode, "ProductPrice")?.toLongOrNull()
            ?: return failure("11번가 상품 가격이 없습니다")

        return ParseResult.Success(
            product = Product(
                name = name,
                price = Money(amount = price),
                imageUrl = imageUrl,
                sourceUrl = sourceUrl,
                mall = Mall.ELEVENST,
            ),
            parserUsed = PARSER_NAME,
        )
    }

    private fun extractProductId(url: String): String? {
        val uri = parseUri(url) ?: return null
        val queryProductId = uri.query
            ?.split("&")
            ?.mapNotNull { part ->
                val pieces = part.split("=", limit = 2)
                if (pieces.size == 2 && pieces[0] == "prdNo") pieces[1] else null
            }
            ?.firstOrNull()
        if (queryProductId != null) {
            return queryProductId
        }

        return uri.path
            ?.split("/")
            ?.asReversed()
            ?.firstOrNull { it.all(Char::isDigit) }
    }

    private fun hostMatches(url: String, domain: String): Boolean {
        val host = parseUri(url)?.host?.lowercase() ?: return false
        return host == domain || host.endsWith(".$domain")
    }

    private fun parseUri(url: String): URI? = runCatching { URI(url) }.getOrNull()

    private fun textOf(root: Element, selector: String): String? =
        root.selectFirst(selector)?.text()?.trim()?.takeIf { it.isNotBlank() }

    private fun failure(reason: String): ParseResult.Failure =
        ParseResult.Failure(reason = reason, parserUsed = PARSER_NAME)

    companion object {
        const val PARSER_NAME = ParserNames.ELEVENST
    }
}
