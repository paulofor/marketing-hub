import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import axios from "axios";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { Experiment } from "../../api/experiment/useExperiments";
import type { ExperimentVideoAsset } from "../../api/experiment/useExperimentVideoAssets";
import ApprovedVideoCreativeAction from "./ApprovedVideoCreativeAction";

const experiment = {
  id: "91",
  followUpActionUrl: "https://landing.test",
  status: "PLANNED",
  adCopy: JSON.stringify({
    adCopy: {
      primaryTextVariants: [
        {
          headline: "Seu primeiro ajuste",
          primaryText: "Use o que já tem.",
          description: "Dia 1 gratuito",
        },
      ],
    },
  }),
} as Experiment;
const video = {
  id: 38,
  slot: "AD",
  status: "READY",
  reviewStatus: "APPROVED",
  assetUrl: "https://media.test/approved.mp4",
} as ExperimentVideoAsset;
const rejected = {
  id: 37,
  slot: "AD",
  reviewStatus: "REJECTED",
  requiredForRelease: true,
} as ExperimentVideoAsset;

function mount(locked = false) {
  render(
    <QueryClientProvider
      client={
        new QueryClient({
          defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
          },
        })
      }
    >
      <MemoryRouter>
        <ApprovedVideoCreativeAction
          experiment={experiment}
          video={video}
          videos={[
            video,
            rejected,
            { ...rejected, id: 39, slot: "LANDING_HERO" },
          ]}
          locked={locked}
        />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("ApprovedVideoCreativeAction", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("usa copy persistida e exige seleção explícita da substituição sem enviar preço, destino ou aprovação", async () => {
    const post = vi
      .spyOn(axios, "post")
      .mockResolvedValue({ data: { id: 301 } });
    mount();
    fireEvent.click(
      screen.getByRole("button", { name: "Usar vídeo em anúncio" }),
    );
    expect(
      (screen.getByLabelText("Título do anúncio") as HTMLInputElement).value,
    ).toBe("Seu primeiro ajuste");
    expect(screen.queryByRole("option", { name: /Vídeo #39/ })).toBeNull();
    fireEvent.change(screen.getByRole("combobox"), { target: { value: "37" } });
    fireEvent.click(
      screen.getByRole("button", { name: "Cadastrar e enviar para revisão" }),
    );
    await screen.findByText(/Anúncio #301 cadastrado/);
    expect(post).toHaveBeenCalledTimes(1);
    expect(post).toHaveBeenCalledWith(
      "/api/experiments/91/video-assets/38/creative",
      {
        headline: "Seu primeiro ajuste",
        primaryText: "Use o que já tem.",
        description: "Dia 1 gratuito",
        replacesVideoAssetId: 37,
      },
    );
    expect(
      screen.getByRole("link", { name: "Criativos" }).getAttribute("href"),
    ).toBe("/experiments/91?tab=creatives");
  });

  it("preserva conteúdo e explica erro de backend antes de permitir repetição", async () => {
    const post = vi
      .spyOn(axios, "post")
      .mockRejectedValueOnce({
        isAxiosError: true,
        response: { data: { message: "Vídeo não aprovado" } },
      })
      .mockResolvedValueOnce({ data: { id: 302 } });
    mount();
    fireEvent.click(
      screen.getByRole("button", { name: "Usar vídeo em anúncio" }),
    );
    fireEvent.click(
      screen.getByRole("button", { name: "Cadastrar e enviar para revisão" }),
    );
    await screen.findByRole("alert");
    expect(screen.getByText("Vídeo não aprovado")).toBeTruthy();
    expect(screen.queryByText(/Anúncio #\d+ cadastrado/)).toBeNull();
    fireEvent.click(
      screen.getByRole("button", { name: "Cadastrar e enviar para revisão" }),
    );
    await waitFor(() => expect(post).toHaveBeenCalledTimes(2));
    await screen.findByText(/Anúncio #302 cadastrado/);
  });

  it("não permite cadastro em experimento bloqueado", () => {
    mount(true);
    expect(
      (
        screen.getByRole("button", {
          name: "Usar vídeo em anúncio",
        }) as HTMLButtonElement
      ).disabled,
    ).toBe(true);
  });
});
