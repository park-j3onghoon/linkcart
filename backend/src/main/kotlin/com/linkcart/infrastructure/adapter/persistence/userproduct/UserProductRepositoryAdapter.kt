package com.linkcart.infrastructure.adapter.persistence.userproduct

import com.linkcart.domain.model.UserProduct
import com.linkcart.domain.port.UserProductRepository
import org.springframework.stereotype.Component

@Component
class UserProductRepositoryAdapter(
    private val jpaRepository: UserProductJpaRepository,
) : UserProductRepository {

    override fun save(product: UserProduct): UserProduct =
        jpaRepository.saveAndFlush(product.toEntity()).toDomain()

    override fun findById(id: Long): UserProduct? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<UserProduct> =
        jpaRepository.findAllByUserIdOrderByCreatedAtDesc(userId).map { it.toDomain() }

    override fun deleteById(id: Long) {
        jpaRepository.deleteById(id)
    }

    override fun existsByUserIdAndSourceUrl(userId: Long, sourceUrl: String): Boolean =
        jpaRepository.existsByUserIdAndSourceUrl(userId, sourceUrl)
}
