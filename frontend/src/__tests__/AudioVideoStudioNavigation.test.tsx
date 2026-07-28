import { render, screen, cleanup } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import React from "react";
import App from "../App";
import axios from "axios";

vi.mock("axios");

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
    (axios.get as any).mockResolvedValue({ data: [] });
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
    expect(screen.getByText(/projeto primeiro, historia primeiro/i)).toBeTruthy();
    expect(screen.getByText(/video curto de 3 minutos/i)).toBeTruthy();
    expect(
      screen.getByRole("form", { name: /briefing do video de 3 minutos/i }),
    ).toBeTruthy();
    expect(screen.getByLabelText(/historia inicial/i)).toBeTruthy();
    expect(
      screen.getByRole("button", { name: /criar projeto exemplo/i }),
    ).toBeTruthy();
    expect(screen.getByText(/projetos recentes do estudio/i)).toBeTruthy();
    expect(screen.getByText(/estrutura de 3 minutos/i)).toBeTruthy();
    expect(screen.getByText(/plano basico de cenas/i)).toBeTruthy();
    expect(screen.getByText(/checklist de producao/i)).toBeTruthy();
    expect(screen.getByText(/o que continua onde esta/i)).toBeTruthy();
  });

  it("renders audio video studio projects page", async () => {
    (axios.get as any).mockImplementation((url: string) => {
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
    expect(screen.getByText(/READY_FOR_SCRIPT/i)).toBeTruthy();
    expect(screen.getByRole("link", { name: /novo projeto/i })).toBeTruthy();
  });

  it("opens audio video editor with persisted project loaded", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/projects/7") {
        return Promise.resolve({
          data: {
            id: 7,
            title: "Projeto MUSA carregado",
            objective: "Aumentar inicio do diagnostico.",
            storyText: "Historia persistida para continuar edicao.",
            contextType: "PDE",
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

  it("renders example project when there are no persisted projects", async () => {
    setup(<App />, ["/audio-video-studio/projects"]);

    expect(
      await screen.findByText(/MUSA PDE v6 - video HLS motivacional/i),
    ).toBeTruthy();
    expect(screen.getByText(/HLS LANDING_HERO/i)).toBeTruthy();
    expect(
      screen.getByText(
        /assets\/hls\/musa-v6-microexperiencia-visivel\/index\.m3u8/i,
      ),
    ).toBeTruthy();
    expect(
      screen.queryByText(/Nenhum projeto criado ainda/i),
    ).not.toBeTruthy();
  });
});
