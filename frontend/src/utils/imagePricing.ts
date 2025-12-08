import type { LeadPortalImagePackage } from "../api/leadPortal/useLeadPortalSubmissions";

type ResolutionKey = `${number}x${number}`;

type PricingTable = Record<string, Record<string, Partial<Record<ResolutionKey, number>>>>;

type Orientation = "SQUARE" | "PORTRAIT" | "LANDSCAPE";

const IMAGE_GENERATION_PRICING_USD: PricingTable = {
  "GPT IMAGE 1": {
    LOW: {
      "1024x1024": 0.011,
      "1024x1536": 0.016,
      "1536x1024": 0.016,
    },
    MEDIUM: {
      "1024x1024": 0.042,
      "1024x1536": 0.063,
      "1536x1024": 0.063,
    },
    HIGH: {
      "1024x1024": 0.167,
      "1024x1536": 0.25,
      "1536x1024": 0.25,
    },
  },
  "GPT IMAGE 1 MINI": {
    LOW: {
      "1024x1024": 0.005,
      "1024x1536": 0.006,
      "1536x1024": 0.006,
    },
    MEDIUM: {
      "1024x1024": 0.011,
      "1024x1536": 0.015,
      "1536x1024": 0.015,
    },
    HIGH: {
      "1024x1024": 0.036,
      "1024x1536": 0.052,
      "1536x1024": 0.052,
    },
  },
  "DALL E 3": {
    STANDARD: {
      "1024x1024": 0.04,
      "1024x1792": 0.08,
      "1792x1024": 0.08,
    },
    HD: {
      "1024x1024": 0.08,
      "1024x1792": 0.12,
      "1792x1024": 0.12,
    },
  },
  "DALL E 2": {
    STANDARD: {
      "256x256": 0.016,
      "512x512": 0.018,
      "1024x1024": 0.02,
    },
  },
};

const DEFAULT_RESOLUTION_BY_ORIENTATION: Record<string, Partial<Record<Orientation, ResolutionKey>>> = {
  "GPT IMAGE 1": {
    SQUARE: "1024x1024",
    PORTRAIT: "1024x1536",
    LANDSCAPE: "1536x1024",
  },
  "GPT IMAGE 1 MINI": {
    SQUARE: "1024x1024",
    PORTRAIT: "1024x1536",
    LANDSCAPE: "1536x1024",
  },
  "DALL E 3": {
    SQUARE: "1024x1024",
    PORTRAIT: "1024x1792",
    LANDSCAPE: "1792x1024",
  },
  "DALL E 2": {
    SQUARE: "1024x1024",
  },
};

const MODEL_ALIASES: Record<string, string> = {
  "GPT IMAGE 1": "GPT IMAGE 1",
  "GPT-IMAGE-1": "GPT IMAGE 1",
  "GPT_IMAGE_1": "GPT IMAGE 1",
  "GPTIMAGE1": "GPT IMAGE 1",
  "GPT IMAGE 1 MINI": "GPT IMAGE 1 MINI",
  "GPT-IMAGE-1 MINI": "GPT IMAGE 1 MINI",
  "GPT_IMAGE_1_MINI": "GPT IMAGE 1 MINI",
  "GPTIMAGE1MINI": "GPT IMAGE 1 MINI",
  "DALL E 3": "DALL E 3",
  "DALLE 3": "DALL E 3",
  "DALLE-3": "DALL E 3",
  "DALLE_3": "DALL E 3",
  "DALL·E 3": "DALL E 3",
  "DALL.E 3": "DALL E 3",
  "DALLE3": "DALL E 3",
  "DALL E3": "DALL E 3",
  "DALL-E 3": "DALL E 3",
  "DALL_E_3": "DALL E 3",
  "DALL E 2": "DALL E 2",
  "DALLE 2": "DALL E 2",
  "DALLE-2": "DALL E 2",
  "DALLE_2": "DALL E 2",
  "DALL·E 2": "DALL E 2",
  "DALL.E 2": "DALL E 2",
  "DALLE2": "DALL E 2",
  "DALL E2": "DALL E 2",
  "DALL-E 2": "DALL E 2",
  "DALL_E_2": "DALL E 2",
};

function sanitizeKey(value?: string | null) {
  if (!value) return null;
  return value
    .normalize("NFKD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-zA-Z0-9]+/g, " ")
    .trim()
    .toUpperCase();
}

function normalizeModelName(modelName?: string | null) {
  const sanitized = sanitizeKey(modelName);
  if (!sanitized) return null;
  return MODEL_ALIASES[sanitized] ?? sanitized;
}

function normalizeQualityName(qualityName?: string | null) {
  const sanitized = sanitizeKey(qualityName);
  if (!sanitized) return null;
  return sanitized;
}

function buildResolutionKey(width?: number | null, height?: number | null): ResolutionKey | null {
  if (typeof width !== "number" || typeof height !== "number") {
    return null;
  }
  if (!Number.isFinite(width) || !Number.isFinite(height)) {
    return null;
  }
  if (width <= 0 || height <= 0) {
    return null;
  }
  return `${width}x${height}` as ResolutionKey;
}

function resolveOrientationFallback(modelKey: string, orientation?: string | null): ResolutionKey | null {
  if (!orientation) return null;
  const orientationKey = sanitizeKey(orientation) as Orientation | null;
  if (!orientationKey) return null;
  return DEFAULT_RESOLUTION_BY_ORIENTATION[modelKey]?.[orientationKey] ?? null;
}

function unique<T>(values: (T | null | undefined)[]) {
  const seen = new Set<T>();
  const result: T[] = [];
  for (const value of values) {
    if (!value) continue;
    if (!seen.has(value)) {
      seen.add(value);
      result.push(value);
    }
  }
  return result;
}

export interface ImagePricingLookupInput {
  modelName?: string | null;
  qualityName?: string | null;
  width?: number | null;
  height?: number | null;
  orientation?: string | null;
}

export function estimateImageUnitPriceUsd(input: ImagePricingLookupInput): number | null {
  const modelKey = normalizeModelName(input.modelName);
  if (!modelKey) return null;

  const modelPricing = IMAGE_GENERATION_PRICING_USD[modelKey];
  if (!modelPricing) return null;

  const qualityKey = normalizeQualityName(input.qualityName);
  const qualityCandidates = qualityKey
    ? unique([qualityKey])
    : Object.keys(modelPricing);

  const resolutionKey = buildResolutionKey(input.width, input.height);
  const fallbackOrientation = resolveOrientationFallback(modelKey, input.orientation);

  const resolutionCandidates = unique([
    resolutionKey,
    resolutionKey
      ? (resolutionKey.split("x") as [string, string]).reverse().join("x") as ResolutionKey
      : null,
    fallbackOrientation,
  ]);

  for (const qualityCandidate of qualityCandidates) {
    const pricingByResolution = modelPricing[qualityCandidate];
    if (!pricingByResolution) continue;
    for (const resolutionCandidate of resolutionCandidates) {
      const price = pricingByResolution[resolutionCandidate];
      if (typeof price === "number") {
        return price;
      }
    }
  }

  return null;
}

export function estimateImagePackageTotalPriceUsd(
  submission: LeadPortalImagePackage,
): number | null {
  if (typeof submission.imageTotalPriceUsd === "number") {
    return submission.imageTotalPriceUsd;
  }

  const providedUnitPrice =
    typeof submission.imageUnitPriceUsd === "number"
      ? submission.imageUnitPriceUsd
      : null;

  const unitPrice =
    providedUnitPrice ??
    estimateImageUnitPriceUsd({
      modelName: submission.imageModelName ?? submission.model,
      qualityName: submission.imageModelQualityName,
      width: submission.imageWidth,
      height: submission.imageHeight,
      orientation: submission.imageOrientation,
    });

  if (typeof unitPrice !== "number") {
    return null;
  }

  const generatedImages = Math.max(0, submission.generatedImageCount ?? 0);
  const freeImages = Math.max(0, submission.freeImages ?? 0);
  const payableImages = Math.max(0, generatedImages - freeImages);

  return payableImages * unitPrice;
}

export function estimateImagePackageUnitPriceUsd(
  submission: LeadPortalImagePackage,
): number | null {
  if (typeof submission.imageUnitPriceUsd === "number") {
    return submission.imageUnitPriceUsd;
  }

  return estimateImageUnitPriceUsd({
    modelName: submission.imageModelName ?? submission.model,
    qualityName: submission.imageModelQualityName,
    width: submission.imageWidth,
    height: submission.imageHeight,
    orientation: submission.imageOrientation,
  });
}
