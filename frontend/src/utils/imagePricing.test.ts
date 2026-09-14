import { describe, expect, it } from "vitest";

import { estimateImageUnitPriceUsd } from "./imagePricing";

describe("imagePricing", () => {
  it("estima Sunburst high para retrato pelo identificador da API", () => {
    expect(
      estimateImageUnitPriceUsd({
        modelName: "gpt-image-2.5-sunburst",
        qualityName: "high",
        width: 1024,
        height: 1536,
      }),
    ).toBe(0.04116);
  });

  it("estima Sunburst max quadrado pelo nome de exibição e orientação", () => {
    expect(
      estimateImageUnitPriceUsd({
        modelName: "GPT Image 2.5 Sunburst",
        qualityName: "max",
        orientation: "square",
      }),
    ).toBe(0.21072);
  });
});
