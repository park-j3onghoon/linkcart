# 테스트 실행

## 백엔드 (Kotlin/Spring Boot)
```bash
cd backend
./gradlew test                     # 전체 테스트
./gradlew test --tests "*.unit.*"  # 단위 테스트만
./gradlew test --tests "*.integration.*"  # 통합 테스트만
```

## 프론트엔드
```bash
cd apps/web && npm run test     # 웹 컴포넌트 테스트
cd apps/mobile && npm run test  # 모바일 컴포넌트 테스트
```

## 전체
```bash
npm run test                    # Turborepo로 전체 (프론트만)
```
