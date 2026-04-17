package com.linkcart.application.usecase

import com.linkcart.application.parser.ParserResolver
import com.linkcart.application.parser.dedicatedStub
import com.linkcart.application.parser.fallbackStub
import com.linkcart.domain.entity.Product
import com.linkcart.domain.model.ParseResult
import com.linkcart.domain.model.ParserName
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ParseProductUseCaseTest {

    @Test
    fun `dedicated parser success returns primary result`() {
        val dedicated = dedicatedStub(
            canParse = { it.contains("coupang.com") },
            parse = { success(parserUsed = ParserName.COUPANG) },
        )
        val fallback = fallbackStub { success(parserUsed = ParserName.OG) }
        val useCase = ParseProductUseCase(ParserResolver(listOf(dedicated), fallback))

        val result = useCase.execute("https://www.coupang.com/vp/products/123")

        assertIs<ParseResult.Success>(result)
        assertEquals(ParserName.COUPANG, result.parserUsed)
        assertFalse(result.fallbackUsed)
    }

    @Test
    fun `dedicated failure falls back to OG success`() {
        val dedicated = dedicatedStub(
            canParse = { it.contains("coupang.com") },
            parse = { ParseResult.Failure("쿠팡 API 호출 실패: 404", ParserName.COUPANG) },
        )
        val fallback = fallbackStub { success(parserUsed = ParserName.OG) }
        val useCase = ParseProductUseCase(ParserResolver(listOf(dedicated), fallback))

        val result = useCase.execute("https://www.coupang.com/vp/products/123")

        assertIs<ParseResult.Success>(result)
        assertEquals(ParserName.OG, result.parserUsed)
        assertTrue(result.fallbackUsed)
    }

    @Test
    fun `dedicated failure falls back to OG partial`() {
        val dedicated = dedicatedStub(
            canParse = { it.contains("11st.co.kr") },
            parse = { ParseResult.Failure("11번가 API 호출 실패: 500", ParserName.ELEVENST) },
        )
        val fallback = fallbackStub {
            ParseResult.Partial(
                fields = mapOf("name" to "OG 상품명"),
                parserUsed = ParserName.OG,
            )
        }
        val useCase = ParseProductUseCase(ParserResolver(listOf(dedicated), fallback))

        val result = useCase.execute("https://www.11st.co.kr/products/123")

        assertIs<ParseResult.Partial>(result)
        assertEquals("OG 상품명", result.fields["name"])
        assertTrue(result.fallbackUsed)
    }

    @Test
    fun `dedicated and fallback failures are combined`() {
        val dedicated = dedicatedStub(
            canParse = { it.contains("coupang.com") },
            parse = { ParseResult.Failure("쿠팡 API 호출 실패: 404", ParserName.COUPANG) },
        )
        val fallback = fallbackStub { ParseResult.Failure("파싱 가능한 정보가 없습니다", ParserName.OG) }
        val useCase = ParseProductUseCase(ParserResolver(listOf(dedicated), fallback))

        val result = useCase.execute("https://www.coupang.com/vp/products/123")

        assertIs<ParseResult.Failure>(result)
        assertTrue(result.reason.contains("coupang-api"))
        assertTrue(result.reason.contains("og"))
        assertEquals(ParserName.OG, result.parserUsed)
    }

    @Test
    fun `timeout from dedicated parser still uses fallback`() {
        val dedicated = dedicatedStub(
            canParse = { it.contains("coupang.com") },
            parse = { ParseResult.Failure("쿠팡 API 타임아웃", ParserName.COUPANG) },
        )
        val fallback = fallbackStub { success(parserUsed = ParserName.OG) }
        val useCase = ParseProductUseCase(ParserResolver(listOf(dedicated), fallback))

        val result = useCase.execute("https://www.coupang.com/vp/products/123")

        assertIs<ParseResult.Success>(result)
        assertTrue(result.fallbackUsed)
        assertEquals(ParserName.OG, result.parserUsed)
    }

    @Test
    fun `when primary is fallback, fallback parse is called only once`() {
        val callCount = AtomicInteger()
        val fallback = fallbackStub {
            callCount.incrementAndGet()
            ParseResult.Failure("파싱 가능한 정보가 없습니다", ParserName.OG)
        }
        val useCase = ParseProductUseCase(ParserResolver(emptyList(), fallback))

        useCase.execute("https://example.com/product/no-dedicated")

        assertEquals(1, callCount.get())
    }

    private fun success(parserUsed: ParserName): ParseResult.Success =
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
