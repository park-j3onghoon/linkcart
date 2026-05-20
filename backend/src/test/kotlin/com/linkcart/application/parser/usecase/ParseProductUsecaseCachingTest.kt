package com.linkcart.application.parser.usecase

import com.linkcart.application.parser.ParserPipeline
import com.linkcart.application.parser.ParserResolver
import com.linkcart.domain.model.Product
import com.linkcart.domain.model.ParseResult
import com.linkcart.domain.vo.ParserName
import com.linkcart.domain.port.DedicatedProductParser
import com.linkcart.domain.port.FallbackProductParser
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(classes = [ParseProductUsecaseCachingTest.TestApp::class])
class ParseProductUsecaseCachingTest {

    @Autowired
    private lateinit var parseProductUseCase: ParseProductUsecase

    @Autowired
    private lateinit var dedicatedParser: DedicatedCountingParser

    @Autowired
    private lateinit var fallbackParser: FallbackCountingParser

    @BeforeEach
    fun setUp() {
        dedicatedParser.reset()
        fallbackParser.reset()
    }

    @Test
    fun `cache proxy is enabled and caches success results`() {
        dedicatedParser.canParse = { it.contains("coupang.com") }
        dedicatedParser.result = success(parserUsed = ParserName.COUPANG)
        fallbackParser.result = success(parserUsed = ParserName.OG)

        val first = parseProductUseCase.execute("https://www.coupang.com/vp/products/123")
        val second = parseProductUseCase.execute("https://www.coupang.com/vp/products/123")

        assertTrue(AopUtils.isAopProxy(parseProductUseCase))
        assertEquals(first, second)
        assertEquals(1, dedicatedParser.parseCallCount.get())
        assertEquals(0, fallbackParser.parseCallCount.get())
    }

    @Test
    fun `failure results are not cached`() {
        dedicatedParser.canParse = { it.contains("coupang.com") }
        dedicatedParser.result = ParseResult.Failure("쿠팡 API 호출 실패: 404", ParserName.COUPANG)
        fallbackParser.result = ParseResult.Failure("파싱 가능한 정보가 없습니다", ParserName.OG)

        parseProductUseCase.execute("https://www.coupang.com/vp/products/404")
        parseProductUseCase.execute("https://www.coupang.com/vp/products/404")

        assertEquals(2, dedicatedParser.parseCallCount.get())
        assertEquals(2, fallbackParser.parseCallCount.get())
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

    abstract class CountingParserBase(private val defaultParserName: ParserName) {
        var result: ParseResult = ParseResult.Failure("unset", defaultParserName)
        val parseCallCount = AtomicInteger()

        fun parseWithCount(): ParseResult {
            parseCallCount.incrementAndGet()
            return result
        }

        open fun reset() {
            parseCallCount.set(0)
            result = ParseResult.Failure("unset", defaultParserName)
        }
    }

    class DedicatedCountingParser : CountingParserBase(defaultParserName = ParserName.COUPANG), DedicatedProductParser {
        var canParse: (String) -> Boolean = { false }

        override fun canParse(url: String): Boolean = canParse.invoke(url)

        override fun parse(url: String): ParseResult = parseWithCount()

        override fun reset() {
            super.reset()
            canParse = { false }
        }
    }

    class FallbackCountingParser : CountingParserBase(defaultParserName = ParserName.OG), FallbackProductParser {
        override fun parse(url: String): ParseResult = parseWithCount()
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableCaching
    @Import(ParseProductUsecase::class, ParserPipeline::class, ParserResolver::class, TestConfig::class)
    class TestApp

    @TestConfiguration
    class TestConfig {
        @Bean
        fun dedicatedParser(): DedicatedCountingParser = DedicatedCountingParser()

        @Bean
        fun fallbackParser(): FallbackCountingParser = FallbackCountingParser()
    }
}
