import { describe, expect, it } from "vitest";
import { apiBaseUrl, resolveDefaultApiBaseUrl } from "./api";

describe("apiBaseUrl", () => {
  it("preserva a porta da origem atual quando não existe URL configurada", () => {
    expect(apiBaseUrl).toBe(window.location.origin);
  });

  it("mantém o contrato produtivo do backend na porta padrão", () => {
    expect(
      resolveDefaultApiBaseUrl(
        {
          origin: "http://191.252.181.168:5173",
          protocol: "http:",
          hostname: "191.252.181.168",
        },
        false,
      ),
    ).toBe("http://191.252.181.168");
  });

  it("usa a origem completa quando o proxy local está ativo", () => {
    expect(
      resolveDefaultApiBaseUrl(
        {
          origin: "http://127.0.0.1:4173",
          protocol: "http:",
          hostname: "127.0.0.1",
        },
        true,
      ),
    ).toBe("http://127.0.0.1:4173");
  });
});
