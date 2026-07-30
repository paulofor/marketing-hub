import { cleanup, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { Experiment } from "../../api/experiment/useExperiments";
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
  useProductPdeVersionVideos: () => ({
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
  }),
}));

vi.mock("../../api/experiment/useUpdateExperimentVideoAssetReview", () => ({
  useUpdateExperimentVideoAssetReview: () => ({
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
  });
});
