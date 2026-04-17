import { expect, test } from "@playwright/test";
import {
  addProduct,
  buildMockProduct,
  expectLocalStorageCount,
  mockImageProxy,
  mockParseApi,
} from "./helpers";

test("상품 추가 후 새로고침해도 localStorage에서 카드가 복원된다", async ({ page }) => {
  const url = "https://example.com/products/persisted";
  await mockImageProxy(page);
  await mockParseApi(page, {
    [url]: buildMockProduct({
      url,
      name: "새로고침 복원 상품",
      priceAmount: 17900,
      imageUrl: "https://images.example.com/persisted.png",
    }),
  });

  await page.goto("/");
  await addProduct(page, url);
  await expectLocalStorageCount(page, 1);

  await page.reload();

  await expect(page.getByText("새로고침 복원 상품")).toBeVisible();
  await expectLocalStorageCount(page, 1);
});
