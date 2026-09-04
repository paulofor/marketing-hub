import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import ResearchIntelligenceLibraryPage from "./ResearchIntelligenceLibraryPage";

vi.mock("axios");

const catalog = {
  contractVersion: "HARNESS_RESEARCH_INTELLIGENCE_V1",
  evaluatedOn: "2026-09-03",
  totalCompiledCards: 2,
  activeCards: 1,
  agentPolicies: [
    {
      agentKey: "videomaker",
      agentName: "Apolo",
      purpose: "Orientar roteiro, ritmo e áudio.",
      authority: "PRODUCTION_ADVISORY",
      collections: ["video", "prazer-audio-visual"],
      maxCardsPerContext: 4,
    },
  ],
  cards: [
    {
      cardId: "RI1-AAAAAAAAAAAA",
      collection: "video",
      title: "Gancho visual reconhecível",
      finding: "O primeiro quadro precisa materializar a dor.",
      mechanism: "Antecipação visual.",
      commercialApplication: "Abrir com uma situação concreta.",
      evidenceStrength: "Evidência externa.",
      publishedOn: "2026-08-31",
      validUntil: "2026-10-15",
      experimentHypothesis: "Aumentar retenção em três segundos.",
      risks: "Generalização.",
      limits: "Não substitui evento humano.",
      sourcePath: "pesquisas/video/2026-08-31-gancho.md",
      sourceSha256: "a".repeat(64),
      evidenceKind: "EXTERNAL_RESEARCH",
    },
    {
      cardId: "RI1-BBBBBBBBBBBB",
      collection: "neuromarketing",
      title: "Artigo histórico vencido",
      finding: "Achado antigo.",
      mechanism: "Mecanismo antigo.",
      commercialApplication: "Não usar sem renovar.",
      evidenceStrength: "Evidência externa.",
      publishedOn: "2024-01-01",
      validUntil: "2025-01-01",
      experimentHypothesis: "Renovar a pesquisa.",
      risks: "Fonte vencida.",
      limits: "Não selecionar.",
      sourcePath: "pesquisas/neuromarketing/2024-01-01-antigo.md",
      sourceSha256: "b".repeat(64),
      evidenceKind: "EXTERNAL_RESEARCH",
    },
  ],
  limitations: ["Cartões não comprovam venda."],
};

function setup() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ResearchIntelligenceLibraryPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("ResearchIntelligenceLibraryPage", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    (axios.get as any).mockResolvedValue({ data: catalog });
  });

  afterEach(() => cleanup());

  it("mostra o catálogo global e a política aplicável a todos os projetos", async () => {
    setup();

    expect(
      await screen.findByRole("heading", {
        name: /biblioteca de inteligência do harness/i,
      }),
    ).toBeTruthy();
    expect(axios.get).toHaveBeenCalledWith(
      "/api/research-intelligence/v1/catalog",
    );
    expect(await screen.findByText("Apolo", { selector: "strong" })).toBeTruthy();
    expect(screen.getByText("Todos", { selector: "strong" })).toBeTruthy();
    expect(screen.getByText(/projetos atuais e futuros/i)).toBeTruthy();
    expect(screen.getByText(/gancho visual reconhecível/i)).toBeTruthy();
    expect(screen.queryByText(/artigo histórico vencido/i)).toBeNull();
  });

  it("permite consultar fontes vencidas sem torná-las elegíveis", async () => {
    const user = userEvent.setup();
    setup();
    await screen.findByText(/gancho visual reconhecível/i);

    await user.selectOptions(screen.getByLabelText(/validade/i), "ALL");

    expect(screen.getByText(/artigo histórico vencido/i)).toBeTruthy();
    expect(screen.getByText("Vencido")).toBeTruthy();
  });
});
