import "@testing-library/jest-dom/vitest";
import React from "react";
import { vi } from "vitest";

const storage = new Map<string, string>();

vi.mock("next/image", () => ({
  default: (
    props: React.ImgHTMLAttributes<HTMLImageElement> & {
      blurDataURL?: string;
      fill?: boolean;
      loader?: unknown;
      placeholder?: string;
      src: string;
      unoptimized?: boolean;
    },
  ) => {
    const imageProps = { ...props };

    Reflect.deleteProperty(imageProps, "blurDataURL");
    Reflect.deleteProperty(imageProps, "fill");
    Reflect.deleteProperty(imageProps, "loader");
    Reflect.deleteProperty(imageProps, "placeholder");
    Reflect.deleteProperty(imageProps, "unoptimized");

    return React.createElement("img", imageProps);
  },
}));

Object.defineProperty(window, "localStorage", {
  value: {
    clear() {
      storage.clear();
    },
    getItem(key: string) {
      return storage.has(key) ? storage.get(key)! : null;
    },
    key(index: number) {
      return Array.from(storage.keys())[index] ?? null;
    },
    removeItem(key: string) {
      storage.delete(key);
    },
    setItem(key: string, value: string) {
      storage.set(key, value);
    },
    get length() {
      return storage.size;
    },
  },
  configurable: true,
});
