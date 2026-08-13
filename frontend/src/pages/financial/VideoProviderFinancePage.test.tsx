import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import VideoProviderFinancePage from "./VideoProviderFinancePage";

const refetch = vi.fn();

vi.mock("../../api/financial/useVideoProviderCreditBalances", () => ({
  useVideoProviderCreditBalances: () => ({
    data: [
      {
        provider: "RUNWAY",
        status: "DIVERGENT_PROVIDER_REJECTION",
        balanceNature: "ESTIMATED_FROM_PURCHASES_AND_LEDGER",
        purchasedCredits: 20,
        estimatedConsumedCredits: 0,
        estimatedAvailableCredits: 20,
        referenceModel: "Runway Gen-4 Turbo",
        referenceClipSeconds: 10,
        referenceClipCredits: 50,
        estimatedReferenceClips: 0,
        lastPurchaseAt: "2026-08-13T12:00:00Z",
        lastCreditFailureAt: "2026-08-13T16:50:00Z",
        lastCreditFailureJobId: 20993,
        lastCreditFailureDetail: "not enough credits",
        knownConsumedCostUsd: 0,
        unknownCostAttempts: 0,
        acceptedSceneRequests: 13,
        sceneRequests: [
          {
            jobId: 21105,
            productionCycleId: 6,
            sceneNumber: 1,
            plannedSceneCount: 3,
            providerTaskId: "task-abc",
            model: "seedance2_5",
            durationSeconds: 10,
            estimatedCredits: 300,
            estimatedCostUsd: 3,
            billedCredits: 300,
            billedCostUsd: 3,
            settlementStatus: "CHARGED",
            settlementBasis: "PROVIDER_REPORTED",
            billingEvidence: "PROVIDER_RATE_CARD_AND_TASK_SUCCESS",
            acceptedAt: "2026-08-13T18:10:35Z",
          },
        ],
        creditsUrl: "https://dev.runwayml.com/",
      },
    ],
    isLoading: false,
    isError: false,
    isFetching: false,
    refetch,
  }),
}));

describe("VideoProviderFinancePage", () => {
  it("expõe a divergência real sem vincular o saldo ao MUSA", async () => {
    render(<VideoProviderFinancePage />);

    expect(
      screen.getByText("Financeiro de provedores de vídeo"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Divergente: provedor recusou"),
    ).toBeInTheDocument();
    expect(screen.getByText(/job #20993/i)).toBeInTheDocument();
    expect(screen.getByText(/0 clipes/i)).toBeInTheDocument();
    expect(screen.getByText("13")).toBeInTheDocument();
    expect(screen.getByText("1/3")).toBeInTheDocument();
    expect(screen.getByText("task-abc")).toBeInTheDocument();
    expect(screen.getByText(/seedance2_5/)).toBeInTheDocument();
    expect(
      screen.getByText(/300 créditos confirmados · US\$ 3,00/),
    ).toBeInTheDocument();
    expect(
      screen.getByText("PROVIDER_RATE_CARD_AND_TASK_SUCCESS"),
    ).toBeInTheDocument();
    expect(screen.queryByText(/MUSA/i)).not.toBeInTheDocument();
    const portal = screen.getByRole("link", { name: /conferir no portal/i });
    expect(portal).toHaveAttribute("target", "_blank");

    await userEvent.click(
      screen.getByRole("button", { name: /atualizar monitor/i }),
    );
    expect(refetch).toHaveBeenCalled();
  });
});
