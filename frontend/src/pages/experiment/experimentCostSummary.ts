import type { Experiment } from "../../api/experiment/useExperiments";

export interface ExperimentCostRow {
  label: string;
  value: number;
  currency: "BRL" | "USD";
  convertedValueBrl?: number;
}

export interface ExperimentCostSummary {
  auditableTotalBrl: number;
  technicalTotalBrl: number;
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
const TECHNICAL_COST_USD_TO_BRL = 5;

export function buildExperimentCostSummary({
  experiment,
  contentPipelineCostUsd,
  geraLandingCostUsd,
  geraSalesPageCostUsd,
}: ExperimentCostSummaryInput): ExperimentCostSummary {
  const originCostBrl = toNumber(experiment.cost);
  const paidMediaCostBrl = toNumber(experiment.campaignMetric?.spend);
  const operationalExpenseBrl = toNumber(experiment.expense);
  const technicalTotalUsd =
    contentPipelineCostUsd + geraLandingCostUsd + geraSalesPageCostUsd;
  const technicalTotalBrl = roundMoney(
    technicalTotalUsd * TECHNICAL_COST_USD_TO_BRL,
  );
  const computedAuditableBaseBrl = roundMoney(
    originCostBrl + paidMediaCostBrl + operationalExpenseBrl,
  );
  const auditableBaseBrl =
    toNumber(experiment.auditableTotalCost) || computedAuditableBaseBrl;
  const auditableTotalBrl = roundMoney(
    auditableBaseBrl + technicalTotalBrl,
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
    technicalTotalBrl,
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
        convertedValueBrl: roundMoney(
          contentPipelineCostUsd * TECHNICAL_COST_USD_TO_BRL,
        ),
      },
      {
        label: "GeraLanding",
        value: geraLandingCostUsd,
        currency: "USD",
        convertedValueBrl: roundMoney(
          geraLandingCostUsd * TECHNICAL_COST_USD_TO_BRL,
        ),
      },
      {
        label: "GeraSalesPage",
        value: geraSalesPageCostUsd,
        currency: "USD",
        convertedValueBrl: roundMoney(
          geraSalesPageCostUsd * TECHNICAL_COST_USD_TO_BRL,
        ),
      },
    ],
  };
}
