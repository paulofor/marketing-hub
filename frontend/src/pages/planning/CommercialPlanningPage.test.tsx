import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
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
        {
          id: 2,
          name: "Plano com dados inesperados",
          planType: "FIRST_SALE",
          status: "UNKNOWN_STATUS",
          daysRemaining: null,
          milestones: [
            {
              id: 10,
              sequenceOrder: 1,
              code: "UNKNOWN",
              name: "Marco com status inesperado",
              status: "UNKNOWN_MILESTONE_STATUS",
            },
          ],
          simulations: [
            {
              id: 20,
              recommendation: "UNKNOWN_RECOMMENDATION",
              mostLikelyScenario: "Cenario com valor inesperado.",
            },
          ],
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

afterEach(() => {
  cleanup();
});

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

  it("renderiza plano mesmo quando status e recomendacao vêm fora do contrato", async () => {
    render(<CommercialPlanningPage />);

    await userEvent.click(screen.getByText("Plano com dados inesperados"));

    expect(screen.getAllByText("Rascunho").length).toBeGreaterThan(0);
    expect(screen.getByText("Corrigir")).toBeTruthy();
    expect(screen.getByText(/Marco com status inesperado/)).toBeTruthy();
    expect(screen.getByText(/Pendente/)).toBeTruthy();
  });
});
