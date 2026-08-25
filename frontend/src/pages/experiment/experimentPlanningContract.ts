import type {
  ExperimentStage,
  ExperimentType,
  ProductAiSubtype,
} from "../../api/experiment/useExperiments";

export interface ExperimentIdentityFields {
  stage: ExperimentStage;
  primaryVariable?: string;
  primaryMetric?: string;
}

export function productAiSubtypeForExperiment(
  experimentType: ExperimentType,
  productAiSubtype: ProductAiSubtype | "",
): ProductAiSubtype | undefined {
  return experimentType === "LOW_TICKET_PRODUCT" && productAiSubtype
    ? productAiSubtype
    : undefined;
}

export function parseOptionalPositiveAmount(
  value: string,
): number | undefined | null {
  if (!value.trim()) return undefined;
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
}

export function parseOptionalConversionRate(
  value: string,
): number | undefined | null {
  if (!value.trim()) return undefined;
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed >= 0 && parsed <= 100
    ? parsed
    : null;
}

export function parseOptionalEntityId(value: string): number | null {
  if (!value.trim()) return null;
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

export function experimentIdentityFields(
  stage: ExperimentStage,
  primaryVariable: string,
  primaryMetric: string,
): ExperimentIdentityFields {
  return {
    stage,
    primaryVariable: primaryVariable.trim() || undefined,
    primaryMetric: primaryMetric.trim() || undefined,
  };
}
