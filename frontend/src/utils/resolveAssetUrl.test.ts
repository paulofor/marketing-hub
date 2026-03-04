import { describe, expect, it } from "vitest";

import { resolveAssetUrl } from "./resolveAssetUrl";

describe("resolveAssetUrl", () => {
  it("normaliza caminhos salvos com prefixo /api/uploads para o endpoint estático do backend", () => {
    expect(resolveAssetUrl("/api/uploads/imagem.png")).toBe(
      "http://localhost:8000/uploads/imagem.png",
    );
  });

  it("mantém caminhos relativos funcionando como antes", () => {
    expect(resolveAssetUrl("uploads/imagem.png")).toBe(
      "http://localhost:8000/uploads/imagem.png",
    );
  });
});
