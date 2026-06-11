import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import OprmPipelinePage from "./OprmPipelinePage";

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
      <MemoryRouter initialEntries={["/oprm/pipeline"]}>
        <OprmPipelinePage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("OprmPipelinePage", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("mostra erro de continuidade no card da etapa 1 quando a fila da etapa 2 falha", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = input.toString();
      if (url.includes("routine-research-orchestrator/recent-processed")) {
        return new Response(
          JSON.stringify([
            {
              researchCycleId: 1,
              sourceNicheId: 1,
              cnaeCode: "9602501",
              cnaeDescription: "Cabeleireiros, manicure e pedicure",
              nicheName:
                "IA para crescimento de Cabeleireiros, manicure e pedicure",
              originalNicheName:
                "IA para crescimento de Cabeleireiros, manicure e pedicure",
              neutralNicheName: "Cabeleireiros, manicure e pedicure",
              researchMode: "ROUTINE_REALITY_RESEARCH",
              solutionLanguageRiskScore: 65,
              sourceScore: 90,
              triggerSource: "AUTO_SCORE_QUEUE",
              cycleStatus: "RUNNING",
              processedAt: "2026-06-03T06:59:59Z",
              finishedAt: null,
              errorMessage: null,
            },
          ]),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }

      if (
        url.includes("niche-research-seed-builder/stage-executions/pending")
      ) {
        return new Response("upstream timeout", { status: 503 });
      }

      return new Response("[]", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });
    vi.stubGlobal("fetch", fetchMock);

    renderPage();

    expect(await screen.findByText("Execução mais recente")).toBeTruthy();
    expect(screen.getByText("#1")).toBeTruthy();
    expect(screen.getAllByText("9602501").length).toBeGreaterThan(0);
    expect(
      screen.getAllByText("Cabeleireiros, manicure e pedicure").length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText("Pesquisa de rotina real").length,
    ).toBeGreaterThan(0);
    expect(screen.getAllByText("Em execução").length).toBeGreaterThan(0);

    await waitFor(() => {
      const alerts = screen.getAllByRole("alert");
      expect(
        alerts.some((alert) =>
          alert.textContent?.includes("Etapa seguinte não inicializou"),
        ),
      ).toBe(true);
      expect(
        alerts.some((alert) =>
          alert.textContent?.includes(
            "Não foi possível validar a fila da etapa seguinte",
          ),
        ),
      ).toBe(true);
    });
    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining(
          "niche-research-seed-builder/stage-executions/pending",
        ),
      );
    });
  });

  it("cria novo ciclo imediato para reprocessar CNAE com ciclo falho", async () => {
    const fetchMock = vi.fn(
      async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = input.toString();
        if (
          url.includes("routine-research-orchestrator/recent-processed") &&
          init?.method === "POST"
        ) {
          return new Response(
            JSON.stringify({
              researchCycleId: 3,
              sourceNicheId: 1,
              cnaeCode: "9602501",
              cnaeDescription: "Cabeleireiros, manicure e pedicure",
              previousCycleStatus: "FAILED",
              previousRoutineResearchStatus: "RESEARCH_RUNNING",
              routineResearchStatus: "RESEARCH_RUNNING",
              lastRoutineResearchCycleId: 3,
              message:
                "Novo ciclo de pesquisa de rotina criado imediatamente para reprocessar o CNAE com falha.",
            }),
            { status: 200, headers: { "Content-Type": "application/json" } },
          );
        }

        if (url.includes("routine-research-orchestrator/recent-processed")) {
          return new Response(
            JSON.stringify([
              {
                researchCycleId: 1,
                sourceNicheId: 1,
                cnaeCode: "9602501",
                cnaeDescription: "Cabeleireiros, manicure e pedicure",
                nicheName:
                  "IA para crescimento de Cabeleireiros, manicure e pedicure",
                originalNicheName:
                  "IA para crescimento de Cabeleireiros, manicure e pedicure",
                neutralNicheName: "Cabeleireiros, manicure e pedicure",
                researchMode: "ROUTINE_REALITY_RESEARCH",
                solutionLanguageRiskScore: 65,
                sourceScore: 90,
                triggerSource: "AUTO_SCORE_QUEUE",
                cycleStatus: "FAILED",
                processedAt: "2026-06-03T06:59:59Z",
                finishedAt: "2026-06-03T07:10:00Z",
                errorMessage: "Falha ao gerar seed da etapa dois",
              },
            ]),
            { status: 200, headers: { "Content-Type": "application/json" } },
          );
        }

        return new Response("[]", {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      },
    );
    vi.stubGlobal("fetch", fetchMock);

    renderPage();

    fireEvent.click(
      await screen.findByRole("button", { name: "Reprocessar CNAE" }),
    );

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining(
          "routine-research-orchestrator/recent-processed/1/reprocess",
        ),
        { method: "POST" },
      );
    });
    expect(
      await screen.findByText(
        "Novo ciclo de pesquisa de rotina criado imediatamente para reprocessar o CNAE com falha.",
      ),
    ).toBeTruthy();
    expect(
      screen.getAllByText("Etapa 2 · Seed de Pesquisa do Nicho").length,
    ).toBeGreaterThan(0);
  });

  it("permite criar novo ciclo quando o status precisa de mais pesquisa", async () => {
    const fetchMock = vi.fn(
      async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = input.toString();
        if (
          url.includes("routine-research-orchestrator/recent-processed") &&
          init?.method === "POST"
        ) {
          return new Response(
            JSON.stringify({
              researchCycleId: 6,
              sourceNicheId: 1,
              cnaeCode: "9602501",
              cnaeDescription: "Cabeleireiros, manicure e pedicure",
              previousCycleStatus: "NEEDS_MORE_RESEARCH",
              previousRoutineResearchStatus: "RESEARCH_RUNNING",
              routineResearchStatus: "RESEARCH_RUNNING",
              lastRoutineResearchCycleId: 6,
              message:
                "Novo ciclo de pesquisa de rotina criado imediatamente para aprofundar um CNAE que precisava de mais pesquisa.",
            }),
            { status: 200, headers: { "Content-Type": "application/json" } },
          );
        }

        if (url.includes("routine-research-orchestrator/recent-processed")) {
          return new Response(
            JSON.stringify([
              {
                researchCycleId: 5,
                sourceNicheId: 1,
                cnaeCode: "9602501",
                cnaeDescription: "Cabeleireiros, manicure e pedicure",
                nicheName: "Cabeleireiros, manicure e pedicure",
                originalNicheName: "Cabeleireiros, manicure e pedicure",
                neutralNicheName: "Cabeleireiros, manicure e pedicure",
                researchMode: "ROUTINE_REALITY_RESEARCH",
                solutionLanguageRiskScore: 0,
                sourceScore: 90,
                triggerSource: "AUTO_SCORE_QUEUE",
                cycleStatus: "NEEDS_MORE_RESEARCH",
                processedAt: "2026-06-07T14:11:00Z",
                finishedAt: null,
                errorMessage: null,
              },
            ]),
            { status: 200, headers: { "Content-Type": "application/json" } },
          );
        }

        return new Response("[]", {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      },
    );
    vi.stubGlobal("fetch", fetchMock);

    renderPage();

    fireEvent.click(
      await screen.findByRole("button", { name: "Pesquisar novamente" }),
    );

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining(
          "routine-research-orchestrator/recent-processed/5/reprocess",
        ),
        { method: "POST" },
      );
    });
    expect(
      await screen.findByText(
        "Novo ciclo de pesquisa de rotina criado imediatamente para aprofundar um CNAE que precisava de mais pesquisa.",
      ),
    ).toBeTruthy();
  });

  it("permite refazer pelo front-end quando a materialização do nicho enriquecido falha", async () => {
    const fetchMock = vi.fn(
      async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = input.toString();
        if (
          url.includes("routine-research-orchestrator/recent-processed") &&
          init?.method === "POST"
        ) {
          return new Response(
            JSON.stringify({
              researchCycleId: 12,
              sourceNicheId: 1,
              cnaeCode: "9602501",
              cnaeDescription: "Cabeleireiros, manicure e pedicure",
              previousCycleStatus: "ENRICHED_NICHE_FAILED",
              previousRoutineResearchStatus: "RESEARCH_RUNNING",
              routineResearchStatus: "RESEARCH_RUNNING",
              lastRoutineResearchCycleId: 12,
              message:
                "Novo ciclo de pesquisa de rotina criado imediatamente para refazer pelo front-end um CNAE aprovado cuja materialização falhou.",
            }),
            { status: 200, headers: { "Content-Type": "application/json" } },
          );
        }

        if (url.includes("routine-research-orchestrator/recent-processed")) {
          return new Response(
            JSON.stringify([
              {
                researchCycleId: 11,
                sourceNicheId: 1,
                cnaeCode: "9602501",
                cnaeDescription: "Cabeleireiros, manicure e pedicure",
                nicheName: "Cabeleireiros, manicure e pedicure",
                originalNicheName:
                  "IA para crescimento de Cabeleireiros, manicure e pedicure",
                neutralNicheName: "Cabeleireiros, manicure e pedicure",
                researchMode: "ROUTINE_REALITY_RESEARCH",
                solutionLanguageRiskScore: 100,
                sourceScore: 90,
                triggerSource: "AUTO_SCORE_QUEUE",
                cycleStatus: "ENRICHED_NICHE_FAILED",
                processedAt: "2026-06-08T20:33:56Z",
                finishedAt: "2026-06-09T01:50:04Z",
                errorMessage: "NullPointerException",
              },
            ]),
            { status: 200, headers: { "Content-Type": "application/json" } },
          );
        }

        if (url.includes("enriched-niche-materializer/stage-executions/11")) {
          return new Response(
            JSON.stringify({
              researchCycleId: 11,
              cycleStatus: "ENRICHED_NICHE_FAILED",
              routineCardId: 8,
              marketNicheId: null,
              enrichedNicheProfileId: null,
              originalNicheName:
                "IA para crescimento de Cabeleireiros, manicure e pedicure",
              neutralNicheName: "Cabeleireiros, manicure e pedicure",
              researchMode: "ROUTINE_REALITY_RESEARCH",
              solutionLanguageRiskScore: 100,
              nicheName: "Cabeleireiros, manicure e pedicure",
              cnaeCode: "9602501",
              qualityStatus: "LIGHTLY_RESEARCHED",
              routineSummary: null,
              painsSummary: null,
              resultsSummary: null,
              mechanismOpportunitiesSummary: null,
              evidenceSummary: null,
              sourceDomains: null,
              routineEvidenceScore: 82,
              difficultyEvidenceScore: 84,
              sourceDiversityScore: 100,
              materializedSolutionLanguageRiskScore: 18,
              materializedAt: null,
            }),
            { status: 200, headers: { "Content-Type": "application/json" } },
          );
        }

        return new Response("[]", {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      },
    );
    vi.stubGlobal("fetch", fetchMock);

    renderPage();

    expect(
      (await screen.findAllByText("NullPointerException")).length,
    ).toBeGreaterThan(0);
    expect(
      await screen.findByText(
        "A materialização falhou antes de criar o nicho enriquecido.",
      ),
    ).toBeTruthy();

    fireEvent.click(
      await screen.findByRole("button", { name: "Refazer pelo front-end" }),
    );

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining(
          "routine-research-orchestrator/recent-processed/11/reprocess",
        ),
        { method: "POST" },
      );
    });
    expect(
      await screen.findByText(
        "Novo ciclo de pesquisa de rotina criado imediatamente para refazer pelo front-end um CNAE aprovado cuja materialização falhou.",
      ),
    ).toBeTruthy();
  });

  it("mostra o ponto do problema quando a pesquisa precisa aprofundar evidências", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = input.toString();
      if (url.includes("routine-research-orchestrator/recent-processed")) {
        return new Response(
          JSON.stringify([
            {
              researchCycleId: 5,
              sourceNicheId: 1,
              cnaeCode: "9602501",
              cnaeDescription: "Cabeleireiros, manicure e pedicure",
              nicheName: "Cabeleireiros, manicure e pedicure",
              originalNicheName: "Cabeleireiros, manicure e pedicure",
              neutralNicheName: "Cabeleireiros, manicure e pedicure",
              researchMode: "ROUTINE_REALITY_RESEARCH",
              solutionLanguageRiskScore: 0,
              sourceScore: 90,
              triggerSource: "AUTO_SCORE_QUEUE",
              cycleStatus: "NEEDS_MORE_RESEARCH",
              processedAt: "2026-06-07T14:11:00Z",
              finishedAt: null,
              errorMessage: null,
            },
          ]),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }

      return new Response("[]", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });
    vi.stubGlobal("fetch", fetchMock);

    renderPage();

    expect(
      (await screen.findAllByText("Ponto do problema:")).length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText("Etapa 9 · Gate MEI/autônomo").length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText(
        "A pesquisa chegou ao gate, mas ainda não reuniu sinais humanos/comportamentais suficientes para liberar o público.",
      ).length,
    ).toBeGreaterThan(0);
  });

  it("mostra a etapa atual inferida para cada execução de CNAE", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = input.toString();
      if (url.includes("routine-research-orchestrator/recent-processed")) {
        return new Response(
          JSON.stringify([
            {
              researchCycleId: 9,
              sourceNicheId: 1,
              cnaeCode: "9602501",
              cnaeDescription: "Cabeleireiros, manicure e pedicure",
              nicheName: "Cabeleireiros, manicure e pedicure",
              originalNicheName:
                "IA para crescimento de Cabeleireiros, manicure e pedicure",
              neutralNicheName: "Cabeleireiros, manicure e pedicure",
              researchMode: "ROUTINE_REALITY_RESEARCH",
              solutionLanguageRiskScore: 100,
              sourceScore: 90,
              triggerSource: "MANUAL_REPROCESS",
              cycleStatus: "ROUTINE_SYNTHESIZED",
              processedAt: "2026-06-08T18:09:23Z",
              finishedAt: null,
              errorMessage: null,
            },
          ]),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }

      return new Response("[]", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });
    vi.stubGlobal("fetch", fetchMock);

    renderPage();

    await waitFor(() => {
      expect(screen.getAllByText("Rotina sintetizada").length).toBeGreaterThan(
        0,
      );
    });
    expect(
      screen.getByText("Etapa 7 · Aguardando perfil MEI/autônomo"),
    ).toBeTruthy();
    expect(
      screen.getByText(
        "A rotina já foi sintetizada; falta materializar o público dono-operador MEI/autônomo antes do gate.",
      ),
    ).toBeTruthy();
  });

  it("mostra seed e queries geradas pela IA no card da etapa 2", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = input.toString();
      if (url.includes("routine-research-orchestrator/recent-processed")) {
        return new Response(
          JSON.stringify([
            {
              researchCycleId: 1,
              sourceNicheId: 1,
              cnaeCode: "9602501",
              cnaeDescription: "Cabeleireiros, manicure e pedicure",
              nicheName:
                "IA para crescimento de Cabeleireiros, manicure e pedicure",
              originalNicheName:
                "IA para crescimento de Cabeleireiros, manicure e pedicure",
              neutralNicheName: "Cabeleireiros, manicure e pedicure",
              researchMode: "ROUTINE_REALITY_RESEARCH",
              solutionLanguageRiskScore: 65,
              sourceScore: 90,
              triggerSource: "AUTO_SCORE_QUEUE",
              cycleStatus: "RUNNING",
              processedAt: "2026-06-03T06:59:59Z",
              finishedAt: null,
              errorMessage: null,
            },
          ]),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }

      if (
        url.includes("niche-research-seed-builder/stage-executions/pending")
      ) {
        return new Response(JSON.stringify([]), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }

      if (url.includes("niche-research-seed-builder/stage-executions/1")) {
        return new Response(
          JSON.stringify({
            researchCycleId: 1,
            cycleStatus: "RUNNING",
            cycleTotalQueries: 15,
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
              totalQueries: 15,
              queries: [
                {
                  queryId: 1,
                  researchCycleId: 1,
                  nicheResearchSeedId: 1,
                  queryText:
                    "Quais são as etapas diárias na rotina de trabalho?",
                  queryGoal: "MEI_ROUTINE_DISCOVERY",
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
        );
      }

      return new Response("[]", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });
    vi.stubGlobal("fetch", fetchMock);

    renderPage();

    await waitFor(() => {
      expect(
        screen.getAllByText("Dados gerados pela IA").length,
      ).toBeGreaterThan(0);
    });
    expect(await screen.findByText("Serviços pessoais de beleza")).toBeTruthy();
    expect(
      screen.getByText("Profissionais enfrentam desafios na gestão de agenda."),
    ).toBeTruthy();
    expect(screen.getByText("MEI_ROUTINE_DISCOVERY: 1")).toBeTruthy();
    expect(
      screen.getByText(
        "1 queries geradas para as próximas etapas. As primeiras consultas mostram se a pesquisa está olhando para a pessoa MEI/autônoma.",
      ),
    ).toBeTruthy();
    expect(
      screen
        .getByRole("link", { name: "Ver detalhe da IA" })
        .getAttribute("href"),
    ).toBe("/oprm/pipeline/niche-research-seed-builder/1");
    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining(
          "niche-research-seed-builder/stage-executions/1",
        ),
      );
    });
  });
});
