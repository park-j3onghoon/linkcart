import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  createAuthenticatedFetch,
  createMemoryTokenStorage,
} from "@linkcart/shared";

describe("createAuthenticatedFetch", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    vi.stubGlobal("fetch", fetchMock);
    fetchMock.mockReset();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("attaches Authorization header when access token is present", async () => {
    const storage = createMemoryTokenStorage();
    storage.setTokens({ access_token: "A", refresh_token: "R" });
    fetchMock.mockResolvedValueOnce(new Response("ok", { status: 200 }));

    const fetcher = createAuthenticatedFetch("http://api", storage);
    await fetcher("/resource");

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(new Headers(init.headers).get("Authorization")).toBe("Bearer A");
  });

  it("refreshes the token on 401 and retries the request", async () => {
    const storage = createMemoryTokenStorage();
    storage.setTokens({ access_token: "expired", refresh_token: "R" });

    fetchMock.mockResolvedValueOnce(new Response(null, { status: 401 }));
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({ access_token: "new_access", refresh_token: "new_refresh" }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    fetchMock.mockResolvedValueOnce(new Response("retried", { status: 200 }));

    const fetcher = createAuthenticatedFetch("http://api", storage);
    const response = await fetcher("/resource");

    expect(response.status).toBe(200);
    expect(fetchMock).toHaveBeenCalledTimes(3);
    const retryInit = fetchMock.mock.calls[2][1] as RequestInit;
    expect(new Headers(retryInit.headers).get("Authorization")).toBe("Bearer new_access");
    expect(storage.getAccessToken()).toBe("new_access");
  });

  it("clears tokens when refresh fails", async () => {
    const storage = createMemoryTokenStorage();
    storage.setTokens({ access_token: "expired", refresh_token: "R" });

    fetchMock.mockResolvedValueOnce(new Response(null, { status: 401 }));
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 401 }));

    const fetcher = createAuthenticatedFetch("http://api", storage);
    await fetcher("/resource");

    expect(storage.getAccessToken()).toBeNull();
    expect(storage.getRefreshToken()).toBeNull();
  });

  it("omits Authorization header when no access token is stored", async () => {
    const storage = createMemoryTokenStorage();
    fetchMock.mockResolvedValueOnce(new Response("ok", { status: 200 }));

    const fetcher = createAuthenticatedFetch("http://api", storage);
    await fetcher("/public");

    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(new Headers(init.headers).has("Authorization")).toBe(false);
  });
});
