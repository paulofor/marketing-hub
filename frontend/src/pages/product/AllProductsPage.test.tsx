import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import axios from "axios";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import AllProductsPage from "./AllProductsPage";
import type { Product } from "../../api/product/useProducts";

vi.mock("axios");

function createProduct(
  id: number,
  internalName: string,
  status: "PLAY" | "STOP",
  name = `${internalName} — nome comercial`,
): Product {
  return {
    id,
    internalName,
    name,
    slug: internalName.toLocaleLowerCase("pt-BR"),
    commercialStatus: "VALIDACAO_COMERCIAL",
    automaticExecutionEnabled: status === "PLAY",
    automaticExecutionStatus: status,
    niche: "Nicho local",
    avatar: "Avatar local",
    explicitPain: "Dor local",
    promise: "Resultado local",
    uniqueMechanism: "Mecanismo local",
    tripwire: "",
    riskReversal: "",
    socialProof: "",
    checkoutMonetization: "",
    funnel: "",
    creativeVolume: "",
    storytelling: "",
    aiCost: 0,
  };
}

function renderPage() {
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <AllProductsPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("AllProductsPage", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it("lista PLAY e STOP em ordem alfabética pelo nome interno", async () => {
    (axios.get as ReturnType<typeof vi.fn>).mockResolvedValue({
      data: [
        createProduct(4, "Vega", "PLAY"),
        createProduct(8, "Ágata", "STOP"),
        createProduct(10, "Mira", "STOP"),
      ],
    });

    const view = renderPage();

    expect(
      await screen.findByRole("heading", { name: "Todos os produtos" }),
    ).toBeInTheDocument();
    expect(view.container.querySelector("main")).toBeNull();
    const productHeadings = await screen.findAllByRole("heading", { level: 2 });
    expect(productHeadings.map((heading) => heading.textContent)).toEqual([
      "Ágata",
      "Mira",
      "Vega",
    ]);
    expect(axios.get).toHaveBeenCalledWith("/api/products");

    const vegaItem = screen
      .getByRole("heading", { name: "Vega" })
      .closest("article");
    expect(vegaItem).not.toBeNull();
    expect(within(vegaItem!).getByText("PLAY")).toBeInTheDocument();
    expect(within(vegaItem!).getByText("Vega — nome comercial")).toBeVisible();
    expect(within(vegaItem!).getByText("ID 4 · vega")).toBeVisible();
    expect(
      within(vegaItem!).getByRole("link", { name: "Editar" }),
    ).toHaveAttribute("href", "/products/4/edit");
    expect(
      screen.getByRole("link", { name: "Ver operação em PLAY" }),
    ).toHaveAttribute("href", "/products");
  });

  it.each([
    { currentStatus: "PLAY" as const, nextStatus: "STOP" as const },
    { currentStatus: "STOP" as const, nextStatus: "PLAY" as const },
  ])(
    "altera $currentStatus para $nextStatus e recarrega a verdade do backend",
    async ({ currentStatus, nextStatus }) => {
      let product = createProduct(4, "Vega", currentStatus);
      let finishRequest:
        | ((response: {
            data: {
              productId: number;
              automaticExecutionEnabled: boolean;
              automaticExecutionStatus: "PLAY" | "STOP";
            };
          }) => void)
        | undefined;

      (axios.get as ReturnType<typeof vi.fn>).mockImplementation(() =>
        Promise.resolve({ data: [product] }),
      );
      (axios.put as ReturnType<typeof vi.fn>).mockImplementation(
        () =>
          new Promise((resolve) => {
            finishRequest = resolve;
          }),
      );
      renderPage();

      const command = await screen.findByRole("button", {
        name: `Colocar Vega em ${nextStatus}`,
      });
      fireEvent.click(command);

      expect(command).toBeDisabled();
      expect(screen.getByText("Alterando...")).toBeVisible();
      await waitFor(() =>
        expect(axios.put).toHaveBeenCalledWith(
          "/api/products/4/automatic-execution",
          { automaticExecutionEnabled: nextStatus === "PLAY" },
        ),
      );

      product = createProduct(4, "Vega", nextStatus);
      await act(async () => {
        finishRequest?.({
          data: {
            productId: 4,
            automaticExecutionEnabled: nextStatus === "PLAY",
            automaticExecutionStatus: nextStatus,
          },
        });
      });

      expect(
        await screen.findByText(`Vega agora está em ${nextStatus}.`),
      ).toBeVisible();
      expect(
        screen.getByRole("button", {
          name: `Colocar Vega em ${currentStatus}`,
        }),
      ).toBeEnabled();
      expect(
        (axios.get as ReturnType<typeof vi.fn>).mock.calls.length,
      ).toBeGreaterThan(1);
    },
  );

  it("pesquisa pela identidade usando o filtro canônico do backend", async () => {
    (axios.get as ReturnType<typeof vi.fn>).mockResolvedValue({
      data: [createProduct(10, "Mira", "STOP")],
    });
    renderPage();
    await screen.findByRole("heading", { name: "Mira" });

    fireEvent.change(
      screen.getByLabelText("Localizar produto por qualquer nome"),
      { target: { value: "Mira" } },
    );

    await waitFor(() =>
      expect(axios.get).toHaveBeenCalledWith("/api/products", {
        params: { query: "Mira" },
      }),
    );
  });

  it("preserva o estado anterior e explica uma falha no comando", async () => {
    (axios.get as ReturnType<typeof vi.fn>).mockResolvedValue({
      data: [createProduct(4, "Vega", "PLAY")],
    });
    (axios.put as ReturnType<typeof vi.fn>).mockRejectedValue(
      new Error("falha local simulada"),
    );
    renderPage();

    fireEvent.click(
      await screen.findByRole("button", { name: "Colocar Vega em STOP" }),
    );

    expect(
      await screen.findByText(
        "Não foi possível alterar Vega. O estado anterior foi preservado.",
      ),
    ).toBeVisible();
    expect(screen.getByText("PLAY")).toBeVisible();
    expect(
      screen.getByRole("button", { name: "Colocar Vega em STOP" }),
    ).toBeEnabled();
  });

  it("explica a falha de carregamento e permite tentar novamente", async () => {
    (axios.get as ReturnType<typeof vi.fn>)
      .mockRejectedValueOnce(new Error("falha local simulada"))
      .mockResolvedValueOnce({ data: [createProduct(10, "Mira", "STOP")] });
    renderPage();

    expect(
      await screen.findByText(
        "Não foi possível carregar os produtos. Nenhum estado foi alterado.",
      ),
    ).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Tentar novamente" }));

    expect(await screen.findByRole("heading", { name: "Mira" })).toBeVisible();
    expect(axios.get).toHaveBeenCalledTimes(2);
  });
});
