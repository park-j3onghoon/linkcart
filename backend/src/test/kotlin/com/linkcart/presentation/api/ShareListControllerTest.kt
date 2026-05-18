package com.linkcart.presentation.api

import com.linkcart.application.share.usecase.CopyShareListResult
import com.linkcart.application.share.usecase.CopyShareListUsecase
import com.linkcart.application.share.usecase.CreateShareListUsecase
import com.linkcart.application.share.usecase.DeleteShareListUsecase
import com.linkcart.application.share.usecase.EmptyShareListException
import com.linkcart.application.share.usecase.GetShareListByTokenUsecase
import com.linkcart.application.share.usecase.ShareListNotFoundException
import com.linkcart.application.user.usecase.UserProductNotFoundException
import com.linkcart.domain.model.ParserName
import com.linkcart.domain.model.UserProduct
import com.linkcart.domain.model.ShareList
import com.linkcart.domain.model.ShareListItem
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money
import com.linkcart.infrastructure.config.WebConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
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
import org.mockito.BDDMockito.willThrow
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@WebMvcTest(
    controllers = [ShareListController::class],
    excludeFilters = [Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [WebConfig::class])],
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
class ShareListControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var createShareListUsecase: CreateShareListUsecase

    @MockBean
    private lateinit var getShareListByTokenUsecase: GetShareListByTokenUsecase

    @MockBean
    private lateinit var copyShareListUsecase: CopyShareListUsecase

    @MockBean
    private lateinit var deleteShareListUsecase: DeleteShareListUsecase

    @BeforeEach
    fun setUpAuthentication() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(OWNER_ID, null, emptyList())
    }

    @AfterEach
    fun clearAuthentication() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `creates share list returns 201 with token and items`() {
        given(
            createShareListUsecase.execute(
                ownerId = OWNER_ID,
                productIds = listOf(1L),
                title = "내 리스트",
                expiresAt = null,
            ),
        ).willReturn(
            ShareList(
                id = 42L,
                ownerId = OWNER_ID,
                token = "TOKEN_ABC",
                title = "내 리스트",
                createdAt = Instant.parse("2026-04-24T10:00:00Z"),
                items = listOf(
                    ShareListItem(
                        id = 1L,
                        name = "아이패드",
                        price = Money(amount = 899000L),
                        imageUrl = "https://cdn/1.jpg",
                        sourceUrl = "https://s/1",
                        mall = Mall.COUPANG,
                    ),
                ),
            ),
        )

        mockMvc.perform(
            post("/api/v1/shareLists")
                
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productIds":[1],"title":"내 리스트"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(42))
            .andExpect(jsonPath("$.token").value("TOKEN_ABC"))
            .andExpect(jsonPath("$.title").value("내 리스트"))
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].name").value("아이패드"))
            .andExpect(jsonPath("$.items[0].sourceUrl").value("https://s/1"))
            .andExpect(jsonPath("$.items[0].mall").value("coupang"))
    }

    @Test
    fun `empty product_ids returns 400 via validation`() {
        mockMvc.perform(
            post("/api/v1/shareLists")
                
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productIds":[]}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
    }

    @Test
    fun `usecase throws EmptyShareListException returns 400`() {
        given(
            createShareListUsecase.execute(
                ownerId = OWNER_ID,
                productIds = listOf(1L),
                title = null,
                expiresAt = null,
            ),
        ).willThrow(EmptyShareListException("공유할 상품을 1개 이상 선택해주세요"))

        mockMvc.perform(
            post("/api/v1/shareLists")
                
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productIds":[1]}"""),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `missing product returns 404`() {
        given(
            createShareListUsecase.execute(
                ownerId = OWNER_ID,
                productIds = listOf(999L),
                title = null,
                expiresAt = null,
            ),
        ).willThrow(UserProductNotFoundException("상품을 찾을 수 없습니다"))

        mockMvc.perform(
            post("/api/v1/shareLists")
                
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productIds":[999]}"""),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `title over 200 chars returns 400`() {
        val longTitle = "a".repeat(201)
        mockMvc.perform(
            post("/api/v1/shareLists")
                
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productIds":[1],"title":"$longTitle"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
    }

    @Test
    fun `findByToken returns 200 with share list`() {
        given(getShareListByTokenUsecase.execute("TOKEN_ABC")).willReturn(
            ShareList(
                id = 42L,
                ownerId = OWNER_ID,
                token = "TOKEN_ABC",
                title = "공유",
                createdAt = Instant.parse("2026-04-24T10:00:00Z"),
                items = listOf(
                    ShareListItem(
                        id = 1L,
                        name = "상품",
                        price = Money(amount = 1000L),
                        imageUrl = null,
                        sourceUrl = "https://s/1",
                        mall = Mall.GENERIC,
                    ),
                ),
            ),
        )

        mockMvc.perform(get("/api/v1/shareLists/TOKEN_ABC"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").value("TOKEN_ABC"))
            .andExpect(jsonPath("$.items[0].name").value("상품"))
    }

    @Test
    fun `findByToken returns 404 when token missing or expired`() {
        given(getShareListByTokenUsecase.execute("missing"))
            .willThrow(ShareListNotFoundException("공유 리스트를 찾을 수 없습니다"))

        mockMvc.perform(get("/api/v1/shareLists/missing"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `copy returns 201 with counts and copied products`() {
        given(copyShareListUsecase.execute(viewerId = OWNER_ID, token = "TOKEN_ABC")).willReturn(
            CopyShareListResult(
                copiedCount = 1,
                skippedCount = 1,
                products = listOf(
                    UserProduct(
                        id = 100L,
                        userId = OWNER_ID,
                        name = "복사된 상품",
                        price = Money(amount = 5000L),
                        imageUrl = null,
                        sourceUrl = "https://s/new",
                        mall = Mall.GENERIC,
                        parserUsed = ParserName.OG,
                        createdAt = Instant.parse("2026-04-24T10:00:00Z"),
                    ),
                ),
            ),
        )

        mockMvc.perform(post("/api/v1/shareLists/TOKEN_ABC:copy"))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.copiedCount").value(1))
            .andExpect(jsonPath("$.skippedCount").value(1))
            .andExpect(jsonPath("$.products[0].name").value("복사된 상품"))
    }

    @Test
    fun `copy returns 404 when token missing`() {
        given(copyShareListUsecase.execute(viewerId = OWNER_ID, token = "missing"))
            .willThrow(ShareListNotFoundException("공유 리스트를 찾을 수 없습니다"))

        mockMvc.perform(post("/api/v1/shareLists/missing:copy"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `delete returns 204 when owner matches`() {
        mockMvc.perform(delete("/api/v1/shareLists/TOKEN"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `delete returns 404 when token missing or other-owner`() {
        willThrow(ShareListNotFoundException("공유 리스트를 찾을 수 없습니다"))
            .given(deleteShareListUsecase)
            .execute(ownerId = OWNER_ID, token = "missing")

        mockMvc.perform(delete("/api/v1/shareLists/missing"))
            .andExpect(status().isNotFound)
    }

    companion object {
        private const val OWNER_ID = 7L
    }
}
