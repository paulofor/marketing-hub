import { cleanup, render, screen, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
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
        agentId: 7,
        nickname: "Dédalo",
        agentName: "Agente Gerador de Landing",
        workStatus: "WORKING",
        currentWork: "Correção autônoma da landing do experimento #88",
        progressDetail: "Etapa em processamento",
        taskId: 14,
        executionId: 326,
        externalDecisionRequired: false,
        lastActivityAt: "2026-08-11T04:00:00Z",
        dailyTokens: 12345,
        dailyTokenDate: "2026-08-11",
      },
    ],
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
    expect(screen.getByText("WORKING")).toBeInTheDocument();
    expect(screen.getByText("Tokens hoje")).toBeInTheDocument();
    expect(screen.getByText("12.345")).toBeInTheDocument();
    expect(screen.getByText("Tarefa #14 · Execução #326")).toBeInTheDocument();
  });
});
