package com.linkcart.domain.model

import com.fasterxml.jackson.annotation.JsonValue

/**
 * @JsonValue를 도메인에 둔 이유: Jackson Module로 분리하면 WebMvcTest slice가 자동 로드 못해
 * 모든 컨트롤러 테스트에 @Import를 달아야 함. jackson-annotations는 declarative 의존이라 허용.
 */
enum class AuthProvider {
    GOOGLE;

    @JsonValue
    fun toJson(): String = name.lowercase()
}
