package com.linkcart

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@AutoConfigureMockMvc
@SpringBootTest
class LinkcartBackendApplicationTests(
	@Autowired private val mockMvc: MockMvc,
) {

	@Test
	fun contextLoads() {
	}

	@Test
	fun openApiUsesCamelCaseForParseResponse() {
		mockMvc.get("/openapi.json")
			.andExpect {
				status { isOk() }
				jsonPath("$.components.schemas.ParseResponse.properties.imageUrl") { exists() }
				jsonPath("$.components.schemas.ParseResponse.properties.sourceUrl") { exists() }
				jsonPath("$.components.schemas.ParseResponse.properties.parserUsed") { exists() }
				jsonPath("$.components.schemas.ParseResponse.properties.fallbackUsed") { exists() }
				jsonPath("$.components.schemas.ParseResponse.properties.image_url") { doesNotExist() }
			}
	}

}
