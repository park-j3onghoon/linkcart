/**
 * 상품 정보 타입 — OpenAPI codegen으로 자동 생성될 예정.
 * PR 4(API 엔드포인트) 완성 후 codegen으로 대체됨.
 * 현재는 개발 편의를 위한 수동 정의.
 */

export interface Money {
  amount: number;
  currency: string;
}

export type MallType = "coupang" | "elevenst" | "generic";

export interface Product {
  name: string;
  price: Money;
  image_url: string;
  source_url: string;
  mall: MallType;
}

export interface ParseResponse extends Product {
  partial: Record<string, unknown> | null;
  parser_used: string;
  fallback_used: boolean;
}
