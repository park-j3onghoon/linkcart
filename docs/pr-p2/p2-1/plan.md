# PR-P2-1: DB 인프라 + User 도메인

## 목적

Phase 2의 **영속성 기반** 구축. PostgreSQL + Spring Data JPA + Flyway를 도입하고, `User` 도메인/엔티티/Repository를 완성한다. API 엔드포인트는 이 PR에 없음 (P2-2부터 소비).

## 브랜치

`feature/pr-p2-1-db-user-domain` (origin/main 기준)

## 변경 상세

### 1. Docker Compose (신규)

```yaml
# docker-compose.yml (루트)
services:
  postgres:
    image: postgres:16
    container_name: linkcart-postgres
    environment:
      POSTGRES_USER: ${POSTGRES_USER:?must be set}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?must be set}
      POSTGRES_DB: ${POSTGRES_DB:?must be set}
    ports:
      - "5432:5432"
    volumes:
      - linkcart-postgres-data:/var/lib/postgresql/data

volumes:
  linkcart-postgres-data:
```

### 1-1. `.env.example` + `.gitignore`

```bash
# .env.example (git-tracked)
POSTGRES_USER=linkcart
POSTGRES_PASSWORD=change-me-local
POSTGRES_DB=linkcart
LINKCART_DB_URL=jdbc:postgresql://localhost:5432/linkcart
LINKCART_DB_USERNAME=linkcart
LINKCART_DB_PASSWORD=change-me-local
```

```gitignore
# .gitignore (add)
.env
```

로컬 개발 시 `cp .env.example .env && docker compose up -d postgres`.

### 2. `backend/build.gradle.kts` 의존성 추가

```kotlin
plugins {
    // 기존 유지
    kotlin("plugin.jpa") version "1.9.25"
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

dependencies {
    // 기존 유지
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}
```

### 3. `application.yml` DB 설정

```yaml
spring:
  # 기존 유지
  datasource:
    url: ${LINKCART_DB_URL}
    username: ${LINKCART_DB_USERNAME}
    password: ${LINKCART_DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate  # Flyway가 스키마 관리, JPA는 검증만
    # format_sql, show-sql은 application-local.yml로 분리 (prod 로그에 PII/쿼리 노출 방지)
    open-in-view: false
    # 주의: open-in-view: false로 Controller에서 lazy loading 불가.
    # P2-4에서 @OneToMany 양방향 관계 도입 시 Service 레이어에서 fetch join 또는 DTO projection 명시적으로 설계 필요.
  flyway:
    enabled: true
    locations: classpath:db/migration
```

**default 값 제거**: env 미설정 시 fail-fast. 로컬은 `.env` + `export $(cat .env | xargs)` 또는 bootRun 시 인라인.

### 4. Flyway Migration V1

```sql
-- backend/src/main/resources/db/migration/V1__create_users.sql

CREATE OR REPLACE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    provider VARCHAR(32) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    avatar_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_provider_user UNIQUE (provider, provider_user_id)
);

CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION set_updated_at();
```

**변경점**:
- `BIGSERIAL` → `BIGINT GENERATED ALWAYS AS IDENTITY` (SQL 표준, id 수동 주입 방지)
- `idx_users_email` 제거 (Phase 2 내 email 단독 쿼리 없음 — YAGNI + enumeration 방지)
- `updated_at` DB trigger 추가 (raw SQL 경로도 커버)

### 5. 도메인: `User` 모델

```kotlin
// backend/src/main/kotlin/com/linkcart/domain/model/User.kt
data class User(
    val id: Long? = null,  // null = 미저장
    val provider: AuthProvider,
    val providerUserId: String,
    val email: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val createdAt: Instant? = null,  // DB DEFAULT NOW()로 채워짐 (saveAndFlush 후 non-null)
    val updatedAt: Instant? = null,
)
```

### 6. `AuthProvider` enum

```kotlin
// backend/src/main/kotlin/com/linkcart/domain/model/AuthProvider.kt
enum class AuthProvider {
    GOOGLE;
    // 후속: KAKAO, APPLE

    @JsonValue
    fun toJson(): String = name.lowercase()
}
```

- Mall / ParserName 선례 동일 패턴.
- **DB 저장값은 enum name (`"GOOGLE"`)** — `@Enumerated(EnumType.STRING)` 관례.
- JSON 외부 노출은 `"google"` (lowercase).
- Converter 불필요 (plan-review Issue 2 반영).

### 7. 도메인 포트: `UserRepository`

```kotlin
// backend/src/main/kotlin/com/linkcart/domain/port/UserRepository.kt
interface UserRepository {
    fun findByProviderAndProviderUserId(provider: AuthProvider, providerUserId: String): User?
    fun save(user: User): User
    fun findById(id: Long): User?
}
```

### 8. 인프라 어댑터

```
infrastructure/adapter/persistence/user/
├─ UserEntity.kt              (JPA 엔티티)
├─ UserJpaRepository.kt       (Spring Data JPA interface)
├─ UserRepositoryAdapter.kt   (포트 구현, @Component)
└─ UserMappers.kt             (top-level internal toDomain/toEntity)
```

#### UserEntity.kt
```kotlin
@Entity
@Table(name = "users")
class UserEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    val provider: AuthProvider,

    @Column(name = "provider_user_id", nullable = false, length = 255)
    val providerUserId: String,

    @Column(nullable = false, length = 255)
    val email: String,

    @Column(name = "display_name", length = 100)
    val displayName: String? = null,

    @Column(name = "avatar_url", length = 500)
    val avatarUrl: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    val createdAt: Instant? = null,  // DB DEFAULT NOW() + trigger

    @Column(name = "updated_at", nullable = false, updatable = false, insertable = false)
    val updatedAt: Instant? = null,  // DB trigger가 갱신
)
```

**변경점**:
- `AuthProviderConverter` 제거. `@Enumerated(EnumType.STRING)` 사용.
- `@PreUpdate` 빈 콜백 제거. DB trigger가 책임.
- `createdAt`/`updatedAt`은 `insertable = false, updatable = false` — JPA는 값 건드리지 않음. INSERT/UPDATE 후 `saveAndFlush`로 재조회.

#### UserJpaRepository.kt
```kotlin
interface UserJpaRepository : JpaRepository<UserEntity, Long> {
    fun findByProviderAndProviderUserId(provider: AuthProvider, providerUserId: String): UserEntity?
}
```

#### UserMappers.kt (top-level internal 확장 함수)
```kotlin
// backend/src/main/kotlin/com/linkcart/infrastructure/adapter/persistence/user/UserMappers.kt
internal fun UserEntity.toDomain(): User = User(
    id = id, provider = provider, providerUserId = providerUserId,
    email = email, displayName = displayName, avatarUrl = avatarUrl,
    createdAt = createdAt, updatedAt = updatedAt,
)

internal fun User.toEntity(): UserEntity = UserEntity(
    id = id, provider = provider, providerUserId = providerUserId,
    email = email, displayName = displayName, avatarUrl = avatarUrl,
    createdAt = createdAt, updatedAt = updatedAt,
)
```

#### UserRepositoryAdapter.kt
```kotlin
@Component
class UserRepositoryAdapter(
    private val userJpaRepository: UserJpaRepository,
) : UserRepository {

    override fun findByProviderAndProviderUserId(provider: AuthProvider, providerUserId: String): User? =
        userJpaRepository.findByProviderAndProviderUserId(provider, providerUserId)?.toDomain()

    override fun save(user: User): User =
        userJpaRepository.saveAndFlush(user.toEntity()).toDomain()

    override fun findById(id: Long): User? =
        userJpaRepository.findById(id).orElse(null)?.toDomain()
}
```

### 9. 테스트 — Testcontainers

```kotlin
// backend/src/test/kotlin/com/linkcart/infrastructure/adapter/persistence/user/UserRepositoryAdapterIntegrationTest.kt

@Testcontainers
@SpringBootTest
class UserRepositoryAdapterIntegrationTest {
    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16")
            .withDatabaseName("linkcart_test")

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }

    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var userJpaRepository: UserJpaRepository

    @BeforeEach
    fun cleanup() {
        userJpaRepository.deleteAll()  // 테스트 간 상태 격리
    }

    @Test fun `save then findById returns same user`() { ... }
    @Test fun `findById returns null when not found`() { ... }
    @Test fun `findByProviderAndProviderUserId returns null when not found`() { ... }
    @Test fun `duplicate provider + providerUserId violates unique constraint`() { ... }
    @Test fun `saved user has createdAt and updatedAt populated by DB`() { ... }
}
```

**5개 케이스**: save/findById happy path, findById not-found, findByProvider... not-found, unique 위반, 타임스탬프 자동 갱신.

**커버리지 메모**:
- `save` update 경로는 P2-2 `FindOrCreateUser` UseCase 통합 테스트에서 자연스럽게 커버 (의도적 지연).
- Flyway + `ddl-auto: validate` 실행은 `@SpringBootTest` 컨텍스트 startup으로 암묵 검증.
- `AuthProvider` enum 저장/로드 round-trip은 `save then findById returns same user`의 `provider` 필드 assert로 커버.

### 10. 기존 테스트 회귀 확인 (IRON RULE)

JPA auto-config 도입이 기존 `@WebMvcTest` 기반 컨트롤러 테스트(`ProductControllerTest`, `ImageProxyControllerTest`)에 영향을 줄 수 있음.

**검증 절차**:
1. 구현 완료 후 `./gradlew test` 전체 실행 → 기존 Phase 1 테스트 전부 PASS 확인
2. 실패 시 `@WebMvcTest(..., excludeAutoConfiguration = [DataSourceAutoConfiguration::class])` 추가 검토

## NOT in scope

- User 찾거나 만드는 UseCase (FindOrCreateUser) — P2-2에서 OAuth 콜백과 함께
- REST API 엔드포인트 — P2-2부터
- RefreshToken 테이블 — P2-3
- UserProduct 테이블 — P2-4
- `save` update 경로 테스트 — P2-2 UseCase 테스트로 커버
- `format_sql`/`show-sql` 로컬 프로파일 분리 — P2-2에서 환경별 yml 분리 시 같이 (지금은 application.yml에 설정 안 함)
- `idx_users_email` — Phase 3 관리자 기능 시점

## TDD 순서

1. `.env.example` + `.gitignore`
2. Docker compose + Gradle 의존성 + application.yml → `./gradlew build` 통과 (JPA 초기화)
3. Flyway V1 마이그레이션
4. UserEntity, UserJpaRepository, AuthProvider enum, User 도메인, UserRepository 포트
5. UserMappers (top-level internal)
6. UserRepositoryAdapter
7. Testcontainers 설정 + 5개 통합 테스트
8. `./gradlew test` 통과 (Phase 1 기존 테스트 회귀 포함)
9. `./gradlew build` 통과

## 파일 변경

### 신규 (+11)
- `docker-compose.yml`
- `.env.example`
- `backend/src/main/resources/db/migration/V1__create_users.sql`
- `backend/src/main/kotlin/com/linkcart/domain/model/AuthProvider.kt`
- `backend/src/main/kotlin/com/linkcart/domain/model/User.kt`
- `backend/src/main/kotlin/com/linkcart/domain/port/UserRepository.kt`
- `backend/src/main/kotlin/com/linkcart/infrastructure/adapter/persistence/user/UserEntity.kt`
- `backend/src/main/kotlin/com/linkcart/infrastructure/adapter/persistence/user/UserJpaRepository.kt`
- `backend/src/main/kotlin/com/linkcart/infrastructure/adapter/persistence/user/UserRepositoryAdapter.kt`
- `backend/src/main/kotlin/com/linkcart/infrastructure/adapter/persistence/user/UserMappers.kt`
- `backend/src/test/kotlin/com/linkcart/infrastructure/adapter/persistence/user/UserRepositoryAdapterIntegrationTest.kt`

### 수정
- `.gitignore` (`.env` 추가)
- `backend/build.gradle.kts` (JPA/Flyway/PG/Testcontainers + kotlin-jpa plugin + allOpen)
- `backend/src/main/resources/application.yml` (datasource + jpa + flyway)

예상 diff: ~380줄

## 검증

```bash
# 로컬 DB 시작
cp .env.example .env
docker compose up -d postgres

# 환경 변수 로드 (또는 direnv/dotenv)
export $(grep -v '^#' .env | xargs)

# 빌드 + 전체 테스트 (Testcontainers가 별도 Postgres 컨테이너 띄움)
cd backend && ./gradlew test
./gradlew build

# 앱 띄워서 startup 확인 (Flyway 마이그레이션 실행됨)
./gradlew bootRun
# 로그에서 "Successfully applied 1 migration" 확인

# 테스트 실패 시 디버깅
# 1. UserEntity 컬럼명/타입과 V1 SQL 대조
# 2. @WebMvcTest 계열은 JPA auto-config exclude 검토
```

## 보안/운영 주의

- `LINKCART_DB_*` 환경변수 필수. 미설정 시 앱 부팅 실패 (fail-fast).
- `.env`는 git ignore. `.env.example`만 저장소에 포함.
- `ddl-auto: validate` → 프로덕션에서 JPA가 스키마 임의 수정 금지.
- `open-in-view: false` → Controller에서 lazy loading 방지. P2-4 관계 도입 시 명시적 fetch 전략 필요.
- Hibernate `format_sql`/`show-sql`은 기본 값 사용 (로깅 없음). 로컬 디버깅 시 개별 환경에서 활성화.

## plan-review 반영 요약 (BIG CHANGE)

- Dimensions: 5/6 active (Performance skip)
- Architecture: 4 이슈 (1 반영: open-in-view 경고, 3 유지)
- Data/Database: 4 이슈 (3 반영: IDENTITY, trigger, index 제거)
- Security: 4 이슈 (3 반영: default 제거, .env 분리, index 제거)
- Coding Standards: 4 이슈 (2 반영: PreUpdate 제거, top-level mapper)
- Test Coverage: 4 이슈 (4 반영: findById null, cleanup, update 이관 명시, 회귀 검증 명시)
- Critical gaps: 0
