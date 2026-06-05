import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import OprmEnrichedNicheDetailPage from "./OprmEnrichedNicheDetailPage";

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
      <MemoryRouter initialEntries={["/oprm/enriched-niches/profile/1"]}>
        <Routes>
          <Route
            path="/oprm/enriched-niches/profile/:profileId"
            element={<OprmEnrichedNicheDetailPage />}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("OprmEnrichedNicheDetailPage", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("busca o perfil enriquecido na URL absoluta do backend", async () => {
    const fetchMock = vi.fn(async () =>
      new Response(
        JSON.stringify({
          researchCycleId: 1,
          cycleStatus: "ENRICHED_NICHE_CREATED",
          routineCardId: 1,
          marketNicheId: 18,
          enrichedNicheProfileId: 1,
          nicheName: "IA para crescimento de Cabeleireiros",
          cnaeCode: "9602501",
          qualityStatus: "LIGHTLY_RESEARCHED",
          routineSummary: "Rotina operacional observada.",
          painsSummary: "Dores observadas.",
          resultsSummary: "Resultados desejados.",
          mechanismOpportunitiesSummary: "Mecanismos plausíveis.",
          evidenceSummary: "Evidências coletadas.",
          sourceDomains: "exemplo.com.br",
          materializedAt: "2026-06-05T02:00:12Z",
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    renderPage();

    expect(
      await screen.findByText("IA para crescimento de Cabeleireiros"),
    ).toBeTruthy();
    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "http://localhost:8000/api/oprm/nichocnae/enriched-niche-materializer/profiles/1",
      );
    });
  });
});
