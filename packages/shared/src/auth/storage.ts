export type StoredTokens = {
  access_token: string;
  refresh_token: string;
};

export type TokenStorage = {
  getAccessToken(): string | null;
  getRefreshToken(): string | null;
  setTokens(tokens: StoredTokens): void;
  clear(): void;
};

export function createMemoryTokenStorage(): TokenStorage {
  let accessToken: string | null = null;
  let refreshToken: string | null = null;
  return {
    getAccessToken: () => accessToken,
    getRefreshToken: () => refreshToken,
    setTokens: (tokens) => {
      accessToken = tokens.access_token;
      refreshToken = tokens.refresh_token;
    },
    clear: () => {
      accessToken = null;
      refreshToken = null;
    },
  };
}

export type WebStorageLike = {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
  removeItem(key: string): void;
};

export function createWebTokenStorage(storage: WebStorageLike): TokenStorage {
  const ACCESS_KEY = "linkcart.auth.access_token";
  const REFRESH_KEY = "linkcart.auth.refresh_token";
  return {
    getAccessToken: () => storage.getItem(ACCESS_KEY),
    getRefreshToken: () => storage.getItem(REFRESH_KEY),
    setTokens: (tokens) => {
      storage.setItem(ACCESS_KEY, tokens.access_token);
      storage.setItem(REFRESH_KEY, tokens.refresh_token);
    },
    clear: () => {
      storage.removeItem(ACCESS_KEY);
      storage.removeItem(REFRESH_KEY);
    },
  };
}
