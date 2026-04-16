package com.linkcart.application.usecase

import com.linkcart.application.parser.ParserFactory
import com.linkcart.domain.entity.Product
import com.linkcart.domain.model.ParseResult
import com.linkcart.domain.port.ProductParser
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.cache.annotation.EnableCaching
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(classes = [ParseProductUseCaseCachingTest.TestApp::class])
class ParseProductUseCaseCachingTest {

    @Autowired
    private lateinit var parseProductUseCase: ParseProductUseCase

    @Autowired
    @Qualifier("dedicatedParser")
    private lateinit var dedicatedParser: CountingProductParser

    @Autowired
    @Qualifier("ogParser")
    private lateinit var fallbackParser: CountingProductParser

    @BeforeEach
    fun setUp() {
        dedicatedParser.reset()
        fallbackParser.reset()
    }

    @Test
    fun `cache proxy is enabled and caches success results`() {
        dedicatedParser.canParse = { it.contains("coupang.com") }
        dedicatedParser.result = success(parserUsed = "coupang-api")
        fallbackParser.result = success(parserUsed = "og")

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
        dedicatedParser.result = ParseResult.Failure("쿠팡 API 호출 실패: 404", "coupang-api")
        fallbackParser.result = ParseResult.Failure("파싱 가능한 정보가 없습니다", "og")

        parseProductUseCase.execute("https://www.coupang.com/vp/products/404")
        parseProductUseCase.execute("https://www.coupang.com/vp/products/404")

        assertEquals(2, dedicatedParser.parseCallCount.get())
        assertEquals(2, fallbackParser.parseCallCount.get())
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

    class CountingProductParser(
        private val fallback: Boolean,
    ) : ProductParser {
        var canParse: (String) -> Boolean = { fallback }
        var result: ParseResult = ParseResult.Failure("unset", if (fallback) "og" else "coupang-api")
        val parseCallCount = AtomicInteger()

        override fun canParse(url: String): Boolean = canParse.invoke(url)

        override fun parse(url: String): ParseResult {
            parseCallCount.incrementAndGet()
            return result
        }

        fun reset() {
            parseCallCount.set(0)
            canParse = { fallback }
            result = ParseResult.Failure("unset", if (fallback) "og" else "coupang-api")
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableCaching
    @Import(ParseProductUseCase::class, TestConfig::class)
    class TestApp

    @TestConfiguration
    class TestConfig {
        @Bean
        fun dedicatedParser(): CountingProductParser = CountingProductParser(fallback = false)

        @Bean("ogParser")
        fun fallbackParser(): CountingProductParser = CountingProductParser(fallback = true)

        @Bean
        fun parserFactory(
            @Qualifier("dedicatedParser")
            dedicatedParser: CountingProductParser,
            @Qualifier("ogParser")
            fallbackParser: CountingProductParser,
        ): ParserFactory = ParserFactory(listOf(dedicatedParser, fallbackParser), fallbackParser)
    }
}
