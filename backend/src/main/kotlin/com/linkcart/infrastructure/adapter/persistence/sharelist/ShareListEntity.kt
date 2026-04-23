package com.linkcart.infrastructure.adapter.persistence.sharelist

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "share_lists")
class ShareListEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "owner_id", nullable = false, updatable = false)
    val ownerId: Long,

    @Column(nullable = false, length = 64, updatable = false, unique = true)
    val token: String,

    @Column(length = 200)
    val title: String? = null,

    @Column(name = "expires_at")
    val expiresAt: Instant? = null,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    val createdAt: Instant? = null,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "share_list_id", nullable = false)
    @OrderBy("id ASC")
    val items: MutableList<ShareListItemEntity> = mutableListOf(),
)
