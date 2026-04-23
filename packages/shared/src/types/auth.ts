export type AuthProvider = "GOOGLE";

export type AuthUser = {
  id: number;
  email: string;
  display_name: string | null;
  avatar_url: string | null;
  provider: AuthProvider;
};

export type AuthTokens = {
  access_token: string;
  refresh_token: string;
  token_type: string;
  expires_in: number;
};

export type OAuthLoginResult = AuthTokens & { user: AuthUser };
