import { cleanup, render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { afterEach, expect, it, vi } from "vitest";
import axios from "axios";
import FacebookExperimentsReadyPage from "./FacebookExperimentsReadyPage";

vi.mock("axios");
afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});

it("keeps failed publications accessible even when the worker queue is empty", async () => {
  vi.mocked(axios.get).mockImplementation(async (url, config) => ({
    data:
      url === "/api/facebook-campaigns/experiments" &&
      config?.params?.status === "FAILED"
        ? [{ id: 91, name: "Vega QA", missingConfiguration: ["creativeCopy"] }]
        : [],
  }));
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <MemoryRouter>
        <FacebookExperimentsReadyPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
  expect(
    await screen.findByRole("link", { name: "#91 — Vega QA" }),
  ).toHaveAttribute("href", "/experiments/91");
  expect(
    screen.getByText("Corrigir o texto do anúncio antes da publicação"),
  ).toBeTruthy();
  expect(screen.getByText("Nenhum experimento pronto")).toBeTruthy();
});
