import { afterEach, describe, expect, it, vi } from "vitest";
import {
  createMessageId,
  fashionChatMessageEndpoint,
} from "./FashionChatPage";

describe("fashionChatMessageEndpoint", () => {
  it("usa o backend como ponto unico de contato do frontend", () => {
    expect(fashionChatMessageEndpoint).toBe("/api/fashion-chat/messages");
  });
});

describe("createMessageId", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("usa crypto.randomUUID quando disponivel", () => {
    vi.stubGlobal("crypto", {
      randomUUID: vi.fn(() => "uuid-do-navegador"),
    });

    expect(createMessageId()).toBe("uuid-do-navegador");
  });

  it("gera fallback quando crypto.randomUUID nao esta disponivel", () => {
    vi.stubGlobal("crypto", {});
    vi.spyOn(Date, "now").mockReturnValue(123456789);
    vi.spyOn(Math, "random").mockReturnValue(0.123456789);

    expect(createMessageId()).toMatch(/^msg-/);
  });
});
