package com.linkcart.application.user.usecase

import com.linkcart.domain.model.UserProduct
import com.linkcart.domain.vo.ParserName
import com.linkcart.domain.port.UserProductRepository
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class DeleteUserProductUsecaseTest {

    @Test
    fun `deletes when product owner matches userId`() {
        val product = sampleProduct(id = 1L, userId = 7L)
        val repo = StubRepo(byId = mapOf(1L to product))
        val sut = DeleteUserProductUsecase(repo)

        sut.execute(userId = 7L, productId = 1L)

        assertEquals(1, repo.deletedIds.size)
    }

    @Test
    fun `throws not-found when product does not exist`() {
        val sut = DeleteUserProductUsecase(StubRepo())

        assertThrows<UserProductNotFoundException> { sut.execute(userId = 7L, productId = 999L) }
    }

    @Test
    fun `throws not-found when product belongs to another user`() {
        val product = sampleProduct(id = 1L, userId = 9L)
        val repo = StubRepo(byId = mapOf(1L to product))
        val sut = DeleteUserProductUsecase(repo)

        assertThrows<UserProductNotFoundException> { sut.execute(userId = 7L, productId = 1L) }
        assertEquals(0, repo.deletedIds.size)
    }

    private fun sampleProduct(id: Long, userId: Long): UserProduct = UserProduct(
        id = id,
        userId = userId,
        name = "x",
        price = Money(amount = 1000L),
        imageUrl = null,
        sourceUrl = "https://shop/$id",
        mall = Mall.GENERIC,
        parserUsed = ParserName.OG,
    )

    private class StubRepo(private val byId: Map<Long, UserProduct> = emptyMap()) : UserProductRepository {
        val deletedIds = mutableListOf<Long>()

        override fun save(product: UserProduct): UserProduct = product.copy(id = 1L)
        override fun findById(id: Long): UserProduct? = byId[id]
        override fun findPageByUserId(userId: Long, cursorCreatedAt: java.time.Instant?, cursorId: Long?, limit: Int): List<UserProduct> = emptyList()
        override fun deleteById(id: Long) {
            deletedIds += id
        }

        override fun existsByUserIdAndSourceUrl(userId: Long, sourceUrl: String): Boolean = false
    }
}
