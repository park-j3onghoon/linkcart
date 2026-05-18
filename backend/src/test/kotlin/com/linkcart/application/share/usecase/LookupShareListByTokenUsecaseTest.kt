package com.linkcart.application.share.usecase

import com.linkcart.domain.model.ShareList
import com.linkcart.domain.model.ShareListItem
import com.linkcart.domain.port.ShareListRepository
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class LookupShareListByTokenUsecaseTest {

    @Test
    fun `returns share list when token exists and not expired`() {
        val shareList = sampleShareList(token = "abc", expiresAt = null)
        val repo = StubRepo(mapOf("abc" to shareList))

        val sut = LookupShareListByTokenUsecase(repo, fixedClock(NOW))
        val result = sut.execute("abc")

        assertEquals("abc", result.token)
        assertEquals(1, result.items.size)
    }

    @Test
    fun `throws ShareListNotFoundException when token does not exist`() {
        val sut = LookupShareListByTokenUsecase(StubRepo(emptyMap()), fixedClock(NOW))

        assertThrows<ShareListNotFoundException> { sut.execute("missing") }
    }

    @Test
    fun `throws ShareListNotFoundException when share list is expired`() {
        val expired = sampleShareList(
            token = "abc",
            expiresAt = Instant.parse("2026-04-24T10:00:00Z"),
        )
        val repo = StubRepo(mapOf("abc" to expired))
        val afterExpiry = Instant.parse("2026-04-24T10:00:01Z")

        val sut = LookupShareListByTokenUsecase(repo, fixedClock(afterExpiry))

        assertThrows<ShareListNotFoundException> { sut.execute("abc") }
    }

    @Test
    fun `returns share list when not yet expired`() {
        val futureExpiry = Instant.parse("2030-01-01T00:00:00Z")
        val list = sampleShareList(token = "abc", expiresAt = futureExpiry)
        val repo = StubRepo(mapOf("abc" to list))

        val sut = LookupShareListByTokenUsecase(repo, fixedClock(NOW))
        val result = sut.execute("abc")

        assertEquals("abc", result.token)
    }

    private fun sampleShareList(token: String, expiresAt: Instant?): ShareList = ShareList(
        id = 1L,
        ownerId = 10L,
        token = token,
        title = "공유",
        expiresAt = expiresAt,
        createdAt = Instant.parse("2026-04-20T00:00:00Z"),
        items = listOf(
            ShareListItem(
                id = 100L,
                name = "상품",
                price = Money(amount = 1000L),
                imageUrl = null,
                sourceUrl = "https://s/1",
                mall = Mall.GENERIC,
            ),
        ),
    )

    private fun fixedClock(at: Instant): Clock = Clock.fixed(at, ZoneOffset.UTC)

    companion object {
        private val NOW: Instant = Instant.parse("2026-04-24T00:00:00Z")
    }

    private class StubRepo(private val byToken: Map<String, ShareList>) : ShareListRepository {
        override fun save(shareList: ShareList): ShareList = shareList
        override fun findById(id: Long): ShareList? = null
        override fun findByToken(token: String): ShareList? = byToken[token]
        override fun findAllByOwnerIdOrderByCreatedAtDesc(ownerId: Long): List<ShareList> = emptyList()
        override fun deleteById(id: Long) {}
    }
}
