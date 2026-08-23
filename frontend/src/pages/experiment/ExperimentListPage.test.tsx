import { cleanup, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";
import { afterEach, describe, it, expect, vi } from "vitest";
import ExperimentListPage from "./ExperimentListPage";
import axios from "axios";

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
      <BrowserRouter>
        <ExperimentListPage />
      </BrowserRouter>
    </QueryClientProvider>,
  );
}

describe("ExperimentListPage", () => {
  it("renders table", async () => {
    (axios.get as any).mockResolvedValueOnce({ data: [] });
    (axios.get as any).mockResolvedValueOnce({ data: [] });

    renderPage();

    expect(await screen.findByText(/Novo Teste/)).toBeTruthy();
  });

  it("shows the 25 most recent experiments with requested columns and pagination", async () => {
    const experiments = Array.from({ length: 26 }, (_, index) => {
      const id = index + 1;
      return {
        id: String(id),
        nicheId: 10,
        hypothesisId: `hypothesis-${id}`,
        name: `Experimento ${id}`,
        hypothesis: `Hipótese ${id}`,
        sessionDurationSummary: {
          totalSessions: 3,
          averageVisibleMsPerSession: 94000,
          variants: [],
        },
        cost: id,
        startDate: `2026-06-${String(id).padStart(2, "0")}`,
        endDate: null,
        creativeApproved: false,
        status: "PLANNED",
        platform: "FACEBOOK",
        stage: "AD",
        createdAt: `2026-06-${String(id).padStart(2, "0")}T00:00:00Z`,
        updatedAt: `2026-06-${String(id).padStart(2, "0")}T00:00:00Z`,
      };
    });

    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/experiments/summary")
        return Promise.resolve({ data: experiments });
      if (url === "/api/niches") {
        return Promise.resolve({
          data: [
            {
              id: 10,
              name: "Nicho Principal",
              description: "",
              demandVolume: "",
              promises: "",
              offers: "",
              baseSegmentation: "",
              interests: "",
              demographicFilters: "",
              extraTips: "",
            },
          ],
        });
      }
      return Promise.resolve({ data: [] });
    });

    renderPage();

    expect(
      await screen.findByRole("columnheader", { name: "ID do experimento" }),
    ).toBeTruthy();
    expect(
      screen.getByRole("columnheader", { name: "Nome do experimento" }),
    ).toBeTruthy();
    expect(
      screen.getByRole("columnheader", { name: "Data de criação" }),
    ).toBeTruthy();
    expect(screen.getByRole("columnheader", { name: "Nicho" })).toBeTruthy();
    expect(screen.getByRole("columnheader", { name: "Hipótese" })).toBeTruthy();
    expect(
      screen.getByRole("columnheader", { name: "Tempo médio sessão" }),
    ).toBeTruthy();
    expect(screen.getByRole("columnheader", { name: "Status" })).toBeTruthy();
    expect(
      screen.getByRole("columnheader", { name: "Custo e receita" }),
    ).toBeTruthy();
    expect(
      screen.getByRole("columnheader", { name: "Botões/Ações" }),
    ).toBeTruthy();
    expect(await screen.findByText("Experimento 26")).toBeTruthy();
    expect(
      screen.getByText((content) =>
        content.includes(
          "Exibindo 1-25 de 26 experimentos não finalizados, com 25 por página.",
        ),
      ),
    ).toBeTruthy();
    expect(screen.queryByText("Experimento 1")).toBeNull();

    await userEvent.click(screen.getByRole("button", { name: "2" }));

    expect(await screen.findByText("Página 2 de 2")).toBeTruthy();
    expect(await screen.findByText("Experimento 1")).toBeTruthy();
    expect(screen.queryByText("Experimento 26")).toBeNull();
    const row = screen.getByText("Experimento 1").closest("tr");
    expect(row).not.toBeNull();
    expect(
      within(row as HTMLTableRowElement).getByText("01/06/2026"),
    ).toBeTruthy();
    expect(
      within(row as HTMLTableRowElement).getByText("Nicho Principal"),
    ).toBeTruthy();
    expect(
      within(row as HTMLTableRowElement).getByRole("link", {
        name: "Cockpit",
      }),
    ).toHaveAttribute("href", "/experiments/1/cockpit");
    expect(
      within(row as HTMLTableRowElement).getByText("Custo: R$ 1,00 / US$ 0,20"),
    ).toBeTruthy();
    expect(within(row as HTMLTableRowElement).getByText("1m 34s")).toBeTruthy();
  });

  it("shows session duration split by A/B variant when available", async () => {
    const experiments = [
      {
        id: "63",
        nicheId: 10,
        hypothesisId: "hypothesis-63",
        name: "MPAE-H001-E001",
        hypothesis: "Hipótese A/B",
        sessionDurationSummary: {
          totalSessions: 11,
          averageVisibleMsPerSession: 120000,
          variants: [
            {
              variantKey: "A",
              variantName: "Página tradicional",
              sessions: 5,
              averageVisibleMsPerSession: 90000,
            },
            {
              variantKey: "B",
              variantName: "Página com vídeo",
              sessions: 6,
              averageVisibleMsPerSession: 150000,
            },
          ],
        },
        cost: 0.03,
        startDate: "2026-07-09",
        endDate: null,
        creativeApproved: true,
        status: "RUNNING",
        platform: "FACEBOOK",
        stage: "AD",
        createdAt: "2026-07-09T00:00:00Z",
        updatedAt: "2026-07-09T00:00:00Z",
      },
    ];

    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/experiments/summary")
        return Promise.resolve({ data: experiments });
      if (url === "/api/niches") {
        return Promise.resolve({
          data: [
            {
              id: 10,
              name: "Mulheres profissionais",
              description: "",
              demandVolume: "",
              promises: "",
              offers: "",
              baseSegmentation: "",
              interests: "",
              demographicFilters: "",
              extraTips: "",
            },
          ],
        });
      }
      return Promise.resolve({ data: [] });
    });

    renderPage();

    const row = (await screen.findByText("MPAE-H001-E001")).closest("tr");

    expect(row).not.toBeNull();
    expect(within(row as HTMLTableRowElement).getByText("2m 00s")).toBeTruthy();
    expect(
      within(row as HTMLTableRowElement).getByText(
        (_, node) => node?.textContent === "A: 1m 30s",
      ),
    ).toBeTruthy();
    expect(
      within(row as HTMLTableRowElement).getByText("1m 30s", {
        exact: false,
      }),
    ).toBeTruthy();
    expect(
      within(row as HTMLTableRowElement).getByText(
        (_, node) => node?.textContent === "B: 2m 30s",
      ),
    ).toBeTruthy();
    expect(
      within(row as HTMLTableRowElement).getByText("2m 30s", {
        exact: false,
      }),
    ).toBeTruthy();
  });

  it("shows the individual backend cost for each experiment instead of the niche total", async () => {
    const experiments = [
      {
        id: "2",
        nicheId: 10,
        hypothesisId: "hypothesis-2",
        name: "Experimento 2",
        hypothesis: "Hipótese 2",
        cost: 33.18,
        startDate: "2026-06-02",
        endDate: null,
        creativeApproved: false,
        status: "PLANNED",
        platform: "FACEBOOK",
        stage: "AD",
        createdAt: "2026-06-02T00:00:00Z",
        updatedAt: "2026-06-02T00:00:00Z",
      },
      {
        id: "1",
        nicheId: 10,
        hypothesisId: "hypothesis-1",
        name: "Experimento 1",
        hypothesis: "Hipótese 1",
        cost: 66.63,
        startDate: "2026-06-01",
        endDate: null,
        creativeApproved: false,
        status: "PLANNED",
        platform: "FACEBOOK",
        stage: "AD",
        createdAt: "2026-06-01T00:00:00Z",
        updatedAt: "2026-06-01T00:00:00Z",
      },
    ];

    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/experiments/summary")
        return Promise.resolve({ data: experiments });
      if (url === "/api/niches") {
        return Promise.resolve({
          data: [
            {
              id: 10,
              name: "Nicho Principal",
              description: "",
              demandVolume: "",
              promises: "",
              offers: "",
              baseSegmentation: "",
              interests: "",
              demographicFilters: "",
              extraTips: "",
            },
          ],
        });
      }
      return Promise.resolve({ data: [] });
    });

    renderPage();

    const row2 = (await screen.findByText("Experimento 2")).closest("tr");
    const row1 = screen.getByText("Experimento 1").closest("tr");

    expect(row2).not.toBeNull();
    expect(row1).not.toBeNull();
    expect(
      within(row2 as HTMLTableRowElement).getByText(
        "Custo: R$ 33,18 / US$ 6,64",
      ),
    ).toBeTruthy();
    expect(
      within(row1 as HTMLTableRowElement).getByText(
        "Custo: R$ 66,63 / US$ 13,33",
      ),
    ).toBeTruthy();
    expect(screen.queryByText("R$ 99,81")).toBeNull();
  });

  it("prioritizes totalCost when the legacy cost field is zero", async () => {
    const experiments = [
      {
        id: "54",
        nicheId: 29,
        hypothesisId: "hypothesis-54",
        name: "Experimento 54",
        hypothesis: "Painel do Almoço para Marmitas",
        cost: 0,
        totalCost: 0.08,
        campaignMetric: { spend: 0 },
        startDate: "2026-07-03",
        endDate: null,
        creativeApproved: true,
        status: "RUNNING",
        platform: "FACEBOOK",
        stage: "AD",
        createdAt: "2026-07-03T00:00:00Z",
        updatedAt: "2026-07-03T00:00:00Z",
      },
    ];

    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/experiments/summary")
        return Promise.resolve({ data: experiments });
      if (url === "/api/niches") {
        return Promise.resolve({
          data: [
            {
              id: 29,
              name: "Marmitarias",
              description: "",
              demandVolume: "",
              promises: "",
              offers: "",
              baseSegmentation: "",
              interests: "",
              demographicFilters: "",
              extraTips: "",
            },
          ],
        });
      }
      return Promise.resolve({ data: [] });
    });

    renderPage();

    const row = (await screen.findByText("Experimento 54")).closest("tr");

    expect(row).not.toBeNull();
    expect(
      within(row as HTMLTableRowElement).getByText("Custo: R$ 0,08 / US$ 0,02"),
    ).toBeTruthy();
    expect(
      within(row as HTMLTableRowElement).queryByText("R$ 0,00"),
    ).toBeNull();
  });

  it("uses auditable backend cost in the list when it reconciles provider costs", async () => {
    const experiments = [
      {
        id: "69",
        nicheId: 34,
        hypothesisId: "hypothesis-69",
        name: "MUSA-H001-E007",
        hypothesis: "Validar vídeo na primeira dobra",
        cost: 0,
        expense: 0,
        totalCost: 12.18,
        auditableTotalCost: 34.87,
        campaignMetric: { spend: 5.37 },
        startDate: "2026-07-24",
        endDate: null,
        creativeApproved: true,
        status: "RUNNING",
        platform: "FACEBOOK",
        stage: "AD",
        createdAt: "2026-07-24T00:00:00Z",
        updatedAt: "2026-07-24T00:00:00Z",
      },
    ];

    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/experiments/summary")
        return Promise.resolve({ data: experiments });
      if (url === "/api/niches") {
        return Promise.resolve({
          data: [
            {
              id: 34,
              name: "Mulheres urbanas",
              description: "",
              demandVolume: "",
              promises: "",
              offers: "",
              baseSegmentation: "",
              interests: "",
              demographicFilters: "",
              extraTips: "",
            },
          ],
        });
      }
      return Promise.resolve({ data: [] });
    });

    renderPage();

    const row = (await screen.findByText("MUSA-H001-E007")).closest("tr");

    expect(row).not.toBeNull();
    expect(
      within(row as HTMLTableRowElement).getByText(
        "Custo: R$ 34,87 / US$ 6,97",
      ),
    ).toBeTruthy();
  });

  it("prioritizes experiments in commercial validation and highlights cost and revenue in BRL and USD", async () => {
    const experiments = [
      {
        id: "80",
        nicheId: 10,
        hypothesisId: "hypothesis-80",
        name: "Experimento recente planejado",
        hypothesis: "Hipótese planejada",
        cost: 10,
        revenue: 0,
        startDate: "2026-07-24",
        endDate: null,
        creativeApproved: false,
        status: "PLANNED",
        platform: "FACEBOOK",
        stage: "AD",
        createdAt: "2026-07-24T00:00:00Z",
        updatedAt: "2026-07-24T00:00:00Z",
      },
      {
        id: "70",
        nicheId: 10,
        hypothesisId: "hypothesis-70",
        name: "Experimento em validação",
        hypothesis: "Hipótese em execução",
        cost: 25,
        revenue: 100,
        startDate: "2026-07-20",
        endDate: null,
        creativeApproved: true,
        status: "RUNNING",
        platform: "FACEBOOK",
        stage: "AD",
        createdAt: "2026-07-20T00:00:00Z",
        updatedAt: "2026-07-20T00:00:00Z",
      },
    ];

    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/experiments/summary")
        return Promise.resolve({ data: experiments });
      if (url === "/api/niches") {
        return Promise.resolve({
          data: [
            {
              id: 10,
              name: "Nicho Principal",
              description: "",
              demandVolume: "",
              promises: "",
              offers: "",
              baseSegmentation: "",
              interests: "",
              demographicFilters: "",
              extraTips: "",
            },
          ],
        });
      }
      return Promise.resolve({ data: [] });
    });

    renderPage();

    const table = await screen.findByRole("table");
    const rows = within(table).getAllByRole("row").slice(1);
    expect(within(rows[0]).getByText("Experimento em validação")).toBeTruthy();
    expect(within(rows[0]).getByText("Validação comercial")).toBeTruthy();
    expect(
      within(rows[0]).getByText("Custo: R$ 25,00 / US$ 5,00"),
    ).toBeTruthy();
    expect(
      within(rows[0]).getByText("Receita: R$ 100,00 / US$ 20,00"),
    ).toBeTruthy();
  });

  it("hides finalized experiments from the main list", async () => {
    const experiments = [
      {
        id: "71",
        nicheId: 10,
        hypothesisId: "hypothesis-71",
        name: "Experimento em execução",
        hypothesis: "Hipótese em execução",
        cost: 10,
        startDate: "2026-07-24",
        endDate: null,
        creativeApproved: true,
        status: "RUNNING",
        platform: "FACEBOOK",
        stage: "AD",
        createdAt: "2026-07-24T00:00:00Z",
        updatedAt: "2026-07-24T00:00:00Z",
      },
      {
        id: "55",
        nicheId: 10,
        hypothesisId: "hypothesis-55",
        name: "Experimento finalizado",
        hypothesis: "Hipótese finalizada",
        cost: 40,
        startDate: "2026-07-01",
        endDate: "2026-07-02",
        creativeApproved: true,
        status: "FINISHED",
        platform: "FACEBOOK",
        stage: "AD",
        createdAt: "2026-07-01T00:00:00Z",
        updatedAt: "2026-07-02T00:00:00Z",
      },
      {
        id: "53",
        nicheId: 10,
        hypothesisId: "hypothesis-53",
        name: "Experimento com falha",
        hypothesis: "Hipótese com falha",
        cost: 40,
        startDate: "2026-07-01",
        endDate: "2026-07-02",
        creativeApproved: true,
        status: "FAILED",
        platform: "FACEBOOK",
        stage: "AD",
        createdAt: "2026-07-01T00:00:00Z",
        updatedAt: "2026-07-02T00:00:00Z",
      },
      {
        id: "54",
        nicheId: 10,
        hypothesisId: "hypothesis-54",
        name: "Experimento invalidado",
        hypothesis: "Hipótese invalidada",
        cost: 40,
        startDate: "2026-07-01",
        endDate: "2026-07-02",
        creativeApproved: true,
        status: "INVALIDATED",
        platform: "FACEBOOK",
        stage: "AD",
        createdAt: "2026-07-01T00:00:00Z",
        updatedAt: "2026-07-02T00:00:00Z",
      },
    ];

    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/experiments/summary")
        return Promise.resolve({ data: experiments });
      if (url === "/api/niches") {
        return Promise.resolve({
          data: [
            {
              id: 10,
              name: "Nicho Principal",
              description: "",
              demandVolume: "",
              promises: "",
              offers: "",
              baseSegmentation: "",
              interests: "",
              demographicFilters: "",
              extraTips: "",
            },
          ],
        });
      }
      return Promise.resolve({ data: [] });
    });

    renderPage();

    expect(await screen.findByText("Experimento em execução")).toBeTruthy();
    expect(screen.queryByText("Experimento finalizado")).toBeNull();
    expect(screen.queryByText("Experimento invalidado")).toBeNull();
    expect(screen.queryByText("Experimento com falha")).toBeNull();
    expect(screen.queryByRole("option", { name: "FINISHED" })).toBeNull();
    expect(screen.queryByRole("option", { name: "FAILED" })).toBeNull();
    expect(
      screen.getByRole("option", { name: "Status não finalizados" }),
    ).toBeTruthy();
    expect(
      screen.getByText((content) =>
        content.includes("Exibindo 1-1 de 1 experimentos não finalizados"),
      ),
    ).toBeTruthy();
  });

  it("reactivates a stopped experiment with a registered reason", async () => {
    const experiments = [
      {
        id: "67",
        nicheId: 10,
        hypothesisId: "hypothesis-67",
        name: "MUSA-H001-E005",
        hypothesis: "Método MUSA",
        cost: 32.34,
        startDate: "2026-07-21",
        endDate: null,
        creativeApproved: true,
        status: "USER_STOPPED",
        reactivationAvailable: true,
        platform: "FACEBOOK",
        stage: "AD",
        createdAt: "2026-07-21T00:00:00Z",
        updatedAt: "2026-07-22T00:16:31Z",
      },
    ];

    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/experiments/summary")
        return Promise.resolve({ data: experiments });
      if (url === "/api/niches") {
        return Promise.resolve({
          data: [
            {
              id: 10,
              name: "Mulheres urbanas",
              description: "",
              demandVolume: "",
              promises: "",
              offers: "",
              baseSegmentation: "",
              interests: "",
              demographicFilters: "",
              extraTips: "",
            },
          ],
        });
      }
      return Promise.resolve({ data: null });
    });
    (axios.post as any).mockResolvedValueOnce({
      data: { ...experiments[0], status: "RUNNING" },
    });

    renderPage();

    await userEvent.click(
      await screen.findByRole("button", { name: "Retornar à atividade" }),
    );
    expect(
      screen.getByRole("heading", { name: "Retornar experimento à atividade" }),
    ).toBeTruthy();
    await userEvent.click(screen.getByRole("button", { name: "Reativar" }));

    expect(axios.post).toHaveBeenCalledWith("/api/experiments/67/reactivate", {
      reason:
        "Retomar o Experimento 67 para medir a versão atual do PDE Musa em produção como novo ciclo dentro do mesmo aprendizado.",
    });
  });

  it("reconciles a stopped experiment that crossed the financial limit", async () => {
    const experiment = {
      id: "88",
      nicheId: 21,
      hypothesisId: "hypothesis-88",
      name: "MAQA-H002-E001",
      hypothesis: "MAQA-H002",
      campaignMetric: { spend: 25.24 },
      terminalReconciliationAvailable: true,
      reactivationAvailable: false,
      status: "USER_STOPPED",
      platform: "FACEBOOK",
      stage: "AD",
      createdAt: "2026-08-08T17:57:29Z",
      updatedAt: "2026-08-22T20:47:24Z",
    };
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/experiments/summary") {
        return Promise.resolve({ data: [experiment] });
      }
      if (url === "/api/niches") {
        return Promise.resolve({
          data: [{ id: 21, name: "Nail designers" }],
        });
      }
      return Promise.resolve({ data: null });
    });
    (axios.post as any).mockResolvedValueOnce({
      data: { experimentId: 88, status: "INVALIDATED", invalidated: true },
    });

    renderPage();

    expect(
      screen.queryByRole("button", { name: "Retornar à atividade" }),
    ).toBeNull();

    await userEvent.click(
      await screen.findByRole("button", {
        name: "Concluir pelo limite financeiro",
      }),
    );

    expect(axios.post).toHaveBeenCalledWith(
      "/api/experiments/88/terminal-reconciliation",
    );
  });
});
