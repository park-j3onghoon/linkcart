package com.linkcart.application.port

import com.linkcart.application.dto.ParseResult

interface ProductParser {
    fun canParse(url: String): Boolean
    fun parse(url: String): ParseResult
}
