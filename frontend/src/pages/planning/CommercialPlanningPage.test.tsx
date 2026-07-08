import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import CommercialPlanningPage from "./CommercialPlanningPage";

const defaultPlans = [
  {
    id: 1,
    name: "Plano sem marcos",
    planType: "FIRST_SALE",
    status: "DRAFT",
    maxBudget: 300,
    targetRevenue: 27,
    operationalRevenueTarget: 81,
    experimentsToCreate: 2,
    experimentsToPublish: 3,
    daysRemaining: 7,
    milestones: null,
    simulations: null,
  },
  {
    id: 2,
    name: "Plano com dados inesperados",
    planType: "FIRST_SALE",
    status: "UNKNOWN_STATUS",
    daysRemaining: null,
    milestones: [
      {
        id: 10,
        sequenceOrder: 1,
        code: "UNKNOWN",
        name: "Marco com status inesperado",
        status: "UNKNOWN_MILESTONE_STATUS",
      },
    ],
    simulations: [
      {
        id: 20,
        recommendation: "UNKNOWN_RECOMMENDATION",
        mostLikelyScenario: "Cenario com valor inesperado.",
      },
    ],
  },
];

let mockPlans: unknown[] = defaultPlans;
let mockWeeks: unknown[] = [
  {
    weekNumber: 1,
    startDate: "2026-07-01",
    endDate: "2026-07-07",
    experimentsCreated: 1,
    totalCost: 37,
    totalRevenue: 27,
    objectives: [
      {
        id: null,
        sequenceOrder: 1,
        objectiveText:
          "Medir como sucesso primário checkout_click, não compra, até a página provar intenção mínima.",
        score: null,
      },
    ],
    experiments: [
      {
        id: 39,
        name: "Kit manutenção",
        productType: "LOW_TICKET",
        status: "ACTIVE",
        createdAt: "2026-07-02T10:00:00Z",
        totalCost: 37,
        videoCost: 12,
        revenue: 27,
        result: "Receita parcial",
      },
    ],
  },
];

vi.mock("../../api/planning/useCommercialPlans", async () => {
  const actual = await vi.importActual<
    typeof import("../../api/planning/useCommercialPlans")
  >("../../api/planning/useCommercialPlans");

  return {
    ...actual,
    useCommercialPlans: () => ({
      data: mockPlans,
      isLoading: false,
      isError: false,
    }),
    useCommercialPlanWeeks: () => ({
      data: mockWeeks,
      isLoading: false,
      isError: false,
    }),
    useUpdateCommercialPlanWeekObjectives: () => ({
      mutate: vi.fn(),
      isPending: false,
      isError: false,
      isSuccess: false,
    }),
  };
});

afterEach(() => {
  mockPlans = defaultPlans;
  mockWeeks = [
    {
      weekNumber: 1,
      startDate: "2026-07-01",
      endDate: "2026-07-07",
      experimentsCreated: 1,
      totalCost: 37,
      totalRevenue: 27,
      objectives: [
        {
          id: null,
          sequenceOrder: 1,
          objectiveText:
            "Medir como sucesso primário checkout_click, não compra, até a página provar intenção mínima.",
          score: null,
        },
      ],
      experiments: [
        {
          id: 39,
          name: "Kit manutenção",
          productType: "LOW_TICKET",
          status: "ACTIVE",
          createdAt: "2026-07-02T10:00:00Z",
          totalCost: 37,
          videoCost: 12,
          revenue: 27,
          result: "Receita parcial",
        },
      ],
    },
  ];
  cleanup();
});

describe("CommercialPlanningPage", () => {
  it("renderiza somente o planejamento superior", () => {
    render(<CommercialPlanningPage />);

    expect(screen.getByText("Planejamento")).toBeTruthy();
    expect(screen.getByText("Plano do mês corrente")).toBeTruthy();
    expect(screen.getAllByText("Custo total").length).toBeGreaterThan(0);
    expect(screen.getByText("Receita mínima")).toBeTruthy();
    expect(screen.queryByText("Tipos de produto")).toBeNull();
    expect(screen.queryByText("Critério de decisão")).toBeNull();
    expect(screen.queryByText("Execução imediata")).toBeNull();
    expect(screen.queryByText("Plano ativo")).toBeNull();
    expect(screen.queryByText("Planos de Primeira Venda")).toBeNull();
    expect(screen.queryByText("Novo Plano de Primeira Venda")).toBeNull();
  });

  it("renderiza experimentos criados por semana do mes", () => {
    render(<CommercialPlanningPage />);

    expect(screen.getByText("Semana 1")).toBeTruthy();
    expect(screen.getByText("Kit manutenção")).toBeTruthy();
    expect(screen.getByText("Vídeo")).toBeTruthy();
    expect(screen.getByText("Receita parcial")).toBeTruthy();
  });

  it("renderiza objetivos semanais como texto e mostra campo apenas para novo objetivo", async () => {
    const user = userEvent.setup();
    render(<CommercialPlanningPage />);

    expect(screen.getByText("Objetivos da semana")).toBeTruthy();
    expect(screen.getByText(/checkout_click/)).toBeTruthy();
    expect(screen.queryByDisplayValue(/checkout_click/)).toBeNull();
    expect(screen.queryByLabelText("Novo objetivo da semana 1")).toBeNull();

    await user.click(screen.getByRole("button", { name: "Inserir novo" }));

    expect(screen.getByLabelText("Novo objetivo da semana 1")).toBeTruthy();
    expect(screen.getByText("Salvar novo")).toBeTruthy();
  });

  it("usa valores seguros quando status vem fora do contrato", () => {
    render(<CommercialPlanningPage />);

    expect(screen.getAllByText("Rascunho").length).toBeGreaterThan(0);
  });

  it("renderiza sugestao de julho quando a API ainda nao retorna planos", () => {
    mockPlans = [];

    render(<CommercialPlanningPage />);

    expect(screen.getByText("Planejamento")).toBeTruthy();
    expect(screen.getByText("Julho 2026")).toBeTruthy();
    expect(screen.queryByText("Plano sugerido")).toBeNull();
  });
});
