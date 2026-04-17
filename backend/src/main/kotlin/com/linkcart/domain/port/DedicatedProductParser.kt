package com.linkcart.domain.port

interface DedicatedProductParser : ProductParser {
    fun canParse(url: String): Boolean
}
