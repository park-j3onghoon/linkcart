package com.linkcart.presentation.api

import com.linkcart.infrastructure.config.WebConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.ComponentScan.Filter
import org.springframework.context.annotation.FilterType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    controllers = [HealthController::class],
    excludeFilters = [Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [WebConfig::class])],
)
@AutoConfigureMockMvc(addFilters = false)
class HealthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `health returns ok`() {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ok"))
    }
}
