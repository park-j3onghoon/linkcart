package com.linkcart.infrastructure.adapter.persistence.sharelist

import com.linkcart.domain.model.ShareList
import com.linkcart.domain.port.ShareListRepository
import org.springframework.stereotype.Component

@Component
class ShareListRepositoryAdapter(
    private val jpaRepository: ShareListJpaRepository,
) : ShareListRepository {

    override fun save(shareList: ShareList): ShareList =
        jpaRepository.saveAndFlush(shareList.toEntity()).toDomain()

    override fun findById(id: Long): ShareList? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByToken(token: String): ShareList? =
        jpaRepository.findByToken(token)?.toDomain()

    override fun findAllByOwnerIdOrderByCreatedAtDesc(ownerId: Long): List<ShareList> =
        jpaRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).map { it.toDomain() }

    override fun deleteById(id: Long) {
        jpaRepository.deleteById(id)
    }
}
