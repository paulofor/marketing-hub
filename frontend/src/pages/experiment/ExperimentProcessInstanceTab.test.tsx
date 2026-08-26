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
        activities: [
          {
            activityInstanceId: 12,
            activityDefinitionId: 4,
            activityId: "html",
            activityName: "Construir HTML",
            objective: "Entregar HTML funcional",
            occurrenceNumber: 1,
            status: "PENDING",
            operationalState: "RELEASED",
            stateReason:
              "Atividade liberada para consumo pelo executor responsável.",
            enteredAt: "2026-08-25T10:00:00Z",
            objectiveAchieved: false,
            knownCostUsd: 0.04,
            costCoverage: "COMPLETE",
            evidenceQuality: "DIRECT",
            tasks: [
              {
                taskId: 30,
                activityInstanceId: 12,
                attemptNumber: 1,
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
            ],
          },
          {
            activityInstanceId: 13,
            activityDefinitionId: 5,
            activityId: "review",
            activityName: "Avaliar percepção",
            occurrenceNumber: 1,
            status: "PENDING",
            operationalState: "WAITING_PREDECESSOR",
            stateReason:
              "Aguardando a conclusão das atividades predecessoras do processo.",
            enteredAt: "2026-08-25T10:01:00Z",
            objectiveAchieved: false,
            costCoverage: "NOT_REPORTED",
            evidenceQuality: "DIRECT",
            tasks: [
              {
                taskId: 31,
                activityInstanceId: 13,
                attemptNumber: 1,
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
          },
        ],
        tasks: [
          {
            taskId: 30,
            attemptNumber: 1,
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
            attemptNumber: 1,
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
            attemptNumber: 1,
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

    expect(screen.getAllByText("Liberada")).toHaveLength(2);
    expect(screen.getAllByText("Aguardando predecessora")).toHaveLength(2);
    expect(
      screen.getByText(/1 tarefa\(s\) legada\(s\) substituída\(s\)/),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Tokens: entrada 2.000 · saída 500 · cache 800"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Objetivo: Entregar HTML funcional"),
    ).toBeInTheDocument();
    expect(screen.getAllByText("Objetivo atingido")).toHaveLength(2);
    expect(screen.getByText("Tentativa 1 · tarefa #30")).toBeInTheDocument();
  });
});
