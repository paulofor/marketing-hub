import { cleanup, render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import ProductPdeVideosPage from "./ProductPdeVideosPage";

vi.mock("axios");
vi.mock("../../components/AdaptiveVideoPlayer", () => ({
  AdaptiveVideoPlayer: ({ src }: { src: string }) => (
    <div data-testid="adaptive-video-player">{src}</div>
  ),
}));

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={["/products/4/pde-videos"]}>
        <Routes>
          <Route
            path="/products/:productId/pde-videos"
            element={<ProductPdeVideosPage />}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("ProductPdeVideosPage", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/products/4") {
        return Promise.resolve({
          data: {
            id: 4,
            slug: "metodo-musa-7-dias",
            name: "Método MUSA - Presença Elegante em 7 Dias",
          },
        });
      }
      if (url === "/api/products/4/pde-videos") {
        return Promise.resolve({
          data: [
            {
              slot: {
                id: 6,
                slotCode: "v6",
                productSlug: "metodo-musa-7-dias",
                domain: "v6.clubemusa.com.br",
                publicUrl: "https://v6.clubemusa.com.br",
                experienceVersion: "musa-pde-entry-v6-video-motivacional",
                layoutKey: "video-motivacional",
                targetEnvironment: "production",
                status: "ACTIVE",
                sourceExperimentId: 76,
              },
              videos: [
                {
                  id: 31,
                  experimentId: 76,
                  assignmentSource: "SOURCE_EXPERIMENT",
                  objective: "Testar promessa de elegância.",
                  primaryMetric: "DIAGNOSTIC_STARTED",
                  provider: "HEYGEN",
                  model: "avatar_iv",
                  status: "READY",
                  hlsPlaybackUrl: "/assets/hls/musa-v6-principal/index.m3u8",
                  durationSeconds: 42,
                  reviewStatus: "APPROVED",
                },
                {
                  id: 30,
                  experimentId: 68,
                  assignmentSource: "VERSION_TOKEN",
                  objective: "Testar prova visual.",
                  primaryMetric: "DIAGNOSTIC_STARTED",
                  provider: "HEYGEN",
                  model: "avatar_iv",
                  status: "READY",
                  hlsPlaybackUrl: "/assets/hls/musa-v6-prova/index.m3u8",
                  durationSeconds: 28,
                  reviewStatus: "APPROVED",
                },
              ],
              alerts: [
                "Vídeo #30 pertence ao experimento 68, mas foi exibido nesta versão porque o HLS aponta para v6.",
              ],
            },
          ],
        });
      }
      return Promise.resolve({ data: [] });
    });
  });

  afterEach(() => {
    cleanup();
  });

  it("deixa claro que uma versão PDE pode ter múltiplos vídeos HLS complementares", async () => {
    renderPage();

    expect(await screen.findByText("v6")).toBeTruthy();
    expect(screen.getByText(/2 vídeos HLS vinculados/i)).toBeTruthy();
    expect(
      screen.getByText(/O backend resolve a versão comercial/i),
    ).toBeTruthy();
    expect(
      screen.getByText(/Vídeo #30 pertence ao experimento 68/i),
    ).toBeTruthy();
    expect(screen.getByText("Abertura / hero")).toBeTruthy();
    expect(screen.getByText("Prova visual")).toBeTruthy();
    expect(screen.getByText("#31")).toBeTruthy();
    expect(screen.getByText("#30")).toBeTruthy();
    expect(
      screen.getAllByText(
        "https://v6.clubemusa.com.br/assets/hls/musa-v6-principal/index.m3u8",
      ),
    ).toHaveLength(2);
    expect(
      screen.getByText(
        "https://v6.clubemusa.com.br/assets/hls/musa-v6-prova/index.m3u8",
      ),
    ).toBeTruthy();
  });
});
