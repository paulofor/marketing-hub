import { describe, expect, it } from "vitest";

import {
  hasLandingCopyContent,
  parseLandingCopyPayload,
} from "./landingCopyParser";

describe("landingCopyParser", () => {
  it("extracts short and long versions when available", () => {
    const raw = JSON.stringify({
      landingPageCopy: {
        messageMatchSource: "Headline A",
        landingCurta: {
          heroPromise: "Promessa direta",
          heroTitle: "Título curto",
          heroBullets: ["Benefício 1", "Benefício 2"],
          primaryCTA: "Gerar amostra",
          formMicrocopy: {
            headline: "Preencha para receber",
          },
          benefitsSection: {
            title: "Benefícios principais",
            bullets: ["Automação", "Escala"],
          },
        },
        landingCompleta: {
          heroPromise: "Transformação detalhada",
          proofSection: {
            title: "Prova social",
            description: "Mais de 2.000 clientes",
          },
          faqSection: [
            { question: "Serve para mim?", answer: "Sim, porque..." },
          ],
        },
      },
    });

    const parsed = parseLandingCopyPayload(raw);
    expect(hasLandingCopyContent(parsed)).toBe(true);
    expect(parsed?.messageMatchSource).toBe("Headline A");
    expect(parsed?.landingCurta?.benefitsSection?.bullets).toHaveLength(2);
    expect(parsed?.landingCompleta?.faqSection?.[0]?.question).toContain("Serve");
  });

  it("returns undefined when no recognizable landing structure is present", () => {
    const parsed = parseLandingCopyPayload("{\"landing\":\"texto solto\"}");
    expect(parsed).toBeUndefined();
  });
});
