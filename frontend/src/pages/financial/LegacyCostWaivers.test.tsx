import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import axios from "axios";
import LegacyCostWaivers from "./LegacyCostWaivers";
import type { ExecutionFinance } from "../../api/financial/useExecutionFinance";
vi.mock("axios");
afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});
it("registra somente seleção explícita e reabre o histórico sem retomar", async () => {
  const view = {
    tasks: [246, 247].map((taskId) => ({
      taskId,
      experimentId: 90,
      status: "CANCELLED",
      costUsd: null,
      waived: false,
      selectable: true,
    })),
    history: [],
    blockingTasks: 2,
  };
  vi.mocked(axios.get).mockResolvedValue({ data: view });
  vi.mocked(axios.post).mockResolvedValue({
    data: {
      ...view,
      blockingTasks: 1,
      history: [
        {
          eventId: 1,
          cycleId: 22,
          operatorName: "Teste",
          decidedAt: "2026-11-22T12:00:00Z",
          reason: "Histórico",
          taskIds: [246],
        },
      ],
      tasks: view.tasks.map((t) => ({
        ...t,
        waived: t.taskId === 246,
        selectable: t.taskId !== 246,
      })),
    },
  });
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <LegacyCostWaivers
        data={
          {
            productId: 4,
            cycleId: 22,
            chainId: 27,
            revision: 4,
            canAuthorize: true,
          } as ExecutionFinance
        }
      />
    </QueryClientProvider>,
  );
  await screen.findByText(/Pendências de conciliação sem dispensa: 2/);
  expect(axios.post).not.toHaveBeenCalled();
  fireEvent.click(screen.getByLabelText(/Tarefa #246/));
  fireEvent.change(screen.getByLabelText(/Responsável/), {
    target: { value: "Teste" },
  });
  fireEvent.change(screen.getByLabelText(/Motivo/), {
    target: { value: "Histórico" },
  });
  fireEvent.click(screen.getByLabelText(/Confirmo/));
  fireEvent.click(screen.getByRole("button", { name: /Registrar dispensa/ }));
  await screen.findByText(/Dispensa registrada/);
  expect(axios.post).toHaveBeenCalledTimes(1);
  expect(axios.post).toHaveBeenCalledWith(
    expect.stringContaining("/agent-cost-waivers/v1/products/4/cycles/22"),
    expect.objectContaining({
      taskIds: [246],
      confirmed: true,
      expectedRevision: 4,
    }),
  );
  expect(screen.getByText(/Nenhum processo foi retomado/)).toBeVisible();
  expect(screen.getByText(/Tarefas: 246/)).toBeInTheDocument();
  await waitFor(() =>
    expect(screen.getByLabelText(/Tarefa #246/)).toBeDisabled(),
  );
  client.clear();
});
