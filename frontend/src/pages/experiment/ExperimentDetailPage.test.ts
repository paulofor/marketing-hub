import { describe, expect, it } from "vitest";
import {
  buildExperimentTestUrl,
  buildPdeInternalPreviewUrl,
  canManageGeraSalesPage,
  canAccessExperimentConstruction,
  resolveGeraSalesPageCommand,
} from "./ExperimentDetailPage";

describe("canManageGeraSalesPage", () => {
  it("exibe criação e auditoria para experimento persistido", () => {
    expect(canManageGeraSalesPage(84)).toBe(true);
  });

  it("não exibe comandos sem experimento persistido", () => {
    expect(canManageGeraSalesPage(null)).toBe(false);
  });
});

describe("resolveGeraSalesPageCommand", () => {
  it("sempre inicia uma rodada auditavel nova, mesmo sem publicação anterior", () => {
    expect(resolveGeraSalesPageCommand()).toBe("rebuild");
  });
});

describe("canAccessExperimentConstruction", () => {
  it("exibe a construção para experimento manual", () => {
    expect(canAccessExperimentConstruction("MANUAL_FLOW", null)).toBe(true);
  });

  it("exibe a construção para microamostra criada pelo fluxo normal", () => {
    expect(
      canAccessExperimentConstruction("SYSTEM_FLOW", "AI_PERSONALIZED_SAMPLE"),
    ).toBe(true);
  });

  it("mantém a construção oculta para outros experimentos automáticos", () => {
    expect(
      canAccessExperimentConstruction("SYSTEM_FLOW", "AI_VISUAL_ASSET_PACK"),
    ).toBe(false);
  });
});

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
