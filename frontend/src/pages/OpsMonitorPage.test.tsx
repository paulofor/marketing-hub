import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import axios from "axios";
import { describe, expect, it, vi } from "vitest";
import OpsMonitorPage from "./OpsMonitorPage";

vi.mock("axios");
vi.mock("echarts-for-react", () => ({
  default: () => <div data-testid="availability-chart" />,
}));

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <OpsMonitorPage />
    </QueryClientProvider>,
  );
}

describe("OpsMonitorPage", () => {
  it("renderiza resumo, gráfico, alerta e tabela com dados do backend", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/ops-monitor/v1/summary") {
        return Promise.resolve({
          data: {
            online: 1,
            degraded: 0,
            offline: 1,
            unknown: 0,
            openIncidents: 1,
          },
        });
      }
      if (url === "/api/ops-monitor/v1/modules/availability") {
        return Promise.resolve({
          data: [
            {
              moduleCode: "backend",
              name: "Backend",
              type: "CORE",
              criticality: "CRITICAL",
              status: "OFFLINE",
              lastCheckedAt: "2026-06-23T10:00:00Z",
              lastResponseTimeMs: null,
              lastError: "timeout",
              attemptedUrl: "http://191.252.181.168/actuator/health",
            },
          ],
        });
      }
      if (url === "/api/ops-monitor/v1/modules/backend/availability-history") {
        return Promise.resolve({
          data: [
            {
              date: "2026-06-23",
              totalChecks: 10,
              successfulChecks: 9,
              failedChecks: 1,
              availabilityPercentage: 90,
              offlineSeconds: 60,
              degradedSeconds: 0,
            },
          ],
        });
      }
      if (url === "/api/ops-monitor/v1/incidents/open") {
        return Promise.resolve({
          data: [
            {
              id: 1,
              moduleCode: "backend",
              moduleName: "Backend",
              status: "OPEN",
              severity: "CRITICAL",
              startedAt: "2026-06-23T10:00:00Z",
              summary: "Backend indisponível",
            },
          ],
        });
      }
      return Promise.resolve({ data: [] });
    });

    renderPage();

    expect(
      await screen.findByText("Operação / Saúde dos Módulos"),
    ).toBeInTheDocument();
    expect(
      await screen.findByText("Backend está fora do ar."),
    ).toBeInTheDocument();
    expect(await screen.findByText("Backend indisponível")).toBeInTheDocument();
    expect(await screen.findByTestId("availability-chart")).toBeInTheDocument();
    expect(await screen.findByText("timeout")).toBeInTheDocument();
    expect(
      await screen.findByText("http://191.252.181.168/actuator/health"),
    ).toBeInTheDocument();
  });
});
