export type AuthProvider = "GOOGLE";

export type AuthUser = {
  id: number;
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
