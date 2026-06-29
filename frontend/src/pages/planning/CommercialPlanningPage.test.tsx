import { render, screen } from "@testing-library/react";
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
  });
});
