import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import axios from "axios";
import { beforeEach, describe, expect, it, vi } from "vitest";
import BusinessProcessesPage from "./BusinessProcessesPage";

vi.mock("axios");

describe("BusinessProcessesPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("apresenta o processo publicado e seus responsáveis a partir do backend", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: [
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
      ],
    });
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
    fireEvent.click(screen.getByRole("button", { name: "Criar versão editável" }));
    expect(screen.getByText("Editar definição do processo")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Psique")).toBeInTheDocument();
    expect(screen.getByDisplayValue("2")).toBeDisabled();
  });

  it("permite excluir uma versão em rascunho após confirmação", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: [
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
      ],
    });
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

    fireEvent.click(await screen.findByRole("button", { name: "Excluir rascunho" }));

    expect(window.confirm).toHaveBeenCalled();
    await waitFor(() =>
      expect(axios.delete).toHaveBeenCalledWith("/api/business-processes/7"),
    );
  });
});
