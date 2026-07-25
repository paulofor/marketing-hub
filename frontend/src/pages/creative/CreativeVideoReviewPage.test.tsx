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
          },
          {
            id: 242,
            sourceType: "CREATIVE",
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
          },
        ],
      };
    });

    setup();

    const summary = await screen.findByLabelText("Resumo da fila");
    await waitFor(() => {
      expect(within(summary).getByText("Reprovados").nextElementSibling).toHaveTextContent("2");
      expect(within(summary).getByText("Relacionados a MUSA").nextElementSibling).toHaveTextContent("2");
    });
    expect(screen.getByText("Nenhum vídeo encontrado para este filtro.")).toBeInTheDocument();
  });
});
