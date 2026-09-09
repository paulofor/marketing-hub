import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import axios from "axios";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import LearningCyclesPage from "./LearningCyclesPage";
import LearningCycleCommandForm from "./LearningCycleCommandForm";
import LearningCycleDiagram from "./LearningCycleDiagram";
import {
  cycleApi,
  type LearningCycle,
  type CycleCatalog,
} from "../../api/learningCycle/useLearningCycles";
vi.mock("axios");
const catalog: CycleCatalog = {
  entry: {
    chainDefinitionId: 1,
    chainName: "Cadeia PDE",
    parentProcessDefinitionId: 6,
    parentProcessName: "Venda e aprendizado",
    sequenceNumber: 6,
    activityId: "learningCycle",
    activityName: "Conduzir o ciclo de aprendizado e vendas",
    activitySequenceNumber: 4,
    processDefinitionId: 70,
    processName: "Ciclos de aprendizado e vendas",
    integrated: true,
    canStartCycle: true,
    guidance: "Registre a decisão no ciclo",
    workspaceUrl: "/business-process-chains/learning-cycles?chainId=1",
    actionLabel: "Abrir ciclo por produto e experimento",
    parentUrl:
      "/products/4/value-chain-history/processes/73/activities?chainId=1#activity-learningCycle",
    returnRoutes: [],
  },
  processDefinitionId: 70,
  version: 1,
  returnTargets: [
    {
      processDefinitionId: 3,
      processName: "Construção",
      activityId: "rework",
      activityName: "Corrigir valor",
      owner: "Dédalo",
      processCode: "pde-construction-approval",
    },
  ],
  experiments: [],
  diagram: {
    nodes: [
      { id: "LEARNING", type: "TASK", label: "Aprendizado" },
      { id: "decision", type: "GATEWAY", label: "Decisão comercial" },
    ],
    flows: [
      {
        from: "decision",
        to: "LEARNING",
        label: "Ajustar: sucessor com memória",
        kind: "REWORK",
      },
    ],
  },
};
const cycle = {
  id: 2,
  productId: 4,
  experimentId: 91,
  revision: 3,
  chainDefinitionId: 12,
  processDefinitionId: 70,
  stage: "DECISION",
  stageLabel: "Decisão comercial",
  status: "OPEN",
  baseline: false,
  createdAt: "2026-09-07T00:00:00Z",
  productVersion: "v7",
  budgetLimitBrl: 100,
  windowStart: "2026-09-07T00:00:00Z",
  windowEnd: "2026-09-09T00:00:00Z",
  nextAction: "Corrija a microação útil com Dédalo.",
  responsible: "Operador",
  workUrl: "/products/4/value-chain-history",
  brief: {
    hypothesis: "Aumentar uso",
    mainChange: "Microação",
    successCriterion: "Primeiro resultado",
  },
  inheritedLearning: {},
  events: [],
  commands: [
    {
      action: "ADJUST",
      label: "Preparar sucessor",
      available: true,
      reason: "Com evidência",
    },
    {
      action: "SCALE",
      label: "Solicitar escala",
      available: false,
      reason: "Sem contribuição positiva.",
    },
  ],
  canCreateSuccessor: false,
} as LearningCycle;
const decisionProposal = {
  id: 9,
  cycleId: cycle.id,
  cycleRevision: cycle.revision,
  status: "READY",
  agentKey: "experiment-strategist",
  agentName: "Atena",
  agentId: 4,
  automaticExecutionEnabled: true,
  activityDefinitionId: 800,
  operatorName: "Operador do ciclo",
  error: null,
  createdAt: cycle.createdAt,
  finishedAt: cycle.createdAt,
  approvedAt: null,
  approvedEventId: null,
  proposal: {
    contractVersion: "LEARNING_CYCLE_DECISION_PROPOSAL_V1",
    action: "ADJUST",
    summary: "Corrija a microação útil com Dédalo.",
    rootCause:
      "Hipótese de esforço na primeira ação, ainda sem causa comprovada.",
    learning: "Quatro sessões não provam rejeição.",
    nextHypothesis: "Reduzir esforço e medir continuidade até compra.",
    evidenceLimits:
      "Amostra pequena; explicação concorrente: origem do tráfego.",
    correctionPlan: "Conferir a fonte.",
    scaleHypothesis: "Não se aplica.",
    evidenceReference: "internal://measurement/2",
    returnProcessId: 3,
    returnActivityId: "rework",
    evidenceEventIds: [2],
    selectedAlternative: 0,
    alternatives: ["Produto", "Comunicação", "Mais dados"].map((option) => ({
      option,
      benefit: "Valor útil",
      risk: "Amostra pequena",
      effort: "Médio",
      salesImpact: "Hipótese a testar",
    })),
  },
};
function wrapper(
  ui: React.ReactElement,
  entry = "/business-process-chains/learning-cycles?productId=4&chainId=12",
) {
  return render(
    <MemoryRouter initialEntries={[entry]}>
      <QueryClientProvider
        client={
          new QueryClient({ defaultOptions: { queries: { retry: false } } })
        }
      >
        {ui}
      </QueryClientProvider>
    </MemoryRouter>,
  );
}
afterEach(cleanup);
beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(axios.get).mockImplementation(async (url) => {
    if (url === "/api/products")
      return { data: [{ id: 4, internalName: "Vega" }] };
    if (url === "/api/business-process-chains")
      return { data: [{ id: 12, name: "Cadeia PDE", versionNumber: 12 }] };
    if (url === `${cycleApi}/products/4/2/decision-proposal`)
      return { data: decisionProposal };
    if (url === `${cycleApi}/catalog`) return { data: catalog };
    if (url === `${cycleApi}/products/4`) return { data: [cycle] };
    throw new Error(`Requisição não simulada: ${url}`);
  });
});
describe("Ciclos de aprendizado e vendas", () => {
  it("identifica a atividade chamadora e permite retornar ao ponto correto do processo", async () => {
    wrapper(<LearningCyclesPage />);
    expect(
      await screen.findByRole("link", {
        name: "Voltar à atividade 4 do Processo 6",
      }),
    ).toHaveAttribute("href", catalog.entry!.parentUrl);
    expect(
      screen.getByRole("navigation", { name: "Local do ciclo na cadeia" }),
    ).toHaveTextContent(
      "Atividade 4: Conduzir o ciclo de aprendizado e vendas",
    );
    expect(axios.post).not.toHaveBeenCalled();
  });
  it("não substitui um ciclo solicitado inexistente pelo mais recente", async () => {
    wrapper(
      <LearningCyclesPage />,
      "/business-process-chains/learning-cycles?productId=4&chainId=12&cycleId=999",
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "O ciclo solicitado não foi encontrado",
    );
    expect(
      screen.queryByRole("heading", { name: "Ciclo #2 · experimento #91" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("form", { name: "Decisão do ciclo" }),
    ).not.toBeInTheDocument();
  });
  it("retoma o ciclo explícito na própria cadeia quando o link de retorno não informa cadeia", async () => {
    const original = vi.mocked(axios.get).getMockImplementation()!;
    vi.mocked(axios.get).mockImplementation(async (url, ...args) =>
      url === "/api/business-process-chains"
        ? {
            data: [
              { id: 99, name: "Outra cadeia", versionNumber: 1 },
              { id: 12, name: "Cadeia PDE", versionNumber: 12 },
            ],
          }
        : original(url, ...args),
    );
    wrapper(
      <LearningCyclesPage />,
      "/business-process-chains/learning-cycles?productId=4&cycleId=2",
    );
    expect(
      await screen.findByRole("heading", {
        name: "Ciclo #2 · experimento #91",
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("combobox", { name: "Cadeia de Valor *" }),
    ).toHaveValue("12");
    expect(axios.get).toHaveBeenCalledWith(`${cycleApi}/catalog`, {
      params: { chainId: 12, productId: 4 },
    });
  });
  it("preserva a confirmação de instrumentação marcada na homologação", async () => {
    const user = userEvent.setup();
    vi.mocked(axios.post).mockResolvedValue({ data: cycle });
    wrapper(
      <LearningCycleCommandForm
        cycle={{
          ...cycle,
          stage: "VALIDATION",
          approvalOptions: [{ id: 42, label: "Versão v7 aprovada" }],
          commands: [
            {
              action: "COMPLETE",
              label: "Concluir etapa",
              available: true,
              reason: "",
            },
          ],
        }}
        catalog={catalog}
        onUpdated={vi.fn()}
      />,
    );
    for (const [label, value] of [
      ["Responsável pela decisão *", "Operador local"],
      ["Síntese e justificativa *", "Jornada homologada"],
      ["Referência da evidência *", "internal://parecer"],
      ["Evidência de aprovação da jornada *", "internal://jornada"],
    ])
      await user.type(screen.getByLabelText(label), value);
    await user.selectOptions(
      screen.getByLabelText("Parecer multiagente aprovado *"),
      "42",
    );
    await user.click(
      screen.getByLabelText(
        "Instrumentação e segregação dos testes verificadas *",
      ),
    );
    await user.click(screen.getByRole("button", { name: "Concluir etapa" }));
    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith(
        `${cycleApi}/products/4/2/commands`,
        expect.objectContaining({
          evidence: expect.objectContaining({
            instrumentationVerified: true,
            approvalInstanceId: 42,
          }),
        }),
      ),
    );
  });
  it("aguarda homologação sem selecionar retrabalho por ausência de parecer", () => {
    wrapper(
      <LearningCycleCommandForm
        cycle={{
          ...cycle,
          stage: "VALIDATION",
          commands: [
            {
              action: "COMPLETE",
              label: "Concluir etapa com evidência",
              available: false,
              reason: "Aguarde o gate aprovado",
            },
            {
              action: "REWORK",
              label: "Devolver para correção",
              available: true,
              reason: "Correção quando houver rejeição",
            },
          ],
        }}
        catalog={catalog}
        onUpdated={vi.fn()}
      />,
    );
    expect(screen.getByRole("combobox", { name: "Decisão *" })).toHaveValue(
      "COMPLETE",
    );
    expect(
      screen.getByRole("button", { name: "Concluir etapa com evidência" }),
    ).toBeDisabled();
    expect(
      screen.queryByLabelText("Nova versão a homologar *"),
    ).not.toBeInTheDocument();
  });
  it("apresenta decisão, experimento e bloqueios enviados pelo backend", async () => {
    wrapper(<LearningCyclesPage />);
    expect(
      await screen.findByRole("heading", {
        name: "Ciclo #2 · experimento #91",
      }),
    ).toBeInTheDocument();
    expect(
      await screen.findByDisplayValue("Corrija a microação útil com Dédalo."),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("option", { name: "Solicitar escala · bloqueado" }),
    ).toBeDisabled();
    expect(
      screen.queryByRole("button", {
        name: "Criar ciclo sucessor com aprendizado",
      }),
    ).not.toBeInTheDocument();
  });
  it("concilia a medição sem exibir nem enviar campos manuais de resultado", async () => {
    const baselineEvent = {
      id: 1,
      revision: 0,
      action: "ADOPT_BASELINE",
      fromStage: "MEASUREMENT",
      toStage: "MEASUREMENT",
      operatorName: "Operador",
      summary: "Referência histórica adotada",
      evidenceReference: "internal://campaign/91",
      evidence: {},
      createdAt: "2026-09-09T00:00:00Z",
    };
    let current = {
      ...cycle,
      revision: 0,
      stage: "MEASUREMENT",
      stageLabel: "Medir vendas e valor entregue",
      events: [baselineEvent],
      commands: [],
    } as LearningCycle;
    const original = vi.mocked(axios.get).getMockImplementation()!;
    vi.mocked(axios.get).mockImplementation(async (url, ...args) =>
      url === `${cycleApi}/products/4`
        ? { data: [current] }
        : original(url, ...args),
    );
    let finishReconciliation!: () => void;
    vi.mocked(axios.post).mockImplementation(async (url, body) => {
      if (url !== `${cycleApi}/products/4/2/measurement-reconciliation`)
        throw new Error(`Requisição não simulada: ${url}`);
      current = {
        ...current,
        revision: 1,
        stage: "DECISION",
        stageLabel: "Decisão comercial",
        events: [
          baselineEvent,
          {
            ...baselineEvent,
            id: 2,
            revision: 1,
            action: "MEASURE",
            toStage: "DECISION",
            operatorName: "Marketing Hub · backend",
            summary: "Conciliação automática concluída",
            evidenceReference: "internal://measurement/91",
            evidence: {
              automatic: true,
              dataValid: true,
              testDataExcluded: true,
              sessions: 4,
              netSales: 0,
              revenueBrl: 0,
              contributionBrl: -27.3,
              periodStart: "2026-09-07T03:00:00Z",
              periodEnd: "2026-09-09T01:00:00Z",
              source: "Fontes oficiais",
            },
          },
        ],
      };
      expect(body).toEqual({
        requestKey: expect.any(String),
        expectedRevision: 0,
      });
      await new Promise<void>((resolve) => {
        finishReconciliation = resolve;
      });
      return { data: current };
    });

    wrapper(<LearningCyclesPage />);

    expect(
      await screen.findByRole("heading", {
        name: "Conciliação automática de resultados",
      }),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith(
        `${cycleApi}/products/4/2/measurement-reconciliation`,
        expect.objectContaining({ expectedRevision: 0 }),
      ),
    );
    await act(async () => finishReconciliation());
    expect(
      screen.queryByLabelText("Sessões atribuídas *"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByLabelText("Responsável pela decisão *"),
    ).not.toBeInTheDocument();
  });
  it("mostra o bloqueio automático e permite somente retentar ou corrigir a fonte", async () => {
    const blocked = {
      ...cycle,
      stage: "MEASUREMENT",
      stageLabel: "Medir vendas e valor entregue",
      events: [
        {
          id: 9,
          revision: 3,
          action: "MEASUREMENT_BLOCKED",
          fromStage: "MEASUREMENT",
          toStage: "MEASUREMENT",
          operatorName: "Marketing Hub · backend",
          summary: "Conciliação automática bloqueada",
          evidenceReference: "internal://measurement-blocker/91",
          evidence: {
            automatic: true,
            dataValid: false,
            blocker: "A sincronização final da campanha está ausente.",
          },
          createdAt: "2026-09-09T01:00:00Z",
        },
      ],
      commands: [
        {
          action: "FIX_MEASUREMENT",
          label: "Corrigir medição neste ciclo",
          available: true,
          reason: "Corrija a fonte",
        },
      ],
    } as LearningCycle;
    const original = vi.mocked(axios.get).getMockImplementation()!;
    vi.mocked(axios.get).mockImplementation(async (url, ...args) =>
      url === `${cycleApi}/products/4`
        ? { data: [blocked] }
        : original(url, ...args),
    );

    wrapper(<LearningCyclesPage />);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "A sincronização final da campanha está ausente.",
    );
    expect(
      screen.getByRole("button", { name: "Tentar conciliação novamente" }),
    ).toBeEnabled();
    expect(
      screen.queryByLabelText("Receita líquida (R$) *"),
    ).not.toBeInTheDocument();
    expect(screen.getByLabelText("Falha da medição *")).toBeInTheDocument();
    expect(axios.post).not.toHaveBeenCalled();
  });
  it("mostra o losango e um retorno navegável para o aprendizado", () => {
    wrapper(
      <LearningCycleDiagram
        diagram={catalog.diagram}
        currentStage="LEARNING"
      />,
    );
    expect(
      screen.getByRole("img", { name: "Losango de decisão" }),
    ).toBeInTheDocument();
    const link = screen.getByRole("link", { name: /sucessor com memória/ });
    expect(
      document.getElementById(link.getAttribute("href")!.slice(1)),
    ).toHaveAttribute("aria-current", "step");
  });
  it("envia causa, destino, conhecimento, revisão e chave idempotente sem publicar mídia", async () => {
    const user = userEvent.setup();
    let finish: (value: unknown) => void = () => {};
    vi.mocked(axios.post).mockImplementation(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    );
    wrapper(
      <LearningCycleCommandForm
        cycle={cycle}
        catalog={catalog}
        onUpdated={() => {}}
      />,
    );
    for (const [label, value] of [
      ["Responsável pela decisão *", "Operador local"],
      ["Síntese e justificativa *", "Ação pouco concreta"],
      ["Referência da evidência *", "docs/relatorio"],
      ["Causa e impacto comercial *", "Valor pouco concreto"],
      ["Aprendizado preservado *", "Poucas sessões não rejeitam o produto"],
      ["Hipótese para o sucessor *", "Ação executável aumenta uso"],
    ])
      await user.type(screen.getByLabelText(label), value);
    await user.selectOptions(
      screen.getByLabelText("Atividade que corrigirá a causa *"),
      "3:rework",
    );
    await user.click(screen.getByRole("button", { name: "Preparar sucessor" }));
    expect(
      screen.getByRole("button", { name: /Preparar sucessor/ }),
    ).toBeDisabled();
    expect(axios.post).toHaveBeenCalledTimes(1);
    expect(axios.post).toHaveBeenCalledWith(
      `${cycleApi}/products/4/2/commands`,
      expect.objectContaining({
        expectedRevision: 3,
        action: "ADJUST",
        requestKey: expect.any(String),
        evidence: expect.objectContaining({
          returnProcessId: 3,
          returnActivityId: "rework",
          learning: "Poucas sessões não rejeitam o produto",
        }),
      }),
    );
    finish({ data: { ...cycle, status: "ADJUSTED" } });
    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: "Preparar sucessor" }),
      ).not.toBeDisabled(),
    );
  });
  it("exibe a memória herdada e permite abrir o predecessor sem substituí-lo pelo #90", async () => {
    const memory = {
      ...cycle,
      previousCycleId: 1,
      inheritedLearning: {
        cycleId: 1,
        experimentId: 90,
        productVersion: "v6",
        events: [
          {
            id: 4,
            action: "ADJUST",
            summary: "Primeiro ajuste precisa ser aplicável",
            evidenceReference: "docs/91",
            operatorName: "Hermes",
            createdAt: "2026-09-08T10:00:00Z",
            evidence: { learning: "Comprovar utilidade" },
          },
        ],
      },
    };
    const original = vi.mocked(axios.get).getMockImplementation()!;
    vi.mocked(axios.get).mockImplementation(async (url, ...args) =>
      url === `${cycleApi}/products/4`
        ? { data: [memory] }
        : original(url, ...args),
    );
    wrapper(<LearningCyclesPage />);
    expect(
      await screen.findByText("Aprendizado recebido do experimento #90"),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: "Ciclo #2 · experimento #91" }),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Aprendizado: Comprovar utilidade"),
    ).toBeInTheDocument();
  });
});

describe("Vídeos nos ciclos PDE", () => {
  it.each([
    ["CAMPAIGN_VIDEO", "campaignVideoAssetId", "Vídeo AD deste experimento"],
    [
      "PDE_ENTRY_VIDEO",
      "pdeVideoAssetId",
      "Vídeo LANDING_HERO deste experimento",
    ],
  ])(
    "seleciona somente a mídia oferecida pelo backend em %s",
    async (stage, key, label) => {
      const user = userEvent.setup();
      const videoCycle = {
        ...cycle,
        stage,
        evidenceOptions: {
          [key]: [{ id: 38, label: "Vídeo #38 · finalidade aprovada" }],
        },
        workLinks: [
          { label: "Produzir no Estúdio", url: "/audio-video-studio" },
        ],
        commands: [
          {
            action: "COMPLETE",
            label: "Concluir etapa com evidência",
            available: true,
            reason: "",
          },
        ],
      };
      vi.mocked(axios.post).mockRejectedValue({
        isAxiosError: true,
        response: { data: { detail: "O vídeo pertence a outro experimento." } },
      });
      vi.mocked(axios.isAxiosError).mockReturnValue(true);
      const updated = vi.fn();
      wrapper(
        <LearningCycleCommandForm
          cycle={videoCycle}
          catalog={catalog}
          onUpdated={updated}
        />,
      );
      expect(
        screen.getByRole("link", { name: "Produzir no Estúdio" }),
      ).toHaveAttribute("href", "/audio-video-studio");
      await user.type(
        screen.getByLabelText("Responsável pela decisão *"),
        "Operador local",
      );
      await user.type(
        screen.getByLabelText("Síntese e justificativa *"),
        "Produção concluída no Estúdio",
      );
      await user.type(
        screen.getByLabelText("Referência da evidência *"),
        "internal://fixture/video",
      );
      await user.selectOptions(screen.getByLabelText(`${label} *`), "38");
      await user.type(
        screen.getByLabelText(
          stage === "CAMPAIGN_VIDEO"
            ? "Evidência da produção no Estúdio *"
            : "Evidência da demonstração da versão real *",
        ),
        "internal://fixture/production",
      );
      await user.click(
        screen.getByRole("button", { name: "Concluir etapa com evidência" }),
      );
      await waitFor(() => expect(axios.post).toHaveBeenCalled());
      expect(vi.mocked(axios.post).mock.calls[0][1]).toMatchObject({
        expectedRevision: cycle.revision,
        evidence: { [key]: 38 },
      });
      expect(updated).not.toHaveBeenCalled();
      expect(await screen.findByRole("alert")).toHaveTextContent(
        "O vídeo pertence a outro experimento.",
      );
    },
  );

  it("preserva o diagrama da ocorrência antiga quando o catálogo evolui", async () => {
    const original = vi.mocked(axios.get).getMockImplementation()!;
    vi.mocked(axios.get).mockImplementation(async (url, ...args) =>
      url === `${cycleApi}/products/4`
        ? {
            data: [
              {
                ...cycle,
                diagram: {
                  nodes: [
                    {
                      id: "LEGACY",
                      type: "TASK",
                      label: "BPM histórico imutável",
                    },
                  ],
                  flows: [],
                },
              },
            ],
          }
        : original(url, ...args),
    );
    wrapper(<LearningCyclesPage />);
    await screen.findByRole("heading", { name: "Ciclo #2 · experimento #91" });
    await userEvent.click(
      screen.getByText("BPM · decisões e retornos do ciclo"),
    );
    expect(screen.getByText("BPM histórico imutável")).toBeVisible();
  });
});
