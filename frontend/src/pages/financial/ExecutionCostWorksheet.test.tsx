import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import axios from "axios";
import ExecutionCostWorksheet from "./ExecutionCostWorksheet";
import type { CostWorksheetRequest } from "../../api/financial/useCostWorksheet";
import fixtureJson from "../../../e2e/fixtures/cost-worksheet.json?raw";
import financeJson from "../../../e2e/fixtures/execution-finance.json?raw";
const fixture = JSON.parse(fixtureJson),
  finance = JSON.parse(financeJson);
vi.mock("axios");
let client: QueryClient;
beforeEach(() => {
  vi.resetAllMocks();
  vi.mocked(axios.get).mockResolvedValue({ data: structuredClone(fixture) });
});
afterEach(() => {
  cleanup();
  client.clear();
});
function mount() {
  client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <ExecutionCostWorksheet data={finance} disabled={false} />
    </QueryClientProvider>,
  );
}
async function fill() {
  await screen.findByLabelText("Início do período *");
  for (const [label, value] of [
    ["Início do período *", "2026-11-01"],
    ["Fim do período *", "2026-11-30"],
    ["Responsável pela planilha *", "Teste local"],
    ["Justificativa e premissas gerais *", "Fonte sintética"],
    ["Orçado — Agentes", "100"],
    ["Previsão total — Agentes", "90"],
    ["Fonte — Agentes", "Tarefas sintéticas"],
  ])
    fireEvent.change(screen.getByLabelText(label), { target: { value } });
}
it("apresenta seis grupos e valores desconhecidos sem criar aprovações", async () => {
  mount();
  await screen.findByLabelText("Orçado — Agentes");
  expect(screen.getAllByRole("columnheader")).toHaveLength(6);
  expect(screen.getAllByRole("rowheader")).toHaveLength(6);
  expect(screen.getByText(/Estimativa automática:/)).toHaveTextContent(/38,89/);
  expect(
    screen.getByText(/Resultado previsto após os seis grupos:/),
  ).toHaveTextContent("Não informado");
  expect(axios.post).not.toHaveBeenCalled();
});
it("preserva rascunho e mesma chave após falha e não preenche ausências com zero", async () => {
  vi.mocked(axios.post).mockRejectedValue(new Error("offline"));
  mount();
  await fill();
  fireEvent.click(
    screen.getByRole("button", { name: "Salvar custos e previsões" }),
  );
  await waitFor(() => expect(axios.post).toHaveBeenCalledTimes(1));
  await screen.findByRole("alert");
  fireEvent.click(
    screen.getByRole("button", { name: "Salvar custos e previsões" }),
  );
  await waitFor(() => expect(axios.post).toHaveBeenCalledTimes(2));
  const first = vi.mocked(axios.post).mock.calls[0][1] as CostWorksheetRequest;
  expect(vi.mocked(axios.post).mock.calls[1][1]).toEqual(first);
  expect(first.approveOperatingBudgets).toBe(false);
  expect(first.lines[0].forecast).toBe(90);
  expect(first.lines[1].forecast).toBeNull();
  expect(first.expectedRevenueBrl).toBeNull();
});
it("envia aprovação somente após confirmação explícita e bloqueia envio duplicado", async () => {
  vi.mocked(axios.post).mockImplementation(() => new Promise(() => {}));
  mount();
  await fill();
  fireEvent.click(
    screen.getByRole("checkbox", { name: /Aprovo os valores orçados/ }),
  );
  fireEvent.click(
    screen.getByRole("button", {
      name: "Salvar e aprovar os três orçamentos operacionais",
    }),
  );
  await waitFor(() => expect(axios.post).toHaveBeenCalledTimes(1));
  expect(
    (vi.mocked(axios.post).mock.calls[0][1] as CostWorksheetRequest)
      .approveOperatingBudgets,
  ).toBe(true);
  expect(screen.getByRole("button", { name: "Salvando…" })).toBeDisabled();
});
it("apresenta cálculos salvos, separa moedas e avisa que mudanças não foram calculadas", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: {
      ...fixture,
      projectedResultBrl: 880,
      totals: fixture.totals.map((t: object) => ({ ...t, forecast: 160 })),
    },
  });
  mount();
  await fill();
  expect(
    screen.getByText(/Resultado previsto após os seis grupos:/),
  ).toHaveTextContent("880,00");
  expect(screen.getByText(/Há alterações não salvas/)).toBeVisible();
  expect(screen.getByText("Dólares (US$)")).toBeVisible();
  expect(screen.getByText("Reais (R$)")).toBeVisible();
});
it("recusa edição de ciclo encerrado", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: { ...fixture, editable: false },
  });
  mount();
  await screen.findByText(/Ciclo encerrado:/);
  expect(
    screen.getByRole("button", { name: "Salvar custos e previsões" }),
  ).toBeDisabled();
  expect(axios.post).not.toHaveBeenCalled();
});
