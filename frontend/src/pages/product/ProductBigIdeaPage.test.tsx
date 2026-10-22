import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, expect, it, vi } from "vitest";
import axios from "axios";
import ProductBigIdeaPage from "./ProductBigIdeaPage";
vi.mock("axios");
afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});
const content = {
  centralIdea: "Uma agenda real vira conteúdo pronto",
  audience: "Manicures",
  desire: "Menos esforço",
  promise: "Kit utilizável",
  mechanism: "Posts por ocasião",
  evidence: "Hipótese sem vendas",
  limits: "Sem garantia de clientes",
};
function mount() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={["/products/701/big-idea"]}>
        <Routes>
          <Route
            path="/products/:id/big-idea"
            element={<ProductBigIdeaPage />}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}
it("creates a complete first revision and shows persisted history", async () => {
  let workspace: any = {
    productId: 701,
    status: "MISSING",
    current: null,
    history: [],
    guidance: "Experimentos preservam a versão.",
  };
  vi.mocked(axios.get).mockImplementation(async (url) => ({
    data: String(url).endsWith("big-idea")
      ? workspace
      : { id: 701, internalName: "Produto sintético" },
  }));
  vi.mocked(axios.post).mockImplementation(async (_url, body: any) => {
    const revision = {
      ...body,
      id: 1,
      productId: 701,
      revision: 1,
      createdBy: "Operador",
      createdAt: "2026-10-22T12:00:00Z",
      sourceTaskId: null,
    };
    workspace = {
      ...workspace,
      status: "DEFINED",
      current: revision,
      history: [revision],
    };
    return { data: revision };
  });
  mount();
  await screen.findByText("Big Idea ainda não definida");
  const values = Object.values(content);
  screen
    .getAllByRole("textbox")
    .forEach((field, index) =>
      fireEvent.change(field, {
        target: { value: values[index] ?? "Direção inicial" },
      }),
    );
  fireEvent.click(screen.getByRole("button", { name: "Criar Big Idea" }));
  await screen.findByText("Versão vigente: v1");
  expect(axios.post).toHaveBeenCalledWith(
    "/api/products/701/big-idea/revisions",
    { expectedRevision: 0, content, changeReason: "Direção inicial" },
  );
  expect(screen.getByText("Histórico de versões (1)")).toBeTruthy();
});
it("keeps the draft visible on conflict and never reports it saved", async () => {
  vi.mocked(axios.get).mockImplementation(async (url) => ({
    data: String(url).endsWith("big-idea")
      ? {
          productId: 701,
          status: "DEFINED",
          current: { revision: 1, content },
          history: [],
          guidance: "Preservar.",
        }
      : { id: 701 },
  }));
  vi.mocked(axios.isAxiosError).mockReturnValue(true);
  vi.mocked(axios.post).mockRejectedValue({
    response: {
      status: 409,
      data: { detail: "A Big Idea mudou. Atualize a tela." },
    },
  });
  mount();
  await screen.findByText("Versão vigente: v1");
  fireEvent.change(screen.getByLabelText("Ideia central *"), {
    target: { value: "Nova direção" },
  });
  fireEvent.change(screen.getByLabelText("Motivo desta versão *"), {
    target: { value: "Aprendizado" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Salvar nova versão" }));
  await screen.findByRole("alert");
  expect(screen.getByLabelText("Ideia central *")).toHaveValue("Nova direção");
  expect(screen.queryByText("Versão vigente: v2")).toBeNull();
});
it("shows query failure without claiming that the product has no idea", async () => {
  vi.mocked(axios.get).mockRejectedValue(new Error("offline"));
  mount();
  await screen.findByText("Não foi possível consultar a Big Idea.");
  expect(screen.queryByText("Big Idea ainda não definida")).toBeNull();
});
