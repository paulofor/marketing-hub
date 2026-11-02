import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import axios from "axios";
import ExecutionCostWorksheet from "./ExecutionCostWorksheet";
import type { ApproveCostCeilingRequest } from "../../api/financial/useCostWorksheet";
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
  vi.unstubAllGlobals();
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
async function fill(group = "Agentes") {
  fireEvent.change(await screen.findByLabelText(`Aprovado — ${group}`), {
    target: { value: "100,50" },
  });
  fireEvent.click(
    screen.getByRole("button", { name: `Aprovar teto — ${group}` }),
  );
  fireEvent.change(screen.getByLabelText("Responsável pela decisão *"), {
    target: { value: "Teste local" },
  });
  fireEvent.change(screen.getByLabelText("Justificativa da decisão *"), {
    target: { value: "Fonte sintética" },
  });
  fireEvent.click(screen.getByRole("checkbox"));
}
it("concentra os seis campos monetários em Aprovado e mantém custos e previsões somente para consulta", async () => {
  mount();
  await screen.findByLabelText("Aprovado — Agentes");
  const table = screen.getByRole("table");
  expect(within(table).getAllByRole("columnheader")).toHaveLength(4);
  expect(within(table).getAllByRole("textbox")).toHaveLength(6);
  for (const field of within(table).getAllByRole("textbox")) {
    expect(field.closest("td")?.cellIndex).toBe(1);
    expect(field).toHaveAccessibleName(/^Aprovado —/);
  }
  expect(
    screen.queryByLabelText(
      /Receita|Câmbio|Fonte —|Orçado —|Custo informado —|Previsão total —/,
    ),
  ).not.toBeInTheDocument();
  expect(screen.getByText(/Estimativas de tarefas/)).toBeVisible();
  expect(
    screen.getByLabelText("Aprovado — Produção de imagens, áudio e vídeo"),
  ).toBeDisabled();
  expect(axios.post).not.toHaveBeenCalled();
});
it.each([true, false])(
  "preserva rascunho, revisão e chave em recuperação; UUID nativo: %s",
  async (native) => {
    if (!native)
      vi.stubGlobal("crypto", {
        getRandomValues: globalThis.crypto.getRandomValues.bind(
          globalThis.crypto,
        ),
      });
    vi.mocked(axios.post).mockRejectedValue(new Error("offline"));
    mount();
    await fill();
    fireEvent.click(
      screen.getByRole("button", { name: "Confirmar e registrar teto" }),
    );
    await screen.findByRole("alert");
    fireEvent.click(
      screen.getByRole("button", { name: "Confirmar e registrar teto" }),
    );
    await waitFor(() => expect(axios.post).toHaveBeenCalledTimes(2));
    const first = vi.mocked(axios.post).mock
      .calls[0][1] as ApproveCostCeilingRequest;
    expect(vi.mocked(axios.post).mock.calls[1][1]).toEqual(first);
    expect(first).toMatchObject({
      code: "AGENTS",
      approved: 100.5,
      confirmed: true,
    });
    expect(first.requestKey).toMatch(/^[0-9a-f-]{36}$/i);
    expect(first).not.toHaveProperty("reportedCost");
    expect(first).not.toHaveProperty("forecast");
    expect(vi.mocked(axios.post).mock.calls[0][0]).toMatch(/\/cost-ceilings$/);
  },
);
it("exige confirmação, impede duplicação e invalida o aceite se o valor mudar", async () => {
  vi.mocked(axios.post).mockImplementation(() => new Promise(() => {}));
  mount();
  await fill("Entrega e suporte");
  fireEvent.change(screen.getByLabelText("Aprovado — Entrega e suporte"), {
    target: { value: "101" },
  });
  expect(screen.getByRole("checkbox")).not.toBeChecked();
  expect(
    screen.getByRole("button", { name: "Confirmar e registrar teto" }),
  ).toBeDisabled();
  fireEvent.click(screen.getByRole("checkbox"));
  fireEvent.click(
    screen.getByRole("button", { name: "Confirmar e registrar teto" }),
  );
  await waitFor(() => expect(axios.post).toHaveBeenCalledTimes(1));
  expect(screen.getByRole("button", { name: "Registrando…" })).toBeDisabled();
  expect(screen.getByLabelText("Aprovado — Agentes")).toBeDisabled();
});
it("apresenta recibo depois de gravar e mantém zero e ausência distintos", async () => {
  const result = structuredClone(fixture);
  result.rows[3].approved = 0;
  result.ceilingHistory = [
    {
      eventId: 98,
      code: "DELIVERY",
      approved: 0,
      current: true,
      operatorName: "Teste local",
      recordedAt: "2026-11-02T12:00:00Z",
      justification: "Sem despesa nesta previsão sintética",
    },
  ];
  vi.mocked(axios.post).mockResolvedValue({ data: result });
  mount();
  await fill("Entrega e suporte");
  fireEvent.change(screen.getByLabelText("Aprovado — Entrega e suporte"), {
    target: { value: "0" },
  });
  fireEvent.click(screen.getByRole("checkbox"));
  fireEvent.click(
    screen.getByRole("button", { name: "Confirmar e registrar teto" }),
  );
  await screen.findByText(/Teto de Entrega e suporte registrado/);
  expect(screen.getByLabelText("Aprovado — Entrega e suporte")).toHaveValue(
    "0",
  );
  expect(screen.getByLabelText("Aprovado — Operação fixa")).toHaveValue("");
  expect(screen.getByText(/Recibo #98/)).toBeInTheDocument();
});
it("mantém ciclos encerrados em consulta sem decisões", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: { ...fixture, editable: false },
  });
  mount();
  await screen.findByText(/Ciclo encerrado:/);
  for (const field of screen.getAllByRole("textbox"))
    expect(field).toBeDisabled();
  expect(axios.post).not.toHaveBeenCalled();
});
