import { afterEach, describe, expect, it, vi } from "vitest";
import {
  createMessageId,
  fashionChatMessageEndpoint,
  shouldRenderFashionSketch,
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

describe("shouldRenderFashionSketch", () => {
  it("mostra croqui para resposta funcional de moda", () => {
    expect(
      shouldRenderFashionSketch({
        id: "1",
        role: "assistant",
        text: "Use um vestido elegante com detalhe floral vermelho.",
      }),
    ).toBe(true);
  });

  it("nao mostra croqui para erro tecnico", () => {
    expect(
      shouldRenderFashionSketch({
        id: "2",
        role: "assistant",
        text: "Nao consegui acionar o modulo de moda agora. Detalhe: HTTP 502",
      }),
    ).toBe(false);
  });
});
