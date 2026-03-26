import type { ExperimentStage } from "../../api/experiment/useExperiments";

export const experimentStageLabels: Record<ExperimentStage, string> = {
  AD: "Anúncio",
  LANDING: "Landing / Formulário",
  SAMPLE: "Amostra",
  SALES: "Oferta / Venda",
};

export function getExperimentStageLabel(stage?: ExperimentStage | null) {
  if (!stage) return "—";
  return experimentStageLabels[stage] ?? stage;
}
