import { describe, expect, it } from "vitest";
import {
  buildExperimentTestUrl,
  buildPdeInternalPreviewUrl,
} from "./ExperimentDetailPage";

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

describe("buildPdeInternalPreviewUrl", () => {
  it("monta link interno de QA sem contaminar analytics PDE", () => {
    expect(
      buildPdeInternalPreviewUrl(
        "https://v5.clubemusa.com.br/login?ref=meta",
        74,
        "musa-pde-entry-v5-estrada-desejo",
      ),
    ).toBe(
      "https://v5.clubemusa.com.br/login?ref=meta&mh_preview=qa&pde_analytics=off&utm_source=internal&utm_medium=qa&utm_campaign=experiment_74_pde_preview_qa&utm_content=musa-pde-entry-v5-estrada-desejo",
    );
  });

  it("retorna nulo para URL PDE vazia ou invalida", () => {
    expect(buildPdeInternalPreviewUrl(" ", 74, "musa-pde-entry-v5")).toBeNull();
    expect(
      buildPdeInternalPreviewUrl("clubemusa.com.br", 74, "musa-pde-entry-v5"),
    ).toBeNull();
  });
});
