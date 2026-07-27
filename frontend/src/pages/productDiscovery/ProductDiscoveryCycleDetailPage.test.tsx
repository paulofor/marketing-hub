import { cleanup, render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import ProductDiscoveryCycleDetailPage from "./ProductDiscoveryCycleDetailPage";

const fetchMock = vi.fn();

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

  render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={["/product-discovery/cycles/12"]}>
        <Routes>
          <Route
            path="/product-discovery/cycles/:cycleId"
            element={<ProductDiscoveryCycleDetailPage />}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("ProductDiscoveryCycleDetailPage", () => {
  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it("shows scientific articles collected for the product mechanism", async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      json: async () => ({
        cycle: {
          id: 12,
          theme: "estilo para mulheres 30+",
          country: "BR",
          language: "pt-BR",
          status: "COMPLETED",
          stageCode: "opportunity-gate",
          decisionSummary:
            "Ciclo pesquisado com evidências públicas e artigos científicos.",
          createdAt: "2026-07-27T00:00:00Z",
          updatedAt: "2026-07-27T00:00:00Z",
        },
        opportunities: [
          {
            id: 7,
            cycleId: 12,
            name: "PDE de alívio para estilo",
            primaryAudience: "Mulheres 30+",
            rootPain: "Insegurança para decidir roupas.",
            pdeExperience: "Diagnóstico guiado.",
            scaleEvidence: "Sinais recorrentes.",
            unmetnessEvidence: "Soluções caras.",
            firstCampaignAngle: "Pare de improvisar.",
            commercialRisk: "Não prometer resultado absoluto.",
            evidenceJson: JSON.stringify({
              publicEvidence: [],
              scientificArticles: [
                {
                  link: "https://pubmed.ncbi.nlm.nih.gov/123456/",
                  originalTitle: "Decision support and behavior change",
                  portugueseTitle:
                    "Suporte à decisão e mudança de comportamento",
                  summary: "Revisão sobre apoio à decisão prática.",
                  mechanismApplication:
                    "Aplicar como base para diagnóstico e recomendação guiada.",
                },
              ],
            }),
            score: 82,
            decision: "APPROVE",
            createdAt: "2026-07-27T00:00:00Z",
            updatedAt: "2026-07-27T00:00:00Z",
          },
        ],
      }),
    } as Response);

    renderPage();

    expect(
      await screen.findByRole("heading", {
        name: /Artigos científicos do mecanismo/i,
      }),
    ).toBeTruthy();
    expect(
      screen.getByText("Suporte à decisão e mudança de comportamento"),
    ).toBeTruthy();
    expect(screen.getByRole("link", { name: /Abrir artigo/i })).toHaveAttribute(
      "href",
      "https://pubmed.ncbi.nlm.nih.gov/123456/",
    );
    expect(screen.getByText(/Aplicação no mecanismo:/i)).toBeTruthy();
  });

  it("warns when the mechanism has no scientific article evidence", async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      json: async () => ({
        cycle: {
          id: 12,
          theme: "estilo para mulheres 30+",
          country: "BR",
          language: "pt-BR",
          status: "COMPLETED",
          stageCode: "opportunity-gate",
          createdAt: "2026-07-27T00:00:00Z",
          updatedAt: "2026-07-27T00:00:00Z",
        },
        opportunities: [
          {
            id: 7,
            cycleId: 12,
            name: "PDE de alívio para estilo",
            primaryAudience: "Mulheres 30+",
            rootPain: "Insegurança para decidir roupas.",
            evidenceJson: JSON.stringify({ publicEvidence: [] }),
            score: 60,
            decision: "RESEARCH_MORE",
            createdAt: "2026-07-27T00:00:00Z",
            updatedAt: "2026-07-27T00:00:00Z",
          },
        ],
      }),
    } as Response);

    renderPage();

    expect(
      await screen.findByText(
        /Nenhum artigo científico candidato foi coletado/i,
      ),
    ).toBeTruthy();
  });
});
