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

export type ParseResponse = Omit<ApiParseResponse, "imageUrl" | "mall" | "name" | "partial" | "price" | "sourceUrl"> &
  Product & {
  partial?: Record<string, unknown> | null;
};
