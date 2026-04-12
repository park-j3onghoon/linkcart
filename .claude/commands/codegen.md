# OpenAPI 타입 코드 생성

백엔드 Spring Boot의 OpenAPI 스펙에서 TypeScript 타입을 자동 생성합니다.

## 사전 조건
백엔드 서버가 실행 중이어야 합니다: `cd backend && ./gradlew bootRun`

## 실행
```bash
cd packages/shared && npm run codegen
```

생성 파일: `packages/shared/src/types/api.gen.ts`
