"use client";

import {
  createAuthClient,
  createMemoryTokenStorage,
  createWebTokenStorage,
  type AuthUser,
  type TokenStorage,
} from "@linkcart/shared";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  BACKEND_URL,
  GOOGLE_CLIENT_ID,
  OAUTH_STATE_KEY,
  buildCallbackRedirectUri,
  buildGoogleAuthUrl,
} from "../lib/authConfig";

type AuthState = {
  user: AuthUser | null;
  isHydrated: boolean;
};

export function useAuth() {
  const [state, setState] = useState<AuthState>({ user: null, isHydrated: false });

  const storage = useMemo<TokenStorage>(
    () =>
      typeof window === "undefined"
        ? createMemoryTokenStorage()
        : createWebTokenStorage(window.localStorage),
    [],
  );

  const authClient = useMemo(() => createAuthClient(BACKEND_URL), []);

  useEffect(() => {
    const accessToken = storage.getAccessToken();
    if (!accessToken) {
      setState({ user: null, isHydrated: true });
      return;
    }
    let cancelled = false;
    authClient.getMe(accessToken).then((result) => {
      if (cancelled) return;
      if (result.ok) {
        setState({ user: result.data.user, isHydrated: true });
      } else {
        storage.clear();
        setState({ user: null, isHydrated: true });
      }
    });
    return () => {
      cancelled = true;
    };
  }, [authClient, storage]);

  const login = useCallback(() => {
    if (typeof window === "undefined") return;
    if (!GOOGLE_CLIENT_ID) {
      console.warn("[auth] NEXT_PUBLIC_GOOGLE_CLIENT_ID가 설정되지 않았습니다");
      return;
    }
    const state = crypto.randomUUID();
    sessionStorage.setItem(OAUTH_STATE_KEY, state);
    const redirectUri = buildCallbackRedirectUri(window.location.origin);
    window.location.href = buildGoogleAuthUrl(GOOGLE_CLIENT_ID, redirectUri, state);
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = storage.getRefreshToken();
    if (refreshToken) {
      await authClient.logout(refreshToken);
    }
    storage.clear();
    setState({ user: null, isHydrated: true });
  }, [authClient, storage]);

  return {
    user: state.user,
    isHydrated: state.isHydrated,
    isAuthenticated: state.user !== null,
    login,
    logout,
  };
}
