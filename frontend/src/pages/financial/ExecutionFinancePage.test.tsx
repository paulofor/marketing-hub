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
  expect(screen.getByText(/Parecer simulado/)).toBeVisible();
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
    },
  });
  mount();
  await screen.findByText("Ciclo encerrado");
  expect(
    screen.queryByRole("button", { name: "Aprovar limite de preparação" }),
  ).not.toBeInTheDocument();
  expect(screen.getByText(/Não foi localizada uma revisão/)).toBeVisible();
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
