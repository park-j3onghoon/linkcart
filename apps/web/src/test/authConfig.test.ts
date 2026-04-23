import { describe, expect, it } from "vitest";
import {
  buildCallbackRedirectUri,
  buildGoogleAuthUrl,
  OAUTH_CALLBACK_PATH,
} from "../lib/authConfig";

describe("buildGoogleAuthUrl", () => {
  it("includes client_id, redirect_uri, state, and OIDC scopes", () => {
    const url = buildGoogleAuthUrl(
      "client-xyz.apps.googleusercontent.com",
      "https://example.com/auth/callback",
      "nonce-123",
    );

    const parsed = new URL(url);
    expect(parsed.origin).toBe("https://accounts.google.com");
    expect(parsed.pathname).toBe("/o/oauth2/v2/auth");
    expect(parsed.searchParams.get("client_id")).toBe("client-xyz.apps.googleusercontent.com");
    expect(parsed.searchParams.get("redirect_uri")).toBe("https://example.com/auth/callback");
    expect(parsed.searchParams.get("state")).toBe("nonce-123");
    expect(parsed.searchParams.get("response_type")).toBe("code");
    expect(parsed.searchParams.get("scope")).toBe("openid email profile");
  });
});

describe("buildCallbackRedirectUri", () => {
  it("joins origin with callback path", () => {
    expect(buildCallbackRedirectUri("https://linkcart.app")).toBe(
      `https://linkcart.app${OAUTH_CALLBACK_PATH}`,
    );
  });
});
