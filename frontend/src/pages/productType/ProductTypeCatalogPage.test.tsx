import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import ProductTypeCatalogPage from "./ProductTypeCatalogPage";

vi.mock("axios");

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <BrowserRouter>
        <ProductTypeCatalogPage />
      </BrowserRouter>
    </QueryClientProvider>,
  );
}

describe("ProductTypeCatalogPage", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });
  afterEach(cleanup);

  it("shows canonical types, aliases and product usage", async () => {
    (axios.get as any).mockResolvedValue({
      data: [
        {
          id: 1,
          code: "PDE",
          name: "PDE - Produto Digital Experiencial",
          internalName: "Opala",
          description: "Jornada de valor observável.",
          aliases: ["PDE", "Experiência guiada"],
          status: "ACTIVE",
          productCount: 4,
        },
      ],
    });

    renderPage();

    expect(
      await screen.findByText("PDE - Produto Digital Experiencial"),
    ).toBeTruthy();
    expect(screen.getByText("Nome interno: Opala")).toBeTruthy();
    expect(screen.getByText("4")).toBeTruthy();
    const aliases = screen.getByLabelText(
      "Apelidos de PDE - Produto Digital Experiencial",
    );
    expect(within(aliases).getByText("Experiência guiada")).toBeTruthy();
    expect(screen.getAllByText("Em uso").length).toBeGreaterThan(0);
  });

  it("creates a proposed type without limiting the new idea", async () => {
    (axios.get as any).mockResolvedValue({ data: [] });
    (axios.post as any).mockResolvedValue({
      data: {
        id: 12,
        code: "EXPERIENCIA_IMERSIVA",
        name: "Experiência imersiva",
        internalName: "Granada",
        aliases: ["Produto imersivo"],
        status: "PROPOSED",
        productCount: 0,
      },
    });
    renderPage();
    await screen.findByText("Nenhum tipo corresponde à busca.");

    fireEvent.change(screen.getByLabelText("Nome canônico *"), {
      target: { value: "Experiência imersiva" },
    });
    fireEvent.change(screen.getByLabelText("Nome interno (mineral) *"), {
      target: { value: "Granada" },
    });
    fireEvent.change(screen.getByLabelText("Apelidos internos"), {
      target: { value: "Produto imersivo" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Cadastrar tipo" }));

    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith("/api/product-types", {
        code: undefined,
        name: "Experiência imersiva",
        internalName: "Granada",
        description: undefined,
        aliases: ["Produto imersivo"],
        status: "PROPOSED",
      }),
    );
  });

  it("shows a useful failure when the catalog cannot be loaded", async () => {
    (axios.get as any).mockRejectedValue(new Error("offline"));
    renderPage();

    expect(
      await screen.findByText("Não foi possível carregar o catálogo de tipos."),
    ).toBeTruthy();
  });
});
