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
  it("mostra candidatas opcionais somente na aba própria, com decisão explícita", async () => {
    const optional = {
      id: 871,
      sourceType: "EXPERIMENT_VIDEO_ASSET",
      experimentId: 372,
      experimentName: "Experimento sintético",
      experimentStatus: "PLANNED",
      format: "VIDEO",
      headline: "Candidata opcional de teste",
      primaryText: "Demonstração sintética",
      videoUrl: "https://fixture.invalid/optional.mp4",
      status: "DRAFT",
      eligibility: {
        state: "OPTIONAL_REVIEW",
        reason: "Não exige sua aprovação para o fluxo continuar.",
        approvalAvailable: true,
        agentReviewRequestAvailable: false,
      },
    };
    mockedAxiosGet.mockImplementation(async (url, config) => {
      if (url.endsWith("/summary"))
        return { data: { awaitingReviewCount: 0, optionalReviewCount: 1 } };
      return {
        data: config?.params?.state === "AWAITING_REVIEW" ? [] : [optional],
      };
    });
    setup();
    expect(
      await screen.findByText("Nenhuma aprovação necessária neste contexto."),
    ).toBeVisible();
    expect(screen.queryByText(optional.headline)).not.toBeInTheDocument();
    expect(mockedAxiosPatch).not.toHaveBeenCalled();
    await userEvent.click(
      screen.getByRole("button", { name: "Candidatas opcionais" }),
    );
    expect(await screen.findByText(optional.headline)).toBeVisible();
    expect(
      screen.getByText("Candidata opcional — sem aprovação obrigatória"),
    ).toBeVisible();
    expect(
      screen.getByRole("button", { name: "Aprovar para portfólio" }),
    ).toBeEnabled();
    expect(mockedAxiosGet).toHaveBeenCalledWith("/api/creatives/video-review", {
      params: {
        state: "OPTIONAL_REVIEW",
        productId: undefined,
        experimentId: undefined,
      },
    });
    expect(mockedAxiosPatch).not.toHaveBeenCalled();
  });

  it("mostra totais gerais da fila mesmo quando o filtro pendente esta vazio", async () => {
    const createdAt = new Date().toISOString();
    mockedAxiosGet.mockImplementation(
      async (_url: string, config?: AxiosRequestConfig) => {
        if (_url.endsWith("/summary"))
          return {
            data: {
              awaitingReviewCount: 0,
              blockedCount: 0,
              historicalCount: 0,
              approvedCount: 0,
              rejectedCount: 2,
            },
          };
        if (config?.params?.state === "AWAITING_REVIEW") {
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
              eligibility: {
                state: "REJECTED",
                reason: "Reprovado",
                approvalAvailable: false,
                agentReviewRequestAvailable: false,
              },
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
              eligibility: {
                state: "REJECTED",
                reason: "Reprovado",
                approvalAvailable: false,
                agentReviewRequestAvailable: false,
              },
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
      screen.getByText("Nenhuma aprovação necessária neste contexto."),
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
          eligibility: {
            state: "AWAITING_REVIEW",
            reason: "Nova peça",
            approvalAvailable: true,
            agentReviewRequestAvailable: false,
          },
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
          eligibility: {
            state: "BLOCKED",
            reason: "Corrigir antes da revisão humana",
            approvalAvailable: false,
            agentReviewRequestAvailable: true,
          },
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
    expect(
      screen.getByRole("link", { name: "Corrigir na aba Criativos" }),
    ).toHaveAttribute("href", "/experiments/91?tab=creatives");

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
          eligibility: {
            state: "AWAITING_REVIEW",
            reason: "Nova peça",
            approvalAvailable: true,
            agentReviewRequestAvailable: false,
          },
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
  it("mantém a tentativa histórica sem oferecer aprovação, reprovação ou reanálise", async () => {
    mockedAxiosGet.mockResolvedValue({
      data: [
        {
          id: 818,
          sourceType: "CREATIVE",
          experimentId: 320,
          experimentName: "Experimento encerrado",
          experimentStatus: "INVALIDATED",
          format: "VIDEO",
          headline: "Versão anterior",
          primaryText: "Demonstração",
          videoUrl: "https://fixture.invalid/demo.mp4",
          status: "DRAFT",
          agentReviewStatus: "FAILED",
          eligibility: {
            state: "HISTORICAL",
            reason: "Substituída pelo criativo aprovado #819.",
            approvalAvailable: false,
            agentReviewRequestAvailable: false,
          },
        },
      ],
    });
    setup();
    expect(
      await screen.findByText("Histórico — sem ação pendente"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Substituída pelo criativo aprovado #819."),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Aprovar para portfólio" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Reavaliar com Têmis" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Reprovar" }),
    ).not.toBeInTheDocument();
    expect(mockedAxiosPatch).not.toHaveBeenCalled();
    expect(mockedAxiosPost).not.toHaveBeenCalled();
  });

  it("solicita cada classificação ao backend sem deduzir elegibilidade de DRAFT", async () => {
    const user = userEvent.setup();
    mockedAxiosGet.mockImplementation(async (url: string) => ({
      data: url.endsWith("/summary")
        ? {
            awaitingReviewCount: 0,
            blockedCount: 3,
            historicalCount: 4,
            approvedCount: 8,
            rejectedCount: 1,
          }
        : [],
    }));
    setup();
    await user.click(
      screen.getByRole("button", { name: "Ajustes e pareceres" }),
    );
    await waitFor(() =>
      expect(mockedAxiosGet).toHaveBeenCalledWith(
        "/api/creatives/video-review",
        { params: { state: "BLOCKED" } },
      ),
    );
    await user.click(screen.getByRole("button", { name: "Histórico" }));
    await waitFor(() =>
      expect(mockedAxiosGet).toHaveBeenCalledWith(
        "/api/creatives/video-review",
        { params: { state: "HISTORICAL" } },
      ),
    );
  });
  it("preserva a exigência de prévia para a decisão humana", async () => {
    mockedAxiosGet.mockResolvedValue({
      data: [
        {
          id: 825,
          sourceType: "CREATIVE",
          experimentId: 325,
          experimentName: "Contexto sintético",
          experimentStatus: "PLANNED",
          format: "VIDEO",
          headline: "Mídia sem prévia",
          primaryText: "Demonstração",
          videoId: "meta-fixture",
          videoUrl: null,
          status: "DRAFT",
          agentReviewStatus: "APPROVED",
          eligibility: {
            state: "AWAITING_REVIEW",
            reason: "Nova peça",
            approvalAvailable: true,
            agentReviewRequestAvailable: false,
          },
        },
      ],
    });
    setup();
    expect(
      await screen.findByText("Vídeo sem URL pública"),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Aprovar para portfólio" }),
    ).toBeDisabled();
  });
});
