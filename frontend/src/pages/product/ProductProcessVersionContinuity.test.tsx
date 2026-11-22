import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import axios from "axios";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import ProductProcessVersionContinuity from "./ProductProcessVersionContinuity";
vi.mock("axios");
const preview = {
  fingerprint: "snapshot",
  sourceProcessId: 117,
  targetProcessId: 127,
  successorRunId: null,
  canPrepare: true,
  reason: "Confira as provas",
  url: "/products/4/value-chain-history/processes/127/activities?chainId=27&learningCycleId=22&sourceReference=experiment%3A115",
  outputs: Array.from({ length: 8 }, (_, n) => ({
    activityId: `step-${n}`,
    name: `Entrega ${n}`,
    accepted: true,
    instanceId: 200 + n,
    changedFields: n === 0 ? ["/description"] : [],
    sourceContract: '{"description":"Aceite original"}',
    targetContract: '{"description":"Aceite atual"}',
    evidence: `Prova ${n}`,
    taskIds: [300 + n],
    owner: "Dédalo",
    destinationStatus: n === 0 ? "REVALIDATION_PENDING" : "ACCEPTED",
    criteria: [
      { description: "Novo requisito", status: "REVALIDATION_PENDING" },
    ],
  })),
};
function mount(productId = 4, cycleId = 22) {
  return render(
    <MemoryRouter>
      <QueryClientProvider
        client={
          new QueryClient({
            defaultOptions: {
              queries: { retry: false },
              mutations: { retry: false },
            },
          })
        }
      >
        <ProductProcessVersionContinuity
          productId={productId}
          cycleId={cycleId}
          processId={117}
        />
      </QueryClientProvider>
    </MemoryRouter>,
  );
}
beforeEach(() => {
  vi.resetAllMocks();
});
afterEach(cleanup);
it("preserva oito entregas, exige conferir alteração e prepara sem disparar retomada", async () => {
  vi.mocked(axios.get).mockResolvedValue({ data: preview });
  vi.mocked(axios.post).mockResolvedValue({
    data: { ...preview, successorRunId: 900, canPrepare: false },
  });
  mount();
  await screen.findByText(/Entrega 7/);
  const button = screen.getByRole("button", {
    name: "Preparar continuidade sem executar",
  });
  expect(button).toBeDisabled();
  fireEvent.change(screen.getByLabelText("Responsável pela conferência *"), {
    target: { value: "Operador sintético" },
  });
  fireEvent.change(screen.getByLabelText("Motivo da continuidade *"), {
    target: { value: "Continuar construção" },
  });
  expect(button).toBeEnabled();
  expect(
    screen.getByText(/Revalidação pendente — responsável: Dédalo/),
  ).toBeInTheDocument();
  fireEvent.click(button);
  expect(
    await screen.findByRole("link", { name: "Abrir continuidade #900" }),
  ).toHaveAttribute("href", preview.url);
  expect(axios.post).toHaveBeenCalledWith(
    "/api/business-processes/117/products/4/automation/v1/continuity",
    {
      fingerprint: "snapshot",
      operatorName: "Operador sintético",
      reason: "Continuar construção",
      compatibilityEvidence: {},
    },
    { params: { learningCycleId: 22 }, timeout: 45000 },
  );
});
it("mostra falha terminal e recupera somente a consulta no contexto do outro produto", async () => {
  vi.mocked(axios.get)
    .mockRejectedValueOnce(new Error("rede"))
    .mockResolvedValueOnce({
      data: { ...preview, canPrepare: false, reason: "Ciclo encerrado" },
    });
  mount(71, 99);
  await screen.findByRole("alert");
  expect(screen.queryByRole("status")).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Consultar novamente" }));
  await screen.findByText(/Ciclo encerrado/);
  expect(axios.get).toHaveBeenLastCalledWith(
    "/api/business-processes/117/products/71/automation/v1/continuity",
    expect.objectContaining({ params: { learningCycleId: 99 } }),
  );
  expect(axios.post).not.toHaveBeenCalled();
  expect(
    screen.queryByRole("button", {
      name: "Preparar continuidade sem executar",
    }),
  ).not.toBeInTheDocument();
});
it("mantém falha da gravação visível sem anunciar sucesso", async () => {
  vi.mocked(axios.get).mockResolvedValue({ data: { ...preview, outputs: [] } });
  vi.mocked(axios.post).mockRejectedValue(new Error("falha"));
  mount();
  await screen.findByLabelText("Responsável pela conferência *");
  fireEvent.change(screen.getByLabelText("Responsável pela conferência *"), {
    target: { value: "Operador" },
  });
  fireEvent.change(screen.getByLabelText("Motivo da continuidade *"), {
    target: { value: "Motivo" },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Preparar continuidade sem executar" }),
  );
  await screen.findByRole("alert");
  expect(
    screen.queryByRole("link", { name: /Abrir continuidade/ }),
  ).not.toBeInTheDocument();
  await waitFor(() => expect(axios.post).toHaveBeenCalledTimes(1));
});
