import { describe, expect, it } from "vitest";
import { canAttemptLandingApproval, resolveLandingHtml } from "./LandingTab";

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

  it("não encontra prévia quando ambos os campos de HTML estão vazios", () => {
    expect(
      resolveLandingHtml({ htmlGeraLanding: "   ", landingPageHtml: null }),
    ).toBeNull();
  });

  it("permite tentar aprovação pelo backend quando ainda não há URL aprovada", () => {
    expect(
      canAttemptLandingApproval({ id: "35", followUpActionUrl: null }),
    ).toBe(true);
  });

  it("bloqueia aprovação quando o experimento não tem id", () => {
    expect(canAttemptLandingApproval({ id: "", followUpActionUrl: null })).toBe(
      false,
    );
  });

  it("bloqueia nova aprovação quando já existe URL oficial de campanha", () => {
    expect(
      canAttemptLandingApproval({
        id: "41",
        followUpActionUrl:
          "https://oportunidadebrasil.shop/api/flows/exp-41-landing-geralanding/page",
      }),
    ).toBe(false);
  });

  it("bloqueia cliques repetidos depois da publicação local", () => {
    expect(
      canAttemptLandingApproval({ id: "41", followUpActionUrl: null }, true),
    ).toBe(false);
  });
});
