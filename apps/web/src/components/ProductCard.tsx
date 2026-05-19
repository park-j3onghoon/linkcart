"use client";

import { MALL_LABELS, type ParseResponse } from "@linkcart/shared";
import Image from "next/image";
import { useState } from "react";

type ProductCardProps = {
  imageSrc: string | null;
  product: ParseResponse;
};

function formatPrice(product: ParseResponse): string {
  if (!product.price) {
    return "가격 정보 없음";
  }

  const formattedAmount = new Intl.NumberFormat("ko-KR").format(product.price.amount);
  if (product.price.currency === "KRW") {
    return `${formattedAmount}원`;
  }

  return `${formattedAmount} ${product.price.currency}`;
}

export function ProductCard({ imageSrc, product }: ProductCardProps) {
  const [imageFailed, setImageFailed] = useState(false);
  const imageAlt = `${product.name ?? "상품"} 이미지`;
  const mallLabel = product.mall ? MALL_LABELS[product.mall] : "출처 미상";
  const hasPartialFields = product.partial && Object.keys(product.partial).length > 0;

  return (
    <article
      data-testid="product-card"
      className="overflow-hidden rounded-[28px] border border-slate-200 bg-white shadow-[0_20px_60px_rgba(15,23,42,0.08)]"
    >
      <div className="relative flex aspect-[4/3] min-h-56 items-center justify-center overflow-hidden bg-[linear-gradient(135deg,#e0f2fe,#f8fafc,#ede9fe)] sm:min-h-64">
        {imageSrc && !imageFailed ? (
          <Image
            src={imageSrc}
            alt={imageAlt}
            className="object-cover"
            fill
            onError={() => setImageFailed(true)}
            sizes="(max-width: 768px) 100vw, (max-width: 1280px) 50vw, 33vw"
            unoptimized
          />
        ) : (
          <div className="flex h-full w-full flex-col items-center justify-center gap-2 px-6 text-center text-slate-500">
            <span className="text-xs font-medium uppercase tracking-[0.25em] text-slate-400">
              Image Fallback
            </span>
            <p className="text-sm font-medium">이미지를 준비하지 못했습니다</p>
          </div>
        )}
      </div>

      <div className="space-y-4 p-5 sm:p-6">
        <div className="flex flex-wrap gap-2">
          <span className="rounded-full bg-slate-950 px-3 py-1 text-xs font-semibold text-white">
            {mallLabel}
          </span>
          <span className="rounded-full bg-sky-100 px-3 py-1 text-xs font-semibold text-sky-800">
            {product.parserUsed}
          </span>
          {product.fallbackUsed ? (
            <span className="rounded-full bg-amber-100 px-3 py-1 text-xs font-semibold text-amber-800">
              OG 폴백
            </span>
          ) : null}
          {hasPartialFields ? (
            <span className="rounded-full bg-rose-100 px-3 py-1 text-xs font-semibold text-rose-700">
              부분 파싱
            </span>
          ) : null}
        </div>

        <div className="space-y-2">
          <h3 className="line-clamp-2 min-h-16 text-xl font-semibold leading-8 text-slate-950">
            {product.name ?? "상품명을 불러오지 못했습니다"}
          </h3>
          <p className="text-2xl font-semibold tracking-tight text-slate-950">
            {formatPrice(product)}
          </p>
        </div>

        {product.sourceUrl ? (
          <a
            className="inline-flex items-center text-sm font-medium text-sky-700 hover:text-sky-900"
            href={product.sourceUrl}
            target="_blank"
            rel="noreferrer"
          >
            원본 상품 페이지 열기
          </a>
        ) : (
          <p className="text-sm font-medium text-slate-400">원본 상품 링크를 제공하지 않았습니다</p>
        )}
      </div>
    </article>
  );
}
