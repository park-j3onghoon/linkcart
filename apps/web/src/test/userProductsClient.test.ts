import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { createUserProductsClient } from "@linkcart/shared";

describe("createUserProductsClient", () => {
  let fetchMock: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    fetchMock = vi.fn();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("listProducts returns products with nextPageToken when present", async () => {
    const authenticatedFetch = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          products: [
            {
              id: 1,
              name: "상품",
              price: { amount: 1000, currency: "KRW" },
              sourceUrl: "https://s/1",
              mall: "coupang",
              parserUsed: "coupang-api",
            },
          ],
          nextPageToken: "TOKEN_NEXT",
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );

    const client = createUserProductsClient(authenticatedFetch);
    const result = await client.listProducts();

    expect(result.ok).toBe(true);
    if (result.ok) {
      expect(result.data.products).toHaveLength(1);
      expect(result.data.products[0].name).toBe("상품");
      expect(result.data.nextPageToken).toBe("TOKEN_NEXT");
    }
    expect(authenticatedFetch).toHaveBeenCalledWith("/api/v1/users/me/products");
  });

  it("listProducts passes pageSize and pageToken as query params", async () => {
    const authenticatedFetch = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({ products: [], nextPageToken: null }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );

    const client = createUserProductsClient(authenticatedFetch);
    await client.listProducts({ pageSize: 20, pageToken: "abc" });

    expect(authenticatedFetch).toHaveBeenCalledWith(
      "/api/v1/users/me/products?pageSize=20&pageToken=abc",
    );
  });

  it("listProducts returns UNAUTHORIZED on 401", async () => {
    const authenticatedFetch = vi
      .fn()
      .mockResolvedValue(new Response(null, { status: 401 }));

    const client = createUserProductsClient(authenticatedFetch);
    const result = await client.listProducts();

    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.error.code).toBe("UNAUTHORIZED");
  });

  it("saveProduct posts the body and returns the saved product", async () => {
    const authenticatedFetch = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          id: 99,
          name: "새 상품",
          price: { amount: 2000, currency: "KRW" },
          sourceUrl: "https://s/new",
          mall: "generic",
          parserUsed: "og",
        }),
        { status: 201, headers: { "Content-Type": "application/json" } },
      ),
    );

    const client = createUserProductsClient(authenticatedFetch);
    const result = await client.saveProduct({
      name: "새 상품",
      price: { amount: 2000, currency: "KRW" },
      sourceUrl: "https://s/new",
      mall: "generic",
      parserUsed: "og",
    });

    expect(result.ok).toBe(true);
    if (result.ok) expect(result.data.id).toBe(99);
    const [, init] = authenticatedFetch.mock.calls[0] as [string, RequestInit];
    expect(init.method).toBe("POST");
    const body = JSON.parse(init.body as string);
    expect(body.name).toBe("새 상품");
  });

  it("deleteProduct returns ok when the server replies 204", async () => {
    const authenticatedFetch = vi
      .fn()
      .mockResolvedValue(new Response(null, { status: 204 }));

    const client = createUserProductsClient(authenticatedFetch);
    const result = await client.deleteProduct(42);

    expect(result.ok).toBe(true);
    expect(authenticatedFetch).toHaveBeenCalledWith(
      "/api/v1/users/me/products/42",
      { method: "DELETE" },
    );
  });

  it("wraps network failures into ApiError", async () => {
    const authenticatedFetch = vi.fn().mockRejectedValue(new TypeError("boom"));

    const client = createUserProductsClient(authenticatedFetch);
    const result = await client.listProducts();

    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.error.code).toBe("NETWORK");
  });
});
