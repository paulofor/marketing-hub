/*
 * Camada de renderização de imagem para o Marketing Hub.
 * Transforma campaign-angle, ad-copy e ad-image-briefing em payload pronto
 * para modelos de geração de imagem.
 */

export type Placement = "feed" | "stories" | "reels";

export interface ExperimentMetadataInput {
  primary_variable?: string;
  variant_id?: string;
  stage?: string;
  control_or_treatment?: string;
}

export interface ExperimentMetadataOutput extends ExperimentMetadataInput {
  asset_role: "ad-image-render";
}

export interface CampaignAngleFields {
  primaryPromise?: string;
  primaryPain?: string;
  mechanismSummary?: string;
  proofSummary?: string;
  singleMindedPromise?: string;
  primaryCTA?: string;
  landingMatchLine?: string;
  audienceFilterLine?: string;
  cta?: string;
  tone?: string;
}

export interface AdCopyVariantInput {
  label?: string;
  openingHookType?: string;
  placementHint?: string;
  primaryText?: string;
  headline?: string;
  description?: string;
  ctaText?: string;
}

export interface AdCopyContentInput {
  primaryTextVariants: AdCopyVariantInput[];
}

export interface ConceptInstruction {
  idea?: string;
  primaryPainToVisualize?: string;
  visualMetaphor?: string;
}

export interface OnImageCopyBlock {
  headline?: string;
  subhead?: string;
  badge?: string;
  cta?: string;
  microcopyOptional?: string;
}

export interface VisualDirectionsBlock {
  imagery?: string[];
  background?: string;
  avoid?: string[];
}

export interface GlobalDesignSystem {
  style?: string;
  colorPalette?: {
    primary?: string;
    accent?: string;
    neutral?: string;
  };
  typography?: {
    headline?: string;
    body?: string;
    rules?: string;
  };
  avoid?: string[];
}

export interface AdImageVariantBriefing {
  id?: string;
  name?: string;
  label?: string;
  mustMatchAdVariant?: string;
  placement?: string;
  concept?: ConceptInstruction;
  layout?: {
    structure?: string;
    hierarchy?: string[];
  };
  onImageCopy?: OnImageCopyBlock;
  visualDirections?: VisualDirectionsBlock;
  globalDesignSystemOverrides?: GlobalDesignSystem;
}

export interface AdImageBriefingDocument {
  objective?: string;
  singleMindedPromise?: string;
  audienceFilterLine?: string;
  mustVisuallyIdentifyAudience?: boolean;
  singleFocalPoint?: string;
  maxOverlayLines?: number;
  imageTextMaxWords?: number;
  assetType?: string;
  nicheVisualSignal?: string;
  adToLandingConsistency?: {
    promiseMatch?: string;
    ctaMatch?: string;
    complianceMatch?: string;
  };
  globalDesignSystem?: GlobalDesignSystem;
  variants: AdImageVariantBriefing[];
  assetIdPrefix?: string;
}

export interface OverlayCopy {
  headline: string;
  subhead: string;
  badge: string;
  cta: string;
}

export interface ConsistencySnapshot {
  singleMindedPromise: string;
  audienceFilterLine: string;
  ctaMatch: string;
  landingMatchLine: string;
}

export interface ImageParams {
  apiMode: "image_api" | "responses_image";
  model: string;
  size: string;
  quality: "medium" | "high";
  background: "opaque" | "transparent";
  format: "png" | "jpg";
}

export interface ImageRenderPayload {
  assetId: string;
  variantId: string;
  placement: Placement;
  label: string;
  imagePrompt: string;
  imageParams: ImageParams;
  overlayCopy: OverlayCopy;
  consistency: ConsistencySnapshot;
  experimentMetadata: ExperimentMetadataOutput;
}

export interface RenderAdImagePayloadInput {
  experimentMetadata: ExperimentMetadataInput;
  campaignAngle: CampaignAngleFields;
  adCopy: AdCopyContentInput;
  adImageBriefing: AdImageBriefingDocument;
}

export interface RenderAdImagePayloadResult {
  imageRenderPayloads: ImageRenderPayload[];
}

const CTA_CANONICAL_MAP: Record<string, string> = {
  "saiba mais": "Saiba mais",
  "quero participar": "Quero participar",
  "quero testar": "Quero testar",
  "quero provar": "Quero provar",
  "comece agora": "Comece agora",
  "assista agora": "Assista agora",
  "fale com especialista": "Fale com especialista",
  "pedir demo": "Pedir demo",
  "garantir vaga": "Garantir vaga",
};

const DEFAULT_AVOID_LIST = [
  "software genérico",
  "dashboards complexos",
  "apresentações corporativas",
  "infográficos confusos",
  "excesso de cards",
  "texto pequeno demais",
  "múltiplas colunas",
];
const NormalizedStoriesMatcher = /(story|storie|9x16|9:16)/;
const NormalizedFeedMatcher = /(feed|4x5|4:5|portrait|vertical)/;


export class RenderAdImagePayloadError extends Error {
  public readonly issues: string[];

  constructor(message: string, issues: string[] = []) {
    super(message);
    this.name = "RenderAdImagePayloadError";
    this.issues = issues.length > 0 ? issues : [message];
  }
}

export function renderAdImagePayloads(
  input: RenderAdImagePayloadInput,
): RenderAdImagePayloadResult {
  const { campaignAngle, adCopy, adImageBriefing, experimentMetadata } = input;
  const issues: string[] = [];

  const singleMindedPromise = pickFirstText(
    adImageBriefing.singleMindedPromise,
    campaignAngle.singleMindedPromise,
    campaignAngle.primaryPromise,
  );
  if (!singleMindedPromise) {
    issues.push("singleMindedPromise ausente no campaign-angle/ad-image-briefing.");
  }

  const audienceFilterLine = pickFirstText(
    adImageBriefing.audienceFilterLine,
    campaignAngle.audienceFilterLine,
    adImageBriefing.nicheVisualSignal,
  );
  if (!audienceFilterLine) {
    issues.push("Filtro de nicho (audienceFilterLine) não encontrado.");
  }

  const landingMatchLine =
    pickFirstText(
      adImageBriefing.adToLandingConsistency?.promiseMatch,
      campaignAngle.landingMatchLine,
      campaignAngle.primaryPromise,
    ) ?? "";

  const normalizedCTA = normalizeCTA(
    pickFirstText(
      adImageBriefing.adToLandingConsistency?.ctaMatch,
      campaignAngle.primaryCTA,
      campaignAngle.cta,
    ),
  );
  if (!normalizedCTA) {
    issues.push("CTA principal não encontrado para manter consistência.");
  }

  if (!adCopy?.primaryTextVariants?.length) {
    issues.push("Nenhuma variação de ad-copy disponível.");
  }

  if (!adImageBriefing?.variants?.length) {
    issues.push("Nenhuma variante em ad-image-briefing para renderizar.");
  }

  if (issues.length > 0) {
    throw new RenderAdImagePayloadError(
      "Briefings insuficientes para renderizar imagem.",
      issues,
    );
  }

  const overlayLimits = {
    maxOverlayLines: clampOverlayLines(adImageBriefing.maxOverlayLines),
    maxWords: clampMaxWords(adImageBriefing.imageTextMaxWords),
  };

  const results: ImageRenderPayload[] = [];
  const variantIssues: string[] = [];

  adImageBriefing.variants.forEach((variant, index) => {
    try {
      const payload = renderSingleVariant({
        variant,
        variantIndex: index,
        singleMindedPromise: singleMindedPromise!,
        audienceFilterLine: audienceFilterLine!,
        landingMatchLine,
        normalizedCTA: normalizedCTA!,
        adCopy,
        globalDesignSystem: adImageBriefing.globalDesignSystem,
        overlayLimits,
        experimentMetadata,
        assetIdPrefix: adImageBriefing.assetIdPrefix,
        campaignTone: campaignAngle.tone,
      });
      results.push(payload);
    } catch (error) {
      if (error instanceof RenderAdImagePayloadError) {
        variantIssues.push(...error.issues);
      } else if (error instanceof Error) {
        variantIssues.push(error.message);
      } else {
        variantIssues.push("Erro desconhecido ao renderizar variante.");
      }
    }
  });

  if (variantIssues.length > 0) {
    throw new RenderAdImagePayloadError(
      "Falha ao renderizar todas as variantes de imagem.",
      variantIssues,
    );
  }

  return { imageRenderPayloads: results };
}

interface RenderVariantContext {
  variant: AdImageVariantBriefing;
  variantIndex: number;
  singleMindedPromise: string;
  audienceFilterLine: string;
  landingMatchLine: string;
  normalizedCTA: string;
  adCopy: AdCopyContentInput;
  globalDesignSystem?: GlobalDesignSystem;
  overlayLimits: {
    maxOverlayLines: number;
    maxWords: number;
  };
  experimentMetadata: ExperimentMetadataInput;
  assetIdPrefix?: string;
  campaignTone?: string;
}

function renderSingleVariant(context: RenderVariantContext): ImageRenderPayload {
  const {
    variant,
    variantIndex,
    singleMindedPromise,
    audienceFilterLine,
    landingMatchLine,
    normalizedCTA,
    adCopy,
    globalDesignSystem,
    overlayLimits,
    experimentMetadata,
    assetIdPrefix,
    campaignTone,
  } = context;

  const label = pickFirstText(
    variant.mustMatchAdVariant,
    variant.label,
    variant.name,
  ) ?? `variant-${variantIndex + 1}`;

  const adCopyVariant = findMatchingAdCopyVariant(label, variant, adCopy);
  if (!adCopyVariant) {
    throw new RenderAdImagePayloadError(
      `Variante visual "${label}" não encontrou copy correspondente.`,
      [
        `Revise mustMatchAdVariant da variante ${variant.id ?? label} para casar com ad-copy disponível.`,
      ],
    );
  }

  const placement = choosePlacement(variant, adCopyVariant);
  const overlayCopy = buildOverlayCopy({
    variant,
    adCopyVariant,
    singleMindedPromise,
    landingMatchLine,
    normalizedCTA,
    audienceFilterLine,
    overlayLimits,
  });

  const imagePrompt = buildImagePrompt({
    variant,
    placement,
    overlayCopy,
    singleMindedPromise,
    landingMatchLine,
    audienceFilterLine,
    normalizedCTA,
    adCopyVariant,
    globalDesignSystem,
    overlayLimits,
    campaignTone,
  });

  validatePromptSpecificity(imagePrompt, audienceFilterLine);
  validatePromptContainsBenefit(imagePrompt, singleMindedPromise);
  validatePromptSingleFocus(imagePrompt);

  const imageParams = resolveImageParams(placement);
  const variantId = sanitizeVariantId(variant.id ?? label ?? `V${variantIndex + 1}`);
  const assetId = buildAssetId(assetIdPrefix, experimentMetadata.variant_id, variantId, placement);

  return {
    assetId,
    variantId,
    placement,
    label,
    imagePrompt,
    imageParams,
    overlayCopy,
    consistency: {
      singleMindedPromise,
      audienceFilterLine,
      ctaMatch: normalizedCTA,
      landingMatchLine,
    },
    experimentMetadata: {
      ...experimentMetadata,
      asset_role: "ad-image-render",
    },
  };
}

interface BuildOverlayParams {
  variant: AdImageVariantBriefing;
  adCopyVariant: AdCopyVariantInput;
  singleMindedPromise: string;
  landingMatchLine: string;
  normalizedCTA: string;
  audienceFilterLine: string;
  overlayLimits: {
    maxOverlayLines: number;
    maxWords: number;
  };
}

function buildOverlayCopy(params: BuildOverlayParams): OverlayCopy {
  const {
    variant,
    adCopyVariant,
    singleMindedPromise,
    landingMatchLine,
    normalizedCTA,
    audienceFilterLine,
    overlayLimits,
  } = params;

  const baseHeadline = pickFirstText(
    variant.onImageCopy?.headline,
    singleMindedPromise,
    adCopyVariant.headline,
  );
  const baseSubhead = pickFirstText(
    variant.onImageCopy?.subhead,
    landingMatchLine,
    adCopyVariant.description,
    adCopyVariant.primaryText,
  );
  const baseBadge = pickFirstText(
    variant.onImageCopy?.badge,
    variant.concept?.visualMetaphor,
    variant.concept?.primaryPainToVisualize,
    audienceFilterLine,
  );
  const baseCTA = pickFirstText(
    variant.onImageCopy?.cta,
    adCopyVariant.ctaText,
    normalizedCTA,
  );

  const overlay: OverlayCopy = {
    headline: limitWords(baseHeadline ?? singleMindedPromise, overlayLimits.maxWords),
    subhead: limitWords(baseSubhead ?? "", overlayLimits.maxWords + 2),
    badge: limitWords(baseBadge ?? "", Math.min(4, overlayLimits.maxWords)),
    cta: limitWords(baseCTA ?? normalizedCTA, 3),
  };

  const constrained = enforceOverlayLineCap(overlay, overlayLimits.maxOverlayLines);
  constrained.cta = normalizeCTA(constrained.cta) ?? normalizedCTA;

  return constrained;
}

interface BuildPromptParams {
  variant: AdImageVariantBriefing;
  placement: Placement;
  overlayCopy: OverlayCopy;
  singleMindedPromise: string;
  landingMatchLine: string;
  audienceFilterLine: string;
  normalizedCTA: string;
  adCopyVariant: AdCopyVariantInput;
  globalDesignSystem?: GlobalDesignSystem;
  overlayLimits: {
    maxOverlayLines: number;
    maxWords: number;
  };
  campaignTone?: string;
}

function buildImagePrompt(params: BuildPromptParams): string {
  const {
    variant,
    placement,
    overlayCopy,
    singleMindedPromise,
    landingMatchLine,
    audienceFilterLine,
    normalizedCTA,
    adCopyVariant,
    globalDesignSystem,
    overlayLimits,
    campaignTone,
  } = params;

  const focus =
    pickFirstText(
      variant.concept?.primaryPainToVisualize,
      variant.concept?.visualMetaphor,
      variant.concept?.idea,
      landingMatchLine,
    ) ?? singleMindedPromise;

  const imagery = variant.visualDirections?.imagery?.length
    ? `Referências visuais: ${variant.visualDirections?.imagery?.join(", ")}.`
    : undefined;

  const palette = buildPalette(globalDesignSystem ?? variant.globalDesignSystemOverrides);
  const typography = globalDesignSystem?.typography?.headline
    ? `Tipografia sugerida: ${globalDesignSystem.typography.headline}`
    : variant.globalDesignSystemOverrides?.typography?.headline
      ? `Tipografia sugerida: ${variant.globalDesignSystemOverrides.typography.headline}`
      : undefined;

  const avoid = uniqueStrings([
    ...(globalDesignSystem?.avoid ?? []),
    ...(variant.visualDirections?.avoid ?? []),
    ...DEFAULT_AVOID_LIST,
  ]);

  const hierarchy = variant.layout?.hierarchy?.length
    ? variant.layout.hierarchy.join(" > ")
    : "hero + reforço + CTA";

  const textLines = countOverlayTextLines(overlayCopy);
  const overlayDetails = buildOverlayDetailsDescription(overlayCopy);
  const copyHook = pickFirstText(
    adCopyVariant.headline,
    adCopyVariant.primaryText,
    adCopyVariant.description,
  );

  const promptSegments = [
    `Crie um anúncio vertical para Instagram/Meta voltado ao nicho ${audienceFilterLine}.`,
    `Formato ${placement === "feed" ? "4:5 para feed" : "9:16 para Stories/Reels"} com 1 foco visual dominante em ${focus}.`,
    `Benefício principal que deve dominar a imagem: ${singleMindedPromise}.`,
    variant.concept?.primaryPainToVisualize
      ? `Mostre o incômodo "${variant.concept.primaryPainToVisualize}" sendo resolvido no foco principal.`
      : undefined,
    variant.concept?.visualMetaphor
      ? `Metáfora visual sugerida: ${variant.concept.visualMetaphor}.`
      : undefined,
    imagery,
    palette,
    typography,
    variant.layout?.structure
      ? `Composição ${variant.layout.structure} seguindo hierarquia ${hierarchy}.`
      : `Composição simples seguindo ${hierarchy}, mobile-first e leitura imediata.`,
    copyHook ? `Traduza o mesmo gancho do texto "${copyHook}" em linguagem visual.` : undefined,
    `Texto sobreposto curto (${textLines} linhas) com ${overlayDetails} e CTA visual curto "${overlayCopy.cta}".`,
    `Respeite o limite de ${overlayLimits.maxWords} palavras por linha e mantenha tudo legível no mobile.`,
    `Garanta continuidade com a landing reforçando "${landingMatchLine}" e CTA "${normalizedCTA}" igual ao anúncio.`,
    `Reforce explicitamente que é para ${audienceFilterLine} em até 2 segundos de leitura.`,
    `Simplificar automaticamente para 1 promessa, 1 foco visual e 1 CTA.`,
    `Parecer anúncio nativo de ${placement === "feed" ? "feed" : "Stories/Reels"}, nunca apresentação, dashboard ou infográfico.`,
    `Evitar ${avoid.join(", ")}.`,
    campaignTone ? `Tom sugerido: ${campaignTone}.` : undefined,
    `CTA principal deve permanecer "${normalizedCTA}" igual à landing.`,
    `Mobile-first, nada de múltiplos cards, colunas ou mini-textos.`,
  ];

  const prompt = promptSegments.filter(Boolean).join(" ");
  return prompt.replace(/\s+/g, " ").trim();
}

function findMatchingAdCopyVariant(
  variantLabel: string,
  variant: AdImageVariantBriefing,
  adCopy: AdCopyContentInput,
): AdCopyVariantInput | undefined {
  const desiredTokens = uniqueStrings(
    [variant.mustMatchAdVariant, variant.label, variant.name, variant.id, variantLabel]
      .map((value) => normalizeIdentifier(value))
      .filter((value): value is string => Boolean(value)),
  );

  if (desiredTokens.length === 0) {
    return undefined;
  }

  return adCopy.primaryTextVariants.find((copyVariant) => {
    const copyTokens = uniqueStrings([
      normalizeIdentifier(copyVariant.label),
      normalizeIdentifier(copyVariant.openingHookType),
      normalizeIdentifier(copyVariant.ctaText),
    ]);
    return copyTokens.some((token) => desiredTokens.includes(token));
  });
}

function choosePlacement(
  variant: AdImageVariantBriefing,
  adCopyVariant: AdCopyVariantInput,
): Placement {
  const candidates = [
    variant.placement,
    adCopyVariant.placementHint,
    variant.layout?.structure,
  ];

  for (const candidate of candidates) {
    const normalized = normalizePlacement(candidate);
    if (normalized) {
      return normalized;
    }
  }

  return "feed";
}

function normalizePlacement(value?: string): Placement | undefined {
  const normalized = normalizeIdentifier(value);
  if (!normalized) return undefined;
  if (NormalizedStoriesMatcher.test(normalized)) {
    return "stories";
  }
  if (normalized.includes("reel")) {
    return "reels";
  }
  if (NormalizedFeedMatcher.test(normalized)) {
    return "feed";
  }
  return undefined;
}

function resolveImageParams(placement: Placement): ImageParams {
  const size = placement === "feed" ? "1024x1536" : "1024x1792";
  return {
    apiMode: "image_api",
    model: "gpt-image-1.5",
    size,
    quality: "medium",
    background: "opaque",
    format: "png",
  };
}

function buildAssetId(
  assetIdPrefix: string | undefined,
  experimentVariantId: string | undefined,
  variantId: string,
  placement: Placement,
): string {
  const prefix = pickFirstText(assetIdPrefix, experimentVariantId);
  const base = prefix ? `AD-${prefix}` : "AD";
  return `${base}-${variantId}-${placement}`;
}

function sanitizeVariantId(value: string): string {
  const trimmed = value.trim();
  return trimmed.replace(/\s+/g, "").toUpperCase();
}

function pickFirstText(...values: (string | undefined)[]): string | undefined {
  for (const value of values) {
    if (typeof value === "string") {
      const trimmed = value.trim();
      if (trimmed.length > 0) {
        return trimmed;
      }
    }
  }
  return undefined;
}

function normalizeCTA(value?: string): string | undefined {
  if (!value) return undefined;
  const trimmed = value.trim();
  if (!trimmed) return undefined;
  const canonical = CTA_CANONICAL_MAP[trimmed.toLowerCase()];
  if (canonical) return canonical;
  return trimmed.charAt(0).toUpperCase() + trimmed.slice(1);
}

function limitWords(value: string, maxWords: number): string {
  const cleaned = value?.trim() ?? "";
  if (!cleaned) return "";
  const words = cleaned.split(/\s+/);
  if (words.length <= maxWords) {
    return cleaned;
  }
  return words.slice(0, maxWords).join(" ");
}

function clampOverlayLines(value?: number): number {
  if (!value || value < 1) return 2;
  return Math.min(3, Math.max(1, Math.floor(value)));
}

function clampMaxWords(value?: number): number {
  if (!value || value < 2) return 8;
  return Math.min(12, Math.max(4, Math.floor(value)));
}

function enforceOverlayLineCap(overlay: OverlayCopy, maxLines: number): OverlayCopy {
  const orderedKeys: (keyof OverlayCopy)[] = ["headline", "subhead", "badge", "cta"];
  let linesUsed = 0;
  const result: OverlayCopy = { ...overlay };

  for (const key of orderedKeys) {
    if (key === "cta") {
      continue;
    }
    const value = result[key];
    if (value && value.trim().length > 0) {
      linesUsed += 1;
      if (linesUsed > maxLines) {
        result[key] = "";
      }
    }
  }

  return result;
}

function countOverlayTextLines(overlay: OverlayCopy): number {
  return [overlay.headline, overlay.subhead, overlay.badge].filter(
    (value) => Boolean(value && value.trim().length > 0),
  ).length;
}

function buildOverlayDetailsDescription(overlay: OverlayCopy): string {
  const parts = [] as string[];
  if (overlay.headline) parts.push(`headline "${overlay.headline}"`);
  if (overlay.subhead) parts.push(`subhead "${overlay.subhead}"`);
  if (overlay.badge) parts.push(`badge "${overlay.badge}"`);
  return parts.join(", ") || "apenas headline";
}

function buildPalette(system?: GlobalDesignSystem): string | undefined {
  if (!system?.colorPalette) return undefined;
  const { primary, accent, neutral } = system.colorPalette;
  const colors = [primary, accent, neutral].filter(Boolean).join(", ");
  return colors.length > 0 ? `Paleta: ${colors}.` : undefined;
}

function uniqueStrings(values: (string | undefined)[]): string[] {
  const seen = new Set<string>();
  const result: string[] = [];
  values.forEach((value) => {
    if (!value) return;
    const trimmed = value.trim();
    if (!trimmed || seen.has(trimmed)) return;
    seen.add(trimmed);
    result.push(trimmed);
  });
  return result;
}

function normalizeIdentifier(value?: string): string | undefined {
  if (!value) return undefined;
  const trimmed = value.trim().toLowerCase();
  if (!trimmed) return undefined;
  return trimmed.normalize("NFD").replace(/[\u0300-\u036f]/g, "");
}

function validatePromptSpecificity(prompt: string, audienceFilterLine: string) {
  if (!includesNormalized(prompt, audienceFilterLine)) {
    throw new RenderAdImagePayloadError("Prompt ficou genérico demais.", [
      `Prompt precisa mencionar explicitamente o nicho: ${audienceFilterLine}.`,
    ]);
  }
}

function validatePromptContainsBenefit(prompt: string, benefit: string) {
  if (!includesNormalized(prompt, benefit)) {
    throw new RenderAdImagePayloadError("Prompt não reforça a promessa principal.", [
      `Inclua a promessa: ${benefit}.`,
    ]);
  }
}

function validatePromptSingleFocus(prompt: string) {
  if (!/(1 foco visual|um único foco visual)/i.test(prompt)) {
    throw new RenderAdImagePayloadError("Prompt não garante foco único.", [
      "Instrua explicitamente que existe apenas 1 foco visual.",
    ]);
  }
}

function includesNormalized(haystack: string, needle: string): boolean {
  if (!haystack || !needle) return false;
  const normalizedHaystack = normalizeIdentifier(haystack);
  const normalizedNeedle = normalizeIdentifier(needle);
  if (!normalizedHaystack || !normalizedNeedle) return false;
  return normalizedHaystack.includes(normalizedNeedle);
}

