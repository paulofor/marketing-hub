import { render, screen, cleanup, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import React from "react";
import App from "../App";
import axios from "axios";

vi.mock("axios");

function setup(ui: React.ReactNode, initialEntries: string[]) {
  const client = new QueryClient();
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={initialEntries}>{ui}</MemoryRouter>
    </QueryClientProvider>,
  );
}

afterEach(() => {
  cleanup();
});

describe("pde video production navigation", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    (axios.get as any).mockResolvedValue({ data: [] });
  });

  it("has menu link to PDE video production", () => {
    setup(<App />, ["/"]);

    const link = screen.getByRole("link", {
      name: /produção de vídeo pde/i,
    });

    expect(link).toBeTruthy();
    expect(link.getAttribute("href")).toBe("/pde-video-production");
  });

  it("renders the PDE video production process page", () => {
    setup(<App />, ["/pde-video-production"]);

    expect(
      screen.getByRole("heading", { name: /produção de vídeo pde/i }),
    ).toBeTruthy();
    expect(screen.getByText(/cockpit por produto/i)).toBeTruthy();
    expect(screen.getByLabelText(/produto pde/i)).toBeTruthy();
    expect(
      screen.getByRole("heading", { name: /cockpit do produto pde/i }),
    ).toBeTruthy();
    expect(screen.getByText(/^Abertura$/i)).toBeTruthy();
    expect(screen.getByText(/^Prova$/i)).toBeTruthy();
    expect(screen.getByText(/^Mecanismo$/i)).toBeTruthy();
    expect(screen.getByText(/^Objecao$/i)).toBeTruthy();
    expect(screen.getByText(/^CTA$/i)).toBeTruthy();
    expect(
      screen.getByRole("heading", { name: /briefing comercial/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole("heading", { name: /roteiro e cenas/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole("heading", { name: /storyboard e prompts/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole("heading", { name: /geracao e variacoes/i }),
    ).toBeTruthy();
    expect(
      screen.getAllByRole("heading", { name: /qualidade comercial/i }).length,
    ).toBeGreaterThan(0);
    expect(
      screen.getByRole("heading", { name: /vinculo ao pde/i }),
    ).toBeTruthy();
    expect(screen.getByRole("heading", { name: /distribuicao/i })).toBeTruthy();
    expect(
      screen.getAllByRole("heading", { name: /aprendizado por metrica/i })
        .length,
    ).toBeGreaterThan(0);
    expect(screen.getByRole("link", { name: /abrir estudio/i })).toBeTruthy();
    expect(screen.getAllByText(/hls pronto para pde/i).length).toBeGreaterThan(
      0,
    );
    expect(screen.getAllByText(/compras/i).length).toBeGreaterThan(0);
  });

  it("shows commercial readiness and next action by PDE funnel slot", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/products") {
        return Promise.resolve({
          data: [
            {
              id: 4,
              name: "MUSA",
              slug: "musa",
              productType: "PDE",
              promise: "Diagnostico visual imediato",
              niche: "Moda",
              avatar: "Mulheres",
              explicitPain: "Look sem presenca",
              uniqueMechanism: "Microajustes visuais",
              tripwire: "Plano MUSA",
              riskReversal: "Garantia",
              socialProof: "Casos reais",
              checkoutMonetization: "R$ 67",
              funnel: "PDE",
              creativeVolume: "Alto",
              storytelling: "Direto",
              aiCost: 0,
            },
          ],
        });
      }
      if (url === "/api/products/4/sales-videos/profiles") {
        return Promise.resolve({
          data: [
            {
              id: 91,
              productId: 4,
              videoKind: "HERO",
              title: "Abertura MUSA",
              avatarStrategy: "PLATFORM_TEST_AVATAR",
              requiresConsent: false,
              status: "PUBLISHED",
            },
          ],
        });
      }
      if (url === "/api/products/4/sales-videos/jobs") {
        return Promise.resolve({ data: [] });
      }
      if (url === "/api/products/4/pde-production-slots") {
        return Promise.resolve({
          data: [
            {
              id: 7,
              slotCode: "v6",
              productSlug: "musa",
              domain: "v6.clubemusa.com.br",
              publicUrl: "https://v6.clubemusa.com.br",
              experienceVersion: "musa-pde-entry-v6-video-motivacional",
              layoutKey: "musa",
              targetEnvironment: "production",
              status: "ACTIVE",
            },
          ],
        });
      }
      if (url === "/api/products/4/pde-videos") {
        return Promise.resolve({
          data: [
            {
              slot: {
                id: 7,
                slotCode: "v6-abertura",
                productSlug: "musa",
                domain: "v6.clubemusa.com.br",
                publicUrl: "https://v6.clubemusa.com.br",
                experienceVersion: "musa-pde-entry-v6-video-motivacional",
                layoutKey: "musa",
                targetEnvironment: "production",
                status: "ACTIVE",
              },
              videos: [
                {
                  assignmentSource: "PUBLISHED_CONTRACT",
                  objective: "Abertura",
                  primaryMetric: "Play e progresso 25%",
                  provider: "LUMA",
                  model: "ray",
                  status: "PUBLISHED",
                  reviewStatus: "APPROVED",
                  hlsPlaybackUrl: "https://cdn.example/video.m3u8",
                },
              ],
              alerts: [],
            },
          ],
        });
      }
      if (url === "/api/video-projects") {
        return Promise.resolve({ data: [] });
      }
      if (url === "/api/sales-videos/profiles/91/performance-summary") {
        return Promise.resolve({
          data: {
            profileId: 91,
            totalViews: 20,
            totalLeads: 4,
            totalQualifiedLeads: 2,
            totalCheckoutStarted: 1,
            totalPurchases: 1,
            totalRevenue: 67,
            variants: [],
            providerScores: [],
          },
        });
      }
      return Promise.resolve({ data: [] });
    });

    setup(<App />, ["/pde-video-production"]);

    await waitFor(() => {
      expect(screen.getByText("Aprendendo")).toBeTruthy();
    });
    expect(screen.getAllByText("Sem briefing").length).toBeGreaterThan(0);
    expect(
      screen.getByText(/Ler retencao, checkout e compra antes de escalar/i),
    ).toBeTruthy();
    expect(screen.getByText("Slots sem briefing")).toBeTruthy();
  });
});
