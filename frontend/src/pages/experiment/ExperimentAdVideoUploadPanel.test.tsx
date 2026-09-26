import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { Experiment } from "../../api/experiment/useExperiments";
import ExperimentAdVideoUploadPanel from "./ExperimentAdVideoUploadPanel";

const fixture = vi.hoisted(() => ({ mutateAsync: vi.fn() }));

vi.mock("../../api/experiment/useUploadExperimentAdVideo", () => ({
  useUploadExperimentAdVideo: () => ({
    mutateAsync: fixture.mutateAsync,
    isPending: false,
  }),
}));

const experiment = {
  id: "94",
  nicheId: 21,
  productId: 7,
  hypothesisId: "capella-hypothesis",
  name: "MAQA-H002-E002",
  hypothesis: "Validar demonstração visual do kit.",
  primaryVariable: "Formato de demonstração: vídeo vertical vs imagem estática",
  primaryMetric: "Compras líquidas e contribuição após mídia (R$)",
  creativeApproved: false,
  status: "PLANNED",
  platform: "FACEBOOK",
  stage: "AD",
  startDate: "2026-09-28",
  endDate: "2026-10-02",
  createdAt: "2026-09-26T00:00:00Z",
  updatedAt: "2026-09-26T00:00:00Z",
} satisfies Experiment;

function fillCommercialEvidence() {
  fireEvent.change(screen.getByLabelText("Roteiro exibido no vídeo"), {
    target: { value: "Seu trabalho é caprichado. Seu Instagram mostra isso?" },
  });
  fireEvent.change(
    screen.getByLabelText("Chave dos ativos visuais de origem"),
    { target: { value: "capella-exp88-approved-assets-v1" } },
  );
  fireEvent.change(screen.getByLabelText("Evidência dos ativos usados"), {
    target: { value: "Posts e stories aprovados do experimento #88." },
  });
  fireEvent.change(screen.getByLabelText("Referência versionada da produção"), {
    target: {
      value: "scripts/marketing/create-capella-successor-video-v1.sh",
    },
  });
  fireEvent.click(
    screen.getByLabelText(
      "Confirmei a reprodução e o arquivo possui áudio utilizável.",
    ),
  );
}

describe("upload governado de vídeo do experimento", () => {
  beforeEach(() => {
    cleanup();
    vi.clearAllMocks();
    fixture.mutateAsync.mockResolvedValue({ id: 81 });
  });

  it("envia MP4 vertical com roteiro, origem e referência versionada", async () => {
    const file = new File(["mp4"], "capella-v1.mp4", { type: "video/mp4" });
    render(
      <ExperimentAdVideoUploadPanel
        experiment={experiment}
        locked={false}
        metadataReader={async () => ({
          durationSeconds: 18,
          width: 1080,
          height: 1920,
        })}
      />,
    );

    fireEvent.change(screen.getByLabelText("Arquivo MP4"), {
      target: { files: [file] },
    });
    await screen.findByText(/1080×1920 · 18s/);
    fillCommercialEvidence();
    fireEvent.click(
      screen.getByRole("button", {
        name: "Enviar para revisão do experimento",
      }),
    );

    await waitFor(() => expect(fixture.mutateAsync).toHaveBeenCalledTimes(1));
    expect(fixture.mutateAsync).toHaveBeenCalledWith(
      expect.objectContaining({
        file,
        durationSeconds: 18,
        hasAudio: true,
        visualSourceKey: "capella-exp88-approved-assets-v1",
        requiredForRelease: true,
      }),
    );
  });

  it("bloqueia arquivo que não seja vertical 9:16", async () => {
    const file = new File(["mp4"], "horizontal.mp4", { type: "video/mp4" });
    render(
      <ExperimentAdVideoUploadPanel
        experiment={experiment}
        locked={false}
        metadataReader={async () => ({
          durationSeconds: 18,
          width: 1920,
          height: 1080,
        })}
      />,
    );

    fireEvent.change(screen.getByLabelText("Arquivo MP4"), {
      target: { files: [file] },
    });

    expect(
      await screen.findByText(
        "O vídeo precisa estar no formato vertical 9:16.",
      ),
    ).toBeInTheDocument();
    expect(fixture.mutateAsync).not.toHaveBeenCalled();
  });
});
