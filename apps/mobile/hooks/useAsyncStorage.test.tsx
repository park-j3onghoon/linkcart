import AsyncStorage from "@react-native-async-storage/async-storage";
import { renderHook, waitFor } from "@testing-library/react-native";
import { act } from "react-test-renderer";
import { useAsyncStorage } from "./useAsyncStorage";

describe("useAsyncStorage", () => {
  const storageKey = "linkcart.mobile.products.test";
  const storage = AsyncStorage as jest.Mocked<typeof AsyncStorage>;

  beforeEach(async () => {
    await storage.clear();
  });

  it("returns the initial value when storage is empty", async () => {
    const { result } = renderHook(() => useAsyncStorage(storageKey, ["initial"]));

    await waitFor(() => {
      expect(result.current.isHydrated).toBe(true);
    });

    expect(result.current.value).toEqual(["initial"]);
  });

  it("restores saved values after a remount", async () => {
    const { result, unmount } = renderHook(() => useAsyncStorage(storageKey, ["initial"]));

    await waitFor(() => {
      expect(result.current.isHydrated).toBe(true);
    });

    await act(async () => {
      result.current.setValue(["saved"]);
    });

    await waitFor(async () => {
      await expect(storage.getItem(storageKey)).resolves.toBe(JSON.stringify(["saved"]));
    });

    unmount();

    const remounted = renderHook(() => useAsyncStorage(storageKey, ["initial"]));

    await waitFor(() => {
      expect(remounted.result.current.isHydrated).toBe(true);
    });

    expect(remounted.result.current.value).toEqual(["saved"]);
  });

  it("falls back to the initial value when stored JSON is corrupted", async () => {
    await storage.setItem(storageKey, "{broken");

    const { result } = renderHook(() => useAsyncStorage(storageKey, ["fallback"]));

    await waitFor(() => {
      expect(result.current.isHydrated).toBe(true);
    });

    expect(result.current.value).toEqual(["fallback"]);
    await expect(storage.getItem(storageKey)).resolves.toBe(JSON.stringify(["fallback"]));
  });
});
