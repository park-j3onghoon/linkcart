import { beforeEach, describe, expect, it } from "vitest";
import {
  createMemoryTokenStorage,
  createWebTokenStorage,
} from "@linkcart/shared";

describe("createMemoryTokenStorage", () => {
  it("returns null before tokens are set", () => {
    const storage = createMemoryTokenStorage();
    expect(storage.getAccessToken()).toBeNull();
    expect(storage.getRefreshToken()).toBeNull();
  });

  it("stores and retrieves tokens", () => {
    const storage = createMemoryTokenStorage();
    storage.setTokens({ access_token: "A", refresh_token: "R" });
    expect(storage.getAccessToken()).toBe("A");
    expect(storage.getRefreshToken()).toBe("R");
  });

  it("clears tokens", () => {
    const storage = createMemoryTokenStorage();
    storage.setTokens({ access_token: "A", refresh_token: "R" });
    storage.clear();
    expect(storage.getAccessToken()).toBeNull();
    expect(storage.getRefreshToken()).toBeNull();
  });
});

describe("createWebTokenStorage", () => {
  let backing: Map<string, string>;
  let web: ReturnType<typeof createWebTokenStorage>;

  beforeEach(() => {
    backing = new Map();
    web = createWebTokenStorage({
      getItem: (k) => backing.get(k) ?? null,
      setItem: (k, v) => void backing.set(k, v),
      removeItem: (k) => void backing.delete(k),
    });
  });

  it("persists tokens into the backing storage", () => {
    web.setTokens({ access_token: "A", refresh_token: "R" });
    expect(backing.get("linkcart.auth.access_token")).toBe("A");
    expect(backing.get("linkcart.auth.refresh_token")).toBe("R");
  });

  it("reads tokens back from backing storage", () => {
    backing.set("linkcart.auth.access_token", "X");
    backing.set("linkcart.auth.refresh_token", "Y");
    expect(web.getAccessToken()).toBe("X");
    expect(web.getRefreshToken()).toBe("Y");
  });

  it("clear removes both keys", () => {
    backing.set("linkcart.auth.access_token", "X");
    backing.set("linkcart.auth.refresh_token", "Y");
    web.clear();
    expect(backing.has("linkcart.auth.access_token")).toBe(false);
    expect(backing.has("linkcart.auth.refresh_token")).toBe(false);
  });
});
