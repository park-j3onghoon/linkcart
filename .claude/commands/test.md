# 테스트 실행

전체 또는 개별 테스트를 실행합니다.

## 전체 테스트
```bash
npm run test                    # Turborepo로 전체
```

## 백엔드
```bash
cd backend && source .venv/bin/activate
pytest                          # 전체
pytest tests/unit/              # 단위 테스트만
pytest tests/integration/       # 통합 테스트만
pytest --cov=app                # 커버리지 포함
```

## 프론트엔드
```bash
cd apps/web && npm run test     # 웹 컴포넌트 테스트
cd apps/mobile && npm run test  # 모바일 컴포넌트 테스트
```
