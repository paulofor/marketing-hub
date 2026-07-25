import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import axios, { AxiosRequestConfig } from "axios";
import CreativeVideoReviewPage from "./CreativeVideoReviewPage";

vi.mock("axios");

const mockedAxiosGet = vi.mocked(axios.get);

function setup() {
  const client = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <CreativeVideoReviewPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

afterEach(() => {
  vi.clearAllMocks();
});

describe("CreativeVideoReviewPage", () => {
  it("mostra totais gerais da fila mesmo quando o filtro pendente esta vazio", async () => {
    mockedAxiosGet.mockImplementation(async (_url: string, config?: AxiosRequestConfig) => {
      const status = config?.params?.status;
      if (status === "DRAFT") {
        return { data: [] };
      }
      return {
        data: [
          {
            id: 243,
            sourceType: "EXPERIMENT_VIDEO_ASSET",
            funnelSlot: "LANDING_HERO",
            experimentId: 71,
            experimentName: "Metodo MUSA - Presenca Elegante em 7 Dias-E001",
            experimentStatus: "PLANNED",
            hypothesisTitle: "Metodo MUSA - Presenca Elegante em 7 Dias",
            nicheName: "Mulheres urbanas - sofisticacao acessivel",
            format: "VIDEO",
            headline: "Descubra sua peca-sinal",
            primaryText: "Texto do criativo",
            videoUrl: "https://example.com/video-243.mp4",
            status: "REJECTED",
            rejectionReason: "A legenda ta ruim",
            reviewedAt: "2026-07-25T15:10:00Z",
            videoCostUsd: 0.08,
            audioCostUsd: 0.02,
            totalProductionCostUsd: 0.1,
          },
          {
            id: 242,
            sourceType: "CREATIVE",
            funnelSlot: "AD",
            experimentId: 70,
            experimentName: "MUSA-H001-E008",
            experimentStatus: "PLANNED",
            hypothesisTitle: "MUSA-H001",
            nicheName: "Mulheres urbanas - sofisticacao acessivel",
            format: "VIDEO",
            headline: "Descubra sua presenca MUSA",
            primaryText: "Texto do criativo",
            videoUrl: "https://example.com/video-242.mp4",
            status: "REJECTED",
            rejectionReason: "O audio esta em ingles",
            reviewedAt: "2026-07-25T15:20:00Z",
            videoCostUsd: 0.05,
            totalProductionCostUsd: 0.05,
          },
        ],
      };
    });

    setup();

    const summary = await screen.findByLabelText("Resumo da fila");
    await waitFor(() => {
      expect(within(summary).getByText("Reprovados").nextElementSibling).toHaveTextContent("2");
      expect(within(summary).getByText("Custo reprovado").nextElementSibling).toHaveTextContent(/US\$\s*0,1500/);
    });
    expect(screen.getByText("Nenhum vídeo encontrado para este filtro.")).toBeInTheDocument();
  });

  it("mostra nomenclatura em portugues para origem e uso no funil", async () => {
    mockedAxiosGet.mockResolvedValue({
      data: [
        {
          id: 21,
          sourceType: "EXPERIMENT_VIDEO_ASSET",
          funnelSlot: "LANDING_HERO",
          experimentId: 71,
          experimentName: "Metodo MUSA - Presenca Elegante em 7 Dias-E001",
          experimentStatus: "PLANNED",
          hypothesisTitle: "Metodo MUSA - Presenca Elegante em 7 Dias",
          nicheName: "Mulheres urbanas - sofisticacao acessivel",
          format: "VIDEO",
          headline: "Video para continuar a jornada",
          primaryText: "Texto do video produzido",
          videoUrl: "https://example.com/video-21.mp4",
          status: "DRAFT",
          videoCostUsd: 0.08,
          totalProductionCostUsd: 0.08,
        },
      ],
    });

    setup();

    expect(await screen.findByText("Vídeo produzido #21")).toBeInTheDocument();
    expect(screen.getByText("Uso no funil")).toBeInTheDocument();
    expect(screen.getByText("PDE / hero da página")).toBeInTheDocument();
  });
});
