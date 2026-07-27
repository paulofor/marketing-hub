import { cleanup, render, screen, within } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import ProductExperimentComparisonPage from "./ProductExperimentComparisonPage";

vi.mock("axios");

describe("ProductExperimentComparisonPage", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    window.history.pushState({}, "", "/products/1/experiment-comparison");
  });

  afterEach(() => {
    cleanup();
  });

  it("shows the product experiment comparison returned by backend", async () => {
    (axios.get as any).mockResolvedValue({
      data: {
        productId: 1,
        productName: "Método MUSA",
        productSlug: "metodo-musa-7-dias",
        commercialStatus: "VALIDACAO_COMERCIAL",
        mainRecommendation:
          "Priorizar correção da ativação/funil antes de comparar novos criativos ou públicos.",
        experiments: [
          {
            experimentId: 69,
            name: "MUSA-H001-E007",
            status: "USER_STOPPED",
            campaignStatus: "PAUSED",
            campaignObjective: "SALES",
            experimentType: "PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL",
            startDate: "2026-07-24",
            endDate: "2026-07-31",
            dailyBudget: 25,
            unitPrice: 67,
            impressions: 500,
            reach: 450,
            clicks: 10,
            leads: 0,
            spend: 6.25,
            cpc: 0.62,
            cpl: 0,
            approvedCreatives: 1,
            totalCreatives: 2,
            funnelStages: [],
            hypothesis: "Público interrompido",
            promise: "Elegância prática",
            learnedLessons: "Interrompido antes de leitura conclusiva.",
            recommendedAction:
              "Aguardar novo ciclo antes de tomar decisão comercial.",
            updatedAt: "2026-07-26T12:00:00Z",
          },
          {
            experimentId: 74,
            name: "MUSA-H001-E009",
            status: "RUNNING",
            campaignStatus: "ACTIVE",
            campaignObjective: "SALES",
            experimentType: "PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL",
            startDate: "2026-07-27",
            endDate: "2026-08-03",
            dailyBudget: 25,
            unitPrice: 67,
            impressions: 1000,
            reach: 900,
            clicks: 20,
            leads: 0,
            spend: 12.5,
            cpc: 0.62,
            cpl: 0,
            approvedCreatives: 3,
            totalCreatives: 3,
            funnelStages: [
              {
                stageCode: "ACESSO_FORM_LEAD",
                stageLabel: "Acesso ao formulário de lead",
                total: 5,
              },
            ],
            hypothesis: "Público amplo Meta",
            promise: "Elegância possível em 7 dias",
            learnedLessons: "Clique barato, mas ativação precisa melhorar.",
            recommendedAction:
              "Corrigir ativação pós-clique: o anúncio gera interesse, mas o funil não registra entrada.",
            updatedAt: "2026-07-27T12:00:00Z",
          },
        ],
      },
    });
    const client = new QueryClient();

    render(
      <QueryClientProvider client={client}>
        <BrowserRouter>
          <Routes>
            <Route
              path="/products/:productId/experiment-comparison"
              element={<ProductExperimentComparisonPage />}
            />
          </Routes>
        </BrowserRouter>
      </QueryClientProvider>,
    );

    expect(
      await screen.findByRole("heading", {
        name: /Comparativo de experimentos/i,
      }),
    ).toBeTruthy();
    expect(await screen.findByText(/Método MUSA/i)).toBeTruthy();
    expect(screen.getByText(/Priorizar correção da ativação/i)).toBeTruthy();
    expect(screen.getByRole("link", { name: /MUSA-H001-E009/i })).toHaveAttribute(
      "href",
      "/experiments/74",
    );
    const rows = screen.getAllByRole("row");
    expect(
      within(rows[1]).getByRole("link", { name: /MUSA-H001-E009/i }),
    ).toBeTruthy();
    expect(rows[1]).toHaveClass("product-comparison-table__row--running");
    expect(screen.getByText(/Clique barato, mas ativação precisa melhorar/i)).toBeTruthy();
  });
});
