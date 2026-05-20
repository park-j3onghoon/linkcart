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

/** 백엔드 Mall enum wire format(`coupang`/`elevenst`/`generic`)과 키를 맞춘다. */
export const MALL_LABELS: Record<MallType, string> = {
  coupang: "쿠팡",
  elevenst: "11번가",
  generic: "일반 링크",
};

export type ParseResponse = Omit<ApiParseResponse, "imageUrl" | "mall" | "name" | "partial" | "price" | "sourceUrl"> &
  Product & {
  partial?: Record<string, unknown> | null;
};
