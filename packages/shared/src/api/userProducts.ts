import type { AuthenticatedFetch } from "../auth/authenticatedFetch";
import type { MallType, Money } from "../types/product";
import type { ApiResult } from "./client";

export type UserProduct = {
  id: number;
  name: string;
  price: Money;
  imageUrl?: string | null;
  sourceUrl: string;
  mall: MallType;
  parserUsed: string;
  createdAt?: string | null;
};

export type SaveProductInput = {
  name: string;
  price: Money;
  imageUrl?: string | null;
  sourceUrl: string;
  mall: MallType;
  parserUsed: string;
};

async function readJsonOrError<T>(response: Response): Promise<ApiResult<T>> {
  if (response.status === 401) {
    return { ok: false, error: { code: "UNAUTHORIZED", message: "인증이 만료되었습니다" } };
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
  if (response.status === 204) {
    return { ok: true, data: undefined as unknown as T };
  }
  return { ok: true, data: (await response.json()) as T };
}

export function createUserProductsClient(authenticatedFetch: AuthenticatedFetch) {
  async function listProducts(): Promise<ApiResult<UserProduct[]>> {
    try {
      const response = await authenticatedFetch("/api/v1/users/me/products");
      const parsed = await readJsonOrError<{ products: UserProduct[] }>(response);
      if (!parsed.ok) return parsed;
      return { ok: true, data: parsed.data.products };
    } catch (err) {
      return {
        ok: false,
        error: {
          code: "NETWORK",
          message: err instanceof Error ? err.message : "네트워크 오류",
        },
      };
    }
  }

  async function saveProduct(input: SaveProductInput): Promise<ApiResult<UserProduct>> {
    try {
      const response = await authenticatedFetch("/api/v1/users/me/products", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(input),
      });
      return readJsonOrError<UserProduct>(response);
    } catch (err) {
      return {
        ok: false,
        error: {
          code: "NETWORK",
          message: err instanceof Error ? err.message : "네트워크 오류",
        },
      };
    }
  }

  async function deleteProduct(productId: number): Promise<ApiResult<void>> {
    try {
      const response = await authenticatedFetch(
        `/api/v1/users/me/products/${productId}`,
        { method: "DELETE" },
      );
      return readJsonOrError<void>(response);
    } catch (err) {
      return {
        ok: false,
        error: {
          code: "NETWORK",
          message: err instanceof Error ? err.message : "네트워크 오류",
        },
      };
    }
  }

  return { listProducts, saveProduct, deleteProduct };
}

export type UserProductsClient = ReturnType<typeof createUserProductsClient>;
