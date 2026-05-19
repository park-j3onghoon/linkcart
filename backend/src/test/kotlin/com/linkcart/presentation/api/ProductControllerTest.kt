package com.linkcart.presentation.api

import com.linkcart.application.parser.usecase.ParseProductUseCase
import com.linkcart.domain.model.Product
import com.linkcart.domain.model.ParseResult
import com.linkcart.domain.model.ParserName
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money
import com.linkcart.application.parser.port.SafeUrlChecker
import com.linkcart.infrastructure.config.WebConfig
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.context.annotation.ComponentScan.Filter
import org.springframework.context.annotation.FilterType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    controllers = [ProductController::class],
    excludeFilters = [Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [WebConfig::class])],
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
class ProductControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var parseProductUseCase: ParseProductUseCase

    @MockBean
    private lateinit var safeUrlChecker: SafeUrlChecker

    @Test
    fun `successful parse returns 200 response`() {
        given(safeUrlChecker.isSafe("https://www.coupang.com/vp/products/123")).willReturn(true)
        given(parseProductUseCase.execute("https://www.coupang.com/vp/products/123")).willReturn(
            ParseResult.Success(
                product = Product(
                    name = "테스트 상품",
                    price = Money(amount = 1000L),
                    imageUrl = "https://example.com/image.jpg",
                    sourceUrl = "https://www.coupang.com/vp/products/123",
                    mall = Mall.COUPANG,
                ),
                parserUsed = ParserName.COUPANG,
            ),
        )

        mockMvc.perform(
            post("/api/v1/products:parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"url":"https://www.coupang.com/vp/products/123"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("테스트 상품"))
            .andExpect(jsonPath("$.price.amount").value(1000))
            .andExpect(jsonPath("$.imageUrl").value("https://example.com/image.jpg"))
            .andExpect(jsonPath("$.sourceUrl").value("https://www.coupang.com/vp/products/123"))
            .andExpect(jsonPath("$.mall").value("coupang"))
            .andExpect(jsonPath("$.partial").value(nullValue()))
            .andExpect(jsonPath("$.parserUsed").value("coupang-api"))
            .andExpect(jsonPath("$.fallbackUsed").value(false))
    }

    @Test
    fun `successful 11st parse returns 200 response with parser_used 11st-api`() {
        given(safeUrlChecker.isSafe("https://www.11st.co.kr/products/456")).willReturn(true)
        given(parseProductUseCase.execute("https://www.11st.co.kr/products/456")).willReturn(
            ParseResult.Success(
                product = Product(
                    name = "11번가 상품",
                    price = Money(amount = 2000L),
                    imageUrl = "https://cdn.11st.co.kr/image.jpg",
                    sourceUrl = "https://www.11st.co.kr/products/456",
                    mall = Mall.ELEVENST,
                ),
                parserUsed = ParserName.ELEVENST,
            ),
        )

        mockMvc.perform(
            post("/api/v1/products:parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"url":"https://www.11st.co.kr/products/456"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.parserUsed").value("11st-api"))
    }

    @Test
    fun `blank url returns 400 response`() {
        mockMvc.perform(
            post("/api/v1/products:parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"url":""}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
    }

    @Test
    fun `invalid url format returns 400 response`() {
        mockMvc.perform(
            post("/api/v1/products:parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"url":"ftp://example.com/product"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
    }

    @Test
    fun `parse failure returns 502 response`() {
        given(safeUrlChecker.isSafe("https://www.coupang.com/vp/products/404")).willReturn(true)
        given(parseProductUseCase.execute("https://www.coupang.com/vp/products/404")).willReturn(
            ParseResult.Failure(
                reason = "상품 정보를 가져올 수 없습니다",
                parserUsed = ParserName.COUPANG,
            ),
        )

        mockMvc.perform(
            post("/api/v1/products:parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"url":"https://www.coupang.com/vp/products/404"}"""),
        )
            .andExpect(status().isBadGateway)
            .andExpect(jsonPath("$.code").value("UNAVAILABLE"))
            .andExpect(jsonPath("$.message").value("상품 정보를 가져올 수 없습니다"))
    }

    @Test
    fun `partial parse returns 200 response with snake case partial keys`() {
        given(safeUrlChecker.isSafe("https://example.com/product/partial")).willReturn(true)
        given(parseProductUseCase.execute("https://example.com/product/partial")).willReturn(
            ParseResult.Partial(
                name = "부분 상품",
                imageUrl = "https://example.com/partial.jpg",
                parserUsed = ParserName.OG,
                fallbackUsed = true,
            ),
        )

        mockMvc.perform(
            post("/api/v1/products:parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"url":"https://example.com/product/partial"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("부분 상품"))
            .andExpect(jsonPath("$.imageUrl").value("https://example.com/partial.jpg"))
            .andExpect(jsonPath("$.sourceUrl").value("https://example.com/product/partial"))
            .andExpect(jsonPath("$.partial.imageUrl").value("https://example.com/partial.jpg"))
            .andExpect(jsonPath("$.parserUsed").value("og"))
            .andExpect(jsonPath("$.fallbackUsed").value(true))
    }

    @Test
    fun `unsafe url returns 400 response`() {
        given(safeUrlChecker.isSafe("http://127.0.0.1/test")).willReturn(false)

        mockMvc.perform(
            post("/api/v1/products:parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"url":"http://127.0.0.1/test"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
            .andExpect(jsonPath("$.message").value("허용되지 않는 URL입니다"))
    }

    @Test
    fun `whitespace-only url returns 400 response`() {
        mockMvc.perform(
            post("/api/v1/products:parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"url":"   "}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
    }

    @Test
    fun `javascript protocol returns 400 response`() {
        mockMvc.perform(
            post("/api/v1/products:parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"url":"javascript:alert(1)"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
    }
}
