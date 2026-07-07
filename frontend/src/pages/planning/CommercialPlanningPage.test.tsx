import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import CommercialPlanningPage from "./CommercialPlanningPage";

const defaultPlans = [
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
];

let mockPlans: unknown[] = defaultPlans;

vi.mock("../../api/planning/useCommercialPlans", async () => {
  const actual = await vi.importActual<
    typeof import("../../api/planning/useCommercialPlans")
  >("../../api/planning/useCommercialPlans");

  return {
    ...actual,
    useCommercialPlans: () => ({
      data: mockPlans,
      isLoading: false,
      isError: false,
    }),
  };
});

afterEach(() => {
  mockPlans = defaultPlans;
  cleanup();
});

describe("CommercialPlanningPage", () => {
  it("renderiza somente o planejamento superior", () => {
    render(<CommercialPlanningPage />);

    expect(screen.getByText("Planejamento")).toBeTruthy();
    expect(screen.getAllByText("Plano sem marcos").length).toBeGreaterThan(0);
    expect(screen.getByText("Plano do mês corrente")).toBeTruthy();
    expect(screen.queryByText("Planos de Primeira Venda")).toBeNull();
    expect(screen.queryByText("Novo Plano de Primeira Venda")).toBeNull();
  });

  it("usa valores seguros quando status vem fora do contrato", () => {
    render(<CommercialPlanningPage />);

    expect(screen.getAllByText("Rascunho").length).toBeGreaterThan(0);
  });

  it("renderiza sugestao de julho quando a API ainda nao retorna planos", () => {
    mockPlans = [];

    render(<CommercialPlanningPage />);

    expect(screen.getByText("Planejamento")).toBeTruthy();
    expect(
      screen.getByText("Planejamento Julho 2026 - Primeira venda"),
    ).toBeTruthy();
    expect(screen.getByText("Plano sugerido")).toBeTruthy();
  });
});
