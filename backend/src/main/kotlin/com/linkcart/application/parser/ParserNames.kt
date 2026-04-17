package com.linkcart.application.parser

object ParserNames {
    /** Open Graph protocol 기반 범용 폴백 파서 */
    const val OG = "og"
    const val COUPANG = "coupang-api"
    const val ELEVENST = "11st-api"

    // 후속 PR 후보: String → enum class ParserName 전환 (ParseResult.parserUsed 시그니처 변경 포함).
}
