import type { components } from "./api.gen";

type ApiParseResponse = components["schemas"]["ParseResponse"];

export type Money = components["schemas"]["Money"];
export type MallType = NonNullable<ApiParseResponse["mall"]>;
export type Product = {
  name?: ApiParseResponse["name"] | null;
  price?: ApiParseResponse["price"] | null;
  imageUrl?: ApiParseResponse["imageUrl"] | null;
  sourceUrl?: ApiParseResponse["sourceUrl"] | null;
  mall?: ApiParseResponse["mall"] | null;
};

/**
 * 쇼핑몰 한글 라벨. 백엔드 Mall enum과 wire-format이 동기화되어 있어야 한다.
 * 라벨이 웹/모바일에 흩어지지 않도록 단일 출처에서 관리한다.
 */
export const MALL_LABELS: Record<MallType, string> = {
  coupang: "쿠팡",
  elevenst: "11번가",
  generic: "일반 링크",
};

export type ParseResponse = Omit<ApiParseResponse, "imageUrl" | "mall" | "name" | "partial" | "price" | "sourceUrl"> &
  Product & {
  partial?: Record<string, unknown> | null;
};
