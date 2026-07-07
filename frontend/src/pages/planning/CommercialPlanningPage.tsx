import { useMemo } from "react";
import PageTitle from "../../components/PageTitle";
import {
  CommercialPlan,
  CommercialPlanWeek,
  CommercialPlanStatus,
  SaveCommercialPlanPayload,
  useCommercialPlanWeeks,
  useCommercialPlans,
} from "../../api/planning/useCommercialPlans";
import "./CommercialPlanningPage.css";

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

function asArray<T>(value: T[] | null | undefined): T[] {
  return Array.isArray(value) ? value : [];
}

function resolvePlanStatus(status?: string | null): CommercialPlanStatus {
  return status && status in statusLabel
    ? (status as CommercialPlanStatus)
    : "DRAFT";
}

function statusClass(status: CommercialPlanStatus) {
  if (status === "BLOCKED") return "text-bg-danger";
  if (status === "IN_PROGRESS") return "text-bg-primary";
  if (status === "COMPLETED") return "text-bg-success";
  if (status === "CANCELLED") return "text-bg-secondary";
  return "text-bg-warning";
}

function planStatusLabel(status?: string | null) {
  return statusLabel[resolvePlanStatus(status)];
}

function planStatusClass(status?: string | null) {
  return statusClass(resolvePlanStatus(status));
}

function formatCurrency(value?: number | null) {
  if (value == null) return "Não definido";
  return value.toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
  });
}

function formatExecutedCurrency(value?: number | null) {
  return (value ?? 0).toLocaleString("pt-BR", {
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

function formatDate(value?: string | null) {
  if (!value) return "Não definido";
  return new Date(value).toLocaleDateString("pt-BR", { timeZone: "UTC" });
}

function progressPercentage(target?: number | null, actual?: number | null) {
  if (!target || target <= 0) return 0;
  return Math.min(100, Math.round(((actual ?? 0) / target) * 100));
}

function fallbackMonthPlan(): CommercialPlan {
  return {
    id: 0,
    name: julyPlanningForm.name,
    planType: "FIRST_SALE",
    status: julyPlanningForm.status ?? "DRAFT",
    commercialObjective: julyPlanningForm.commercialObjective,
    targetAudience: julyPlanningForm.targetAudience,
    mainPain: julyPlanningForm.mainPain,
    mainOffer: julyPlanningForm.mainOffer,
    mainLeadMagnet: julyPlanningForm.mainLeadMagnet,
    mainChannel: julyPlanningForm.mainChannel,
    mainMetric: julyPlanningForm.mainMetric,
    successCriteria: julyPlanningForm.successCriteria,
    stopCriteria: julyPlanningForm.stopCriteria,
    deadline: julyPlanningForm.deadline,
    maxBudget: julyPlanningForm.maxBudget,
    targetRevenue: julyPlanningForm.targetRevenue,
    operationalRevenueTarget: julyPlanningForm.operationalRevenueTarget,
    experimentsToCreate: julyPlanningForm.experimentsToCreate,
    experimentsToPublish: julyPlanningForm.experimentsToPublish,
    actualCampaignCost: null,
    actualAiCost: null,
    actualTotalCost: null,
    actualRevenue: null,
    actualExperimentsCreated: null,
    actualExperimentsPublished: null,
    daysRemaining: 25,
    nextAction: julyPlanningForm.nextAction,
    currentBlocker: julyPlanningForm.currentBlocker,
    rootCause: julyPlanningForm.rootCause,
    milestones: [],
    simulations: [],
  };
}

function MonthlyMetricCard({
  label,
  target,
  actual,
  percentage,
  tone = "primary",
}: {
  label: string;
  target: string;
  actual: string;
  percentage: number;
  tone?: "primary" | "success" | "warning" | "info";
}) {
  return (
    <div className="commercial-planning-month-metric">
      <div className="d-flex justify-content-between align-items-start gap-2">
        <span className="commercial-planning-month-metric-label">{label}</span>
        <span className={`badge text-bg-${tone}`}>{percentage}%</span>
      </div>
      <div className="commercial-planning-month-metric-values">
        <strong>{target}</strong>
        <span>Meta</span>
      </div>
      <div className="commercial-planning-month-metric-values executed">
        <strong>{actual}</strong>
        <span>Execução</span>
      </div>
      <div className="commercial-planning-month-progress" aria-hidden="true">
        <span style={{ width: `${percentage}%` }} />
      </div>
    </div>
  );
}

function WeeklyExperimentTable({ weeks }: { weeks: CommercialPlanWeek[] }) {
  return (
    <section className="commercial-planning-week-list">
      {weeks.map((week) => (
        <article className="commercial-planning-week-card" key={week.weekNumber}>
          <div className="commercial-planning-week-card-header">
            <div>
              <h3>Semana {week.weekNumber}</h3>
              <p>
                {formatDate(week.startDate)} até {formatDate(week.endDate)}
              </p>
            </div>
            <div className="commercial-planning-week-totals">
              <span>{week.experimentsCreated} experimentos</span>
              <strong>{formatExecutedCurrency(week.totalCost)}</strong>
              <small>{formatExecutedCurrency(week.totalRevenue)} receita</small>
            </div>
          </div>

          <div className="commercial-planning-week-table-wrap">
            <table className="table table-sm align-middle mb-0 commercial-planning-week-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Nome</th>
                  <th>Tipo</th>
                  <th>Status</th>
                  <th>Criado</th>
                  <th>Custo total</th>
                  <th>Vídeo</th>
                  <th>Receita</th>
                  <th>Resultado</th>
                </tr>
              </thead>
              <tbody>
                {week.experiments.length > 0 ? (
                  week.experiments.map((experiment) => (
                    <tr key={experiment.id}>
                      <td>{experiment.id}</td>
                      <td>{experiment.name}</td>
                      <td>{experiment.productType ?? "Não definido"}</td>
                      <td>{experiment.status ?? "Não definido"}</td>
                      <td>{formatDate(experiment.createdAt)}</td>
                      <td>{formatExecutedCurrency(experiment.totalCost)}</td>
                      <td>{formatExecutedCurrency(experiment.videoCost)}</td>
                      <td>{formatExecutedCurrency(experiment.revenue)}</td>
                      <td>{experiment.result ?? "Sem resultado"}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={9} className="text-muted">
                      Nenhum experimento criado nesta semana.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </article>
      ))}
    </section>
  );
}

export default function CommercialPlanningPage() {
  const plansQuery = useCommercialPlans();
  const plans = asArray(plansQuery.data);
  const currentMonthPlan = useMemo<CommercialPlan>(
    () => plans[0] ?? fallbackMonthPlan(),
    [plans],
  );
  const planWeeksQuery = useCommercialPlanWeeks(
    currentMonthPlan.id > 0 ? currentMonthPlan.id : null,
  );
  const weeks = asArray(planWeeksQuery.data);
  const costProgress = progressPercentage(
    currentMonthPlan.maxBudget,
    currentMonthPlan.actualTotalCost,
  );
  const revenueProgress = progressPercentage(
    currentMonthPlan.targetRevenue,
    currentMonthPlan.actualRevenue,
  );
  const operationalRevenueProgress = progressPercentage(
    currentMonthPlan.operationalRevenueTarget,
    currentMonthPlan.actualRevenue,
  );
  const createdExperimentsProgress = progressPercentage(
    currentMonthPlan.experimentsToCreate,
    currentMonthPlan.actualExperimentsCreated,
  );
  const publishedExperimentsProgress = progressPercentage(
    currentMonthPlan.experimentsToPublish,
    currentMonthPlan.actualExperimentsPublished,
  );

  return (
    <div className="commercial-planning-page d-flex flex-column gap-4">
      <header className="d-flex flex-column flex-xl-row justify-content-between gap-3">
        <div>
          <PageTitle>Planejamento</PageTitle>
        </div>
      </header>

      {plansQuery.isError ? (
        <div className="alert alert-danger mb-0" role="alert">
          Não foi possível carregar os planos comerciais.
        </div>
      ) : null}

      <section className="commercial-planning-month-plan">
        <div className="d-flex flex-column gap-4">
          <div className="commercial-planning-month-heading">
            <div>
              <span
                className={`badge ${planStatusClass(currentMonthPlan.status)} mb-3`}
              >
                {planStatusLabel(currentMonthPlan.status)}
              </span>
              <p className="commercial-planning-month-eyebrow mb-1">
                Plano do mês corrente
              </p>
              <h2 className="commercial-planning-month-title mb-2">
                Julho 2026
              </h2>
            </div>
          </div>

          <div className="commercial-planning-month-metrics">
            <MonthlyMetricCard
              label="Custo total"
              target={formatCurrency(currentMonthPlan.maxBudget)}
              actual={formatExecutedCurrency(currentMonthPlan.actualTotalCost)}
              percentage={costProgress}
              tone="warning"
            />
            <MonthlyMetricCard
              label="Receita mínima"
              target={formatCurrency(currentMonthPlan.targetRevenue)}
              actual={formatExecutedCurrency(currentMonthPlan.actualRevenue)}
              percentage={revenueProgress}
              tone="success"
            />
            <MonthlyMetricCard
              label="Receita operacional"
              target={formatCurrency(currentMonthPlan.operationalRevenueTarget)}
              actual={formatExecutedCurrency(currentMonthPlan.actualRevenue)}
              percentage={operationalRevenueProgress}
              tone="info"
            />
            <MonthlyMetricCard
              label="Experimentos criados"
              target={formatNumber(currentMonthPlan.experimentsToCreate)}
              actual={formatExecutedNumber(
                currentMonthPlan.actualExperimentsCreated,
              )}
              percentage={createdExperimentsProgress}
            />
            <MonthlyMetricCard
              label="Experimentos publicados"
              target={formatNumber(currentMonthPlan.experimentsToPublish)}
              actual={formatExecutedNumber(
                currentMonthPlan.actualExperimentsPublished,
              )}
              percentage={publishedExperimentsProgress}
            />
          </div>
        </div>
      </section>

      {planWeeksQuery.isError ? (
        <div className="alert alert-danger mb-0" role="alert">
          Não foi possível carregar os experimentos por semana.
        </div>
      ) : null}

      {weeks.length > 0 ? <WeeklyExperimentTable weeks={weeks} /> : null}
    </div>
  );
}
