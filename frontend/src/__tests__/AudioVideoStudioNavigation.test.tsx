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

  it("renders audio video studio page", () => {
    setup(<App />, ["/audio-video-studio"]);

    expect(
      screen.getByRole("heading", { name: /estudio de audio e video/i }),
    ).toBeTruthy();
    expect(screen.getByText(/videos com narrativa, som, cenas/i)).toBeTruthy();
    expect(screen.getByText(/video curto de 3 minutos/i)).toBeTruthy();
    expect(
      screen.getByRole("form", { name: /briefing do video de 3 minutos/i }),
    ).toBeTruthy();
    expect(screen.getByText(/estrutura de 3 minutos/i)).toBeTruthy();
    expect(screen.getByText(/plano basico de cenas/i)).toBeTruthy();
    expect(screen.getByText(/checklist de producao/i)).toBeTruthy();
    expect(screen.getByText(/o que continua onde esta/i)).toBeTruthy();
  });
});
