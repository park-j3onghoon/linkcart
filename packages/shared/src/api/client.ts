import type { ParseResponse, ShareList } from "../types";
import { API_PATHS } from "./paths";
import { request } from "./request";

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

export function createApiClient(baseUrl: string = DEFAULT_BASE_URL) {
  async function parseProduct(url: string): Promise<ApiResult<ParseResponse>> {
    const result = await request<ParseResponse>(`${baseUrl}${API_PATHS.productsParse}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ url }),
    });
    // request 헬퍼는 일반 실패를 UNKNOWN 으로 반환하므로, 도메인 의미(PARSE_FAILED) 로 좁힌다.
    if (!result.ok && result.error.code === "UNKNOWN") {
      return { ok: false, error: { code: "PARSE_FAILED", message: result.error.message } };
    }
    return result;
  }

  function imageProxyUrl(originalUrl: string): string {
    return `${baseUrl}${API_PATHS.imagesProxy}?url=${encodeURIComponent(originalUrl)}`;
  }

  /** AIP-131: token이 secret이라 URL이 아닌 body로 전달. */
  function lookupShareListByToken(token: string): Promise<ApiResult<ShareList>> {
    return request<ShareList>(
      `${baseUrl}${API_PATHS.shareListsLookup}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token }),
      },
      {
        404: {
          ok: false,
          error: { code: "NOT_FOUND", message: "공유 리스트를 찾을 수 없거나 만료되었습니다" },
        },
      },
    );
  }

  return { parseProduct, imageProxyUrl, lookupShareListByToken };
}
