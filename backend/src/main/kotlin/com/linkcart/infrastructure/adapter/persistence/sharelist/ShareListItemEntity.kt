package com.linkcart.infrastructure.adapter.persistence.sharelist

import com.linkcart.domain.vo.Mall
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "share_list_items")
class ShareListItemEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 500)
    val name: String,

    @Column(name = "price_amount", nullable = false)
    val priceAmount: Long,

    @Column(name = "price_currency", nullable = false, length = 3)
    val priceCurrency: String,

    @Column(name = "image_url", length = 1000)
    val imageUrl: String? = null,

    @Column(name = "source_url", nullable = false, length = 2000)
    val sourceUrl: String,

    @Column(nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    val mall: Mall,
)
