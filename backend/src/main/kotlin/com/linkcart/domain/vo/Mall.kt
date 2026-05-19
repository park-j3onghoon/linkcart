package com.linkcart.domain.vo

import com.fasterxml.jackson.annotation.JsonValue

enum class Mall {
    COUPANG,
    ELEVENST,
    GENERIC;

    @JsonValue
    fun toJson(): String = name.lowercase()
}
