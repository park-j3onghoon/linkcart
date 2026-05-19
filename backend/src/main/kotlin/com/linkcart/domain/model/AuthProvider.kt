package com.linkcart.domain.model

import com.fasterxml.jackson.annotation.JsonValue

/**
 * 도메인 enum이 wire format을 자체 선언한다(@JsonValue).
 * jackson-annotations는 declarative dependency로만 사용하며 도메인 로직에 침투하지 않는다.
 * 별도 Jackson Module로 분리하는 대안은 WebMvcTest 등 slice test에서 Configuration 자동 로드가
 * 안 되어 모든 테스트에 @Import 추가 부담이 생기므로 채택하지 않는다.
 */
enum class AuthProvider {
    GOOGLE;

    @JsonValue
    fun toJson(): String = name.lowercase()
}
