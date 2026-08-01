import { render, screen, waitFor, cleanup } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import AudioVideoStudioPage from "./AudioVideoStudioPage";

vi.mock("axios");

function setup() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <AudioVideoStudioPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

afterEach(() => {
  cleanup();
});

describe("AudioVideoStudioPage", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    (axios.get as any).mockResolvedValue({ data: [] });
    (axios.post as any).mockResolvedValue({
      data: {
        id: 101,
        title: "MUSA v7 - O espelho antes de sair",
      },
    });
  });

  it("preenche e salva o blueprint cinematografico da MUSA v7", async () => {
    const user = userEvent.setup();
    setup();

    await user.click(
      await screen.findByRole("button", {
        name: /musa v7 hero cinematografico/i,
      }),
    );
    expect(
      screen.getByDisplayValue("MUSA v7 - O espelho antes de sair"),
    ).toBeTruthy();

    await user.click(screen.getByRole("button", { name: /criar blueprint/i }));

    await waitFor(() => {
      expect(axios.post).toHaveBeenCalledWith(
        "/api/sales-videos/projects",
        expect.objectContaining({
          productId: 4,
          campaignKey: "musa-pde-entry-v7-espelho-antes-de-sair",
          videoCategory: "COMMERCIAL_SHORT",
          productionMode: "CINEMATIC_SCENE_BLUEPRINT",
          targetChannel: "PDE_HERO_DIAGNOSTIC",
          title: "MUSA v7 - O espelho antes de sair",
          targetDurationSeconds: 30,
          primaryMetric:
            "CTA_CLICK_TO_DIAGNOSTIC; apoio: VIDEO_PLAY, VIDEO_75, DIAGNOSTIC_COMPLETED, PAYWALL_VIEWED, CHECKOUT_STARTED, PURCHASE",
          status: "READY_FOR_SCRIPT",
        }),
      );
    });
    expect((axios.post as any).mock.calls[0][1].scenePlan).toContain(
      "Cena 1 (6-8s)",
    );
    expect((axios.post as any).mock.calls[0][1].qualityGate).toContain(
      "heroVideos da v7",
    );
  });
});
