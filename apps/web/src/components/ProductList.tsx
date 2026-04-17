import type { ParseResponse } from "@linkcart/shared";
import { ProductCard } from "./ProductCard";

type ProductListProps = {
  buildImageSrc: (imageUrl?: string | null) => string | null;
  isHydrated: boolean;
  products: ParseResponse[];
};

export function ProductList({
  buildImageSrc,
  isHydrated,
  products,
}: ProductListProps) {
  if (!isHydrated) {
    return (
      <section className="rounded-[32px] border border-white/60 bg-white/75 p-8 shadow-[0_24px_80px_rgba(15,23,42,0.08)] backdrop-blur">
        <p className="text-sm font-medium uppercase tracking-[0.28em] text-sky-700">
          Product List
        </p>
        <p className="mt-4 text-base leading-7 text-slate-600">
          브라우저에 저장된 이전 결과를 불러오고 있습니다.
        </p>
      </section>
    );
  }

  if (products.length === 0) {
    return (
      <section className="rounded-[32px] border border-dashed border-slate-300 bg-white/70 p-8 shadow-[0_24px_80px_rgba(15,23,42,0.06)] backdrop-blur">
        <p className="text-sm font-medium uppercase tracking-[0.28em] text-sky-700">
          Product List
        </p>
        <h2 className="mt-4 text-2xl font-semibold tracking-tight text-slate-950">
          아직 수집한 상품이 없습니다.
        </h2>
        <p className="mt-3 max-w-2xl text-base leading-7 text-slate-600">
          첫 번째 링크를 추가하면 파싱 결과가 카드로 쌓이고, 브라우저를 새로고침해도 같은 리스트를 다시 볼 수 있습니다.
        </p>
      </section>
    );
  }

  return (
    <section className="space-y-5">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-medium uppercase tracking-[0.28em] text-sky-700">
            Product List
          </p>
          <h2 className="mt-2 text-3xl font-semibold tracking-tight text-slate-950">
            저장된 상품 {products.length}건
          </h2>
        </div>
        <p className="max-w-xl text-sm leading-6 text-slate-600">
          최근에 추가한 상품이 먼저 보입니다. 이미지가 막힌 경우에는 서버 프록시를 거친 결과만 보여줍니다.
        </p>
      </div>

      <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
        {products.map((product) => (
          <ProductCard
            key={product.source_url ?? `${product.parser_used}-${product.name ?? "unknown"}`}
            imageSrc={buildImageSrc(product.image_url)}
            product={product}
          />
        ))}
      </div>
    </section>
  );
}
