package com.linkcart.domain.vo

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MoneyTest {

    @Test
    fun `rejects negative amount`() {
        assertThrows<IllegalArgumentException> { Money(amount = -1, currency = "KRW") }
    }

    @Test
    fun `rejects currency that is not 3 chars`() {
        assertThrows<IllegalArgumentException> { Money(amount = 100, currency = "KR") }
        assertThrows<IllegalArgumentException> { Money(amount = 100, currency = "KRWX") }
    }

    @Test
    fun `rejects currency containing lower-case letters`() {
        assertThrows<IllegalArgumentException> { Money(amount = 100, currency = "krw") }
    }
}
