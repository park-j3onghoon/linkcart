package com.linkcart.infrastructure.adapter.persistence.userproduct

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface UserProductJpaRepository : JpaRepository<UserProductEntity, Long> {
    fun existsByUserIdAndSourceUrl(userId: Long, sourceUrl: String): Boolean

    @Query(
        """
        SELECT p FROM UserProductEntity p
        WHERE p.userId = :userId
          AND (:cursorCreatedAt IS NULL
               OR p.createdAt < :cursorCreatedAt
               OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId))
        ORDER BY p.createdAt DESC, p.id DESC
        """,
    )
    fun findPage(
        @Param("userId") userId: Long,
        @Param("cursorCreatedAt") cursorCreatedAt: Instant?,
        @Param("cursorId") cursorId: Long?,
        pageable: Pageable,
    ): List<UserProductEntity>
}
