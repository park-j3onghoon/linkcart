import type { MallType, Money } from "./product";

/** AIP-148: name = "shareLists/{parent}/items/{id}". */
export type ShareListItem = {
  name: string;
  displayName: string;
  price: Money;
  imageUrl?: string | null;
  sourceUrl: string;
  mall: MallType;
};

export type ShareList = {
  name: string;
  token: string;
  title?: string | null;
  expireTime?: string | null;
  createTime?: string | null;
  items: ShareListItem[];
};

export type CopyShareListResult = {
  copiedCount: number;
  skippedCount: number;
  products: Array<{
    name: string;
    displayName: string;
    price: Money;
    imageUrl?: string | null;
    sourceUrl: string;
    mall: MallType;
    createTime?: string | null;
  }>;
};
