import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import AgentWorkspacePage from "./AgentWorkspacePage";

const testState = vi.hoisted(() => ({
  agentKey: "landing-generator",
  nickname: "Dédalo",
  taskStatus: "PENDING",
  sourceReference: undefined as string | undefined,
  experiments: [] as Array<Record<string, unknown>>,
}));

vi.mock("../../api/agent/useAgents", () => ({
  useAgents: () => ({
    isLoading: false,
    data: [
      {
        id: 7,
        agentKey: testState.agentKey,
        nickname: testState.nickname,
        name: "Agente Gerador de Landing",
        status: "TEST",
      },
    ],
  }),
}));

vi.mock("../../api/agentTask/useAgentTasks", () => ({
  useAgentTasks: () => ({
    isLoading: false,
    data: [
      {
        id: 41,
        assignedAgentId: 7,
        assignedAgentKey: testState.agentKey,
        assignedAgentNickname: testState.nickname,
        requestedByType: "AGENT",
        requestedByName: "Têmis",
        title: "Aprimorar hero",
        description: "Melhorar a clareza da promessa no celular.",
        priority: "HIGH",
        status: testState.taskStatus,
        sourceReference: testState.sourceReference,
        createdAt: "2026-08-11T15:00:00Z",
        updatedAt: "2026-08-11T15:00:00Z",
      },
    ],
  }),
  useCreateAgentTask: () => ({ mutate: vi.fn(), isPending: false }),
  useUpdateAgentTaskStatus: () => ({ mutate: vi.fn(), isPending: false }),
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

describe("AgentWorkspacePage", () => {
  beforeEach(() => {
    testState.agentKey = "landing-generator";
    testState.nickname = "Dédalo";
    testState.taskStatus = "PENDING";
    testState.sourceReference = undefined;
    testState.experiments = [];
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
    expect(screen.getByText("Aprimorar hero")).toBeInTheDocument();
    expect(screen.getByText(/Solicitado por Têmis/)).toBeInTheDocument();
    expect(screen.getByLabelText("Plano comercial *")).toHaveValue("9");
    expect(screen.getByText(/contexto v3/)).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Em andamento" }),
    ).toBeInTheDocument();
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
