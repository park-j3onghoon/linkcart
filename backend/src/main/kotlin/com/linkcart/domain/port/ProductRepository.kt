package com.linkcart.domain.port

import com.linkcart.domain.model.Product

/** Phase 2용 — 현재는 인터페이스만 정의 */
interface ProductRepository {
    fun save(product: Product)
    fun findByUrl(url: String): Product?
}
