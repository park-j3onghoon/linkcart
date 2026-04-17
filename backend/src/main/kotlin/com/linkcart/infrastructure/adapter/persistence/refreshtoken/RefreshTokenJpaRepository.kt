package com.linkcart.infrastructure.adapter.persistence.refreshtoken

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface RefreshTokenJpaRepository : JpaRepository<RefreshTokenEntity, UUID> {
    fun findByTokenHash(tokenHash: String): RefreshTokenEntity?

    @Modifying
    @Query(
        "UPDATE RefreshTokenEntity r " +
            "SET r.revokedAt = :revokedAt " +
            "WHERE r.userId = :userId AND r.revokedAt IS NULL",
    )
    fun revokeAllActiveForUser(@Param("userId") userId: Long, @Param("revokedAt") revokedAt: Instant): Int

    @Modifying
    @Query(
        "UPDATE RefreshTokenEntity r " +
            "SET r.revokedAt = :revokedAt, r.replacedByTokenId = :replacedByTokenId " +
            "WHERE r.id = :id",
    )
    fun markRevoked(
        @Param("id") id: UUID,
        @Param("revokedAt") revokedAt: Instant,
        @Param("replacedByTokenId") replacedByTokenId: UUID?,
    ): Int
}
