import { cleanup, render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import axios from "axios";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import OperationalInventoryPage from "./OperationalInventoryPage";

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
        <OperationalInventoryPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("OperationalInventoryPage", () => {
  it("exibe ação de edição para cada host VPS", async () => {
    (axios.get as any).mockResolvedValue({
      data: {
        services: [],
        deployments: [],
        hosts: [
          {
            host: "191.252.210.83",
            providerName: "Locaweb",
            cpu: "4 vCPU",
            memoryGb: 8,
            diskGb: 160,
            operatingSystem: "Ubuntu",
            monthlyCostBrl: 149.9,
          },
        ],
      },
    });

    renderPage();

    expect(await screen.findByText("191.252.210.83")).toBeInTheDocument();
    const editLink = screen.getByRole("link", { name: /editar/i });
    expect(editLink).toHaveAttribute(
      "href",
      "/microservices/vps-inventory/191.252.210.83/edit",
    );
  });
});
