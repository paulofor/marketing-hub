import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen } from "@testing-library/react";
import axios from "axios";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import BusinessProcessActivityDocumentsPage from "./BusinessProcessActivityDocumentsPage";

vi.mock("axios");

describe("BusinessProcessActivityDocumentsPage", () => {
  beforeEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("exibe os dez documentos recentes com origem, conteúdo, tokens e custo", async () => {
    const process = {
      id: 22,
      processCode: "pde-opportunity-discovery",
      name: "Descoberta da oportunidade PDE",
      purpose: "Comprovar uma dor.",
      ownerName: "Inteligência",
      triggerDescription: "Sinal",
      outcomeDescription: "Dossiê",
      versionNumber: 1,
      status: "PUBLISHED",
      createdAt: "2026-08-20T10:00:00Z",
      diagram: {
        nodes: [
          { id: "start", type: "START", label: "Início" },
          {
            id: "evidence",
            type: "TASK",
            label: "Comprovar dor e demanda",
            documentOutput: { label: "dossiês de evidências" },
          },
          { id: "end", type: "END", label: "Fim" },
        ],
        flows: [],
      },
    };
    const documents = Array.from({ length: 10 }, (_, index) => ({
      taskId: 200 - index,
      title: `Dossiê ${10 - index}`,
      sourceReference: `opportunity:${10 - index}`,
      assignedAgentKey: "market-radar",
      assignedAgentNickname: "Argos",
      resultJson: JSON.stringify({ decision: "APPROVE", index }),
      evidenceJson: JSON.stringify({ sources: 2 }),
      inputTokens: 1000,
      cachedInputTokens: 200,
      outputTokens: 500,
      estimatedCostUsd: 0.12345678,
      costEstimationStatus: "ESTIMATED",
      generatedAt: "2026-08-20T21:41:24Z",
    }));
    vi.mocked(axios.get).mockImplementation(
      async (url) =>
        ({
          data: String(url).endsWith("/documents") ? documents : [process],
        }) as never,
    );
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <MemoryRouter
        initialEntries={[
          "/business-processes/22/activities/evidence/documents",
        ]}
      >
        <QueryClientProvider client={client}>
          <Routes>
            <Route
              path="/business-processes/:processDefinitionId/activities/:activityId/documents"
              element={<BusinessProcessActivityDocumentsPage />}
            />
          </Routes>
        </QueryClientProvider>
      </MemoryRouter>,
    );

    expect(await screen.findByText("Dossiê 10")).toBeInTheDocument();
    expect(screen.getAllByText(/Tarefa #/)).toHaveLength(10);
    expect(screen.getByText("opportunity:10")).toBeInTheDocument();
    expect(
      screen.getAllByText(/entrada 1000 · cache 200 · saída 500/),
    ).toHaveLength(10);
    expect(screen.getAllByText("US$ 0.12345678")).toHaveLength(10);
    expect(screen.getAllByText(/"decision": "APPROVE"/)).toHaveLength(10);
    expect(screen.getByRole("link", { name: "Voltar ao BPM" })).toHaveAttribute(
      "href",
      "/business-processes?processId=22",
    );
  });

  it("explica quando a atividade ainda não possui documento concluído", async () => {
    vi.mocked(axios.get).mockImplementation(async (url) => {
      if (String(url).endsWith("/documents")) return { data: [] } as never;
      return { data: [] } as never;
    });
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <MemoryRouter
        initialEntries={[
          "/business-processes/22/activities/evidence/documents",
        ]}
      >
        <QueryClientProvider client={client}>
          <Routes>
            <Route
              path="/business-processes/:processDefinitionId/activities/:activityId/documents"
              element={<BusinessProcessActivityDocumentsPage />}
            />
          </Routes>
        </QueryClientProvider>
      </MemoryRouter>,
    );

    expect(
      await screen.findByText(
        "Esta atividade ainda não possui documento concluído.",
      ),
    ).toBeInTheDocument();
  });

  it("consulta os dez documentos do processo ao abrir o link do objetivo principal", async () => {
    vi.mocked(axios.get).mockImplementation(async (url) => {
      if (url === "/api/business-processes/22/documents") {
        return {
          data: [
            {
              taskId: 126,
              title: "Dossiê de oportunidade",
              assignedAgentKey: "market-radar",
              assignedAgentNickname: "Argos",
              resultJson: "{\"decision\":\"APPROVE\"}",
              costEstimationStatus: "ESTIMATED",
              generatedAt: "2026-08-20T21:41:24Z",
            },
          ],
        } as never;
      }
      return { data: [] } as never;
    });
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <MemoryRouter initialEntries={["/business-processes/22/documents"]}>
        <QueryClientProvider client={client}>
          <Routes>
            <Route
              path="/business-processes/:processDefinitionId/documents"
              element={<BusinessProcessActivityDocumentsPage />}
            />
          </Routes>
        </QueryClientProvider>
      </MemoryRouter>,
    );

    expect(await screen.findByText("Dossiê de oportunidade")).toBeInTheDocument();
    expect(axios.get).toHaveBeenCalledWith(
      "/api/business-processes/22/documents",
    );
    expect(
      screen.getByText(/Todos os objetivos documentais/),
    ).toBeInTheDocument();
  });
});
