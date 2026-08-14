import { cleanup, render, screen, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import userEvent from "@testing-library/user-event";
import AgentListPage from "./AgentListPage";

vi.mock("../../api/agent/useAgents", () => ({
  useAgents: () => ({
    isLoading: false,
    data: [
      {
        id: 7,
        name: "Agente Gerador de Landing",
        nickname: "Dédalo",
        status: "TEST",
        currentVersion: 1,
        executionMode: "EVENT_DRIVEN",
        themeId: 1,
        inputs: [],
        outputs: [],
        internalFunctions: [],
      },
    ],
  }),
}));

vi.mock("../../api/agent/useAgentMaturity", () => ({
  useAgentMaturity: () => ({
    data: [
      {
        agentId: 7,
        agentName: "Agente Gerador de Landing",
        agentKey: "landing-generator",
        executions: 4,
        completionRate: 75,
        openTasks: 1,
        resolvedTasks: 3,
        confirmedResults: 1,
        maturityLevel: "EM_VALIDACAO",
        nextMaturityAction: "Concluir pendências.",
      },
    ],
  }),
}));

vi.mock("../../api/agent/useAgentWorkMonitor", () => ({
  useAgentWorkMonitor: () => ({
    data: [
      {
        agentId: 2,
        nickname: "Apolo",
        agentName: "Agente Videomaker",
        agentKey: "video-maker",
        workStatus: "WORKING",
        currentWork: "Produção audiovisual do ciclo #5",
        progressDetail: "Execução em modo sombra",
        externalDecisionRequired: false,
        dailyTokens: 0,
        executorHealth: {
          status: "BLOCKED",
          expectedVersion: 2,
          deployedVersion: 2,
          versionCurrent: true,
          backendAccessible: true,
          codexAuthenticated: false,
        },
      },
      {
        agentId: 10,
        nickname: "Atena",
        agentName: "Estrategista de experimentos",
        agentKey: "experiment-strategist",
        workStatus: "BLOCKED",
        currentWork: "Parecer de Atena no dossiê #6",
        progressDetail: "Execução canônica FAILED",
        difficulty: "Falha HTTP 500 ao consultar pendências.",
        sourceReference: "opportunity-dossier:6",
        executionId: 7,
        externalDecisionRequired: false,
        dailyTokens: 0,
        executorHealth: {
          status: "READY",
          expectedVersion: 4,
          deployedVersion: 4,
          versionCurrent: true,
          backendAccessible: true,
          codexAuthenticated: true,
          detail: "Executor pronto.",
        },
        combinedStatus: "READY — parecer bloqueado",
      },
      {
        agentId: 7,
        nickname: "Dédalo",
        agentName: "Agente Gerador de Landing",
        agentKey: "landing-generator",
        workStatus: "WORKING",
        currentWork: "Correção autônoma da landing do experimento #88",
        progressDetail: "Etapa em processamento",
        taskId: 14,
        executionId: 326,
        externalDecisionRequired: false,
        lastActivityAt: "2026-08-11T04:00:00Z",
        dailyTokens: 12345,
        dailyTokenDate: "2026-08-11",
        executorHealth: {
          status: "READY",
          expectedVersion: 1,
          deployedVersion: 1,
          versionCurrent: true,
          backendAccessible: true,
          codexAuthenticated: true,
          detail: "Executor pronto.",
        },
      },
      {
        agentId: 3,
        nickname: "Plutus",
        agentName: "Agente Financeiro",
        agentKey: "financial-agent",
        workStatus: "BLOCKED",
        currentWork: "Avaliar ciclo MUSA #5",
        progressDetail: "Aguardando autenticação",
        externalDecisionRequired: false,
        dailyTokens: 0,
        executorHealth: {
          status: "BLOCKED",
          expectedVersion: 3,
          deployedVersion: 3,
          versionCurrent: true,
          backendAccessible: true,
          codexAuthenticated: false,
        },
      },
    ],
  }),
}));

vi.mock("../../api/agent/useAgentExecutorOperation", () => ({
  useAgentExecutorOperation: () => ({ data: null }),
  useStartAgentExecutorOperation: () => ({
    isPending: false,
    isError: false,
    mutate: vi.fn(),
  }),
}));

describe("AgentListPage", () => {
  afterEach(() => cleanup());

  it("exibe o apelido e preserva o nome formal no quadro de maturidade", () => {
    render(
      <MemoryRouter>
        <AgentListPage />
      </MemoryRouter>,
    );

    const maturitySection = screen
      .getByRole("heading", { name: "Maturidade e fechamento de ciclos" })
      .closest("section");

    expect(maturitySection).not.toBeNull();
    expect(within(maturitySection!).getByText("Dédalo")).toBeInTheDocument();
    expect(
      within(maturitySection!).getByText("Agente Gerador de Landing"),
    ).toBeInTheDocument();
  });

  it("distingue o health pronto do parecer canônico bloqueado de Atena", () => {
    render(
      <MemoryRouter>
        <AgentListPage />
      </MemoryRouter>,
    );

    expect(screen.getByText("READY — parecer bloqueado")).toBeInTheDocument();
    expect(
      screen.getByText("Parecer de Atena no dossiê #6"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Falha HTTP 500 ao consultar pendências."),
    ).toBeInTheDocument();
    expect(screen.getByText("Execução #7")).toBeInTheDocument();
  });

  it("mostra trabalho real e necessidade de decisão no monitor", () => {
    render(
      <MemoryRouter>
        <AgentListPage />
      </MemoryRouter>,
    );
    expect(
      screen.getByRole("heading", { name: "Monitor de trabalho dos agentes" }),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Correção autônoma da landing do experimento #88"),
    ).toBeInTheDocument();
    expect(screen.getAllByText("WORKING")).toHaveLength(2);
    expect(screen.getByText("Tokens hoje")).toBeInTheDocument();
    expect(screen.getByText("12.345")).toBeInTheDocument();
    expect(screen.getByText("Tarefa #14 · Execução #326")).toBeInTheDocument();
  });

  it("oferece reconexão individual para todos os executores Codex", () => {
    render(
      <MemoryRouter>
        <AgentListPage />
      </MemoryRouter>,
    );

    expect(
      screen.getAllByRole("button", { name: "Reconectar Codex" }),
    ).toHaveLength(4);
  });

  it("conduz as sessões em sequência usando o estado informado pelo backend", async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <AgentListPage />
      </MemoryRouter>,
    );

    await user.click(
      screen.getByRole("button", { name: "Preparar sessões Codex" }),
    );

    expect(
      screen.getByRole("heading", { name: "Preparar 4 agentes Codex" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Criar sessão de Apolo" }),
    ).toBeInTheDocument();
  });
});
