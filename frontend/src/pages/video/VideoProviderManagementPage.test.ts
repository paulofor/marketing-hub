import { describe, expect, it } from "vitest";
import { SalesVideoProviderOption } from "../../api/salesVideo/videoProviderCatalog";
import { SalesVideoProviderScore } from "../../api/salesVideo/types";
import { compareProviderRows } from "./VideoProviderManagementPage";

function provider(label: string): SalesVideoProviderOption {
  return {
    key: label.toLowerCase(),
    label,
    providerName: label.toUpperCase(),
    providerFamily: "EXTERNAL_VIDEO_MODULE",
    recommendedUse: "Teste",
    clipDurationSeconds: 10,
    supportsHeroVideo: true,
    supportsSceneAssembly: true,
    supportsOpenAiReferenceImage: false,
  };
}

function score(
  providerName: string,
  overrides: Partial<SalesVideoProviderScore>,
): SalesVideoProviderScore {
  return {
    providerName,
    score: 0,
    readyJobs: 0,
    failedJobs: 0,
    operationalFailedJobs: 0,
    approvedAssets: 0,
    rejectedAssets: 0,
    leads: 0,
    qualifiedLeads: 0,
    checkoutStarts: 0,
    purchases: 0,
    revenue: 0,
    recommendation: "bloquear_ou_regenerar",
    ...overrides,
  };
}

describe("VideoProviderManagementPage", () => {
  it("ordena provedores do melhor score para o pior e deixa sem historico no fim", () => {
    const rows = [
      { option: provider("Luma"), score: score("LUMA", { score: 0 }) },
      { option: provider("Kling"), score: score("KLING", { score: 38 }) },
      { option: provider("Runway") },
      { option: provider("Veo"), score: score("VEO", { score: 12 }) },
    ];

    expect(rows.sort(compareProviderRows).map((row) => row.option.label)).toEqual([
      "Kling",
      "Veo",
      "Luma",
      "Runway",
    ]);
  });

  it("usa sinais comerciais como desempate quando o score e igual", () => {
    const rows = [
      {
        option: provider("Provider A"),
        score: score("A", { score: 50, revenue: 100, purchases: 1 }),
      },
      {
        option: provider("Provider B"),
        score: score("B", { score: 50, revenue: 200, purchases: 0 }),
      },
    ];

    expect(rows.sort(compareProviderRows).map((row) => row.option.label)).toEqual([
      "Provider B",
      "Provider A",
    ]);
  });
});
