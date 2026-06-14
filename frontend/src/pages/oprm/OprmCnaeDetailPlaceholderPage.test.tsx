import { cleanup, render, screen, within } from "@testing-library/react";
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
    cleanup();
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
              executionCostUsd: 0.0123,
              cnaeTotalCostUsd: 0.0456,
              startedAt: "2026-06-12T19:08:26Z",
              finishedAt: null,
              errorMessage: null,
            },
          ]),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }

      if (url.includes("routine-quality-gate/stage-executions/24")) {
        return new Response(
          JSON.stringify({
            researchCycleId: 24,
            cycleStatus: "OUTDATED_SOURCES",
            routineCardId: 15,
            qualityStatus: "OUTDATED_SOURCES",
            readyForHypothesis: false,
            specificityScore: 71,
            confidenceScore: 43,
            duplicationScore: 0,
            routineEvidenceScore: 64,
            difficultyEvidenceScore: 0,
            sourceDiversityScore: 80,
            solutionLanguageRiskScore: 20,
            qualityNotes: {
              status: "OUTDATED_SOURCES",
              proximoMovimentoCodigo: "BUSCAR_FONTES_BRASILEIRAS_RECENTES",
              proximoMovimento: "Abrir nova pesquisa priorizando fontes brasileiras recentes dos ultimos 24 meses",
              fontes: 54,
              sinais: 85,
              tarefasConcretasDistintas: 0,
              aquisicaoOuCanal: 12,
              dorPratica: 0,
              riscoFonteAntiga: 85,
              fontesRecentesSuficientes: true,
              riscoEmpresaEstruturada: 30,
              riscoLinguagemSolucao: 20,
              dominadoPorSolucao: false,
              rotinaRevelaTarefasReaisExecutor: false,
              mixMinimoMeiAutonomo: false,
              faltaEvidenciaAquisicaoCanaisRecorrenciaOuComportamentoClientes: true,
            },
            checkedBy: "oprmRoutineQualityGate",
            checkedAt: "2026-06-12T19:40:05Z",
          }),
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
    expect(screen.getByText("Custo total do CNAE")).toBeTruthy();
    expect(screen.getAllByText("US$ 0,0456").length).toBeGreaterThan(0);
    expect(screen.getByText(/custo do job atual:/i)).toBeTruthy();
    expect(screen.getAllByText("US$ 0,0123").length).toBeGreaterThan(0);
    expect(screen.getByText("Jobs executados para este CNAE")).toBeTruthy();
    expect(
      screen.getByText(/bloqueada por fontes antigas ou sem atualidade/i),
    ).toBeTruthy();
    expect(
      screen.getByText(/o pipeline não está em execução agora/i),
    ).toBeTruthy();
    expect(
      await screen.findByText("Situações rejeitadas pelo gate"),
    ).toBeTruthy();
    expect(screen.getByText(/Resultado apurado:/i)).toBeTruthy();
    expect(screen.getByText(/54 fontes · 85 sinais/i)).toBeTruthy();
    expect(screen.getByText("Próximo movimento automático")).toBeTruthy();
    expect(
      screen.getByText(/Abrir nova pesquisa priorizando fontes brasileiras recentes/i),
    ).toBeTruthy();
    expect(screen.getByText(/Atualidade das fontes: risco 85%/i)).toBeTruthy();
    expect(screen.getByText(/Rotina do executor insuficiente/i)).toBeTruthy();
    expect(screen.getByText(/Dor prática ausente/i)).toBeTruthy();

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

  it("diferencia visualmente os cards concluídos e em execução por contraste forte", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: RequestInfo | URL) => {
        const url = input.toString();

        if (url.includes("routine-research-cycle/stage-executions")) {
          return new Response(
            JSON.stringify([
              {
                researchCycleId: 31,
                sourceNicheId: 1,
                cnaeCode: "9602501",
                nicheName: "Cabeleireiros, manicure e pedicure",
                status: "RUNNING",
                totalQueries: 48,
                totalSourceCandidates: 340,
                totalSourceSnapshots: 104,
                totalExtractedSignals: 266,
                startedAt: "2026-06-13T07:31:00Z",
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
      }),
    );

    renderPage();

    await screen.findByText(/status atual:/i);

    const seedCard = screen.getByText("2. Seed");
    expect(seedCard.closest(".card")?.className).toContain("bg-success");
    expect(seedCard.closest(".card")?.className).toContain("text-white");
    expect(
      within(seedCard.closest(".card") as HTMLElement).getByLabelText(
        "Etapa com uso direto de IA",
      ),
    ).toBeTruthy();

    const searchCard = screen.getByText("3. Busca").closest(".card");
    expect(
      within(searchCard as HTMLElement).getByLabelText(
        "Etapa que acessa a internet para pesquisar fontes",
      ),
    ).toBeTruthy();

    const fetchCard = screen.getByText("4. Coleta").closest(".card");
    expect(
      within(fetchCard as HTMLElement).getByLabelText(
        "Etapa que acessa a internet para pesquisar fontes",
      ),
    ).toBeTruthy();

    const synthesisCard = screen.getByText("6. Síntese").closest(".card");
    expect(synthesisCard?.className).toContain("bg-primary-subtle");
    expect(synthesisCard?.className).toContain("border-2");

    const meiCard = screen.getByText("7. MEI").closest(".card");
    expect(meiCard?.className).toContain("bg-body-tertiary");
    expect(
      within(meiCard as HTMLElement).getByLabelText(
        "Etapa com uso direto de IA",
      ),
    ).toBeTruthy();
  });
});
