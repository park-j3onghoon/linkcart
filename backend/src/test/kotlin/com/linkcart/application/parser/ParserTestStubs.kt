package com.linkcart.application.parser

import com.linkcart.domain.model.ParseResult
import com.linkcart.domain.port.DedicatedProductParser
import com.linkcart.domain.port.FallbackProductParser

internal fun dedicatedStub(
    canParse: (String) -> Boolean,
    parse: (String) -> ParseResult,
): DedicatedProductParser = object : DedicatedProductParser {
    override fun canParse(url: String): Boolean = canParse(url)
    override fun parse(url: String): ParseResult = parse(url)
}

internal fun fallbackStub(
    parse: (String) -> ParseResult,
): FallbackProductParser = object : FallbackProductParser {
    override fun parse(url: String): ParseResult = parse(url)
}
