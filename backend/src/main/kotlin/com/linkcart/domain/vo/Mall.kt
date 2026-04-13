package com.linkcart.domain.vo

import com.fasterxml.jackson.annotation.JsonValue

enum class Mall(val displayName: String) {
    COUPANG("쿠팡"),
    ELEVENST("11번가"),
    GENERIC("기타");

    @JsonValue
    fun toJson(): String = name.lowercase()
}
