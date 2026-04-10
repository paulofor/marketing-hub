import {
  JsonRecord,
  extractObjectCandidates,
  pickText,
  toStringArray,
} from "./parserUtils";

export interface LandingPlannedImage {
  sectionId?: string;
  sectionName?: string;
  imageRole?: string;
  conversionRole?: string;
  emotionalJob?: string;
  sectionVisualGoal?: string;
  placement?: string;
  priority?: string;
  hierarchyLevel?: string;
  attentionPriority?: string;
  visualWeight?: string;
  distanceToCTA?: string;
  supportsFormConversion?: string;
  formRelationNotes?: string;
  objective?: string;
  imagePrompt?: string;
  negativePrompt?: string;
  desktopDimensions?: string;
  mobileDimensions?: string;
  messageMatchNotes?: string;
  textOverlayGuidance?: string;
  complianceNotes?: string[];
  imageUrl?: string;
  altText?: string;
}

export interface LandingImagePlanningContent {
  pageGoal?: string;
  visualDirectionSummary?: string;
  sequencingNotes?: string;
  ctaIntegrationNotes?: string;
  images: LandingPlannedImage[];
}

function resolvePlanningPayload(candidate: JsonRecord): JsonRecord | undefined {
  if (
    candidate.landingPageImagePlanning &&
    typeof candidate.landingPageImagePlanning === "object" &&
    !Array.isArray(candidate.landingPageImagePlanning)
  ) {
    return candidate.landingPageImagePlanning as JsonRecord;
  }
  if (
    candidate.imagePlan &&
    typeof candidate.imagePlan === "object" &&
    !Array.isArray(candidate.imagePlan)
  ) {
    return candidate.imagePlan as JsonRecord;
  }
  if (
    candidate.artifact &&
    typeof candidate.artifact === "object" &&
    !Array.isArray(candidate.artifact)
  ) {
    const artifact = candidate.artifact as JsonRecord;
    if (
      artifact.content &&
      typeof artifact.content === "object" &&
      !Array.isArray(artifact.content)
    ) {
      return artifact.content as JsonRecord;
    }
  }
  return candidate;
}

function parseDimensions(value: unknown): string | undefined {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    return undefined;
  }
  const payload = value as JsonRecord;
  const width = pickText(payload.width);
  const height = pickText(payload.height);
  if (!width || !height) return undefined;
  return `${width}x${height}`;
}

function parseImage(value: unknown): LandingPlannedImage | undefined {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    return undefined;
  }
  const payload = value as JsonRecord;
  const dimensions =
    payload.dimensions && typeof payload.dimensions === "object"
      ? (payload.dimensions as JsonRecord)
      : undefined;
  const image = payload.image as JsonRecord | undefined;

  const parsed: LandingPlannedImage = {
    sectionId: pickText(payload.sectionId),
    sectionName: pickText(payload.sectionName),
    imageRole: pickText(payload.imageRole),
    conversionRole: pickText(payload.conversionRole),
    emotionalJob: pickText(payload.emotionalJob),
    sectionVisualGoal: pickText(payload.sectionVisualGoal),
    placement: pickText(payload.placement),
    priority: pickText(payload.priority ?? payload.priorityLevel),
    hierarchyLevel: pickText(payload.hierarchyLevel),
    attentionPriority: pickText(payload.attentionPriority),
    visualWeight: pickText(payload.visualWeight),
    distanceToCTA: pickText(payload.distanceToCTA),
    supportsFormConversion:
      typeof payload.supportsFormConversion === "boolean"
        ? String(payload.supportsFormConversion)
        : pickText(payload.supportsFormConversion),
    formRelationNotes: pickText(payload.formRelationNotes),
    objective: pickText(payload.objective),
    imagePrompt: pickText(payload.imagePrompt ?? payload.prompt),
    negativePrompt: pickText(payload.negativePrompt),
    desktopDimensions: parseDimensions(dimensions?.desktop),
    mobileDimensions: parseDimensions(dimensions?.mobile),
    messageMatchNotes: pickText(payload.messageMatchNotes),
    textOverlayGuidance: pickText(payload.textOverlayGuidance),
    complianceNotes: toStringArray(payload.complianceNotes),
    imageUrl: pickText(
      image?.url ??
        payload.imageUrl ??
        payload.assetUrl ??
        payload.generatedImageUrl ??
        payload.src,
    ),
    altText: pickText(image?.altText ?? payload.altText),
  };

  const hasAny = Object.values(parsed).some((field) => {
    if (typeof field === "string") return field.trim().length > 0;
    if (Array.isArray(field)) return field.length > 0;
    return false;
  });
  return hasAny ? parsed : undefined;
}

export function hasLandingImagePlanningContent(
  payload?: LandingImagePlanningContent | null,
): payload is LandingImagePlanningContent {
  if (!payload) return false;
  return Boolean(
    pickText(payload.pageGoal) ||
    pickText(payload.visualDirectionSummary) ||
    payload.images.length > 0,
  );
}

export function parseLandingImagePlanningPayload(
  raw?: string | null,
): LandingImagePlanningContent | undefined {
  const candidates = extractObjectCandidates(raw);
  for (const candidate of candidates) {
    const scope = resolvePlanningPayload(candidate);
    if (!scope) continue;
    const imagesRaw = scope.images;
    const images = Array.isArray(imagesRaw)
      ? imagesRaw
          .map((entry) => parseImage(entry))
          .filter((entry): entry is LandingPlannedImage => Boolean(entry))
      : [];
    const parsed: LandingImagePlanningContent = {
      pageGoal: pickText(scope.pageGoal),
      visualDirectionSummary: pickText(scope.visualDirectionSummary),
      sequencingNotes: pickText(scope.sequencingNotes),
      ctaIntegrationNotes: pickText(scope.ctaIntegrationNotes),
      images,
    };
    if (hasLandingImagePlanningContent(parsed)) {
      return parsed;
    }
  }
  return undefined;
}
