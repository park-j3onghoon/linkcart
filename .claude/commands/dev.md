# 개발 서버 실행

백엔드와 프론트엔드 개발 서버를 동시에 실행합니다.

## 실행 방법

1. 백엔드: `cd backend && source .venv/bin/activate && uvicorn main:app --reload`
2. 웹: `cd apps/web && npm run dev`
3. 모바일: `cd apps/mobile && npx expo start`

## 포트
- 백엔드 API: http://localhost:8000 (Swagger: http://localhost:8000/docs)
- 웹: http://localhost:3000
