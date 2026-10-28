import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, it, expect, vi } from "vitest";
import axios from "axios";
import ChatgptAdsPage from "./ChatgptAdsPage";
vi.mock("axios");
const draft = {
  experimentId: 901,
  creativeId: 902,
  name: "Piloto sintético",
  title: "Seu trabalho merece destaque",
  body: "Veja uma amostra pronta para usar.",
  imageUrl: "https://example.org/a.png",
  targetUrl: "https://example.org/sample",
  contextHints: ["Divulgar a semana"],
  currency: "BRL",
  totalBudget: 25,
  maxCpc: 1,
  startsAt: "2030-01-01T00:00:00Z",
  endsAt: "2030-01-06T00:00:00Z",
};
const campaign = {
  id: 903,
  experimentId: 901,
  productName: "Produto sintético",
  status: "DRAFT",
  draft,
  fingerprint: "snapshot",
  authorized: false,
  blockers: ["Homologação pendente."],
  canPublish: false,
  canActivate: false,
  canPause: false,
  remote: {},
  metrics: {},
  updatedAt: "2030-01-01T00:00:00Z",
};
function mount(
  path = "/chatgpt-ads",
  experimentIdentity: string | number = "901",
) {
  vi.mocked(axios.get).mockImplementation(async (url) => ({
    data: String(url).includes("chatgpt-ads")
      ? {
          connection: {
            status: "active",
            name: "Conta teste",
            currency_code: "BRL",
          },
          campaigns: [campaign],
        }
      : String(url) === "/api/experiments"
        ? [
            {
              id: experimentIdentity,
              name: "Experimento correto",
              status: "PLANNED",
              platform: "CHATGPT_ADS",
            },
            {
              id: "995",
              name: "Outro contexto",
              status: "PLANNED",
              platform: "CHATGPT_ADS",
            },
          ]
        : [],
  }));
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[path]}>
        <ChatgptAdsPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
  return client;
}
afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});
describe("ChatGPT Ads", () => {
  it("preserva o experimento ao abrir a operação a partir de seu relatório", async () => {
    const client = mount("/chatgpt-ads?experimentId=901");
    await screen.findByText("Piloto sintético");
    expect(axios.get).toHaveBeenCalledWith("/api/chatgpt-ads/v1", {
      params: { experimentId: 901 },
    });
    expect(axios.post).not.toHaveBeenCalled();
    client.clear();
  });
  it.each(["901", 901])(
    "abre o experimento %s usando o formato de ID recebido do cadastro",
    async (id) => {
      const client = mount("/chatgpt-ads?experimentId=901", id);
      await screen.findByRole("option", { name: "Experimento correto · #901" });
      expect(
        screen.queryByRole("option", { name: /Outro contexto/ }),
      ).not.toBeInTheDocument();
      expect(screen.getByLabelText("Experimento *")).toHaveValue("901");
      client.clear();
    },
  );
  it("mostra pendências e nunca transforma ausência de medição em vendas", async () => {
    const c = mount();
    await screen.findByText("Piloto sintético");
    expect(
      screen.getByRole("button", { name: "Ativar teste autorizado" }),
    ).toBeDisabled();
    expect(
      screen.getByRole("button", { name: "Publicar pausada" }),
    ).toBeDisabled();
    fireEvent.click(screen.getByText("O que falta para avançar (1)"));
    expect(screen.getByText("Homologação pendente.")).toBeInTheDocument();
    expect(screen.getAllByText("—")).toHaveLength(3);
    expect(axios.post).not.toHaveBeenCalled();
    c.clear();
  });
  it("exige confirmação explícita do operador para o teto próprio do canal", async () => {
    vi.mocked(axios.post).mockResolvedValue({ data: campaign });
    const c = mount();
    fireEvent.click(
      await screen.findByRole("button", { name: "Conferir autorização" }),
    );
    const submit = screen.getByRole("button", {
      name: "Registrar autorização",
    });
    expect(submit).toBeDisabled();
    fireEvent.change(screen.getByLabelText("Responsável *"), {
      target: { value: "Operadora QA" },
    });
    fireEvent.click(screen.getByRole("checkbox"));
    fireEvent.click(submit);
    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith(
        "/api/chatgpt-ads/v1/campaigns/903/authorize",
        {
          fingerprint: "snapshot",
          decidedBy: "Operadora QA",
          confirmation: "AUTORIZO CHATGPT ADS",
        },
      ),
    );
    c.clear();
  });
});
