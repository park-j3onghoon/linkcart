package com.linkcart.application.usecase

import com.linkcart.application.parser.ParserFactory
import com.linkcart.domain.entity.Product
import com.linkcart.domain.model.ParseResult
import com.linkcart.domain.port.ProductParser
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ParseProductUseCaseTest {

    @Test
    fun `dedicated parser success returns primary result`() {
        val dedicated = stubParser(
            canParse = { it.contains("coupang.com") },
            result = success(parserUsed = "coupang-api"),
        )
        val fallback = stubParser(canParse = { true }, result = success(parserUsed = "og"))
        val useCase = ParseProductUseCase(ParserFactory(listOf(dedicated, fallback), fallback))

        val result = useCase.execute("https://www.coupang.com/vp/products/123")

        assertIs<ParseResult.Success>(result)
        assertEquals("coupang-api", result.parserUsed)
        assertFalse(result.fallbackUsed)
    }

    @Test
    fun `dedicated failure falls back to OG success`() {
        val dedicated = stubParser(
            canParse = { it.contains("coupang.com") },
            result = ParseResult.Failure("쿠팡 API 호출 실패: 404", "coupang-api"),
        )
        val fallback = stubParser(canParse = { true }, result = success(parserUsed = "og"))
        val useCase = ParseProductUseCase(ParserFactory(listOf(dedicated, fallback), fallback))

        val result = useCase.execute("https://www.coupang.com/vp/products/123")

        assertIs<ParseResult.Success>(result)
        assertEquals("og", result.parserUsed)
        assertTrue(result.fallbackUsed)
    }

    @Test
    fun `dedicated failure falls back to OG partial`() {
        val dedicated = stubParser(
            canParse = { it.contains("11st.co.kr") },
            result = ParseResult.Failure("11번가 API 호출 실패: 500", "11st-api"),
        )
        val fallback = stubParser(
            canParse = { true },
            result = ParseResult.Partial(
                fields = mapOf("name" to "OG 상품명"),
                parserUsed = "og",
            ),
        )
        val useCase = ParseProductUseCase(ParserFactory(listOf(dedicated, fallback), fallback))

        val result = useCase.execute("https://www.11st.co.kr/products/123")

        assertIs<ParseResult.Partial>(result)
        assertEquals("OG 상품명", result.fields["name"])
        assertTrue(result.fallbackUsed)
    }

    @Test
    fun `dedicated and fallback failures are combined`() {
        val dedicated = stubParser(
            canParse = { it.contains("coupang.com") },
            result = ParseResult.Failure("쿠팡 API 호출 실패: 404", "coupang-api"),
        )
        val fallback = stubParser(
            canParse = { true },
            result = ParseResult.Failure("파싱 가능한 정보가 없습니다", "og"),
        )
        val useCase = ParseProductUseCase(ParserFactory(listOf(dedicated, fallback), fallback))

        val result = useCase.execute("https://www.coupang.com/vp/products/123")

        assertIs<ParseResult.Failure>(result)
        assertTrue(result.reason.contains("coupang-api"))
        assertTrue(result.reason.contains("og"))
        assertEquals("og", result.parserUsed)
    }

    @Test
    fun `timeout from dedicated parser still uses fallback`() {
        val dedicated = stubParser(
            canParse = { it.contains("coupang.com") },
            result = ParseResult.Failure("쿠팡 API 타임아웃", "coupang-api"),
        )
        val fallback = stubParser(canParse = { true }, result = success(parserUsed = "og"))
        val useCase = ParseProductUseCase(ParserFactory(listOf(dedicated, fallback), fallback))

        val result = useCase.execute("https://www.coupang.com/vp/products/123")

        assertIs<ParseResult.Success>(result)
        assertTrue(result.fallbackUsed)
        assertEquals("og", result.parserUsed)
    }

    private fun stubParser(
        canParse: (String) -> Boolean,
        result: ParseResult,
    ): ProductParser = object : ProductParser {
        override fun canParse(url: String): Boolean = canParse(url)
        override fun parse(url: String): ParseResult = result
    }

    private fun success(parserUsed: String): ParseResult.Success =
        ParseResult.Success(
            product = Product(
                name = "테스트 상품",
                price = Money(amount = 1000L),
                imageUrl = "https://example.com/image.jpg",
                sourceUrl = "https://example.com/product/1",
                mall = Mall.GENERIC,
            ),
            parserUsed = parserUsed,
        )
}
