import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";
import { describe, it, expect, vi } from "vitest";
import NicheListPage from "./NicheListPage";
import axios from "axios";

vi.mock("axios");

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <BrowserRouter>
        <NicheListPage />
      </BrowserRouter>
    </QueryClientProvider>,
  );
}

function pageResponse(items: unknown[], totalPages = 1) {
  return {
    items,
    totalElements: items.length,
    totalPages,
    page: 0,
    size: 30,
  };
}

describe("NicheListPage", () => {
  it("renders table", async () => {
    (axios.get as any).mockResolvedValue({ data: pageResponse([]) });
    renderPage();
    expect(await screen.findByText(/Nenhum nicho encontrado/)).toBeTruthy();
    expect(screen.queryByText(/Novo Nicho/)).toBeNull();
  });

  it("shows detail button without linking the niche name", async () => {
    const niche = {
      id: 1,
      name: "Teste",
      totalCost: 25,
      pipelineHypothesesCount: 3,
      experimentsCount: 2,
      enrichedNicheProfileId: null,
    };
    (axios.get as any).mockResolvedValueOnce({ data: pageResponse([niche]) });
    renderPage();

    expect(await screen.findByText("Detalhes")).toBeTruthy();
    expect(screen.getByText("1")).toBeTruthy();
    expect(screen.getByText("Teste").closest("a")).toBeNull();
    expect(screen.queryByText("Editar")).toBeNull();
    expect(screen.queryByText("Segmentação (I/C/B)")).toBeNull();
    expect(screen.getByText("R$ 25,00")).toBeTruthy();
  });

  it("shows enriched niche link only when the niche has an enriched profile", async () => {
    const enriched = {
      id: 1,
      name: "Com enriquecimento",
      totalCost: 0,
      pipelineHypothesesCount: 1,
      experimentsCount: 1,
      enrichedNicheProfileId: 77,
    };
    const plain = {
      ...enriched,
      id: 2,
      name: "Sem enriquecimento",
      enrichedNicheProfileId: null,
    };
    (axios.get as any).mockResolvedValueOnce({
      data: pageResponse([enriched, plain]),
    });
    renderPage();

    const links = await screen.findAllByRole("link", {
      name: "Nicho enriquecido",
    });
    expect(links).toHaveLength(1);
    expect(links[0].getAttribute("href")).toBe(
      "/oprm/enriched-niches/profile/77",
    );
  });
});
