package com.linkcart.application.user.usecase

import com.linkcart.domain.model.ParserName
import com.linkcart.domain.model.UserProduct
import com.linkcart.domain.port.UserProductRepository
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ListUserProductsUsecaseTest {

    @Test
    fun `returns first page with nextPageToken when more items remain`() {
        val repo = StubRepo(
            listOf(
                product(id = 1L, createdAt = "2026-04-01T00:00:00Z"),
                product(id = 2L, createdAt = "2026-04-02T00:00:00Z"),
                product(id = 3L, createdAt = "2026-04-03T00:00:00Z"),
            ),
        )
        val sut = ListUserProductsUsecase(repo)

        val page = sut.execute(userId = USER_ID, pageSize = 2, pageToken = null)

        assertEquals(2, page.products.size)
        // createdAt desc + id desc 정렬이므로 id=3, 2 순서
        assertEquals(3L, page.products[0].id)
        assertEquals(2L, page.products[1].id)
        assertNotNull(page.nextPageToken)
    }

    @Test
    fun `returns last page without nextPageToken when items fit`() {
        val repo = StubRepo(
            listOf(
                product(id = 1L, createdAt = "2026-04-01T00:00:00Z"),
                product(id = 2L, createdAt = "2026-04-02T00:00:00Z"),
            ),
        )
        val sut = ListUserProductsUsecase(repo)

        val page = sut.execute(userId = USER_ID, pageSize = 10, pageToken = null)

        assertEquals(2, page.products.size)
        assertNull(page.nextPageToken)
    }

    @Test
    fun `next page resumes after the previous cursor`() {
        val repo = StubRepo(
            listOf(
                product(id = 1L, createdAt = "2026-04-01T00:00:00Z"),
                product(id = 2L, createdAt = "2026-04-02T00:00:00Z"),
                product(id = 3L, createdAt = "2026-04-03T00:00:00Z"),
            ),
        )
        val sut = ListUserProductsUsecase(repo)

        val first = sut.execute(userId = USER_ID, pageSize = 2, pageToken = null)
        val second = sut.execute(userId = USER_ID, pageSize = 2, pageToken = first.nextPageToken)

        assertEquals(listOf(1L), second.products.map { it.id })
        assertNull(second.nextPageToken)
    }

    @Test
    fun `throws InvalidPageTokenException when token is not base64`() {
        val sut = ListUserProductsUsecase(StubRepo(emptyList()))

        assertThrows<InvalidPageTokenException> {
            sut.execute(userId = USER_ID, pageSize = 50, pageToken = "not-base64!@#")
        }
    }

    @Test
    fun `throws InvalidPageTokenException when token has wrong shape (no colon)`() {
        val sut = ListUserProductsUsecase(StubRepo(emptyList()))
        val badToken = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("123.456-no-colon-id".toByteArray())

        assertThrows<InvalidPageTokenException> {
            sut.execute(userId = USER_ID, pageSize = 50, pageToken = badToken)
        }
    }

    @Test
    fun `throws InvalidPageTokenException when time segment lacks dot separator`() {
        val sut = ListUserProductsUsecase(StubRepo(emptyList()))
        val badToken = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("1234567890:42".toByteArray())

        assertThrows<InvalidPageTokenException> {
            sut.execute(userId = USER_ID, pageSize = 50, pageToken = badToken)
        }
    }

    @Test
    fun `throws InvalidPageTokenException when numeric parts are not numbers`() {
        val sut = ListUserProductsUsecase(StubRepo(emptyList()))
        val badToken = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("abc.def:xyz".toByteArray())

        assertThrows<InvalidPageTokenException> {
            sut.execute(userId = USER_ID, pageSize = 50, pageToken = badToken)
        }
    }

    @Test
    fun `cursor preserves sub-millisecond precision so rows in same ms are not skipped`() {
        // PostgreSQL TIMESTAMPTZ가 microsecond 정밀도를 가지므로 cursor가 millis로 truncate되면
        // 같은 ms 안의 다른 micro row가 페이지에서 누락될 수 있다 (Codex adversarial 회귀 방지).
        val baseMillis = Instant.parse("2026-04-01T00:00:00Z")
        val older = baseMillis.plusNanos(500_000) // 500 microseconds
        val newer = baseMillis.plusNanos(800_000) // 800 microseconds
        val repo = StubRepo(
            listOf(
                productAt(id = 1L, createdAt = older),
                productAt(id = 2L, createdAt = newer),
            ),
        )
        val sut = ListUserProductsUsecase(repo)

        val first = sut.execute(USER_ID, pageSize = 1, pageToken = null)
        val second = sut.execute(USER_ID, pageSize = 1, pageToken = first.nextPageToken)

        assertEquals(listOf(2L), first.products.map { it.id })
        assertEquals(listOf(1L), second.products.map { it.id })
        assertNull(second.nextPageToken)
    }

    @Test
    fun `throws InvalidPageSizeException when pageSize is out of bounds`() {
        val sut = ListUserProductsUsecase(StubRepo(emptyList()))

        assertThrows<InvalidPageSizeException> {
            sut.execute(userId = USER_ID, pageSize = 0, pageToken = null)
        }
        assertThrows<InvalidPageSizeException> {
            sut.execute(userId = USER_ID, pageSize = 9999, pageToken = null)
        }
        assertThrows<InvalidPageSizeException> {
            sut.execute(userId = USER_ID, pageSize = -1, pageToken = null)
        }
    }

    private fun product(id: Long, createdAt: String): UserProduct = UserProduct(
        id = id,
        userId = USER_ID,
        name = "p$id",
        price = Money(amount = 1000L),
        imageUrl = null,
        sourceUrl = "https://s/$id",
        mall = Mall.GENERIC,
        parserUsed = ParserName.OG,
        createdAt = Instant.parse(createdAt),
    )

    private fun productAt(id: Long, createdAt: Instant): UserProduct = UserProduct(
        id = id,
        userId = USER_ID,
        name = "p$id",
        price = Money(amount = 1000L),
        imageUrl = null,
        sourceUrl = "https://s/$id",
        mall = Mall.GENERIC,
        parserUsed = ParserName.OG,
        createdAt = createdAt,
    )

    companion object {
        private const val USER_ID = 1L
    }

    private class StubRepo(items: List<UserProduct>) : UserProductRepository {
        private val sorted = items.sortedWith(
            compareByDescending<UserProduct> { it.createdAt!! }.thenByDescending { it.id!! },
        )

        override fun save(product: UserProduct): UserProduct = product
        override fun findById(id: Long): UserProduct? = null

        override fun findPageByUserId(
            userId: Long,
            cursorCreatedAt: Instant?,
            cursorId: Long?,
            limit: Int,
        ): List<UserProduct> {
            val filtered = if (cursorCreatedAt == null) {
                sorted
            } else {
                sorted.filter {
                    it.createdAt!!.isBefore(cursorCreatedAt) ||
                        (it.createdAt == cursorCreatedAt && it.id!! < cursorId!!)
                }
            }
            return filtered.take(limit)
        }

        override fun deleteById(id: Long) {}
        override fun existsByUserIdAndSourceUrl(userId: Long, sourceUrl: String): Boolean = false
    }
}
