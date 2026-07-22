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
import { afterEach, beforeEach, describe, it, expect, vi } from "vitest";
import ProductListPage from "./ProductListPage";
import axios from "axios";

vi.mock("axios");

describe("ProductListPage", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it("renders product actions", async () => {
    (axios.get as any).mockResolvedValue({ data: [] });
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <BrowserRouter>
          <ProductListPage />
        </BrowserRouter>
      </QueryClientProvider>,
    );
    expect(await screen.findByText(/Novo Produto/)).toBeTruthy();
  });

  it("shows edit option for listed product", async () => {
    (axios.get as any).mockResolvedValue({
      data: [
        {
          id: 1,
          slug: "pde-anti-invisibilidade-profissional-7-dias",
          name: "PDE Anti-Invisibilidade Profissional",
          currentPriceBrl: 47,
        },
      ],
    });
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <BrowserRouter>
          <ProductListPage />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    const editLink = await screen.findByRole("link", { name: /Editar dados/i });
    expect(editLink).toHaveAttribute("href", "/products/1/edit");
    expect(
      await screen.findByText("PDE Anti-Invisibilidade Profissional"),
    ).toBeTruthy();
  });

  it("shows empty state when there are no products", async () => {
    (axios.get as any).mockResolvedValue({ data: [] });
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <BrowserRouter>
          <ProductListPage />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    expect(
      await screen.findByText(/Nenhum produto comercial cadastrado/i),
    ).toBeTruthy();
  });

  it("shows marketing definition links for listed product", async () => {
    (axios.get as any).mockResolvedValue({
      data: [
        {
          id: 1,
          slug: "metodo-musa-7-dias",
          name: "Método MUSA - Presença Elegante em 7 Dias",
        },
      ],
    });
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <BrowserRouter>
          <ProductListPage />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    const definitionLinks = await screen.findByText(
      /Links de definição do produto/i,
    );
    expect(definitionLinks).not.toHaveTextContent(/Editar dados/i);
    expect(definitionLinks).not.toHaveTextContent(/Vídeos de venda/i);

    const formattedLink = within(definitionLinks).getByRole("link", {
      name: /Definição formatada/i,
    });
    expect(formattedLink).toHaveAttribute(
      "href",
      "/api/products/public/metodo-musa-7-dias/marketing-definition",
    );

    const markdownLink = within(definitionLinks).getByRole("link", {
      name: /Markdown/i,
    });
    expect(markdownLink).toHaveAttribute(
      "href",
      "/api/products/public/metodo-musa-7-dias/marketing-definition.md",
    );
  });

  it("shows preview QA link that suppresses commercial metrics", async () => {
    (axios.get as any).mockResolvedValue({
      data: [
        {
          id: 1,
          slug: "metodo-musa-7-dias",
          name: "Método MUSA - Presença Elegante em 7 Dias",
          publicUrl: "https://clubemusa.com.br",
        },
      ],
    });
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <BrowserRouter>
          <ProductListPage />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    const previewLink = await screen.findByRole("link", {
      name: /Preview\/QA sem métricas/i,
    });

    expect(previewLink).toHaveAttribute(
      "href",
      "https://clubemusa.com.br/?mh_preview=qa&pde_analytics=off&utm_source=internal&utm_medium=qa&utm_campaign=metodo-musa-7-dias_preview_qa&utm_content=product_card",
    );
    expect(previewLink).toHaveAttribute("target", "_blank");
  });

  it("shows the PDE persuasive journey registered in the product contract", async () => {
    (axios.get as any).mockResolvedValue({
      data: [
        {
          id: 1,
          slug: "metodo-musa-7-dias",
          name: "Método MUSA - Presença Elegante em 7 Dias",
          pdeExperienceJson: JSON.stringify({
            persuasiveJourney: {
              version: "aida-interactive-v1",
              framework: "AIDA",
              steps: [
                {
                  stage: "interest",
                  aidaLabel: "Interesse",
                  trackedSectionId: "interactive_diagnostic",
                  commercialFunction:
                    "Levar a usuária a interagir com o diagnóstico.",
                },
              ],
            },
          }),
        },
      ],
    });
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <BrowserRouter>
          <ProductListPage />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(/Jornada persuasiva PDE/i)).toBeTruthy();
    expect(screen.getByText(/AIDA · aida-interactive-v1/i)).toBeTruthy();
    expect(screen.getByText(/Levar a usuária a interagir/i)).toBeTruthy();
    expect(screen.getByText(/seção: interactive_diagnostic/i)).toBeTruthy();
  });

  it("calls backend to insert the default PDE persuasive journey", async () => {
    (axios.get as any).mockResolvedValue({
      data: [
        {
          id: 1,
          slug: "metodo-musa-7-dias",
          name: "Método MUSA - Presença Elegante em 7 Dias",
        },
      ],
    });
    (axios.post as any).mockResolvedValue({
      data: {
        id: 1,
        slug: "metodo-musa-7-dias",
        name: "Método MUSA - Presença Elegante em 7 Dias",
      },
    });
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <BrowserRouter>
          <ProductListPage />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    fireEvent.click(
      await screen.findByRole("button", { name: /Inserir jornada PDE/i }),
    );

    await waitFor(() => {
      expect(axios.post).toHaveBeenCalledWith(
        "/api/products/1/pde-persuasive-journey/default",
      );
    });
  });
});
