import { render, screen } from "@testing-library/react";
import type { ParseResponse } from "@linkcart/shared";
import { describe, it, expect } from "vitest";
import { ProductList } from "./ProductList";

function buildImageSrc(imageUrl?: string | null) {
  return imageUrl ? `proxy:${imageUrl}` : null;
}

const sampleProduct: ParseResponse = {
  name: "아이폰 16 Pro",
  price: { amount: 1550000, currency: "KRW" },
  imageUrl: "https://example.com/p.jpg",
  sourceUrl: "https://shop.example.com/p/1",
  mall: "coupang",
  partial: null,
  parserUsed: "coupang-api",
  fallbackUsed: false,
};

describe("ProductList", () => {
  it("hydration 중에는 안내 메시지를 표시한다", () => {
    render(<ProductList buildImageSrc={buildImageSrc} isHydrated={false} products={[]} />);

    expect(screen.getByText(/이전 결과를 불러오고 있습니다/)).toBeInTheDocument();
    expect(screen.queryByTestId("product-grid")).not.toBeInTheDocument();
  });

  it("hydration 후 상품이 없으면 비어 있음 안내", () => {
    render(<ProductList buildImageSrc={buildImageSrc} isHydrated products={[]} />);

    expect(screen.getByText(/아직 수집한 상품이 없습니다/)).toBeInTheDocument();
  });

  it("상품 리스트 렌더, 개수 표시 + grid 노출", () => {
    render(
      <ProductList
        buildImageSrc={buildImageSrc}
        isHydrated
        products={[sampleProduct, { ...sampleProduct, sourceUrl: "https://shop/2" }]}
      />,
    );

    expect(screen.getByText(/저장된 상품 2건/)).toBeInTheDocument();
    expect(screen.getByTestId("product-grid")).toBeInTheDocument();
    expect(screen.getAllByTestId("product-card")).toHaveLength(2);
  });
});
