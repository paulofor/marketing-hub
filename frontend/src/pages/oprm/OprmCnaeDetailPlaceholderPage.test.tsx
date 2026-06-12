import { render, screen, within } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import OprmCnaeDetailPlaceholderPage from "./OprmCnaeDetailPlaceholderPage";

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
      <MemoryRouter initialEntries={["/oprm/cnaes/9602501"]}>
        <Routes>
          <Route
            path="/oprm/cnaes/:cnaeCode"
            element={<OprmCnaeDetailPlaceholderPage />}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("OprmCnaeDetailPlaceholderPage", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("mostra bloqueio de qualidade em vez de síntese em execução para fontes antigas", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = input.toString();

      if (url.includes("latest-volume")) {
        return new Response(
          JSON.stringify({
            cnaeCode: "9602501",
            cnaeDescription: "Cabeleireiros, manicure e pedicure",
            opportunityScore: 90,
            totalEstabelecimentos: 100,
            totalEstabelecimentosAtivos: 80,
            totalEmpresasMei: 70,
            totalEmpresas: 100,
            scoreStatus: "SCORED",
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }

      if (url.includes("opportunity-score")) {
        return new Response(
          JSON.stringify({
            cnaeCode: "9602501",
            cnaeDescription: "Cabeleireiros, manicure e pedicure",
            opportunityScore: 90,
            marketVolumeScore: 90,
            meiDensityScore: 90,
            digitalFitScore: 90,
            painClarityScore: 90,
            scoreStatus: "SCORED",
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }

      if (url.includes("routine-research-cycle/stage-executions")) {
        return new Response(
          JSON.stringify([
            {
              researchCycleId: 24,
              sourceNicheId: 1,
              cnaeCode: "9602501",
              nicheName: "Cabeleireiros, manicure e pedicure",
              originalNicheName:
                "IA para crescimento de Cabeleireiros, manicure e pedicure",
              neutralNicheName: "Cabeleireiros, manicure e pedicure",
              researchMode: "ROUTINE_REALITY_RESEARCH",
              solutionLanguageRiskScore: 100,
              sourceScore: 90,
              status: "OUTDATED_SOURCES",
              totalQueries: 48,
              totalSourceCandidates: 340,
              totalSourceSnapshots: 104,
              totalExtractedSignals: 266,
              startedAt: "2026-06-12T19:08:26Z",
              finishedAt: null,
              errorMessage: null,
            },
          ]),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }

      return new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });
    vi.stubGlobal("fetch", fetchMock);

    renderPage();

    expect(await screen.findByText(/status atual:/i)).toBeTruthy();
    expect(
      screen.getByText(/bloqueada por fontes antigas ou sem atualidade/i),
    ).toBeTruthy();
    expect(
      screen.getByText(/o pipeline não está em execução agora/i),
    ).toBeTruthy();

    const synthesisCard = screen.getByText("6. Síntese").closest(".card");
    expect(synthesisCard).toBeTruthy();
    expect(
      within(synthesisCard as HTMLElement).getByText("Concluído"),
    ).toBeTruthy();

    const qualityCard = screen.getByText("8. Qualidade").closest(".card");
    expect(qualityCard).toBeTruthy();
    expect(
      within(qualityCard as HTMLElement).getByText("Fontes antigas"),
    ).toBeTruthy();

    const materializationCard = screen
      .getByText("9. Materialização")
      .closest(".card");
    expect(materializationCard).toBeTruthy();
    expect(
      within(materializationCard as HTMLElement).getByText("Bloqueado"),
    ).toBeTruthy();
    expect(screen.queryByText("Em execução")).toBeNull();
  });
});
