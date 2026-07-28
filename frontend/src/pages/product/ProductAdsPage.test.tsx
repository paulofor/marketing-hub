import { cleanup, render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import ProductAdsPage from "./ProductAdsPage";

vi.mock("axios");

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  window.history.pushState({}, "", "/products/1/ads");
  return render(
    <QueryClientProvider client={client}>
      <BrowserRouter>
        <Routes>
          <Route path="/products/:productId/ads" element={<ProductAdsPage />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>,
  );
}

describe("ProductAdsPage", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it("shows reusable ads from the product endpoint", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/products/1/ads") {
        return Promise.resolve({
          data: {
            productId: 1,
            productName: "Método MUSA",
            productSlug: "metodo-musa-7-dias",
            commercialStatus: "VALIDACAO_COMERCIAL",
            mainRecommendation:
              "Priorize anúncios prontos como controle criativo.",
            ads: [
              {
                creativeId: 12,
                experimentId: 74,
                experimentName: "MUSA-H001-E009",
                experimentStatus: "RUNNING",
                format: "IMAGE",
                status: "READY",
                headline: "Elegância visível em 7 dias",
                primaryText:
                  "Descubra quais escolhas deixam sua presença mais elegante.",
                cta: "LEARN_MORE",
                destinationUrl: "https://clubemusa.com.br",
                imageUrl: "https://cdn.example.com/musa-ad.png",
                reuseRecommendation:
                  "Pode ser reaproveitado em novos experimentos.",
                reviewedAt: "2026-07-28T12:00:00Z",
              },
            ],
          },
        });
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`));
    });

    renderPage();

    expect(await screen.findByText("Elegância visível em 7 dias")).toBeTruthy();
    expect(screen.getByText(/Priorize anúncios prontos/i)).toBeTruthy();
    expect(screen.getByText(/Experimento #74/i)).toBeTruthy();
    expect(screen.getByRole("link", { name: /Abrir imagem/i })).toHaveAttribute(
      "href",
      "https://cdn.example.com/musa-ad.png",
    );
  });
});
