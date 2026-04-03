import { describe, expect, it } from "vitest";

import { createMockRenderAdImageInput } from "./mockData";
import {
  RenderAdImagePayloadError,
  renderAdImagePayloads,
} from "./renderAdImagePayloads";

describe("renderAdImagePayloads", () => {
  it("gera payloads completos para variantes dor, resultado e prova", () => {
    const input = createMockRenderAdImageInput();
    const result = renderAdImagePayloads(input);

    expect(result.imageRenderPayloads).toHaveLength(3);

    const dor = result.imageRenderPayloads.find((payload) => payload.label === "dor");
    expect(dor).toBeDefined();
    expect(dor?.placement).toBe("feed");
    expect(dor?.imageParams.size).toBe("1024x1536");
    expect(dor?.overlayCopy.badge).toBe("");
    expect(dor?.overlayCopy.cta).toBe("Saiba mais");
    expect(dor?.imagePrompt).toContain("Instagram/Meta");
    expect(dor?.imagePrompt).toContain("1 foco visual");
    expect(dor?.imagePrompt).toContain("Evitar");
  });

  it("configura corretamente stories para a variante de resultado", () => {
    const input = createMockRenderAdImageInput();
    const result = renderAdImagePayloads(input);


    const stories = result.imageRenderPayloads.find((payload) => payload.label === "resultado");
    expect(stories).toBeDefined();
    expect(stories?.placement).toBe("stories");
    expect(stories?.imageParams.size).toBe("1024x1792");
    expect(stories?.assetId).toContain("stories");
    expect(stories?.imagePrompt).toContain("Stories/Reels");
  });

  it("mantém consistência para a variante de prova", () => {
    const input = createMockRenderAdImageInput();
    const result = renderAdImagePayloads(input);

    const proof = result.imageRenderPayloads.find((payload) => payload.label === "prova");
    expect(proof).toBeDefined();
    expect(proof?.consistency.singleMindedPromise).toBe(
      input.campaignAngle.singleMindedPromise,
    );
    expect(proof?.consistency.ctaMatch).toBe("Saiba mais");
    expect(proof?.experimentMetadata.asset_role).toBe("ad-image-render");
    expect(proof?.imagePrompt).toContain("nicho");
  });

  it("lança erro quando falta CTA principal", () => {
    const input = createMockRenderAdImageInput();
    input.campaignAngle.primaryCTA = undefined;
    if (input.adImageBriefing.adToLandingConsistency) {
      input.adImageBriefing.adToLandingConsistency.ctaMatch = undefined;
    }

    try {
      renderAdImagePayloads(input);
      expect.fail("Deveria ter lançado erro de validação");
    } catch (error) {
      expect(error).toBeInstanceOf(RenderAdImagePayloadError);
      const typed = error as RenderAdImagePayloadError;
      expect(typed.issues.some((issue) => issue.toLowerCase().includes("cta"))).toBe(true);
    }
  });
});
