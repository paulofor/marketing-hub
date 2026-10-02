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
  it("distingue configuração de runtime e custo desconhecido de zero", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        services: [
          { serviceName: "example", hostPort: 18110, containerPort: 8090 },
        ],
        deployments: [],
        hosts: [
          { host: "example.invalid", diskGb: 47, monthlyCostBrl: null },
          { host: "free.invalid", monthlyCostBrl: 0 },
        ],
      },
    });
    renderPage();
    expect(await screen.findByText("example.invalid")).toBeInTheDocument();
    expect(screen.getByText("Não informado")).toBeInTheDocument();
    expect(screen.getAllByText(/R\$\s*0,00/)).toHaveLength(2);
    expect(
      screen.getByText(/1 de 2 hosts com custo informado/),
    ).toHaveTextContent("O custo total permanece desconhecido");
    expect(screen.getByText("47 GiB")).toBeInTheDocument();
    expect(screen.getByText("Portas previstas no Compose")).toBeInTheDocument();
    expect(
      screen.getByText(/Atualizar recarrega o cadastro, sem consultar os VPS/),
    ).toBeInTheDocument();
  });

  it("soma apenas custos informados sem apresentar cobertura parcial como total", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        services: [],
        deployments: [],
        hosts: [
          { host: "a.invalid", monthlyCostBrl: 10.11 },
          { host: "b.invalid", monthlyCostBrl: 20.22 },
          { host: "unknown.invalid", monthlyCostBrl: null },
        ],
      },
    });
    renderPage();
    expect(
      await screen.findByText(/Subtotal mensal conhecido: R\$\s*30,33/),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/2 de 3 hosts com custo informado/),
    ).toHaveTextContent("O custo total permanece desconhecido");
  });

  it("mantém o custo desconhecido quando nenhum host possui valor", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        services: [],
        deployments: [],
        hosts: [{ host: "unknown.invalid" }],
      },
    });
    renderPage();
    expect(
      await screen.findByText("Custo mensal não informado"),
    ).toBeInTheDocument();
    expect(screen.queryByText(/R\$\s*0,00/)).not.toBeInTheDocument();
  });

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
