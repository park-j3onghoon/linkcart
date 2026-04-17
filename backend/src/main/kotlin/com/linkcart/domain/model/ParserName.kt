package com.linkcart.domain.model

import com.fasterxml.jackson.annotation.JsonValue

enum class ParserName(val code: String) {
    OG("og"),
    COUPANG("coupang-api"),
    ELEVENST("11st-api");

    @JsonValue
    fun toJson(): String = code
}
