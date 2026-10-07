import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import axios from "axios";
import { afterEach, expect, it, vi } from "vitest";
import CycleAdjustmentPreparationForm from "./CycleAdjustmentPreparationForm";
import {
  cycleApi,
  type LearningCycle,
} from "../../api/learningCycle/useLearningCycles";

vi.mock("axios");
afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});
const cycle = {
  id: 7106,
  productId: 8010,
  experimentId: 9099,
  status: "ADJUSTED",
  revision: 4,
  productVersion: "candidate-v1",
} as LearningCycle;
const endpoint = `${cycleApi}/products/8010/7106/decision-proposal/adjustment-successor`;
function mount(value = cycle, onUpdated = vi.fn()) {
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <CycleAdjustmentPreparationForm cycle={value} onUpdated={onUpdated} />
    </QueryClientProvider>,
  );
  return onUpdated;
}

it("reutiliza o ajuste pelo endpoint oficial, sem datas ou orçamento no envio", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: { available: true, reason: "Ajuste válido sem exposição." },
  });
  const successor = {
    ...cycle,
    id: 7110,
    previousCycleId: 7106,
    status: "OPEN",
  };
  vi.mocked(axios.post).mockResolvedValue({ data: successor });
  const updated = mount();
  const input = await screen.findByRole("textbox", {
    name: "Identificação da nova versão *",
  });
  expect(screen.getAllByRole("textbox")).toHaveLength(1);
  expect(axios.post).not.toHaveBeenCalled();
  fireEvent.change(input, { target: { value: "candidate-v2" } });
  fireEvent.submit(screen.getByRole("form"));
  await waitFor(() => expect(updated).toHaveBeenCalledWith(successor));
  expect(axios.get).toHaveBeenCalledWith(endpoint);
  expect(axios.post).toHaveBeenCalledWith(endpoint, {
    expectedRevision: 4,
    productVersion: "candidate-v2",
  });
});

it("respeita a indisponibilidade do backend e o sucessor existente", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: { available: false, reason: "Há mercado ou decisão incompatível." },
  });
  mount();
  await waitFor(() => expect(axios.get).toHaveBeenCalled());
  expect(screen.queryByRole("form")).toBeNull();
  expect(axios.post).not.toHaveBeenCalled();
  cleanup();
  vi.clearAllMocks();
  mount({ ...cycle, successorCycleId: 7110 });
  expect(axios.get).not.toHaveBeenCalled();
  expect(screen.queryByRole("form")).toBeNull();
});

it("mostra falha real e conserva os dados para corrigir sem enviar automaticamente", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: { available: true, reason: "Disponível" },
  });
  vi.mocked(axios.post).mockRejectedValue({
    response: { data: { message: "O ciclo mudou" } },
  });
  const updated = mount();
  const input = await screen.findByRole("textbox");
  fireEvent.change(input, { target: { value: "candidate-v1" } });
  expect(screen.getByRole("button")).toBeDisabled();
  fireEvent.change(input, { target: { value: "candidate-v2" } });
  fireEvent.submit(screen.getByRole("form"));
  await screen.findByRole("alert");
  expect(input).toHaveValue("candidate-v2");
  expect(updated).not.toHaveBeenCalled();
  expect(axios.post).toHaveBeenCalledTimes(1);
});
