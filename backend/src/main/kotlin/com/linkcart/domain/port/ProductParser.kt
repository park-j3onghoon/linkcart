package com.linkcart.domain.port

import com.linkcart.domain.model.ParseResult

interface ProductParser {
    fun canParse(url: String): Boolean
    fun parse(url: String): ParseResult
}
