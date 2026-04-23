package com.linkcart.application.share.usecase

import com.linkcart.domain.model.ShareList
import com.linkcart.domain.port.ShareListRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals

class DeleteShareListUsecaseTest {

    @Test
    fun `deletes share list when owner matches`() {
        val shareList = sample(id = 5L, ownerId = OWNER_ID, token = "t")
        val repo = StubRepo(mapOf("t" to shareList))

        DeleteShareListUsecase(repo).execute(ownerId = OWNER_ID, token = "t")

        assertEquals(listOf(5L), repo.deletedIds)
    }

    @Test
    fun `throws ShareListNotFoundException when token missing`() {
        val repo = StubRepo(emptyMap())

        assertThrows<ShareListNotFoundException> {
            DeleteShareListUsecase(repo).execute(ownerId = OWNER_ID, token = "missing")
        }
        assertEquals(emptyList(), repo.deletedIds)
    }

    @Test
    fun `throws ShareListNotFoundException when owner differs`() {
        val shareList = sample(id = 5L, ownerId = 999L, token = "t")
        val repo = StubRepo(mapOf("t" to shareList))

        assertThrows<ShareListNotFoundException> {
            DeleteShareListUsecase(repo).execute(ownerId = OWNER_ID, token = "t")
        }
        assertEquals(emptyList(), repo.deletedIds)
    }

    private fun sample(id: Long, ownerId: Long, token: String): ShareList = ShareList(
        id = id,
        ownerId = ownerId,
        token = token,
        title = null,
        expiresAt = null,
        createdAt = Instant.parse("2026-04-20T00:00:00Z"),
        items = emptyList(),
    )

    companion object {
        private const val OWNER_ID = 1L
    }

    private class StubRepo(private val byToken: Map<String, ShareList>) : ShareListRepository {
        val deletedIds = mutableListOf<Long>()
        override fun save(shareList: ShareList): ShareList = shareList
        override fun findByToken(token: String): ShareList? = byToken[token]
        override fun findAllByOwnerIdOrderByCreatedAtDesc(ownerId: Long): List<ShareList> = emptyList()
        override fun deleteById(id: Long) {
            deletedIds.add(id)
        }
    }
}
