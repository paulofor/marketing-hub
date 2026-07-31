import { cleanup, render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import ExperimentCockpitPage from "./ExperimentCockpitPage";

vi.mock("axios");

afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});

function renderPage() {
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={["/experiments/67/cockpit"]}>
        <Routes>
          <Route
            path="/experiments/:id/cockpit"
            element={<ExperimentCockpitPage />}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("ExperimentCockpitPage", () => {
  it("renders the backend commercial diagnosis and recommended action", async () => {
    (axios.get as any).mockResolvedValueOnce({
      data: {
        experimentId: 67,
        experimentName: "MUSA PDE",
        status: "RUNNING",
        experimentType: "PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL",
        campaignObjective: "SALES",
        scoreboard: {
          spend: 125,
          revenue: 0,
          margin: -125,
          roas: 0,
          impressions: 1200,
          clicks: 60,
          ctr: 5,
          cpc: 2.08,
          pageViews: 42,
          leads: 0,
          checkoutAccesses: 0,
          purchases: 0,
          costPerLead: null,
          costPerCheckoutAccess: null,
          costPerPurchase: null,
        },
        question: {
          pain: "Não consegue usar IA no dia a dia",
          promise: "Transformar IA em experiência prática",
          mechanism: "AI_PERSONALIZED_SAMPLE",
          offer: "Assinatura PDE",
          primaryCta: "Começar agora",
          primaryVariable: "primeira dobra",
          primaryMetric: "compra",
        },
        health: {
          status: "READY",
          headline: "Pronto para leitura",
          description: "Sem bloqueios",
          blockers: [],
        },
        funnel: [
          {
            stage: "ACESSO_FORM_LEAD",
            label: "Acesso ao formulário de lead",
            order: 2,
            totalCount: 42,
            uniqueCount: 40,
            source: "analytics",
          },
        ],
        bottleneck: {
          code: "PAGINA_SEM_CONVERSAO",
          title: "Página recebeu tráfego, mas não converteu",
          severity: "warning",
          diagnosis:
            "Há visualizações de página sem avanço funcional no funil.",
          commercialImpact:
            "O clique chegou, mas primeira dobra, promessa, prova ou formulário não moveram a pessoa.",
          recommendedFocus:
            "Criar nova primeira dobra e reforçar promessa, prova e CTA.",
        },
        learnings: ["O clique chegou, mas a página não converteu."],
        nextActions: [
          {
            code: "NOVA_PRIMEIRA_DOBRA",
            label: "Gerar nova primeira dobra",
            rationale: "O clique chegou, mas a página não moveu a pessoa.",
            targetRoute: "/experiments/67",
          },
        ],
      },
    });

    renderPage();

    expect(await screen.findByText("Cockpit do Experimento")).toBeTruthy();
    expect(
      screen.getByText("Página recebeu tráfego, mas não converteu"),
    ).toBeTruthy();
    expect(screen.getByText("Gerar nova primeira dobra")).toBeTruthy();
    expect(axios.get).toHaveBeenCalledWith("/api/experiments/67/cockpit");
  });
});
