import { describe, expect, it } from "vitest";
import { resolveLandingHtml } from "./LandingTab";

describe("LandingTab", () => {
  it("usa htmlGeraLanding como HTML suficiente para liberar a aprovação", () => {
    expect(
      resolveLandingHtml({
        htmlGeraLanding: "<html><body>GeraLanding</body></html>",
        landingPageHtml: null,
      }),
    ).toBe("<html><body>GeraLanding</body></html>");
  });

  it("mantém landingPageHtml como fallback legado", () => {
    expect(
      resolveLandingHtml({
        htmlGeraLanding: null,
        landingPageHtml: "<html><body>Landing legado</body></html>",
      }),
    ).toBe("<html><body>Landing legado</body></html>");
  });

  it("bloqueia aprovação quando ambos os campos de HTML estão vazios", () => {
    expect(
      resolveLandingHtml({ htmlGeraLanding: "   ", landingPageHtml: null }),
    ).toBeNull();
  });
});
