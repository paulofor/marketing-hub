export interface AdCopyVariant {
  label?: string;
  primaryText?: string;
  headline?: string;
  description?: string;
  ctaText?: string;
}

export interface AdCopyContent {
  primaryTextVariants: AdCopyVariant[];
}

function pickText(value: unknown): string | undefined {
  if (typeof value !== "string") return undefined;
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : undefined;
}

function safeParseJson(value: string): unknown {
  try {
    return JSON.parse(value) as unknown;
  } catch {
    return undefined;
  }
}

function parseEmbeddedJson(text: string): Record<string, unknown> | undefined {
  const fenced = text.match(/```(?:json)?\s*([\s\S]*?)```/i)?.[1]?.trim();
  if (fenced) {
    const parsedFence = safeParseJson(fenced);
    if (parsedFence && typeof parsedFence === "object" && !Array.isArray(parsedFence)) {
      return parsedFence as Record<string, unknown>;
    }
  }

  const firstBrace = text.indexOf("{");
  const lastBrace = text.lastIndexOf("}");
  if (firstBrace >= 0 && lastBrace > firstBrace) {
    const candidate = text.slice(firstBrace, lastBrace + 1);
    const parsedCandidate = safeParseJson(candidate);
    if (
      parsedCandidate &&
      typeof parsedCandidate === "object" &&
      !Array.isArray(parsedCandidate)
    ) {
      return parsedCandidate as Record<string, unknown>;
    }
  }

  return undefined;
}

function collectObjects(value: unknown): Record<string, unknown>[] {
  if (value == null) return [];

  if (Array.isArray(value)) {
    return value.flatMap((item) => collectObjects(item));
  }

  if (typeof value === "object") {
    const asRecord = value as Record<string, unknown>;
    const nestedFromStrings = Object.values(asRecord).flatMap((item) =>
      typeof item === "string"
        ? (() => {
            const parsed = parseEmbeddedJson(item);
            return parsed ? [parsed] : [];
          })()
        : [],
    );

    return [
      asRecord,
      ...Object.values(asRecord).flatMap((item) => collectObjects(item)),
      ...nestedFromStrings,
    ];
  }

  if (typeof value === "string") {
    const direct = safeParseJson(value);
    if (direct && typeof direct === "object" && !Array.isArray(direct)) {
      return [direct as Record<string, unknown>];
    }
    const embedded = parseEmbeddedJson(value);
    return embedded ? [embedded] : [];
  }

  return [];
}

function parseVariant(value: unknown): AdCopyVariant | undefined {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    return undefined;
  }

  const payload = value as Record<string, unknown>;
  const variant: AdCopyVariant = {
    label: pickText(payload.label) ?? pickText(payload.variant),
    primaryText: pickText(payload.primaryText) ?? pickText(payload.text),
    headline: pickText(payload.headline) ?? pickText(payload.title),
    description: pickText(payload.description) ?? pickText(payload.subtitle),
    ctaText: pickText(payload.ctaText) ?? pickText(payload.cta),
  };

  const hasAnyValue = Object.values(variant).some(
    (field) => typeof field === "string" && field.trim().length > 0,
  );
  return hasAnyValue ? variant : undefined;
}

function mapCandidate(payload: Record<string, unknown>): AdCopyContent | undefined {
  const rawVariants = payload.primaryTextVariants ?? payload.variants;
  if (!Array.isArray(rawVariants)) {
    return undefined;
  }

  const parsedVariants = rawVariants
    .map((variant) => parseVariant(variant))
    .filter((variant): variant is AdCopyVariant => Boolean(variant));

  if (parsedVariants.length === 0) {
    return undefined;
  }

  return {
    primaryTextVariants: parsedVariants,
  };
}

export function hasAdCopyContent(
  payload?: AdCopyContent | null,
): payload is AdCopyContent {
  if (!payload) return false;
  return payload.primaryTextVariants.length > 0;
}

export function parseAdCopyPayload(raw?: string | null): AdCopyContent | undefined {
  if (raw == null) return undefined;
  const trimmed = typeof raw === "string" ? raw.trim() : "";
  if (typeof raw === "string" && trimmed.length === 0) {
    return undefined;
  }

  const base = typeof raw === "string" ? safeParseJson(trimmed) : raw;
  const candidates = collectObjects(base ?? raw);

  for (const payload of candidates) {
    const parsed = mapCandidate(payload);
    if (hasAdCopyContent(parsed)) {
      return parsed;
    }
  }

  return undefined;
}
