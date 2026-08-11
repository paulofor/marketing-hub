import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import SystemImprovementsPage from "./SystemImprovementsPage";

vi.mock("../../api/agent/useAgents", () => ({
  useAgents: () => ({
    data: [
      {
        id: 7,
        agentKey: "landing-generator",
        nickname: "Dédalo",
        name: "Gerador de Landing",
      },
    ],
  }),
}));

vi.mock("../../api/systemImprovement/useSystemImprovements", () => ({
  useSystemImprovements: () => ({
    isLoading: false,
    data: [
      {
        id: 1,
        agentNickname: "Dédalo",
        title: "Melhorar prova visual",
        description: "Tornar a comparação legível no celular.",
        taskReference: "experimento 88",
        status: "SUGGESTED",
        requestedAt: "2026-08-11T12:00:00Z",
      },
    ],
  }),
  useCreateSystemImprovement: () => ({
    isPending: false,
    mutateAsync: vi.fn(),
  }),
}));

describe("SystemImprovementsPage", () => {
  it("exibe cadastro, data, agente solicitante e origem da tarefa", () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <SystemImprovementsPage />
      </QueryClientProvider>,
    );

    expect(screen.getByText("Melhorias do Sistema")).toBeInTheDocument();
    expect(screen.getByRole("option", { name: /Dédalo/ })).toBeInTheDocument();
    expect(screen.getByText("Melhorar prova visual")).toBeInTheDocument();
    expect(screen.getByText("experimento 88")).toBeInTheDocument();
    expect(screen.getByText("SUGGESTED")).toBeInTheDocument();
  });
});
