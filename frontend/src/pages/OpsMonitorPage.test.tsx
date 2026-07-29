import { cleanup, render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import axios from "axios";
import { afterEach, describe, expect, it, vi } from "vitest";
import OpsMonitorPage from "./OpsMonitorPage";

vi.mock("axios");
vi.mock("echarts-for-react", () => ({
  default: () => <div data-testid="availability-chart" />,
}));

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

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
              lastCheckAgeSeconds: 90,
              heartbeatStale: false,
              statusReason: "Último heartbeat dentro da janela esperada",
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

  it("renderiza visão focada em PDE com filtro padrão de versões produtivas", async () => {
    (axios.get as any).mockImplementation((url: string, config?: any) => {
      if (url === "/api/ops-monitor/v1/summary") {
        return Promise.resolve({
          data: {
            online: 8,
            degraded: 0,
            offline: 0,
            unknown: 0,
            openIncidents: 0,
          },
        });
      }
      if (url === "/api/ops-monitor/v1/modules/availability") {
        expect(config?.params).toEqual({
          criticality: "CRITICAL",
          type: "PDE",
        });
        return Promise.resolve({
          data: [
            {
              moduleCode: "pde-musa-v5",
              name: "Clube MUSA PDE v5",
              type: "PDE",
              criticality: "CRITICAL",
              publishedVersion: "musa-pde-entry-v5-video-explicativo",
              productUrl: "https://v5.clubemusa.com.br",
              monitoringUrl: "https://v5.clubemusa.com.br/?mh_monitor=1",
              containerImageVersion:
                "pde-platform-backend / pde-platform-frontend: tag do workflow",
              status: "ONLINE",
              lastCheckedAt: "2026-07-27T10:00:00Z",
              lastResponseTimeMs: 120,
              lastError: null,
              attemptedUrl: "https://v5.clubemusa.com.br/healthz",
              lastCheckAgeSeconds: 60,
              heartbeatStale: false,
              statusReason: "Último heartbeat dentro da janela esperada",
            },
          ],
        });
      }
      if (
        url === "/api/ops-monitor/v1/modules/pde-musa-v5/availability-history"
      ) {
        return Promise.resolve({
          data: [
            {
              date: "2026-07-27",
              totalChecks: 20,
              successfulChecks: 19,
              failedChecks: 1,
              availabilityPercentage: 95,
              offlineSeconds: 120,
              degradedSeconds: 30,
            },
          ],
        });
      }
      if (url === "/api/ops-monitor/v1/incidents/open") {
        return Promise.resolve({ data: [] });
      }
      return Promise.resolve({ data: [] });
    });

    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    render(
      <QueryClientProvider client={client}>
        <OpsMonitorPage
          defaultCriticalityFilter="CRITICAL"
          defaultTypeFilter="PDE"
          title="Saúde PDE 24/7"
          subtitle="Monitoramento das versões PDE produtivas."
          pdeFocus
        />
      </QueryClientProvider>,
    );

    expect(await screen.findByText("Saúde PDE 24/7")).toBeInTheDocument();
    expect(await screen.findAllByText("Clube MUSA PDE v5")).toHaveLength(3);
    expect(
      await screen.findByText("musa-pde-entry-v5-video-explicativo"),
    ).toBeInTheDocument();
    expect(
      await screen.findByText("Abrir sem estatística comercial"),
    ).toHaveAttribute("href", "https://v5.clubemusa.com.br/?mh_monitor=1");
    expect(await screen.findByText("Revalidar agora")).toBeInTheDocument();
    expect(await screen.findByText("Tempo fora")).toBeInTheDocument();
    expect(await screen.findByText("2min")).toBeInTheDocument();
    expect(
      await screen.findByText("Monitoramento 24/7 das versões vendidas."),
    ).toBeInTheDocument();
  });

  it("destaca heartbeat vencido como monitor atrasado", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/ops-monitor/v1/summary") {
        return Promise.resolve({
          data: {
            online: 0,
            degraded: 0,
            offline: 0,
            unknown: 1,
            openIncidents: 0,
          },
        });
      }
      if (url === "/api/ops-monitor/v1/modules/availability") {
        return Promise.resolve({
          data: [
            {
              moduleCode: "backend",
              name: "Backend",
              type: "BACKEND",
              criticality: "CRITICAL",
              status: "UNKNOWN",
              lastCheckedAt: "2026-07-29T04:38:26Z",
              lastResponseTimeMs: 5000,
              lastError:
                "Monitor sem heartbeat recente há 47min; última leitura gravada foi OFFLINE.",
              attemptedUrl:
                "http://191.252.181.168/ops-mh-observability-v2/health",
              lastCheckAgeSeconds: 2820,
              heartbeatStale: true,
              statusReason:
                "Monitor sem heartbeat recente; revalidar antes de tratar como indisponibilidade atual",
            },
          ],
        });
      }
      if (url === "/api/ops-monitor/v1/modules/backend/availability-history") {
        return Promise.resolve({ data: [] });
      }
      if (url === "/api/ops-monitor/v1/incidents/open") {
        return Promise.resolve({ data: [] });
      }
      return Promise.resolve({ data: [] });
    });

    renderPage();

    expect(
      await screen.findByText("Monitor atrasado há 47min"),
    ).toBeInTheDocument();
    expect(
      await screen.findByText(
        "Monitor sem heartbeat recente há 47min; última leitura gravada foi OFFLINE.",
      ),
    ).toBeInTheDocument();
  });
});
