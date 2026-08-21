import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import AgentWorkspacePage from "./AgentWorkspacePage";

const testState = vi.hoisted(() => ({
  agentKey: "landing-generator",
  nickname: "Dédalo",
  agentName: "Agente Gerador de Landing",
  taskStatus: "PENDING",
  sourceReference: undefined as string | undefined,
  experiments: [] as Array<Record<string, unknown>>,
  taskCount: 1,
  exceptional: false,
  exceptionReason: undefined as string | undefined,
  createMutate: vi.fn(),
  processes: [] as Array<Record<string, unknown>>,
}));

vi.mock("../../api/agent/useAgents", () => ({
  useAgents: () => ({
    isLoading: false,
    data: [
      {
        id: 7,
        agentKey: testState.agentKey,
        nickname: testState.nickname,
        name: testState.agentName,
        status: "TEST",
      },
    ],
  }),
}));

vi.mock("../../api/agentTask/useAgentTasks", () => ({
  useAgentTasks: () => ({
    isLoading: false,
    data: Array.from({ length: testState.taskCount }, (_, index) => ({
      id: 41 - index,
      assignedAgentId: 7,
      assignedAgentKey: testState.agentKey,
      assignedAgentNickname: testState.nickname,
      requestedByType: "AGENT",
      requestedByName: "Têmis",
      title: index === 0 ? "Aprimorar hero" : `Tarefa histórica ${index}`,
      description: "Melhorar a clareza da promessa no celular.",
      priority: "HIGH",
      status: testState.taskStatus,
      sourceReference: testState.sourceReference,
      exceptional: testState.exceptional,
      exceptionReason: testState.exceptionReason,
      inputTokens: 1500,
      cachedInputTokens: 500,
      outputTokens: 400,
      estimatedCostUsd: 0.025,
      costEstimationStatus: "ESTIMATED",
      receivedAt: "2026-08-11T15:00:00Z",
      createdAt: "2026-08-11T15:00:00Z",
      updatedAt: "2026-08-11T15:00:00Z",
    })),
  }),
  useCreateAgentTask: () => ({
    mutate: testState.createMutate,
    isPending: false,
  }),
  useUpdateAgentTaskStatus: () => ({ mutate: vi.fn(), isPending: false }),
}));

vi.mock("../../api/agent/useAgentWorkMonitor", () => ({
  useAgentWorkMonitor: () => ({
    isLoading: false,
    data: [
      {
        agentId: 7,
        agentKey: testState.agentKey,
        nickname: testState.nickname,
        agentName: "Agente em teste",
        workStatus: "WORKING",
        currentWork: "Construção da landing do experimento #88",
        progressDetail: "Etapa landing-generation-agent-v1 · PROCESSANDO",
        difficulty: null,
        externalDecisionRequired: false,
        lastActivityAt: "2026-08-15T13:01:00Z",
        dailyTokens: 12345,
        dailyTokenDate: "2026-08-15",
        executionActivity: {
          status: "RUNNING",
          processAlive: true,
          eventCount: 18,
          outputBytes: 4096,
          inputTokens: 1200,
          outputTokens: 350,
          lastEventType: "OUTPUT",
          startedAt: "2026-08-15T13:27:56Z",
          lastHeartbeatAt: "2026-08-15T13:28:30Z",
          finishedAt: null,
          stale: false,
        },
        executorHealth: { status: "READY" },
        combinedStatus: "ATUANDO",
      },
    ],
  }),
}));

vi.mock("../../api/planning/useCommercialPlans", () => ({
  useCommercialPlans: () => ({
    data: [{ id: 9, name: "MUSA v7" }],
    isLoading: false,
  }),
  useCommercialPlanVersions: () => ({
    data: [{ versionNumber: 3 }],
    isLoading: false,
  }),
}));

vi.mock("../../api/experiment/useExperiments", () => ({
  useExperiments: () => ({ data: testState.experiments, isLoading: false }),
}));

vi.mock("../../api/businessProcess/useBusinessProcesses", () => ({
  useBusinessProcesses: () => ({
    data: testState.processes,
    isLoading: false,
  }),
}));

describe("AgentWorkspacePage", () => {
  beforeEach(() => {
    cleanup();
    testState.agentKey = "landing-generator";
    testState.nickname = "Dédalo";
    testState.agentName = "Agente Gerador de Landing";
    testState.taskStatus = "PENDING";
    testState.sourceReference = undefined;
    testState.experiments = [];
    testState.taskCount = 1;
    testState.exceptional = false;
    testState.exceptionReason = undefined;
    testState.createMutate.mockReset();
    testState.processes = [];
  });

  it("quebra justificativa excepcional longa sem ampliar o card", () => {
    testState.exceptional = true;
    testState.exceptionReason =
      "Execução histórica preservada com uma justificativa operacional extensa que precisa quebrar dentro da coluna da tarefa.";

    render(
      <QueryClientProvider client={new QueryClient()}>
        <MemoryRouter initialEntries={["/agents/7"]}>
          <Routes>
            <Route path="/agents/:id" element={<AgentWorkspacePage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByText(/Execução histórica preservada/)).toHaveClass(
      "agent-task-card__exception",
    );
  });

  it("destaca somente as cinco tarefas mais recentes do agente", () => {
    testState.taskCount = 7;

    render(
      <QueryClientProvider client={new QueryClient()}>
        <MemoryRouter initialEntries={["/agents/7"]}>
          <Routes>
            <Route path="/agents/:id" element={<AgentWorkspacePage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByText("Últimas tarefas")).toBeInTheDocument();
    expect(screen.getByText("5 exibidas")).toBeInTheDocument();
    expect(
      screen.getByText("Tarefa #41", { exact: false }),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Tarefa #37", { exact: false }),
    ).toBeInTheDocument();
    expect(
      screen.queryByText("Tarefa #36", { exact: false }),
    ).not.toBeInTheDocument();
  });

  it("mostra sinais persistidos de atuação sem estimar consumo ausente", () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <MemoryRouter initialEntries={["/agents/7"]}>
          <Routes>
            <Route path="/agents/:id" element={<AgentWorkspacePage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByText("Atuação observável")).toBeInTheDocument();
    expect(screen.getByText("12.345")).toBeInTheDocument();
    expect(screen.getByText(/confirmam execução técnica/)).toBeInTheDocument();
    expect(screen.getByText("Execução atual")).toBeInTheDocument();
    expect(screen.getByText("Ativo agora")).toBeInTheDocument();
    expect(screen.getByText("18 eventos")).toBeInTheDocument();
    expect(screen.getByText("4.096 bytes produzidos")).toBeInTheDocument();
    expect(
      screen.getByText("1.550 tokens na execução atual"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Sinal dentro da janela esperada"),
    ).toBeInTheDocument();
  });

  it("exibe a identidade, a caixa de entrada e a autoria de outro agente", () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <MemoryRouter initialEntries={["/agents/7"]}>
          <Routes>
            <Route path="/agents/:id" element={<AgentWorkspacePage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByText("Mesa de Dédalo")).toBeInTheDocument();
    expect(screen.getAllByText("Aprimorar hero")).toHaveLength(2);
    expect(screen.getByText(/Solicitado por Têmis/)).toBeInTheDocument();
    expect(screen.getByLabelText("Plano comercial *")).toHaveValue("9");
    expect(screen.getByText(/contexto v3/)).toBeInTheDocument();
    expect(screen.getByText(/Recebida em:/)).toBeInTheDocument();
    expect(screen.getByText(/Ainda não entregue/)).toBeInTheDocument();
    expect(
      screen.getAllByText("Tokens: entrada 1.500 · saída 400 · cache 500"),
    ).not.toHaveLength(0);
    expect(
      screen.getByRole("button", { name: "Em andamento" }),
    ).toBeInTheDocument();
  });

  it("registra uma nova tarefa no contexto auditável do experimento", () => {
    testState.agentKey = "growth-operator";
    testState.nickname = "Hermes";
    testState.agentName = "Operador de Crescimento";
    testState.experiments = [{ id: "88", name: "Agenda Cheia Nail Design" }];
    testState.processes = [
      {
        id: 20,
        processCode: "operacao-otimizacao-experimento",
        name: "Operação e otimização de experimento",
        versionNumber: 3,
        status: "PUBLISHED",
        diagram: {
          nodes: [
            {
              id: "confirmar-instrumentacao",
              type: "TASK",
              label: "Confirmar instrumentação",
              owner: "Operador de Crescimento",
            },
          ],
          flows: [],
        },
      },
    ];

    render(
      <QueryClientProvider client={new QueryClient()}>
        <MemoryRouter initialEntries={["/agents/7"]}>
          <Routes>
            <Route path="/agents/:id" element={<AgentWorkspacePage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.change(screen.getByLabelText("Contexto da execução *"), {
      target: { value: "EXPERIMENT" },
    });
    fireEvent.change(screen.getByLabelText("Experimento *"), {
      target: { value: "88" },
    });
    fireEvent.change(screen.getByLabelText("Processo publicado *"), {
      target: { value: "20" },
    });
    fireEvent.change(screen.getByLabelText("Atividade do processo *"), {
      target: { value: "confirmar-instrumentacao" },
    });
    fireEvent.change(screen.getByLabelText("Tarefa *"), {
      target: { value: "Reconciliar eventos do experimento #88" },
    });
    fireEvent.change(screen.getByLabelText("Resultado esperado *"), {
      target: {
        value: "Métricas comerciais contêm somente visitas humanas válidas.",
      },
    });
    fireEvent.click(screen.getByRole("button", { name: "Enviar para Hermes" }));

    expect(testState.createMutate).toHaveBeenCalledWith(
      expect.objectContaining({
        sourceReference: "experiment:88",
        processDefinitionId: 20,
        processActivityId: "confirmar-instrumentacao",
      }),
      expect.any(Object),
    );
  });

  it("mostra motivo e contagem Meta antes de retomar Têmis", () => {
    testState.agentKey = "meta-ad-approver";
    testState.nickname = "Têmis";
    testState.taskStatus = "BLOCKED";
    testState.sourceReference = "experiment:88";
    testState.experiments = [
      {
        id: "88",
        creativeGenerationError:
          "Copy Meta inválida: primaryText excede 125 caracteres (atual: 148); reescrita obrigatória",
        creativeMetaCopyViolations: [
          { field: "primaryText", actualLength: 148, maxLength: 125 },
        ],
      },
    ];

    render(
      <QueryClientProvider client={new QueryClient()}>
        <MemoryRouter initialEntries={["/agents/7"]}>
          <Routes>
            <Route path="/agents/:id" element={<AgentWorkspacePage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(
      screen.getByText("Corrija antes de reenfileirar Têmis"),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Texto principal: 148\/125 caracteres/),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/primaryText excede 125 caracteres/),
    ).toBeInTheDocument();
  });
});
