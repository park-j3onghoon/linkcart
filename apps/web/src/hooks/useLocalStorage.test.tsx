import { act, renderHook, waitFor } from "@testing-library/react";
import { useLocalStorage } from "./useLocalStorage";

describe("useLocalStorage", () => {
  const storageKey = "linkcart.products.test";

  beforeEach(() => {
    window.localStorage.clear();
  });

  it("returns the initial value when storage is empty", async () => {
    const { result } = renderHook(() => useLocalStorage(storageKey, ["initial"]));

    await waitFor(() => {
      expect(result.current.isHydrated).toBe(true);
    });

    expect(result.current.value).toEqual(["initial"]);
  });

  it("restores saved values after a remount", async () => {
    const { result, unmount } = renderHook(() => useLocalStorage(storageKey, ["initial"]));

    await waitFor(() => {
      expect(result.current.isHydrated).toBe(true);
    });

    act(() => {
      result.current.setValue(["saved"]);
    });

    await waitFor(() => {
      expect(window.localStorage.getItem(storageKey)).toBe(JSON.stringify(["saved"]));
    });

    unmount();

    const remounted = renderHook(() => useLocalStorage(storageKey, ["initial"]));

    await waitFor(() => {
      expect(remounted.result.current.isHydrated).toBe(true);
    });

    expect(remounted.result.current.value).toEqual(["saved"]);
  });

  it("falls back to the initial value when stored JSON is corrupted", async () => {
    window.localStorage.setItem(storageKey, "{broken");

    const { result } = renderHook(() => useLocalStorage(storageKey, ["fallback"]));

    await waitFor(() => {
      expect(result.current.isHydrated).toBe(true);
    });

    expect(result.current.value).toEqual(["fallback"]);
    expect(window.localStorage.getItem(storageKey)).toBe(JSON.stringify(["fallback"]));
  });
});
