import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import App from "../App";
import axios from "axios";

vi.mock("axios");

function mockApi() {
  (axios.get as any).mockImplementation((url: string) => {
    if (url === "/api/niches") {
      return Promise.resolve({ data: [{ id: 1, name: "Fitness" }] });
    }
    if (url === "/api/niches/summary") {
      return Promise.resolve({
        data: {
          items: [
            {
              id: 1,
              name: "Fitness",
              enrichedNicheProfileId: null,
              totalCost: 0,
              pipelineHypothesesCount: 1,
              experimentsCount: 1,
            },
          ],
          totalElements: 1,
          totalPages: 1,
          page: 0,
          size: 30,
        },
      });
    }
    if (url.startsWith("/api/niches/1/hypotheses")) {
      return Promise.resolve({
        data: [
          {
            id: "10",
            title: "Hip 1",
            offerType: "LEAD",
            status: "BACKLOG",
            kpiTargetCpl: 5,
            createdAt: "",
            framework: {
              pain: { summary: "Dor principal" },
              result: { summary: "Resultado esperado" },
              mechanism: { summary: "Mecanismo claro" },
              proof: { summary: "Prova objetiva" },
              offer: { summary: "Oferta direta" },
              checklist: {},
            },
          },
        ],
      });
    }
    if (url.startsWith("/api/niches/1/experiments")) {
      return Promise.resolve({
        data: [
          {
            id: 100,
            nicheId: 1,
            hypothesisId: "10",
            name: "Exp 1",
            hypothesis: "Hip 1",
            kpiTarget: 5,
            startDate: null,
            endDate: null,
            status: "RUNNING",
            platform: "FACEBOOK",
            createdAt: "",
            updatedAt: "",
            instagramAccount: null,
          },
        ],
      });
    }
    if (url === "/api/niches/1") {
      return Promise.resolve({ data: { id: 1, name: "Fitness" } });
    }
    if (url === "/api/experiments/100") {
      return Promise.resolve({
        data: {
          id: 100,
          nicheId: 1,
          hypothesisId: "10",
          name: "Exp 1",
          hypothesis: "Hip 1",
          kpiTarget: 5,
          startDate: null,
          endDate: null,
          status: "RUNNING",
          platform: "FACEBOOK",
          createdAt: "",
          updatedAt: "",
          instagramAccount: null,
        },
      });
    }
    return Promise.resolve({ data: [] });
  });
}

describe("niche navigation", () => {
  it("navigates from niche list to generated hypothesis detail", async () => {
    mockApi();
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <MemoryRouter initialEntries={["/niches"]}>
          <App />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const user = userEvent.setup();
    await screen.findByText("Fitness");
    await user.click(screen.getByRole("link", { name: "Detalhes" }));
    await screen.findByText("Hipóteses do nicho");
    expect(screen.queryByRole("link", { name: /^criar hipótese$/i })).toBeNull();
    await user.click(screen.getByRole("link", { name: /entrar/i }));
    await screen.findByText("Dor");
    await screen.findByText("Resultado");
    await screen.findByText("Mecanismo");
    await screen.findByText("Prova");
    await screen.findByText("Oferta");
    await screen.findByText("Criar Experimento");
  });
});
