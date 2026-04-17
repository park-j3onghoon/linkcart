package com.linkcart.infrastructure.adapter.persistence.userproduct

import com.linkcart.domain.model.ParserName
import com.linkcart.domain.vo.Mall
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "user_products")
class UserProductEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: Long,

    @Column(nullable = false, length = 500)
    val name: String,

    @Column(name = "price_amount", nullable = false)
    val priceAmount: Long,

    @Column(name = "price_currency", nullable = false, length = 3)
    val priceCurrency: String,

    @Column(name = "image_url", length = 1000)
    val imageUrl: String? = null,

    @Column(name = "source_url", nullable = false, length = 2000, updatable = false)
    val sourceUrl: String,

    @Column(nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    val mall: Mall,

    @Column(name = "parser_used", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    val parserUsed: ParserName,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    val createdAt: Instant? = null,
)
