import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, cleanup } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import HomePage from "./HomePage";

vi.mock("axios");

function renderHome() {
  const client = new QueryClient();
  return render(
    <QueryClientProvider client={client}>
      <BrowserRouter>
        <HomePage />
      </BrowserRouter>
    </QueryClientProvider>,
  );
}

describe("HomePage", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it("shows active product cards from products endpoint", async () => {
    (axios.get as any).mockResolvedValue({
      data: [
        {
          id: 1,
          slug: "metodo-musa-7-dias",
          name: "Metodo MUSA",
          commercialStatus: "VALIDACAO_COMERCIAL",
          currentPriceBrl: 27,
          targetAudience: "Mulheres que querem melhorar a presença visual",
          primaryHypothesis:
            "Diagnóstico de presença elegante em poucos minutos",
          primaryCta: "Começar diagnóstico",
          colorPalette: "#7a2444, #d6a75c",
          updatedAt: "2026-07-28T01:00:00Z",
        },
        {
          id: 2,
          slug: "produto-pausado",
          name: "Produto Pausado",
          commercialStatus: "PAUSED",
          updatedAt: "2026-07-28T02:00:00Z",
        },
      ],
    });

    renderHome();

    expect(await screen.findByText("Metodo MUSA")).toBeTruthy();
    expect(screen.queryByText("Produto Pausado")).toBeNull();
    expect(screen.getByText("R$ 27,00")).toBeTruthy();
    expect(screen.getByText(/Diagnóstico de presença elegante/i)).toBeTruthy();
    expect(screen.getByRole("link", { name: /Vídeos/i })).toHaveAttribute(
      "href",
      "/products/1/sales-videos",
    );
  });

  it("shows the current value-chain process as a human-readable link", async () => {
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

    renderHome();

    expect(await screen.findByText("Etapa 4 de 6")).toBeTruthy();
    expect(
      screen.getByRole("link", {
        name: /Comunicação e jornada de venda do PDE/i,
      }),
    ).toHaveAttribute("href", "/business-processes?processId=43");
    expect(screen.getByText("Status: Comunicação e jornada")).toBeTruthy();
  });

  it("shows empty state when there are no active products", async () => {
    (axios.get as any).mockResolvedValue({
      data: [{ id: 1, name: "Produto Inativo", commercialStatus: "INATIVO" }],
    });

    renderHome();

    expect(
      await screen.findByText(/Nenhum produto ativo encontrado/i),
    ).toBeTruthy();
  });
});
