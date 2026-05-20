import type { ApiResult } from "./client";

export const REQUEST_TIMEOUT_MS = 15_000;

/**
 * fetch + AbortController + AIP-193 에러 매핑을 한 번에 처리하는 공용 헬퍼.
 * 각 client 가 동일한 timeout·error 분기를 복붙하지 않도록 한다.
 *
 * - status 가 `extraStatusHandlers` 에 매칭되면 해당 분기로 우선 진입
 * - 그 외 4xx/5xx 는 body.message(AIP-193) 를 우선 사용해 UNKNOWN 으로 매핑
 * - 204 는 undefined 데이터로 success
 */
export async function request<T>(
  url: string,
  init: RequestInit = {},
  extraStatusHandlers?: Record<number, ApiResult<T>>,
): Promise<ApiResult<T>> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  try {
    const response = await fetch(url, { ...init, signal: controller.signal });

    if (extraStatusHandlers && response.status in extraStatusHandlers) {
      return extraStatusHandlers[response.status];
    }
    if (response.status === 401) {
      return { ok: false, error: { code: "UNAUTHORIZED", message: "인증 실패" } };
    }
    if (!response.ok) {
      const body = await response.json().catch(() => ({} as Record<string, unknown>));
      const message =
        typeof body === "object" && body !== null && "message" in body
          ? String(body.message)
          : `HTTP ${response.status} ${response.statusText}`;
      return { ok: false, error: { code: "UNKNOWN", message } };
    }
    if (response.status === 204) {
      return { ok: true, data: undefined as unknown as T };
    }
    return { ok: true, data: (await response.json()) as T };
  } catch (err) {
    if (err instanceof DOMException && err.name === "AbortError") {
      return { ok: false, error: { code: "TIMEOUT", message: "요청 시간이 초과되었습니다" } };
    }
    return {
      ok: false,
      error: {
        code: "NETWORK",
        message: err instanceof Error ? err.message : "네트워크 오류가 발생했습니다",
      },
    };
  } finally {
    clearTimeout(timeoutId);
  }
}
