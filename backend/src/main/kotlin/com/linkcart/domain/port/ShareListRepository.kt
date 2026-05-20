package com.linkcart.domain.port

import com.linkcart.domain.model.ShareList

interface ShareListRepository {
    fun save(shareList: ShareList): ShareList
    fun findById(id: Long): ShareList?
    fun findByToken(token: String): ShareList?
    fun deleteById(id: Long)
}
