import { cleanup, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { Experiment } from "../../api/experiment/useExperiments";
import { useProductPdeVersionVideos } from "../../api/product/usePdeVersionVideos";
import ExperimentVideoTab from "./ExperimentVideoTab";

vi.mock("../../api/experiment/useExperimentVideoAssets", () => ({
  useExperimentVideoAssets: () => ({ data: [], isLoading: false }),
}));

vi.mock("../../api/experiment/useGeraSalesPagePublications", () => ({
  useGeraSalesPagePublications: () => ({ data: [] }),
}));

vi.mock("../../api/experiment/useExperimentVideoPerformanceDashboard", () => ({
  useExperimentVideoPerformanceDashboard: () => ({
    data: {
      summary: {
        approvedAssets: 0,
        metaVideoCreatives: 0,
        impressions: 343,
        clicks: 34,
        diagnosticStarts: 0,
        checkoutAccesses: 0,
        purchases: 0,
        spend: 5.31,
        lastMetricAt: "2026-07-30T17:48:45Z",
        recommendation:
          "Clique sem início de diagnóstico: revisar primeira dobra, promessa e CTA de baixo esforço.",
      },
      assets: [],
      campaigns: [],
    },
    isLoading: false,
    isError: false,
  }),
}));

vi.mock("../../api/product/usePdeVersionVideos", () => ({
  useProductPdeVersionVideos: vi.fn(() => ({
    data: [
      {
        slot: {
          id: 4,
          slotCode: "v6",
          productSlug: "metodo-musa-7-dias",
          domain: "v6.clubemusa.com.br",
          publicUrl: "https://v6.clubemusa.com.br",
          experienceVersion: "musa-pde-entry-v6-video-motivacional",
          layoutKey: "video-motivacional",
          targetEnvironment: "production-v6",
          status: "ACTIVE",
          sourceExperimentId: 76,
        },
        videos: [
          {
            id: 22,
            experimentId: 68,
            assignmentSource: "VERSION_TOKEN",
            objective: "Microexperiência visível",
            primaryMetric: "DIAGNOSTIC_STARTED",
            provider: "HEYGEN",
            model: "avatar",
            status: "READY",
            reviewStatus: "APPROVED",
            hlsPlaybackUrl:
              "/assets/hls/musa-v6-microexperiencia-visivel/index.m3u8",
            durationSeconds: 42,
            salesVideoJobId: 20462,
            assetId: 1935,
          },
        ],
        alerts: [],
      },
    ],
  })),
}));

vi.mock("../../api/experiment/useUpdateExperimentVideoAssetReview", () => ({
  useUpdateExperimentVideoAssetReview: () => ({
    mutateAsync: vi.fn(),
    isPending: false,
  }),
}));

vi.mock("../../api/experiment/useUploadExperimentAdVideo", () => ({
  useUploadExperimentAdVideo: () => ({
    mutateAsync: vi.fn(),
    isPending: false,
  }),
}));

vi.mock("../../components/AdaptiveVideoPlayer", () => ({
  AdaptiveVideoPlayer: ({ src }: { src: string }) => (
    <div data-testid="adaptive-video-player">{src}</div>
  ),
}));

const experiment = {
  id: "76",
  nicheId: 31,
  productId: 4,
  hypothesisId: "hypothesis-76",
  name: "Metodo MUSA - Presenca Elegante em 7 Dias-E005",
  hypothesis: "Validar PDE MUSA v6 com video motivacional.",
  experimentType: "PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL",
  followUpActionUrl: "https://v6.clubemusa.com.br",
  creativeApproved: true,
  status: "RUNNING",
  platform: "FACEBOOK",
  stage: "AD",
  startDate: "2026-07-28",
  endDate: "2026-08-04",
  createdAt: "2026-07-28T18:44:46Z",
  updatedAt: "2026-07-30T17:38:26Z",
} satisfies Experiment;

describe("ExperimentVideoTab", () => {
  afterEach(() => {
    cleanup();
  });

  it("permite importar uma candidata pronta para PDE sem retirar a revisão", () => {
    render(
      <MemoryRouter>
        <ExperimentVideoTab
          experiment={{
            ...experiment,
            id: "125",
            productId: 18,
            status: "PLANNED",
          }}
        />
      </MemoryRouter>,
    );
    expect(screen.getByLabelText("Arquivo MP4")).toBeTruthy();
    expect(
      screen.getByLabelText("IDs dos vídeos aprovados usados como fonte"),
    ).toBeTruthy();
    expect(
      screen.getByRole("button", {
        name: "Enviar para revisão do experimento",
      }),
    ).toBeDisabled();
    expect(
      screen
        .getByRole("link", { name: "Gerenciar no produto" })
        .getAttribute("href"),
    ).toBe("/products/18/sales-videos");
    expect(useProductPdeVersionVideos).toHaveBeenLastCalledWith(18);
  });

  it("não consulta vídeos de Vega quando o produto do experimento está ausente", () => {
    render(
      <MemoryRouter>
        <ExperimentVideoTab
          experiment={{ ...experiment, productId: undefined }}
        />
      </MemoryRouter>,
    );
    expect(useProductPdeVersionVideos).toHaveBeenLastCalledWith(undefined);
    expect(
      screen
        .getByRole("link", { name: "Gerenciar no produto" })
        .getAttribute("href"),
    ).toBe("/products");
  });

  it("preserva o bloqueio de alterações também na importação de PDE", () => {
    render(
      <MemoryRouter>
        <ExperimentVideoTab
          experiment={{ ...experiment, productId: 18 }}
          alterationLocked
        />
      </MemoryRouter>,
    );
    expect(screen.getByLabelText("Arquivo MP4")).toBeDisabled();
    expect(
      screen.getByRole("button", {
        name: "Enviar para revisão do experimento",
      }),
    ).toBeDisabled();
  });

  it("mostra o video HLS publicado no PDE quando o experimento nao possui asset direto", async () => {
    render(
      <MemoryRouter>
        <ExperimentVideoTab experiment={experiment} />
      </MemoryRouter>,
    );

    expect(await screen.findByText("Vídeos PDE publicados")).toBeTruthy();
    expect(screen.getByText("1")).toBeTruthy();
    expect(screen.getByText("PDE v6 · READY")).toBeTruthy();
    expect(
      screen.getByText(
        "https://v6.clubemusa.com.br/assets/hls/musa-v6-microexperiencia-visivel/index.m3u8",
      ),
    ).toBeTruthy();
    expect(
      screen.getByText(/Vídeo publicado no PDE v6: asset #22/i),
    ).toBeTruthy();
    expect(
      screen.getByText("PDE em produção pelo destino do experimento"),
    ).toBeTruthy();
    expect(screen.getByText("Experiência PDE")).toBeTruthy();
    expect(
      screen.getByText(/A experiência PDE abre em nova aba de teste/i),
    ).toBeTruthy();
    expect(
      screen.queryByTitle(`Página de venda do experimento ${experiment.id}`),
    ).toBeNull();
    expect(
      screen
        .getByRole("link", { name: "Abrir experiência" })
        .getAttribute("href"),
    ).toBe("https://v6.clubemusa.com.br/?mh_test=1&pde_analytics=off");
  });
});
