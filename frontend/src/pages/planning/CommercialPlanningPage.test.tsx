import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
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
    objectivesEditable: true,
    objectiveEditWindowMessage: "Objetivos liberados ate 2026-07-09.",
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
        nicheId: 21,
        nicheName: "Manicure profissional",
        hypothesisId: "11111111-1111-1111-1111-111111111111",
        hypothesisTitle: "Kit de manutenção guiada para manicures",
        productType: "LOW_TICKET",
        manual: true,
        abTest: true,
        status: "ACTIVE",
        createdAt: "2026-07-02T10:00:00Z",
        totalCost: 37,
        videoCost: 12,
        revenue: 27,
        clicks: 44,
        leads: 6,
        checkoutClicks: 2,
        purchases: 1,
        averageProductViewTimeMs: 45000,
        result: "Receita parcial",
      },
      {
        id: 40,
        name: "Agenda recorrente",
        nicheId: 21,
        nicheName: "Manicure profissional",
        hypothesisId: "11111111-1111-1111-1111-111111111111",
        hypothesisTitle: "Kit de manutenção guiada para manicures",
        productType: "LEAD_MAGNET",
        manual: false,
        abTest: false,
        status: "ACTIVE",
        createdAt: "2026-07-03T10:00:00Z",
        totalCost: 12,
        videoCost: 0,
        revenue: 0,
        clicks: 12,
        leads: 3,
        checkoutClicks: 0,
        purchases: 0,
        averageProductViewTimeMs: 90000,
        result: "Sem receita rastreada",
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

function renderPage() {
  return render(
    <MemoryRouter>
      <CommercialPlanningPage />
    </MemoryRouter>,
  );
}

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
      objectivesEditable: true,
      objectiveEditWindowMessage: "Objetivos liberados ate 2026-07-09.",
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
          nicheId: 21,
          nicheName: "Manicure profissional",
          hypothesisId: "11111111-1111-1111-1111-111111111111",
          hypothesisTitle: "Kit de manutenção guiada para manicures",
          productType: "LOW_TICKET",
          manual: true,
          abTest: true,
          status: "ACTIVE",
          createdAt: "2026-07-02T10:00:00Z",
          totalCost: 37,
          videoCost: 12,
          revenue: 27,
          clicks: 44,
          leads: 6,
          checkoutClicks: 2,
          purchases: 1,
          averageProductViewTimeMs: 45000,
          result: "Receita parcial",
        },
        {
          id: 40,
          name: "Agenda recorrente",
          nicheId: 21,
          nicheName: "Manicure profissional",
          hypothesisId: "11111111-1111-1111-1111-111111111111",
          hypothesisTitle: "Kit de manutenção guiada para manicures",
          productType: "LEAD_MAGNET",
          manual: false,
          abTest: false,
          status: "ACTIVE",
          createdAt: "2026-07-03T10:00:00Z",
          totalCost: 12,
          videoCost: 0,
          revenue: 0,
          clicks: 12,
          leads: 3,
          checkoutClicks: 0,
          purchases: 0,
          averageProductViewTimeMs: 90000,
          result: "Sem receita rastreada",
        },
      ],
    },
  ];
  cleanup();
});

describe("CommercialPlanningPage", () => {
  it("renderiza somente o planejamento superior", () => {
    renderPage();

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

  it("renderiza experimentos criados por semana em tabela ordenada por media de tempo", () => {
    renderPage();

    expect(screen.getByText("Semana 1")).toBeTruthy();
    expect(screen.getByText("Nome do experimento")).toBeTruthy();
    expect(screen.getByText("Média de tempo")).toBeTruthy();
    expect(screen.getByText("Tipo de produto")).toBeTruthy();
    expect(screen.getByText("Teste A/B")).toBeTruthy();
    expect(
      screen.getByRole("link", { name: "Kit manutenção" }),
    ).toHaveAttribute("href", "/experiments/39");
    expect(
      screen.getByRole("link", { name: "Agenda recorrente" }),
    ).toHaveAttribute("href", "/experiments/40");

    const rows = screen.getAllByRole("row");
    expect(rows[1]).toHaveTextContent("Agenda recorrente");
    expect(rows[1]).toHaveTextContent("1min 30s");
    expect(rows[2]).toHaveTextContent("Kit manutenção");
    expect(rows[2]).toHaveTextContent("45s");
    expect(screen.getAllByText("Manicure profissional")).toHaveLength(2);
    expect(
      screen.getAllByText("Kit de manutenção guiada para manicures"),
    ).toHaveLength(2);
    expect(screen.getByText("LOW_TICKET")).toBeTruthy();
    expect(screen.getByText("LEAD_MAGNET")).toBeTruthy();
  });

  it("renderiza objetivos semanais como texto e mostra campo apenas para novo objetivo", async () => {
    const user = userEvent.setup();
    renderPage();

    expect(screen.getByText("Objetivos da semana")).toBeTruthy();
    expect(screen.getByText(/checkout_click/)).toBeTruthy();
    expect(screen.queryByDisplayValue(/checkout_click/)).toBeNull();
    expect(screen.queryByLabelText("Novo objetivo da semana 1")).toBeNull();

    await user.click(screen.getByRole("button", { name: "Inserir novo" }));

    expect(screen.getByLabelText("Novo objetivo da semana 1")).toBeTruthy();
    expect(screen.getByText("Salvar novo")).toBeTruthy();
  });

  it("oculta insercao de objetivo quando a semana esta fora da janela permitida", () => {
    mockWeeks = [
      {
        ...(mockWeeks[0] as Record<string, unknown>),
        objectivesEditable: false,
        objectiveEditWindowMessage:
          "Objetivos disponiveis de 2026-07-12 ate 2026-07-16.",
      },
    ];

    renderPage();

    expect(screen.getByText("Objetivos da semana")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Inserir novo" })).toBeNull();
    expect(screen.queryByLabelText("Novo objetivo da semana 1")).toBeNull();
  });

  it("usa valores seguros quando status vem fora do contrato", () => {
    renderPage();

    expect(screen.getAllByText("Rascunho").length).toBeGreaterThan(0);
  });

  it("renderiza sugestao de julho quando a API ainda nao retorna planos", () => {
    mockPlans = [];

    renderPage();

    expect(screen.getByText("Planejamento")).toBeTruthy();
    expect(screen.getByText("Julho 2026")).toBeTruthy();
    expect(screen.queryByText("Plano sugerido")).toBeNull();
  });
});
