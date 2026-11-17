import { cleanup, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, expect, it } from "vitest";
import type { ProcessAutomation } from "../../api/businessProcess/useProcessAutomation";
import ProductProcessPauseNotice from "./ProductProcessPauseNotice";

afterEach(cleanup);

it.each([4, 99004])(
  "mostra ação e link oficiais junto à atividade do produto %s",
  (productId) => {
    const execution = {
      status: "PAUSED",
      reason: "Pausado pelo operador.",
      userAction: {
        title: "Confira os custos antes de retomar",
        reason: "Há dez tarefas sem custo.",
        responsible: "Você · decisão financeira",
        actionLabel: "Ver custos pendentes",
        actionUrl: `/financial/execution?productId=${productId}&chainId=27&cycleId=22`,
        afterAction: "Após conferir, Têmis revisará as provas adicionais.",
      },
    } as ProcessAutomation;
    render(
      <MemoryRouter>
        <ProductProcessPauseNotice execution={execution} />
      </MemoryRouter>,
    );
    expect(
      screen.getByRole("region", {
        name: "Por que esta atividade não continua",
      }),
    ).toHaveTextContent("Há dez tarefas sem custo.");
    expect(screen.getByText(/Sua ação/)).toBeVisible();
    expect(
      screen.getByRole("link", { name: "Ver custos pendentes" }),
    ).toHaveAttribute("href", execution.userAction!.actionUrl);
  },
);

it("preserva pausa comum sem inventar decisão financeira e não anuncia pausa em execução ativa", () => {
  const { rerender } = render(
    <MemoryRouter>
      <ProductProcessPauseNotice
        execution={
          { status: "PAUSED", reason: "Pausa solicitada." } as ProcessAutomation
        }
      />
    </MemoryRouter>,
  );
  expect(screen.getByText("Pausa solicitada.")).toBeVisible();
  expect(screen.queryByText(/Sua ação/)).not.toBeInTheDocument();
  rerender(
    <MemoryRouter>
      <ProductProcessPauseNotice
        execution={{ status: "WAITING_ACTIVITY" } as ProcessAutomation}
      />
    </MemoryRouter>,
  );
  expect(screen.queryByRole("region")).not.toBeInTheDocument();
});
