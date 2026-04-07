import { describe, expect, it } from "vitest";

import {
  hasLandingLayoutContent,
  parseLandingLayoutPayload,
} from "./landingLayoutParser";

describe("landingLayoutParser", () => {
  it("maps section order with metadata", () => {
    const raw = JSON.stringify({
      landingPageWireframe: {
        pageGoal: "Formulário preenchido",
        variantLayoutId: "form-first",
        sectionOrder: [
          {
            sectionName: "Hero",
            objective: "Reforçar promessa",
            mobilePriorityScore: 10,
            dropOffRisk: "baixo",
          },
        ],
        mobilePriorityNotes: "Garantir CTA acima da dobra",
      },
    });

    const parsed = parseLandingLayoutPayload(raw);
    expect(hasLandingLayoutContent(parsed)).toBe(true);
    expect(parsed?.sectionOrder?.[0]?.sectionName).toBe("Hero");
    expect(parsed?.variantLayoutId).toBe("form-first");
  });

  it("returns undefined for payloads without structure", () => {
    const parsed = parseLandingLayoutPayload('{"wireframe":"texto livre"}');
    expect(parsed).toBeUndefined();
  });

  it("reads artifact-oriented envelope", () => {
    const raw = JSON.stringify({
      artifact: {
        artifactType: "experiment.landing.layout",
        content: {
          pageGoal: "Gerar lead qualificado",
          variantLayoutId: "proof-first",
        },
      },
    });
    const parsed = parseLandingLayoutPayload(raw);
    expect(parsed?.variantLayoutId).toBe("proof-first");
  });
});
