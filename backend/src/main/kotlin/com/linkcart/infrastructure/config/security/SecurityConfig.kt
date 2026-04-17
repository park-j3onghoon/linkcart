package com.linkcart.infrastructure.config.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.linkcart.application.auth.port.AccessTokenIssuer
import com.linkcart.infrastructure.adapter.auth.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun jwtAuthenticationFilter(accessTokenIssuer: AccessTokenIssuer): JwtAuthenticationFilter =
        JwtAuthenticationFilter(accessTokenIssuer)

    @Bean
    fun jsonAuthenticationEntryPoint(objectMapper: ObjectMapper): JsonAuthenticationEntryPoint =
        JsonAuthenticationEntryPoint(objectMapper)

    @Bean
    fun filterChain(
        http: HttpSecurity,
        jwtAuthenticationFilter: JwtAuthenticationFilter,
        jsonAuthenticationEntryPoint: JsonAuthenticationEntryPoint,
    ): SecurityFilterChain = http
        .cors { /* WebConfig.addCorsMappings와 연동 */ }
        .csrf { it.disable() }
        .formLogin { it.disable() }
        .httpBasic { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            it.requestMatchers(
                "/api/v1/auth/oauth/**",
                "/api/v1/products/parse",
                "/api/v1/images/proxy",
                "/openapi.json",
                "/docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs/**",
                "/error",
            ).permitAll()
                .anyRequest().authenticated()
        }
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        .exceptionHandling { it.authenticationEntryPoint(jsonAuthenticationEntryPoint) }
        .build()
}
