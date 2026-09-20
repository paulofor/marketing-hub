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
          humanVisitors: 4,
          pageViews: 42,
          partialVideoViews: 12,
          completeVideoViews: 4,
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
        sampleDecision: {
          applicable: true,
          measurementAvailable: true,
          measurementSource: "PDE_ATTRIBUTED_HUMAN_COHORT",
          status: "INSUFFICIENT_DATA",
          headline: "Amostra comercial ainda insuficiente",
          explanation:
            "4 de 100 visitantes humanos distintos atribuídos; ainda faltam 96 para a primeira decisão.",
          humanVisitors: 4,
          initialTargetVisitors: 100,
          visitorsRemainingForInitialDecision: 96,
          targetPurchasesAtInitialDecision: 5,
          precisionTargetVisitors: 500,
          visitorsRemainingForPrecisionDecision: 496,
          purchases: 0,
          observedPurchaseRatePercent: 0,
          confidenceLower95Percent: 0,
          confidenceUpper95Percent: 60.24,
          zeroPurchaseUpper95Percent: 52.71,
          estimatedCostPerHumanVisitor: 1.47,
          projectedSpendForInitialTarget: 147.25,
          projectedSpendForPrecisionTarget: 736.25,
          zeroPrimaryResultStopSpend: 25,
          mediaSpendLimit: 100,
          initialTargetFitsMediaSpendLimit: false,
          precisionTargetFitsMediaSpendLimit: false,
          projectionConfidence: "PRELIMINARY",
          financialGuardrail:
            "O teto atual protege o caixa, mas não financia a primeira amostra no custo observado; qualquer ampliação exige nova autorização.",
          recommendation:
            "Preservar versão, preço, público e oferta até a primeira decisão.",
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
          code: "VIDEO_SEM_PROXIMO_PASSO",
          title: "Vídeo engajou, mas não levou ao próximo passo",
          severity: "warning",
          diagnosis:
            "Há consumo parcial ou completo do vídeo sem avanço para formulário, login, paywall ou checkout.",
          commercialImpact:
            "A promessa consegue prender atenção, mas a ponte para monetização ainda não está clara ou forte o suficiente.",
          recommendedFocus:
            "Reforçar CTA pós-vídeo, promessa de continuidade e transição para diagnóstico, login ou pagamento.",
        },
        learnings: ["O vídeo reteve atenção, mas a página não monetizou."],
        nextActions: [
          {
            code: "REFORCAR_CTA_POS_VIDEO",
            label: "Reforçar CTA pós-vídeo",
            rationale:
              "O vídeo reteve atenção; a próxima alavanca é transformar esse momento em clique para diagnóstico, login ou compra.",
            targetRoute: "/experiments/67",
          },
        ],
      },
    });

    renderPage();

    expect(await screen.findByText("Cockpit do Experimento")).toBeTruthy();
    expect(
      screen.getByText("Vídeo engajou, mas não levou ao próximo passo"),
    ).toBeTruthy();
    expect(screen.getByText("Reforçar CTA pós-vídeo")).toBeTruthy();
    expect(screen.getByText("Vídeos completos")).toBeTruthy();
    expect(screen.getByText("Amostra comercial ainda insuficiente")).toBeTruthy();
    expect(screen.getByText("4 / 100")).toBeTruthy();
    expect(screen.getByText("0 / 5")).toBeTruthy();
    expect(screen.getByText("4 / 500")).toBeTruthy();
    expect(screen.getByText("Projeção preliminar")).toBeTruthy();
    expect(screen.getByText(/Parada automática sem resultado primário:/)).toBeTruthy();
    expect(axios.get).toHaveBeenCalledWith("/api/experiments/67/cockpit");
  });
});
