import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen } from "@testing-library/react";
import axios from "axios";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import BusinessProcessActivityExecutionsPage from "./BusinessProcessActivityExecutionsPage";

vi.mock("axios");

describe("BusinessProcessActivityExecutionsPage", () => {
  beforeEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("exibe dez tarefas do Argos com prompt, comentários, custo e JSON em árvore", async () => {
    const executions = Array.from({ length: 10 }, (_, index) => ({
      taskId: 126 - index,
      processDefinitionId: 22,
      processVersionNumber: 1,
      title: `Comprovar dor e demanda · rodada ${10 - index}`,
      status: "COMPLETED",
      sourceReference: `pde-opportunity:round-${10 - index}`,
      assignedAgentKey: "market-radar",
      assignedAgentNickname: "Argos",
      comments: JSON.stringify({ decision: "APPROVE", index }),
      evidenceJson: JSON.stringify({ sources: 2 }),
      inputTokens: 2834,
      cachedInputTokens: 2304,
      outputTokens: 5861,
      estimatedCostUsd: 0.0134724,
      costEstimationStatus: "ESTIMATED",
      createdAt: "2026-08-20T21:40:00Z",
      startedAt: "2026-08-20T21:40:00Z",
      finishedAt: "2026-08-20T21:41:24Z",
      modelCode: "gpt-5.4-mini-2026-03-17",
      reasoningEffort: "high",
      productInternalName: "VEGA-01",
      promptSent: JSON.stringify({ instruction: "Comprove a dor" }),
    }));
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        selectedProcessDefinitionId: 37,
        processCode: "pde-opportunity-discovery",
        processName: "Descoberta e priorização da oportunidade PDE",
        selectedProcessVersionNumber: 4,
        selectedProcessStatus: "RETIRED",
        activityId: "evidence",
        activityName: "Comprovar dor e demanda",
        activityOwnerName: "Argos",
        executions,
      },
    });
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <MemoryRouter
        initialEntries={[
          "/business-processes/37/activities/evidence/executions",
        ]}
      >
        <QueryClientProvider client={client}>
          <Routes>
            <Route
              path="/business-processes/:processDefinitionId/activities/:activityId/executions"
              element={<BusinessProcessActivityExecutionsPage />}
            />
          </Routes>
        </QueryClientProvider>
      </MemoryRouter>,
    );

    expect(
      await screen.findByRole("heading", {
        name: "Descoberta e priorização da oportunidade PDE · Comprovar dor e demanda",
      }),
    ).toBeInTheDocument();
    expect(screen.getByText(/responsável: Argos/)).toBeInTheDocument();
    expect(screen.getAllByText(/Tarefa #/)).toHaveLength(10);
    expect(screen.getAllByText("gpt-5.4-mini-2026-03-17")).toHaveLength(10);
    expect(screen.getAllByText("high")).toHaveLength(10);
    expect(screen.getAllByText("VEGA-01")).toHaveLength(10);
    expect(screen.getAllByText("US$ 0.01347240")).toHaveLength(10);
    expect(screen.getAllByText("1min 24s")).toHaveLength(10);
    expect(screen.getAllByText("Prompt recebido por Argos")).toHaveLength(10);
    expect(screen.getAllByText("Comentários de Argos")).toHaveLength(10);
    expect(screen.getAllByText("Visualizar JSON em árvore")).toHaveLength(30);
    expect(screen.getByRole("link", { name: "Voltar ao BPM" })).toHaveAttribute(
      "href",
      "/business-processes/retired?processId=37",
    );
    expect(axios.get).toHaveBeenCalledWith(
      "/api/business-processes/37/activities/evidence/executions",
    );
  });

  it("identifica prompt legado ausente sem fabricar seu conteúdo", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        selectedProcessDefinitionId: 37,
        processCode: "pde-opportunity-discovery",
        processName: "Descoberta e priorização da oportunidade PDE",
        selectedProcessVersionNumber: 4,
        selectedProcessStatus: "RETIRED",
        activityId: "evidence",
        activityName: "Comprovar dor e demanda",
        activityOwnerName: "Argos",
        executions: [
          {
            taskId: 117,
            processDefinitionId: 22,
            processVersionNumber: 1,
            title: "Comprovar dor e demanda",
            status: "COMPLETED",
            assignedAgentKey: "market-radar",
            assignedAgentNickname: "Argos",
            comments: "Pesquisa concluída.",
            costEstimationStatus: "NOT_REPORTED",
            createdAt: "2026-08-20T21:25:11Z",
          },
        ],
      },
    });
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <MemoryRouter
        initialEntries={[
          "/business-processes/37/activities/evidence/executions",
        ]}
      >
        <QueryClientProvider client={client}>
          <Routes>
            <Route
              path="/business-processes/:processDefinitionId/activities/:activityId/executions"
              element={<BusinessProcessActivityExecutionsPage />}
            />
          </Routes>
        </QueryClientProvider>
      </MemoryRouter>,
    );

    expect(
      await screen.findByText("Prompt não registrado nesta execução legada."),
    ).toBeInTheDocument();
    expect(screen.getAllByText("Não informado")).toHaveLength(3);
    expect(screen.getByText("Pesquisa concluída.")).toBeInTheDocument();
  });
});
