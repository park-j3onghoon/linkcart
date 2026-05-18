import type { MallType, Money } from "./product";

export type ShareListItem = {
  id: number;
  name: string;
  price: Money;
  imageUrl?: string | null;
  sourceUrl: string;
  mall: MallType;
};

export type ShareList = {
  id: number;
  token: string;
  title?: string | null;
  expiresAt?: string | null;
  createdAt?: string | null;
  items: ShareListItem[];
};

export type CopyShareListResult = {
  copiedCount: number;
  skippedCount: number;
  products: Array<{
    id: number;
    name: string;
    price: Money;
    imageUrl?: string | null;
    sourceUrl: string;
    mall: MallType;
    createdAt?: string | null;
  }>;
};
