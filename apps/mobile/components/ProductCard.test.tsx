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

  it("이미지 URI가 null이면 fallback 메시지 표시", () => {
    render(<ProductCard imageUri={null} product={product} />);

    expect(screen.getByText("이미지를 준비하지 못했습니다")).toBeTruthy();
  });

  it("price가 null이면 가격 정보 없음 + mall 미정이면 출처 미상", () => {
    render(
      <ProductCard
        imageUri={null}
        product={{ ...product, price: null, mall: null }}
      />,
    );

    expect(screen.getByText("가격 정보 없음")).toBeTruthy();
    expect(screen.getByText("출처 미상")).toBeTruthy();
  });

  it("외화(USD) 가격은 통화 코드와 함께 표시", () => {
    render(
      <ProductCard
        imageUri={null}
        product={{ ...product, price: { amount: 99, currency: "USD" } }}
      />,
    );

    expect(screen.getByText("99 USD")).toBeTruthy();
  });

  it("fallbackUsed=true이면 OG 폴백 배지 노출", () => {
    render(
      <ProductCard imageUri={null} product={{ ...product, fallbackUsed: true }} />,
    );

    expect(screen.getByText("OG 폴백")).toBeTruthy();
  });

  it("partial 필드가 있으면 부분 파싱 배지 노출, sourceUrl이 없으면 비활성 텍스트", () => {
    render(
      <ProductCard
        imageUri={null}
        product={{ ...product, sourceUrl: undefined, partial: { name: "x" } }}
      />,
    );

    expect(screen.getByText("부분 파싱")).toBeTruthy();
    expect(screen.getByText("원본 상품 링크를 제공하지 않았습니다")).toBeTruthy();
  });

  it("상품명이 없으면 fallback 텍스트 표시", () => {
    render(<ProductCard imageUri={null} product={{ ...product, name: null }} />);

    expect(screen.getByText("상품명을 불러오지 못했습니다")).toBeTruthy();
  });
});
