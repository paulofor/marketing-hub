import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import AgentWorkspacePage from "./AgentWorkspacePage";

vi.mock("../../api/agent/useAgents", () => ({
  useAgents: () => ({
    isLoading: false,
    data: [
      {
        id: 7,
        agentKey: "landing-generator",
        nickname: "Dédalo",
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
        assignedAgentKey: "landing-generator",
        assignedAgentNickname: "Dédalo",
        requestedByType: "AGENT",
        requestedByName: "Têmis",
        title: "Aprimorar hero",
        description: "Melhorar a clareza da promessa no celular.",
        priority: "HIGH",
        status: "PENDING",
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

describe("AgentWorkspacePage", () => {
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
});
