export type JsonRecord = Record<string, unknown>;

export function pickText(value: unknown): string | undefined {
  if (typeof value !== "string") return undefined;
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : undefined;
}

export function pickNumber(value: unknown): number | undefined {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === "string") {
    const parsed = Number(value.replace(/[^0-9.-]/g, ""));
    if (!Number.isNaN(parsed)) {
      return parsed;
    }
  }
  return undefined;
}

export function toStringArray(value: unknown): string[] | undefined {
  if (Array.isArray(value)) {
    const normalized = value
      .map((item) => (typeof item === "string" ? item.trim() : ""))
      .filter((item) => item.length > 0);
    return normalized.length > 0 ? normalized : undefined;
  }
  if (typeof value === "string") {
    const trimmed = value.trim();
    if (trimmed.length === 0) return undefined;
    return [trimmed];
  }
  return undefined;
}

export function extractObjectCandidates(source?: string | null): JsonRecord[] {
  if (source == null) return [];
  if (typeof source !== "string") {
    if (isRecord(source)) {
      return collectObjects(source);
    }
    return [];
  }
  const trimmed = source.trim();
  if (trimmed.length === 0) {
    return [];
  }
  return collectObjects(trimmed);
}

function collectObjects(value: unknown): JsonRecord[] {
  if (value == null) return [];

  if (Array.isArray(value)) {
    return value.flatMap((entry) => collectObjects(entry));
  }

  if (typeof value === "object") {
    if (isRecord(value)) {
      const fromNestedStrings = Object.values(value)
        .filter((entry): entry is string => typeof entry === "string")
        .flatMap((entry) => collectObjects(entry));
      return [
        value,
        ...Object.values(value).flatMap((entry) => collectObjects(entry)),
        ...fromNestedStrings,
      ];
    }
    return [];
  }

  if (typeof value === "string") {
    const direct = safeParseJson(value);
    if (isRecord(direct)) {
      return collectObjects(direct);
    }
    const embedded = parseEmbeddedJson(value);
    return embedded ? collectObjects(embedded) : [];
  }

  return [];
}

function safeParseJson(value: string): unknown {
  try {
    return JSON.parse(value) as unknown;
  } catch {
    return undefined;
  }
}

function parseEmbeddedJson(text: string): JsonRecord | undefined {
  const fenced = text.match(/```(?:json)?\s*([\s\S]*?)```/i)?.[1]?.trim();
  if (fenced) {
    const parsedFence = safeParseJson(fenced);
    if (isRecord(parsedFence)) {
      return parsedFence;
    }
  }
  const firstBrace = text.indexOf("{");
  const lastBrace = text.lastIndexOf("}");
  if (firstBrace >= 0 && lastBrace > firstBrace) {
    const candidate = text.slice(firstBrace, lastBrace + 1);
    const parsedCandidate = safeParseJson(candidate);
    if (isRecord(parsedCandidate)) {
      return parsedCandidate;
    }
  }
  return undefined;
}

function isRecord(value: unknown): value is JsonRecord {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}
