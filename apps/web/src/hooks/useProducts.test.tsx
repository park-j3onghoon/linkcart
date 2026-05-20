import { act, renderHook, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useProducts } from "./useProducts";

describe("useProducts", () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("hydration 직후 isHydrated=true가 된다", async () => {
    const { result } = renderHook(() => useProducts());

    await waitFor(() => expect(result.current.isHydrated).toBe(true));
    expect(result.current.products).toEqual([]);
    expect(result.current.phase).toBe("idle");
  });

  it("잘못된 URL 입력 시 error feedback을 설정한다", async () => {
    const { result } = renderHook(() => useProducts());
    await waitFor(() => expect(result.current.isHydrated).toBe(true));

    let submitted = true;
    await act(async () => {
      submitted = await result.current.submitUrl("not-a-url");
    });

    expect(submitted).toBe(false);
    expect(result.current.phase).toBe("error");
    expect(result.current.feedback?.kind).toBe("error");
  });

  it("성공 응답 시 products 리스트 맨 앞에 추가", async () => {
    vi.spyOn(window, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          name: "갤럭시 S25",
          price: { amount: 1100000, currency: "KRW" },
          imageUrl: "https://x/i.jpg",
          sourceUrl: "https://shop.example.com/p/100",
          mall: "coupang",
          parserUsed: "coupang-api",
          fallbackUsed: false,
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );

    const { result } = renderHook(() => useProducts());
    await waitFor(() => expect(result.current.isHydrated).toBe(true));

    await act(async () => {
      await result.current.submitUrl("https://shop.example.com/p/100");
    });

    expect(result.current.products).toHaveLength(1);
    expect(result.current.products[0].name).toBe("갤럭시 S25");
    expect(result.current.phase).toBe("success");
    expect(result.current.feedback?.kind).toBe("success");
  });

  it("동일 sourceUrl로 두 번 호출하면 두 번째는 중복 경고", async () => {
    vi.spyOn(window, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          name: "x",
          price: { amount: 1, currency: "KRW" },
          sourceUrl: "https://dup.example.com/p/1",
          mall: "coupang",
          parserUsed: "coupang-api",
          fallbackUsed: false,
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );

    const { result } = renderHook(() => useProducts());
    await waitFor(() => expect(result.current.isHydrated).toBe(true));

    await act(async () => {
      await result.current.submitUrl("https://dup.example.com/p/1");
    });
    await act(async () => {
      await result.current.submitUrl("https://dup.example.com/p/1");
    });

    expect(result.current.products).toHaveLength(1);
    expect(result.current.feedback?.kind).toBe("warning");
  });

  it("API 실패 시 error feedback + message 표시", async () => {
    vi.spyOn(window, "fetch").mockResolvedValueOnce(
      new Response(JSON.stringify({ code: "UNAVAILABLE", message: "쇼핑몰 응답 실패" }), {
        status: 503,
        headers: { "Content-Type": "application/json" },
      }),
    );

    const { result } = renderHook(() => useProducts());
    await waitFor(() => expect(result.current.isHydrated).toBe(true));

    await act(async () => {
      await result.current.submitUrl("https://shop.example.com/p/fail");
    });

    expect(result.current.phase).toBe("error");
    expect(result.current.feedback?.text).toContain("쇼핑몰 응답 실패");
  });

  it("buildImageSrc는 null 입력에 null을 반환한다", async () => {
    const { result } = renderHook(() => useProducts());
    await waitFor(() => expect(result.current.isHydrated).toBe(true));

    expect(result.current.buildImageSrc(null)).toBeNull();
    expect(result.current.buildImageSrc(undefined)).toBeNull();
  });

  it("buildImageSrc는 이미지 URL을 백엔드 프록시 경로로 감싼다", async () => {
    const { result } = renderHook(() => useProducts());
    await waitFor(() => expect(result.current.isHydrated).toBe(true));

    const proxied = result.current.buildImageSrc("https://x.com/i.jpg");
    expect(proxied).toContain("/api/v1/images:proxy");
    expect(proxied).toContain(encodeURIComponent("https://x.com/i.jpg"));
  });
});
