import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, cleanup } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import WinningAdsLibraryPage from "./WinningAdsLibraryPage";

vi.mock("axios");

function renderPage() {
  const client = new QueryClient();
  return render(
    <QueryClientProvider client={client}>
      <WinningAdsLibraryPage />
    </QueryClientProvider>,
  );
}

describe("WinningAdsLibraryPage", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it("shows Costure e Venda pilot ads from backend truth", async () => {
    (axios.get as any).mockResolvedValue({
      data: {
        total: 1,
        items: [
          {
            id: 1,
            productSlug: "costure-e-venda",
            productName: "Costure e Venda",
            niche: "Costureiras autônomas",
            funnelStage: "AQUISICAO",
            channel: "META_ADS",
            format: "CARROSSEL",
            winningStatus: "PILOTO",
            score: 94,
            hook: "Sua costura está boa, mas seus pedidos ainda chegam no improviso?",
            primaryText: "Texto principal do anúncio.",
            creativeBrief: "Carrossel com WhatsApp e agenda.",
            offerAngle: "Organização de vendas.",
            proofSignal: "Prova visual de conversa estruturada.",
            metricSnapshot: "Piloto sem métrica real ainda.",
            learning: "Dor concreta supera promessa genérica.",
            nextAction: "Testar em Meta Ads.",
            sourceReference: "piloto-costure-e-venda",
            updatedAt: "2026-08-02T00:00:00Z",
          },
        ],
      },
    });

    renderPage();

    expect(
      await screen.findByText("Biblioteca de Anúncios Vencedores"),
    ).toBeTruthy();
    expect(
      await screen.findByText("Costure e Venda · CARROSSEL · AQUISICAO"),
    ).toBeTruthy();
    expect(screen.getAllByText("94")).toHaveLength(2);
    expect(screen.getByText("Dor concreta supera promessa genérica.")).toBeTruthy();
    expect(axios.get).toHaveBeenCalledWith("/api/winning-ads-library", {
      params: { productSlug: "costure-e-venda" },
    });
  });
});
