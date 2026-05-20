export type AuthProvider = "GOOGLE";

/** AIP-148: name = "users/{id}". */
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
