import { expect, test } from "@playwright/test";
import {
  addProduct,
  buildMockProduct,
  expectLocalStorageCount,
  expectStoredProductCount,
  mockImageProxy,
  mockParseApi,
} from "./helpers";

test("여러 URL을 추가하면 리스트가 누적된다", async ({ page }) => {
  const products = [
    { url: "https://example.com/products/1", name: "첫 번째 상품", priceAmount: 10000 },
    { url: "https://example.com/products/2", name: "두 번째 상품", priceAmount: 20000 },
    { url: "https://example.com/products/3", name: "세 번째 상품", priceAmount: 30000 },
  ];
  await mockImageProxy(page);
  await mockParseApi(
    page,
    Object.fromEntries(
      products.map((product, index) => [
        product.url,
        buildMockProduct({
          ...product,
          imageUrl: `https://images.example.com/${index + 1}.png`,
        }),
      ]),
    ),
  );

  await page.goto("/");
  for (const product of products) {
    await addProduct(page, product.url);
  }

  for (const product of products) {
    await expect(page.getByText(product.name)).toBeVisible();
  }
  await expect(page.getByTestId("product-grid").getByTestId("product-card")).toHaveCount(3);
  await expectStoredProductCount(page, 3);
  await expectLocalStorageCount(page, 3);
});
