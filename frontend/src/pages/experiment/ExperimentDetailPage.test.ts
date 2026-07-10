import { describe, expect, it } from "vitest";
import { buildExperimentTestUrl } from "./ExperimentDetailPage";

describe("buildExperimentTestUrl", () => {
  it("adiciona o parametro de teste em URL sem query string", () => {
    expect(buildExperimentTestUrl("https://vendas.exemplo.com/oferta")).toBe(
      "https://vendas.exemplo.com/oferta?mh_test=1",
    );
  });

  it("preserva parametros existentes e define o modo de teste", () => {
    expect(
      buildExperimentTestUrl(
        "https://vendas.exemplo.com/oferta?utm_source=meta",
      ),
    ).toBe("https://vendas.exemplo.com/oferta?utm_source=meta&mh_test=1");
  });

  it("retorna nulo quando nao existe URL publicada", () => {
    expect(buildExperimentTestUrl(" ")).toBeNull();
  });
});
