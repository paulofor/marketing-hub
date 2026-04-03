export type Placement = "feed" | "stories" | "reels";

export interface RenderExperimentMetadata {
  primary_variable: string;
  variant_id: string;
  stage: "AD";
  control_or_treatment: string;
}

export interface RenderCampaignAngle {
  singleMindedPromise?: string;
  primaryCTA?: string;
  cta?: string;
  landingMatchLine?: string;
  audienceFilterLine?: string;
}

export interface RenderAdCopyVariant {
  label?: string;
  openingHookType?: string;
  placementHint?: string;
  headline?: string;
  description?: string;
  ctaText?: string;
}

export interface RenderAdCopy {
  primaryTextVariants: RenderAdCopyVariant[];
}

export interface RenderOnImageCopy {
  headline?: string;
  subhead?: string;
  badge?: string;
  cta?: string;
}

export interface RenderVisualConcept {
  idea?: string;
}

export interface RenderImageBriefingVariant {
  variantId?: string;
  label?: string;
  mustMatchAdVariant?: string;
  formatByPlacement?: string;
  concept?: RenderVisualConcept;
  primaryPainToVisualize?: string;
  visualMetaphor?: string;
  onImageCopy?: RenderOnImageCopy;
  visualDirections?: string[] | string;
  globalDesignSystem?: string[] | string;
}

export interface RenderAdImageBriefing {
  briefings: RenderImageBriefingVariant[];
}

export interface RenderAdImagePayloadsInput {
  experimentMetadata: RenderExperimentMetadata;
  campaignAngle: RenderCampaignAngle;
  adCopy: RenderAdCopy;
  adImageBriefing: RenderAdImageBriefing;
}

export interface ImageParams {
  apiMode: "image_api" | "responses_image_generation";
  model: "gpt-image-1.5";
  size: "1024x1536" | "1024x1792";
  quality: "medium";
  background: "opaque";
  format: "png";
}

export interface ImageRenderPayload {
  assetId: string;
  variantId: string;
  placement: Placement;
  label: string;
  imagePrompt: string;
  imageParams: ImageParams;
  overlayCopy: Required<RenderOnImageCopy>;
  consistency: {
    singleMindedPromise: string;
    audienceFilterLine: string;
    ctaMatch: string;
    landingMatchLine: string;
  };
  experimentMetadata: RenderExperimentMetadata & { asset_role: "ad-image-render" };
}

export interface RenderAdImagePayloadsOutput {
  imageRenderPayloads: ImageRenderPayload[];
}

const FORBIDDEN_INFOGRAPHIC_TERMS = [
  "dashboard",
  "infográfico",
  "infographic",
  "múltiplas colunas",
  "vários cards",
  "muitos cards",
  "apresentação corporativa",
  "software genérico",
];

export function choosePlacement(briefing: RenderImageBriefingVariant, copy?: RenderAdCopyVariant): Placement {
  const hint = `${briefing.formatByPlacement ?? ""} ${copy?.placementHint ?? ""}`.toLowerCase();
  if (hint.includes("story") || hint.includes("stories")) return "stories";
  if (hint.includes("reel") || hint.includes("reels")) return "reels";
  return "feed";
}

export function chooseCopyForVariant(
  adCopy: RenderAdCopy,
  mustMatchAdVariant?: string,
): RenderAdCopyVariant {
  const target = normalizeKey(mustMatchAdVariant);
  if (!target) {
    throw new Error("Visual briefing sem mustMatchAdVariant.");
  }
  const found = adCopy.primaryTextVariants.find((variant) => {
    const candidate = normalizeKey(variant.label ?? variant.openingHookType);
    return candidate === target;
  });
  if (!found) {
    throw new Error(`Variante visual '${mustMatchAdVariant}' não casa com variante de copy.`);
  }
  return found;
}

export function normalizeCTA(campaignAngle: RenderCampaignAngle, adCopyVariant?: RenderAdCopyVariant): string {
  const cta = campaignAngle.primaryCTA ?? campaignAngle.cta ?? adCopyVariant?.ctaText;
  const normalized = cta?.trim();
  if (!normalized) {
    throw new Error("CTA principal ausente.");
  }
  return normalized;
}

export function limitTextWords(text: string | undefined, maxWords: number): string {
  if (!text) return "";
  const words = text.trim().split(/\s+/).filter(Boolean);
  return words.slice(0, maxWords).join(" ");
}

export function validatePromptSpecificity(prompt: string, audienceFilterLine: string): void {
  const niche = audienceFilterLine.trim();
  if (niche.length < 4 || /público|audience|qualquer|geral/i.test(niche)) {
    throw new Error("Filtro de nicho ausente ou genérico demais.");
  }
  if (!prompt.toLowerCase().includes(niche.toLowerCase())) {
    throw new Error("Prompt final genérico demais: nicho não foi explicitamente citado.");
  }
}

export function validateSingleVisualFocus(prompt: string): void {
  if (!prompt.toLowerCase().includes("1 foco visual principal")) {
    throw new Error("Prompt sem foco visual único.");
  }
}

function validateNotInfographic(briefing: RenderImageBriefingVariant): void {
  const source = [
    briefing.concept?.idea ?? "",
    briefing.primaryPainToVisualize ?? "",
    briefing.visualMetaphor ?? "",
    ...toArray(briefing.visualDirections),
  ]
    .join(" ")
    .toLowerCase();

  const hasForbidden = FORBIDDEN_INFOGRAPHIC_TERMS.some((term) => source.includes(term));
  if (hasForbidden) {
    throw new Error("Prompt com aparência de infográfico/apresentação confusa.");
  }
}

function toArray(value?: string[] | string): string[] {
  if (!value) return [];
  return Array.isArray(value) ? value.filter((item) => item.trim().length > 0) : [value];
}

function normalizeKey(value?: string): string {
  return (value ?? "")
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .trim()
    .toLowerCase();
}

function resolveImageParams(placement: Placement): ImageParams {
  const verticalFeed = "1024x1536" as const;
  const verticalStory = "1024x1792" as const;
  return {
    apiMode: "image_api",
    model: "gpt-image-1.5",
    size: placement === "feed" ? verticalFeed : verticalStory,
    quality: "medium",
    background: "opaque",
    format: "png",
  };
}

function buildOverlayCopy(
  onImageCopy: RenderOnImageCopy | undefined,
  adCopyVariant: RenderAdCopyVariant,
  cta: string,
): Required<RenderOnImageCopy> {
  return {
    headline: limitTextWords(onImageCopy?.headline ?? adCopyVariant.headline, 8),
    subhead: limitTextWords(onImageCopy?.subhead ?? adCopyVariant.description, 12),
    badge: limitTextWords(onImageCopy?.badge, 4),
    cta: limitTextWords(onImageCopy?.cta ?? cta, 4),
  };
}

export function convertBriefingToImagePrompt(args: {
  placement: Placement;
  audienceFilterLine: string;
  singleMindedPromise: string;
  landingMatchLine: string;
  cta: string;
  conceptIdea?: string;
  primaryPainToVisualize?: string;
  visualMetaphor?: string;
  overlayCopy: Required<RenderOnImageCopy>;
  visualDirections?: string[] | string;
  globalDesignSystem?: string[] | string;
}): string {
  const style = toArray(args.globalDesignSystem).join(", ") || "visual contemporâneo, anúncio social paid media";
  const directions = toArray(args.visualDirections).join("; ") || "composição limpa, contraste forte, leitura rápida";
  const focal =
    args.visualMetaphor ?? args.primaryPainToVisualize ?? args.conceptIdea ?? args.singleMindedPromise;
  const placementLabel = args.placement === "feed" ? "feed" : "stories/reels";

  return [
    `Crie um anúncio vertical para Instagram/Meta (placement ${placementLabel}) voltado ao nicho: ${args.audienceFilterLine}.`,
    `Promessa central obrigatória: ${args.singleMindedPromise}. CTA principal obrigatório: ${args.cta}. Mensagem alinhada à landing: ${args.landingMatchLine}.`,
    `Cena/objeto principal: ${focal}.`,
    `Contexto visual: ${args.conceptIdea ?? "ambiente real de uso do nicho"}.`,
    `Estilo: ${style}. Direções visuais: ${directions}.`,
    "Composição simples e forte, 1 foco visual principal, leitura imediata no mobile.",
    `Texto sobreposto curto e legível: headline '${args.overlayCopy.headline}'. Subhead opcional '${args.overlayCopy.subhead}'. Badge opcional '${args.overlayCopy.badge}'. CTA curto '${args.overlayCopy.cta}'.`,
    "Evitar aparência de software genérico, dashboard, apresentação corporativa, infográfico confuso, múltiplos cards, excesso de mini-textos ou colunas.",
    "A peça deve parecer anúncio de feed e identificar o nicho em até 2 segundos.",
  ].join(" ");
}

function ensureRequiredConsistency(campaignAngle: RenderCampaignAngle): {
  singleMindedPromise: string;
  audienceFilterLine: string;
  landingMatchLine: string;
} {
  const singleMindedPromise = campaignAngle.singleMindedPromise?.trim();
  if (!singleMindedPromise) {
    throw new Error("singleMindedPromise ausente.");
  }

  const audienceFilterLine = campaignAngle.audienceFilterLine?.trim();
  if (!audienceFilterLine) {
    throw new Error("audienceFilterLine ausente.");
  }

  const landingMatchLine = campaignAngle.landingMatchLine?.trim();
  if (!landingMatchLine) {
    throw new Error("landingMatchLine ausente.");
  }

  return { singleMindedPromise, audienceFilterLine, landingMatchLine };
}

function buildAssetId(input: RenderAdImagePayloadsInput, variantId: string, placement: Placement): string {
  const stage = input.experimentMetadata.stage ?? "AD";
  const primaryVariable = input.experimentMetadata.primary_variable || "asset";
  return `${stage}-${primaryVariable}-${variantId}-${placement}`;
}

export function renderAdImagePayloads(
  input: RenderAdImagePayloadsInput,
): RenderAdImagePayloadsOutput {
  const { singleMindedPromise, audienceFilterLine, landingMatchLine } = ensureRequiredConsistency(
    input.campaignAngle,
  );

  const payloads = input.adImageBriefing.briefings.map((briefing, index) => {
    const adCopyVariant = chooseCopyForVariant(input.adCopy, briefing.mustMatchAdVariant);
    const placement = choosePlacement(briefing, adCopyVariant);
    const cta = normalizeCTA(input.campaignAngle, adCopyVariant);
    const overlayCopy = buildOverlayCopy(briefing.onImageCopy, adCopyVariant, cta);

    const imagePrompt = convertBriefingToImagePrompt({
      placement,
      audienceFilterLine,
      singleMindedPromise,
      landingMatchLine,
      cta,
      conceptIdea: briefing.concept?.idea,
      primaryPainToVisualize: briefing.primaryPainToVisualize,
      visualMetaphor: briefing.visualMetaphor,
      overlayCopy,
      visualDirections: briefing.visualDirections,
      globalDesignSystem: briefing.globalDesignSystem,
    });

    validatePromptSpecificity(imagePrompt, audienceFilterLine);
    validateSingleVisualFocus(imagePrompt);
    validateNotInfographic(briefing);

    const overlayWordCount = [overlayCopy.headline, overlayCopy.subhead, overlayCopy.badge, overlayCopy.cta]
      .join(" ")
      .trim()
      .split(/\s+/)
      .filter(Boolean).length;

    if (overlayWordCount > 20) {
      throw new Error("Prompt final com texto sobreposto demais.");
    }

    const variantId = briefing.variantId?.trim() || input.experimentMetadata.variant_id || `V${index + 1}`;

    return {
      assetId: buildAssetId(input, variantId, placement),
      variantId,
      placement,
      label: briefing.mustMatchAdVariant ?? briefing.label ?? `variant-${index + 1}`,
      imagePrompt,
      imageParams: resolveImageParams(placement),
      overlayCopy,
      consistency: {
        singleMindedPromise,
        audienceFilterLine,
        ctaMatch: cta,
        landingMatchLine,
      },
      experimentMetadata: {
        ...input.experimentMetadata,
        variant_id: variantId,
        asset_role: "ad-image-render",
      },
    } satisfies ImageRenderPayload;
  });

  return { imageRenderPayloads: payloads };
}

export const imageRenderMockExampleInput: RenderAdImagePayloadsInput = {
  experimentMetadata: {
    primary_variable: "10",
    variant_id: "V1",
    stage: "AD",
    control_or_treatment: "treatment",
  },
  campaignAngle: {
    singleMindedPromise: "Reduzir desperdício no tráfego pago sem aumentar o orçamento.",
    primaryCTA: "Ver plano agora",
    landingMatchLine: "Mesmo método prático apresentado na landing.",
    audienceFilterLine: "Gestores de tráfego para e-commerce de moda.",
  },
  adCopy: {
    primaryTextVariants: [
      {
        label: "dor",
        headline: "Pare de desperdiçar verba",
        description: "Ajuste campanhas em minutos",
        ctaText: "Ver plano agora",
      },
    ],
  },
  adImageBriefing: {
    briefings: [
      {
        variantId: "V1",
        mustMatchAdVariant: "dor",
        formatByPlacement: "feed",
        concept: { idea: "Gestor olhando relatório com alerta de gasto alto" },
        primaryPainToVisualize: "Verba queimando sem retorno",
        visualMetaphor: "Dinheiro escapando por rachadura no funil",
        onImageCopy: {
          headline: "Verba indo embora?",
          subhead: "Corrija em 15 minutos",
          cta: "Ver plano",
        },
        visualDirections: ["close no problema", "contraste alto no ponto crítico"],
        globalDesignSystem: ["realista", "luz natural", "fundo limpo"],
      },
    ],
  },
};
