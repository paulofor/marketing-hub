import { render, screen, cleanup } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import React from "react";
import App from "../App";
import axios from "axios";

vi.mock("axios");

const studioCatalog = {
  characters: [
    {
      key: "musa-natural-editorial",
      name: "Mulher urbana natural",
      status: "Aprovado",
      imageUrl: "/assets/musa-editorial-presenca.png",
      description: "Boa para a v7.",
      reason: "Usar na cena do espelho.",
      bibleText: "Personagem aprovada para cena do espelho.",
    },
    {
      key: "sofia-cabides-rejected",
      name: "Sofia com cabides",
      status: "Reprovado",
      imageUrl: "/assets/musa-diagnostic-slide-2.png",
      description: "Nao usar na v7.",
      reason: "Segura cabides o tempo todo.",
      bibleText: "Personagem reprovada para novos videos.",
    },
  ],
  captionPresets: [
    {
      key: "mobile-high-conversion",
      label: "Legenda alta conversao mobile",
      style: "Texto grande",
      description: "Boa para mobile.",
      planText: "Preset de legenda: alta conversao mobile.",
    },
  ],
};

function setup(ui: React.ReactNode, initialEntries: string[]) {
  const client = new QueryClient();
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={initialEntries}>{ui}</MemoryRouter>
    </QueryClientProvider>,
  );
}

afterEach(() => {
  cleanup();
});

describe("audio video studio navigation", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/studio/catalog") {
        return Promise.resolve({ data: studioCatalog });
      }
      return Promise.resolve({ data: [] });
    });
  });

  it("has menu link to /audio-video-studio", () => {
    setup(<App />, ["/"]);

    const link = screen.getByRole("link", {
      name: /estudio de audio e video/i,
    });

    expect(link).toBeTruthy();
    expect(link.getAttribute("href")).toBe("/audio-video-studio");
  });

  it("has studio projects submenu link", () => {
    setup(<App />, ["/"]);

    const link = screen.getByRole("link", {
      name: /lista de projetos/i,
    });

    expect(link).toBeTruthy();
    expect(link.getAttribute("href")).toBe("/audio-video-studio/projects");
  });

  it("renders audio video studio page", () => {
    setup(<App />, ["/audio-video-studio"]);

    expect(
      screen.getByRole("heading", { name: /estudio de audio e video/i }),
    ).toBeTruthy();
    expect(
      screen.getByText(/padronize o video antes de gerar cenas/i),
    ).toBeTruthy();
    expect(screen.getByText(/180 segundos ou mais/i)).toBeTruthy();
    expect(
      screen.getByRole("form", {
        name: /blueprint operacional de video comercial/i,
      }),
    ).toBeTruthy();
    expect(screen.getByLabelText(/historia inicial/i)).toBeTruthy();
    expect(
      screen.getByRole("button", { name: /criar blueprint/i }),
    ).toBeTruthy();
    expect(screen.getByText(/etapas de producao premium com ia/i)).toBeTruthy();
    expect(screen.getAllByText(/1\. estrategia/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/6\. geracao ia/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/9\. aprendizado/i).length).toBeGreaterThan(0);
    expect(
      screen
        .getByRole("link", { name: /oferta e funil.*1\. estrategia/i })
        .getAttribute("href"),
    ).toBe("#audio-video-stage-estrategia");
    expect(
      screen
        .getByRole("link", { name: /provider.*6\. geracao ia/i })
        .getAttribute("href"),
    ).toBe("#audio-video-stage-provider");
    expect(
      screen
        .getByRole("link", { name: /metricas.*9\. aprendizado/i })
        .getAttribute("href"),
    ).toBe("#audio-video-stage-aprendizado");
    expect(screen.getByText(/3\. biblia visual premium/i)).toBeTruthy();
    expect(screen.getByText(/projetos recentes do estudio/i)).toBeTruthy();
    expect(
      screen.getByRole("heading", { name: /estrutura narrativa/i }),
    ).toBeTruthy();
    expect(screen.getByText(/plano basico de cenas/i)).toBeTruthy();
    expect(screen.getByText(/checklist de producao/i)).toBeTruthy();
    expect(screen.getByText(/o que continua onde esta/i)).toBeTruthy();
  });

  it("renders audio video studio projects page", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/studio/catalog") {
        return Promise.resolve({ data: studioCatalog });
      }

      if (url === "/api/sales-videos/projects") {
        return Promise.resolve({
          data: [
            {
              id: 7,
              title: "Projeto MUSA",
              objective: "Aumentar inicio do diagnostico.",
              targetChannel: "PDE",
              format: "VERTICAL_9_16",
              status: "READY_FOR_SCRIPT",
              createdAt: "2026-07-28T10:00:00Z",
            },
          ],
        });
      }

      return Promise.resolve({ data: [] });
    });

    setup(<App />, ["/audio-video-studio/projects"]);

    expect(
      screen.getByRole("heading", { name: /lista de projetos/i }),
    ).toBeTruthy();
    const projectLink = await screen.findByRole("link", {
      name: /#7 projeto musa/i,
    });
    expect(projectLink.getAttribute("href")).toBe(
      "/audio-video-studio/projects/7",
    );
    expect(screen.getByText(/Pronto para roteiro/i)).toBeTruthy();
    expect(
      screen.getByText(/Vertical para Reels\/TikTok\/Shorts/i),
    ).toBeTruthy();
    expect(screen.getByRole("link", { name: /novo projeto/i })).toBeTruthy();
  });

  it("opens audio video editor with persisted project loaded", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/studio/catalog") {
        return Promise.resolve({ data: studioCatalog });
      }

      if (url === "/api/sales-videos/projects/7") {
        return Promise.resolve({
          data: {
            id: 7,
            title: "Projeto MUSA carregado",
            objective: "Aumentar inicio do diagnostico.",
            storyText: "Historia persistida para continuar edicao.",
            contextType: "PDE",
            videoCategory: "LONG_FORM",
            productionMode: "STORY_FIRST_AUDIO_VIDEO",
            targetChannel: "PDE",
            format: "VERTICAL_9_16",
            status: "READY_FOR_SCRIPT",
            ctaText: "Ver meu plano MUSA",
            visualReferences: "Video HLS da v6",
            primaryMetric: "DIAGNOSTIC_START",
          },
        });
      }

      return Promise.resolve({ data: [] });
    });

    setup(<App />, ["/audio-video-studio/projects/7"]);

    expect(
      await screen.findByDisplayValue(/Projeto MUSA carregado/i),
    ).toBeTruthy();
    expect(
      screen.getByDisplayValue(/Historia persistida para continuar edicao/i),
    ).toBeTruthy();
    expect(
      screen.getByRole("button", { name: /salvar continuidade/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole("link", { name: /voltar para lista de projetos/i }),
    ).toBeTruthy();
  });

  it("shows rendered mp4 for review when project has ready video job", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/studio/catalog") {
        return Promise.resolve({ data: studioCatalog });
      }

      if (url === "/api/sales-videos/projects/7") {
        return Promise.resolve({
          data: {
            id: 7,
            title: "Projeto MUSA carregado",
            objective: "Aumentar inicio do diagnostico.",
            storyText: "Historia persistida para continuar edicao.",
            contextType: "PDE",
            videoCategory: "COMMERCIAL_SHORT",
            productionMode: "AVATAR_EXPLAINER",
            targetChannel: "PDE_HERO_DIAGNOSTIC",
            format: "VERTICAL_9_16",
            status: "READY_FOR_REVIEW",
            targetDurationSeconds: 30,
            salesVideoProfileId: 52,
          },
        });
      }

      if (url === "/api/sales-videos/profiles/52/jobs") {
        return Promise.resolve({
          data: [
            {
              id: 20487,
              profileId: 52,
              providerFamily: "EXTERNAL_VIDEO_MODULE",
              providerName: "HEYGEN",
              jobType: "RENDER",
              status: "VIDEO_READY",
              assetId: 1940,
              finishedAt: "2026-08-01T06:03:43.298027Z",
            },
          ],
        });
      }

      if (url === "/api/media/1940") {
        return Promise.resolve({
          data: {
            id: 1940,
            type: "VIDEO",
            provider: "VIDEO_MODULE",
            status: "READY",
            url: "https://assets.example/musa-v7.mp4",
          },
        });
      }

      return Promise.resolve({ data: [] });
    });

    setup(<App />, ["/audio-video-studio/projects/7"]);

    expect(
      await screen.findByRole("heading", { name: /mp4 gerado para revisao/i }),
    ).toBeTruthy();
    expect(
      await screen.findByText(/job #20487 · heygen · asset #1940/i),
    ).toBeTruthy();
    const mp4Link = screen.getByRole("link", { name: /abrir mp4/i });
    expect(mp4Link.getAttribute("href")).toBe(
      "https://assets.example/musa-v7.mp4",
    );
  });

  it("blocks audio video studio project below three minutes", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/studio/catalog") {
        return Promise.resolve({ data: studioCatalog });
      }

      if (url === "/api/sales-videos/projects/8") {
        return Promise.resolve({
          data: {
            id: 8,
            title: "Projeto curto",
            objective: "Testar corte curto.",
            storyText: "Historia curta para criativo rapido.",
            contextType: "PDE",
            productionMode: "STORY_FIRST_AUDIO_VIDEO",
            targetChannel: "PDE",
            format: "VERTICAL_9_16",
            targetDurationSeconds: 120,
            status: "READY_FOR_SCRIPT",
          },
        });
      }

      return Promise.resolve({ data: [] });
    });

    setup(<App />, ["/audio-video-studio/projects/8"]);

    expect(
      await screen.findByText(
        /video longo ou vsl deve ter 180 segundos ou mais/i,
      ),
    ).toBeTruthy();
    expect(
      screen.getByRole("button", { name: /salvar continuidade/i }),
    ).toBeDisabled();
  });

  it("renders example project when there are no persisted projects", async () => {
    setup(<App />, ["/audio-video-studio/projects"]);

    expect(
      await screen.findByText(/MUSA PDE v6 - video HLS motivacional/i),
    ).toBeTruthy();
    expect(screen.getByText(/Hero HLS da landing/i)).toBeTruthy();
    expect(
      screen.getByText(
        /assets\/hls\/musa-v6-microexperiencia-visivel\/index\.m3u8/i,
      ),
    ).toBeTruthy();
    expect(screen.queryByText(/Nenhum projeto criado ainda/i)).not.toBeTruthy();
  });
});
