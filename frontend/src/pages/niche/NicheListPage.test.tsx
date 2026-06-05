import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";
import { describe, it, expect, vi } from "vitest";
import NicheListPage from "./NicheListPage";
import axios from "axios";

vi.mock("axios");

describe("NicheListPage", () => {
  it("renders table", async () => {
    (axios.get as any).mockResolvedValue({ data: [] });
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <BrowserRouter>
          <NicheListPage />
        </BrowserRouter>
      </QueryClientProvider>,
    );
    expect(await screen.findByText(/Novo Nicho/)).toBeTruthy();
  });

  it("shows detail button", async () => {
    const niche = {
      id: 1,
      name: "Teste",
      description: "",
      interestCategory: "",
      roleCategory: "",
      demandVolume: "",
      promises: "",
      offers: "",
      baseSegmentation: "",
      interests: "",
      demographicFilters: "",
      extraTips: "",
    };
    (axios.get as any)
      .mockResolvedValueOnce({ data: [niche] })
      .mockResolvedValue({ data: [] });
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <BrowserRouter>
          <NicheListPage />
        </BrowserRouter>
      </QueryClientProvider>,
    );
    expect(await screen.findByText("Detalhes")).toBeTruthy();
  });

  it("shows enriched niche link only when the niche has an enriched profile", async () => {
    const enriched = {
      id: 1,
      name: "Com enriquecimento",
      description: "",
      interestCategory: "",
      roleCategory: "",
      demandVolume: "",
      promises: "",
      offers: "",
      baseSegmentation: "",
      interests: "",
      demographicFilters: "",
      extraTips: "",
      enrichedNicheProfileId: 77,
    };
    const plain = {
      ...enriched,
      id: 2,
      name: "Sem enriquecimento",
      enrichedNicheProfileId: null,
    };
    (axios.get as any)
      .mockResolvedValueOnce({ data: [enriched, plain] })
      .mockResolvedValue({ data: [] });
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <BrowserRouter>
          <NicheListPage />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    const links = await screen.findAllByRole("link", {
      name: "Nicho enriquecido",
    });
    expect(links).toHaveLength(1);
    expect(links[0].getAttribute("href")).toBe(
      "/oprm/enriched-niches/profile/77",
    );
  });
});
