import { describe, expect, it } from "vitest";

import {
  hasLandingImagePlanningContent,
  parseLandingImagePlanningPayload,
} from "./landingImagePlanningParser";

describe("landingImagePlanningParser", () => {
  it("maps artifact envelope with planned images", () => {
    const raw = JSON.stringify({
      artifact: {
        artifactType: "experiment.landing.image_plan",
        content: {
          pageGoal: "Explicar oferta e converter no formulário",
          visualDirectionSummary: "Visual com prova social e CTA repetido",
          images: [
            {
              sectionId: "hero",
              sectionName: "Hero",
              placement: "hero",
              objective: "Reforçar promessa",
              imagePrompt: "Personal trainer com celular e dashboard",
              dimensions: {
                desktop: { width: "1440", height: "900" },
                mobile: { width: "720", height: "1080" },
              },
              image: {
                url: "https://cdn.exemplo.com/hero.jpg",
                altText: "Personal trainer usando dashboard",
              },
            },
          ],
        },
      },
    });

    const parsed = parseLandingImagePlanningPayload(raw);
    expect(hasLandingImagePlanningContent(parsed)).toBe(true);
    expect(parsed?.images[0]?.imageUrl).toBe(
      "https://cdn.exemplo.com/hero.jpg",
    );
    expect(parsed?.images[0]?.desktopDimensions).toBe("1440x900");
  });

  it("returns undefined when no structured image plan exists", () => {
    const parsed = parseLandingImagePlanningPayload('{"images":"texto"}');
    expect(parsed).toBeUndefined();
  });
});
