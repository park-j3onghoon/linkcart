package com.linkcart.infrastructure.adapter.persistence.sharelist

import org.springframework.data.jpa.repository.JpaRepository

interface ShareListJpaRepository : JpaRepository<ShareListEntity, Long> {
    fun findByToken(token: String): ShareListEntity?
}
