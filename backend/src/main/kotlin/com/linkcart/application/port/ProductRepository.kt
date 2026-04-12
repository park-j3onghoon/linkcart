package com.linkcart.application.port

import com.linkcart.domain.entity.Product

/** Phase 2용 — 현재는 인터페이스만 정의 */
interface ProductRepository {
    fun save(product: Product)
    fun findByUrl(url: String): Product?
}
