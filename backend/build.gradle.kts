plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	kotlin("plugin.jpa") version "1.9.25"
	id("org.springframework.boot") version "3.5.3"
	id("io.spring.dependency-management") version "1.1.7"
	jacoco
}

group = "com.linkcart"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")

	// JWT (Access token)
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

	// Google ID Token 검증
	implementation("com.google.api-client:google-api-client:2.8.0")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// DB
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")
	runtimeOnly("org.postgresql:postgresql")

	// OG 태그 파싱 (폴백)
	implementation("org.jsoup:jsoup:1.18.3")

	// TTL 캐시
	implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

	// OpenAPI 문서 자동 생성
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:postgresql")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("com.h2database:h2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

jacoco {
	toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		html.required.set(true)
		xml.required.set(true)
	}
	classDirectories.setFrom(
		files(classDirectories.files.map {
			fileTree(it) {
				// 프레임워크 글루·생성 코드는 측정 제외 (단위 테스트로 의미 있게 잡히지 않음).
				// 통합 테스트가 필요한 영역은 Testcontainers 기반으로 별도 진행.
				exclude(
					"com/linkcart/LinkcartBackendApplication*",
					"com/linkcart/**/dto/**",
					"com/linkcart/**/config/**",
					"com/linkcart/infrastructure/adapter/persistence/**/*Entity*",
					"com/linkcart/infrastructure/adapter/persistence/**/*JpaRepository*",
					"com/linkcart/infrastructure/adapter/persistence/**/*Mappers*",
					"com/linkcart/infrastructure/adapter/persistence/**/*RepositoryAdapter*",
				)
			}
		}),
	)
}

tasks.withType<Test> {
	useJUnitPlatform()
	finalizedBy(tasks.jacocoTestReport)
	// Rancher Desktop 사용 시 Testcontainers가 Docker socket을 자동 감지하도록 보조.
	// DOCKER_HOST가 이미 설정되어 있으면 그대로 사용, 아니면 ~/.rd/docker.sock 우선 탐지.
	val existingDockerHost = System.getenv("DOCKER_HOST").orEmpty()
	if (existingDockerHost.isBlank()) {
		val rancherSocket = file("${System.getProperty("user.home")}/.rd/docker.sock")
		if (rancherSocket.exists()) {
			environment("DOCKER_HOST", "unix://${rancherSocket.absolutePath}")
			println("[test] DOCKER_HOST auto-set to unix://${rancherSocket.absolutePath}")
		}
	} else {
		environment("DOCKER_HOST", existingDockerHost)
	}
}
