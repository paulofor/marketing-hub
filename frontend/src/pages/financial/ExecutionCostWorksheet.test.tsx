import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  act,
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
  ).toBeEnabled();
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
  await waitFor(() => expect(screen.getByRole("checkbox")).not.toBeChecked());
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

it.each(["Produção de imagens, áudio e vídeo", "Mídia"])(
  "registra teto antecipado de %s na própria planilha sem apresentar liberação",
  async (group) => {
    vi.mocked(axios.post).mockImplementation(async (_, body) => {
      const request = body as ApproveCostCeilingRequest;
      const saved = structuredClone(fixture);
      const row = saved.rows.find(
        (item: { code: string }) => item.code === request.code,
      );
      row.approved = request.approved;
      row.approvalStatus =
        "Teto de planejamento salvo; aplicação operacional ainda não autorizada";
      return { data: saved };
    });
    mount();
    await fill(group);
    expect(
      screen.getByText(/Esta decisão registra somente o teto de planejamento/),
    ).toBeVisible();
    fireEvent.click(
      screen.getByRole("button", { name: "Confirmar e registrar teto" }),
    );
    await screen.findByText(
      "Teto de planejamento salvo; aplicação operacional ainda não autorizada",
    );
    expect(screen.getByLabelText(`Aprovado — ${group}`)).toHaveValue("100.5");
    expect(axios.post).toHaveBeenCalledTimes(1);
    expect(vi.mocked(axios.post).mock.calls[0][0]).toMatch(/cost-ceilings$/);
  },
);

it("exige novo aceite quando a etapa muda o efeito de planejamento para limite operacional", async () => {
  mount();
  await fill("Produção de imagens, áudio e vídeo");
  expect(screen.getByRole("checkbox")).toBeChecked();
  const updated = structuredClone(fixture);
  updated.revision += 1;
  updated.rows[1].approvalScope = "EXECUTION_LIMIT";
  updated.rows[1].executionBlocker = null;
  act(() =>
    client.setQueryData(
      ["cost-worksheet", finance.productId, finance.chainId, finance.cycleId],
      updated,
    ),
  );
  await waitFor(() => expect(screen.getByRole("checkbox")).not.toBeChecked());
  expect(
    screen.getByRole("button", { name: "Confirmar e registrar teto" }),
  ).toBeDisabled();
  expect(screen.getByText(/O teto operacional cobre/)).toBeVisible();
  expect(axios.post).not.toHaveBeenCalled();
});

it("apresenta o teto herdado e a origem do recibo sem exigir outra aprovação", async () => {
  const next = structuredClone(fixture);
  next.rows[0].approved = 120;
  next.rows[0].approvalStatus =
    "Teto acumulado mantido do ciclo #1899 · recibo #8001. Mudar de ciclo não exige nova aprovação do teto nem renova o saldo.";
  next.ceilingHistory = [
    {
      eventId: 8001,
      code: "AGENTS",
      approved: 120,
      operatorName: "Operador local",
      recordedAt: finance.observedAt,
      justification: "Teto acumulado",
      current: true,
      sourceCycleId: 1899,
      carriedForward: true,
    },
  ];
  vi.mocked(axios.get).mockResolvedValue({ data: next });
  mount();
  expect(await screen.findByLabelText("Aprovado — Agentes")).toHaveValue("120");
  expect(
    screen.getByText(/Teto acumulado mantido do ciclo #1899/),
  ).toBeVisible();
  expect(screen.getByText(/Ciclo de origem #1899/)).toBeInTheDocument();
  expect(axios.post).not.toHaveBeenCalled();
});
