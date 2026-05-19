import type { TokenStorage } from "./storage";

export type AuthenticatedFetch = (path: string, init?: RequestInit) => Promise<Response>;

export function createAuthenticatedFetch(
  baseUrl: string,
  storage: TokenStorage,
): AuthenticatedFetch {
  let refreshInFlight: Promise<boolean> | null = null;

  async function refresh(): Promise<boolean> {
    if (refreshInFlight) return refreshInFlight;
    const refreshToken = storage.getRefreshToken();
    if (!refreshToken) return false;
    refreshInFlight = (async () => {
      try {
        const response = await fetch(`${baseUrl}/api/v1/auth/tokens:refresh`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken }),
        });
        if (!response.ok) {
          storage.clear();
          return false;
        }
        const tokens = (await response.json()) as {
          accessToken: string;
          refreshToken: string;
        };
        storage.setTokens({
          accessToken: tokens.accessToken,
          refreshToken: tokens.refreshToken,
        });
        return true;
      } catch {
        storage.clear();
        return false;
      } finally {
        refreshInFlight = null;
      }
    })();
    return refreshInFlight;
  }

  return async function authenticatedFetch(path: string, init: RequestInit = {}) {
    const headers = new Headers(init.headers);
    const accessToken = storage.getAccessToken();
    if (accessToken) {
      headers.set("Authorization", `Bearer ${accessToken}`);
    }
    let response = await fetch(`${baseUrl}${path}`, { ...init, headers });

    if (response.status === 401 && (await refresh())) {
      const retryHeaders = new Headers(init.headers);
      const newAccessToken = storage.getAccessToken();
      if (newAccessToken) {
        retryHeaders.set("Authorization", `Bearer ${newAccessToken}`);
      }
      response = await fetch(`${baseUrl}${path}`, { ...init, headers: retryHeaders });
    }
    return response;
  };
}
