import { expect, test } from "@playwright/test";
import {
  addProduct,
  buildMockProduct,
  expectLocalStorageCount,
  expectStoredProductCount,
  mockImageProxy,
  mockParseApi,
} from "./helpers";

test("URL 입력 후 카드가 표시되고 localStorage에 저장된다", async ({ page }) => {
  const url = "https://example.com/products/happy";
  await mockImageProxy(page);
  await mockParseApi(page, {
    [url]: buildMockProduct({
      url,
      name: "테스트 해피 패스 상품",
      priceAmount: 12900,
      imageUrl: "https://images.example.com/happy.png",
    }),
  });

  await page.goto("/");
  await addProduct(page, url);

  await expect(page.getByText("테스트 해피 패스 상품")).toBeVisible();
  await expectStoredProductCount(page, 1);
  await expectLocalStorageCount(page, 1);
});
