import type { ApiResult } from "../api/client";
import { API_PATHS } from "../api/paths";
import { request } from "../api/request";
import type { AuthTokens, AuthUser, OAuthLoginResult } from "../types/auth";

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
