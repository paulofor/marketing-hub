import { describe, expect, it } from "vitest";
import { buildExperimentCostSummary } from "./experimentCostSummary";
import type { Experiment } from "../../api/experiment/useExperiments";

const baseExperiment = {
  id: "59",
  nicheId: 1,
  hypothesisId: "hypothesis-59",
  name: "Experimento 59",
  hypothesis: "Hipótese",
  startDate: null,
  endDate: null,
  creativeApproved: true,
  status: "RUNNING",
  platform: "FACEBOOK",
  stage: "AD",
  createdAt: "2026-07-07T00:00:00Z",
  updatedAt: "2026-07-07T00:00:00Z",
} satisfies Experiment;

describe("buildExperimentCostSummary", () => {
  it("prioritizes auditable BRL cost and isolates unreconciled legacy difference", () => {
    const summary = buildExperimentCostSummary({
      experiment: {
        ...baseExperiment,
        cost: 0,
        expense: 0,
        campaignMetric: { spend: 12.33 },
        totalCost: 91.31,
      },
      contentPipelineCostUsd: 0.01,
      geraLandingCostUsd: 0,
      geraSalesPageCostUsd: 1.460795,
    });

    expect(summary.auditableTotalBrl).toBe(12.33);
    expect(summary.legacyTotalBrl).toBe(91.31);
    expect(summary.unreconciledLegacyCostBrl).toBe(78.98);
    expect(summary.brlRows.map((row) => row.currency)).toEqual([
      "BRL",
      "BRL",
      "BRL",
    ]);
    expect(summary.technicalRows.map((row) => row.currency)).toEqual([
      "USD",
      "USD",
      "USD",
    ]);
  });

  it("uses backend reconciliation fields when available", () => {
    const summary = buildExperimentCostSummary({
      experiment: {
        ...baseExperiment,
        cost: 99,
        expense: 99,
        campaignMetric: { spend: 99 },
        auditableTotalCost: 19.56,
        legacyTotalCost: 91.15,
        unreconciledLegacyCost: 71.59,
      },
      contentPipelineCostUsd: 0,
      geraLandingCostUsd: 0,
      geraSalesPageCostUsd: 0,
    });

    expect(summary.auditableTotalBrl).toBe(19.56);
    expect(summary.legacyTotalBrl).toBe(91.15);
    expect(summary.unreconciledLegacyCostBrl).toBe(71.59);
  });
});
