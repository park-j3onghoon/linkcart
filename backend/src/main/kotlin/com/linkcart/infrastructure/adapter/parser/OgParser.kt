package com.linkcart.infrastructure.adapter.parser

import com.linkcart.domain.model.ParseResult
import com.linkcart.domain.entity.Product
import com.linkcart.domain.port.ProductParser
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money
import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import java.io.IOException

@Component("ogParser")
class OgParser(
    private val safeUrlChecker: SafeUrlChecker,
) : ProductParser {

    override fun canParse(url: String): Boolean = true

    override fun parse(url: String): ParseResult {
        if (!safeUrlChecker.isSafe(url)) {
            return ParseResult.Failure(reason = "허용되지 않는 URL입니다", parserUsed = PARSER_NAME)
        }

        return try {
            val doc = Jsoup.connect(url)
                .timeout(CONNECT_TIMEOUT_MS)
                .userAgent("LinkCartBot/1.0")
                .get()
            parseHtml(doc.html(), url)
        } catch (e: IOException) {
            ParseResult.Failure(reason = "페이지를 가져올 수 없습니다: ${e.message}", parserUsed = PARSER_NAME)
        }
    }

    fun parseHtml(html: String, sourceUrl: String): ParseResult {
        if (html.isBlank()) {
            return ParseResult.Failure(reason = "빈 HTML", parserUsed = PARSER_NAME)
        }

        val doc = Jsoup.parse(html)
        val ogTitle = doc.select("meta[property=og:title]").attr("content").ifBlank { null }
        val ogImage = doc.select("meta[property=og:image]").attr("content").ifBlank { null }
        val ogPriceAmount = doc.select("meta[property=product:price:amount]").attr("content").ifBlank { null }
        val ogPriceCurrency = doc.select("meta[property=product:price:currency]").attr("content").ifBlank { "KRW" }

        val name = ogTitle ?: doc.title().ifBlank { null }
        val imageUrl = ogImage
        val priceAmount = ogPriceAmount?.toLongOrNull()

        if (name != null && imageUrl != null && priceAmount != null) {
            return ParseResult.Success(
                product = Product(
                    name = name,
                    price = Money(amount = priceAmount, currency = ogPriceCurrency),
                    imageUrl = imageUrl,
                    sourceUrl = sourceUrl,
                    mall = Mall.GENERIC,
                ),
                parserUsed = PARSER_NAME,
            )
        }

        val partialFields = mutableMapOf<String, Any>()
        name?.let { partialFields["name"] = it }
        imageUrl?.let { partialFields["imageUrl"] = it }
        priceAmount?.let { partialFields["price"] = Money(amount = it, currency = ogPriceCurrency) }

        if (partialFields.isEmpty()) {
            return ParseResult.Failure(reason = "파싱 가능한 정보가 없습니다", parserUsed = PARSER_NAME)
        }

        return ParseResult.Partial(
            fields = partialFields,
            parserUsed = PARSER_NAME,
        )
    }

    companion object {
        const val PARSER_NAME = "og"
        private const val CONNECT_TIMEOUT_MS = 10_000
    }
}
