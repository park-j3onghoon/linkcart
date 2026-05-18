export type AuthProvider = "GOOGLE";

/**
 * AIP-122/148: name = "users/{id}", 사용자 표시명은 displayName.
 */
export type AuthUser = {
  name: string;
  email: string;
  displayName: string | null;
  avatarUrl: string | null;
  provider: AuthProvider;
};

export type AuthTokens = {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
};

export type OAuthLoginResult = AuthTokens & { user: AuthUser };
