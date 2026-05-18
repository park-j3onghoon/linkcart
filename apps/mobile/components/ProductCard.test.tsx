import type { ParseResponse } from "@linkcart/shared";
import { render, screen } from "@testing-library/react-native";
import { ProductCard } from "./ProductCard";

const product: ParseResponse = {
  fallbackUsed: false,
  imageUrl: "https://example.com/image.jpg",
  mall: "coupang",
  name: "테스트 상품",
  parserUsed: "coupang-api",
  price: {
    amount: 15900,
    currency: "KRW",
  },
  sourceUrl: "https://example.com/product/1",
};

describe("ProductCard", () => {
  it("renders product details", () => {
    render(
      <ProductCard
        imageUri="https://example.com/proxy-image.jpg"
        product={product}
      />,
    );

    expect(screen.getByText("테스트 상품")).toBeTruthy();
    expect(screen.getByText("15,900원")).toBeTruthy();
    expect(screen.getByText("쿠팡")).toBeTruthy();
    expect(screen.getByText("coupang-api")).toBeTruthy();
    expect(screen.getByText("원본 상품 페이지 열기")).toBeTruthy();
  });
});
