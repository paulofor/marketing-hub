import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import ProductProcessTaskTracking from "./ProductProcessTaskTracking";

const task = {
  taskId: 378,
  status: "IN_PROGRESS",
  agentName: "Dédalo",
  createdAt: "2026-09-10T17:51:05Z",
  startedAt: "2026-09-10T17:51:28Z",
};
afterEach(cleanup);

describe("ProductProcessTaskTracking", () => {
  it("confirms the created identifier before the activity query catches up", () => {
    render(
      <ProductProcessTaskTracking
        task={{
          ...task,
          taskId: 377,
          status: "BLOCKED",
          executionError: "Erro antigo",
        }}
        feedback={{ taskIds: [378] }}
      />,
    );
    expect(screen.getByRole("status")).toHaveTextContent(
      "Tarefa #378 registrada",
    );
    expect(screen.queryByText("Erro antigo")).not.toBeInTheDocument();
  });
  it("keeps the known task when tracking fails without calling it completed", () => {
    render(<ProductProcessTaskTracking task={task} trackingError />);
    expect(screen.getByRole("status")).toHaveTextContent(
      "Tarefa #378 · Em execução",
    );
    expect(screen.getByRole("alert")).toHaveTextContent(
      "última situação conhecida foi preservada",
    );
    expect(screen.queryByText(/Concluída/)).not.toBeInTheDocument();
  });
  it("replaces the initial receipt with the persisted blocked outcome", () => {
    render(
      <ProductProcessTaskTracking
        task={{
          ...task,
          status: "BLOCKED",
          executionError: "URL executável ausente",
          recommendedAction: "Disponibilizar a versão corrigida.",
        }}
        feedback={{ taskIds: [378], message: "Tarefa criada" }}
      />,
    );
    expect(screen.getByRole("status")).toHaveTextContent(
      "Tarefa #378 · Bloqueada",
    );
    expect(
      screen.getByText("Disponibilizar a versão corrigida."),
    ).toBeVisible();
    expect(
      screen.getByText("A atividade ainda não foi concluída."),
    ).toBeVisible();
    expect(screen.queryByText("Tarefa criada")).not.toBeInTheDocument();
  });
  it("shows the completed task and preserves an independent send failure", () => {
    render(
      <ProductProcessTaskTracking
        task={{
          ...task,
          status: "COMPLETED",
          finishedAt: "2026-09-10T17:53:00Z",
        }}
        feedback={{ error: "Não foi possível criar a próxima tarefa." }}
      />,
    );
    expect(screen.getByRole("status")).toHaveTextContent(
      "Tarefa #378 · Concluída",
    );
    expect(screen.getByRole("alert")).toHaveTextContent(
      "Não foi possível criar",
    );
    expect(screen.getByText(/Fim:/)).toBeVisible();
  });
});
