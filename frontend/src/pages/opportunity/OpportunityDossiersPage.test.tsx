import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import OpportunityDossiersPage from "./OpportunityDossiersPage";

const apiMocks = vi.hoisted(() => ({ action: vi.fn() }));

vi.mock("../../api/opportunityDossiers", () => ({
  useOpportunityDossiers: () => ({
    isLoading: false,
    data: [
      {
        id: 1,
        title: "Atendimento IA",
        ownerAgentKey: "ARGOS",
        status: "RESEARCHING",
        targetAudience: "Negócios locais",
        mainPain: "Atendimento lento",
        referenceProduct: "Assistente validado",
        aiAdvantage: "Respostas melhores",
        evidence: [],
        reviews: [
          {
            id: 24,
            agentKey: "ATENA",
            executionStatus: "FAILED",
            errorMessage: "Falha temporária de conexão",
            requestedAt: "2026-08-13T10:00:00Z",
          },
        ],
      },
    ],
  }),
  useCreateOpportunityDossier: () => ({ isPending: false, mutate: vi.fn() }),
  useDossierAction: () => ({ isPending: false, mutate: apiMocks.action }),
}));

/** Responsabilidade: comprovar que o portfólio expõe cadastro, evidências e governança. */
describe("OpportunityDossiersPage", () => {
  /** Exibe a oportunidade e impede que a pesquisa seja confundida com plano. */
  it("apresenta o dossiê e as ações governadas", () => {
    render(<OpportunityDossiersPage />);
    expect(screen.getByText("Oportunidades")).toBeInTheDocument();
    expect(screen.getAllByText("Atendimento IA").length).toBeGreaterThan(0);
    expect(screen.getByText("Solicitar pareceres")).toBeInTheDocument();
    expect(screen.getByText("Adicionar evidência")).toBeInTheDocument();
    expect(screen.getByText("Reenfileirar ATENA")).toBeInTheDocument();
    expect(screen.getByText("Execução #24")).toBeInTheDocument();
    fireEvent.click(screen.getByText("Reenfileirar ATENA"));
    expect(apiMocks.action).toHaveBeenCalledWith({
      id: 1,
      path: "reviews/ATENA/requeue",
      payload: {},
    });
    expect(
      screen.queryByText("Converter em Plano Comercial"),
    ).not.toBeInTheDocument();
  });
});
