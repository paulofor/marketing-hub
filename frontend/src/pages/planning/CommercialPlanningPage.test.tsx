import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import CommercialPlanningPage from "./CommercialPlanningPage";

vi.mock("../../api/agent/useAgents", () => ({
  useAgents: () => ({
    data: [
      {
        id: 1,
        name: "Operador de Crescimento",
        nickname: "Hermes",
        agentKey: "growth-operator",
        status: "ACTIVE",
        currentVersion: 1,
        executionMode: "ON_DEMAND",
        themeId: 1,
        inputs: [],
        outputs: [],
        internalFunctions: [],
      },
    ],
  }),
}));

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
let lastReferenceMonth: string | null | undefined = null;
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
    funnelStages: [
      {
        code: "AD_VIEW",
        name: "Visualizacao do anuncio",
        plannedTotal: null,
        actualTotal: 2000,
        conversionFromPreviousStep: null,
        costPerConversion: 0.02,
        uniqueCount: 2000,
        lastEventAt: "2026-07-03T10:00:00Z",
        applicable: true,
        evidenceSource: "experiment_campaign_metric.impressions",
      },
      {
        code: "AD_CLICK",
        name: "Clique no anuncio",
        plannedTotal: null,
        actualTotal: 56,
        conversionFromPreviousStep: 2.8,
        costPerConversion: 0.88,
        uniqueCount: 56,
        lastEventAt: "2026-07-03T10:00:00Z",
        applicable: true,
        evidenceSource: "experiment_campaign_metric.clicks",
      },
      {
        code: "CHECKOUT_CLICK",
        name: "Clique no plano ou checkout",
        plannedTotal: null,
        actualTotal: 2,
        conversionFromPreviousStep: 3.57,
        costPerConversion: 24.5,
        uniqueCount: 2,
        lastEventAt: "2026-07-03T10:00:00Z",
        applicable: true,
        evidenceSource: "experiment_financial_metric.checkout_clicks",
      },
      {
        code: "FIRST_USE",
        name: "Primeiro uso ou ativacao",
        plannedTotal: null,
        actualTotal: null,
        conversionFromPreviousStep: null,
        costPerConversion: null,
        uniqueCount: null,
        lastEventAt: null,
        applicable: false,
        evidenceSource: "Sem fonte canonica persistida para a semana",
      },
    ],
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
const createPlanMutate = vi.fn();
const updateWeekMutate = vi.fn();
const requestRevenueProjectionMutate = vi.fn();
const requestCommercialAssumptionsMutate = vi.fn();

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
    useCommercialPlanVersions: (planId?: number | null) => ({
      data:
        planId && planId > 0
          ? [
              {
                id: 101,
                commercialPlanId: planId,
                versionNumber: 2,
                snapshotJson: "{}",
                changedBy: "USER",
                changeReason: "Atualização do contexto comercial",
                createdAt: "2026-08-11T12:00:00Z",
              },
            ]
          : [],
      isLoading: false,
      isError: false,
    }),
    useCommercialPlanAgentActivity: (planId?: number | null) => ({
      data:
        planId && planId > 0
          ? {
              commercialPlanId: planId,
              currentVersion: 2,
              budgetLimitBrl: 400,
              campaignCostBrl: 25,
              aiCostBrl: 10,
              totalCostBrl: 35,
              revenueBrl: 0,
              videoBudgetLimitUsd: 40,
              videoKnownCostUsd: 0,
              openTasks: 1,
              pendingDecisions: 1,
              entries: [
                {
                  recordType: "TASK",
                  agentKey: "experiment-strategist",
                  agentNickname: "Atena",
                  title: "Análise inicial do plano",
                  status: "COMPLETED",
                  finalOpinion:
                    "Validar a oferta com pagamento real antes de ampliar aquisição.",
                  sourceReference: "commercial-plan:1@v1",
                  occurredAt: "2026-08-10T12:00:00Z",
                },
                {
                  recordType: "FINANCIAL_GATE",
                  agentKey: "financial-agent",
                  agentNickname: "Plutus",
                  title: "Controle financeiro do ciclo de vídeo #1",
                  status: "PENDING_FINANCIAL_REVIEW",
                  externalDecisionRequired: true,
                  externalDecision: "Plutus precisa decidir o orçamento.",
                  sourceReference: "video-production-cycle:1",
                  budgetLimitUsd: 40,
                  knownCostUsd: 0,
                  occurredAt: "2026-08-11T12:00:00Z",
                },
              ],
            }
          : undefined,
      isLoading: false,
      isError: false,
    }),
    useRevenueProjections: (planId?: number | null) => ({
      data:
        planId && planId > 0
          ? [
              {
                id: 77,
                commercialPlanId: planId,
                status: "COMPLETED",
                authorityMode: "READ_ONLY_REVENUE_PROJECTION",
                commercialPlanVersion: 2,
                agentTaskId: 88,
                dailyReport:
                  "Cenário base recomenda começar pequeno e validar o CAC.",
                createdAt: "2026-08-11T13:00:00Z",
              },
            ]
          : [],
      isLoading: false,
      isError: false,
    }),
    useRequestRevenueProjection: () => ({
      mutate: requestRevenueProjectionMutate,
      isPending: false,
      isError: false,
    }),
    useCommercialAssumptionDefinitions: () => ({
      data: [],
      isLoading: false,
      isError: false,
    }),
    useRequestCommercialAssumptions: () => ({
      mutate: requestCommercialAssumptionsMutate,
      isPending: false,
      isError: false,
    }),
    useCommercialPlanWeeks: (
      _planId?: number | null,
      referenceMonth?: string | null,
    ) => {
      lastReferenceMonth = referenceMonth;
      return {
        data:
          referenceMonth === "2026-08"
            ? [
                {
                  weekNumber: 1,
                  startDate: "2026-08-03",
                  endDate: "2026-08-09",
                  experimentsCreated: 0,
                  totalCost: 0,
                  totalRevenue: 0,
                  objectivesEditable: false,
                  objectiveEditWindowMessage:
                    "Objetivos editaveis apenas no mes de referencia do plano.",
                  funnelStages: [],
                  objectives: [],
                  experiments: [],
                },
                {
                  weekNumber: 5,
                  startDate: "2026-08-31",
                  endDate: "2026-09-06",
                  experimentsCreated: 0,
                  totalCost: 0,
                  totalRevenue: 0,
                  objectivesEditable: false,
                  objectiveEditWindowMessage:
                    "Objetivos editaveis apenas no mes de referencia do plano.",
                  funnelStages: [],
                  objectives: [],
                  experiments: [],
                },
              ]
            : mockWeeks,
        isLoading: false,
        isError: false,
      };
    },
    useUpdateCommercialPlanWeekObjectives: () => ({
      mutate: updateWeekMutate,
      isPending: false,
      isError: false,
      isSuccess: false,
    }),
    useUpdateCommercialPlanWeekCommitmentStatus: () => ({
      mutate: vi.fn(),
      isPending: false,
    }),
    useCreateCommercialPlan: () => ({
      mutate: createPlanMutate,
      isPending: false,
      isError: false,
      isSuccess: false,
    }),
    useUpdateCommercialPlan: () => ({
      mutate: vi.fn(),
      isPending: false,
      isError: false,
      isSuccess: false,
    }),
  };
});

vi.mock("../../api/planning/useGrowthOperator", () => ({
  useGrowthOperatorMcpTools: () => ({
    data: [],
    isError: false,
  }),
  useGrowthOperatorExecutions: () => ({
    data: [],
    isError: false,
  }),
  useGrowthOperatorTasks: () => ({ data: [], isError: false }),
  useResolveGrowthOperatorTask: () => ({ mutate: vi.fn(), isPending: false }),
  useStartGrowthOperator: () => ({
    mutate: vi.fn(),
    isPending: false,
    isError: false,
    isSuccess: false,
  }),
}));

vi.mock("../../api/planning/useFinancialAgent", () => ({
  useFinancialAgentExecutions: () => ({ data: [], isError: false }),
  useStartFinancialAgent: () => ({
    mutate: vi.fn(),
    isPending: false,
    isError: false,
  }),
}));

vi.mock("../../api/planning/useExperimentStrategist", () => ({
  useExperimentStrategistExecutions: () => ({ data: [], isError: false }),
  useStartExperimentStrategist: () => ({
    mutate: vi.fn(),
    isPending: false,
    isError: false,
  }),
}));

function renderPage(path?: string) {
  const operationalPlan = mockPlans.find(
    (candidate) =>
      typeof candidate === "object" &&
      candidate !== null &&
      "deadline" in candidate &&
      String(candidate.deadline).startsWith("2026-08"),
  ) as { id?: number } | undefined;
  const initialPath = path ?? `/planning/${operationalPlan?.id ?? 1}`;
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route path="/planning" element={<CommercialPlanningPage />} />
        <Route path="/planning/:planId" element={<CommercialPlanningPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

afterEach(() => {
  createPlanMutate.mockReset();
  mockPlans = defaultPlans;
  lastReferenceMonth = null;
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
      funnelStages: [
        {
          code: "AD_VIEW",
          name: "Visualizacao do anuncio",
          plannedTotal: null,
          actualTotal: 2000,
          conversionFromPreviousStep: null,
          costPerConversion: 0.02,
          uniqueCount: 2000,
          lastEventAt: "2026-07-03T10:00:00Z",
          applicable: true,
          evidenceSource: "experiment_campaign_metric.impressions",
        },
        {
          code: "AD_CLICK",
          name: "Clique no anuncio",
          plannedTotal: null,
          actualTotal: 56,
          conversionFromPreviousStep: 2.8,
          costPerConversion: 0.88,
          uniqueCount: 56,
          lastEventAt: "2026-07-03T10:00:00Z",
          applicable: true,
          evidenceSource: "experiment_campaign_metric.clicks",
        },
        {
          code: "CHECKOUT_CLICK",
          name: "Clique no plano ou checkout",
          plannedTotal: null,
          actualTotal: 2,
          conversionFromPreviousStep: 3.57,
          costPerConversion: 24.5,
          uniqueCount: 2,
          lastEventAt: "2026-07-03T10:00:00Z",
          applicable: true,
          evidenceSource: "experiment_financial_metric.checkout_clicks",
        },
        {
          code: "FIRST_USE",
          name: "Primeiro uso ou ativacao",
          plannedTotal: null,
          actualTotal: null,
          conversionFromPreviousStep: null,
          costPerConversion: null,
          uniqueCount: null,
          lastEventAt: null,
          applicable: false,
          evidenceSource: "Sem fonte canonica persistida para a semana",
        },
      ],
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

    expect(
      screen.getByRole("link", { name: /todos os planos comerciais/i }),
    ).toBeTruthy();
    expect(
      screen.getByText(/tudo que os agentes fizeram em cada plano/i),
    ).toBeTruthy();
    expect(screen.getByText("Plano do mês corrente")).toBeTruthy();
    expect(screen.getAllByText("Custo total").length).toBeGreaterThan(0);
    expect(screen.getByText("Receita mínima")).toBeTruthy();
    expect(screen.getByText("Direcionamento de verbas")).toBeTruthy();
    expect(screen.getByText("Detalhe abaixo do custo mensal")).toBeTruthy();
    expect(screen.getByText("Vídeos e criativos")).toBeTruthy();
    expect(screen.getByText("Sem classificação")).toBeTruthy();
    expect(screen.getByText("Funil acumulado do mês")).toBeTruthy();
    expect(
      screen.getAllByText("Clique no plano ou checkout").length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText("Gargalo principal: Clique no anuncio").length,
    ).toBeGreaterThan(0);
    expect(screen.queryByText("Tipos de produto")).toBeNull();
    expect(screen.queryByText("Critério de decisão")).toBeNull();
    expect(screen.queryByText("Execução imediata")).toBeNull();
    expect(screen.queryByText("Plano ativo")).toBeNull();
    expect(screen.queryByText("Planos de Primeira Venda")).toBeNull();
    expect(screen.queryByText("Novo Plano de Primeira Venda")).toBeNull();
  });

  it("permite editar as decisoes comerciais do plano pela tela", async () => {
    const user = userEvent.setup();
    mockPlans = [
      {
        ...defaultPlans[0],
        stopCriteria: "Gate vigente do plano selecionado",
        nextAction: "Próxima ação do plano selecionado",
      },
      defaultPlans[1],
    ];
    renderPage();

    await user.click(screen.getByRole("button", { name: "Editar plano" }));

    expect(screen.getByLabelText("Status")).toBeTruthy();
    expect(screen.getByLabelText("Prazo da meta")).toBeTruthy();
    expect(screen.getByLabelText("Meta de receita")).toBeTruthy();
    expect(screen.getByLabelText("Teto total do plano (R$)")).toHaveValue(300);
    expect(screen.getByLabelText("Objetivo comercial")).toBeTruthy();
    expect(screen.getByLabelText("Critério de sucesso")).toBeTruthy();
    expect(screen.getByLabelText("Critério de parada")).toBeTruthy();
    expect(screen.getByLabelText("Critério de parada")).toHaveValue(
      "Gate vigente do plano selecionado",
    );
    expect(screen.getByLabelText("Próxima ação")).toBeTruthy();
    expect(screen.getByLabelText("Próxima ação")).toHaveValue(
      "Próxima ação do plano selecionado",
    );
    expect(screen.getByLabelText("Gargalo atual")).toBeTruthy();
    expect(screen.getByLabelText("Causa-raiz")).toBeTruthy();
    expect(
      screen.getByRole("button", { name: "Salvar planejamento" }),
    ).toBeTruthy();
  });

  it("seleciona o plano do mes operacional em vez do primeiro plano retornado", () => {
    mockPlans = [
      {
        ...defaultPlans[0],
        name: "Plano de julho",
        deadline: "2026-07-31",
      },
      {
        ...defaultPlans[1],
        name: "Plano de agosto",
        deadline: "2026-08-09",
        status: "DRAFT",
        maxBudget: 400,
      },
    ];

    renderPage();

    expect(screen.getByRole("heading", { name: "Agosto 2026" })).toBeTruthy();
    expect(screen.getAllByText("R$ 400,00").length).toBeGreaterThan(0);
  });

  it("permite preparar um plano comercial dedicado com teto de producao", async () => {
    const user = userEvent.setup();
    renderPage("/planning");

    await user.click(
      screen.getByRole("button", { name: "Novo plano comercial" }),
    );

    await user.type(
      screen.getByLabelText("Nome *"),
      "MUSA v7 - produção supervisionada",
    );
    await user.type(screen.getByLabelText("Prazo *"), "2026-08-31");
    await user.type(screen.getByLabelText("Teto (R$) *"), "100");
    await user.type(
      screen.getByLabelText("Objetivo comercial *"),
      "Concluir MECANISMO e CTA sem gerar cenas já aprovadas.",
    );

    await user.click(
      screen.getByRole("button", { name: "Criar e abrir plano" }),
    );

    expect(createPlanMutate).toHaveBeenCalledWith(
      expect.objectContaining({
        name: "MUSA v7 - produção supervisionada",
        maxBudget: 100,
        commercialObjective:
          "Concluir MECANISMO e CTA sem gerar cenas já aprovadas.",
      }),
      expect.objectContaining({ onSuccess: expect.any(Function) }),
    );
  });

  it("permite alternar entre planos comerciais sem misturar atribuicao", async () => {
    mockPlans = [
      { ...defaultPlans[0], name: "Agenda Cheia", deadline: "2026-08-09" },
      {
        ...defaultPlans[1],
        id: 9,
        name: "MUSA v7",
        deadline: "2026-08-31",
        status: "DRAFT",
        maxBudget: 100,
      },
    ];
    renderPage("/planning");

    expect(screen.getByRole("heading", { name: "Agenda Cheia" })).toBeTruthy();
    expect(screen.getByRole("heading", { name: "MUSA v7" })).toBeTruthy();
    expect(
      screen.getAllByRole("link", { name: "Ver detalhe completo" })[1],
    ).toHaveAttribute("href", "/planning/9");
  });

  it("inicializa semanas fechadas e abre a tabela ordenada por media de tempo", async () => {
    const user = userEvent.setup();
    const { container } = renderPage();

    const weekDetails = container.querySelector(
      ".commercial-planning-week-card",
    ) as HTMLDetailsElement;
    expect(weekDetails.open).toBe(false);

    expect(screen.getByText("Semana 1")).toBeTruthy();

    await user.click(screen.getByText("Semana 1"));

    expect(weekDetails.open).toBe(true);
    expect(screen.getByText("Nome do experimento")).toBeTruthy();
    expect(screen.getByText("Média de tempo")).toBeTruthy();
    expect(screen.getByText("Tipo de produto")).toBeTruthy();
    expect(screen.getByText("Teste A/B")).toBeTruthy();
    expect(screen.getByText("Funil da semana 1")).toBeTruthy();
    expect(
      screen.getAllByText("Etapa sem fonte canônica nesta versão.").length,
    ).toBeGreaterThan(0);
    expect(screen.getAllByText("Lucro").length).toBeGreaterThan(0);
    expect(
      screen.getAllByRole("link", { name: "Kit manutenção" })[0],
    ).toHaveAttribute("href", "/experiments/39");
    expect(
      screen.getAllByRole("link", { name: "Agenda recorrente" })[0],
    ).toHaveAttribute("href", "/experiments/40");

    const experimentTable = screen
      .getByText("Nome do experimento")
      .closest("table")!;
    const rows = Array.from(experimentTable.querySelectorAll("tr"));
    expect(rows[1]).toHaveTextContent("Agenda recorrente");
    expect(rows[1]).toHaveTextContent("1min 30s");
    expect(rows[2]).toHaveTextContent("Kit manutenção");
    expect(rows[2]).toHaveTextContent("45s");
    expect(screen.getAllByText("Manicure profissional").length).toBeGreaterThan(
      1,
    );
    expect(
      screen.getAllByText("Kit de manutenção guiada para manicures"),
    ).toHaveLength(2);
    expect(screen.getByText("LOW_TICKET")).toBeTruthy();
    expect(screen.getByText("LEAD_MAGNET")).toBeTruthy();
  });

  it("renderiza top 5 no final por tempo medio em ordem decrescente", () => {
    mockWeeks = [
      {
        ...(mockWeeks[0] as Record<string, unknown>),
        experiments: [
          {
            id: 39,
            name: "Kit manutenção",
            nicheName: "Manicure profissional",
            totalCost: 37,
            revenue: 137,
            averageProductViewTimeMs: 45000,
          },
          {
            id: 40,
            name: "Agenda recorrente",
            nicheName: "Manicure profissional",
            totalCost: 12,
            revenue: 12,
            averageProductViewTimeMs: 90000,
          },
          {
            id: 41,
            name: "Amostra rápida",
            nicheName: "Manicure profissional",
            totalCost: 8,
            revenue: 8,
            averageProductViewTimeMs: 120000,
          },
        ],
      },
    ];

    const { container } = renderPage();

    expect(screen.getByText("Top 5 por tempo médio")).toBeTruthy();
    expect(screen.getByText(/maior tempo médio de tela/)).toBeTruthy();

    const rankingItems = Array.from(
      container.querySelectorAll(".commercial-planning-ranking-item"),
    );
    expect(rankingItems).toHaveLength(3);
    expect(rankingItems[0]).toHaveTextContent("Amostra rápida");
    expect(rankingItems[0]).toHaveTextContent("2min");
    expect(rankingItems[1]).toHaveTextContent("Agenda recorrente");
    expect(rankingItems[1]).toHaveTextContent("1min 30s");
    expect(rankingItems[2]).toHaveTextContent("Kit manutenção");
    expect(rankingItems[2]).toHaveTextContent("45s");
  });

  it("renderiza objetivos para a proxima semana como texto e mostra campo apenas para novo objetivo", async () => {
    const user = userEvent.setup();
    renderPage();

    expect(screen.getByText("Objetivos para a próxima semana")).toBeTruthy();
    expect(screen.getByText(/checkout_click/)).toBeTruthy();
    expect(screen.queryByDisplayValue(/checkout_click/)).toBeNull();
    expect(screen.queryByLabelText("Novo objetivo para a semana 2")).toBeNull();

    await user.click(screen.getByRole("button", { name: "Inserir novo" }));

    expect(screen.getByLabelText("Novo objetivo para a semana 2")).toBeTruthy();
    expect(screen.getByText("Salvar novo")).toBeTruthy();
  });

  it("cria compromisso semanal vinculado ao plano, agente e metas financeiras", async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(screen.getByRole("button", { name: "Inserir novo" }));
    await user.type(
      screen.getByLabelText("Novo objetivo para a semana 2"),
      "Corrigir o gargalo do checkout",
    );
    await user.type(
      screen.getByLabelText("Resultado comercial esperado"),
      "Uma compra aprovada",
    );
    await user.selectOptions(
      screen.getByLabelText("Agente responsável"),
      "growth-operator",
    );
    await user.type(screen.getByLabelText("Custo planejado da semana"), "50");
    await user.type(
      screen.getByLabelText("Receita planejada da semana"),
      "134",
    );
    await user.click(screen.getByRole("button", { name: "Salvar novo" }));

    expect(updateWeekMutate).toHaveBeenCalledWith(
      expect.objectContaining({
        weekNumber: 1,
        objectives: expect.arrayContaining([
          expect.objectContaining({
            objectiveText: "Corrigir o gargalo do checkout",
            planVersionNumber: 2,
            assignedAgentKey: "growth-operator",
            assignedAgentNickname: "Hermes",
            expectedResult: "Uma compra aprovada",
            executionStatus: "PLANNED",
            plannedCost: 50,
            plannedRevenue: 134,
          }),
        ]),
      }),
    );
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

    expect(screen.getByText("Objetivos para a próxima semana")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Inserir novo" })).toBeNull();
    expect(screen.queryByLabelText("Novo objetivo para a semana 2")).toBeNull();
  });

  it("usa valores seguros quando status vem fora do contrato", () => {
    renderPage();

    expect(screen.getAllByText("Rascunho").length).toBeGreaterThan(0);
  });

  it("mostra o catalogo MCP dentro do painel do Operador", () => {
    renderPage();

    expect(screen.getByText("Ferramentas disponíveis via MCP")).toBeTruthy();
    expect(
      screen.getByText(/Catálogo autorizado para investigação direta/),
    ).toBeTruthy();
  });

  it("mostra trabalhos, decisões e finanças dos agentes dentro do plano", () => {
    renderPage();

    expect(screen.getByText("Atuação dos agentes no plano")).toBeTruthy();
    expect(screen.getByText("Histórico cronológico")).toBeTruthy();
    expect(screen.getByText("Parecer final")).toBeTruthy();
    expect(
      screen.getByText(
        "Validar a oferta com pagamento real antes de ampliar aquisição.",
      ),
    ).toBeTruthy();
    expect(screen.getAllByText("Plutus").length).toBeGreaterThan(0);
    expect(screen.getByText("1 decisões pendentes")).toBeTruthy();
    expect(
      screen.getAllByText("Plutus precisa decidir o orçamento.").length,
    ).toBeGreaterThan(0);
    expect(screen.getAllByText("US$ 0.00 / US$ 40.00").length).toBeGreaterThan(
      0,
    );
    const chronologicalRows = screen.getAllByRole("row");
    const plutusRow = chronologicalRows.findIndex((row) =>
      row.textContent?.includes("Plutus"),
    );
    const atenaRow = chronologicalRows.findIndex((row) =>
      row.textContent?.includes("Atena"),
    );
    expect(plutusRow).toBeGreaterThan(-1);
    expect(atenaRow).toBeGreaterThan(plutusRow);
  });

  it("consolida bloqueios do plano com causa, impacto, ação e evidência", () => {
    mockPlans = [
      {
        ...defaultPlans[0],
        currentBlocker: "Ainda não existe compra aprovada.",
        rootCause:
          "A jornada entre página, checkout e entrega não foi homologada.",
        nextAction:
          "Homologar uma compra completa com dados de teste segregados.",
        milestones: [
          {
            id: 31,
            sequenceOrder: 1,
            code: "DELIVERY_GATE",
            name: "Homologar entrega",
            status: "BLOCKED",
            blocker: "Briefing não retorna ao plano.",
            recommendedNextAction:
              "Corrigir a correlação e repetir a homologação.",
            evidenceSource: "execução #88",
          },
        ],
      },
    ];

    renderPage();

    expect(
      screen.getByRole("heading", { name: "O que bloqueia este plano" }),
    ).toBeTruthy();
    expect(screen.getByText("Gargalo comercial vigente")).toBeTruthy();
    expect(
      screen.getAllByText("Ainda não existe compra aprovada.").length,
    ).toBeGreaterThan(0);
    expect(screen.getByText("Homologar entrega")).toBeTruthy();
    expect(screen.getByText("Briefing não retorna ao plano.")).toBeTruthy();
    expect(screen.getByText("Premissas financeiras incompletas")).toBeTruthy();
    expect(screen.getAllByText("Como desbloquear").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Evidência").length).toBeGreaterThan(0);
  });

  it("solicita projeção de receita a Plutus sem apresentá-la como venda", async () => {
    const user = userEvent.setup();
    renderPage();

    expect(screen.getByText("Projeções de receita por Plutus")).toBeTruthy();
    await user.click(screen.getByRole("button", { name: "Editar plano" }));
    expect(screen.getAllByText("Preço da oferta (R$)").length).toBeGreaterThan(
      0,
    );
    expect(
      screen.getByText(
        "Cenário base recomenda começar pequeno e validar o CAC.",
      ),
    ).toBeTruthy();
    expect(screen.getByText(/Projeções não são vendas/)).toBeTruthy();

    await user.click(
      screen.getByRole("button", { name: "Solicitar projeção" }),
    );

    expect(requestRevenueProjectionMutate).toHaveBeenCalledWith(undefined);
  });

  it("inicia a definição conjunta de premissas por Atena e Plutus", async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(
      screen.getByRole("button", { name: "Definir premissas ausentes" }),
    );

    expect(requestCommercialAssumptionsMutate).toHaveBeenCalledTimes(1);
    expect(screen.getByText(/hipóteses versionadas/i)).toBeTruthy();
  });

  it("renderiza sugestao de julho quando a API ainda nao retorna planos", () => {
    mockPlans = [];

    renderPage();

    expect(
      screen.getByRole("link", { name: /todos os planos comerciais/i }),
    ).toBeTruthy();
    expect(screen.getByText("Julho 2026")).toBeTruthy();
    expect(screen.queryByText("Plano sugerido")).toBeNull();
  });

  it("mostra agosto de 2026 ao clicar em proximo mes", async () => {
    const user = userEvent.setup();
    renderPage();

    expect(screen.getByText("Julho 2026")).toBeTruthy();
    expect(lastReferenceMonth).toBe("2026-07");

    await user.click(screen.getByRole("button", { name: "Próximo mês" }));

    expect(screen.getByText("Agosto 2026")).toBeTruthy();
    expect(screen.getByText("Plano do próximo mês")).toBeTruthy();
    expect(lastReferenceMonth).toBe("2026-08");
    expect(screen.getByText("03/08/2026 até 09/08/2026")).toBeTruthy();
    expect(screen.getByText("31/08/2026 até 06/09/2026")).toBeTruthy();
    expect(screen.getByRole("button", { name: "Mês atual" })).toBeTruthy();
  });
});
