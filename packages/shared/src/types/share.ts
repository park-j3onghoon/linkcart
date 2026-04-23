import type { MallType, Money } from "./product";

export type ShareListItem = {
  id: number;
  name: string;
  price: Money;
  image_url?: string | null;
  source_url: string;
  mall: MallType;
};

export type ShareList = {
  id: number;
  token: string;
  title?: string | null;
  expires_at?: string | null;
  created_at?: string | null;
  items: ShareListItem[];
};

export type CopyShareListResult = {
  copied_count: number;
  skipped_count: number;
  products: Array<{
    id: number;
    name: string;
    price: Money;
    image_url?: string | null;
    source_url: string;
    mall: MallType;
    created_at?: string | null;
  }>;
};
