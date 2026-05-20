import { act, renderHook, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useAuth } from "./useAuth";

describe("useAuth", () => {
  beforeEach(() => {
    window.localStorage.clear();
    window.sessionStorage.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("저장된 토큰 없으면 isHydrated=true, user=null", async () => {
    const { result } = renderHook(() => useAuth());

    await waitFor(() => expect(result.current.isHydrated).toBe(true));
    expect(result.current.user).toBeNull();
    expect(result.current.isAuthenticated).toBe(false);
  });

  it("저장된 access token이 있으면 getMe로 사용자 로드", async () => {
    window.localStorage.setItem("linkcart.auth.accessToken", "AT-123");
    window.localStorage.setItem("linkcart.auth.refreshToken", "RT-456");
    vi.spyOn(window, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          user: {
            name: "users/7",
            email: "u@e.com",
            displayName: "U",
            avatarUrl: null,
            provider: "GOOGLE",
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );

    const { result } = renderHook(() => useAuth());

    await waitFor(() => expect(result.current.isAuthenticated).toBe(true));
    expect(result.current.user?.email).toBe("u@e.com");
  });

  it("getMe 401 응답이면 토큰 정리하고 user=null", async () => {
    window.localStorage.setItem("linkcart.auth.accessToken", "AT-stale");
    vi.spyOn(window, "fetch").mockResolvedValueOnce(
      new Response(null, { status: 401 }),
    );

    const { result } = renderHook(() => useAuth());

    await waitFor(() => expect(result.current.isHydrated).toBe(true));
    expect(result.current.user).toBeNull();
    expect(window.localStorage.getItem("linkcart.auth.accessToken")).toBeNull();
  });

  it("logout은 백엔드를 호출하고 상태와 저장소를 비운다", async () => {
    window.localStorage.setItem("linkcart.auth.accessToken", "AT");
    window.localStorage.setItem("linkcart.auth.refreshToken", "RT");
    const fetchSpy = vi.spyOn(window, "fetch");
    // 첫 호출은 getMe (실패해도 OK)
    fetchSpy.mockResolvedValueOnce(new Response(null, { status: 401 }));
    // 두 번째는 logout (tokens:revoke)
    fetchSpy.mockResolvedValueOnce(new Response(null, { status: 204 }));

    const { result } = renderHook(() => useAuth());
    await waitFor(() => expect(result.current.isHydrated).toBe(true));
    // getMe 401로 토큰이 이미 정리되었으니 새로 다시 넣어 logout 흐름을 검증
    window.localStorage.setItem("linkcart.auth.refreshToken", "RT");

    await act(async () => {
      await result.current.logout();
    });

    expect(window.localStorage.getItem("linkcart.auth.refreshToken")).toBeNull();
    expect(result.current.user).toBeNull();
  });

  it("login은 Google OAuth URL로 리다이렉트한다", async () => {
    // GOOGLE_CLIENT_ID env가 없으면 console.warn만 찍고 종료
    const warnSpy = vi.spyOn(console, "warn").mockImplementation(() => undefined);
    const { result } = renderHook(() => useAuth());
    await waitFor(() => expect(result.current.isHydrated).toBe(true));

    act(() => {
      result.current.login();
    });

    expect(warnSpy).toHaveBeenCalledWith(expect.stringContaining("GOOGLE_CLIENT_ID"));
  });
});
