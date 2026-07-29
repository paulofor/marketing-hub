import { render, screen, cleanup, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import React from "react";
import App from "../App";

vi.mock("axios");

const mockedAxios = vi.mocked(axios, true);

function setup(initialEntries: string[]) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={initialEntries}>
        <App />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

describe("pde vps management navigation", () => {
  it("has menu link to /pde/vps", () => {
    setup(["/"]);

    const link = screen
      .getAllByRole("link", { name: /infra dos pdes/i })
      .find((element) => element.getAttribute("href") === "/pde/vps");
    expect(link).toBeTruthy();
  });

  it("renders pde vps management page with fixed costs", async () => {
    mockedAxios.get.mockImplementation(async (url) => {
      if (url === "/api/pde/vps") {
        return {
          data: {
            totalMonthlyCostBrl: 49.9,
            totalServers: 1,
            activeServers: 1,
            servers: [
              {
                id: 10,
                name: "DokeHost PDE principal",
                provider: "DokeHost",
                ipAddress: "163.245.200.7",
                planName: "VPS Linux",
                region: "Brasil",
                vcpuCount: 2,
                ramGb: 4,
                storageGb: 80,
                monthlyCostBrl: 49.9,
                productSlug: "metodo-musa-7-dias",
                environment: "production",
                domains: "v6.clubemusa.com.br",
                status: "ACTIVE",
                notes: "Produção inicial",
              },
            ],
          },
        };
      }
      return { data: [] };
    });

    setup(["/pde/vps"]);

    expect(
      await screen.findByRole("heading", { name: "Infra dos PDEs" }),
    ).toBeTruthy();
    expect(screen.getByText("DokeHost PDE principal")).toBeTruthy();
    expect(screen.getByText(/163\.245\.200\.7/)).toBeTruthy();
    await waitFor(() => {
      expect(screen.getAllByText("R$ 49,90").length).toBeGreaterThan(0);
    });
  });
});
