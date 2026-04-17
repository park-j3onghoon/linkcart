"use client";

import { createApiClient, validateUrl, type ParseResponse } from "@linkcart/shared";
import { startTransition, useState } from "react";
import { useLocalStorage } from "./useLocalStorage";

const STORAGE_KEY = "linkcart.products.v1";
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "";

const apiClient = createApiClient(API_BASE_URL);

export type Feedback = {
  kind: "error" | "warning" | "success";
  text: string;
};

export type ParsePhase = "idle" | "validating" | "parsing" | "success" | "error";

export function useProducts() {
  const {
    value: products,
    setValue: setProducts,
    isHydrated,
  } = useLocalStorage<ParseResponse[]>(STORAGE_KEY, []);
  const [phase, setPhase] = useState<ParsePhase>("idle");
  const [feedback, setFeedback] = useState<Feedback | null>(null);

  async function submitUrl(rawUrl: string): Promise<boolean> {
    const normalizedUrl = rawUrl.trim();
    const validation = validateUrl(normalizedUrl);

    if (!validation.valid) {
      setPhase("error");
      setFeedback({
        kind: "error",
        text: validation.error ?? "올바른 URL을 입력해주세요",
      });
      return false;
    }

    setFeedback(null);
    setPhase("validating");

    if (products.some((product) => product.source_url === normalizedUrl)) {
      setPhase("idle");
      setFeedback({
        kind: "warning",
        text: "이미 추가한 링크입니다. 기존 카드에서 결과를 확인해주세요.",
      });
      return false;
    }

    setPhase("parsing");
    const result = await apiClient.parseProduct(normalizedUrl);

    if (!result.ok) {
      setPhase("error");
      setFeedback({ kind: "error", text: result.error.message });
      return false;
    }

    startTransition(() => {
      setProducts((currentProducts) => {
        if (currentProducts.some((product) => product.source_url === result.data.source_url)) {
          return currentProducts;
        }
        return [result.data, ...currentProducts];
      });
    });

    setPhase("success");
    setFeedback({ kind: "success", text: "상품 카드를 리스트에 추가했습니다." });
    return true;
  }

  function buildImageSrc(imageUrl?: string | null): string | null {
    if (!imageUrl) {
      return null;
    }

    return apiClient.imageProxyUrl(imageUrl);
  }

  return {
    buildImageSrc,
    feedback,
    isHydrated,
    isWorking: phase === "validating" || phase === "parsing",
    phase,
    products,
    submitUrl,
  };
}
