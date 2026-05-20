package com.linkcart.infrastructure.adapter.persistence.userproduct

import com.linkcart.domain.model.UserProduct
import com.linkcart.domain.port.UserProductRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class UserProductRepositoryAdapter(
    private val jpaRepository: UserProductJpaRepository,
) : UserProductRepository {

    override fun save(product: UserProduct): UserProduct =
        jpaRepository.saveAndFlush(product.toEntity()).toDomain()

    override fun findById(id: Long): UserProduct? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findPageByUserId(
        userId: Long,
        cursorCreatedAt: Instant?,
        cursorId: Long?,
        limit: Int,
    ): List<UserProduct> = jpaRepository.findPage(
        userId = userId,
        cursorCreatedAt = cursorCreatedAt,
        cursorId = cursorId,
        pageable = PageRequest.of(0, limit),
    ).map { it.toDomain() }

    override fun deleteById(id: Long) {
        jpaRepository.deleteById(id)
    }

    override fun existsByUserIdAndSourceUrl(userId: Long, sourceUrl: String): Boolean =
        jpaRepository.existsByUserIdAndSourceUrl(userId, sourceUrl)
}
