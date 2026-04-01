export interface CampaignAngleSummary {
  primaryPromise?: string;
  primaryPain?: string;
  mechanismSummary?: string;
  proofSummary?: string;
  cta?: string;
  singleMindedPromise?: string;
  primaryCTA?: string;
  landingMatchLine?: string;
  funnelStage?: string;
  tone?: string;
}

const TEXT_KEYS: (keyof CampaignAngleSummary)[] = [
  "primaryPromise",
  "primaryPain",
  "mechanismSummary",
  "proofSummary",
  "cta",
  "singleMindedPromise",
  "primaryCTA",
  "landingMatchLine",
  "funnelStage",
  "tone",
];

const SAFE_ALIASES: Record<keyof CampaignAngleSummary, string[]> = {
  primaryPromise: ["campaignAngle", "mainPromise", "promise", "primaryOutcome"],
  primaryPain: ["pain", "corePain"],
  mechanismSummary: ["mechanism", "mechanismExplained"],
  proofSummary: ["proofUsed", "socialProof", "evidence"],
  cta: ["callToAction", "ctaText"],
  singleMindedPromise: ["singlePromise", "corePromise"],
  primaryCTA: ["mainCTA", "ctaPrincipal"],
  landingMatchLine: ["landingMessageBridge", "messageMatchLine"],
  funnelStage: ["funnelStageName", "funnel"],
  tone: ["toneOfVoice", "voice", "style"],
};

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

function mapCandidate(payload: Record<string, unknown>): CampaignAngleSummary {
  const summary: CampaignAngleSummary = {};

  for (const key of TEXT_KEYS) {
    const value = payload[key];
    const aliasList = SAFE_ALIASES[key] ?? [];
    const fallback = aliasList
      .map((alias) => pickText(payload[alias]))
      .find((text): text is string => Boolean(text));
    summary[key] = pickText(value) ?? fallback;
  }

  return summary;
}

export function hasCampaignAngleContent(
  summary?: CampaignAngleSummary | null,
): summary is CampaignAngleSummary {
  if (!summary) return false;
  return TEXT_KEYS.some((key) => {
    const value = summary[key];
    return typeof value === "string" && value.trim().length > 0;
  });
}

export function parseCampaignAnglePayload(
  raw?: string | null,
): CampaignAngleSummary | undefined {
  if (raw == null) return undefined;
  const trimmed = typeof raw === "string" ? raw.trim() : "";
  if (typeof raw === "string" && trimmed.length === 0) {
    return undefined;
  }

  const base = typeof raw === "string" ? safeParseJson(trimmed) : raw;
  const candidates = collectObjects(base ?? raw);

  for (const payload of candidates) {
    const summary = mapCandidate(payload);
    if (hasCampaignAngleContent(summary)) {
      return summary;
    }
  }

  return undefined;
}
