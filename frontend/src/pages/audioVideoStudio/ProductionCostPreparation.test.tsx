import {
  render,
  screen,
  fireEvent,
  waitFor,
  cleanup,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, expect, it, vi } from "vitest";
import axios from "axios";
import { ProductionCostPreparation } from "./ProductionCostPreparation";
vi.mock("axios");
afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

it.each([
  [14, 4, 114, 51],
  [81, 44, 215, 91],
])(
  "prepara custos do projeto %s e reavalia o mesmo ciclo sem teto novo",
  async (id, productId, experimentId, cycleId) => {
    (axios.put as any).mockResolvedValue({
      data: {
        id: cycleId,
        status: "PENDING_FINANCIAL_REVIEW",
        budgetLimitUsd: 15,
      },
    });
    const project = {
      id,
      productId,
      experimentId,
      providerPlan: "RUNWAY_GEN_4_5",
      targetDurationSeconds: 32,
    } as any;
    const cycle = {
      id: cycleId,
      status: "PROVIDER_PREFLIGHT_BLOCKED",
      providerPreflight: { failureCode: "PROVIDER_FULL_COST_PLAN_REQUIRED" },
    } as any;
    render(
      <QueryClientProvider client={new QueryClient()}>
        <ProductionCostPreparation project={project} cycle={cycle} />
      </QueryClientProvider>,
    );
    screen
      .getAllByLabelText("Máximo restante em US$ *")
      .forEach((input) =>
        fireEvent.change(input, { target: { value: "0.50" } }),
      );
    screen
      .getAllByLabelText("Fonte ou evidência *")
      .forEach((input) =>
        fireEvent.change(input, {
          target: { value: "Tarifa e consumo verificados; fixture local" },
        }),
      );
    fireEvent.change(screen.getByLabelText("Validade da previsão (UTC) *"), {
      target: { value: "2099-01-01T12:00" },
    });
    fireEvent.submit(
      screen.getByRole("form", { name: "Preparação integral de custos" }),
    );
    await waitFor(() =>
      expect(axios.put).toHaveBeenCalledWith(
        `/api/sales-videos/projects/${id}/autonomy/v1/cycles/${cycleId}/production-cost-plan`,
        expect.objectContaining({
          cycleId,
          productId,
          experimentId,
          unresolvedAttemptIds: [],
        }),
      ),
    );
    const payload = (axios.put as any).mock.calls[0][1];
    expect(Object.keys(payload.remainingCosts)).toHaveLength(7);
    expect(payload.budgetLimitUsd).toBeUndefined();
    expect(payload.remainingCosts.AGENTS.maximumUsd).toBe(0.5);
    expect(await screen.findByRole("status")).toHaveTextContent(
      "Teto preservado: US$ 15.00",
    );
  },
);

it("não oferece edição de previsão de um ciclo já encaminhado", () => {
  render(
    <QueryClientProvider client={new QueryClient()}>
      <ProductionCostPreparation
        project={{ id: 14 } as any}
        cycle={{ id: 51, status: "PENDING_FINANCIAL_REVIEW" } as any}
      />
    </QueryClientProvider>,
  );
  expect(screen.queryByRole("form")).toBeNull();
});
