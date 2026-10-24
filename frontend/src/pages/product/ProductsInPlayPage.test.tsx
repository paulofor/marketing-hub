import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import axios from "axios";
import { afterEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import ProductsInPlayPage from "./ProductsInPlayPage";
import type { ProductsInPlay } from "../../api/product/useProductsInPlay";

vi.mock("axios");
const fixture: ProductsInPlay = {
  observedAt: "2026-10-24T12:00:00Z",
  total: 1,
  executing: 0,
  waiting: 1,
  blocked: 0,
  paused: 0,
  unavailable: 0,
  products: [
    {
      productId: 9007,
      productName: "Produto QA",
      commercialName: "Nome comercial QA",
      chainId: 26,
      cycleId: 9012,
      sourceReference: "experiment:90105",
      processDefinitionId: 77,
      runId: 9080,
      activityId: "economics",
      activityNumber: "5.1.5",
      activityName: "Validar custos, limites e margem",
      responsible: "Plutus",
      status: "AWAITING_DECISION",
      statusLabel: "Aguardando decisão",
      reason: "Defina a meta de contribuição.",
      lastReconciledAt: "2026-10-24T11:59:00Z",
      activityUrl:
        "/products/9007/value-chain-history/processes/77/activities?chainId=26&learningCycleId=9012&sourceReference=experiment:90105#activity-economics",
    },
  ],
};

function mount() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <ProductsInPlayPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
  return client;
}

describe("Produtos em play", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("mostra atividade e estado oficiais sem trocar espera por execução", async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: fixture });
    mount();
    expect(
      await screen.findByRole("heading", { name: "Produto QA" }),
    ).toBeVisible();
    expect(screen.getByText("Aguardando decisão")).toBeVisible();
    expect(screen.getByText("Validar custos, limites e margem")).toBeVisible();
    expect(
      screen.getByRole("link", { name: "Ver atividade de Produto QA" }),
    ).toHaveAttribute("href", fixture.products[0].activityUrl);
    expect(
      screen.getByText("Defina a meta de contribuição."),
    ).not.toBeVisible();
    fireEvent.click(screen.getByText("Entender o status"));
    expect(screen.getByText("Defina a meta de contribuição.")).toBeVisible();
    expect(axios.get).toHaveBeenCalledWith(
      "/api/products/operations/v1/in-play",
      expect.objectContaining({ signal: expect.anything(), timeout: 45000 }),
    );
    expect(axios.post).not.toHaveBeenCalled();
  });

  it("expõe carregamento e desabilita nova consulta enquanto a atual não termina", () => {
    vi.mocked(axios.get).mockReturnValue(new Promise(() => {}));
    const client = mount();
    expect(
      screen.getByText("Consultando as atividades dos produtos..."),
    ).toBeVisible();
    expect(
      screen.getByRole("button", { name: "Atualizando..." }),
    ).toBeDisabled();
    client.clear();
  });

  it("apresenta vazio sem iniciar trabalho", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: { ...fixture, total: 0, waiting: 0, products: [] },
    });
    mount();
    expect(
      await screen.findByRole("heading", { name: "Nenhum produto em play" }),
    ).toBeVisible();
    expect(
      screen.getByRole("link", { name: /Ver todos os produtos/ }),
    ).toHaveAttribute("href", "/products/all");
    expect(axios.post).not.toHaveBeenCalled();
  });

  it("mantém o último retrato datado quando a atualização falha", async () => {
    vi.mocked(axios.get)
      .mockResolvedValueOnce({ data: fixture })
      .mockRejectedValue(new Error("Falha simulada"));
    mount();
    await screen.findByRole("heading", { name: "Produto QA" });
    fireEvent.click(screen.getByRole("button", { name: "Atualizar" }));
    await waitFor(
      () =>
        expect(screen.getByRole("alert")).toHaveTextContent(
          "última consulta disponível",
        ),
      { timeout: 4000 },
    );
    expect(screen.getByRole("heading", { name: "Produto QA" })).toBeVisible();
    expect(document.querySelector("time")).toHaveAttribute(
      "datetime",
      fixture.observedAt,
    );
  });

  it("mostra falha total e estado desconhecido sem fabricar status", async () => {
    vi.mocked(axios.get).mockRejectedValue(new Error("Falha simulada"));
    mount();
    expect(
      await screen.findByRole("alert", {}, { timeout: 4000 }),
    ).toHaveTextContent("Não foi possível consultar");
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        ...fixture,
        unavailable: 1,
        products: [
          {
            ...fixture.products[0],
            status: "UNAVAILABLE",
            statusLabel: "Não confirmado",
          },
        ],
      },
    });
    fireEvent.click(screen.getByRole("button", { name: "Atualizar" }));
    expect(await screen.findByText("Não confirmado")).toBeVisible();
  });
});
