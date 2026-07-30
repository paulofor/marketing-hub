import { render, screen, cleanup } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
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

describe("pde video production navigation", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    (axios.get as any).mockResolvedValue({ data: [] });
  });

  it("has menu link to PDE video production", () => {
    setup(<App />, ["/"]);

    const link = screen.getByRole("link", {
      name: /produção de vídeo pde/i,
    });

    expect(link).toBeTruthy();
    expect(link.getAttribute("href")).toBe("/pde-video-production");
  });

  it("renders the PDE video production process page", () => {
    setup(<App />, ["/pde-video-production"]);

    expect(
      screen.getByRole("heading", { name: /produção de vídeo pde/i }),
    ).toBeTruthy();
    expect(screen.getByText(/briefing científico-comercial/i)).toBeTruthy();
    expect(screen.getByText(/roteiro por cena/i)).toBeTruthy();
    expect(screen.getByText(/storyboard e prompt visual/i)).toBeTruthy();
    expect(screen.getByText(/geração do vídeo/i)).toBeTruthy();
    expect(screen.getByText(/controle de qualidade/i)).toBeTruthy();
    expect(screen.getByText(/aprovação humana/i)).toBeTruthy();
    expect(screen.getByText(/vinculação ao pde versionado/i)).toBeTruthy();
    expect(screen.getByText(/métricas por etapa/i)).toBeTruthy();
    expect(screen.getByRole("heading", { name: /aprendizado/i })).toBeTruthy();
    expect(
      screen.getByRole("link", { name: /estúdio de áudio e vídeo/i }),
    ).toBeTruthy();
    expect(screen.getByText(/diagnóstico iniciado/i)).toBeTruthy();
    expect(screen.getAllByText(/compra/i).length).toBeGreaterThan(0);
  });
});
