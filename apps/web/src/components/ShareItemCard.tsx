"use client";

import type { ShareListItem } from "@linkcart/shared";
import Image from "next/image";
import { useState } from "react";

type ShareItemCardProps = {
  imageSrc: string | null;
  item: ShareListItem;
};

const mallLabels = {
  coupang: "쿠팡",
  elevenst: "11번가",
  generic: "일반 링크",
} as const;

function formatPrice(item: ShareListItem): string {
  const formatted = new Intl.NumberFormat("ko-KR").format(item.price.amount);
  return item.price.currency === "KRW" ? `${formatted}원` : `${formatted} ${item.price.currency}`;
}

export function ShareItemCard({ imageSrc, item }: ShareItemCardProps) {
  const [imageFailed, setImageFailed] = useState(false);

  return (
    <article
      data-testid="share-item-card"
      className="overflow-hidden rounded-[28px] border border-slate-200 bg-white shadow-[0_20px_60px_rgba(15,23,42,0.08)]"
    >
      <div className="relative flex aspect-[4/3] min-h-56 items-center justify-center overflow-hidden bg-[linear-gradient(135deg,#e0f2fe,#f8fafc,#ede9fe)] sm:min-h-64">
        {imageSrc && !imageFailed ? (
          <Image
            src={imageSrc}
            alt={`${item.name} 이미지`}
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
        <span className="inline-block rounded-full bg-slate-950 px-3 py-1 text-xs font-semibold text-white">
          {mallLabels[item.mall]}
        </span>

        <div className="space-y-2">
          <h3 className="line-clamp-2 min-h-16 text-xl font-semibold leading-8 text-slate-950">
            {item.name}
          </h3>
          <p className="text-2xl font-semibold tracking-tight text-slate-950">
            {formatPrice(item)}
          </p>
        </div>

        <a
          className="inline-flex items-center text-sm font-medium text-sky-700 hover:text-sky-900"
          href={item.sourceUrl}
          target="_blank"
          rel="noreferrer"
        >
          원본 상품 페이지 열기
        </a>
      </div>
    </article>
  );
}
