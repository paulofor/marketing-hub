import { render, screen, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
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

describe("AgentListPage", () => {
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
});
