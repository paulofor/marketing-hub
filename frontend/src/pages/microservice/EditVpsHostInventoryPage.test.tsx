import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import axios from "axios";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import EditVpsHostInventoryPage from "./EditVpsHostInventoryPage";

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
      <MemoryRouter
        initialEntries={["/microservices/vps-inventory/191.252.210.83/edit"]}
      >
        <Routes>
          <Route
            path="/microservices/vps-inventory/:host/edit"
            element={<EditVpsHostInventoryPage />}
          />
          <Route
            path="/microservices/vps-inventory"
            element={<p>Inventário salvo</p>}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("EditVpsHostInventoryPage", () => {
  it("salva as características físicas e financeiras do VPS", async () => {
    (axios.get as any).mockResolvedValue({
      data: {
        host: "191.252.210.83",
        providerName: "Locaweb",
        providerEvidence: "RDAP",
        cpu: "",
        memoryGb: null,
        diskGb: null,
        operatingSystem: "",
        monthlyCostBrl: null,
        billingCycle: "",
        costEvidence: "",
        physicalSpecsEvidence: "",
        notes: "",
      },
    });
    (axios.put as any).mockResolvedValue({
      data: {
        host: "191.252.210.83",
        providerName: "Locaweb",
        providerEvidence: "RDAP",
        cpu: "4 vCPU",
        memoryGb: 8,
        diskGb: 160,
        operatingSystem: "Ubuntu 24.04",
        monthlyCostBrl: 149.9,
        billingCycle: "mensal",
        costEvidence: "Fatura Locaweb",
        physicalSpecsEvidence: "MCP vps_host_inventory",
        notes: "Host dos workers comerciais",
      },
    });

    renderPage();

    expect(await screen.findByDisplayValue("191.252.210.83")).toBeDisabled();
    await userEvent.type(screen.getByLabelText("CPU"), "4 vCPU");
    await userEvent.type(screen.getByLabelText("Memória GB"), "8");
    await userEvent.type(screen.getByLabelText("Disco GB"), "160");
    await userEvent.type(
      screen.getByLabelText("Sistema operacional"),
      "Ubuntu 24.04",
    );
    await userEvent.type(screen.getByLabelText("Custo mensal BRL"), "149.90");
    await userEvent.type(screen.getByLabelText("Ciclo de cobrança"), "mensal");
    await userEvent.type(
      screen.getByLabelText("Evidência de capacidade física"),
      "MCP vps_host_inventory",
    );
    await userEvent.click(screen.getByRole("button", { name: "Salvar" }));

    await waitFor(() => expect(axios.put).toHaveBeenCalledTimes(1));
    expect(axios.put).toHaveBeenCalledWith(
      "/api/microservices/operational-inventory/hosts/191.252.210.83",
      expect.objectContaining({
        cpu: "4 vCPU",
        memoryGb: 8,
        diskGb: 160,
        operatingSystem: "Ubuntu 24.04",
        monthlyCostBrl: 149.9,
        billingCycle: "mensal",
        physicalSpecsEvidence: "MCP vps_host_inventory",
      }),
    );
    expect(await screen.findByText("Inventário salvo")).toBeInTheDocument();
  });
});
