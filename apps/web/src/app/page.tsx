"use client";

import { AuthBar } from "../components/AuthBar";
import { ParseProgress } from "../components/ParseProgress";
import { ProductList } from "../components/ProductList";
import { UrlInput } from "../components/UrlInput";
import { useAuth } from "../hooks/useAuth";
import { useProducts } from "../hooks/useProducts";

export default function Home() {
  const {
    buildImageSrc,
    feedback,
    isHydrated,
    isWorking,
    phase,
    products,
    submitUrl,
  } = useProducts();
  const auth = useAuth();
  const lastParserUsed = products[0]?.parserUsed ?? "대기 중";

  return (
    <div className="relative min-h-screen overflow-hidden">
      <div className="absolute inset-x-0 top-[-8rem] h-72 bg-[radial-gradient(circle_at_top,_rgba(103,232,249,0.35),_transparent_58%)]" />
      <div className="absolute right-[-8rem] top-32 h-80 w-80 rounded-full bg-[radial-gradient(circle,_rgba(56,189,248,0.18),_transparent_70%)] blur-3xl" />
      <div className="absolute left-[-6rem] top-[28rem] h-72 w-72 rounded-full bg-[radial-gradient(circle,_rgba(244,114,182,0.12),_transparent_70%)] blur-3xl" />

      <main className="relative mx-auto flex w-full max-w-6xl flex-col gap-6 px-5 py-8 sm:px-8 lg:px-10 lg:py-12">
        <div className="flex items-center justify-end">
          <AuthBar
            isHydrated={auth.isHydrated}
            user={auth.user}
            onLogin={auth.login}
            onLogout={auth.logout}
          />
        </div>
        <section className="grid gap-6 lg:grid-cols-[1.15fr_0.85fr]">
          <div className="rounded-[32px] border border-white/65 bg-white/80 p-8 shadow-[0_28px_90px_rgba(15,23,42,0.1)] backdrop-blur">
            <p className="text-sm font-medium uppercase tracking-[0.35em] text-sky-700">
              Linkcart Web
            </p>
            <h1 className="mt-4 max-w-3xl text-4xl font-semibold tracking-tight text-slate-950 sm:text-5xl">
              상품 링크 하나로, 파싱 결과와 저장 상태를 같은 화면에서 바로 확인합니다.
            </h1>
            <p className="mt-5 max-w-2xl text-base leading-8 text-slate-600 sm:text-lg">
              API 응답 타입은 OpenAPI에서 생성하고, 웹 UI는 로컬 저장소를 기준으로 결과를 이어받습니다.
              브라우저를 새로고침해도 최근 카드가 그대로 복원됩니다.
            </p>

            <div className="mt-8 grid gap-4 sm:grid-cols-3">
              <div className="rounded-[24px] bg-slate-950 px-5 py-4 text-white">
                <p className="text-xs font-medium uppercase tracking-[0.28em] text-cyan-300">
                  Stored
                </p>
                <p className="mt-3 text-3xl font-semibold">{products.length}</p>
                <p className="mt-2 text-sm text-slate-300">브라우저에 저장된 상품 카드 수</p>
              </div>
              <div className="rounded-[24px] bg-sky-50 px-5 py-4">
                <p className="text-xs font-medium uppercase tracking-[0.28em] text-sky-700">
                  Last Parser
                </p>
                <p className="mt-3 text-2xl font-semibold text-slate-950">{lastParserUsed}</p>
                <p className="mt-2 text-sm text-slate-600">최근 성공 응답에서 사용된 파서</p>
              </div>
              <div className="rounded-[24px] bg-emerald-50 px-5 py-4">
                <p className="text-xs font-medium uppercase tracking-[0.28em] text-emerald-700">
                  Storage
                </p>
                <p className="mt-3 text-2xl font-semibold text-slate-950">
                  {isHydrated ? "복원 완료" : "복원 중"}
                </p>
                <p className="mt-2 text-sm text-slate-600">초기 진입 시 localStorage에서 데이터 로딩</p>
              </div>
            </div>
          </div>

          <div className="rounded-[32px] bg-slate-950 p-6 shadow-[0_28px_90px_rgba(15,23,42,0.18)]">
            <UrlInput
              feedback={feedback}
              isHydrated={isHydrated}
              isWorking={isWorking}
              onSubmit={submitUrl}
            />
            <div className="mt-6">
              <ParseProgress phase={phase} />
            </div>
          </div>
        </section>

        <ProductList
          buildImageSrc={buildImageSrc}
          isHydrated={isHydrated}
          products={products}
        />
      </main>
    </div>
  );
}
