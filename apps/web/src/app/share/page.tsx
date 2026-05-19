"use client";

import { createApiClient, type ShareList } from "@linkcart/shared";
import { useEffect, useMemo, useState } from "react";
import { ShareItemCard } from "../../components/ShareItemCard";
import { BACKEND_URL } from "../../lib/authConfig";

type ViewState =
  | { kind: "loading" }
  | { kind: "missing-token" }
  | { kind: "not-found" }
  | { kind: "error"; message: string }
  | { kind: "loaded"; shareList: ShareList };

function formatDate(iso: string | null | undefined): string | null {
  if (!iso) return null;
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return null;
  return new Intl.DateTimeFormat("ko-KR", { dateStyle: "long" }).format(date);
}

/**
 * AIP-131 보안: ShareList token은 URL path/query에 두지 않는다.
 * 토큰은 URL fragment(#)로 전달되어 서버 access log·Referer 헤더로 누설되지 않는다.
 * 페이지 로드 직후 history.replaceState로 fragment를 제거하여 주소 표시줄에도 남기지 않는다.
 */
export default function SharePage() {
  const [state, setState] = useState<ViewState>({ kind: "loading" });
  const api = useMemo(() => createApiClient(BACKEND_URL), []);

  useEffect(() => {
    let cancelled = false;

    const token = window.location.hash.replace(/^#/, "").trim();
    if (!token) {
      setState({ kind: "missing-token" });
      return;
    }
    // 주소 표시줄·history에서 fragment를 즉시 제거 (shoulder-surfing/스크린샷 대비)
    window.history.replaceState(null, "", window.location.pathname);

    api.lookupShareListByToken(token).then((result) => {
      if (cancelled) return;
      if (result.ok) {
        setState({ kind: "loaded", shareList: result.data });
      } else if (result.error.code === "NOT_FOUND") {
        setState({ kind: "not-found" });
      } else {
        setState({ kind: "error", message: result.error.message });
      }
    });

    return () => {
      cancelled = true;
    };
  }, [api]);

  if (state.kind === "loading") {
    return (
      <main className="mx-auto flex w-full max-w-6xl items-center justify-center px-5 py-20">
        <p className="text-sm text-slate-500">공유 리스트를 불러오는 중…</p>
      </main>
    );
  }

  if (state.kind === "missing-token") {
    return (
      <main className="mx-auto flex w-full max-w-6xl flex-col items-center gap-4 px-5 py-20 text-center">
        <h1 className="text-2xl font-semibold text-slate-950">공유 링크가 올바르지 않습니다</h1>
        <p className="text-sm text-slate-600">
          공유 받은 링크의 # 뒤에 토큰이 포함되어 있어야 합니다.
        </p>
      </main>
    );
  }

  if (state.kind === "not-found") {
    return (
      <main className="mx-auto flex w-full max-w-6xl flex-col items-center gap-4 px-5 py-20 text-center">
        <h1 className="text-2xl font-semibold text-slate-950">공유 리스트를 찾을 수 없습니다</h1>
        <p className="text-sm text-slate-600">
          링크가 만료되었거나 존재하지 않습니다.
        </p>
      </main>
    );
  }

  if (state.kind === "error") {
    return (
      <main className="mx-auto flex w-full max-w-6xl flex-col items-center gap-4 px-5 py-20 text-center">
        <h1 className="text-2xl font-semibold text-slate-950">불러오기에 실패했습니다</h1>
        <p className="text-sm text-slate-600">{state.message}</p>
      </main>
    );
  }

  const { shareList } = state;
  const createTime = formatDate(shareList.createTime);

  return (
    <main className="mx-auto flex w-full max-w-6xl flex-col gap-8 px-5 py-10 sm:px-8 lg:px-10 lg:py-16">
      <header className="rounded-[32px] border border-white/65 bg-white/80 p-8 shadow-[0_28px_90px_rgba(15,23,42,0.1)] backdrop-blur">
        <p className="text-sm font-medium uppercase tracking-[0.35em] text-sky-700">
          Shared List
        </p>
        <h1 className="mt-4 text-4xl font-semibold tracking-tight text-slate-950 sm:text-5xl">
          {shareList.title ?? "공유된 상품 리스트"}
        </h1>
        <p className="mt-4 text-sm text-slate-600">
          총 {shareList.items.length}개 상품
          {createTime ? ` · ${createTime} 공유` : null}
        </p>
      </header>

      {shareList.items.length === 0 ? (
        <p className="rounded-[28px] border border-slate-200 bg-white p-8 text-center text-slate-500">
          공유된 상품이 없습니다.
        </p>
      ) : (
        <section
          className="grid gap-6 md:grid-cols-2 lg:grid-cols-3"
          data-testid="share-item-list"
        >
          {shareList.items.map((item) => (
            <ShareItemCard
              key={item.name}
              item={item}
              imageSrc={item.imageUrl ? api.imageProxyUrl(item.imageUrl) : null}
            />
          ))}
        </section>
      )}
    </main>
  );
}
