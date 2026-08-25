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
    expect(
      screen.getByRole("link", { name: "Tipos de produto" }),
    ).toHaveAttribute("href", "/product-types");
  });

  it("shows edit option for listed product", async () => {
    (axios.get as any).mockResolvedValue({
      data: [
        {
          id: 1,
          slug: "pde-anti-invisibilidade-profissional-7-dias",
          name: "PDE Anti-Invisibilidade Profissional",
          currentPriceBrl: 47,
          productType: "PDE - Produto Digital Experiencial",
          productTypeCode: "PDE",
          productTypeInternalName: "Opala",
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
      screen.getByRole("link", { name: "Histórico da cadeia" }),
    ).toHaveAttribute("href", "/products/1/value-chain-history");
    expect(
      await screen.findByText("PDE Anti-Invisibilidade Profissional"),
    ).toBeTruthy();
    expect(screen.getByText("PDE - Produto Digital Experiencial")).toBeTruthy();
    expect(screen.getByText("PDE")).toBeTruthy();
    expect(screen.getByText("Família interna: Opala")).toBeTruthy();
  });

  it("shows and searches the internal identity without replacing the commercial name", async () => {
    (axios.get as any).mockImplementation(
      (url: string, config?: { params?: { query?: string } }) => {
        if (url === "/api/products/value-chain-positions") {
          return Promise.resolve({ data: [] });
        }
        return Promise.resolve({
          data: [
            {
              id: 4,
              slug: "metodo-musa-7-dias",
              name: "Método MUSA - Presença Elegante em 7 Dias",
              internalName: "MUSA desejo v7",
              aliases: ["MUSA v7", "Vídeos orientados ao desejo"],
              commercialStatus: "VALIDACAO_COMERCIAL",
              queryReceived: config?.params?.query,
            },
          ],
        });
      },
    );
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <BrowserRouter>
          <ProductListPage />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    expect(
      await screen.findByRole("heading", {
        name: "Método MUSA - Presença Elegante em 7 Dias",
      }),
    ).toBeTruthy();
    expect(screen.getByText("MUSA desejo v7")).toBeTruthy();
    const aliases = screen.getByLabelText(
      "Apelidos internos de Método MUSA - Presença Elegante em 7 Dias",
    );
    expect(within(aliases).getByText("MUSA v7")).toBeTruthy();
    expect(
      within(aliases).getByText("Vídeos orientados ao desejo"),
    ).toBeTruthy();

    fireEvent.change(
      screen.getByLabelText("Localizar produto por qualquer nome"),
      { target: { value: "MUSA v7" } },
    );

    await waitFor(() =>
      expect(axios.get).toHaveBeenCalledWith("/api/products", {
        params: { query: "MUSA v7" },
      }),
    );
  });

  it("shows the product position in the published value chain", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/products/value-chain-positions") {
        return Promise.resolve({
          data: [
            {
              productId: 9,
              commercialStatus: "COMUNICACAO_E_JORNADA",
              resolutionStatus: "IDENTIFIED",
              resolutionMessage: "Posição identificada.",
              chainDefinitionId: 5,
              chainName:
                "Criação e entrega de valor de Produtos Digitais Experienciais",
              chainVersion: 5,
              processDefinitionId: 43,
              processCode: "pde-communication-sales-journey",
              processName: "Comunicação e jornada de venda do PDE",
              processVersion: 4,
              sequenceNumber: 4,
              processCount: 6,
            },
          ],
        });
      }
      return Promise.resolve({
        data: [
          {
            id: 9,
            slug: "kit-whatsapp-pronto",
            name: "Kit WhatsApp Pronto",
            commercialStatus: "COMUNICACAO_E_JORNADA",
          },
        ],
      });
    });
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <BrowserRouter>
          <ProductListPage />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    expect(await screen.findByText("Etapa 4 de 6")).toBeTruthy();
    expect(
      screen.getByRole("link", {
        name: /Comunicação e jornada de venda do PDE/i,
      }),
    ).toHaveAttribute("href", "/business-processes?processId=43");
    expect(
      screen.getByText("Status comercial: Comunicação e jornada"),
    ).toBeTruthy();
  });

  it("shows product video images action for listed product", async () => {
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

    const videoImagesLink = await screen.findByRole("link", {
      name: /Imagens Para Vídeos/i,
    });
    expect(videoImagesLink).toHaveAttribute("href", "/products/1/video-images");

    const pdeVideosLink = screen.getByRole("link", {
      name: /Vídeos HLS/i,
    });
    expect(pdeVideosLink).toHaveAttribute("href", "/products/1/pde-videos");

    const financialLink = screen.getByRole("link", {
      name: /Financeiro/i,
    });
    expect(financialLink).toHaveAttribute("href", "/products/1/financial");

    const adsLink = screen.getByRole("link", {
      name: /Anúncios/i,
    });
    expect(adsLink).toHaveAttribute("href", "/products/1/ads");

    const scientificArticlesLink = screen.getByRole("link", {
      name: /Artigos científicos/i,
    });
    expect(scientificArticlesLink).toHaveAttribute(
      "href",
      "/products/1/scientific-articles",
    );

    const comparisonLink = screen.getByRole("link", {
      name: /Comparar experimentos/i,
    });
    expect(comparisonLink).toHaveAttribute(
      "href",
      "/products/1/experiment-comparison",
    );
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

  it("prioritizes products in commercial validation by recent activity", async () => {
    (axios.get as any).mockResolvedValue({
      data: [
        {
          id: 1,
          slug: "produto-estavel",
          name: "Produto Estável",
          commercialStatus: "ESCALA",
          updatedAt: "2026-07-26T04:00:00Z",
        },
        {
          id: 2,
          slug: "validacao-antiga",
          name: "Validação Antiga",
          commercialStatus: "VALIDACAO_COMERCIAL",
          associatedExperiments: "Experimento 10",
          updatedAt: "2026-07-24T04:00:00Z",
        },
        {
          id: 3,
          slug: "validacao-ativa",
          name: "Validação Ativa",
          commercialStatus: "VALIDACAO_COMERCIAL",
          associatedExperiments: "Experimento 11; Experimento 12",
          updatedAt: "2026-07-26T03:00:00Z",
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

    const cards = await screen.findAllByRole("heading", { level: 2 });
    expect(cards.map((card) => card.textContent)).toEqual([
      "Validação Ativa",
      "Validação Antiga",
      "Produto Estável",
    ]);
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
              version: "commercial-stages-v1",
              framework: "Funil experiencial PDE",
              steps: [
                {
                  stageNumber: 2,
                  stage: "diagnostic_value",
                  stageName: "Envolvimento diagnóstico",
                  psychologicalRole: "Interesse + Desejo",
                  trackedSectionIds: [
                    "interactive_diagnostic",
                    "free_diagnostic_preview",
                  ],
                  commercialFunction:
                    "Questionário e plano de 7 dias aumentam valor percebido.",
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

    expect(await screen.findByText(/Jornada comercial PDE/i)).toBeTruthy();
    expect(
      screen.getByText(/Funil experiencial PDE · commercial-stages-v1/i),
    ).toBeTruthy();
    expect(
      screen.getByText(/Estágio 2: Envolvimento diagnóstico/i),
    ).toBeTruthy();
    expect(screen.getByText(/Questionário e plano de 7 dias/i)).toBeTruthy();
    expect(
      screen.getByText(
        /seções: interactive_diagnostic, free_diagnostic_preview/i,
      ),
    ).toBeTruthy();
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
