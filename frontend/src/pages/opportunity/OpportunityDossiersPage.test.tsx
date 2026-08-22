import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import OpportunityDossiersPage from "./OpportunityDossiersPage";

const apiMocks = vi.hoisted(() => ({ action: vi.fn(), create: vi.fn() }));

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

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
        proposedOffer: "Webapp revisável",
        deliveryModel: "Concierge assistido",
        knownRisks: "Duplicar solução gratuita",
        experimentRecommendation: "Comparar três formatos",
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
  useCreateOpportunityDossier: () => ({
    isPending: false,
    mutate: apiMocks.create,
  }),
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
    expect(screen.getByText("Concierge assistido")).toBeInTheDocument();
    expect(screen.getByText("Duplicar solução gratuita")).toBeInTheDocument();
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

  /** Preserva modelo de entrega e riscos no contrato criado pelo formulário. */
  it("envia os campos estratégicos aceitos pelo backend", () => {
    render(<OpportunityDossiersPage />);
    fireEvent.change(screen.getByLabelText("Título *"), {
      target: { value: "Propostas claras" },
    });
    fireEvent.change(screen.getByLabelText("Público *"), {
      target: { value: "Prestadores locais" },
    });
    fireEvent.change(screen.getByLabelText("Dor principal *"), {
      target: { value: "Retrabalho em propostas" },
    });
    fireEvent.change(
      screen.getByLabelText("Produto comprovado de referência *"),
      { target: { value: "Softwares de orçamento" } },
    );
    fireEvent.change(screen.getByLabelText("Como a IA entrega melhor *"), {
      target: { value: "Gera proposta revisável" },
    });
    fireEvent.change(screen.getByLabelText("Oferta preliminar"), {
      target: { value: "Comparar kit, webapp e concierge" },
    });
    fireEvent.change(screen.getByLabelText("Modelo de entrega candidato"), {
      target: { value: "Concierge em 48 horas" },
    });
    fireEvent.change(screen.getByLabelText("Riscos conhecidos"), {
      target: { value: "Não superar alternativas gratuitas" },
    });
    fireEvent.change(screen.getByLabelText("Experimento recomendado"), {
      target: { value: "Cinco casos reais sem mídia" },
    });
    fireEvent.click(screen.getByText("Cadastrar dossiê"));

    expect(apiMocks.create).toHaveBeenCalledWith(
      {
        title: "Propostas claras",
        ownerAgentKey: "ARGOS",
        targetAudience: "Prestadores locais",
        mainPain: "Retrabalho em propostas",
        referenceProduct: "Softwares de orçamento",
        aiAdvantage: "Gera proposta revisável",
        proposedOffer: "Comparar kit, webapp e concierge",
        deliveryModel: "Concierge em 48 horas",
        knownRisks: "Não superar alternativas gratuitas",
        experimentRecommendation: "Cinco casos reais sem mídia",
      },
      expect.any(Object),
    );
  });
});
