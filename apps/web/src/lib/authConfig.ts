export const BACKEND_URL =
  process.env.NEXT_PUBLIC_BACKEND_URL ?? "http://localhost:8080";

export const GOOGLE_CLIENT_ID = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID ?? "";

export const GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
export const OAUTH_STATE_KEY = "linkcart.oauth.state";
export const OAUTH_CALLBACK_PATH = "/auth/callback";

export function buildGoogleAuthUrl(
  clientId: string,
  redirectUri: string,
  state: string,
): string {
  // Google OAuth 2.0 (RFC 6749) authorize endpoint은 snake_case 파라미터를 요구한다.
  // AIP-140 lowerCamelCase는 우리 백엔드 API JSON에만 적용한다.
  const params = new URLSearchParams({
    client_id: clientId,
    redirect_uri: redirectUri,
    response_type: "code",
    scope: "openid email profile",
    access_type: "offline",
    prompt: "consent",
    state,
  });
  return `${GOOGLE_AUTH_URL}?${params.toString()}`;
}

export function buildCallbackRedirectUri(origin: string): string {
  return `${origin}${OAUTH_CALLBACK_PATH}`;
}
