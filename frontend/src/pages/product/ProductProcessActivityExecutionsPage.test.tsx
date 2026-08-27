import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen } from "@testing-library/react";
import axios from "axios";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ProductProcessActivityExecutionsPage from "./ProductProcessActivityExecutionsPage";

vi.mock("axios");

const dedaloTask = {
  taskId: 243,
  processDefinitionId: 18,
  processVersionNumber: 4,
  title: "Experimento #89 · construir e homologar a landing",
  status: "COMPLETED",
  sourceReference: "commercial-plan:4@v3:journey",
  assignedAgentKey: "landing-generator",
  assignedAgentNickname: "Dédalo",
  comments: JSON.stringify({ summary: "Landing construída" }),
  evidenceJson: JSON.stringify({ approvalRecommendation: "APPROVE" }),
  inputTokens: 947056,
  cachedInputTokens: 796288,
  outputTokens: 25323,
  estimatedCostUsd: 1.4280472,
  costEstimationStatus: "ESTIMATED",
  createdAt: "2026-08-27T03:26:19Z",
  startedAt: "2026-08-27T03:26:45Z",
  finishedAt: "2026-08-27T03:35:14Z",
  modelCode: "gpt-5.6-sol",
  reasoningEffort: "high",
  productInternalName: "Rigel",
  promptSent: "Construa a landing com ativos aprovados.",
};

const psiqueTask = {
  ...dedaloTask,
  taskId: 244,
  title: "Avaliar percepção da cliente",
  status: "BLOCKED",
  assignedAgentKey: "customer-agent",
  assignedAgentNickname: "Psique",
  comments: "Checkout ausente na evidência.",
  estimatedCostUsd: 0.1895768,
  reasoningEffort: undefined,
};

const history = {
  productId: 9,
  productName: "Kit WhatsApp Pronto",
  productInternalName: "Rigel",
  selectedProcessDefinitionId: 18,
  processCode: "landing-page-generation",
  processName: "Geração de landing page",
  selectedProcessVersionNumber: 4,
  selectedProcessStatus: "PUBLISHED",
  activityCount: 4,
  activitiesWithTasksCount: 3,
  uniqueTaskCount: 2,
  knownEstimatedCostUsd: 1.617624,
  costCoverage: "PARTIAL",
  activities: [
    {
      activityDefinitionId: 119,
      activityId: "select",
      activityName: "Selecionar provas reais da entrega",
      activityObjective: "Selecionar ativos aprovados e rastreáveis.",
      activityOwnerName: "Dédalo",
      sequenceNumber: 1,
      selectedVersionActivity: true,
      taskCount: 1,
      tasks: [dedaloTask],
    },
    {
      activityDefinitionId: 122,
      activityId: "html",
      activityName: "Construir HTML completo com ativos aprovados",
      activityObjective: "Entregar documento responsivo e instrumentado.",
      activityOwnerName: "Dédalo",
      sequenceNumber: 2,
      selectedVersionActivity: true,
      taskCount: 1,
      tasks: [dedaloTask],
    },
    {
      activityDefinitionId: 124,
      activityId: "customer",
      activityName: "Avaliar percepção da cliente",
      activityObjective: "Validar clareza, desejo e confiança.",
      activityOwnerName: "Psique",
      sequenceNumber: 3,
      selectedVersionActivity: true,
      taskCount: 1,
      tasks: [psiqueTask],
    },
    {
      activityDefinitionId: 126,
      activityId: "human",
      activityName: "Aprovação humana para publicar",
      activityObjective: "Autorizar publicação explícita.",
      activityOwnerName: "Operador humano",
      sequenceNumber: 4,
      selectedVersionActivity: true,
      taskCount: 0,
      tasks: [],
    },
  ],
};

function renderPage(
  initialEntry = "/products/9/value-chain-history/processes/18/activities",
) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <QueryClientProvider client={client}>
        <Routes>
          <Route
            path="/products/:productId/value-chain-history/processes/:processDefinitionId/activities"
            element={<ProductProcessActivityExecutionsPage />}
          />
        </Routes>
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

describe("ProductProcessActivityExecutionsPage", () => {
  beforeEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("shows product activities and tasks without duplicating the summary", async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: history });

    renderPage();

    expect(
      await screen.findByRole("heading", {
        name: "Rigel · Geração de landing page",
      }),
    ).toBeInTheDocument();
    expect(screen.getByText("3 com tarefas reais")).toBeInTheDocument();
    expect(
      screen.getByText("Execuções sem duplicar tarefas compostas"),
    ).toBeInTheDocument();
    expect(screen.getByText("Cobertura parcial")).toBeInTheDocument();
    expect(
      screen.getByRole("heading", {
        name: "Selecionar provas reais da entrega",
      }),
    ).toBeInTheDocument();
    expect(screen.getAllByText(/Tarefa #243/)).toHaveLength(2);
    expect(screen.getAllByText("gpt-5.6-sol")).toHaveLength(3);
    expect(screen.getAllByText("Rigel")).toHaveLength(3);
    expect(
      screen.getByText("Nenhuma tarefa registrada para este produto."),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Voltar ao histórico" }),
    ).toHaveAttribute("href", "/products/9/value-chain-history");
    expect(screen.getByRole("link", { name: "Abrir BPM" })).toHaveAttribute(
      "href",
      "/business-processes?processId=18",
    );
    expect(axios.get).toHaveBeenCalledWith(
      "/api/business-processes/18/products/9/activity-executions",
    );
  });

  it("makes a backend failure explicit", async () => {
    vi.mocked(axios.get).mockRejectedValue(new Error("backend unavailable"));

    renderPage();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Não foi possível consultar as atividades e tarefas deste produto no processo.",
    );
    expect(
      screen.queryByRole("region", { name: "Atividades e tarefas do produto" }),
    ).not.toBeInTheDocument();
  });

  it("rejects invalid product or process identifiers before querying", () => {
    renderPage(
      "/products/not-a-product/value-chain-history/processes/18/activities",
    );

    expect(screen.getByRole("alert")).toHaveTextContent(
      "Produto ou processo inválido.",
    );
    expect(axios.get).not.toHaveBeenCalled();
  });
});
