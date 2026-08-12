import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import ActiveAgentTasksPage from "./ActiveAgentTasksPage";

vi.mock("../../api/agentTask/useAgentTasks", () => ({
  useActiveAgentTasks: () => ({
    isLoading: false,
    isError: false,
    data: [
      {
        id: 18,
        assignedAgentId: 7,
        assignedAgentKey: "landing-generator",
        assignedAgentNickname: "Dédalo",
        requestedByType: "AGENT",
        requestedByName: "Têmis",
        title: "Corrigir landing do experimento 88",
        description: "Demonstrar o produto digital na página.",
        priority: "URGENT",
        status: "BLOCKED",
        sourceReference: "experiment:88",
        taskKind: "WORK",
        createdAt: "2026-08-12T10:00:00Z",
        updatedAt: "2026-08-12T10:05:00Z",
      },
    ],
  }),
}));

describe("ActiveAgentTasksPage", () => {
  it("mostra a tarefa ativa, o executor e o motivo operacional", () => {
    render(
      <MemoryRouter>
        <ActiveAgentTasksPage />
      </MemoryRouter>,
    );

    expect(
      screen.getByText(/Corrigir landing do experimento 88/),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Dédalo" })).toHaveAttribute(
      "href",
      "/agents/7",
    );
    expect(screen.getByText("Bloqueada")).toBeInTheDocument();
    expect(
      screen.getByText("Demonstrar o produto digital na página."),
    ).toBeInTheDocument();
    expect(screen.getByText("Têmis")).toBeInTheDocument();
  });
});
