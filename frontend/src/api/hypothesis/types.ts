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
}

export interface HypothesisFrameworkResult {
  desiredResult?: string;
  desiredIdentity?: string;
  businessOutcome?: string;
  successSignal?: string;
  summary?: string;
}

export interface HypothesisFrameworkMechanism {
  core?: string;
  unique?: string;
  visible?: string;
  believability?: string;
  summary?: string;
}

export interface HypothesisFrameworkProof {
  type?: string;
  asset?: string;
  message?: string;
  deliveryStage?: string;
  summary?: string;
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

export function normalizeFramework(
  framework?: HypothesisFramework | null,
): HypothesisFramework {
  return {
    version: framework?.version ?? EMPTY_FRAMEWORK.version,
    pain: { ...EMPTY_FRAMEWORK.pain, ...framework?.pain },
    result: { ...EMPTY_FRAMEWORK.result, ...framework?.result },
    mechanism: { ...EMPTY_FRAMEWORK.mechanism, ...framework?.mechanism },
    proof: { ...EMPTY_FRAMEWORK.proof, ...framework?.proof },
    offer: {
      ...EMPTY_FRAMEWORK.offer,
      ...framework?.offer,
      priceAmount: framework?.offer?.priceAmount ?? undefined,
      offerType: framework?.offer?.offerType ?? undefined,
    },
    checklist: { ...EMPTY_FRAMEWORK.checklist, ...framework?.checklist },
  };
}
