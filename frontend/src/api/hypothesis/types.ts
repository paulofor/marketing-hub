export type HypothesisFrameworkSection =
  | "pain"
  | "result"
  | "mechanism"
  | "proof"
  | "offer";

export interface HypothesisFrameworkPain {
  surface?: string;
  root?: string;
  emotional?: string;
  social?: string;
  cost?: string;
  summary?: string;
  evidenceSignals?: string[];
}

export interface HypothesisFrameworkResult {
  desiredResult?: string;
  desiredIdentity?: string;
  businessOutcome?: string;
  successSignal?: string;
  summary?: string;
  evidenceSignals?: string[];
}

export interface HypothesisFrameworkMechanism {
  core?: string;
  unique?: string;
  visible?: string;
  believability?: string;
  summary?: string;
  evidenceSignals?: string[];
}

export interface HypothesisFrameworkProof {
  type?: string;
  asset?: string;
  message?: string;
  deliveryStage?: string;
  summary?: string;
  evidenceSignals?: string[];
}

export interface HypothesisFrameworkOffer {
  name?: string;
  corePromise?: string;
  deliverables?: string;
  riskReversal?: string;
  priceLogic?: string;
  cta?: string;
  priceAmount?: number;
  offerType?: string;
  summary?: string;
  evidenceSignals?: string[];
}

export interface HypothesisFrameworkChecklist {
  painReady?: boolean;
  resultReady?: boolean;
  mechanismReady?: boolean;
  proofReady?: boolean;
  offerReady?: boolean;
  approvedForExperiment?: boolean;
  notes?: string;
}

export interface HypothesisFramework {
  version?: string;
  pain: HypothesisFrameworkPain;
  result: HypothesisFrameworkResult;
  mechanism: HypothesisFrameworkMechanism;
  proof: HypothesisFrameworkProof;
  offer: HypothesisFrameworkOffer;
  checklist: HypothesisFrameworkChecklist;
}

export const EMPTY_FRAMEWORK: HypothesisFramework = {
  version: "dor-resultado-mecanismo-prova-oferta/v1",
  pain: {},
  result: {},
  mechanism: {},
  proof: {},
  offer: {},
  checklist: {
    painReady: false,
    resultReady: false,
    mechanismReady: false,
    proofReady: false,
    offerReady: false,
    approvedForExperiment: false,
    notes: "",
  },
};

type UnknownRecord = Record<string, unknown>;

function isRecord(value: unknown): value is UnknownRecord {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function parseJsonObject(value: unknown): UnknownRecord | undefined {
  if (isRecord(value)) return value;
  if (typeof value !== "string") return undefined;
  const trimmed = value.trim();
  if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return undefined;

  try {
    const parsed = JSON.parse(trimmed) as unknown;
    return isRecord(parsed) ? parsed : undefined;
  } catch {
    return undefined;
  }
}

function normalizeSectionRecord(section: unknown): UnknownRecord {
  const direct = parseJsonObject(section) ?? (isRecord(section) ? section : {});
  const embedded = Object.values(direct)
    .map(parseJsonObject)
    .find((value) => value && Object.keys(value).length > 1);

  return { ...direct, ...(embedded ?? {}) };
}

function stringFrom(record: UnknownRecord, ...keys: string[]) {
  for (const key of keys) {
    const value = record[key];
    if (typeof value === "string" && value.trim()) {
      const parsed = parseJsonObject(value);
      if (!parsed) return value;
    }
    if (typeof value === "number") return String(value);
    if (Array.isArray(value)) return value.filter(Boolean).join("\n");
  }
  return undefined;
}

function numberFrom(record: UnknownRecord, ...keys: string[]) {
  for (const key of keys) {
    const value = record[key];
    if (typeof value === "number") return value;
    if (typeof value === "string" && value.trim()) {
      const parsed = Number(value.replace(",", "."));
      if (Number.isFinite(parsed)) return parsed;
    }
  }
  return undefined;
}

function stringArrayFrom(record: UnknownRecord, ...keys: string[]) {
  for (const key of keys) {
    const value = record[key];
    if (Array.isArray(value)) {
      return value
        .filter((item): item is string => typeof item === "string")
        .filter((item) => item.trim());
    }
  }
  return undefined;
}

function normalizePainSection(section: unknown): HypothesisFrameworkPain {
  const record = normalizeSectionRecord(section);
  return {
    surface: stringFrom(record, "surface"),
    root: stringFrom(record, "root"),
    emotional: stringFrom(record, "emotional"),
    social: stringFrom(record, "social"),
    cost: stringFrom(record, "cost"),
    summary: stringFrom(record, "summary"),
    evidenceSignals: stringArrayFrom(record, "evidenceSignals"),
  };
}

function normalizeResultSection(section: unknown): HypothesisFrameworkResult {
  const record = normalizeSectionRecord(section);
  return {
    desiredResult: stringFrom(record, "desiredResult", "desiredOutcome"),
    desiredIdentity: stringFrom(
      record,
      "desiredIdentity",
      "beforeAfterContrast",
    ),
    businessOutcome: stringFrom(record, "businessOutcome", "businessValue"),
    successSignal: stringFrom(record, "successSignal", "measurableChange"),
    summary: stringFrom(record, "summary"),
    evidenceSignals: stringArrayFrom(record, "evidenceSignals"),
  };
}

function normalizeMechanismSection(
  section: unknown,
): HypothesisFrameworkMechanism {
  const record = normalizeSectionRecord(section);
  return {
    core: stringFrom(record, "core", "coreMechanism"),
    unique: stringFrom(record, "unique", "mechanismName"),
    visible: stringFrom(record, "visible", "howItWorks", "steps"),
    believability: stringFrom(record, "believability", "whyBelievable"),
    summary: stringFrom(record, "summary"),
    evidenceSignals: stringArrayFrom(record, "evidenceSignals"),
  };
}

function normalizeProofSection(section: unknown): HypothesisFrameworkProof {
  const record = normalizeSectionRecord(section);
  return {
    type: stringFrom(record, "type", "proofType"),
    asset: stringFrom(record, "asset", "proofAsset"),
    message: stringFrom(record, "message", "proofMessage"),
    deliveryStage: stringFrom(record, "deliveryStage", "collectionMethod"),
    summary: stringFrom(record, "summary"),
    evidenceSignals: stringArrayFrom(record, "evidenceSignals"),
  };
}

function normalizeOfferSection(section: unknown): HypothesisFrameworkOffer {
  const record = normalizeSectionRecord(section);
  return {
    name: stringFrom(record, "name", "offerName"),
    corePromise: stringFrom(record, "corePromise", "coreOffer"),
    deliverables: stringFrom(record, "deliverables", "steps"),
    riskReversal: stringFrom(record, "riskReversal", "objectionReduced"),
    priceLogic: stringFrom(record, "priceLogic", "whyBelievable"),
    cta: stringFrom(record, "cta"),
    priceAmount: numberFrom(record, "priceAmount"),
    offerType: stringFrom(record, "offerType"),
    summary: stringFrom(record, "summary"),
    evidenceSignals: stringArrayFrom(record, "evidenceSignals"),
  };
}

export function normalizeFramework(framework?: unknown): HypothesisFramework {
  const source: UnknownRecord = isRecord(framework) ? framework : {};
  return {
    version:
      typeof source.version === "string"
        ? source.version
        : EMPTY_FRAMEWORK.version,
    pain: { ...EMPTY_FRAMEWORK.pain, ...normalizePainSection(source.pain) },
    result: {
      ...EMPTY_FRAMEWORK.result,
      ...normalizeResultSection(source.result),
    },
    mechanism: {
      ...EMPTY_FRAMEWORK.mechanism,
      ...normalizeMechanismSection(source.mechanism),
    },
    proof: { ...EMPTY_FRAMEWORK.proof, ...normalizeProofSection(source.proof) },
    offer: {
      ...EMPTY_FRAMEWORK.offer,
      ...normalizeOfferSection(source.offer),
    },
    checklist: {
      ...EMPTY_FRAMEWORK.checklist,
      ...(isRecord(source.checklist) ? source.checklist : {}),
    },
  };
}
