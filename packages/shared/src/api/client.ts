import type { ParseResponse, ShareList } from "../types";

export type ApiResult<T> =
  | { ok: true; data: T }
  | { ok: false; error: ApiError };

export type ApiError =
  | { code: "NETWORK"; message: string }
  | { code: "PARSE_FAILED"; message: string }
  | { code: "TIMEOUT"; message: string }
  | { code: "NOT_FOUND"; message: string }
  | { code: "UNAUTHORIZED"; message: string }
  | { code: "UNKNOWN"; message: string };

const DEFAULT_BASE_URL = "";
const TIMEOUT_MS = 15_000;

export function createApiClient(baseUrl: string = DEFAULT_BASE_URL) {
  async function parseProduct(url: string): Promise<ApiResult<ParseResponse>> {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), TIMEOUT_MS);

    try {
      const response = await fetch(`${baseUrl}/api/v1/products:parse`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ url }),
        signal: controller.signal,
      });

      if (!response.ok) {
        const body = await response.json().catch(() => ({} as Record<string, unknown>));
        const detail = typeof body === "object" && body !== null && "detail" in body
          ? String(body.detail)
          : `HTTP ${response.status} ${response.statusText}`;
        return {
          ok: false,
          error: { code: "PARSE_FAILED", message: detail },
        };
      }

      const data = (await response.json()) as ParseResponse;
      return { ok: true, data };
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

  function imageProxyUrl(originalUrl: string): string {
    return `${baseUrl}/api/v1/images:proxy?url=${encodeURIComponent(originalUrl)}`;
  }

  async function getShareList(token: string): Promise<ApiResult<ShareList>> {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), TIMEOUT_MS);

    try {
      const response = await fetch(
        `${baseUrl}/api/v1/shareLists/${encodeURIComponent(token)}`,
        { signal: controller.signal },
      );

      if (response.status === 404) {
        return {
          ok: false,
          error: { code: "NOT_FOUND", message: "공유 리스트를 찾을 수 없거나 만료되었습니다" },
        };
      }
      if (!response.ok) {
        return {
          ok: false,
          error: {
            code: "UNKNOWN",
            message: `HTTP ${response.status} ${response.statusText}`,
          },
        };
      }

      const data = (await response.json()) as ShareList;
      return { ok: true, data };
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

  return { parseProduct, imageProxyUrl, getShareList };
}
