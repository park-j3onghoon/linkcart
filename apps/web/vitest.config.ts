import { configDefaults, defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/test/setup.ts"],
    exclude: [...configDefaults.exclude, "e2e/**", ".next/**"],
    coverage: {
      // 페이지·layout은 Playwright e2e 영역, 빌드 설정 파일은 측정 제외
      exclude: [
        ...(configDefaults.coverage?.exclude ?? []),
        "**/*.config.{ts,mjs,js}",
        "src/app/**/page.tsx",
        "src/app/**/layout.tsx",
        "e2e/**",
        "coverage/**",
      ],
    },
  },
});
