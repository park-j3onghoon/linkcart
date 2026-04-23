"use client";

import type { AuthUser } from "@linkcart/shared";
import Image from "next/image";

type AuthBarProps = {
  isHydrated: boolean;
  user: AuthUser | null;
  onLogin: () => void;
  onLogout: () => void;
};

export function AuthBar({ isHydrated, user, onLogin, onLogout }: AuthBarProps) {
  if (!isHydrated) {
    return (
      <div
        data-testid="auth-bar-loading"
        className="h-10 w-32 animate-pulse rounded-full bg-slate-100"
      />
    );
  }

  if (!user) {
    return (
      <button
        type="button"
        onClick={onLogin}
        data-testid="login-button"
        className="inline-flex items-center gap-2 rounded-full bg-slate-950 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-slate-800"
      >
        Google로 로그인
      </button>
    );
  }

  return (
    <div
      data-testid="user-badge"
      className="inline-flex items-center gap-3 rounded-full border border-slate-200 bg-white/80 px-3 py-1.5 text-sm"
    >
      {user.avatar_url ? (
        <Image
          src={user.avatar_url}
          alt={`${user.display_name ?? user.email} 프로필`}
          width={28}
          height={28}
          className="rounded-full"
          unoptimized
        />
      ) : (
        <div className="flex h-7 w-7 items-center justify-center rounded-full bg-slate-200 text-xs font-semibold text-slate-600">
          {(user.display_name ?? user.email).charAt(0).toUpperCase()}
        </div>
      )}
      <span className="font-medium text-slate-800">
        {user.display_name ?? user.email}
      </span>
      <button
        type="button"
        onClick={onLogout}
        className="rounded-full border border-slate-300 px-3 py-1 text-xs font-semibold text-slate-700 transition hover:bg-slate-100"
      >
        로그아웃
      </button>
    </div>
  );
}
