import type { ApiResult } from "../api/client";
import { API_PATHS } from "../api/paths";
import type { AuthTokens, AuthUser, OAuthLoginResult } from "../types/auth";

const TIMEOUT_MS = 15_000;

async function request<T>(
  url: string,
  init: RequestInit,
): Promise<ApiResult<T>> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    const response = await fetch(url, { ...init, signal: controller.signal });
    if (response.status === 401) {
      return { ok: false, error: { code: "UNAUTHORIZED", message: "인증 실패" } };
    }
    if (!response.ok) {
      return {
        ok: false,
        error: {
          code: "UNKNOWN",
          message: `HTTP ${response.status} ${response.statusText}`,
        },
      };
    }
    if (response.status === 204) {
      return { ok: true, data: undefined as unknown as T };
    }
    const data = (await response.json()) as T;
    return { ok: true, data };
  } catch (err) {
    if (err instanceof DOMException && err.name === "AbortError") {
      return { ok: false, error: { code: "TIMEOUT", message: "요청 시간이 초과되었습니다" } };
    }
    return {
      ok: false,
      error: {
        code: "NETWORK",
        message: err instanceof Error ? err.message : "네트워크 오류가 발생했습니다",
      },
    };
  } finally {
    clearTimeout(timeoutId);
  }
}

export function createAuthClient(baseUrl: string) {
  function loginWithGoogle(code: string, redirectUri: string): Promise<ApiResult<OAuthLoginResult>> {
    return request<OAuthLoginResult>(`${baseUrl}${API_PATHS.authOAuthGoogle}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ code, redirectUri }),
    });
  }

  function refreshTokens(refreshToken: string): Promise<ApiResult<AuthTokens>> {
    return request<AuthTokens>(`${baseUrl}${API_PATHS.authTokensRefresh}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
  }

  function logout(refreshToken: string): Promise<ApiResult<void>> {
    return request<void>(`${baseUrl}${API_PATHS.authTokensRevoke}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
  }

  function getMe(accessToken: string): Promise<ApiResult<{ user: AuthUser }>> {
    return request<{ user: AuthUser }>(`${baseUrl}${API_PATHS.usersMe}`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
  }

  return { loginWithGoogle, refreshTokens, logout, getMe };
}

export type AuthClient = ReturnType<typeof createAuthClient>;
