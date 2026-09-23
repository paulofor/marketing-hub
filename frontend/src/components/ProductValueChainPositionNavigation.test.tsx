import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import axios from "axios";
import { BrowserRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import ProductValueChainPosition from "./ProductValueChainPosition";

vi.mock("axios");

/** Comprova a troca da sugestão interna pela continuação canônica do macroprocesso. */
describe("Continuidade do card do produto", () => {
  const clients: QueryClient[] = [];

  beforeEach(() => {
    vi.resetAllMocks();
  });
  afterEach(() => {
    cleanup();
    clients.splice(0).forEach((client) => client.clear());
  });

  /** Mira abre o processo 5 e não oferece a landing dispensada depois de concluir o processo 4. */
  it("mostra o botão azul para o próximo macroprocesso informado pelo backend", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        productId: 10,
        selectedProcessDefinitionId: 85,
        processName: "Comunicação e jornada de venda do PDE",
        currentActivityId: null,
        objectiveAchieved: true,
        activities: [],
      },
    });
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    clients.push(client);

    render(
      <QueryClientProvider client={client}>
        <BrowserRouter>
          <ProductValueChainPosition
            productName="Mira"
            position={{
              productId: 10,
              commercialStatus: "COMUNICACAO_E_JORNADA",
              resolutionStatus: "IDENTIFIED",
              resolutionMessage: "Posição identificada.",
              chainDefinitionId: 19,
              chainName:
                "Criação e entrega de valor de Produtos Digitais Experienciais",
              chainVersion: 19,
              processDefinitionId: 85,
              processCode: "pde-communication-sales-journey",
              processName: "Comunicação e jornada de venda do PDE",
              processVersion: 8,
              sequenceNumber: 4,
              processCount: 6,
              nextProcess: {
                processDefinitionId: 82,
                processCode: "pde-commercial-homologation-activation",
                processName: "Homologação e ativação comercial do PDE",
                processVersion: 8,
                sequenceNumber: 5,
              },
              subprocessPosition: {
                trackingStatus: "RECORDED",
                subprocessCount: 2,
                nextSubprocessDefinitionId: 65,
                nextSubprocessSequenceNumber: 3,
                nextSubprocessCode: "landing-page-generation",
                nextSubprocessName:
                  "Geração e aprovação independente de landing",
                measurements: [],
              },
            }}
          />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    await waitFor(() =>
      expect(
        screen.getByRole("link", { name: "Abrir próximo processo" }),
      ).toHaveAttribute(
        "href",
        "/products/10/value-chain-history/processes/82/activities?chainId=19#process-execution",
      ),
    );
    await waitFor(() =>
      expect(screen.queryByText("Próximo subprocesso")).not.toBeInTheDocument(),
    );
    expect(
      screen.queryByText("Geração e aprovação independente de landing"),
    ).not.toBeInTheDocument();
    expect(axios.post).not.toHaveBeenCalled();
  });
});
