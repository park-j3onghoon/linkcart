package com.linkcart.presentation.api

import com.linkcart.application.user.usecase.DeleteUserProductUsecase
import com.linkcart.application.user.usecase.DuplicateUserProductException
import com.linkcart.application.user.usecase.InvalidPageSizeException
import com.linkcart.application.user.usecase.InvalidPageTokenException
import com.linkcart.application.user.usecase.ListUserProductsPage
import com.linkcart.application.user.usecase.ListUserProductsUsecase
import com.linkcart.application.user.usecase.SaveUserProductUsecase
import com.linkcart.application.user.usecase.UserProductNotFoundException
import com.linkcart.domain.model.ParserName
import com.linkcart.domain.model.UserProduct
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money
import com.linkcart.infrastructure.config.WebConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willThrow
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.ComponentScan.Filter
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@WebMvcTest(
    controllers = [UserProductController::class],
    excludeFilters = [Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [WebConfig::class])],
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
class UserProductControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var saveUserProductUsecase: SaveUserProductUsecase

    @MockBean
    private lateinit var listUserProductsUsecase: ListUserProductsUsecase

    @MockBean
    private lateinit var deleteUserProductUsecase: DeleteUserProductUsecase

    @BeforeEach
    fun setUpAuth() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(USER_ID, null, emptyList())
    }

    @AfterEach
    fun clearAuth() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `save returns 201 with AIP-148 resource shape`() {
        given(saveUserProductUsecase.execute(anyUserProduct())).willAnswer {
            (it.arguments[0] as UserProduct).copy(id = 7L, createdAt = Instant.parse("2026-05-19T00:00:00Z"))
        }

        mockMvc.perform(
            post("/api/v1/users/me/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "displayName": "테스트 상품",
                      "price": { "amount": 12900, "currency": "KRW" },
                      "imageUrl": "https://img/1.jpg",
                      "sourceUrl": "https://shop.example.com/p/1",
                      "mall": "coupang",
                      "parserUsed": "coupang-api"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("users/me/products/7"))
            .andExpect(jsonPath("$.displayName").value("테스트 상품"))
            .andExpect(jsonPath("$.price.amount").value(12900))
            .andExpect(jsonPath("$.mall").value("coupang"))
            .andExpect(jsonPath("$.parserUsed").value("coupang-api"))
    }

    @Test
    fun `save returns 409 ALREADY_EXISTS when source URL is duplicated`() {
        willThrow(DuplicateUserProductException("이미 저장된 상품입니다"))
            .given(saveUserProductUsecase).execute(anyUserProduct())

        mockMvc.perform(
            post("/api/v1/users/me/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "displayName": "x",
                      "price": { "amount": 1, "currency": "KRW" },
                      "sourceUrl": "https://shop/dup",
                      "mall": "coupang",
                      "parserUsed": "coupang-api"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("ALREADY_EXISTS"))
    }

    @Test
    fun `save returns 400 INVALID_ARGUMENT for non-http sourceUrl`() {
        mockMvc.perform(
            post("/api/v1/users/me/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "displayName": "x",
                      "price": { "amount": 1, "currency": "KRW" },
                      "sourceUrl": "ftp://shop/p",
                      "mall": "coupang",
                      "parserUsed": "coupang-api"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
    }

    @Test
    fun `list returns 200 with pagination`() {
        given(
            listUserProductsUsecase.execute(userId = USER_ID, pageSize = 50, pageToken = null),
        ).willReturn(
            ListUserProductsPage(
                products = listOf(
                    UserProduct(
                        id = 1L,
                        userId = USER_ID,
                        name = "p1",
                        price = Money(amount = 1000L),
                        imageUrl = null,
                        sourceUrl = "https://s/1",
                        mall = Mall.GENERIC,
                        parserUsed = ParserName.OG,
                        createdAt = Instant.parse("2026-05-19T00:00:00Z"),
                    ),
                ),
                nextPageToken = "next-cursor",
            ),
        )

        mockMvc.perform(get("/api/v1/users/me/products"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.products.length()").value(1))
            .andExpect(jsonPath("$.products[0].name").value("users/me/products/1"))
            .andExpect(jsonPath("$.nextPageToken").value("next-cursor"))
    }

    @Test
    fun `list propagates pageSize and pageToken query params`() {
        given(
            listUserProductsUsecase.execute(userId = USER_ID, pageSize = 10, pageToken = "TOKEN"),
        ).willReturn(ListUserProductsPage(products = emptyList(), nextPageToken = null))

        mockMvc.perform(get("/api/v1/users/me/products?pageSize=10&pageToken=TOKEN"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nextPageToken").doesNotExist())
    }

    @Test
    fun `list returns 400 INVALID_ARGUMENT when pageSize is out of range`() {
        willThrow(InvalidPageSizeException("범위 초과"))
            .given(listUserProductsUsecase).execute(userId = USER_ID, pageSize = 9999, pageToken = null)

        mockMvc.perform(get("/api/v1/users/me/products?pageSize=9999"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
    }

    @Test
    fun `list returns 400 INVALID_ARGUMENT when pageToken is malformed`() {
        willThrow(InvalidPageTokenException("형식 오류"))
            .given(listUserProductsUsecase).execute(userId = USER_ID, pageSize = 50, pageToken = "garbage")

        mockMvc.perform(get("/api/v1/users/me/products?pageToken=garbage"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
    }

    @Test
    fun `delete returns 204 and delegates to usecase`() {
        mockMvc.perform(delete("/api/v1/users/me/products/9"))
            .andExpect(status().isNoContent)

        verify(deleteUserProductUsecase).execute(userId = USER_ID, productId = 9L)
    }

    @Test
    fun `delete returns 404 when product is missing or owned by another user`() {
        willThrow(UserProductNotFoundException("상품을 찾을 수 없습니다"))
            .given(deleteUserProductUsecase).execute(userId = USER_ID, productId = 99L)

        mockMvc.perform(delete("/api/v1/users/me/products/99"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
    }

    companion object {
        private const val USER_ID = 1L

        // Kotlin non-null 파라미터에서 Mockito any()가 null을 반환해 NPE가 나는 문제를 우회.
        // 제네릭 erasure 덕분에 runtime cast check가 비활성화된다.
        @Suppress("UNCHECKED_CAST")
        private fun <T> uninitialized(): T = null as T

        private fun anyUserProduct(): UserProduct {
            org.mockito.ArgumentMatchers.any(UserProduct::class.java)
            return uninitialized()
        }
    }
}
