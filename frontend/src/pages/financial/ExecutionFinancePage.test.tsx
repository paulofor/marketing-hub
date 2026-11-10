vi.mock("./SingleVideoBudgetPanel", () => ({ default: () => null }));
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import axios from "axios";
import ExecutionFinancePage from "./ExecutionFinancePage";
import fixtureJson from "../../../e2e/fixtures/execution-finance.json?raw";
const fixture = JSON.parse(fixtureJson);
vi.mock("axios");
// A planilha tem suíte própria; esta suíte preserva o contrato anterior das aprovações.
vi.mock("./ExecutionCostWorksheet", () => ({ default: () => null }));
let client: QueryClient;
beforeEach(() => {
  vi.resetAllMocks();
  vi.mocked(axios.get).mockResolvedValue({ data: fixture });
});
afterEach(() => {
  cleanup();
  client?.clear();
  vi.unstubAllGlobals();
});
function mount() {
  client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter
        initialEntries={[
          "/financial/execution?productId=700&chainId=2700&cycleId=1900",
        ]}
      >
        <ExecutionFinancePage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}
it("mostra custos e parecer real, sem autorizar ou disparar análise ao abrir", async () => {
  mount();
  await screen.findByText(fixture.productName);
  expect(screen.getAllByText(/Parecer simulado/)[0]).toBeVisible();
  expect(
    screen.getByRole("link", { name: /Analisar cenários/ }),
  ).toHaveAttribute("href", fixture.planUrl);
  expect(
    screen.queryByRole("button", { name: "Aprovar limite de preparação" }),
  ).not.toBeInTheDocument();
  expect(axios.post).not.toHaveBeenCalled();
  expect(axios.get).toHaveBeenCalledWith(
    expect.stringContaining("/products/700/1900/financial-review"),
    { params: { chainId: 2700 } },
  );
});
it("apresenta a situação informada pelo backend sem deduzir liberação pelo saldo", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: {
      ...fixture,
      availableUsd: 10,
      preparationBalance: {
        status: "COST_UNKNOWN",
        title: "Custo acumulado ainda desconhecido",
        explanation:
          "O recibo está salvo, mas a cobertura precisa ser conferida.",
        shortfallUsd: null,
        requiresBudgetReview: false,
      },
    },
  });
  mount();
  await screen.findByText("Custo acumulado ainda desconhecido");
  expect(
    screen.queryByRole("link", { name: "Rever teto ou reserva" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByText(/saldo estimado disponível/),
  ).not.toBeInTheDocument();
  expect(axios.post).not.toHaveBeenCalled();
});
it("mantém histórico de ciclo encerrado sem formulário de aprovação", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: {
      ...fixture,
      canAuthorize: false,
      authorizationBlocker: "Ciclo encerrado",
      financialPlan: null,
      advice: {
        ...fixture.advice,
        status: "UNAVAILABLE",
        sourceId: null,
        figures: [],
        fullRationale: null,
        title: "Sem parecer disponível",
        recommendation: "Ainda não há parecer de Plutus neste ciclo.",
      },
    },
  });
  mount();
  await screen.findByText("Sem parecer disponível");
  expect(
    screen.queryByRole("button", { name: "Aprovar limite de preparação" }),
  ).not.toBeInTheDocument();
  expect(screen.getByText(/Ainda não há parecer de Plutus/)).toBeVisible();
  expect(axios.post).not.toHaveBeenCalled();
});
it("oculta decisões de uma consulta indisponível", async () => {
  vi.mocked(axios.get).mockRejectedValue(new Error("offline"));
  mount();
  await waitFor(() => expect(screen.getByRole("alert")).toBeVisible());
  expect(
    screen.queryByRole("button", { name: "Aprovar limite de preparação" }),
  ).not.toBeInTheDocument();
});

it("mostra referência anterior de Plutus com números e mantém o relatório recolhido", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: {
      ...fixture,
      financialPlan: null,
      advice: {
        ...fixture.advice,
        status: "STALE",
        title: "Reavaliar antes de investir mais",
        recommendation: "A estratégia mudou depois deste parecer.",
        basis: "PLANNING",
        sourceId: 883,
        fullRationale: "Parecer integral anterior, sem renovação automática.",
      },
    },
  });
  mount();
  await screen.findByText("Reavaliar antes de investir mais");
  expect(
    screen.getByText("A estratégia mudou depois deste parecer."),
  ).toBeVisible();
  expect(screen.getByText("R$ 67,00", { exact: false })).toBeVisible();
  const detail = screen
    .getByText("Ver parecer completo, cenários e fontes")
    .closest("details");
  expect(detail).not.toHaveAttribute("open");
  expect(screen.getByText(/Parecer integral anterior/)).not.toBeVisible();
  fireEvent.click(screen.getByText("Ver parecer completo, cenários e fontes"));
  expect(detail).toHaveAttribute("open");
  expect(axios.post).not.toHaveBeenCalled();
});

const videoBudget = {
  productId: 700,
  productName: fixture.productName,
  internalName: "teste",
  chainDefinitionId: 2700,
  processDefinitionId: 1200,
  cycleId: 1900,
  experimentId: 11200,
  productVersion: "teste-v3",
  revision: 1,
  stageLabel: "Definir os dois vídeos",
  status: "AWAITING_LIMIT",
  instruction: "Decisão financeira",
  scope: "TWO_VIDEOS_PRODUCTION_AND_REVIEW",
  currency: "USD",
  canAuthorize: true,
  blocker: null,
  currentAuthorization: null,
  history: [],
  cycleUrl: fixture.cycleUrl,
  financeUrl: fixture.videoUrl,
};

it("concentra os atalhos dos três orçamentos na planilha sem formulários monetários duplicados", async () => {
  mount();
  await screen.findByText(fixture.productName);
  const links = screen.getAllByRole("link", { name: "Ver teto na planilha" });
  expect(links.map((link) => link.getAttribute("href"))).toEqual([
    "#ceiling-AGENTS",
    "#ceiling-PRODUCTION",
    "#ceiling-MEDIA",
  ]);
  expect(screen.queryByRole("spinbutton")).not.toBeInTheDocument();
  expect(axios.post).not.toHaveBeenCalled();
});

it("preserva o ciclo de origem e não solicita aprovação repetida ao consultar um sucessor", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: {
      ...fixture,
      knownPreparationUsd: 76.35,
      availableUsd: 23.65,
      authorization: {
        eventId: 8001,
        sourceCycleId: 1899,
        carriedForward: true,
        reference: "internal://learning-cycles/1899/preparation-budget/test",
        totalLimitUsd: 120,
        reserveUsd: 20,
        operatorName: "Operador local",
        justification: "Teto acumulado",
        approvedAt: fixture.observedAt,
        productVersion: "versao-anterior",
        current: true,
      },
      preparationBalance: {
        status: "BALANCE_AVAILABLE",
        title: "Teto acumulado vigente",
        explanation: "O saldo acumulado cobre a preparação sem renovar o teto.",
        shortfallUsd: 0,
        requiresBudgetReview: false,
      },
    },
  });
  mount();
  await screen.findByText("Teto acumulado vigente");
  expect(screen.getByText(/Recibo #8001 · Ciclo #1899/)).toBeVisible();
  expect(
    screen.getByText(/Teto e reserva mantidos do ciclo anterior/),
  ).toBeVisible();
  expect(screen.getByText(/23,65 disponíveis/)).toBeVisible();
  expect(
    screen.queryByRole("link", { name: "Rever teto aprovado" }),
  ).not.toBeInTheDocument();
  expect(axios.post).not.toHaveBeenCalled();
});

it.each([0, -1.25])(
  "mostra atenção e revisão quando o backend informa teto atingido: %s",
  async (availableUsd) => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        ...fixture,
        availableUsd,
        authorization: {
          totalLimitUsd: 120,
          reserveUsd: 20,
          sourceCycleId: 1899,
          carriedForward: true,
          eventId: 8001,
          operatorName: "Operador local",
          approvedAt: fixture.observedAt,
        },
        preparationBalance: {
          status: "NO_BALANCE",
          title: "Atenção: teto de agentes atingido",
          explanation:
            "Revise o teto acumulado na planilha antes de nova tarefa.",
          shortfallUsd: Math.abs(availableUsd),
          requiresBudgetReview: true,
        },
      },
    });
    mount();
    await screen.findByText("Atenção: teto de agentes atingido");
    expect(
      screen.getByRole("link", { name: "Rever teto aprovado" }),
    ).toHaveAttribute("href", "#ceiling-AGENTS");
    expect(axios.post).not.toHaveBeenCalled();
  },
);
