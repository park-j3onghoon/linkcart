package com.linkcart.application.user.usecase

import com.linkcart.domain.model.UserProduct
import com.linkcart.domain.vo.ParserName
import com.linkcart.domain.port.UserProductRepository
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class SaveUserProductUsecaseTest {

    @Test
    fun `saves product when source url is unique for user`() {
        val repo = StubRepo()
        val sut = SaveUserProductUsecase(repo)

        val saved = sut.execute(sampleProduct())

        assertEquals(1L, saved.id)
        assertEquals(1, repo.saveCount)
    }

    @Test
    fun `throws DuplicateUserProductException when source url already exists for user`() {
        val repo = StubRepo(alreadyExists = true)
        val sut = SaveUserProductUsecase(repo)

        assertThrows<DuplicateUserProductException> { sut.execute(sampleProduct()) }
        assertEquals(0, repo.saveCount)
    }

    private fun sampleProduct(): UserProduct = UserProduct(
        userId = 1L,
        name = "아이패드",
        price = Money(amount = 899000L),
        imageUrl = "https://cdn/i.jpg",
        sourceUrl = "https://shop.example.com/p/1",
        mall = Mall.GENERIC,
        parserUsed = ParserName.OG,
    )

    private class StubRepo(val alreadyExists: Boolean = false) : UserProductRepository {
        var saveCount = 0
            private set

        override fun save(product: UserProduct): UserProduct {
            saveCount += 1
            return product.copy(id = 1L)
        }

        override fun findById(id: Long): UserProduct? = null
        override fun findPageByUserId(userId: Long, cursorCreatedAt: java.time.Instant?, cursorId: Long?, limit: Int): List<UserProduct> = emptyList()
        override fun deleteById(id: Long) {}
        override fun existsByUserIdAndSourceUrl(userId: Long, sourceUrl: String): Boolean = alreadyExists
    }
}
