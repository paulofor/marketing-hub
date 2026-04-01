import { describe, expect, it } from "vitest";
import { hasAdCopyContent, parseAdCopyPayload } from "./adCopyParser";

describe("adCopyParser", () => {
  it("parseia payload direto com primaryTextVariants", () => {
    const payload = JSON.stringify({
      primaryTextVariants: [
        {
          label: "dor",
          primaryText: "Seu lead some no WhatsApp?",
          headline: "Reative em 7 dias",
          description: "Fluxo automático com IA",
          ctaText: "Gerar prévia",
        },
      ],
    });

    const result = parseAdCopyPayload(payload);

    expect(result?.primaryTextVariants).toHaveLength(1);
    expect(result?.primaryTextVariants[0]?.label).toBe("dor");
    expect(result?.primaryTextVariants[0]?.ctaText).toBe("Gerar prévia");
    expect(hasAdCopyContent(result)).toBe(true);
  });

  it("parseia JSON com content stringificado", () => {
    const payload = JSON.stringify({
      content:
        '{"primaryTextVariants":[{"label":"resultado","primaryText":"Agenda cheia sem desconto","headline":"Mantenha 70% ativos","description":"Mensagens prontas por IA","ctaText":"Ver roteiro"}]}',
    });

    const result = parseAdCopyPayload(payload);

    expect(result?.primaryTextVariants).toHaveLength(1);
    expect(result?.primaryTextVariants[0]?.label).toBe("resultado");
    expect(result?.primaryTextVariants[0]?.headline).toBe("Mantenha 70% ativos");
  });

  it("retorna undefined quando não há bloco estruturado", () => {
    expect(parseAdCopyPayload("apenas texto livre")).toBeUndefined();
  });
});
