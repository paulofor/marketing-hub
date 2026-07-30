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
    expect(screen.getByText(/cockpit por produto/i)).toBeTruthy();
    expect(screen.getByLabelText(/produto pde/i)).toBeTruthy();
    expect(
      screen.getByRole("heading", { name: /cockpit do produto pde/i }),
    ).toBeTruthy();
    expect(screen.getByText(/^Abertura$/i)).toBeTruthy();
    expect(screen.getByText(/^Prova$/i)).toBeTruthy();
    expect(screen.getByText(/^Mecanismo$/i)).toBeTruthy();
    expect(screen.getByText(/^Objecao$/i)).toBeTruthy();
    expect(screen.getByText(/^CTA$/i)).toBeTruthy();
    expect(
      screen.getByRole("heading", { name: /briefing comercial/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole("heading", { name: /roteiro e cenas/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole("heading", { name: /storyboard e prompts/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole("heading", { name: /geracao e variacoes/i }),
    ).toBeTruthy();
    expect(
      screen.getAllByRole("heading", { name: /qualidade comercial/i }).length,
    ).toBeGreaterThan(0);
    expect(
      screen.getByRole("heading", { name: /vinculo ao pde/i }),
    ).toBeTruthy();
    expect(screen.getByRole("heading", { name: /distribuicao/i })).toBeTruthy();
    expect(
      screen.getAllByRole("heading", { name: /aprendizado por metrica/i })
        .length,
    ).toBeGreaterThan(0);
    expect(screen.getByRole("link", { name: /abrir estudio/i })).toBeTruthy();
    expect(screen.getAllByText(/hls pronto para pde/i).length).toBeGreaterThan(
      0,
    );
    expect(screen.getAllByText(/compras/i).length).toBeGreaterThan(0);
  });
});
