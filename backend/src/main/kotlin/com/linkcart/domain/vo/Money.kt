package com.linkcart.domain.vo

data class Money(
    val amount: Long,
    val currency: String = DEFAULT_CURRENCY,
) {
    init {
        require(amount >= 0) { "Money.amount는 0 이상이어야 합니다 (입력: $amount)" }
        require(currency.length == 3 && currency.all { it.isUpperCase() }) {
            "Money.currency는 3자리 대문자 ISO 4217 코드여야 합니다 (입력: '$currency')"
        }
    }

    companion object {
        const val DEFAULT_CURRENCY = "KRW"
    }
}
