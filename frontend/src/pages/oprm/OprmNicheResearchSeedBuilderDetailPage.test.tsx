import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import OprmNicheResearchSeedBuilderDetailPage from "./OprmNicheResearchSeedBuilderDetailPage";

function renderPage() {
  const client = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter
        initialEntries={["/oprm/pipeline/niche-research-seed-builder/1"]}
      >
        <Routes>
          <Route
            path="/oprm/pipeline/niche-research-seed-builder/:researchCycleId"
            element={<OprmNicheResearchSeedBuilderDetailPage />}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("OprmNicheResearchSeedBuilderDetailPage", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("mostra a requisição reconstruída da IA e o JSON gerado", async () => {
    const fetchMock = vi.fn(
      async () =>
        new Response(
          JSON.stringify({
            researchCycleId: 1,
            cycleStatus: "RUNNING",
            cycleTotalQueries: 1,
            cycleErrorMessage: null,
            seed: {
              researchCycleId: 1,
              nicheResearchSeedId: 1,
              cnaeCode: "9602501",
              cnaeDescription: "Cabeleireiros, manicure e pedicure",
              nicheName:
                "IA para crescimento de Cabeleireiros, manicure e pedicure",
              businessType: "Serviços pessoais de beleza",
              operationType: "Pesquisa de rotina",
              customerType: "Profissional de beleza",
              commercialObjects: "serviços de corte, manicure e pedicure",
              initialAssumptions:
                "Profissionais enfrentam desafios na gestão de agenda.",
              confidenceLevel: "INFERRED_FROM_CNAE",
              createdBy: "AI",
              createdAt: "2026-06-03T17:45:44Z",
              totalQueries: 1,
              queries: [
                {
                  queryId: 1,
                  researchCycleId: 1,
                  nicheResearchSeedId: 1,
                  queryText:
                    "Quais são as etapas diárias na rotina de trabalho?",
                  queryGoal: "ROUTINE_DISCOVERY",
                  sourceGroup: "rotina",
                  priority: 1,
                  status: "PENDING",
                  resultCount: 0,
                  createdBy: "AI",
                  createdAt: "2026-06-03T17:45:44Z",
                  updatedAt: "2026-06-03T17:45:44Z",
                },
              ],
            },
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        ),
    );
    vi.stubGlobal("fetch", fetchMock);

    renderPage();

    expect(
      await screen.findByText("Detalhe da IA — Seed de Pesquisa do Nicho"),
    ).toBeTruthy();
    expect(
      await screen.findByText("Requisição enviada para a IA"),
    ).toBeTruthy();
    expect(await screen.findByText("JSON gerado e gravado")).toBeTruthy();
    expect(
      screen.getAllByText(/https:\/\/api.openai.com\/v1\/responses/).length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText(/oprm_niche_research_seed_builder/).length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText(/Quais são as etapas diárias na rotina de trabalho\?/)
        .length,
    ).toBeGreaterThan(0);
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("niche-research-seed-builder/stage-executions/1"),
    );
  });
});
