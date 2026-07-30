import { cleanup, render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import axios from "axios";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import MicroserviceListPage from "./MicroserviceListPage";

vi.mock("axios");

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <MicroserviceListPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("MicroserviceListPage", () => {
  it("mostra o DNS conhecido dos slots produtivos do Clube MUSA", async () => {
    (axios.get as any).mockResolvedValue({
      data: [
        {
          id: 1,
          code: "pde-musa-v6",
          name: "Clube MUSA PDE v6",
          type: "PDE",
          baseUrl: "https://v6.clubemusa.com.br",
          healthPath: "/healthz",
          publishedVersion: "musa-pde-entry-v6-video-motivacional",
          enabled: true,
          criticality: "CRITICAL",
          offlineThresholdSeconds: 120,
        },
      ],
    });

    renderPage();

    expect(await screen.findByText("Clube MUSA PDE v6")).toBeInTheDocument();
    expect(await screen.findByText("163.245.200.7")).toBeInTheDocument();
    expect(
      await screen.findByText(/ativo v6 · proxy -> 5177/i),
    ).toBeInTheDocument();
  });
});
