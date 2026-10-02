import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import axios from "axios";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { BusinessProcessChainSave } from "../../api/businessProcessChain/types";
import BusinessProcessChainsPage from "./BusinessProcessChainsPage";

vi.mock("axios");
afterEach(cleanup);

describe("BusinessProcessChainsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("apresenta a cadeia e seus processos na ordem informada pelo backend", async () => {
    vi.mocked(axios.get).mockImplementation(async (url) => {
      if (url === "/api/business-process-chains/learning-cycles/v1/catalog")
        return { data: { version: 1, diagram: { nodes: [], flows: [] } } };

      if (url === "/api/business-process-chains/catalog") {
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
    ).toHaveAttribute("href", "/business-processes?processId=11&chainId=1");
    expect(
      screen.getByRole("link", {
        name: "Abrir atividades de Plano Comercial e oferta PDE no diagrama BPM",
      }),
    ).toHaveAttribute(
      "href",
      "/business-processes/retired?processId=12&chainId=1",
    );
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
      if (url === "/api/business-process-chains/learning-cycles/v1/catalog")
        return { data: { version: 1, diagram: { nodes: [], flows: [] } } };

      if (url === "/api/business-process-chains/catalog")
        return { data: summaries };
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

  it("preserva a referência histórica mesmo fora da lista operacional", async () => {
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
      if (url === "/api/business-process-chains/learning-cycles/v1/catalog")
        return { data: { version: 1, diagram: { nodes: [], flows: [] } } };

      if (url === "/api/business-process-chains/catalog") {
        return { data: [currentChain] };
      }
      if (url === "/api/business-process-chains/3") {
        return {
          data: {
            ...currentChain,
            id: 3,
            versionNumber: 3,
            status: "RETIRED",
            createdAt: "2026-08-22",
            processes: [],
          },
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

    expect(await screen.findByText("Cadeia PDE · v3")).toBeInTheDocument();
    expect(axios.get).toHaveBeenCalledWith("/api/business-process-chains/3");
    expect(axios.get).not.toHaveBeenCalledWith(
      "/api/business-process-chains/4",
    );
  });
});

describe("Versionamento administrativo da cadeia", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("cria, edita e publica pelo contrato existente sem reescrever a cadeia histórica", async () => {
    const original = {
      id: 41,
      chainCode: "test-chain",
      name: "Cadeia local",
      purpose: "Preservar valor",
      outcomeDescription: "Resultado",
      primaryMetric: "Contribuição",
      versionNumber: 3,
      status: "PUBLISHED",
      processCount: 1,
      createdAt: "2026-10-02",
      processes: [
        {
          sequenceNumber: 1,
          processDefinitionId: 101,
          processCode: "test-process",
          name: "Planejamento",
          purpose: "Planejar",
          ownerName: "Atena",
          triggerDescription: "Evidência",
          outcomeDescription: "Hipótese",
          versionNumber: 1,
          status: "PUBLISHED",
          valueContribution: "Aprendizado",
        },
      ],
    };
    let draft: typeof original | undefined;
    vi.mocked(axios.get).mockImplementation(async (url) => {
      if (url === "/api/business-process-chains/catalog")
        return { data: draft ? [draft, original] : [original] };
      if (url === "/api/business-process-chains/41") return { data: original };
      if (url === "/api/business-process-chains/42") return { data: draft };
      if (url === "/api/business-processes")
        return {
          data: [
            {
              id: 102,
              processCode: "test-process",
              name: "Planejamento",
              versionNumber: 2,
              status: "PUBLISHED",
              processType: "VALUE_PROCESS",
            },
          ],
        };
      if (String(url).includes("learning-cycles"))
        return { data: { diagram: { nodes: [], flows: [] } } };
      throw Error(String(url));
    });
    vi.mocked(axios.post).mockImplementation(async (url) => {
      if (url === "/api/business-process-chains/41/draft") {
        draft = { ...original, id: 42, versionNumber: 4, status: "DRAFT" };
        return { data: draft };
      }
      if (url === "/api/business-process-chains/42/publish") {
        draft = { ...draft!, status: "PUBLISHED" };
        return { data: draft };
      }
      throw Error(String(url));
    });
    vi.mocked(axios.put).mockImplementation(async (_url, input) => {
      const value = input as BusinessProcessChainSave;
      draft = {
        ...draft!,
        ...value,
        processes: [
          { ...draft!.processes[0], ...value.processes[0], versionNumber: 2 },
        ],
      };
      return { data: draft };
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    render(
      <MemoryRouter initialEntries={["/?chainId=41"]}>
        <QueryClientProvider
          client={
            new QueryClient({ defaultOptions: { queries: { retry: false } } })
          }
        >
          <BusinessProcessChainsPage />
        </QueryClientProvider>
      </MemoryRouter>,
    );
    fireEvent.click(
      await screen.findByRole("button", {
        name: "Criar nova versão da cadeia",
      }),
    );
    const purpose = await screen.findByLabelText("Propósito da cadeia");
    fireEvent.change(purpose, {
      target: { value: "Cada mudança exige novo ciclo e novo experimento" },
    });
    fireEvent.change(await screen.findByLabelText("Versão do processo 1"), {
      target: { value: "102" },
    });
    fireEvent.change(screen.getByLabelText("Contribuição do processo 1"), {
      target: { value: "Uma hipótese por comparação" },
    });
    expect(
      screen.getByRole("button", { name: "Publicar cadeia" }),
    ).toBeDisabled();
    fireEvent.click(
      screen.getByRole("button", { name: "Salvar rascunho da cadeia" }),
    );
    await screen.findByText(
      "Rascunho salvo. Revise a cadeia antes de publicar.",
    );
    expect(axios.put).toHaveBeenCalledWith(
      "/api/business-process-chains/42",
      expect.objectContaining({
        purpose: "Cada mudança exige novo ciclo e novo experimento",
        processes: [
          {
            processDefinitionId: 102,
            valueContribution: "Uma hipótese por comparação",
          },
        ],
      }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Publicar cadeia" }));
    await screen.findByText(
      "Cadeia publicada. As execuções anteriores mantêm suas versões.",
    );
    expect(original.purpose).toBe("Preservar valor");
    expect(axios.put).toHaveBeenCalledTimes(1);
    expect(axios.post).toHaveBeenCalledTimes(2);
    vi.mocked(window.confirm).mockRestore();
  });

  it("mostra a causa da falha e conserva a versão selecionada", async () => {
    const chain = {
      id: 56,
      name: "Cadeia preservada",
      versionNumber: 8,
      status: "PUBLISHED",
      processes: [],
    };
    vi.mocked(axios.get).mockImplementation(async (url) => ({
      data:
        String(url).endsWith("/catalog") &&
        !String(url).includes("learning-cycles")
          ? [chain]
          : String(url).includes("learning-cycles")
            ? { diagram: { nodes: [], flows: [] } }
            : chain,
    }));
    vi.mocked(axios.isAxiosError).mockReturnValue(true);
    vi.mocked(axios.post).mockRejectedValue({
      response: { data: { detail: "Conflito de versão. Atualize a leitura." } },
    });
    render(
      <MemoryRouter initialEntries={["/?chainId=56"]}>
        <QueryClientProvider
          client={
            new QueryClient({ defaultOptions: { queries: { retry: false } } })
          }
        >
          <BusinessProcessChainsPage />
        </QueryClientProvider>
      </MemoryRouter>,
    );
    fireEvent.click(
      await screen.findByRole("button", {
        name: "Criar nova versão da cadeia",
      }),
    );
    await waitFor(() =>
      expect(screen.getByRole("alert")).toHaveTextContent("Conflito de versão"),
    );
    expect(
      screen.getByRole("heading", { name: "Cadeia preservada · v8" }),
    ).toBeInTheDocument();
    expect(axios.put).not.toHaveBeenCalled();
  });
});
