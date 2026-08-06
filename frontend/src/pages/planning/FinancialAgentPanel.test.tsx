import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { StudioProviderEfficiencyTable } from "./FinancialAgentPanel";

describe("StudioProviderEfficiencyTable", () => {
  it("mostra custo por aprovado e bloqueia comparacao com custo incompleto", () => {
    render(
      <StudioProviderEfficiencyTable
        rows={[
          {
            provider: "RUNWAY",
            totalAttempts: 10,
            knownCostAttempts: 9,
            unknownCostAttempts: 1,
            knownCostUsd: 9.6,
            reviewedAssets: 8,
            approvedAssets: 6,
            pendingReviewAssets: 1,
            commercialApprovalRatePercent: 75,
            knownCostPerApprovedAssetUsd: 1.6,
            decisionCoverage: "INCOMPLETE_COSTS",
          },
        ]}
      />,
    );

    expect(screen.getByText("75%")).toBeInTheDocument();
    expect(screen.getByText("US$ 1.6")).toBeInTheDocument();
    expect(screen.getByText("1 custo(s) desconhecido(s)")).toBeInTheDocument();
    expect(screen.getByText(/não autoriza compra de créditos/i)).toBeInTheDocument();
  });
});
