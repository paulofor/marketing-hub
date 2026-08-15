import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react";
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
});
