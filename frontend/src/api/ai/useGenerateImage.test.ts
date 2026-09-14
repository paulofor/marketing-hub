import { afterEach, expect, it, vi } from "vitest";
import { createImageOperationKey } from "./useGenerateImage";

afterEach(() => {
  vi.unstubAllGlobals();
});

it("creates distinct correlation keys without the HTTPS-only randomUUID method", () => {
  const getRandomValues = crypto.getRandomValues.bind(crypto);
  vi.stubGlobal("crypto", { getRandomValues });
  const keys = Array.from({ length: 100 }, () => createImageOperationKey());
  expect(new Set(keys).size).toBe(100);
  expect(keys.every((key) => /^[a-zA-Z0-9._:-]{1,100}$/.test(key))).toBe(true);
});
