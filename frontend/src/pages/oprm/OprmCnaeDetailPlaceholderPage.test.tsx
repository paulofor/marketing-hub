import { cleanup, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
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
              triggerSource: "MANUAL_CNAE_DETAIL",
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
              proximoMovimento:
                "Abrir nova pesquisa priorizando fontes brasileiras recentes dos ultimos 24 meses",
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

    await userEvent.click(
      await screen.findByRole("button", { name: "Criar novo subnicho" }),
    );
    expect(await screen.findByText(/status atual:/i)).toBeTruthy();
    expect(screen.getByText("Custo total do subnicho")).toBeTruthy();
    expect(screen.getByText(/custo do job atual:/i)).toBeTruthy();
    expect(screen.getAllByText("US$ 0,0123").length).toBeGreaterThan(0);
    expect(screen.getByText("Jobs deste subnicho")).toBeTruthy();
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
      screen.getByText(
        /Abrir nova pesquisa priorizando fontes brasileiras recentes/i,
      ),
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

  it("mostra o nome do subnicho identificado durante a execução do pipeline", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: RequestInfo | URL) => {
        const url = input.toString();

        if (url.includes("routine-research-cycle/stage-executions")) {
          return new Response(
            JSON.stringify([
              {
                researchCycleId: 61,
                sourceNicheId: 18,
                cnaeCode: "9602501",
                nicheName: "Manicure autônoma com agenda recorrente",
                originalNicheName: "Cabeleireiros, manicure e pedicure",
                neutralNicheName: "Manicure autônoma com agenda recorrente",
                researchMode: "ROUTINE_REALITY_RESEARCH",
                solutionLanguageRiskScore: 10,
                sourceScore: 90,
                triggerSource: "MANUAL_CNAE_DETAIL",
                status: "RUNNING",
                totalQueries: 12,
                totalSourceCandidates: 20,
                totalSourceSnapshots: 4,
                totalExtractedSignals: 0,
                executionCostUsd: 0.0377,
                cnaeTotalCostUsd: 0.0377,
                startedAt: "2026-06-17T14:03:00Z",
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

    await userEvent.click(
      await screen.findByRole("button", { name: "Criar novo subnicho" }),
    );

    expect(
      await screen.findByText("Subnicho identificado neste pipeline"),
    ).toBeTruthy();
    expect(
      screen.getAllByText("Manicure autônoma com agenda recorrente").length,
    ).toBeGreaterThan(0);
  });

  it("lista ciclos em processamento que ainda nao viraram subnicho", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: RequestInfo | URL) => {
        const url = input.toString();

        if (url.includes("routine-research-cycle/stage-executions")) {
          return new Response(
            JSON.stringify([
              {
                researchCycleId: 62,
                sourceNicheId: 18,
                cnaeCode: "9602501",
                nicheName: "Manicure autônoma para atendimento em domicílio",
                originalNicheName: "Cabeleireiros, manicure e pedicure",
                neutralNicheName:
                  "Manicure autônoma para atendimento em domicílio",
                researchMode: "ROUTINE_REALITY_RESEARCH",
                solutionLanguageRiskScore: 10,
                sourceScore: 90,
                triggerSource: "MANUAL_CNAE_DETAIL",
                status: "RUNNING",
                totalQueries: 4,
                totalSourceCandidates: 8,
                totalSourceSnapshots: 0,
                totalExtractedSignals: 0,
                executionCostUsd: 0.015,
                cnaeTotalCostUsd: 0.0377,
                startedAt: "2026-06-17T14:03:00Z",
                finishedAt: null,
                errorMessage: null,
              },
              {
                researchCycleId: 60,
                sourceNicheId: 18,
                cnaeCode: "9602501",
                nicheName: "Manicure autônoma com agenda recorrente",
                originalNicheName: "Cabeleireiros, manicure e pedicure",
                neutralNicheName: "Manicure autônoma com agenda recorrente",
                researchMode: "ROUTINE_REALITY_RESEARCH",
                solutionLanguageRiskScore: 10,
                sourceScore: 90,
                triggerSource: "MANUAL_CNAE_DETAIL",
                status: "ENRICHED_NICHE_CREATED",
                totalQueries: 12,
                totalSourceCandidates: 20,
                totalSourceSnapshots: 4,
                totalExtractedSignals: 0,
                executionCostUsd: 0.0227,
                cnaeTotalCostUsd: 0.0377,
                startedAt: "2026-06-15T17:03:00Z",
                finishedAt: "2026-06-15T17:39:00Z",
                errorMessage: null,
              },
            ]),
            { status: 200, headers: { "Content-Type": "application/json" } },
          );
        }

        if (url.includes("enriched-niches")) {
          return new Response(
            JSON.stringify([
              {
                enrichedNicheProfileId: 60,
                marketNicheId: 21,
                researchCycleId: 60,
                cnaeCode: "9602501",
                cnaeDescription: "Cabeleireiros, manicure e pedicure",
                nicheName: "Manicure autônoma com agenda recorrente",
                qualityStatus: "MEI_AUDIENCE_READY",
                routineEvidenceScore: 84,
                difficultyEvidenceScore: 82,
                sourceDiversityScore: 100,
                materializedAt: "2026-06-15T17:39:00Z",
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

    expect(
      await screen.findByText("Em processamento antes de virar subnicho"),
    ).toBeTruthy();
    expect(
      await screen.findByText((_, element) =>
        Boolean(element?.textContent?.trim() === "1 em aberto"),
      ),
    ).toBeTruthy();
    expect(
      await screen.findByText(
        "Manicure autônoma para atendimento em domicílio",
      ),
    ).toBeTruthy();
    expect(
      screen.getByText("Ainda não virou subnicho materializado"),
    ).toBeTruthy();
    expect(screen.getByText("#62")).toBeTruthy();
  });

  it("traduz a coluna qualidade dos subnichos gerados para portugues", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: RequestInfo | URL) => {
        const url = input.toString();

        if (url.includes("routine-research-cycle/stage-executions")) {
          return new Response(JSON.stringify([]), {
            status: 200,
            headers: { "Content-Type": "application/json" },
          });
        }

        if (url.includes("enriched-niches")) {
          return new Response(
            JSON.stringify([
              {
                enrichedNicheProfileId: 60,
                marketNicheId: 21,
                researchCycleId: 60,
                cnaeCode: "9602501",
                cnaeDescription: "Cabeleireiros, manicure e pedicure",
                nicheName: "Manicure autônoma com agenda recorrente",
                qualityStatus: "MEI_AUDIENCE_READY",
                routineEvidenceScore: 84,
                difficultyEvidenceScore: 82,
                sourceDiversityScore: 100,
                materializedAt: "2026-06-15T17:39:00Z",
              },
              {
                enrichedNicheProfileId: 11,
                marketNicheId: 18,
                researchCycleId: 11,
                cnaeCode: "9602501",
                cnaeDescription: "Cabeleireiros, manicure e pedicure",
                nicheName: "Cabeleireiros, manicure e pedicure",
                qualityStatus: "LIGHTLY_RESEARCHED",
                routineEvidenceScore: 82,
                difficultyEvidenceScore: 84,
                sourceDiversityScore: 100,
                materializedAt: "2026-06-09T00:40:00Z",
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

    expect(await screen.findByText("Público MEI/autônomo pronto")).toBeTruthy();
    expect(screen.getByText("Pesquisa inicial concluída")).toBeTruthy();
    expect(screen.queryByText("MEI_AUDIENCE_READY")).toBeNull();
    expect(screen.queryByText("LIGHTLY_RESEARCHED")).toBeNull();
  });

  it("informa quando o ciclo atual foi reprocessado automaticamente com aprendizado", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: RequestInfo | URL) => {
        const url = input.toString();

        if (url.includes("routine-research-cycle/stage-executions")) {
          return new Response(
            JSON.stringify([
              {
                researchCycleId: 48,
                sourceNicheId: 1,
                cnaeCode: "9602501",
                nicheName: "Manicure autônoma com agenda instável",
                originalNicheName: "Cabeleireiros, manicure e pedicure",
                neutralNicheName: "Manicure autônoma com agenda instável",
                researchMode: "ROUTINE_REALITY_RESEARCH",
                solutionLanguageRiskScore: 10,
                sourceScore: 90,
                triggerSource: "AUTO_QUALITY_REPROCESS",
                status: "RUNNING",
                totalQueries: 8,
                totalSourceCandidates: 20,
                totalSourceSnapshots: 4,
                totalExtractedSignals: 12,
                executionCostUsd: 0.01,
                cnaeTotalCostUsd: 0.05,
                startedAt: "2026-06-14T19:07:28Z",
                finishedAt: null,
                errorMessage: null,
              },
              {
                researchCycleId: 47,
                sourceNicheId: 1,
                cnaeCode: "9602501",
                nicheName: "Manicure autônoma com agenda instável",
                originalNicheName: "Cabeleireiros, manicure e pedicure",
                neutralNicheName: "Manicure autônoma com agenda instável",
                researchMode: "ROUTINE_REALITY_RESEARCH",
                solutionLanguageRiskScore: 10,
                sourceScore: 90,
                triggerSource: "MANUAL_CNAE_DETAIL",
                status: "TOO_CORPORATE",
                totalQueries: 29,
                totalSourceCandidates: 160,
                totalSourceSnapshots: 17,
                totalExtractedSignals: 59,
                executionCostUsd: 0.0373,
                cnaeTotalCostUsd: 0.05,
                startedAt: "2026-06-14T18:33:52Z",
                finishedAt: "2026-06-14T18:59:59Z",
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

    await userEvent.click(
      await screen.findByRole("button", { name: "Criar novo subnicho" }),
    );
    expect(
      await screen.findByText("Reprocessamento automático em andamento"),
    ).toBeTruthy();
    expect(
      screen.getByText(
        /ciclo #48 foi criado automaticamente depois que o ciclo #47 terminou como corporativo demais/i,
      ),
    ).toBeTruthy();
    expect(
      screen.getByText(/tentativas automáticas usadas: 1\/3/i),
    ).toBeTruthy();
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
                triggerSource: "MANUAL_CNAE_DETAIL",
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

    await userEvent.click(
      await screen.findByRole("button", { name: "Criar novo subnicho" }),
    );
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
