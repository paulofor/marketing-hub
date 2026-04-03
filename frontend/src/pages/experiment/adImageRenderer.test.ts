import { describe, expect, it } from "vitest";

import {
  RenderAdImagePayloadsInput,
  imageRenderMockExampleInput,
  renderAdImagePayloads,
} from "./adImageRenderer";

function buildBaseInput(): RenderAdImagePayloadsInput {
  return {
    experimentMetadata: {
      primary_variable: "10",
      variant_id: "V1",
      stage: "AD",
      control_or_treatment: "treatment",
    },
    campaignAngle: {
      singleMindedPromise: "Ganhar previsibilidade no volume de leads qualificados.",
      primaryCTA: "Agendar diagnóstico",
      landingMatchLine: "Diagnóstico com framework mostrado na landing.",
      audienceFilterLine: "Clínicas odontológicas que vendem implante.",
    },
    adCopy: {
      primaryTextVariants: [
        {
          label: "dor",
          headline: "Agenda vazia no implante?",
          description: "Preencha horários com previsibilidade",
          ctaText: "Agendar diagnóstico",
          placementHint: "feed",
        },
        {
          label: "resultado",
          headline: "Agenda cheia de implante",
          description: "Mais consultas qualificadas",
          ctaText: "Agendar diagnóstico",
          placementHint: "stories",
        },
        {
          label: "prova",
          headline: "Caso real de crescimento",
          description: "Resultados comprovados",
          ctaText: "Agendar diagnóstico",
          placementHint: "feed",
        },
      ],
    },
    adImageBriefing: {
      briefings: [
        {
          variantId: "V1",
          mustMatchAdVariant: "dor",
          formatByPlacement: "feed",
          concept: { idea: "Dentista olhando agenda vazia no celular" },
          primaryPainToVisualize: "Cadeiras ociosas",
          visualMetaphor: "Cadeiras vazias em destaque",
          onImageCopy: {
            headline: "Cadeiras vazias?",
            subhead: "Encha com pacientes certos",
            cta: "Agendar",
          },
          visualDirections: ["close no dentista", "fundo de clínica real"],
          globalDesignSystem: ["realista", "alto contraste", "tons de azul"],
        },
        {
          variantId: "V2",
          mustMatchAdVariant: "resultado",
          formatByPlacement: "stories",
          concept: { idea: "Recepção cheia com pacientes satisfeitos" },
          primaryPainToVisualize: "Baixa ocupação revertida",
          visualMetaphor: "Agenda preenchida com checkmarks",
          onImageCopy: {
            headline: "Agenda lotada",
            subhead: "Mais implantes toda semana",
            cta: "Ver método",
          },
          visualDirections: ["movimento natural", "foco no sorriso do paciente"],
          globalDesignSystem: ["lifestyle", "luminoso"],
        },
        {
          variantId: "V3",
          mustMatchAdVariant: "prova",
          formatByPlacement: "feed",
          concept: { idea: "Dentista mostrando painel simples de antes/depois" },
          primaryPainToVisualize: "Incerteza sobre retorno",
          visualMetaphor: "linha de crescimento simples ao fundo",
          onImageCopy: {
            headline: "Caso comprovado",
            subhead: "Mais consultas de implante",
            cta: "Ver prova",
          },
          visualDirections: ["1 elemento de prova", "sem poluição visual"],
          globalDesignSystem: ["editorial", "clean"],
        },
      ],
    },
  };
}

describe("adImageRenderer", () => {
  it("renderiza variante dor/feed", () => {
    const input = buildBaseInput();
    input.adImageBriefing.briefings = [input.adImageBriefing.briefings[0]];

    const output = renderAdImagePayloads(input);
    const payload = output.imageRenderPayloads[0];

    expect(payload.label).toBe("dor");
    expect(payload.placement).toBe("feed");
    expect(payload.imageParams.size).toBe("1024x1536");
    expect(payload.imagePrompt).toContain("Instagram/Meta");
    expect(payload.imagePrompt).toContain("Clínicas odontológicas");
    expect(payload.imagePrompt).toContain("1 foco visual principal");
  });

  it("renderiza variante resultado/stories", () => {
    const input = buildBaseInput();
    input.adImageBriefing.briefings = [input.adImageBriefing.briefings[1]];

    const output = renderAdImagePayloads(input);
    const payload = output.imageRenderPayloads[0];

    expect(payload.label).toBe("resultado");
    expect(payload.placement).toBe("stories");
    expect(payload.imageParams.size).toBe("1024x1792");
    expect(payload.overlayCopy.cta).toBe("Ver método");
  });

  it("renderiza variante prova/feed", () => {
    const input = buildBaseInput();
    input.adImageBriefing.briefings = [input.adImageBriefing.briefings[2]];

    const output = renderAdImagePayloads(input);
    const payload = output.imageRenderPayloads[0];

    expect(payload.label).toBe("prova");
    expect(payload.placement).toBe("feed");
    expect(payload.consistency.singleMindedPromise).toBe(
      "Ganhar previsibilidade no volume de leads qualificados.",
    );
    expect(payload.consistency.ctaMatch).toBe("Agendar diagnóstico");
  });

  it("falha quando variante visual não casa com copy", () => {
    const input = buildBaseInput();
    input.adImageBriefing.briefings = [
      {
        mustMatchAdVariant: "autoridade",
        concept: { idea: "Cena" },
      },
    ];

    expect(() => renderAdImagePayloads(input)).toThrow(/não casa com variante de copy/i);
  });

  it("expõe exemplo de uso mock realista", () => {
    const output = renderAdImagePayloads(imageRenderMockExampleInput);

    expect(output.imageRenderPayloads[0].assetId).toBe("AD-10-V1-feed");
    expect(output.imageRenderPayloads[0].experimentMetadata.asset_role).toBe(
      "ad-image-render",
    );
  });
});
