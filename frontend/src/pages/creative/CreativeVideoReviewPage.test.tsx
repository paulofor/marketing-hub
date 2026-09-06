import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  cleanup,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import axios, { AxiosRequestConfig } from "axios";
import { toast } from "react-toastify";
import CreativeVideoReviewPage from "./CreativeVideoReviewPage";

vi.mock("axios");
vi.mock("react-toastify", () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

const mockedAxiosGet = vi.mocked(axios.get);
const mockedAxiosPost = vi.mocked(axios.post);
const mockedAxiosPatch = vi.mocked(axios.patch);

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
  cleanup();
  vi.clearAllMocks();
});

describe("CreativeVideoReviewPage", () => {
  it("mostra totais gerais da fila mesmo quando o filtro pendente esta vazio", async () => {
    const createdAt = new Date().toISOString();
    mockedAxiosGet.mockImplementation(
      async (_url: string, config?: AxiosRequestConfig) => {
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
              createdAt,
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
              createdAt,
              videoCostUsd: 0.05,
              totalProductionCostUsd: 0.05,
            },
          ],
        };
      },
    );

    setup();

    const summary = await screen.findByLabelText("Resumo da fila");
    await waitFor(() => {
      expect(
        within(summary).getByText("Reprovados").nextElementSibling,
      ).toHaveTextContent("2");
      expect(
        within(summary).getByText("Custo total").nextElementSibling,
      ).toHaveTextContent(/US\$\s*0,1500.*R\$\s*0,75/);
      expect(
        within(summary).getByText("Custo mês").nextElementSibling,
      ).toHaveTextContent(/US\$\s*0,1500.*R\$\s*0,75/);
      expect(
        within(summary).getByText("Custo ano").nextElementSibling,
      ).toHaveTextContent(/US\$\s*0,1500.*R\$\s*0,75/);
      expect(
        within(summary).getByText("Custo reprovado").nextElementSibling,
      ).toHaveTextContent(/US\$\s*0,1500.*R\$\s*0,75/);
    });
    expect(
      screen.getByText("Nenhum vídeo encontrado para este filtro."),
    ).toBeInTheDocument();
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

  it("explica o bloqueio especialista e permite reenviar o anúncio para Têmis", async () => {
    const user = userEvent.setup();
    mockedAxiosGet.mockResolvedValue({
      data: [
        {
          id: 524,
          sourceType: "CREATIVE",
          funnelSlot: "AD",
          experimentId: 91,
          experimentName: "MUSA-H003-E002",
          experimentStatus: "PLANNED",
          format: "VIDEO",
          headline: "Seu 1º ajuste sem comprar tudo",
          primaryText: "Texto do anúncio",
          videoUrl: "https://example.com/video-524.mp4",
          status: "DRAFT",
          agentReviewStatus: "ADJUST",
          agentReviewSummary: "A inspeção visual precisa ser repetida.",
          approvalBlockedReason:
            "Aprovação bloqueada: Têmis, Agente Especialista em Anúncios, ainda não aprovou o anúncio. Reenvie-o para a revisão independente.",
        },
      ],
    });
    mockedAxiosPost.mockResolvedValue({
      data: { id: 524, agentReviewStatus: "PENDING" },
    });

    setup();

    expect(
      await screen.findByText("Revisão de Têmis: ajustes necessários."),
    ).toBeInTheDocument();
    expect(
      screen.getByText("A inspeção visual precisa ser repetida."),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Aprovar para portfólio" }),
    ).toBeDisabled();

    await user.click(
      screen.getByRole("button", { name: "Reavaliar com Têmis" }),
    );

    await waitFor(() => {
      expect(mockedAxiosPost).toHaveBeenCalledWith(
        "/api/creatives/524/agent-review/request",
      );
    });
    expect(toast.success).toHaveBeenCalledWith(
      "Anúncio reenviado para a revisão independente de Têmis",
    );
  });

  it("mostra a mensagem funcional do backend quando a aprovação é recusada", async () => {
    const user = userEvent.setup();
    mockedAxiosGet.mockResolvedValue({
      data: [
        {
          id: 524,
          sourceType: "CREATIVE",
          funnelSlot: "AD",
          experimentId: 91,
          experimentName: "MUSA-H003-E002",
          experimentStatus: "PLANNED",
          format: "VIDEO",
          headline: "Seu 1º ajuste sem comprar tudo",
          primaryText: "Texto do anúncio",
          videoUrl: "https://example.com/video-524.mp4",
          status: "DRAFT",
          agentReviewStatus: "APPROVED",
        },
      ],
    });
    vi.mocked(axios.isAxiosError).mockReturnValue(true);
    mockedAxiosPatch.mockRejectedValue({
      response: {
        data: {
          message: "Aprovação bloqueada por um gate comercial vigente.",
        },
      },
    });

    setup();
    await user.click(
      await screen.findByRole("button", { name: "Aprovar para portfólio" }),
    );

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith(
        "Aprovação bloqueada por um gate comercial vigente.",
      );
    });
  });
});
