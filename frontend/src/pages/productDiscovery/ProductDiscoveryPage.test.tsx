import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import ProductDiscoveryPage from "./ProductDiscoveryPage";

const fetchMock = vi.fn();

describe("ProductDiscoveryPage", () => {
  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it("shows the maturity ranking from the backend and creates a recommended cycle", async () => {
    fetchMock.mockImplementation(async (url: string, options?: RequestInit) => {
      if (url.includes("/api/product-discovery/v1/maturity-ranking")) {
        return {
          ok: true,
          json: async () => ({
            strategyName: "Ranking por maturidade comercial",
            decisionCriterion:
              "Dor concreta, lacuna clara e microexperiência rápida.",
            recommendedPriority: "Começar por renda extra.",
            items: [
              {
                position: 1,
                niche: "Renda extra",
                maturity: "Oportunidade promissora",
                summary: "Mercado grande.",
                commercialReason: "Dor urgente sem promessa garantida.",
                recommendedAction: "Abrir ciclo de pesquisa.",
                evidence: ["Encaixe com WhatsApp"],
                guardrails: ["Sem ganho garantido"],
              },
            ],
            recommendedTracks: [
              {
                name: "Renda extra para autônomos/MEIs",
                focus: "WhatsApp e primeira venda.",
                reason: "Maior chance de compra rápida.",
                theme: "renda extra para autonomos e MEIs",
                targetAudience: "Autônomos e MEIs",
                acquisitionChannel: "TikTok, Reels e WhatsApp",
                objective: "Encontrar dor concreta.",
                commercialConstraints: "Baixo esforço.",
                forbiddenCategories: "Promessa de renda garantida.",
              },
            ],
          }),
        } as Response;
      }

      if (
        url.includes("/api/product-discovery/v1/cycles") &&
        options?.method === "POST"
      ) {
        return {
          ok: true,
          json: async () => ({
            id: 10,
            theme: "renda extra para autonomos e MEIs",
            country: "BR",
            language: "pt-BR",
            status: "READY_FOR_RESEARCH",
            stageCode: "research",
            createdAt: "2026-07-26T00:00:00Z",
            updatedAt: "2026-07-26T00:00:00Z",
          }),
        } as Response;
      }

      return {
        ok: true,
        json: async () => [],
      } as Response;
    });

    const client = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    });

    render(
      <QueryClientProvider client={client}>
        <BrowserRouter>
          <ProductDiscoveryPage />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    expect(
      await screen.findByRole("heading", {
        name: /Ranking de maturidade comercial/i,
      }),
    ).toBeTruthy();
    expect(
      await screen.findByRole("heading", {
        name: /Top 10 produtos com mais chance de sucesso/i,
      }),
    ).toBeTruthy();
    expect((await screen.findAllByText("Renda extra")).length).toBeGreaterThan(
      0,
    );
    expect(screen.getAllByText("Oportunidade promissora").length).toBeGreaterThan(
      0,
    );

    await userEvent.click(screen.getByRole("button", { name: /Criar ciclo/i }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining("/api/product-discovery/v1/cycles"),
        expect.objectContaining({
          method: "POST",
          body: expect.stringContaining("renda extra para autonomos e MEIs"),
        }),
      );
    });
  });
});
