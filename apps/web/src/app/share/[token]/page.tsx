import { createApiClient } from "@linkcart/shared";
import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ShareItemCard } from "../../../components/ShareItemCard";

const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

export const metadata: Metadata = {
  title: "공유 리스트 · Linkcart",
  description: "Linkcart에서 공유된 상품 리스트",
};

type SharePageProps = {
  params: Promise<{ token: string }>;
};

function formatDate(iso: string | null | undefined): string | null {
  if (!iso) return null;
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return null;
  return new Intl.DateTimeFormat("ko-KR", { dateStyle: "long" }).format(date);
}

export default async function SharePage({ params }: SharePageProps) {
  const { token } = await params;
  const api = createApiClient(BACKEND_URL);
  const result = await api.lookupShareListByToken(token);

  if (!result.ok) {
    if (result.error.code === "NOT_FOUND") {
      notFound();
    }
    throw new Error(result.error.message);
  }

  const shareList = result.data;
  const createTime = formatDate(shareList.createTime);

  return (
    <main className="mx-auto flex w-full max-w-6xl flex-col gap-8 px-5 py-10 sm:px-8 lg:px-10 lg:py-16">
      <header className="rounded-[32px] border border-white/65 bg-white/80 p-8 shadow-[0_28px_90px_rgba(15,23,42,0.1)] backdrop-blur">
        <p className="text-sm font-medium uppercase tracking-[0.35em] text-sky-700">
          Shared List
        </p>
        <h1 className="mt-4 text-4xl font-semibold tracking-tight text-slate-950 sm:text-5xl">
          {shareList.title ?? "공유된 상품 리스트"}
        </h1>
        <p className="mt-4 text-sm text-slate-600">
          총 {shareList.items.length}개 상품
          {createTime ? ` · ${createTime} 공유` : null}
        </p>
      </header>

      {shareList.items.length === 0 ? (
        <p className="rounded-[28px] border border-slate-200 bg-white p-8 text-center text-slate-500">
          공유된 상품이 없습니다.
        </p>
      ) : (
        <section
          className="grid gap-6 md:grid-cols-2 lg:grid-cols-3"
          data-testid="share-item-list"
        >
          {shareList.items.map((item) => (
            <ShareItemCard
              key={item.name}
              item={item}
              imageSrc={
                item.imageUrl
                  ? `${BACKEND_URL}/api/v1/images:proxy?url=${encodeURIComponent(item.imageUrl)}`
                  : null
              }
            />
          ))}
        </section>
      )}
    </main>
  );
}
