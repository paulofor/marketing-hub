import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, cleanup, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, it, expect, vi } from "vitest";
import axios from "axios";
import LearningCycleDecisionPanel from "./LearningCycleDecisionPanel";
import type {
  LearningCycle,
  CycleCatalog,
} from "../../api/learningCycle/useLearningCycles";
vi.mock("axios");
const cycle = {
  id: 1,
  productId: 4,
  experimentId: 91,
  revision: 1,
  stage: "DECISION",
  status: "OPEN",
  commands: [
    { action: "ADJUST", label: "Ajustar", available: true, reason: "" },
    { action: "STOP", label: "Encerrar", available: true, reason: "" },
  ],
  productVersion: "v7",
  responsible: "Atena",
  nextAction: "Revise a proposta",
} as LearningCycle;
const catalog = {
  returnTargets: [
    {
      processDefinitionId: 3,
      activityId: "rework",
      processName: "Processo 3",
      activityName: "Corrigir produto",
      owner: "Dédalo",
    },
  ],
} as CycleCatalog;
const draft = {
  id: 5,
  cycleId: 1,
  cycleRevision: 1,
  status: "READY",
  agentKey: "experiment-strategist",
  agentName: "Atena",
  agentId: 4,
  automaticExecutionEnabled: true,
  activityDefinitionId: 7,
  operatorName: "Responsável declarado",
  error: null,
  createdAt: null,
  finishedAt: null,
  approvedAt: null,
  approvedEventId: null,
  proposal: {
    contractVersion: "LEARNING_CYCLE_DECISION_PROPOSAL_V1",
    action: "ADJUST",
    summary: "Melhorar a primeira ação útil",
    rootCause: "Hipótese de esforço, sem causa comprovada",
    learning: "Amostra insuficiente",
    nextHypothesis: "Reduzir esforço e medir continuidade",
    evidenceLimits: "Quatro sessões não provam rejeição",
    correctionPlan: "Conferir fonte",
    scaleHypothesis: "Não se aplica",
    evidenceReference: "internal://measurement/2",
    returnProcessId: 3,
    returnActivityId: "rework",
    evidenceEventIds: [2],
    selectedAlternative: 0,
    alternatives: ["Produto", "Mensagem", "Dados"].map((option) => ({
      option,
      benefit: "Valor",
      risk: "Amostra",
      effort: "Médio",
      salesImpact: "Hipótese",
    })),
  },
};
afterEach(cleanup);
beforeEach(() => {
  vi.resetAllMocks();
  vi.mocked(axios.get).mockResolvedValue({ data: draft });
});
function mount(selectedCycle = cycle) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const updated = vi.fn();
  render(
    <MemoryRouter>
      <QueryClientProvider client={client}>
        <LearningCycleDecisionPanel
          cycle={selectedCycle}
          catalog={catalog}
          onUpdated={updated}
        />
      </QueryClientProvider>
    </MemoryRouter>,
  );
  return { client, updated };
}
it("preenche todos os campos e só envia após aprovação explícita", async () => {
  const user = userEvent.setup();
  const { updated } = mount();
  const summary = await screen.findByLabelText("Síntese e justificativa *");
  expect(summary).toHaveValue(draft.proposal.summary);
  expect(screen.getByLabelText("Responsável pela decisão *")).toHaveValue(
    draft.operatorName,
  );
  expect(screen.getByLabelText("Referência da evidência *")).toHaveValue(
    draft.proposal.evidenceReference,
  );
  expect(
    screen.getByLabelText("Atividade que corrigirá a causa *"),
  ).toHaveValue("3:rework");
  expect(screen.getByLabelText("Hipótese para o sucessor *")).toHaveValue(
    draft.proposal.nextHypothesis,
  );
  expect(axios.post).not.toHaveBeenCalled();
  await user.clear(summary);
  await user.type(summary, "Proposta editada pela pessoa");
  vi.mocked(axios.post).mockResolvedValue({
    data: { ...cycle, status: "ADJUSTED", revision: 2 },
  });
  await user.click(
    screen.getByRole("button", { name: "Aprovar decisão e registrar no BPM" }),
  );
  await waitFor(() => expect(updated).toHaveBeenCalled());
  expect(axios.post).toHaveBeenCalledWith(
    expect.stringContaining("/products/4/1/commands"),
    expect.objectContaining({
      summary: "Proposta editada pela pessoa",
      operatorName: draft.operatorName,
      evidence: expect.objectContaining({
        humanApproved: true,
        decisionProposalId: 5,
        returnProcessId: 3,
        returnActivityId: "rework",
      }),
    }),
  );
});
it("não sobrescreve a edição quando a consulta recebe novamente a mesma proposta", async () => {
  const user = userEvent.setup();
  const { client } = mount();
  const summary = await screen.findByLabelText("Síntese e justificativa *");
  await user.clear(summary);
  await user.type(summary, "Meu ajuste preservado");
  await client.invalidateQueries({ queryKey: ["cycle-decision-proposal"] });
  expect(summary).toHaveValue("Meu ajuste preservado");
  await user.selectOptions(screen.getByLabelText("Decisão *"), "STOP");
  expect(summary).toHaveValue("Meu ajuste preservado");
});
it("mostra a fila e impede formulário manual enquanto Atena não concluir", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: { ...draft, status: "RUNNING", proposal: null },
  });
  mount();
  expect(await screen.findByText(/Atena está preenchendo/)).toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: /Aprovar decisão/ }),
  ).not.toBeInTheDocument();
  expect(axios.post).not.toHaveBeenCalled();
});
it("exibe falha e solicita nova tentativa sem apagar a anterior", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: {
      ...draft,
      status: "FAILED",
      proposal: null,
      error: "Fonte indisponível",
    },
  });
  vi.mocked(axios.post).mockResolvedValue({ data: {} });
  const user = userEvent.setup();
  mount();
  expect(await screen.findByText("Fonte indisponível")).toBeInTheDocument();
  await user.click(
    screen.getByRole("button", { name: "Tentar novamente com Atena" }),
  );
  expect(axios.post).toHaveBeenCalledWith(
    expect.stringContaining("decision-proposal/retry"),
    { expectedRevision: 1, previousProposalId: 5 },
  );
});
it("não libera proposta de outra revisão e comunica falha de consulta", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: { ...draft, cycleRevision: 0 },
  });
  mount();
  await screen.findByText(/Quatro sessões/);
  expect(
    screen.queryByRole("button", { name: /Aprovar decisão/ }),
  ).not.toBeInTheDocument();
});
it("uma falha HTTP não se torna formulário vazio", async () => {
  vi.mocked(axios.get).mockRejectedValue(new Error("API indisponível"));
  mount();
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Não foi possível consultar a proposta",
  );
  expect(axios.post).not.toHaveBeenCalled();
});

it("apresenta revisão de mercado v2 com limites, adaptações e critério financeiro", async () => {
  const marketReview = {
    recommendedScope: "ADJACENT_SEGMENTS",
    currentAudience: "Nail designers",
    proposedAudience: "Cabeleireiros e barbeiros",
    sharedProblem: "Divulgar serviços sem criar tudo do zero",
    deliveryReadiness: "REQUIRES_ADAPTATION",
    requiredAdaptations: "Adaptar briefing, imagens e calendário por profissão",
    excludedAudiences: "Profissões sem entrega homologada",
    evidenceLimits: "Seis visitantes não comprovam mercado estreito",
    primaryMetric: "NET_CONTRIBUTION_AFTER_ACQUISITION",
    continueWhen: "Vendas líquidas, contribuição e entrega comprovadas",
    adjustWhen: "Interesse com falha corrigível",
    stopWhen: "Limite atingido ou entrega inviável",
    requiresNewCycle: true,
  };
  vi.mocked(axios.get).mockResolvedValue({
    data: {
      ...draft,
      proposal: {
        ...draft.proposal,
        contractVersion: "LEARNING_CYCLE_DECISION_PROPOSAL_V2",
        marketReview,
        selectedAlternative: 1,
        alternatives: draft.proposal.alternatives.map((value, index) => ({
          ...value,
          marketScope: ["KEEP_FOCUS", "ADJACENT_SEGMENTS", "BROAD_PROBLEM"][
            index
          ],
        })),
      },
    },
  });
  mount();
  expect(
    await screen.findByRole("region", { name: "Avaliação de mercado" }),
  ).toHaveTextContent("Cabeleireiros e barbeiros");
  expect(screen.getByText("Precisa adaptar a entrega")).toBeInTheDocument();
  expect(
    screen.getByText(/Novo ciclo e novo experimento obrigatórios/),
  ).toBeInTheDocument();
  expect(
    screen.getByText(/Contribuição líquida após aquisição/),
  ).toBeInTheDocument();
  expect(axios.post).not.toHaveBeenCalled();
});
it("rejeita resposta v2 incompleta sem liberar aprovação", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: {
      ...draft,
      proposal: {
        ...draft.proposal,
        contractVersion: "LEARNING_CYCLE_DECISION_PROPOSAL_V2",
      },
    },
  });
  mount();
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Não foi possível consultar",
  );
  expect(
    screen.queryByRole("button", { name: /Aprovar decisão/ }),
  ).not.toBeInTheDocument();
});
it("identifica a ausência de revisão de mercado no histórico v1", async () => {
  mount();
  expect(await screen.findByText(/Proposta histórica/)).toBeInTheDocument();
});

it("recupera o mesmo parecer sem transcrição, aprovação fictícia ou inferência", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: { ...draft, preparationAvailable: true },
  });
  const successor = { ...cycle, id: 12, experimentId: 101, stage: "PLANNING" };
  vi.mocked(axios.post).mockResolvedValue({ data: successor });
  const { updated } = mount();
  const button = await screen.findByRole("button", {
    name: "Preparar continuidade sem gasto",
  });
  expect(axios.post).not.toHaveBeenCalled();
  expect(
    screen.queryByLabelText("Responsável pela decisão *"),
  ).not.toBeInTheDocument();
  await userEvent.click(button);
  await waitFor(() => expect(updated).toHaveBeenCalledWith(successor));
  expect(axios.post).toHaveBeenCalledTimes(1);
  expect(axios.post).toHaveBeenCalledWith(
    "/api/business-process-chains/learning-cycles/v1/products/4/1/decision-proposal/prepare-successor",
  );
});

it("apresenta bloqueio sem reenviar o modelo nem fabricar sucesso", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: { ...draft, preparationAvailable: true },
  });
  vi.mocked(axios.post).mockRejectedValue(new Error("Produto em STOP"));
  const { updated } = mount();
  await userEvent.click(
    await screen.findByRole("button", {
      name: "Preparar continuidade sem gasto",
    }),
  );
  await screen.findByRole("alert");
  expect(updated).not.toHaveBeenCalled();
  expect(axios.post).toHaveBeenCalledTimes(1);
});

it("recupera decisão aprovada sem exigir nova aprovação no ciclo encerrado", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: { ...draft, status: "APPROVED", preparationAvailable: true },
  });
  const successor = { ...cycle, id: 13, stage: "PLANNING" };
  vi.mocked(axios.post).mockResolvedValue({ data: successor });
  const { updated } = mount({ ...cycle, status: "ADJUSTED", revision: 2 });
  expect(
    await screen.findByText(/Você não precisa preencher um novo formulário/),
  ).toHaveTextContent("sem iniciar agentes, consumir IA ou liberar mídia");
  await userEvent.click(
    await screen.findByRole("button", {
      name: "Preparar continuidade sem gasto",
    }),
  );
  await waitFor(() => expect(updated).toHaveBeenCalledWith(successor));
  expect(axios.post).toHaveBeenCalledTimes(1);
  expect(
    screen.queryByRole("button", { name: /Aprovar decisão/ }),
  ).not.toBeInTheDocument();
});

it("mostra a continuidade do inconclusivo aprovado sem pedir a mesma decisão novamente", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: {
      ...draft,
      status: "APPROVED",
      preparationAvailable: true,
      proposal: { ...draft.proposal, action: "INCONCLUSIVE" },
    },
  });
  const ended = {
    ...cycle,
    status: "INCONCLUSIVE",
    revision: 2,
    commands: [],
    nextAction:
      "Resultado inconclusivo preservado. Preparação do sucessor pelo backend.",
  };
  mount(ended);
  await screen.findByRole("button", {
    name: "Preparar continuidade sem gasto",
  });
  expect(
    screen.getByRole("heading", { name: "Preparar continuidade do produto" }),
  ).toBeInTheDocument();
  expect(
    screen.getByText(
      "Proposta #5 aprovada. A decisão final está no histórico do ciclo.",
    ),
  ).toBeInTheDocument();
  expect(
    screen.queryByRole("button", {
      name: "Aprovar decisão e registrar no BPM",
    }),
  ).not.toBeInTheDocument();
  expect(axios.post).not.toHaveBeenCalled();
});

it("apresenta a passagem persistida depois da aprovação sem sugerir outra análise", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: { ...draft, status: "APPROVED", preparationAvailable: false },
  });
  mount({
    ...cycle,
    status: "INCONCLUSIVE",
    commands: [],
    nextAction:
      "Continue no ciclo #9 · experimento #109, que recebeu o aprendizado.",
  });
  await screen.findByText("Decisão aprovada; continuidade do produto");
  expect(
    screen.getByText(
      "Continue no ciclo #9 · experimento #109, que recebeu o aprendizado.",
    ),
  ).toBeInTheDocument();
  expect(
    screen.queryByText("Atena prepara; você edita e aprova"),
  ).not.toBeInTheDocument();
  expect(axios.post).not.toHaveBeenCalled();
});
