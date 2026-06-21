import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import OprmNichoCnaeV2JobDetailPage from "./OprmNichoCnaeV2JobDetailPage";

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
        initialEntries={["/oprm/cnaes/4781400/pipeline-v2/jobs/job-9"]}
      >
        <Routes>
          <Route
            path="/oprm/cnaes/:cnaeCode/pipeline-v2/jobs/:jobId"
            element={<OprmNichoCnaeV2JobDetailPage />}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("OprmNichoCnaeV2JobDetailPage", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("exibe o conteúdo completo dos payloads JSON sob demanda", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(
        async () =>
          new Response(
            JSON.stringify({
              jobId: "job-9",
              cnaeCode: "4781400",
              status: "FAILED",
              finalDecision: "NO_VIABLE_SUBNICHE",
              finalDecisionLabel: "Encerrado sem subnicho viável",
              finalDecisionReason: "Sem finalistas viáveis.",
              outcomeStatus: "FAILURE",
              outcomeMessage: "O torneio terminou sem finalistas viáveis.",
              stages: [
                {
                  stageExecutionId: "150",
                  stageCode: "candidate-generator",
                  status: "COMPLETED",
                  failureType: null,
                  attemptNumber: 1,
                  technicalRetryNumber: null,
                  knowledgeVersion: null,
                  materializationEnabled: null,
                  inputPayload: JSON.stringify({ cnaeCode: "4781400" }),
                  outputPayload: JSON.stringify({
                    candidates: [{ name: "ajustes de roupa plus size" }],
                    candidateCount: 1,
                  }),
                  errorMessage: null,
                  nextStageCode: "source-safety-filter",
                  createdAt: "2026-06-21T17:50:00Z",
                  updatedAt: "2026-06-21T17:50:00Z",
                },
              ],
            }),
            { status: 200, headers: { "Content-Type": "application/json" } },
          ),
      ),
    );

    renderPage();

    expect(
      await screen.findByText(/JSON registrado com campos: cnaeCode/),
    ).toBeInTheDocument();
    await userEvent.click(
      screen.getByText(/campos: candidates, candidateCount/),
    );

    expect(
      screen.getByText(/"name": "ajustes de roupa plus size"/),
    ).toBeInTheDocument();
    expect(screen.getByText(/"candidateCount": 1/)).toBeInTheDocument();
  });
});
