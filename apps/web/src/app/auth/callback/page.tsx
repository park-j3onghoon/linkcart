"use client";

import {
  createAuthClient,
  createWebTokenStorage,
} from "@linkcart/shared";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import {
  BACKEND_URL,
  OAUTH_STATE_KEY,
  buildCallbackRedirectUri,
} from "../../../lib/authConfig";

type CallbackStatus =
  | { kind: "working" }
  | { kind: "error"; message: string };

export default function AuthCallbackPage() {
  const router = useRouter();
  const params = useSearchParams();
  const [status, setStatus] = useState<CallbackStatus>({ kind: "working" });

  useEffect(() => {
    const code = params.get("code");
    const receivedState = params.get("state");
    const savedState = sessionStorage.getItem(OAUTH_STATE_KEY);
    sessionStorage.removeItem(OAUTH_STATE_KEY);

    if (!code) {
      setStatus({ kind: "error", message: "인증 코드를 받지 못했습니다" });
      return;
    }
    if (!savedState || receivedState !== savedState) {
      setStatus({ kind: "error", message: "state가 일치하지 않습니다 (CSRF 방지)" });
      return;
    }

    const api = createAuthClient(BACKEND_URL);
    const storage = createWebTokenStorage(window.localStorage);
    const redirectUri = buildCallbackRedirectUri(window.location.origin);

    api.loginWithGoogle(code, redirectUri).then((result) => {
      if (!result.ok) {
        setStatus({ kind: "error", message: result.error.message });
        return;
      }
      storage.setTokens({
        accessToken: result.data.accessToken,
        refreshToken: result.data.refreshToken,
      });
      router.replace("/");
    });
  }, [params, router]);

  return (
    <main className="mx-auto flex min-h-screen max-w-xl flex-col items-center justify-center gap-4 px-6 text-center">
      {status.kind === "working" ? (
        <>
          <p className="text-sm font-medium uppercase tracking-[0.35em] text-sky-700">
            Signing in
          </p>
          <h1 className="text-2xl font-semibold text-slate-950">
            Google 로그인을 처리 중입니다…
          </h1>
        </>
      ) : (
        <>
          <p className="text-sm font-medium uppercase tracking-[0.35em] text-rose-600">
            로그인 실패
          </p>
          <h1 className="text-2xl font-semibold text-slate-950">{status.message}</h1>
          <button
            type="button"
            onClick={() => router.replace("/")}
            className="rounded-full bg-slate-950 px-5 py-2.5 text-sm font-semibold text-white hover:bg-slate-800"
          >
            홈으로 돌아가기
          </button>
        </>
      )}
    </main>
  );
}
