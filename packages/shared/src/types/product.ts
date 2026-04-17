import type { components } from "./api.gen";

type ApiParseResponse = components["schemas"]["ParseResponse"];

export type Money = components["schemas"]["Money"];
export type MallType = NonNullable<ApiParseResponse["mall"]>;
export type Product = {
  name?: ApiParseResponse["name"] | null;
  price?: ApiParseResponse["price"] | null;
  image_url?: ApiParseResponse["image_url"] | null;
  source_url?: ApiParseResponse["source_url"] | null;
  mall?: ApiParseResponse["mall"] | null;
};

export type ParseResponse = Omit<ApiParseResponse, "image_url" | "mall" | "name" | "partial" | "price" | "source_url"> &
  Product & {
  partial?: Record<string, unknown> | null;
};
