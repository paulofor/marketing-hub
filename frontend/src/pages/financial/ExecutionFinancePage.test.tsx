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
    screen.getByRole("button", { name: "Aprovar limite de preparação" }),
  ).toBeDisabled();
  expect(axios.post).not.toHaveBeenCalled();
  expect(axios.get).toHaveBeenCalledWith(
    expect.stringContaining("/products/700/1900/financial-review"),
    { params: { chainId: 2700 } },
  );
});
it.each([true, false])(
  "preserva decisão e chave após falha e aprova com recibo; randomUUID disponível: %s",
  async (nativeUuid) => {
    if (!nativeUuid)
      vi.stubGlobal("crypto", {
        getRandomValues: globalThis.crypto.getRandomValues.bind(
          globalThis.crypto,
        ),
      });
    vi.mocked(axios.post).mockRejectedValueOnce(new Error("rede"));
    vi.mocked(axios.post).mockResolvedValueOnce({
      data: {
        ...fixture,
        revision: 2,
        availableUsd: 7.61,
        authorization: { eventId: 8000, totalLimitUsd: 50, reserveUsd: 3.5 },
        history: [],
      },
    });
    mount();
    await screen.findByText(fixture.productName);
    fireEvent.change(screen.getByLabelText("Teto total acumulado (US$) *"), {
      target: { value: "50" },
    });
    fireEvent.change(
      screen.getByLabelText("Reserva para custos pendentes (US$) *"),
      { target: { value: "3.50" } },
    );
    fireEvent.change(screen.getByLabelText("Responsável pela decisão *"), {
      target: { value: "Teste local" },
    });
    fireEvent.change(
      screen.getByLabelText("Justificativa e fontes consideradas *"),
      { target: { value: "Conferência local" } },
    );
    fireEvent.click(screen.getByRole("checkbox"));
    fireEvent.click(
      screen.getByRole("button", { name: "Aprovar limite de preparação" }),
    );
    await screen.findByRole("alert");
    fireEvent.click(
      screen.getByRole("button", { name: "Aprovar limite de preparação" }),
    );
    await screen.findByText(/Decisão registrada com recibo/);
    const calls = vi.mocked(axios.post).mock.calls;
    expect(calls).toHaveLength(2);
    expect(calls[0][1]).toEqual(calls[1][1]);
    expect((calls[0][1] as { requestKey: string }).requestKey).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i,
    );
    expect(calls[1][1]).toMatchObject({
      totalLimitUsd: 50,
      reserveUsd: 3.5,
      confirmed: true,
      experimentId: 11200,
      expectedRevision: 1,
    });
    expect(screen.getByRole("checkbox")).not.toBeChecked();
  },
);
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
  await screen.findByText("Ciclo encerrado");
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

it("explica três finalidades e mostra bloqueio dos anúncios sem sair da demonstração", async () => {
  mount();
  await screen.findByText(fixture.productName);
  expect(
    screen.getByRole("heading", { name: "Preparar o produto" }),
  ).toBeVisible();
  expect(
    screen.getByRole("heading", { name: "Produzir vídeos" }),
  ).toBeVisible();
  expect(screen.getByRole("heading", { name: "Pagar anúncios" })).toBeVisible();
  expect(screen.getByText(/Produzir o vídeo é uma despesa/)).toBeVisible();
  fireEvent.click(
    screen.getByRole("button", { name: "Ver decisão dos anúncios aqui" }),
  );
  expect(await screen.findByText(fixture.mediaDecision.reason)).toBeVisible();
  expect(screen.getByText(/ainda não definido neste ciclo/)).toBeVisible();
  expect(axios.get).toHaveBeenCalledTimes(1);
  expect(axios.post).not.toHaveBeenCalled();
});

it("aprova vídeos na mesma página pelo contrato próprio sem alterar preparação ou anúncios", async () => {
  vi.mocked(axios.get).mockImplementation(async (url) => ({
    data: String(url).endsWith("/video-budget") ? videoBudget : fixture,
  }));
  vi.mocked(axios.post).mockResolvedValue({ data: videoBudget });
  mount();
  await screen.findByText(fixture.productName);
  fireEvent.click(
    screen.getByRole("button", { name: "Ver decisão dos vídeos aqui" }),
  );
  await screen.findByRole("form", { name: "Autorizar teto dos vídeos" });
  fireEvent.change(screen.getByLabelText(/Teto total autorizado \(USD\)/), {
    target: { value: "12" },
  });
  fireEvent.change(screen.getByLabelText(/Responsável pela autorização/), {
    target: { value: "Teste local" },
  });
  fireEvent.change(
    screen.getByLabelText(/Objetivo e justificativa do investimento/),
    { target: { value: "Somente produção de dois vídeos" } },
  );
  fireEvent.click(screen.getByLabelText(/Autorizo este teto total somente/));
  fireEvent.click(
    screen.getByRole("button", { name: "Registrar teto autorizado" }),
  );
  await waitFor(() => expect(axios.post).toHaveBeenCalledTimes(1));
  expect(vi.mocked(axios.post).mock.calls[0][0]).toContain(
    "/products/700/1900/video-budget",
  );
  expect(vi.mocked(axios.post).mock.calls[0][1]).toMatchObject({
    budgetLimitUsd: "12",
    chainDefinitionId: 2700,
    experimentId: 11200,
  });
  expect(
    screen.getByRole("heading", {
      name: "Demonstração financeira",
    }),
  ).toBeVisible();
});

it("mostra na mesma tela o bloqueio de vídeos informado pelo backend", async () => {
  vi.mocked(axios.get).mockImplementation(async (url) => ({
    data: String(url).endsWith("/video-budget")
      ? {
          ...videoBudget,
          canAuthorize: false,
          blocker: "Conclua o planejamento antes de definir vídeos.",
        }
      : fixture,
  }));
  mount();
  await screen.findByText(fixture.productName);
  fireEvent.click(
    screen.getByRole("button", { name: "Ver decisão dos vídeos aqui" }),
  );
  expect(
    await screen.findByText("Conclua o planejamento antes de definir vídeos."),
  ).toBeVisible();
  expect(
    screen.queryByRole("form", { name: "Autorizar teto dos vídeos" }),
  ).not.toBeInTheDocument();
  expect(axios.post).not.toHaveBeenCalled();
});

it("reutiliza a autorização de mídia na mesma tela com limites e replay próprios", async () => {
  const view = {
    ...fixture,
    mediaDecision: { ...fixture.mediaDecision, available: true },
  };
  const cycle = {
    id: 1900,
    productId: 700,
    chainDefinitionId: 2700,
    experimentId: 11200,
    revision: 1,
    budgetLimitBrl: 89.95,
    authorizationReview: { dailyBudgetBrl: 17.99 },
    commands: [{ action: "COMPLETE", available: true }],
    commercialPreparation: null,
  };
  vi.mocked(axios.get).mockImplementation(async (url) => ({
    data: String(url).endsWith("/financial-review") ? view : [cycle],
  }));
  vi.mocked(axios.post).mockRejectedValue(new Error("Falha simulada"));
  mount();
  await screen.findByText(fixture.productName);
  fireEvent.click(
    screen.getByRole("button", { name: "Ver decisão dos anúncios aqui" }),
  );
  const submit = await screen.findByRole("button", {
    name: "Aprovar limite de anúncios",
  });
  fireEvent.click(submit);
  await screen.findByRole("alert");
  fireEvent.click(submit);
  await waitFor(() => expect(axios.post).toHaveBeenCalledTimes(2));
  const first = vi.mocked(axios.post).mock.calls[0];
  expect(first[0]).toContain("/products/700/1900/budget-authorization");
  expect(first[1]).toEqual(vi.mocked(axios.post).mock.calls[1][1]);
  fireEvent.change(screen.getByLabelText(/Orçamento total \(R\$\)/), {
    target: { value: "90" },
  });
  fireEvent.click(submit);
  await waitFor(() => expect(axios.post).toHaveBeenCalledTimes(3));
  expect(
    (vi.mocked(axios.post).mock.calls[2][1] as { requestKey: string })
      .requestKey,
  ).not.toEqual((first[1] as { requestKey: string }).requestKey);
});
