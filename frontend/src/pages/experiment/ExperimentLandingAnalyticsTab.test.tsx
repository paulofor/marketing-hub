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
});
