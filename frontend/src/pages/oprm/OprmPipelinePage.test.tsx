import { render, screen, waitFor } from "@testing-library/react";
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
    expect(screen.getAllByText("RUNNING").length).toBeGreaterThan(0);

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
        );
      }

      return new Response("[]", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });
    vi.stubGlobal("fetch", fetchMock);

    renderPage();

    expect(await screen.findByText("Dados gerados pela IA")).toBeTruthy();
    expect(await screen.findByText("Serviços pessoais de beleza")).toBeTruthy();
    expect(
      screen.getByText("Profissionais enfrentam desafios na gestão de agenda."),
    ).toBeTruthy();
    expect(
      screen.getByText(
        "1 queries geradas para as próximas etapas. Abra o detalhe para ver a requisição enviada à IA e o JSON completo gerado.",
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
