package com.linkcart.domain.model

import com.fasterxml.jackson.annotation.JsonValue

enum class AuthProvider {
    GOOGLE;

    @JsonValue
    fun toJson(): String = name.lowercase()
}
