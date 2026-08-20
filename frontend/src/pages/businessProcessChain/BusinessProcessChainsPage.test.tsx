import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import axios from "axios";
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
                status: "PUBLISHED",
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
      <QueryClientProvider client={client}>
        <BusinessProcessChainsPage />
      </QueryClientProvider>,
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
    expect(axios.get).toHaveBeenCalledWith("/api/business-process-chains/1");
  });
});
