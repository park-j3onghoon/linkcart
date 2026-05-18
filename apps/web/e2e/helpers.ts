import { expect, type Page } from "@playwright/test";
import { PRODUCTS_STORAGE_KEY, type ParseResponse } from "@linkcart/shared";

const pngBody = Buffer.from(
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wn0m3QAAAAASUVORK5CYII=",
  "base64",
);

export type MockProduct = ParseResponse;

type BuildMockProductInput = {
  url: string;
  name: string;
  priceAmount?: number | null;
  imageUrl?: string | null;
  mall?: ParseResponse["mall"];
  parserUsed?: string;
  fallbackUsed?: boolean;
  partial?: ParseResponse["partial"];
};

export function buildMockProduct(input: BuildMockProductInput): MockProduct {
  const {
    url,
    name,
    priceAmount = null,
    imageUrl = null,
    mall = "generic",
    parserUsed = "og",
    fallbackUsed = false,
    partial = null,
  } = input;

  return {
    fallbackUsed: fallbackUsed,
    imageUrl: imageUrl,
    mall,
    name,
    parserUsed: parserUsed,
    partial,
    price: priceAmount === null ? null : { amount: priceAmount, currency: "KRW" },
    sourceUrl: url,
  };
}

export async function mockParseApi(page: Page, productsByUrl: Record<string, MockProduct>) {
  await page.route("**/api/v1/products/parse", async (route) => {
    const body = route.request().postDataJSON() as { url?: string };
    const url = body.url ?? "";
    const response = productsByUrl[url];

    if (!response) {
      await route.fulfill({
        status: 502,
        contentType: "application/json",
        body: JSON.stringify({ detail: `mock not found for ${url}` }),
      });
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(response),
    });
  });
}

export async function mockImageProxy(page: Page) {
  await page.route("**/api/v1/images/proxy**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "image/png",
      body: pngBody,
    });
  });
}

export async function addProduct(page: Page, url: string) {
  await page.getByLabel("상품 URL").fill(url);
  await page.getByRole("button", { name: "상품 추가" }).click();
}

export async function expectStoredProductCount(page: Page, count: number) {
  await expect(page.getByText(`저장된 상품 ${count}건`)).toBeVisible();
}

export async function expectLocalStorageCount(page: Page, count: number) {
  await expect
    .poll(async () =>
      page.evaluate((key) => {
        const raw = window.localStorage.getItem(key);
        if (!raw) return 0;
        try {
          const parsed = JSON.parse(raw);
          return Array.isArray(parsed) ? parsed.length : 0;
        } catch {
          return 0;
        }
      }, PRODUCTS_STORAGE_KEY),
    )
    .toBe(count);
}
