import { expect, test } from "@playwright/test";
import { addProduct, buildMockProduct, mockImageProxy, mockParseApi } from "./helpers";

test("잘못된 URL 에러 후 올바른 URL 재입력으로 복구된다", async ({ page }) => {
  const url = "https://example.com/products/recovered";
  await mockImageProxy(page);
  await mockParseApi(page, {
    [url]: buildMockProduct({
      url,
      name: "복구 성공 상품",
      priceAmount: 21900,
      imageUrl: "https://images.example.com/recovered.png",
    }),
  });

  await page.goto("/");
  await page.getByLabel("상품 URL").fill("not-a-url");
  await page.getByRole("button", { name: "상품 추가" }).click();

  await expect(page.getByText("올바른 URL 형식이 아닙니다 (http:// 또는 https://)")).toBeVisible();

  await addProduct(page, url);
  await expect(page.getByText("복구 성공 상품")).toBeVisible();
  await expect(page.getByText("상품 카드를 리스트에 추가했습니다.")).toBeVisible();
});
