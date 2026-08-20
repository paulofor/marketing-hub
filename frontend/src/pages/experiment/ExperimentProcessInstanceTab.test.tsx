import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import ExperimentProcessInstanceTab from "./ExperimentProcessInstanceTab";

vi.mock("../../api/agentTask/useAgentTasks", () => ({
  useProcessInstances: () => ({
    isLoading: false,
    isError: false,
    data: [
      {
        processDefinitionId: 2,
        processCode: "LANDING_PAGE_GENERATION",
        processVersionNumber: 2,
        sourceReference: "experiment:88",
        tasks: [
          {
            taskId: 30,
            activityName: "Construir HTML",
            agentKey: "landing-generator",
            agentNickname: "Dédalo",
            taskStatus: "PENDING",
            operationalState: "RELEASED",
            stateReason:
              "Atividade liberada para consumo pelo executor responsável.",
            inputTokens: 2000,
            cachedInputTokens: 800,
            outputTokens: 500,
            estimatedCostUsd: 0.04,
            costEstimationStatus: "ESTIMATED",
          },
          {
            taskId: 31,
            activityName: "Avaliar percepção",
            agentKey: "customer-psychology",
            agentNickname: "Psique",
            taskStatus: "PENDING",
            operationalState: "WAITING_PREDECESSOR",
            stateReason:
              "Aguardando a conclusão das atividades predecessoras do processo.",
            costEstimationStatus: "NOT_REPORTED",
          },
        ],
        supersededLegacyTasks: [
          {
            taskId: 27,
            activityName: "Correção antiga",
            agentKey: "landing-generator",
            agentNickname: "Dédalo",
            taskStatus: "PENDING",
            operationalState: "SUPERSEDED_LEGACY",
            stateReason: "Tarefa legada substituída.",
            costEstimationStatus: "NOT_REPORTED",
          },
        ],
      },
    ],
  }),
}));

describe("ExperimentProcessInstanceTab", () => {
  it("distingue tarefa liberada, predecessora pendente e legado substituído", () => {
    render(<ExperimentProcessInstanceTab experimentId="88" />);

    expect(screen.getByText("Liberada")).toBeInTheDocument();
    expect(screen.getByText("Aguardando predecessora")).toBeInTheDocument();
    expect(
      screen.getByText(/1 tarefa\(s\) legada\(s\) substituída\(s\)/),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Tokens: entrada 2.000 · saída 500 · cache 800"),
    ).toBeInTheDocument();
  });
});
