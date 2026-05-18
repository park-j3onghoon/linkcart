export type StoredTokens = {
  accessToken: string;
  refreshToken: string;
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
      accessToken = tokens.accessToken;
      refreshToken = tokens.refreshToken;
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
  const ACCESS_KEY = "linkcart.auth.accessToken";
  const REFRESH_KEY = "linkcart.auth.refreshToken";
  return {
    getAccessToken: () => storage.getItem(ACCESS_KEY),
    getRefreshToken: () => storage.getItem(REFRESH_KEY),
    setTokens: (tokens) => {
      storage.setItem(ACCESS_KEY, tokens.accessToken);
      storage.setItem(REFRESH_KEY, tokens.refreshToken);
    },
    clear: () => {
      storage.removeItem(ACCESS_KEY);
      storage.removeItem(REFRESH_KEY);
    },
  };
}
