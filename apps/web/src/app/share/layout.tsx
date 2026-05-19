import type { Metadata } from "next";
import type { ReactNode } from "react";

export const metadata: Metadata = {
  title: "공유 리스트 · Linkcart",
  description: "Linkcart에서 공유된 상품 리스트",
};

export default function ShareLayout({ children }: { children: ReactNode }) {
  return children;
}
