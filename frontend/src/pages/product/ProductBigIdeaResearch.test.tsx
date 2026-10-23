import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, expect, it, vi } from "vitest";
import axios from "axios";
import ProductBigIdeaResearch from "./ProductBigIdeaResearch";
vi.mock("axios");
afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});

function mount(id = 99101) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <ProductBigIdeaResearch productId={id} />
    </QueryClientProvider>,
  );
  return client;
}

it("requests once, follows backend state and adopts only the completed proposal", async () => {
  let state: any = {
    status: "NOT_REQUESTED",
    canRequest: true,
    canAdopt: false,
    guidance: "Pesquisa independente",
  };
  vi.mocked(axios.get).mockImplementation(async () => ({ data: state }));
  vi.mocked(axios.post).mockImplementation(async () => {
    state = {
      status: "PENDING",
      canRequest: false,
      canAdopt: false,
      executionId: 880,
      guidance: "Atena acompanha",
    };
    return { data: state };
  });
  const client = mount();
  fireEvent.change(
    await screen.findByLabelText("Orientação para Atena (opcional)"),
    { target: { value: "Fontes públicas" } },
  );
  fireEvent.click(
    screen.getByRole("button", { name: "Pedir Big Idea a Atena" }),
  );
  await screen.findByText(/Na fila de Atena/);
  expect(axios.post).toHaveBeenCalledTimes(1);
  expect(axios.post).toHaveBeenCalledWith(
    "/api/products/99101/big-idea/research",
    { brief: "Fontes públicas" },
  );
  expect(
    screen.queryByRole("button", { name: "Aplicar Big Idea de Atena" }),
  ).toBeNull();
  state = {
    status: "COMPLETED",
    canRequest: false,
    canAdopt: true,
    executionId: 880,
    guidance: "Proposta concluída",
    estimatedCostUsd: 0.2,
    modelName: "gpt-6-astra",
    proposal: {
      bigIdea: { centralIdea: "Uma ocasião, uma decisão simples" },
      selectionReason: "Mecanismo plausível",
      demandAssessment: { validationPlan: "Medir compras reais" },
      alternatives: [],
      sources: [],
    },
  };
  await client.invalidateQueries();
  fireEvent.click(
    await screen.findByRole("button", { name: "Aplicar Big Idea de Atena" }),
  );
  await waitFor(() =>
    expect(axios.post).toHaveBeenLastCalledWith(
      "/api/products/99101/big-idea/research/880/adopt",
    ),
  );
});

it("preserves failed research without inventing retry permission or commercial approval", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: {
      status: "FAILED",
      canRequest: false,
      canAdopt: false,
      executionId: 881,
      guidance: "Recibo preservado para diagnóstico",
    },
  });
  mount(99288);
  await screen.findByText(/Pesquisa interrompida/);
  expect(
    screen.queryByRole("button", { name: "Pedir Big Idea a Atena" }),
  ).toBeNull();
  expect(
    screen.queryByRole("button", { name: "Aplicar Big Idea de Atena" }),
  ).toBeNull();
  expect(axios.post).not.toHaveBeenCalled();
});
