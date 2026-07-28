import { render, screen, waitFor, cleanup } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import VideoHubPage from "./VideoHubPage";

vi.mock("axios");
vi.mock("../../components/AdaptiveVideoPlayer", () => ({
  AdaptiveVideoPlayer: ({ src }: { src: string }) => (
    <div data-testid="adaptive-video-player">{src}</div>
  ),
}));

function setup() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <VideoHubPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

afterEach(() => {
  cleanup();
});

describe("VideoHubPage", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/products") {
        return Promise.resolve({
          data: [{ id: 4, slug: "metodo-musa-7-dias", name: "MUSA" }],
        });
      }
      if (url === "/api/experiments") {
        return Promise.resolve({
          data: [{ id: 74, name: "PDE Musa v6" }],
        });
      }
      if (url === "/api/experiments/video-assets") {
        return Promise.resolve({
          data: [
            {
              id: 22,
              experimentId: 74,
              slot: "LANDING_HERO",
              objective: "Aumentar inicio do diagnostico.",
              primaryMetric: "DIAGNOSTIC_STARTED",
              provider: "MUSA_POST_PRODUCTION",
              model: "hls-final",
              status: "READY",
              assetUrl: "https://cdn.test/video.mp4",
              hlsPlaybackUrl: "",
              durationSeconds: 27,
              hasAudio: true,
              reviewStatus: "PENDING",
              requiredForRelease: true,
              salesVideoProfileId: 35,
              salesVideoJobId: 20462,
            },
          ],
        });
      }
      return Promise.resolve({ data: [] });
    });
    (axios.patch as any).mockResolvedValue({
      data: {
        id: 22,
        experimentId: 74,
        hlsPlaybackUrl:
          "https://v6.clubemusa.com.br/assets/hls/musa-v6-microexperiencia-visivel/index.m3u8",
      },
    });
  });

  it("permite salvar a playlist HLS no ativo comercial do Marketing Hub", async () => {
    const user = userEvent.setup();
    setup();

    const hlsInput = await screen.findByLabelText(/playlist hls do pde/i);
    await user.clear(hlsInput);
    await user.type(
      hlsInput,
      "https://v6.clubemusa.com.br/assets/hls/musa-v6-microexperiencia-visivel/index.m3u8",
    );
    await user.click(screen.getByRole("button", { name: /salvar hls/i }));

    await waitFor(() => {
      expect(axios.patch).toHaveBeenCalledWith(
        "/api/experiments/74/video-assets/22",
        {
          reviewStatus: undefined,
          rejectionReason: undefined,
          reviewedBy: undefined,
          hlsPlaybackUrl:
            "https://v6.clubemusa.com.br/assets/hls/musa-v6-microexperiencia-visivel/index.m3u8",
        },
      );
    });
  });
});
