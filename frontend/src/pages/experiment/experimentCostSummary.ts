import type { Experiment } from "../../api/experiment/useExperiments";

export interface ExperimentCostRow {
  label: string;
  value: number;
  currency: "BRL" | "USD";
}

export interface ExperimentCostSummary {
  auditableTotalBrl: number;
  legacyTotalBrl: number;
  unreconciledLegacyCostBrl: number;
  brlRows: ExperimentCostRow[];
  technicalRows: ExperimentCostRow[];
}

export interface ExperimentCostSummaryInput {
  experiment: Experiment;
  contentPipelineCostUsd: number;
  geraLandingCostUsd: number;
  geraSalesPageCostUsd: number;
}

const toNumber = (value: number | string | null | undefined) => {
  if (value == null) return 0;
  if (typeof value === "number") return Number.isFinite(value) ? value : 0;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
};

const roundMoney = (value: number) => Math.round(value * 100) / 100;

export function buildExperimentCostSummary({
  experiment,
  contentPipelineCostUsd,
  geraLandingCostUsd,
  geraSalesPageCostUsd,
}: ExperimentCostSummaryInput): ExperimentCostSummary {
  const originCostBrl = toNumber(experiment.cost);
  const paidMediaCostBrl = toNumber(experiment.campaignMetric?.spend);
  const operationalExpenseBrl = toNumber(experiment.expense);
  const computedAuditableTotalBrl = roundMoney(
    originCostBrl + paidMediaCostBrl + operationalExpenseBrl,
  );
  const auditableTotalBrl = roundMoney(
    toNumber(experiment.auditableTotalCost) || computedAuditableTotalBrl,
  );
  const legacyTotalBrl = roundMoney(
    toNumber(experiment.legacyTotalCost) || toNumber(experiment.totalCost),
  );
  const unreconciledLegacyCostBrl = roundMoney(
    Math.max(
      0,
      toNumber(experiment.unreconciledLegacyCost) ||
        legacyTotalBrl - auditableTotalBrl,
    ),
  );

  return {
    auditableTotalBrl,
    legacyTotalBrl,
    unreconciledLegacyCostBrl,
    brlRows: [
      {
        label: "Custo de origem",
        value: originCostBrl,
        currency: "BRL",
      },
      {
        label: "Mídia paga",
        value: paidMediaCostBrl,
        currency: "BRL",
      },
      {
        label: "Despesa operacional",
        value: operationalExpenseBrl,
        currency: "BRL",
      },
    ],
    technicalRows: [
      {
        label: "Pipeline de conteúdo",
        value: contentPipelineCostUsd,
        currency: "USD",
      },
      {
        label: "GeraLanding",
        value: geraLandingCostUsd,
        currency: "USD",
      },
      {
        label: "GeraSalesPage",
        value: geraSalesPageCostUsd,
        currency: "USD",
      },
    ],
  };
}
