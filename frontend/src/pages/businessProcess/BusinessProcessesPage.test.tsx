import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import axios from "axios";
import { beforeEach, describe, expect, it, vi } from "vitest";
import BusinessProcessesPage from "./BusinessProcessesPage";

vi.mock("axios");

const studio = {
  id: 1,
  resourceCode: "themis-image-studio",
  name: "Estúdio de Imagens de Têmis",
  description: "Cria e edita imagens premium.",
  resourceType: "CONTAINER",
  responsibleAgentKey: "meta-ad-approver",
  executorReference: "themis-image-studio",
  usageInstructions: "Use o pending do backend.",
};

function mockCatalog(processes: unknown[], resources: unknown[] = [studio]) {
  vi.mocked(axios.get).mockImplementation(
    async (url) =>
      ({
        data:
          url === "/api/business-process-execution-resources"
            ? resources
            : processes,
      }) as never,
  );
}

describe("BusinessProcessesPage", () => {
  beforeEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("apresenta o processo publicado e seus responsáveis a partir do backend", async () => {
    mockCatalog([
      {
        id: 1,
        processCode: "landing-page-generation",
        name: "Geração de landing page",
        purpose: "Gerar uma landing aprovada.",
        ownerName: "Operação",
        triggerDescription: "Briefing pronto",
        outcomeDescription: "Landing aprovada",
        versionNumber: 1,
        status: "PUBLISHED",
        technicalReference: "GeraLanding",
        createdAt: "2026-08-14T20:00:00Z",
        publishedAt: "2026-08-14T20:00:00Z",
        diagram: {
          nodes: [
            { id: "start", type: "START", label: "Briefing pronto" },
            {
              id: "customer",
              type: "TASK",
              label: "Avaliação da cliente",
              owner: "Psique",
            },
            { id: "end", type: "END", label: "Landing pronta" },
          ],
          flows: [
            { from: "start", to: "customer" },
            { from: "customer", to: "end" },
          ],
        },
      },
    ]);
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={client}>
        <BusinessProcessesPage />
      </QueryClientProvider>,
    );

    expect(
      await screen.findByText("Geração de landing page · v1"),
    ).toBeInTheDocument();
    expect(screen.getByText("Responsável: Psique")).toBeInTheDocument();
    expect(screen.getByText("3 etapas · 0 gates")).toBeInTheDocument();
    fireEvent.click(
      screen.getByRole("button", { name: "Criar versão editável" }),
    );
    expect(
      screen.getByText("Editar definição do processo"),
    ).toBeInTheDocument();
    expect(screen.getByDisplayValue("Psique")).toBeInTheDocument();
    expect(screen.getByDisplayValue("2")).toBeDisabled();
  });

  it("permite excluir uma versão em rascunho após confirmação", async () => {
    mockCatalog([
      {
        id: 7,
        processCode: "experiment-optimization-copy",
        name: "Operação e otimização de experimento",
        purpose: "Otimizar vendas.",
        ownerName: "Operação",
        triggerDescription: "Experimento ativo",
        outcomeDescription: "Decisão registrada",
        versionNumber: 1,
        status: "DRAFT",
        createdAt: "2026-08-15T00:00:00Z",
        diagram: {
          nodes: [
            { id: "start", type: "START", label: "Início" },
            { id: "task", type: "TASK", label: "Medir" },
            { id: "end", type: "END", label: "Fim" },
          ],
          flows: [
            { from: "start", to: "task" },
            { from: "task", to: "end" },
          ],
        },
      },
    ]);
    vi.mocked(axios.delete).mockResolvedValue({ status: 204 });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={client}>
        <BusinessProcessesPage />
      </QueryClientProvider>,
    );

    fireEvent.click(
      await screen.findByRole("button", { name: "Excluir rascunho" }),
    );

    expect(window.confirm).toHaveBeenCalled();
    await waitFor(() =>
      expect(axios.delete).toHaveBeenCalledWith("/api/business-processes/7"),
    );
  });

  it("vincula o Estúdio de Têmis à atividade e persiste o código oficial", async () => {
    const process = {
      id: 8,
      processCode: "pde-construction-approval",
      name: "Construção e aprovação do PDE",
      purpose: "Construir uma experiência premium.",
      ownerName: "Operação de Produto",
      triggerDescription: "Plano aprovado",
      outcomeDescription: "PDE aprovado",
      versionNumber: 2,
      status: "DRAFT",
      createdAt: "2026-08-20T10:00:00Z",
      diagram: {
        nodes: [
          { id: "start", type: "START", label: "Início" },
          {
            id: "deliverables",
            type: "TASK",
            label: "Produzir entregáveis premium",
            owner: "Têmis",
            executionResourceCode: "themis-image-studio",
          },
          { id: "end", type: "END", label: "Fim" },
        ],
        flows: [
          { from: "start", to: "deliverables" },
          { from: "deliverables", to: "end" },
        ],
      },
    };
    mockCatalog([process]);
    vi.mocked(axios.put).mockImplementation(
      async (_url, value) =>
        ({ data: { ...process, ...(value as object) } }) as never,
    );
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={client}>
        <BusinessProcessesPage />
      </QueryClientProvider>,
    );

    expect(
      await screen.findByText(
        /Recurso obrigatório: Estúdio de Imagens de Têmis/,
      ),
    ).toBeInTheDocument();
    fireEvent.click(
      await screen.findByRole("button", { name: "Editar rascunho" }),
    );
    const resourceSelect = await screen.findByRole("combobox", {
      name: "Recurso especializado de Produzir entregáveis premium",
    });
    fireEvent.change(resourceSelect, { target: { value: "" } });
    fireEvent.change(resourceSelect, {
      target: { value: "themis-image-studio" },
    });
    expect(
      screen.getByText(/Cria e edita imagens premium/),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Salvar rascunho" }));

    await waitFor(() =>
      expect(axios.put).toHaveBeenCalledWith(
        "/api/business-processes/8",
        expect.objectContaining({
          diagram: expect.objectContaining({
            nodes: expect.arrayContaining([
              expect.objectContaining({
                id: "deliverables",
                executionResourceCode: "themis-image-studio",
              }),
            ]),
          }),
        }),
      ),
    );
  });
});
