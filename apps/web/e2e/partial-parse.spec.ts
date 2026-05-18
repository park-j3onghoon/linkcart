import { expect, test } from "@playwright/test";
import { addProduct, buildMockProduct, mockImageProxy, mockParseApi } from "./helpers";

test("부분 파싱 결과를 카드에서 식별할 수 있다", async ({ page }) => {
  const url = "https://example.com/products/partial";
  await mockImageProxy(page);
  await mockParseApi(page, {
    [url]: buildMockProduct({
      url,
      name: "부분 파싱 상품",
      priceAmount: null,
      imageUrl: null,
      fallbackUsed: true,
      partial: {
        imageUrl: "https://images.example.com/partial.png",
        name: "부분 파싱 상품",
      },
    }),
  });

  await page.goto("/");
  await addProduct(page, url);

  await expect(page.getByRole("heading", { name: "부분 파싱 상품" })).toBeVisible();
  await expect(page.getByText("부분 파싱", { exact: true })).toBeVisible();
  await expect(page.getByText("OG 폴백")).toBeVisible();
  await expect(page.getByText("가격 정보 없음")).toBeVisible();
});
