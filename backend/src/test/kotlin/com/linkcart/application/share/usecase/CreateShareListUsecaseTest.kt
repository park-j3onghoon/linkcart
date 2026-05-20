package com.linkcart.application.share.usecase

import com.linkcart.application.share.port.ShareListTokenGenerator
import com.linkcart.application.user.usecase.UserProductNotFoundException
import com.linkcart.domain.model.EmptyShareListException
import com.linkcart.domain.vo.ParserName
import com.linkcart.domain.model.ShareList
import com.linkcart.domain.model.UserProduct
import com.linkcart.domain.port.ShareListRepository
import com.linkcart.domain.port.UserProductRepository
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CreateShareListUsecaseTest {

    @Test
    fun `creates share list with snapshots of selected user products`() {
        val stubProducts = mapOf(
            10L to sampleProduct(id = 10L, userId = OWNER_ID, name = "아이패드", source = "https://s/1"),
            20L to sampleProduct(id = 20L, userId = OWNER_ID, name = "키보드", source = "https://s/2"),
        )
        val productRepo = StubUserProductRepository(stubProducts)
        val shareRepo = StubShareListRepository()
        val tokenGen = FixedTokenGenerator("TOKEN_ABC")

        val sut = CreateShareListUsecase(productRepo, shareRepo, tokenGen)

        val result = sut.execute(
            ownerId = OWNER_ID,
            productIds = listOf(10L, 20L),
            title = "내 리스트",
            expiresAt = null,
        )

        assertNotNull(result.id)
        assertEquals(OWNER_ID, result.ownerId)
        assertEquals("TOKEN_ABC", result.token)
        assertEquals("내 리스트", result.title)
        assertEquals(2, result.items.size)
        assertEquals("아이패드", result.items[0].name)
        assertEquals("https://s/1", result.items[0].sourceUrl)
        assertEquals("키보드", result.items[1].name)
    }

    @Test
    fun `snapshots are copies, mutating source product afterwards does not affect share list`() {
        val product = sampleProduct(id = 10L, userId = OWNER_ID, name = "원본", source = "https://s/1")
        val productRepo = StubUserProductRepository(mapOf(10L to product))
        val shareRepo = StubShareListRepository()

        val sut = CreateShareListUsecase(productRepo, shareRepo, FixedTokenGenerator("T"))
        val shared = sut.execute(OWNER_ID, listOf(10L))

        assertEquals("원본", shared.items[0].name)
        assertEquals(Money(amount = 1000L), shared.items[0].price)
    }

    @Test
    fun `throws EmptyShareListException when productIds is empty`() {
        val sut = CreateShareListUsecase(
            StubUserProductRepository(emptyMap()),
            StubShareListRepository(),
            FixedTokenGenerator("T"),
        )

        assertThrows<EmptyShareListException> {
            sut.execute(ownerId = OWNER_ID, productIds = emptyList())
        }
    }

    @Test
    fun `throws UserProductNotFoundException when productId does not exist`() {
        val productRepo = StubUserProductRepository(emptyMap())
        val sut = CreateShareListUsecase(productRepo, StubShareListRepository(), FixedTokenGenerator("T"))

        assertThrows<UserProductNotFoundException> {
            sut.execute(ownerId = OWNER_ID, productIds = listOf(999L))
        }
    }

    @Test
    fun `throws UserProductNotFoundException when product is owned by another user`() {
        val otherOwned = sampleProduct(id = 10L, userId = 999L, name = "남의것", source = "https://s/x")
        val productRepo = StubUserProductRepository(mapOf(10L to otherOwned))
        val shareRepo = StubShareListRepository()
        val sut = CreateShareListUsecase(productRepo, shareRepo, FixedTokenGenerator("T"))

        assertThrows<UserProductNotFoundException> {
            sut.execute(ownerId = OWNER_ID, productIds = listOf(10L))
        }
        assertEquals(0, shareRepo.saveCount)
    }

    @Test
    fun `propagates expiresAt to saved share list`() {
        val product = sampleProduct(id = 10L, userId = OWNER_ID, name = "a", source = "https://s/1")
        val productRepo = StubUserProductRepository(mapOf(10L to product))
        val shareRepo = StubShareListRepository()
        val expiresAt = Instant.parse("2030-01-01T00:00:00Z")

        val sut = CreateShareListUsecase(productRepo, shareRepo, FixedTokenGenerator("T"))
        val result = sut.execute(OWNER_ID, listOf(10L), expiresAt = expiresAt)

        assertEquals(expiresAt, result.expiresAt)
    }

    private fun sampleProduct(
        id: Long,
        userId: Long,
        name: String,
        source: String,
    ): UserProduct = UserProduct(
        id = id,
        userId = userId,
        name = name,
        price = Money(amount = 1000L),
        imageUrl = "https://cdn/$id.jpg",
        sourceUrl = source,
        mall = Mall.GENERIC,
        parserUsed = ParserName.OG,
    )

    companion object {
        private const val OWNER_ID = 1L
    }

    private class StubUserProductRepository(
        private val products: Map<Long, UserProduct>,
    ) : UserProductRepository {
        override fun save(product: UserProduct): UserProduct = product
        override fun findById(id: Long): UserProduct? = products[id]
        override fun findPageByUserId(userId: Long, cursorCreatedAt: java.time.Instant?, cursorId: Long?, limit: Int): List<UserProduct> = emptyList()
        override fun deleteById(id: Long) {}
        override fun existsByUserIdAndSourceUrl(userId: Long, sourceUrl: String): Boolean = false
    }

    private class StubShareListRepository : ShareListRepository {
        var saveCount = 0
            private set
        private var nextId = 1L

        override fun save(shareList: ShareList): ShareList {
            saveCount += 1
            return shareList.copy(
                id = nextId++,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            )
        }

        override fun findById(id: Long): ShareList? = null
        override fun findByToken(token: String): ShareList? = null
        override fun deleteById(id: Long) {}
    }

    private class FixedTokenGenerator(private val token: String) : ShareListTokenGenerator {
        override fun generate(): String = token
    }
}
