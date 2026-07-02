import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import CommercialPlanningPage from "./CommercialPlanningPage";

vi.mock("../../api/planning/useCommercialPlans", async () => {
  const actual = await vi.importActual<
    typeof import("../../api/planning/useCommercialPlans")
  >("../../api/planning/useCommercialPlans");

  return {
    ...actual,
    useCommercialPlans: () => ({
      data: [
        {
          id: 1,
          name: "Plano sem marcos",
          planType: "FIRST_SALE",
          status: "DRAFT",
          maxBudget: 300,
          targetRevenue: 27,
          operationalRevenueTarget: 81,
          experimentsToCreate: 2,
          experimentsToPublish: 3,
          daysRemaining: 7,
          milestones: null,
          simulations: null,
        },
      ],
      isLoading: false,
      isError: false,
    }),
    useCreateCommercialPlan: () => ({ isPending: false }),
    useUpdateCommercialPlan: () => ({ isPending: false }),
    useSimulateCommercialPlan: () => ({
      isPending: false,
      mutateAsync: vi.fn(),
    }),
  };
});

vi.mock("../../api/niche/useNiches", () => ({
  useNiches: () => ({ data: null }),
}));

vi.mock("../../api/hypothesis/useHypotheses", () => ({
  useHypotheses: () => ({ data: null }),
}));

vi.mock("../../api/experiment/useExperiments", () => ({
  useExperiments: () => ({ data: null }),
}));

describe("CommercialPlanningPage", () => {
  it("renderiza o planejamento mesmo quando listas auxiliares ou marcos vêm vazios", () => {
    render(<CommercialPlanningPage />);

    expect(screen.getByText("Planejamento")).toBeTruthy();
    expect(screen.getAllByText("Plano sem marcos").length).toBeGreaterThan(0);
    expect(screen.getByText("Novo Plano de Primeira Venda")).toBeTruthy();
    expect(
      screen.getByText("Nenhum marco cadastrado para este plano."),
    ).toBeTruthy();
  });

  it("preenche o formulario com o planejamento de julho em tres cenarios", async () => {
    render(<CommercialPlanningPage />);

    await userEvent.click(screen.getAllByText("Usar planejamento de julho")[0]);

    expect(
      screen.getByDisplayValue("Planejamento Julho 2026 - Primeira venda"),
    ).toBeTruthy();
    expect(screen.getByDisplayValue("2026-07-31")).toBeTruthy();
    expect(screen.getByDisplayValue("300")).toBeTruthy();
    expect(screen.getByDisplayValue("27")).toBeTruthy();
    expect(screen.getByDisplayValue("81")).toBeTruthy();
    expect(screen.getByDisplayValue("2")).toBeTruthy();
    expect(screen.getByDisplayValue("3")).toBeTruthy();
    expect(screen.getByDisplayValue(/Compra aprovada/)).toBeTruthy();
    expect(screen.getByDisplayValue(/Cenario venda direta/)).toBeTruthy();
  });
});
