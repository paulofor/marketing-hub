import { RenderAdImagePayloadInput } from "./renderAdImagePayloads";

export function createMockRenderAdImageInput(): RenderAdImagePayloadInput {
  return {
    experimentMetadata: {
      primary_variable: "10",
      variant_id: "10",
      stage: "AD",
      control_or_treatment: "treatment",
    },
    campaignAngle: {
      primaryPromise: "Preencher a agenda com pacientes particulares",
      primaryPain: "Profissionais dependentes de indicação",
      singleMindedPromise: "Ganhe pacientes premium toda semana",
      primaryCTA: "saiba mais",
      landingMatchLine: "Converta visitas em consultas pagas",
      audienceFilterLine: "nutricionistas de performance",
      tone: "confiante e acessível",
    },
    adCopy: {
      primaryTextVariants: [
        {
          label: "dor",
          primaryText: "Cansada de consultas vazias? Descubra como lotar a agenda sem promoções.",
          headline: "Agenda cheia com pacientes premium",
          description: "Sistema próprio para nutricionistas que vendem valor.",
          ctaText: "Saiba Mais",
          placementHint: "feed",
        },
        {
          label: "resultado",
          primaryText: "Nutricionistas do programa fecharam 5 pacotes em 30 dias.",
          headline: "Resultados rápidos e previsíveis",
          description: "Estrutura de aquisição pronta para aplicar.",
          ctaText: "Conheça agora",
          placementHint: "stories",
        },
        {
          label: "prova",
          primaryText: "Veja os números de quem aplicou o método.",
          headline: "Case real: +48% em tickets altos",
          description: "Metodologia validada com nutricionistas de alta renda.",
          ctaText: "Quero aplicar",
          placementHint: "feed",
        },
      ],
    },
    adImageBriefing: {
      singleMindedPromise: "Ganhe pacientes premium toda semana",
      audienceFilterLine: "nutricionistas de performance",
      maxOverlayLines: 2,
      imageTextMaxWords: 8,
      nicheVisualSignal: "consultório moderno de nutrição esportiva",
      adToLandingConsistency: {
        promiseMatch: "Converta visitas em consultas pagas",
        ctaMatch: "saiba mais",
      },
      globalDesignSystem: {
        style: "fotografia editorial com toques de cor vibrante",
        colorPalette: {
          primary: "verde clínico",
          accent: "laranja energia",
          neutral: "off white",
        },
        typography: {
          headline: "Sans bold condensada",
        },
        avoid: ["elementos genéricos de software"],
      },
      variants: [
        {
          id: "V1",
          name: "dor",
          mustMatchAdVariant: "dor",
          concept: {
            idea: "Profissional olhando para agenda vazia",
            primaryPainToVisualize: "consultório sem pacientes",
            visualMetaphor: "cadeira vazia iluminada",
          },
          layout: {
            structure: "hero com recorte diagonal",
            hierarchy: ["hero", "promessa", "cta"],
          },
          onImageCopy: {
            headline: "Agenda cheia, tickets altos",
            subhead: "Sem promoções",
            badge: "nutrição premium",
            cta: "Saiba mais",
          },
          visualDirections: {
            imagery: ["profissional em consultório", "agenda destacada"],
            background: "consultório iluminado",
          },
        },
        {
          id: "V2",
          name: "resultado",
          mustMatchAdVariant: "resultado",
          placement: "stories",
          concept: {
            idea: "Gráfico ascendente em tablet",
            primaryPainToVisualize: "receio de não converter",
            visualMetaphor: "seta crescente sobre cardápio saudável",
          },
          layout: {
            structure: "stories full-bleed",
            hierarchy: ["hero", "prova", "cta"],
          },
          onImageCopy: {
            headline: "Fechamos 5 pacotes",
            subhead: "em 30 dias",
            badge: "case real",
            cta: "Aplicar método",
          },
          visualDirections: {
            imagery: ["tablet nas mãos", "pacientes felizes"],
            background: "tons claros com acento laranja",
          },
        },
        {
          id: "V3",
          name: "prova",
          mustMatchAdVariant: "prova",
          concept: {
            idea: "Depoimento destacado em cartão",
            primaryPainToVisualize: "dúvida sobre confiança",
            visualMetaphor: "selo de aprovação",
          },
          layout: {
            structure: "card único",
            hierarchy: ["selo", "prova", "cta"],
          },
          onImageCopy: {
            headline: "+48% em tickets altos",
            subhead: "com 2 semanas de campanha",
            badge: "case real",
            cta: "Validar oferta",
          },
          visualDirections: {
            imagery: ["close da profissional", "print de depoimento"],
            background: "textura neutra",
          },
        },
      ],
    },
  };
}
