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


  it("normaliza URLs absolutas legadas com /api/uploads para o caminho estático", () => {
    expect(resolveAssetUrl("http://191.252.92.222:8000/api/uploads/imagem.png")).toBe(
      "http://191.252.92.222:8000/uploads/imagem.png",
    );
  });
});
