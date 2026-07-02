import { FormEvent, useMemo, useState } from "react";
import {
  BrainCircuit,
  CalendarClock,
  CheckCircle2,
  Circle,
  PauseCircle,
  PlayCircle,
  Save,
} from "lucide-react";
import PageTitle from "../../components/PageTitle";
import { useExperiments } from "../../api/experiment/useExperiments";
import { useHypotheses } from "../../api/hypothesis/useHypotheses";
import { useNiches } from "../../api/niche/useNiches";
import {
  CommercialPlan,
  CommercialPlanMilestoneStatus,
  CommercialPlanSimulation,
  CommercialPlanStatus,
  SaveCommercialPlanPayload,
  useCommercialPlans,
  useCreateCommercialPlan,
  useSimulateCommercialPlan,
  useUpdateCommercialPlan,
} from "../../api/planning/useCommercialPlans";

const emptyForm: SaveCommercialPlanPayload = {
  name: "",
  status: "DRAFT",
  nicheId: null,
  hypothesisId: null,
  experimentId: null,
  commercialObjective: "",
  targetAudience: "",
  mainPain: "",
  mainOffer: "",
  mainLeadMagnet: "",
  mainChannel: "Meta Ads",
  mainMetric: "",
  successCriteria: "",
  stopCriteria: "",
  deadline: "",
  maxBudget: null,
  targetRevenue: null,
  operationalRevenueTarget: null,
  experimentsToCreate: null,
  experimentsToPublish: null,
  nextAction: "",
  currentBlocker: "",
  rootCause: "",
};

const julyPlanningForm: SaveCommercialPlanPayload = {
  name: "Planejamento Julho 2026 - Primeira venda",
  status: "IN_PROGRESS",
  nicheId: null,
  hypothesisId: null,
  experimentId: null,
  commercialObjective:
    "Chegar ate 31/07/2026 com a primeira venda validada de um produto digital low-ticket, usando Meta Ads com controle de aprendizado por tres cenarios.",
  targetAudience:
    "Nail designers, manicures e profissionais de alongamento que atendem em domicilio e dependem do WhatsApp para retorno, manutencao e encaixes.",
  mainPain:
    "A cliente sai satisfeita, mas some da manutencao, volta atrasada ou chama apenas quando quebra, descola ou precisa de urgencia.",
  mainOffer:
    "Kit Manutencao Guiada para Alongamento em Domicilio por R$ 19,90, com mensagens prontas, checklist e mini-calculadora de janela de manutencao.",
  mainLeadMagnet:
    "Amostra gratuita com 3 mensagens prontas de WhatsApp para recuperar clientes atrasadas na manutencao.",
  mainChannel:
    "Meta Ads com pagina propria de venda direta, captura secundaria e comparativo futuro com Instant Form.",
  mainMetric:
    "Compra aprovada; se ainda nao houver compra, clique no checkout como sinal intermediario.",
  successCriteria:
    "Cenario venda direta: ao menos 1 compra aprovada ate 31/07/2026. Cenario captura: lead-to-checkout indicando recuperacao real. Cenario Instant Form: leads que avancam para checkout apos a amostra.",
  stopCriteria:
    "Parar ou corrigir se houver CTR bom sem clique no checkout, clique no checkout sem compra, ou leads sem evolucao para checkout apos a amostra.",
  deadline: "2026-07-31",
  maxBudget: 300,
  targetRevenue: 27,
  operationalRevenueTarget: 81,
  experimentsToCreate: 2,
  experimentsToPublish: 3,
  nextAction:
    "Preparar produto compravel, pagina curta com checkout na primeira dobra e 3 criativos: dor, prova visual do kit e oferta direta.",
  currentBlocker:
    "Historico recente indicou clique e visualizacao sem envio/conversao, entao o gargalo esta depois do clique.",
  rootCause:
    "O funil mediu interesse antes de provar disposicao de pagamento; julho deve priorizar compra real e usar captura apenas como recuperacao.",
};

const statusLabel: Record<CommercialPlanStatus, string> = {
  DRAFT: "Rascunho",
  IN_PROGRESS: "Em andamento",
  BLOCKED: "Bloqueado",
  COMPLETED: "Concluído",
  CANCELLED: "Cancelado",
};

const milestoneStatusLabel: Record<CommercialPlanMilestoneStatus, string> = {
  PENDING: "Pendente",
  IN_PROGRESS: "Em andamento",
  DONE: "Concluído",
  BLOCKED: "Bloqueado",
};

const recommendationLabel: Record<
  CommercialPlanSimulation["recommendation"],
  string
> = {
  CONTINUE: "Continuar",
  CORRECT: "Corrigir",
  PAUSE: "Pausar",
  END: "Encerrar",
};

function asArray<T>(value: T[] | null | undefined): T[] {
  return Array.isArray(value) ? value : [];
}

function toForm(plan: CommercialPlan): SaveCommercialPlanPayload {
  return {
    name: plan.name,
    status: plan.status,
    nicheId: plan.nicheId ?? null,
    hypothesisId: plan.hypothesisId ?? null,
    experimentId: plan.experimentId ?? null,
    commercialObjective: plan.commercialObjective ?? "",
    targetAudience: plan.targetAudience ?? "",
    mainPain: plan.mainPain ?? "",
    mainOffer: plan.mainOffer ?? "",
    mainLeadMagnet: plan.mainLeadMagnet ?? "",
    mainChannel: plan.mainChannel ?? "",
    mainMetric: plan.mainMetric ?? "",
    successCriteria: plan.successCriteria ?? "",
    stopCriteria: plan.stopCriteria ?? "",
    deadline: plan.deadline ?? "",
    maxBudget: plan.maxBudget ?? null,
    targetRevenue: plan.targetRevenue ?? null,
    operationalRevenueTarget: plan.operationalRevenueTarget ?? null,
    experimentsToCreate: plan.experimentsToCreate ?? null,
    experimentsToPublish: plan.experimentsToPublish ?? null,
    nextAction: plan.nextAction ?? "",
    currentBlocker: plan.currentBlocker ?? "",
    rootCause: plan.rootCause ?? "",
  };
}

function statusClass(status: CommercialPlanStatus) {
  if (status === "BLOCKED") return "text-bg-danger";
  if (status === "IN_PROGRESS") return "text-bg-primary";
  if (status === "COMPLETED") return "text-bg-success";
  if (status === "CANCELLED") return "text-bg-secondary";
  return "text-bg-warning";
}

function milestoneIcon(status: CommercialPlanMilestoneStatus) {
  if (status === "DONE")
    return (
      <CheckCircle2 size={18} className="text-success" aria-hidden="true" />
    );
  if (status === "IN_PROGRESS")
    return <PlayCircle size={18} className="text-primary" aria-hidden="true" />;
  if (status === "BLOCKED")
    return <PauseCircle size={18} className="text-danger" aria-hidden="true" />;
  return <Circle size={18} className="text-secondary" aria-hidden="true" />;
}

function formatCurrency(value?: number | null) {
  if (value == null) return "Não definido";
  return value.toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
  });
}

function formatNumber(value?: number | null) {
  return value == null ? "Não definido" : String(value);
}

function formatExecutedNumber(value?: number | null) {
  return value == null ? "0" : String(value);
}

function numberOrNull(value?: number | null) {
  return value == null ? null : Number(value);
}

export default function CommercialPlanningPage() {
  const plansQuery = useCommercialPlans();
  const nichesQuery = useNiches();
  const hypothesesQuery = useHypotheses("ALL");
  const experimentsQuery = useExperiments();
  const createPlan = useCreateCommercialPlan();
  const updatePlan = useUpdateCommercialPlan();
  const simulatePlan = useSimulateCommercialPlan();
  const plans = asArray(plansQuery.data);
  const niches = asArray(nichesQuery.data);
  const hypotheses = asArray(hypothesesQuery.data);
  const experiments = asArray(experimentsQuery.data);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const selectedPlan = useMemo(
    () => plans.find((plan) => plan.id === selectedId) ?? plans[0],
    [plans, selectedId],
  );
  const [form, setForm] = useState<SaveCommercialPlanPayload>(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [simulationFeedback, setSimulationFeedback] = useState<{
    planId: number;
    simulation?: CommercialPlanSimulation;
    error?: string;
  } | null>(null);
  const visibleSimulation =
    simulationFeedback?.planId === selectedPlan?.id &&
    simulationFeedback.simulation
      ? simulationFeedback.simulation
      : asArray(selectedPlan?.simulations)[0];
  const selectedMilestones = asArray(selectedPlan?.milestones);

  function updateField<K extends keyof SaveCommercialPlanPayload>(
    field: K,
    value: SaveCommercialPlanPayload[K],
  ) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function startEdit(plan: CommercialPlan) {
    setEditingId(plan.id);
    setForm(toForm(plan));
  }

  function resetForm() {
    setEditingId(null);
    setForm(emptyForm);
  }

  function useJulyPlanning() {
    setEditingId(null);
    setForm(julyPlanningForm);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const payload = {
      ...form,
      nicheId: form.nicheId ? Number(form.nicheId) : null,
      hypothesisId: form.hypothesisId ? String(form.hypothesisId) : null,
      experimentId: form.experimentId ? Number(form.experimentId) : null,
      maxBudget: numberOrNull(form.maxBudget),
      targetRevenue: numberOrNull(form.targetRevenue),
      operationalRevenueTarget: numberOrNull(form.operationalRevenueTarget),
      experimentsToCreate: numberOrNull(form.experimentsToCreate),
      experimentsToPublish: numberOrNull(form.experimentsToPublish),
    };
    if (editingId) {
      await updatePlan.mutateAsync({ id: editingId, payload });
    } else {
      const created = await createPlan.mutateAsync(payload);
      setSelectedId(created.id);
    }
    resetForm();
  }

  async function handleSimulate(plan: CommercialPlan) {
    setSimulationFeedback(null);
    try {
      const simulation = await simulatePlan.mutateAsync({
        id: plan.id,
        decisionNotes: "Simulação manual solicitada na tela de Planejamento.",
      });
      setSelectedId(plan.id);
      setSimulationFeedback({ planId: plan.id, simulation });
    } catch {
      setSimulationFeedback({
        planId: plan.id,
        error:
          "Não foi possível gerar o cenário. A simulação não foi registrada.",
      });
    }
  }

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column flex-xl-row justify-content-between gap-3">
        <div>
          <PageTitle>Planejamento</PageTitle>
          <p className="text-secondary mb-0">
            Direção comercial para manter nicho, hipótese, oferta, campanha e
            landing conectados à primeira venda.
          </p>
        </div>
        <div className="d-flex align-items-center gap-2 text-secondary">
          <CalendarClock size={18} aria-hidden="true" />
          <span>
            Plano ativo precisa de prazo, métrica e critério de parada.
          </span>
        </div>
      </header>

      {plansQuery.isError ? (
        <div className="alert alert-danger mb-0" role="alert">
          Não foi possível carregar os planos comerciais.
        </div>
      ) : null}

      <section className="row g-3">
        <div className="col-12 col-xl-4">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <h2 className="h6 mb-3">Planos de Primeira Venda</h2>
              {plansQuery.isLoading ? (
                <div className="placeholder-glow">
                  <span className="placeholder col-12" />
                </div>
              ) : null}
              {!plansQuery.isLoading && plans.length === 0 ? (
                <p className="text-secondary mb-0">
                  Nenhum plano cadastrado ainda.
                </p>
              ) : null}
              <div className="list-group list-group-flush">
                {plans.map((plan) => (
                  <button
                    type="button"
                    key={plan.id}
                    className={`list-group-item list-group-item-action px-0 ${selectedPlan?.id === plan.id ? "active" : ""}`}
                    onClick={() => setSelectedId(plan.id)}
                  >
                    <div className="d-flex justify-content-between gap-2">
                      <strong>{plan.name}</strong>
                      <span className={`badge ${statusClass(plan.status)}`}>
                        {statusLabel[plan.status]}
                      </span>
                    </div>
                    <small>
                      {plan.nextAction || "Defina a próxima ação comercial"}
                    </small>
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>

        <div className="col-12 col-xl-8">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              {selectedPlan ? (
                <div className="d-flex flex-column gap-4">
                  <div className="d-flex flex-wrap justify-content-between gap-3">
                    <div>
                      <span
                        className={`badge ${statusClass(selectedPlan.status)} mb-2`}
                      >
                        {statusLabel[selectedPlan.status]}
                      </span>
                      <h2 className="h4 mb-1">{selectedPlan.name}</h2>
                      <p className="text-secondary mb-0">
                        {selectedPlan.commercialObjective ||
                          "Objetivo comercial ainda não definido."}
                      </p>
                    </div>
                    <div className="d-flex flex-wrap gap-2">
                      <button
                        className="btn btn-outline-primary"
                        type="button"
                        onClick={() => startEdit(selectedPlan)}
                      >
                        Editar
                      </button>
                      <button
                        className="btn btn-primary d-inline-flex align-items-center gap-2"
                        type="button"
                        disabled={simulatePlan.isPending}
                        onClick={() => handleSimulate(selectedPlan)}
                      >
                        {simulatePlan.isPending ? (
                          <span
                            className="spinner-border spinner-border-sm"
                            aria-hidden="true"
                          />
                        ) : (
                          <BrainCircuit size={18} aria-hidden="true" />
                        )}
                        {simulatePlan.isPending
                          ? "Simulando..."
                          : "Simular cenários"}
                      </button>
                    </div>
                  </div>

                  {simulationFeedback?.planId === selectedPlan.id &&
                  simulationFeedback.error ? (
                    <div className="alert alert-danger mb-0" role="alert">
                      {simulationFeedback.error}
                    </div>
                  ) : null}

                  {visibleSimulation ? (
                    <section className="border rounded-3 p-3 bg-light">
                      <div className="d-flex flex-wrap justify-content-between gap-2 mb-2">
                        <h3 className="h6 mb-0">Última simulação</h3>
                        <span className="badge text-bg-primary">
                          {
                            recommendationLabel[
                              visibleSimulation.recommendation
                            ]
                          }
                        </span>
                      </div>
                      <div className="row g-3">
                        <div className="col-12 col-lg-4">
                          <p className="text-secondary small mb-1">
                            Cenário provável
                          </p>
                          <p className="mb-0">
                            {visibleSimulation.mostLikelyScenario ||
                              "Sem cenário registrado."}
                          </p>
                        </div>
                        <div className="col-12 col-lg-4">
                          <p className="text-secondary small mb-1">
                            Melhor realista
                          </p>
                          <p className="mb-0">
                            {visibleSimulation.bestRealisticScenario ||
                              "Sem melhor cenário registrado."}
                          </p>
                        </div>
                        <div className="col-12 col-lg-4">
                          <p className="text-secondary small mb-1">
                            Pior provável
                          </p>
                          <p className="mb-0">
                            {visibleSimulation.worstLikelyScenario ||
                              "Sem pior cenário registrado."}
                          </p>
                        </div>
                      </div>
                      <div className="row g-3 mt-1">
                        <div className="col-12 col-lg-4">
                          <p className="text-secondary small mb-1">
                            Risco principal
                          </p>
                          <p className="mb-0">
                            {visibleSimulation.mainRisk ||
                              "Sem risco registrado."}
                          </p>
                        </div>
                        <div className="col-12 col-lg-4">
                          <p className="text-secondary small mb-1">
                            Próxima ação
                          </p>
                          <p className="mb-0">
                            {visibleSimulation.bestNextAction ||
                              "Sem ação recomendada."}
                          </p>
                        </div>
                        <div className="col-12 col-lg-4">
                          <p className="text-secondary small mb-1">
                            Condição de parada
                          </p>
                          <p className="mb-0">
                            {visibleSimulation.stopCondition ||
                              "Sem condição registrada."}
                          </p>
                        </div>
                      </div>
                    </section>
                  ) : null}

                  <section>
                    <h3 className="h6">Direção de julho em 3 cenários</h3>
                    <div className="row g-3">
                      <div className="col-12 col-lg-4">
                        <div className="border rounded-3 p-3 h-100">
                          <p className="text-secondary small mb-1">
                            Prioridade
                          </p>
                          <strong>Venda direta</strong>
                          <p className="mb-0 mt-2">
                            Produto pronto, baixo preco, checkout visivel e
                            compra como principal sinal de validacao.
                          </p>
                        </div>
                      </div>
                      <div className="col-12 col-lg-4">
                        <div className="border rounded-3 p-3 h-100">
                          <p className="text-secondary small mb-1">
                            Recuperação
                          </p>
                          <strong>Captura secundaria</strong>
                          <p className="mb-0 mt-2">
                            Amostra gratuita para recuperar quem nao comprou,
                            sem competir com a venda principal.
                          </p>
                        </div>
                      </div>
                      <div className="col-12 col-lg-4">
                        <div className="border rounded-3 p-3 h-100">
                          <p className="text-secondary small mb-1">
                            Comparativo
                          </p>
                          <strong>Instant Form</strong>
                          <p className="mb-0 mt-2">
                            Teste controlado quando a amostra personalizada
                            puder aumentar desejo e avanco para checkout.
                          </p>
                        </div>
                      </div>
                    </div>
                  </section>

                  <div className="row g-3">
                    <div className="col-12 col-md-3">
                      <div className="border rounded-3 p-3 h-100">
                        <p className="text-secondary small mb-1">
                          Dias restantes
                        </p>
                        <strong className="h4 mb-0">
                          {selectedPlan.daysRemaining}
                        </strong>
                      </div>
                    </div>
                    <div className="col-12 col-md-3">
                      <div className="border rounded-3 p-3 h-100">
                        <p className="text-secondary small mb-1">Métrica</p>
                        <strong>
                          {selectedPlan.mainMetric || "Não definida"}
                        </strong>
                      </div>
                    </div>
                    <div className="col-12 col-md-3">
                      <div className="border rounded-3 p-3 h-100">
                        <p className="text-secondary small mb-1">Oferta</p>
                        <strong>
                          {selectedPlan.mainOffer || "Não definida"}
                        </strong>
                      </div>
                    </div>
                    <div className="col-12 col-md-3">
                      <div className="border rounded-3 p-3 h-100">
                        <p className="text-secondary small mb-1">Canal</p>
                        <strong>
                          {selectedPlan.mainChannel || "Não definido"}
                        </strong>
                      </div>
                    </div>
                  </div>

                  <section>
                    <h3 className="h6">Metas numéricas do mês</h3>
                    <div className="row g-3">
                      <div className="col-12 col-md-3">
                        <div className="border rounded-3 p-3 h-100">
                          <p className="text-secondary small mb-1">
                            Custo máximo
                          </p>
                          <strong>{formatCurrency(selectedPlan.maxBudget)}</strong>
                          <div className="text-secondary small">
                            Executado:{" "}
                            {formatCurrency(selectedPlan.actualTotalCost)}
                          </div>
                        </div>
                      </div>
                      <div className="col-12 col-md-3">
                        <div className="border rounded-3 p-3 h-100">
                          <p className="text-secondary small mb-1">
                            Receita mínima
                          </p>
                          <strong>
                            {formatCurrency(selectedPlan.targetRevenue)}
                          </strong>
                          <div className="text-secondary small">
                            Executado:{" "}
                            {formatCurrency(selectedPlan.actualRevenue)}
                          </div>
                        </div>
                      </div>
                      <div className="col-12 col-md-3">
                        <div className="border rounded-3 p-3 h-100">
                          <p className="text-secondary small mb-1">
                            Receita operacional
                          </p>
                          <strong>
                            {formatCurrency(
                              selectedPlan.operationalRevenueTarget,
                            )}
                          </strong>
                          <div className="text-secondary small">
                            Realizado vs operacional:{" "}
                            {formatCurrency(selectedPlan.actualRevenue)}
                          </div>
                        </div>
                      </div>
                      <div className="col-12 col-md-3">
                        <div className="border rounded-3 p-3 h-100">
                          <p className="text-secondary small mb-1">
                            Experimentos
                          </p>
                          <strong>
                            {formatNumber(selectedPlan.experimentsToCreate)}{" "}
                            criados /{" "}
                            {formatNumber(selectedPlan.experimentsToPublish)}{" "}
                            publicados
                          </strong>
                          <div className="text-secondary small">
                            Executado:{" "}
                            {formatExecutedNumber(
                              selectedPlan.actualExperimentsCreated,
                            )}{" "}
                            criados /{" "}
                            {formatExecutedNumber(
                              selectedPlan.actualExperimentsPublished,
                            )}{" "}
                            publicados
                          </div>
                        </div>
                      </div>
                    </div>
                    <div className="row g-3 mt-1">
                      <div className="col-12 col-md-4">
                        <div className="border rounded-3 p-3 h-100">
                          <p className="text-secondary small mb-1">
                            Custo de campanha executado
                          </p>
                          <strong>
                            {formatCurrency(selectedPlan.actualCampaignCost)}
                          </strong>
                        </div>
                      </div>
                      <div className="col-12 col-md-4">
                        <div className="border rounded-3 p-3 h-100">
                          <p className="text-secondary small mb-1">
                            Custo de IA executado
                          </p>
                          <strong>
                            {formatCurrency(selectedPlan.actualAiCost)}
                          </strong>
                        </div>
                      </div>
                      <div className="col-12 col-md-4">
                        <div className="border rounded-3 p-3 h-100">
                          <p className="text-secondary small mb-1">
                            Última sincronização
                          </p>
                          <strong>
                            {selectedPlan.executionSyncedAt
                              ? new Date(
                                  selectedPlan.executionSyncedAt,
                                ).toLocaleString("pt-BR")
                              : "Não sincronizado"}
                          </strong>
                        </div>
                      </div>
                    </div>
                  </section>

                  <section>
                    <h3 className="h6">Próxima ação</h3>
                    <p className="mb-1">
                      {selectedPlan.nextAction || "Sem próxima ação definida."}
                    </p>
                    <p className="text-secondary mb-0">
                      Gargalo:{" "}
                      {selectedPlan.currentBlocker ||
                        "nenhum bloqueio registrado."}
                    </p>
                  </section>

                  <section>
                    <h3 className="h6">Cenário futuro</h3>
                    <div className="row g-3">
                      <div className="col-12 col-lg-4">
                        <p className="text-secondary small mb-1">
                          Mais provável
                        </p>
                        <p className="mb-0">
                          {selectedPlan.mostLikelyScenario ||
                            "Execute uma simulação para registrar o cenário."}
                        </p>
                      </div>
                      <div className="col-12 col-lg-4">
                        <p className="text-secondary small mb-1">
                          Risco principal
                        </p>
                        <p className="mb-0">
                          {selectedPlan.mainFutureRisk || "Sem risco simulado."}
                        </p>
                      </div>
                      <div className="col-12 col-lg-4">
                        <p className="text-secondary small mb-1">
                          Ação a evitar
                        </p>
                        <p className="mb-0">
                          {selectedPlan.actionToAvoid ||
                            "Sem ação a evitar registrada."}
                        </p>
                      </div>
                    </div>
                  </section>

                  <section className="border rounded-3 p-3 bg-light">
                    <h3 className="h6">Pronto para ligar com IA</h3>
                    <div className="row g-3">
                      <div className="col-12 col-lg-4">
                        <p className="text-secondary small mb-1">Entrada</p>
                        <p className="mb-0">
                          objetivo, prazo, nicho, dor, oferta, canal, historico
                          e restricoes do plano.
                        </p>
                      </div>
                      <div className="col-12 col-lg-4">
                        <p className="text-secondary small mb-1">Saída</p>
                        <p className="mb-0">
                          tres cenarios comparaveis com risco, acao, evidencia
                          esperada e criterio de parada.
                        </p>
                      </div>
                      <div className="col-12 col-lg-4">
                        <p className="text-secondary small mb-1">Gate</p>
                        <p className="mb-0">
                          IA recomenda, mas gasto, publicacao e mudanca de rota
                          ficam condicionados a aprovacao e dados persistidos.
                        </p>
                      </div>
                    </div>
                  </section>

                  <section>
                    <h3 className="h6">Marcos comerciais</h3>
                    {selectedMilestones.length > 0 ? (
                      <div className="d-flex flex-column gap-2">
                        {selectedMilestones.map((milestone) => (
                          <div
                            className="d-flex align-items-start gap-2 border rounded-3 p-2"
                            key={milestone.id}
                          >
                            {milestoneIcon(milestone.status)}
                            <div>
                              <strong>
                                {milestone.sequenceOrder}. {milestone.name}
                              </strong>
                              <div className="text-secondary small">
                                {milestoneStatusLabel[milestone.status]} ·{" "}
                                {milestone.recommendedNextAction ||
                                  "sem ação recomendada"}
                              </div>
                              <div className="text-secondary small">
                                Planejado: custo{" "}
                                {formatCurrency(milestone.targetCost)} · receita{" "}
                                {formatCurrency(milestone.targetRevenue)} ·
                                Exp.{" "}
                                {formatNumber(milestone.experimentsToCreate)}
                                /{formatNumber(milestone.experimentsToPublish)}
                              </div>
                              <div className="text-secondary small">
                                Executado: custo{" "}
                                {formatCurrency(milestone.actualTotalCost)}{" "}
                                (campanha{" "}
                                {formatCurrency(
                                  milestone.actualCampaignCost,
                                )}{" "}
                                + IA {formatCurrency(milestone.actualAiCost)}) ·
                                receita{" "}
                                {formatCurrency(milestone.actualRevenue)} ·
                                Exp.{" "}
                                {formatExecutedNumber(
                                  milestone.actualExperimentsCreated,
                                )}
                                /
                                {formatExecutedNumber(
                                  milestone.actualExperimentsPublished,
                                )}
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <p className="text-secondary mb-0">
                        Nenhum marco cadastrado para este plano.
                      </p>
                    )}
                  </section>
                </div>
              ) : (
                <p className="text-secondary mb-0">
                  Crie um plano para visualizar a direção comercial.
                </p>
              )}
            </div>
          </div>
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-center gap-3 mb-3">
            <h2 className="h5 mb-0">
              {editingId ? "Editar plano" : "Novo Plano de Primeira Venda"}
            </h2>
            <div className="d-flex flex-wrap gap-2">
              <button
                type="button"
                className="btn btn-outline-primary btn-sm"
                onClick={useJulyPlanning}
              >
                Usar planejamento de julho
              </button>
              {editingId ? (
                <button
                  type="button"
                  className="btn btn-outline-secondary btn-sm"
                  onClick={resetForm}
                >
                  Cancelar edição
                </button>
              ) : null}
            </div>
          </div>
          <form className="row g-3" onSubmit={handleSubmit}>
            <div className="col-12 col-md-6">
              <label className="form-label">Nome do plano *</label>
              <input
                className="form-control"
                required
                value={form.name}
                onChange={(event) => updateField("name", event.target.value)}
              />
            </div>
            <div className="col-12 col-md-3">
              <label className="form-label">Status</label>
              <select
                className="form-select"
                value={form.status}
                onChange={(event) =>
                  updateField(
                    "status",
                    event.target.value as CommercialPlanStatus,
                  )
                }
              >
                {Object.entries(statusLabel).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-12 col-md-3">
              <label className="form-label">Prazo final</label>
              <input
                className="form-control"
                type="date"
                value={form.deadline ?? ""}
                onChange={(event) =>
                  updateField("deadline", event.target.value)
                }
              />
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label">Nicho</label>
              <select
                className="form-select"
                value={form.nicheId ?? ""}
                onChange={(event) =>
                  updateField(
                    "nicheId",
                    event.target.value ? Number(event.target.value) : null,
                  )
                }
              >
                <option value="">Sem vínculo</option>
                {niches.map((niche) => (
                  <option key={niche.id} value={niche.id}>
                    {niche.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label">Hipótese</label>
              <select
                className="form-select"
                value={form.hypothesisId ?? ""}
                onChange={(event) =>
                  updateField("hypothesisId", event.target.value || null)
                }
              >
                <option value="">Sem vínculo</option>
                {hypotheses.map((hypothesis) => (
                  <option key={hypothesis.id} value={hypothesis.id}>
                    {hypothesis.title}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label">Experimento</label>
              <select
                className="form-select"
                value={form.experimentId ?? ""}
                onChange={(event) =>
                  updateField(
                    "experimentId",
                    event.target.value ? Number(event.target.value) : null,
                  )
                }
              >
                <option value="">Sem vínculo</option>
                {experiments.map((experiment) => (
                  <option key={experiment.id} value={experiment.id}>
                    {experiment.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-12">
              <label className="form-label">Objetivo comercial</label>
              <textarea
                className="form-control"
                rows={2}
                value={form.commercialObjective ?? ""}
                onChange={(event) =>
                  updateField("commercialObjective", event.target.value)
                }
              />
            </div>
            <div className="col-12 col-md-6">
              <label className="form-label">Público alvo</label>
              <input
                className="form-control"
                value={form.targetAudience ?? ""}
                onChange={(event) =>
                  updateField("targetAudience", event.target.value)
                }
              />
            </div>
            <div className="col-12 col-md-6">
              <label className="form-label">Dor principal</label>
              <input
                className="form-control"
                value={form.mainPain ?? ""}
                onChange={(event) =>
                  updateField("mainPain", event.target.value)
                }
              />
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label">Oferta principal</label>
              <input
                className="form-control"
                value={form.mainOffer ?? ""}
                onChange={(event) =>
                  updateField("mainOffer", event.target.value)
                }
              />
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label">Isca principal</label>
              <input
                className="form-control"
                value={form.mainLeadMagnet ?? ""}
                onChange={(event) =>
                  updateField("mainLeadMagnet", event.target.value)
                }
              />
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label">Canal principal</label>
              <input
                className="form-control"
                value={form.mainChannel ?? ""}
                onChange={(event) =>
                  updateField("mainChannel", event.target.value)
                }
              />
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label">Métrica principal</label>
              <input
                className="form-control"
                value={form.mainMetric ?? ""}
                onChange={(event) =>
                  updateField("mainMetric", event.target.value)
                }
              />
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label">Orçamento máximo</label>
              <input
                className="form-control"
                type="number"
                min="0"
                step="0.01"
                value={form.maxBudget ?? ""}
                onChange={(event) =>
                  updateField(
                    "maxBudget",
                    event.target.value ? Number(event.target.value) : null,
                  )
                }
              />
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label">Receita mínima</label>
              <input
                className="form-control"
                type="number"
                min="0"
                step="0.01"
                value={form.targetRevenue ?? ""}
                onChange={(event) =>
                  updateField(
                    "targetRevenue",
                    event.target.value ? Number(event.target.value) : null,
                  )
                }
              />
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label">Meta operacional de receita</label>
              <input
                className="form-control"
                type="number"
                min="0"
                step="0.01"
                value={form.operationalRevenueTarget ?? ""}
                onChange={(event) =>
                  updateField(
                    "operationalRevenueTarget",
                    event.target.value ? Number(event.target.value) : null,
                  )
                }
              />
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label">Experimentos criados</label>
              <input
                className="form-control"
                type="number"
                min="0"
                step="1"
                value={form.experimentsToCreate ?? ""}
                onChange={(event) =>
                  updateField(
                    "experimentsToCreate",
                    event.target.value ? Number(event.target.value) : null,
                  )
                }
              />
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label">Experimentos publicados</label>
              <input
                className="form-control"
                type="number"
                min="0"
                step="1"
                value={form.experimentsToPublish ?? ""}
                onChange={(event) =>
                  updateField(
                    "experimentsToPublish",
                    event.target.value ? Number(event.target.value) : null,
                  )
                }
              />
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label">Próxima ação</label>
              <input
                className="form-control"
                value={form.nextAction ?? ""}
                onChange={(event) =>
                  updateField("nextAction", event.target.value)
                }
              />
            </div>
            <div className="col-12 col-md-6">
              <label className="form-label">Critério de sucesso</label>
              <textarea
                className="form-control"
                rows={2}
                value={form.successCriteria ?? ""}
                onChange={(event) =>
                  updateField("successCriteria", event.target.value)
                }
              />
            </div>
            <div className="col-12 col-md-6">
              <label className="form-label">Critério de parada</label>
              <textarea
                className="form-control"
                rows={2}
                value={form.stopCriteria ?? ""}
                onChange={(event) =>
                  updateField("stopCriteria", event.target.value)
                }
              />
            </div>
            <div className="col-12 col-md-6">
              <label className="form-label">Bloqueio atual</label>
              <input
                className="form-control"
                value={form.currentBlocker ?? ""}
                onChange={(event) =>
                  updateField("currentBlocker", event.target.value)
                }
              />
            </div>
            <div className="col-12 col-md-6">
              <label className="form-label">Causa-raiz</label>
              <input
                className="form-control"
                value={form.rootCause ?? ""}
                onChange={(event) =>
                  updateField("rootCause", event.target.value)
                }
              />
            </div>
            <div className="col-12">
              <button
                className="btn btn-primary d-inline-flex align-items-center gap-2"
                type="submit"
                disabled={createPlan.isPending || updatePlan.isPending}
              >
                <Save size={18} aria-hidden="true" />
                Salvar plano
              </button>
            </div>
          </form>
        </div>
      </section>
    </div>
  );
}
