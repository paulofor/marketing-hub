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
function mount() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const updated = vi.fn();
  render(
    <MemoryRouter>
      <QueryClientProvider client={client}>
        <LearningCycleDecisionPanel
          cycle={cycle}
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
