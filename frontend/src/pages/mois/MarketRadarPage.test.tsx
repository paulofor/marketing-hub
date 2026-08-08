import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { expect, test, vi } from "vitest";
import MarketRadarPage from "./MarketRadarPage";

vi.mock("../../api/mois/useMoisSalesLibrary", () => ({
  useMoisSalesLibraryOpportunityRanking: () => ({
    isLoading: false,
    isError: false,
    data: {
      items: [
        {
          pageId: 7,
          title: "Produto observado",
          source: "HOTMART",
          combinedCommercialScore: 82,
          pageScoreTotal: 80,
          warmupScoreTotal: 84,
          marketTemperature: "HOT",
          ecosystemType: "COMPETITORS_HEATED",
          recommendation: "PRIORITIZE",
          evidenceSummary: "Evidência rastreável.",
          suggestedNextAction: "Validar hipótese própria.",
        },
      ],
    },
  }),
}));

test("exibe oportunidade sem tratá-la como venda comprovada", () => {
  render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter>
        <MarketRadarPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
  expect(
    screen.getByText("Radar de produtos e oportunidades"),
  ).toBeInTheDocument();
  expect(screen.getByText("Produto observado")).toBeInTheDocument();
  expect(screen.getByText(/Ranking não comprova vendas/i)).toBeInTheDocument();
  expect(screen.getByText("Priorizar validação")).toBeInTheDocument();
});
