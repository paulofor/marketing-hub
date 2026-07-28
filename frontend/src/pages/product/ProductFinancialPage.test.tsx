import { cleanup, render, screen, within } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import ProductFinancialPage from "./ProductFinancialPage";

vi.mock("axios");
vi.mock("echarts-for-react", () => ({
  default: () => <div data-testid="product-financial-chart" />,
}));

describe("ProductFinancialPage", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    window.history.pushState({}, "", "/products/1/financial");
  });

  afterEach(() => {
    cleanup();
  });

  it("shows monthly and annual product financials from backend", async () => {
    (axios.get as any).mockResolvedValue({
      data: {
        productId: 1,
        productName: "Método MUSA",
        productSlug: "metodo-musa-7-dias",
        exchangeRateBrlPerUsd: 5,
        monthStart: "2026-07-01T00:00:00Z",
        yearStart: "2026-01-01T00:00:00Z",
        monthlyResults: [
          {
            monthStart: "2026-07-01T00:00:00Z",
            monthLabel: "Julho 2026",
            cost: { brl: 75, usd: 15 },
            revenue: { brl: 67, usd: 13.4 },
            profit: { brl: -8, usd: -1.6 },
          },
          {
            monthStart: "2026-06-01T00:00:00Z",
            monthLabel: "Junho 2026",
            cost: { brl: 25, usd: 5 },
            revenue: { brl: 120, usd: 24 },
            profit: { brl: 95, usd: 19 },
          },
        ],
        costs: [
          {
            type: "VIDEO_PRODUCTION",
            label: "Produção de vídeo",
            monthly: { brl: 50, usd: 10 },
            annual: { brl: 150, usd: 30 },
            source: "Vídeos de experimentos",
          },
          {
            type: "MEDIA",
            label: "Mídia paga",
            monthly: { brl: 25, usd: 5 },
            annual: { brl: 250, usd: 50 },
            source: "Métricas de campanha",
          },
        ],
        revenue: {
          type: "SALES",
          label: "Receitas de vendas",
          monthly: { brl: 67, usd: 13.4 },
          annual: { brl: 670, usd: 134 },
          source: "Vendas aprovadas",
        },
        profit: {
          type: "PROFIT",
          label: "Lucro",
          monthly: { brl: -8, usd: -1.6 },
          annual: { brl: 270, usd: 54 },
          source: "Receita menos custos",
        },
      },
    });
    const client = new QueryClient();

    render(
      <QueryClientProvider client={client}>
        <BrowserRouter>
          <Routes>
            <Route
              path="/products/:productId/financial"
              element={<ProductFinancialPage />}
            />
          </Routes>
        </BrowserRouter>
      </QueryClientProvider>,
    );

    expect(
      await screen.findByRole("heading", { name: /Financeiro do produto/i }),
    ).toBeTruthy();
    expect(await screen.findByText(/Método MUSA/i)).toBeTruthy();
    expect(screen.getByText(/R\$ 5 por US\$ 1/i)).toBeTruthy();
    expect(screen.getByText("Evolução financeira")).toBeTruthy();
    expect(screen.getByText("Composição do custo mensal")).toBeTruthy();
    expect(screen.getAllByTestId("product-financial-chart")).toHaveLength(2);
    expect(screen.getByText("Resultado dos últimos 4 meses")).toBeTruthy();

    const revenueTile = screen.getByText("Receita mensal").closest("section");
    expect(revenueTile).toBeTruthy();
    expect(
      within(revenueTile as HTMLElement).getByText("R$ 67,00", {
        selector: "strong",
      }),
    ).toBeTruthy();
    expect(
      within(revenueTile as HTMLElement).getByText("$13.40", {
        selector: "small",
      }),
    ).toBeTruthy();

    const costTile = screen.getByText("Custo mensal").closest("section");
    expect(costTile).toBeTruthy();
    expect(
      within(costTile as HTMLElement).getByText("R$ 75,00", {
        selector: "strong",
      }),
    ).toBeTruthy();
    expect(
      within(costTile as HTMLElement).getByText("$15.00", {
        selector: "small",
      }),
    ).toBeTruthy();

    const profitTile = screen.getByText("Lucro anual").closest("section");
    expect(profitTile).toBeTruthy();
    expect(
      within(profitTile as HTMLElement).getByText("R$ 270,00", {
        selector: "strong",
      }),
    ).toBeTruthy();
    expect(
      within(profitTile as HTMLElement).getByText("$54.00", {
        selector: "small",
      }),
    ).toBeTruthy();

    const julyRow = screen.getByText("Julho 2026").closest("tr");
    expect(julyRow).toBeTruthy();
    expect(within(julyRow as HTMLElement).getByText("R$ 75,00")).toBeTruthy();
    expect(within(julyRow as HTMLElement).getByText("$15.00")).toBeTruthy();
    expect(within(julyRow as HTMLElement).getByText("R$ 67,00")).toBeTruthy();
    expect(within(julyRow as HTMLElement).getByText("-R$ 8,00")).toBeTruthy();

    const videoRow = screen.getByText("Produção de vídeo").closest("tr");
    expect(videoRow).toBeTruthy();
    expect(within(videoRow as HTMLElement).getByText("$10.00")).toBeTruthy();
    expect(within(videoRow as HTMLElement).getByText("R$ 50,00")).toBeTruthy();

    const profitRow = screen.getByText("Receita menos custos").closest("tr");
    expect(profitRow).toBeTruthy();
    expect(within(profitRow as HTMLElement).getByText("$54.00")).toBeTruthy();
    expect(
      within(profitRow as HTMLElement).getByText("R$ 270,00"),
    ).toBeTruthy();
  });
});
