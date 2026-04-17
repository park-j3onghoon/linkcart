"use client";

import { validateUrl } from "@linkcart/shared";
import { useState } from "react";
import type { Feedback } from "../hooks/useProducts";

type UrlInputProps = {
  feedback: Feedback | null;
  isHydrated: boolean;
  isWorking: boolean;
  onSubmit: (url: string) => Promise<boolean> | boolean;
};

const noticeStyles: Record<NonNullable<Feedback>["kind"], string> = {
  error: "border-rose-200 bg-rose-50 text-rose-700",
  warning: "border-amber-200 bg-amber-50 text-amber-700",
  success: "border-emerald-200 bg-emerald-50 text-emerald-700",
};

export function UrlInput({
  feedback,
  isHydrated,
  isWorking,
  onSubmit,
}: UrlInputProps) {
  const [url, setUrl] = useState("");
  const [localError, setLocalError] = useState<string | null>(null);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const normalizedUrl = url.trim();
    const validation = validateUrl(normalizedUrl);

    if (!validation.valid) {
      setLocalError(validation.error ?? "올바른 URL을 입력해주세요");
      return;
    }

    setLocalError(null);
    const didSave = await onSubmit(normalizedUrl);
    if (didSave) {
      setUrl("");
    }
  }

  const activeNotice = localError
    ? { kind: "error" as const, text: localError }
    : feedback;

  return (
    <div className="space-y-4">
      <div className="space-y-2">
        <p className="text-sm font-medium uppercase tracking-[0.28em] text-cyan-300">
          Parser Input
        </p>
        <h2 className="text-2xl font-semibold tracking-tight text-white">
          쿠팡, 11번가, 일반 상품 링크를 바로 파싱합니다.
        </h2>
        <p className="text-sm leading-6 text-slate-300">
          백엔드 파서 응답을 로컬에 보관해서 새로고침 후에도 같은 리스트를 이어서 볼 수 있습니다.
        </p>
      </div>

      <form className="space-y-3" onSubmit={handleSubmit}>
        <label className="block space-y-2">
          <span className="text-sm font-medium text-slate-200">상품 URL</span>
          <input
            className="w-full rounded-2xl border border-white/15 bg-white/10 px-4 py-3 text-sm text-white outline-none transition focus:border-cyan-300 focus:bg-white/14"
            placeholder="https://www.coupang.com/vp/products/..."
            value={url}
            onChange={(event) => {
              setUrl(event.target.value);
              if (localError) {
                setLocalError(null);
              }
            }}
            disabled={!isHydrated || isWorking}
          />
        </label>

        <button
          type="submit"
          className="inline-flex h-12 w-full items-center justify-center rounded-2xl bg-cyan-300 px-4 text-sm font-semibold text-slate-950 transition hover:bg-cyan-200 disabled:cursor-not-allowed disabled:bg-slate-500 disabled:text-slate-200"
          disabled={!isHydrated || isWorking}
        >
          {isWorking ? "파싱 중..." : isHydrated ? "상품 추가" : "저장소 준비 중..."}
        </button>
      </form>

      <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm leading-6 text-slate-300">
        같은 링크를 다시 넣으면 중복 경고만 보여주고, 성공한 결과는 브라우저 저장소에 바로 보관합니다.
      </div>

      {activeNotice ? (
        <div
          className={`rounded-2xl border px-4 py-3 text-sm font-medium ${noticeStyles[activeNotice.kind]}`}
        >
          {activeNotice.text}
        </div>
      ) : null}
    </div>
  );
}
