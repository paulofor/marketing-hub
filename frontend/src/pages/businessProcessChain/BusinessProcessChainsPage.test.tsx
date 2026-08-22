import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import axios from "axios";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import BusinessProcessChainsPage from "./BusinessProcessChainsPage";

vi.mock("axios");

describe("BusinessProcessChainsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("apresenta a cadeia e seus processos na ordem informada pelo backend", async () => {
    vi.mocked(axios.get).mockImplementation(async (url) => {
      if (url === "/api/business-process-chains") {
        return {
          data: [
            {
              id: 1,
              chainCode: "pde-value-creation-delivery",
              name: "Criação e entrega de valor PDE",
              purpose: "Transformar oportunidade em valor entregue.",
              outcomeDescription: "Venda entregue com satisfação.",
              primaryMetric: "Tempo até venda entregue com satisfação",
              versionNumber: 1,
              status: "PUBLISHED",
              processCount: 2,
              publishedAt: "2026-08-20T10:00:00Z",
            },
          ],
        };
      }
      if (url === "/api/business-process-chains/1") {
        return {
          data: {
            id: 1,
            chainCode: "pde-value-creation-delivery",
            name: "Criação e entrega de valor PDE",
            purpose: "Transformar oportunidade em valor entregue.",
            outcomeDescription: "Venda entregue com satisfação.",
            primaryMetric: "Tempo até venda entregue com satisfação",
            versionNumber: 1,
            status: "PUBLISHED",
            processCount: 2,
            createdAt: "2026-08-20T10:00:00Z",
            publishedAt: "2026-08-20T10:00:00Z",
            processes: [
              {
                sequenceNumber: 1,
                valueContribution: "Escolhe uma dor real.",
                processDefinitionId: 11,
                processCode: "pde-opportunity-discovery",
                name: "Descoberta da oportunidade PDE",
                purpose: "Comprovar demanda.",
                ownerName: "Inteligência de Mercado",
                triggerDescription: "Sinais de oportunidade.",
                outcomeDescription: "Oportunidade aprovada.",
                versionNumber: 1,
                status: "PUBLISHED",
              },
              {
                sequenceNumber: 2,
                valueContribution: "Define uma oferta desejável.",
                processDefinitionId: 12,
                processCode: "pde-commercial-plan-offer",
                name: "Plano Comercial e oferta PDE",
                purpose: "Definir a oferta.",
                ownerName: "Planejamento Comercial",
                triggerDescription: "Oportunidade aprovada.",
                outcomeDescription: "Plano Comercial aprovado.",
                versionNumber: 1,
                status: "RETIRED",
              },
            ],
          },
        };
      }
      throw new Error(`URL inesperada: ${url}`);
    });
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <MemoryRouter>
        <QueryClientProvider client={client}>
          <BusinessProcessChainsPage />
        </QueryClientProvider>
      </MemoryRouter>,
    );

    expect(
      await screen.findByText("Criação e entrega de valor PDE · v1"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Tempo até venda entregue com satisfação"),
    ).toBeInTheDocument();
    const processNames = screen.getAllByRole("heading", { level: 3 });
    expect(processNames.map((item) => item.textContent)).toEqual([
      "Descoberta da oportunidade PDE",
      "Plano Comercial e oferta PDE",
    ]);
    expect(screen.getByText("2 em sequência")).toBeInTheDocument();
    expect(
      screen.getByRole("link", {
        name: "Abrir atividades de Descoberta da oportunidade PDE no diagrama BPM",
      }),
    ).toHaveAttribute("href", "/business-processes?processId=11");
    expect(
      screen.getByRole("link", {
        name: "Abrir atividades de Plano Comercial e oferta PDE no diagrama BPM",
      }),
    ).toHaveAttribute("href", "/business-processes/retired?processId=12");
    expect(axios.get).toHaveBeenCalledWith("/api/business-process-chains/1");
  });

  it("abre diretamente a cadeia indicada pelo link do processo", async () => {
    const summaries = [
      {
        id: 3,
        chainCode: "other-chain",
        name: "Outra cadeia",
        purpose: "Outro objetivo.",
        outcomeDescription: "Outro resultado.",
        primaryMetric: "Outra métrica",
        versionNumber: 1,
        status: "PUBLISHED",
        processCount: 1,
      },
      {
        id: 4,
        chainCode: "pde-value-creation-delivery",
        name: "Cadeia PDE",
        purpose: "Criar valor.",
        outcomeDescription: "Venda entregue.",
        primaryMetric: "Tempo até venda entregue com satisfação",
        versionNumber: 1,
        status: "PUBLISHED",
        processCount: 0,
      },
    ];
    vi.mocked(axios.get).mockImplementation(async (url) => {
      if (url === "/api/business-process-chains") return { data: summaries };
      if (url === "/api/business-process-chains/4") {
        return {
          data: { ...summaries[1], createdAt: "2026-08-20", processes: [] },
        };
      }
      return {
        data: { ...summaries[0], createdAt: "2026-08-20", processes: [] },
      };
    });
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <MemoryRouter initialEntries={["/business-process-chains?chainId=4"]}>
        <QueryClientProvider client={client}>
          <BusinessProcessChainsPage />
        </QueryClientProvider>
      </MemoryRouter>,
    );

    expect(await screen.findByText("Cadeia PDE · v1")).toBeInTheDocument();
    expect(axios.get).toHaveBeenCalledWith("/api/business-process-chains/4");
  });

  it("ignora link antigo para cadeia que não está mais na lista operacional", async () => {
    const currentChain = {
      id: 4,
      chainCode: "pde-value-creation-delivery",
      name: "Cadeia PDE",
      purpose: "Criar valor.",
      outcomeDescription: "Venda entregue.",
      primaryMetric: "Tempo até venda entregue com satisfação",
      versionNumber: 4,
      status: "PUBLISHED",
      processCount: 0,
    };
    vi.mocked(axios.get).mockImplementation(async (url) => {
      if (url === "/api/business-process-chains") {
        return { data: [currentChain] };
      }
      if (url === "/api/business-process-chains/4") {
        return {
          data: { ...currentChain, createdAt: "2026-08-22", processes: [] },
        };
      }
      throw new Error(`Versão obsoleta consultada: ${url}`);
    });
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <MemoryRouter initialEntries={["/business-process-chains?chainId=3"]}>
        <QueryClientProvider client={client}>
          <BusinessProcessChainsPage />
        </QueryClientProvider>
      </MemoryRouter>,
    );

    expect(await screen.findByText("Cadeia PDE · v4")).toBeInTheDocument();
    expect(axios.get).toHaveBeenCalledWith("/api/business-process-chains/4");
    expect(axios.get).not.toHaveBeenCalledWith(
      "/api/business-process-chains/3",
    );
  });
});
