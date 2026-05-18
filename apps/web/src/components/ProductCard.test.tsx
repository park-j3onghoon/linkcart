import { fireEvent, render, screen } from "@testing-library/react";
import type { ParseResponse } from "@linkcart/shared";
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
    render(<ProductCard imageSrc="/api/v1/images/proxy?url=test" product={product} />);

    expect(screen.getByText("테스트 상품")).toBeInTheDocument();
    expect(screen.getByText("15,900원")).toBeInTheDocument();
    expect(screen.getByText("쿠팡")).toBeInTheDocument();
    expect(screen.getByText("coupang-api")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "원본 상품 페이지 열기" })).toHaveAttribute(
      "href",
      "https://example.com/product/1",
    );
  });

  it("shows a placeholder when the image fails to load", () => {
    render(<ProductCard imageSrc="/api/v1/images/proxy?url=test" product={product} />);

    fireEvent.error(screen.getByAltText("테스트 상품 이미지"));

    expect(screen.getByText("이미지를 준비하지 못했습니다")).toBeInTheDocument();
  });
});
