import { describe, expect, it } from "vitest";
import {
  buildExperimentTestUrl,
  buildPdeInternalPreviewUrl,
  canAccessExperimentConstruction,
  canManageGeraSalesPage,
  canStartDirectExperiment,
  experimentDetailTabs,
  getVisibleExperimentDetailTabs,
  resolveGeraSalesPageCommand,
  supportsTraditionalLandingForExperiment,
} from "./ExperimentDetailPage";

describe("canManageGeraSalesPage", () => {
  it("exibe criação e auditoria para experimento persistido", () => {
    expect(canManageGeraSalesPage(84)).toBe(true);
  });

  it("não exibe comandos sem experimento persistido", () => {
    expect(canManageGeraSalesPage(null)).toBe(false);
  });
});

describe("supportsTraditionalLandingForExperiment", () => {
  it("mantém GeraSalesPage fora do PDE e disponível para landing tradicional", () => {
    expect(
      supportsTraditionalLandingForExperiment(
        "PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL",
      ),
    ).toBe(false);
    expect(supportsTraditionalLandingForExperiment("LEAD_GENERATION")).toBe(
      true,
    );
    expect(supportsTraditionalLandingForExperiment(undefined)).toBe(false);
  });
});

describe("resolveGeraSalesPageCommand", () => {
  it("sempre inicia uma rodada auditavel nova, mesmo sem publicação anterior", () => {
    expect(resolveGeraSalesPageCommand()).toBe("rebuild");
  });
});

describe("canStartDirectExperiment", () => {
  it("libera somente o canal individual planejado com todos os gates aprovados", () => {
    expect(canStartDirectExperiment("PLANNED", "DIRECT_ONE_TO_ONE", true)).toBe(
      true,
    );
    expect(canStartDirectExperiment("PLANNED", "FACEBOOK", true)).toBe(false);
    expect(
      canStartDirectExperiment("PLANNED", "DIRECT_ONE_TO_ONE", false),
    ).toBe(false);
    expect(canStartDirectExperiment("RUNNING", "DIRECT_ONE_TO_ONE", true)).toBe(
      false,
    );
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

describe("ExperimentDetailPage", () => {
  it("mantém o run e o preflight acessíveis pela navegação", () => {
    expect(experimentDetailTabs).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          value: "execucao",
          label: "Preflight e runs",
          group: "audit",
        }),
      ]),
    );
  });

  it("mantém o painel operacional do GeraLanding acessível pela navegação", () => {
    expect(experimentDetailTabs).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          value: "gera-landing",
          label: "GeraLanding",
          group: "planning",
        }),
      ]),
    );
  });

  it("mostra somente áreas aplicáveis ao experimento PDE no Facebook", () => {
    const tabs = getVisibleExperimentDetailTabs({
      creationSource: "MANUAL_FLOW",
      productAiSubtype: null,
      experimentType: "PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL",
      platform: "FACEBOOK",
    });

    expect(tabs.map((item) => item.value)).toEqual([
      "funnel",
      "campaign",
      "post-deploy",
      "analytics",
      "creatives",
      "video",
      "publico",
      "construction",
      "content-structure",
      "execucao",
      "process",
      "history",
    ]);
    expect(tabs.find((item) => item.value === "analytics")?.label).toBe(
      "Comportamento PDE",
    );
  });

  it("retira do PDE ferramentas de landing tradicional que contaminariam a operação", () => {
    const tabs = getVisibleExperimentDetailTabs({
      creationSource: "MANUAL_FLOW",
      experimentType: "PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL",
      platform: "FACEBOOK",
    });

    expect(tabs.map((item) => item.value)).not.toEqual(
      expect.arrayContaining(["landing", "gera-landing", "ab-test"]),
    );
  });
});
