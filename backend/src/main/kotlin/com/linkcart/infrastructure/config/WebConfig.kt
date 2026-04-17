package com.linkcart.infrastructure.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Duration

@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins("http://localhost:3000")
            .allowedMethods("GET", "POST", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
    }

    @Bean("imageProxyRestTemplate")
    fun imageProxyRestTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate {
        return restTemplateBuilder
            .connectTimeout(Duration.ofSeconds(3))
            .readTimeout(Duration.ofSeconds(3))
            .build()
    }
}
