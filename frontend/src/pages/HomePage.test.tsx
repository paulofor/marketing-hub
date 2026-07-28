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
          primaryHypothesis: "Diagnóstico de presença elegante em poucos minutos",
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
