package com.linkcart.domain.port

import com.linkcart.domain.model.ParseResult

interface ProductParser {
    fun parse(url: String): ParseResult
}
