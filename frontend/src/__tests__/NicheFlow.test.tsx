import { render, screen, within } from "@testing-library/react";
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
    if (url.startsWith("/api/niches/1/hypotheses")) {
      return Promise.resolve({
        data: [
          {
            id: 10,
            title: "Hip 1",
            offerType: "LEAD",
            status: "BACKLOG",
            kpiTargetCpl: 5,
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
        },
      });
    }
    return Promise.resolve({ data: [] });
  });
}

describe("niche navigation", () => {
  it("navigates to experiment detail", async () => {
    mockApi();
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <MemoryRouter initialEntries={["/niches"]}>
          <App />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Fitness");
    userEvent.click(screen.getByText("Fitness"));
    await screen.findByText("Nova Hipótese");
    userEvent.click(screen.getByText("Ver detalhes"));
    await screen.findByText("Criar Experimento");
    userEvent.click(screen.getByText("Abrir"));
    expect(await screen.findByText(/Teste 100/)).toBeTruthy();
    const bc = screen.getByRole("navigation", { name: /breadcrumb/i });
    expect(bc).toBeTruthy();
    expect(within(bc).getByText("Nichos")).toBeTruthy();
    expect(within(bc).getByText("Fitness")).toBeTruthy();
    expect(within(bc).getByText("Hip 1")).toBeTruthy();
    expect(within(bc).getByText("Exp 1")).toBeTruthy();
  });
});
