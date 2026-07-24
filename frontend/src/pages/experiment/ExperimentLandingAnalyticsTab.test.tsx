import { cleanup, render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import ExperimentLandingAnalyticsTab from "./ExperimentLandingAnalyticsTab";

vi.mock("axios");

describe("ExperimentLandingAnalyticsTab", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it("shows the persuasive journey with analytics evidence by tracked section", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/experiments/67/funnel/analytics") {
        return Promise.resolve({
          data: {
            totalEvents: 4,
            totalSessions: 2,
            pageViews: 2,
            sectionViewEvents: 2,
            totalVisibleMs: 18000,
            averageVisibleMsPerSession: 9000,
            deviceBreakdown: [],
            mobileOperatingSystemBreakdown: [],
            screenSizeBreakdown: [],
            sessions: [
              {
                sessionId: "s1",
                eventCount: 2,
                pageViews: 1,
                sectionViewEvents: 1,
                totalVisibleMs: 12000,
                topSections: [
                  {
                    sectionId: "interactive_diagnostic",
                    visibleMs: 12000,
                    events: 1,
                  },
                  {
                    sectionId: "free_diagnostic_preview",
                    visibleMs: 3000,
                    events: 1,
                  },
                ],
              },
              {
                sessionId: "s2",
                eventCount: 2,
                pageViews: 1,
                sectionViewEvents: 1,
                totalVisibleMs: 6000,
                topSections: [],
              },
            ],
          },
        });
      }
      if (
        url === "/api/products/public/metodo-musa-7-dias/pde-persuasive-journey"
      ) {
        return Promise.resolve({
          data: {
            version: "commercial-stages-v1",
            framework: "Funil experiencial PDE",
            steps: [
              {
                stageNumber: 2,
                stage: "diagnostic_value",
                stageName: "Envolvimento diagnóstico",
                psychologicalRole: "Interesse + Desejo",
                trackedSectionIds: [
                  "interactive_diagnostic",
                  "free_diagnostic_preview",
                ],
                commercialFunction:
                  "Questionário e plano de 7 dias aumentam valor percebido.",
                primaryMetric: "questionário concluído e plano visualizado",
                optimizationRule:
                  "reduzir fricção da primeira pergunta e tornar o plano mais concreto",
              },
            ],
          },
        });
      }
      return Promise.reject(new Error(`URL inesperada: ${url}`));
    });
    const client = new QueryClient();

    render(
      <QueryClientProvider client={client}>
        <ExperimentLandingAnalyticsTab experimentId="67" />
      </QueryClientProvider>,
    );

    expect(
      await screen.findByText(/Jornada persuasiva interativa/i),
    ).toBeTruthy();
    expect(
      screen.getByText(/Estágio 2: Envolvimento diagnóstico/i),
    ).toBeTruthy();
    expect(screen.getByText(/Interesse \+ Desejo/i)).toBeTruthy();
    expect(
      screen.getAllByText(/interactive_diagnostic/i).length,
    ).toBeGreaterThan(0);
    expect(screen.getByText(/50.0% do tráfego/i)).toBeTruthy();
    expect(screen.getByText(/tornar o plano mais concreto/i)).toBeTruthy();
  });

  it("shows average visible time per PDE session instead of total visible time", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/experiments/67/post-deploy-monitor") {
        return Promise.resolve({
          data: {
            experimentId: 67,
            productSlug: "metodo-musa-7-dias",
            generatedAt: "2026-07-23T12:00:00Z",
            decision: "PAUSE_AND_FIX",
            decisionLabel: "Pausar e corrigir",
            recommendation: "Ajustar primeira dobra.",
            metaAds: {},
            pde: {
              available: true,
              status: "ONLINE",
              currentExperienceVersion: "musa-pde-entry-v4-video-hero",
              totalEvents: 181,
              uniqueVisitors: 23,
              sessions: 24,
              pdeEntries: 24,
              pageViews: 24,
              presenceMapClicks: 0,
              diagnosticClicks: 0,
              fieldFilled: 0,
              loginStarted: 0,
              loginCompleted: 0,
              paywallViewed: 0,
              subscriptionClicked: 0,
              checkoutStarted: 0,
              subscriptionApproved: 0,
              totalVisibleMs: 934000,
              averageVisibleMsPerSession: 38916,
              events: {},
              experienceVersions: [],
              trafficSources: [],
              deviceBreakdown: [],
              screenSizeBreakdown: [],
              recentJourneys: [],
            },
            pdePromotionControl: {
              homologAvailable: true,
              productionAvailable: true,
              productionBehind: false,
              productionUpToDate: true,
              productionDeployAvailable: false,
              productionDeployBlocked: false,
              statusLabel: "Atualizado",
              recommendation: "Monitorar",
              targetEnvironment: "production",
              workflowFile: "pde-platform-deploy.yml",
            },
            pdeProductionSlots: [],
            pdeDeployments: [],
            logs: {
              totalLogs: 0,
              errorLogs: 0,
              recentErrors: [],
            },
            alerts: [],
          },
        });
      }
      if (
        url === "/api/products/public/metodo-musa-7-dias/pde-persuasive-journey"
      ) {
        return Promise.resolve({ data: { steps: [] } });
      }
      return Promise.reject(new Error(`URL inesperada: ${url}`));
    });
    const client = new QueryClient();

    render(
      <QueryClientProvider client={client}>
        <ExperimentLandingAnalyticsTab
          experimentId="67"
          experimentType="PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL"
        />
      </QueryClientProvider>,
    );

    expect(await screen.findByText("Tempo médio/sessão")).toBeTruthy();
    expect(screen.getByText("39s")).toBeTruthy();
    expect(screen.queryByText("15min 34s")).toBeNull();
  });
});
