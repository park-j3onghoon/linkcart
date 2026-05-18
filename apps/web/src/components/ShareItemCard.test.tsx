import { fireEvent, render, screen } from "@testing-library/react";
import type { ShareListItem } from "@linkcart/shared";
import { ShareItemCard } from "./ShareItemCard";

const item: ShareListItem = {
  name: "shareLists/1/items/1",
  displayName: "테스트 상품",
  price: { amount: 15900, currency: "KRW" },
  imageUrl: "https://example.com/image.jpg",
  sourceUrl: "https://example.com/product/1",
  mall: "coupang",
};

describe("ShareItemCard", () => {
  it("renders item details and mall label", () => {
    render(<ShareItemCard imageSrc="/api/v1/images:proxy?url=test" item={item} />);

    expect(screen.getByText("테스트 상품")).toBeInTheDocument();
    expect(screen.getByText("15,900원")).toBeInTheDocument();
    expect(screen.getByText("쿠팡")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "원본 상품 페이지 열기" })).toHaveAttribute(
      "href",
      "https://example.com/product/1",
    );
  });

  it("shows a placeholder when the image fails to load", () => {
    render(<ShareItemCard imageSrc="/api/v1/images:proxy?url=test" item={item} />);

    fireEvent.error(screen.getByAltText("테스트 상품 이미지"));

    expect(screen.getByText("이미지를 준비하지 못했습니다")).toBeInTheDocument();
  });

  it("renders placeholder when imageSrc is null", () => {
    render(<ShareItemCard imageSrc={null} item={item} />);

    expect(screen.getByText("이미지를 준비하지 못했습니다")).toBeInTheDocument();
  });

  it("formats non-KRW currency with suffix", () => {
    const usdItem: ShareListItem = {
      ...item,
      price: { amount: 99, currency: "USD" },
    };
    render(<ShareItemCard imageSrc={null} item={usdItem} />);

    expect(screen.getByText("99 USD")).toBeInTheDocument();
  });
});
