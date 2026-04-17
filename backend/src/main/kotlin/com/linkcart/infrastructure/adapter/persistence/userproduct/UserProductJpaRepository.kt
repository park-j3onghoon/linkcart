package com.linkcart.infrastructure.adapter.persistence.userproduct

import org.springframework.data.jpa.repository.JpaRepository

interface UserProductJpaRepository : JpaRepository<UserProductEntity, Long> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<UserProductEntity>
    fun existsByUserIdAndSourceUrl(userId: Long, sourceUrl: String): Boolean
}
