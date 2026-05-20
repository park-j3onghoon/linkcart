package com.linkcart.application.share.usecase

import com.linkcart.domain.vo.ParserName
import com.linkcart.domain.model.ShareList
import com.linkcart.domain.model.ShareListItem
import com.linkcart.domain.model.UserProduct
import com.linkcart.domain.port.ShareListRepository
import com.linkcart.domain.port.UserProductRepository
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class CopyShareListUsecaseTest {

    @Test
    fun `copies all items into viewer's products when token matches`() {
        val shareList = shareListWith(
            items = listOf(
                item(name = "a", sourceUrl = "https://s/1", mall = Mall.COUPANG),
                item(name = "b", sourceUrl = "https://s/2", mall = Mall.GENERIC),
            ),
        )
        val repo = StubUserProductRepository()
        val sut = newSut(shareList, repo)

        val result = sut.execute(viewerId = VIEWER_ID, shareListId = SHARE_LIST_ID, token = TOKEN)

        assertEquals(2, result.copiedCount)
        assertEquals(0, result.skippedCount)
        assertEquals(2, repo.saved.size)
        assertEquals(VIEWER_ID, repo.saved[0].userId)
        assertEquals(ParserName.COUPANG, repo.saved[0].parserUsed)
        assertEquals(ParserName.OG, repo.saved[1].parserUsed)
    }

    @Test
    fun `skips items already in viewer's products by sourceUrl`() {
        val shareList = shareListWith(
            items = listOf(
                item(name = "a", sourceUrl = "https://s/dup", mall = Mall.COUPANG),
                item(name = "b", sourceUrl = "https://s/new", mall = Mall.GENERIC),
            ),
        )
        val repo = StubUserProductRepository(existingSourceUrls = setOf("https://s/dup"))
        val sut = newSut(shareList, repo)

        val result = sut.execute(viewerId = VIEWER_ID, shareListId = SHARE_LIST_ID, token = TOKEN)

        assertEquals(1, result.copiedCount)
        assertEquals(1, result.skippedCount)
        assertEquals("https://s/new", repo.saved.single().sourceUrl)
    }

    @Test
    fun `throws ShareListNotFoundException when token does not match any list`() {
        val repo = StubUserProductRepository()
        val sut = newSut(shareList = null, productRepo = repo)

        assertThrows<ShareListNotFoundException> {
            sut.execute(VIEWER_ID, SHARE_LIST_ID, token = "missing")
        }
    }

    @Test
    fun `throws ShareListNotFoundException when path id does not match token's share list`() {
        val shareList = shareListWith(
            items = listOf(item(name = "a", sourceUrl = "https://s/1", mall = Mall.GENERIC)),
        )
        val repo = StubUserProductRepository()
        val sut = newSut(shareList, repo)

        // 토큰은 유효하지만 path id가 다른 ShareList를 가리킴 → enumeration 방지
        assertThrows<ShareListNotFoundException> {
            sut.execute(VIEWER_ID, shareListId = SHARE_LIST_ID + 1, token = TOKEN)
        }
        assertEquals(0, repo.saved.size)
    }

    @Test
    fun `maps 11st mall to 11st parser when copying`() {
        val shareList = shareListWith(
            items = listOf(item(name = "a", sourceUrl = "https://s/1", mall = Mall.ELEVENST)),
        )
        val repo = StubUserProductRepository()
        val sut = newSut(shareList, repo)

        sut.execute(VIEWER_ID, SHARE_LIST_ID, token = TOKEN)

        assertEquals(ParserName.ELEVENST, repo.saved.single().parserUsed)
    }

    private fun newSut(
        shareList: ShareList?,
        productRepo: StubUserProductRepository,
    ): CopyShareListUsecase {
        val byToken = if (shareList == null) emptyMap() else mapOf(TOKEN to shareList)
        return CopyShareListUsecase(
            lookupShareListByTokenUsecase = LookupShareListByTokenUsecase(
                StubShareRepo(byToken),
                fixedClock(),
            ),
            userProductRepository = productRepo,
        )
    }

    private fun shareListWith(items: List<ShareListItem>): ShareList = ShareList(
        id = SHARE_LIST_ID,
        ownerId = 99L,
        token = TOKEN,
        title = "공유",
        expiresAt = null,
        createdAt = Instant.parse("2026-04-20T00:00:00Z"),
        items = items,
    )

    private fun item(name: String, sourceUrl: String, mall: Mall): ShareListItem = ShareListItem(
        id = 1L,
        name = name,
        price = Money(amount = 1000L),
        imageUrl = null,
        sourceUrl = sourceUrl,
        mall = mall,
    )

    private fun fixedClock(): Clock = Clock.fixed(Instant.parse("2026-04-24T00:00:00Z"), ZoneOffset.UTC)

    companion object {
        private const val VIEWER_ID = 42L
        private const val SHARE_LIST_ID = 1L
        private const val TOKEN = "TOKEN"
    }

    private class StubUserProductRepository(
        private val existingSourceUrls: Set<String> = emptySet(),
    ) : UserProductRepository {
        val saved = mutableListOf<UserProduct>()
        private var nextId = 1L

        override fun save(product: UserProduct): UserProduct {
            val persisted = product.copy(id = nextId++)
            saved.add(persisted)
            return persisted
        }

        override fun findById(id: Long): UserProduct? = null
        override fun findPageByUserId(userId: Long, cursorCreatedAt: java.time.Instant?, cursorId: Long?, limit: Int): List<UserProduct> = emptyList()
        override fun deleteById(id: Long) {}
        override fun existsByUserIdAndSourceUrl(userId: Long, sourceUrl: String): Boolean =
            sourceUrl in existingSourceUrls
    }

    private class StubShareRepo(private val byToken: Map<String, ShareList>) : ShareListRepository {
        override fun save(shareList: ShareList): ShareList = shareList
        override fun findById(id: Long): ShareList? = null
        override fun findByToken(token: String): ShareList? = byToken[token]
        override fun deleteById(id: Long) {}
    }
}
