import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useAgents } from "../../api/agent/useAgents";
import { Agent } from "../../api/agent/types";
import PageTitle from "../../components/PageTitle";
import {
  CommercialPlan,
  CommercialPlanAgentActivity,
  CommercialPlanFunnelStage,
  CommercialPlanWeek,
  CommercialPlanWeekObjective,
  CommercialPlanStatus,
  SaveCommercialPlanPayload,
  useCommercialPlanWeeks,
  useCommercialPlanAgentActivity,
  useCommercialPlanVersions,
  useCommercialPlans,
  useCreateCommercialPlan,
  useUpdateCommercialPlan,
  useUpdateCommercialPlanWeekObjectives,
  useUpdateCommercialPlanWeekCommitmentStatus,
  useRevenueProjections,
  useRequestRevenueProjection,
  useCommercialAssumptionDefinitions,
  useRequestCommercialAssumptions,
} from "../../api/planning/useCommercialPlans";
import "./CommercialPlanningPage.css";
import GrowthOperatorPanel from "./GrowthOperatorPanel";
import ExperimentStrategistPanel from "./ExperimentStrategistPanel";
import FinancialAgentPanel from "./FinancialAgentPanel";

const CURRENT_OPERATIONAL_MONTH = new Date().toISOString().slice(0, 7);
const LEGACY_PLAN_REFERENCE_MONTH = "2026-07";

const emptyCommercialPlan: SaveCommercialPlanPayload = {
  name: "",
  status: "DRAFT",
  commercialObjective: "",
  targetAudience: "",
  mainPain: "",
  mainOffer: "",
  mainChannel: "",
  mainMetric: "",
  successCriteria: "",
  stopCriteria: "",
  deadline: "",
  maxBudget: null,
  targetRevenue: null,
  operationalRevenueTarget: null,
  offerPriceBrl: null,
  variableCostPerSaleBrl: null,
  expectedMonthlyTraffic: null,
  expectedConversionRatePercent: null,
  expectedCacBrl: null,
  expectedRefundRatePercent: null,
  fixedOperationalCostBrl: null,
  experimentsToCreate: 1,
  experimentsToPublish: 0,
  productsToValidate: 1,
  productTypesToExplore: 1,
  approachesToTest: 2,
  customerConversationsTarget: 5,
  nextAction: "",
  currentBlocker: "",
  rootCause: "",
};

const augustRevenuePlan: SaveCommercialPlanPayload = {
  name: "Planejamento Agosto 2026 - Receita Agenda Cheia",
  status: "IN_PROGRESS",
  experimentId: 81,
  commercialObjective:
    "Gerar receita ainda em agosto de 2026 com o Agenda Cheia Nail Design, priorizando compra aprovada e preservando caixa com decisões rápidas por etapa do funil.",
  targetAudience:
    "Nail designers e manicures que publicam fotos de unhas, mas recebem poucas conversas e pedidos de horário pelo WhatsApp.",
  mainPain:
    "A profissional produz um trabalho visualmente bonito, mas suas publicações não conduzem interessadas a perguntar sobre horários e serviços.",
  mainOffer:
    "Agenda Cheia Nail Design por R$ 67: kit personalizado com 10 posts, 10 stories, 10 legendas, 5 mensagens de WhatsApp e calendário de 7 dias.",
  mainLeadMagnet:
    "Amostra demonstrativa com quatro peças do kit na página de vendas, sem cadastro antes da compra.",
  mainChannel:
    "Meta Ads para página de venda direta com checkout Mercado Pago e recuperação por WhatsApp.",
  mainMetric:
    "Compra aprovada; checkout iniciado e clique no checkout como sinais intermediários.",
  successCriteria:
    "Obter ao menos 1 compra aprovada nos primeiros 7 dias e buscar 5 vendas no mês, com entrega do kit concluída e receita atribuída ao experimento.",
  stopCriteria:
    "Revisar o criativo se não houver clique qualificado em 3 dias; revisar página e prova visual se houver visita sem checkout; revisar confiança, preço ou checkout se houver início sem compra; pausar antes de ultrapassar R$ 175 sem venda.",
  deadline: "2026-08-31",
  maxBudget: 400,
  targetRevenue: 67,
  operationalRevenueTarget: 335,
  offerPriceBrl: 67,
  variableCostPerSaleBrl: null,
  expectedMonthlyTraffic: null,
  expectedConversionRatePercent: null,
  expectedCacBrl: null,
  expectedRefundRatePercent: null,
  fixedOperationalCostBrl: null,
  experimentsToCreate: 1,
  experimentsToPublish: 1,
  productsToValidate: 1,
  productTypesToExplore: 1,
  approachesToTest: 2,
  customerConversationsTarget: 5,
  nextAction:
    "Acompanhar diariamente o experimento 81 a R$ 25 por dia, proteger a primeira venda e corrigir somente o ponto comprovado de abandono.",
  currentBlocker:
    "Ainda não há compra comercial atribuída à campanha ativa; o primeiro objetivo é transformar tráfego em receita real sem abrir novas frentes prematuramente.",
  rootCause:
    "Julho acumulou execução sem um plano mensal orientado a receita. Agosto precisa concentrar oferta, mídia e análise em um único funil comprável e operacionalmente entregue.",
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
  offerPriceBrl: 19.9,
  variableCostPerSaleBrl: null,
  expectedMonthlyTraffic: null,
  expectedConversionRatePercent: null,
  expectedCacBrl: null,
  expectedRefundRatePercent: null,
  fixedOperationalCostBrl: null,
  experimentsToCreate: 2,
  experimentsToPublish: 3,
  productsToValidate: 1,
  productTypesToExplore: 1,
  approachesToTest: 2,
  customerConversationsTarget: 5,
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

function planToPayload(plan: CommercialPlan): SaveCommercialPlanPayload {
  return {
    name: plan.name,
    status: plan.status,
    nicheId: plan.nicheId,
    hypothesisId: plan.hypothesisId,
    experimentId: plan.experimentId,
    commercialObjective: plan.commercialObjective ?? undefined,
    targetAudience: plan.targetAudience ?? undefined,
    mainPain: plan.mainPain ?? undefined,
    mainOffer: plan.mainOffer ?? undefined,
    mainLeadMagnet: plan.mainLeadMagnet ?? undefined,
    mainChannel: plan.mainChannel ?? undefined,
    mainMetric: plan.mainMetric ?? undefined,
    successCriteria: plan.successCriteria ?? undefined,
    stopCriteria: plan.stopCriteria ?? undefined,
    deadline: plan.deadline ?? undefined,
    maxBudget: plan.maxBudget,
    targetRevenue: plan.targetRevenue,
    operationalRevenueTarget: plan.operationalRevenueTarget,
    offerPriceBrl: plan.offerPriceBrl,
    variableCostPerSaleBrl: plan.variableCostPerSaleBrl,
    expectedMonthlyTraffic: plan.expectedMonthlyTraffic,
    expectedConversionRatePercent: plan.expectedConversionRatePercent,
    expectedCacBrl: plan.expectedCacBrl,
    expectedRefundRatePercent: plan.expectedRefundRatePercent,
    fixedOperationalCostBrl: plan.fixedOperationalCostBrl,
    experimentsToCreate: plan.experimentsToCreate,
    experimentsToPublish: plan.experimentsToPublish,
    productsToValidate: plan.productsToValidate,
    productTypesToExplore: plan.productTypesToExplore,
    approachesToTest: plan.approachesToTest,
    customerConversationsTarget: plan.customerConversationsTarget,
    nextAction: plan.nextAction ?? undefined,
    currentBlocker: plan.currentBlocker ?? undefined,
    rootCause: plan.rootCause ?? undefined,
  };
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

function formatFunnelNumber(value?: number | null) {
  return value == null ? "-" : value.toLocaleString("pt-BR");
}

function formatPercentage(value?: number | null) {
  return value == null ? "-" : `${value.toLocaleString("pt-BR")}%`;
}

function formatAverageViewTime(valueMs?: number | null) {
  if (valueMs == null || valueMs <= 0) return "Sem dados";
  const totalSeconds = Math.round(valueMs / 1000);
  if (totalSeconds < 60) return `${totalSeconds}s`;
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return seconds > 0 ? `${minutes}min ${seconds}s` : `${minutes}min`;
}

type CommercialPlanBlocker = {
  key: string;
  title: string;
  cause: string;
  impact: string;
  nextAction: string;
  owner: string;
  source: string;
};

const commercialAssumptionLabels: Array<[keyof CommercialPlan, string]> = [
  ["variableCostPerSaleBrl", "custo variável por venda"],
  ["expectedMonthlyTraffic", "tráfego mensal esperado"],
  ["expectedConversionRatePercent", "conversão esperada"],
  ["expectedCacBrl", "CAC esperado"],
  ["expectedRefundRatePercent", "taxa de reembolso"],
  ["fixedOperationalCostBrl", "custo operacional fixo"],
];

function buildCommercialPlanBlockers(
  plan: CommercialPlan,
  activity?: CommercialPlanAgentActivity,
): CommercialPlanBlocker[] {
  const blockers: CommercialPlanBlocker[] = [];

  if (plan.currentBlocker?.trim()) {
    blockers.push({
      key: "plan",
      title: "Gargalo comercial vigente",
      cause: plan.rootCause?.trim() || "Causa-raiz ainda não registrada.",
      impact: plan.currentBlocker.trim(),
      nextAction:
        plan.nextAction?.trim() ||
        "Definir a próxima ação responsável pelo desbloqueio.",
      owner: "Plano comercial",
      source: "Contexto oficial versionado",
    });
  }

  asArray(plan.milestones)
    .filter((milestone) => milestone.status === "BLOCKED")
    .forEach((milestone) =>
      blockers.push({
        key: `milestone-${milestone.id}`,
        title: milestone.name,
        cause:
          milestone.blocker?.trim() ||
          "Marco marcado como bloqueado sem causa informada.",
        impact:
          "O marco não pode ser concluído enquanto este impedimento estiver ativo.",
        nextAction:
          milestone.recommendedNextAction?.trim() ||
          "Registrar a ação recomendada para liberar o marco.",
        owner: "Marco do plano",
        source: milestone.evidenceSource?.trim() || "Marco persistido",
      }),
    );

  activity?.entries
    .filter(
      (entry) =>
        entry.externalDecisionRequired ||
        Boolean(entry.difficulty?.trim()) ||
        /BLOCKED|FAILED|ERROR/.test(entry.status),
    )
    .forEach((entry, index) =>
      blockers.push({
        key: `activity-${entry.recordType}-${entry.sourceReference ?? index}`,
        title: entry.title,
        cause:
          entry.difficulty?.trim() ||
          entry.externalDecision?.trim() ||
          `Execução no estado ${entry.status}.`,
        impact: entry.externalDecisionRequired
          ? "A execução aguarda uma decisão externa para avançar."
          : "A execução do agente não consegue avançar no estado atual.",
        nextAction:
          entry.externalDecision?.trim() ||
          "Corrigir a dificuldade registrada e retomar a execução.",
        owner: entry.agentNickname,
        source: entry.sourceReference?.trim() || entry.recordType,
      }),
    );

  const missingAssumptions = commercialAssumptionLabels
    .filter(([field]) => plan[field] == null)
    .map(([, label]) => label);
  if (missingAssumptions.length > 0) {
    blockers.push({
      key: "commercial-assumptions",
      title: "Premissas financeiras incompletas",
      cause: `Faltam ${missingAssumptions.join(", ")}.`,
      impact:
        "Plutus não consegue recomendar investimento com margem, CAC e ponto de equilíbrio confiáveis.",
      nextAction: "Executar “Definir premissas ausentes” com Atena + Plutus.",
      owner: "Atena + Plutus",
      source: "Campos estruturados do plano",
    });
  }

  return blockers;
}

function CommercialPlanBlockersPanel({
  plan,
  activity,
}: {
  plan: CommercialPlan;
  activity?: CommercialPlanAgentActivity;
}) {
  const blockers = useMemo(
    () => buildCommercialPlanBlockers(plan, activity),
    [plan, activity],
  );

  return (
    <section
      className="commercial-planning-blockers"
      aria-labelledby="commercial-plan-blockers-title"
    >
      <div className="commercial-planning-blockers-header">
        <div>
          <p className="commercial-planning-month-eyebrow mb-1">
            Decisão operacional
          </p>
          <h2 id="commercial-plan-blockers-title">O que bloqueia este plano</h2>
          <p>
            Impedimentos ativos, causa, impacto e ação necessária para voltar a
            avançar.
          </p>
        </div>
        <span
          className={`badge ${blockers.length > 0 ? "text-bg-danger" : "text-bg-success"}`}
        >
          {blockers.length}{" "}
          {blockers.length === 1 ? "bloqueio ativo" : "bloqueios ativos"}
        </span>
      </div>
      {blockers.length > 0 ? (
        <div className="commercial-planning-blockers-list">
          {blockers.map((blocker) => (
            <article
              key={blocker.key}
              className="commercial-planning-blocker-item"
            >
              <div className="commercial-planning-blocker-title">
                <strong>{blocker.title}</strong>
                <span>{blocker.owner}</span>
              </div>
              <dl>
                <div>
                  <dt>Causa</dt>
                  <dd>{blocker.cause}</dd>
                </div>
                <div>
                  <dt>Impacto</dt>
                  <dd>{blocker.impact}</dd>
                </div>
                <div>
                  <dt>Como desbloquear</dt>
                  <dd>{blocker.nextAction}</dd>
                </div>
                <div>
                  <dt>Evidência</dt>
                  <dd>{blocker.source}</dd>
                </div>
              </dl>
            </article>
          ))}
        </div>
      ) : (
        <div className="alert alert-success mb-0" role="status">
          Nenhum bloqueio ativo foi registrado para este plano.
        </div>
      )}
    </section>
  );
}

function formatDate(value?: string | null) {
  if (!value) return "Não definido";
  return new Date(value).toLocaleDateString("pt-BR", { timeZone: "UTC" });
}

function resolvePlanReferenceMonth(plan: CommercialPlan) {
  return plan.deadline?.slice(0, 7) ?? LEGACY_PLAN_REFERENCE_MONTH;
}

function addMonths(referenceMonth: string, months: number) {
  const [year, month] = referenceMonth.split("-").map(Number);
  const date = new Date(Date.UTC(year, month - 1 + months, 1));
  return `${date.getUTCFullYear()}-${String(date.getUTCMonth() + 1).padStart(
    2,
    "0",
  )}`;
}

function monthLabel(referenceMonth: string) {
  const [year, month] = referenceMonth.split("-").map(Number);
  const date = new Date(Date.UTC(year, month - 1, 1));
  const label = date
    .toLocaleDateString("pt-BR", {
      month: "long",
      year: "numeric",
      timeZone: "UTC",
    })
    .replace(" de ", " ");
  return label.charAt(0).toUpperCase() + label.slice(1);
}

function progressPercentage(target?: number | null, actual?: number | null) {
  if (!target || target <= 0) return 0;
  return Math.min(100, Math.round(((actual ?? 0) / target) * 100));
}

function sumExperimentValue(
  weeks: CommercialPlanWeek[],
  field: "totalCost" | "campaignCost" | "aiCost" | "videoCost",
) {
  return weeks.reduce(
    (total, week) =>
      total +
      asArray(week.experiments).reduce(
        (weekTotal, experiment) => weekTotal + (experiment[field] ?? 0),
        0,
      ),
    0,
  );
}

function aggregateFunnelStages(weeks: CommercialPlanWeek[]) {
  const byCode = new Map<string, CommercialPlanFunnelStage>();
  const costByCode = new Map<string, number>();
  weeks.forEach((week) => {
    asArray(week.funnelStages).forEach((stage) => {
      if (stage.costPerConversion != null && stage.actualTotal != null) {
        costByCode.set(
          stage.code,
          (costByCode.get(stage.code) ?? 0) +
            stage.costPerConversion * stage.actualTotal,
        );
      }
      const current = byCode.get(stage.code);
      if (!current) {
        byCode.set(stage.code, { ...stage });
        return;
      }
      byCode.set(stage.code, {
        ...current,
        actualTotal: (current.actualTotal ?? 0) + (stage.actualTotal ?? 0),
        uniqueCount: (current.uniqueCount ?? 0) + (stage.uniqueCount ?? 0),
        applicable: current.applicable === true || stage.applicable === true,
        lastEventAt:
          (stage.lastEventAt ?? "") > (current.lastEventAt ?? "")
            ? stage.lastEventAt
            : current.lastEventAt,
      });
    });
  });
  let previousActual: number | null = null;
  return Array.from(byCode.values()).map((stage) => {
    const actual = stage.actualTotal ?? null;
    const totalCost = costByCode.get(stage.code);
    const conversion =
      stage.applicable === true &&
      actual != null &&
      previousActual &&
      previousActual > 0
        ? Number(((actual / previousActual) * 100).toFixed(2))
        : null;
    if (stage.applicable === true) {
      previousActual = actual;
    }
    return {
      ...stage,
      conversionFromPreviousStep: conversion,
      costPerConversion:
        totalCost != null && actual && actual > 0
          ? Number((totalCost / actual).toFixed(2))
          : stage.costPerConversion,
    };
  });
}

function findMainFunnelBottleneck(stages: CommercialPlanFunnelStage[]) {
  return stages
    .filter(
      (stage) =>
        stage.applicable === true &&
        stage.conversionFromPreviousStep != null &&
        stage.actualTotal != null,
    )
    .sort(
      (first, second) =>
        (first.conversionFromPreviousStep ?? 101) -
        (second.conversionFromPreviousStep ?? 101),
    )[0];
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
    offerPriceBrl: julyPlanningForm.offerPriceBrl,
    variableCostPerSaleBrl: julyPlanningForm.variableCostPerSaleBrl,
    expectedMonthlyTraffic: julyPlanningForm.expectedMonthlyTraffic,
    expectedConversionRatePercent:
      julyPlanningForm.expectedConversionRatePercent,
    expectedCacBrl: julyPlanningForm.expectedCacBrl,
    expectedRefundRatePercent: julyPlanningForm.expectedRefundRatePercent,
    fixedOperationalCostBrl: julyPlanningForm.fixedOperationalCostBrl,
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

type BudgetDirection = {
  code: string;
  label: string;
  amount: number;
  percentage: number;
  recommendation: string;
};

function buildBudgetDirections(
  plan: CommercialPlan,
  weeks: CommercialPlanWeek[],
): BudgetDirection[] {
  const experimentTotal = sumExperimentValue(weeks, "totalCost");
  const campaignCost = sumExperimentValue(weeks, "campaignCost");
  const aiCost = sumExperimentValue(weeks, "aiCost");
  const videoCost = sumExperimentValue(weeks, "videoCost");
  const unknownCost = Math.max(
    0,
    experimentTotal - campaignCost - aiCost - videoCost,
  );
  const totalBudget = plan.actualTotalCost ?? experimentTotal;
  const referenceTotal = totalBudget > 0 ? totalBudget : experimentTotal;

  return [
    {
      code: "campaign",
      label: "Mídia paga",
      amount: campaignCost,
      recommendation:
        "Direcionar aumento só para anúncios com clique qualificado e avanço no funil.",
    },
    {
      code: "ai",
      label: "Produção com IA",
      amount: aiCost,
      recommendation:
        "Usar para criar variações de oferta, copy e criativos ligados ao gargalo principal.",
    },
    {
      code: "video",
      label: "Vídeos e criativos",
      amount: videoCost,
      recommendation:
        "Priorizar assets aprovados que possam virar prova visual ou criativo Meta.",
    },
    {
      code: "other",
      label: "Sem classificação",
      amount: unknownCost,
      recommendation:
        "Classificar a origem do custo para decidir se a verba deve escalar, corrigir ou pausar.",
    },
  ]
    .filter((item) => item.amount > 0)
    .map((item) => ({
      ...item,
      percentage:
        referenceTotal > 0
          ? Math.round((item.amount / referenceTotal) * 100)
          : 0,
    }));
}

function BudgetDirectionPanel({
  plan,
  weeks,
}: {
  plan: CommercialPlan;
  weeks: CommercialPlanWeek[];
}) {
  const directions = useMemo(
    () => buildBudgetDirections(plan, weeks),
    [plan, weeks],
  );

  if (directions.length === 0) {
    return null;
  }

  return (
    <section className="commercial-planning-budget-direction">
      <div className="commercial-planning-budget-direction-header">
        <div>
          <p className="commercial-planning-month-eyebrow mb-1">
            Direcionamento de verbas
          </p>
          <h3>Detalhe abaixo do custo mensal</h3>
        </div>
        <span>Total do mês mantido no card acima</span>
      </div>
      <div className="commercial-planning-budget-direction-grid">
        {directions.map((direction) => (
          <article
            className="commercial-planning-budget-direction-item"
            key={direction.code}
          >
            <div>
              <span>{direction.label}</span>
              <strong>{formatExecutedCurrency(direction.amount)}</strong>
            </div>
            <b>{direction.percentage}% do custo</b>
            <p>{direction.recommendation}</p>
          </article>
        ))}
      </div>
    </section>
  );
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
  percentage?: number;
  tone?: "primary" | "success" | "warning" | "info";
}) {
  return (
    <div className="commercial-planning-month-metric">
      <div className="d-flex justify-content-between align-items-start gap-2">
        <span className="commercial-planning-month-metric-label">{label}</span>
        <span className={`badge text-bg-${tone}`}>{percentage ?? 0}%</span>
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
        <span style={{ width: `${percentage ?? 0}%` }} />
      </div>
    </div>
  );
}

function WeeklyObjectiveEditor({
  planId,
  week,
  agents,
  planVersionNumber,
}: {
  planId: number;
  week: CommercialPlanWeek;
  agents: Agent[];
  planVersionNumber: number;
}) {
  const updateObjectives = useUpdateCommercialPlanWeekObjectives(planId);
  const updateCommitmentStatus =
    useUpdateCommercialPlanWeekCommitmentStatus(planId);
  const [objectives, setObjectives] = useState<CommercialPlanWeekObjective[]>(
    () => normalizeObjectives(week.objectives),
  );
  const [isAddingObjective, setIsAddingObjective] = useState(false);
  const [newObjectiveText, setNewObjectiveText] = useState("");
  const [newExpectedResult, setNewExpectedResult] = useState("");
  const [newAgentKey, setNewAgentKey] = useState("");
  const [newPlannedCost, setNewPlannedCost] = useState("");
  const [newPlannedRevenue, setNewPlannedRevenue] = useState("");
  const objectivesEditable = week.objectivesEditable === true;
  const objectiveWeekNumber = week.weekNumber + 1;
  const bottleneck = findMainFunnelBottleneck(asArray(week.funnelStages));

  useEffect(() => {
    setObjectives(normalizeObjectives(week.objectives));
    setIsAddingObjective(false);
    setNewObjectiveText("");
    setNewExpectedResult("");
    setNewAgentKey("");
    setNewPlannedCost("");
    setNewPlannedRevenue("");
  }, [week.objectives]);

  function saveNewObjective() {
    const objectiveText = newObjectiveText.trim();
    if (!objectiveText) return;
    const assignedAgent = agents.find(
      (agent) => agent.agentKey === newAgentKey,
    );
    const nextObjectives = [
      ...objectives,
      {
        id: null,
        sequenceOrder: objectives.length + 1,
        objectiveText,
        score: null,
        planVersionNumber,
        assignedAgentKey: assignedAgent?.agentKey ?? null,
        assignedAgentNickname: assignedAgent?.nickname ?? null,
        expectedResult: newExpectedResult.trim() || null,
        executionStatus: "PLANNED" as const,
        plannedCost: newPlannedCost === "" ? null : Number(newPlannedCost),
        plannedRevenue:
          newPlannedRevenue === "" ? null : Number(newPlannedRevenue),
      },
    ];
    updateObjectives.mutate({
      weekNumber: week.weekNumber,
      objectives: nextObjectives.map((objective, index) => ({
        ...objective,
        sequenceOrder: index + 1,
        planVersionNumber: objective.planVersionNumber ?? planVersionNumber,
        executionStatus: objective.executionStatus ?? "PLANNED",
      })),
    });
  }

  return (
    <div className="commercial-planning-week-objectives">
      <div className="commercial-planning-week-objectives-header">
        <div>
          <h4>Objetivos para a próxima semana</h4>
          <span>
            Execução do Plano Comercial v{planVersionNumber} · Gargalo:{" "}
            {bottleneck?.name ?? "sem conversão suficiente"}
          </span>
        </div>
        {objectivesEditable ? (
          <button
            className="btn btn-sm btn-outline-primary"
            type="button"
            onClick={() => setIsAddingObjective((current) => !current)}
          >
            Inserir novo
          </button>
        ) : null}
      </div>

      <div className="commercial-planning-week-objective-list">
        {objectives.map((objective, index) => (
          <div
            className="commercial-planning-week-objective"
            key={`${objective.id ?? "new"}-${index}`}
          >
            <span className="commercial-planning-week-objective-bullet">
              {index + 1}
            </span>
            <div className="commercial-planning-week-objective-content">
              <p className="commercial-planning-week-objective-text mb-0">
                {objective.objectiveText || "Objetivo sem descrição."}
              </p>
              <div className="d-flex flex-wrap gap-2 align-items-center">
                <span className="badge text-bg-primary">
                  {objective.assignedAgentNickname ??
                    "Responsável não definido"}
                </span>
                {objective.id ? (
                  <select
                    aria-label={`Status do compromisso ${index + 1}`}
                    className="form-select form-select-sm w-auto"
                    value={objective.executionStatus ?? "PLANNED"}
                    disabled={updateCommitmentStatus.isPending}
                    onChange={(event) =>
                      updateCommitmentStatus.mutate({
                        commitmentId: objective.id as number,
                        status: event.target.value as NonNullable<
                          CommercialPlanWeekObjective["executionStatus"]
                        >,
                        score: objective.score,
                      })
                    }
                  >
                    <option value="PLANNED">Planejado</option>
                    <option value="IN_PROGRESS">Em andamento</option>
                    <option value="BLOCKED">Bloqueado</option>
                    <option value="COMPLETED">Concluído</option>
                    <option value="CANCELLED">Cancelado</option>
                  </select>
                ) : (
                  <span className="badge text-bg-light border">Planejado</span>
                )}
                {objective.planVersionNumber ? (
                  <span className="badge text-bg-light border">
                    Plano v{objective.planVersionNumber}
                  </span>
                ) : null}
              </div>
              {objective.expectedResult ? (
                <small>Resultado esperado: {objective.expectedResult}</small>
              ) : null}
              <small>
                Custo planejado: {formatExecutedCurrency(objective.plannedCost)}
                {" · "}Receita planejada:{" "}
                {formatExecutedCurrency(objective.plannedRevenue)}
                {objective.dueDate
                  ? ` · Prazo: ${formatDate(objective.dueDate)}`
                  : ""}
              </small>
              {objective.score != null ? (
                <span className="badge text-bg-light border align-self-start">
                  Nota {objective.score}
                </span>
              ) : null}
            </div>
          </div>
        ))}
      </div>

      {isAddingObjective && objectivesEditable ? (
        <div className="commercial-planning-week-objective-form">
          <textarea
            aria-label={`Novo objetivo para a semana ${objectiveWeekNumber}`}
            className="form-control form-control-sm"
            rows={2}
            value={newObjectiveText}
            onChange={(event) => setNewObjectiveText(event.target.value)}
            placeholder="Ação semanal ligada ao gargalo e à venda"
          />
          <textarea
            aria-label="Resultado comercial esperado"
            className="form-control form-control-sm"
            rows={2}
            value={newExpectedResult}
            onChange={(event) => setNewExpectedResult(event.target.value)}
            placeholder="Resultado mensurável esperado ao final da semana"
          />
          <select
            aria-label="Agente responsável"
            className="form-select form-select-sm"
            value={newAgentKey}
            onChange={(event) => setNewAgentKey(event.target.value)}
          >
            <option value="">Responsável não definido</option>
            {agents.map((agent) => (
              <option key={agent.id} value={agent.agentKey ?? ""}>
                {agent.nickname} — {agent.name}
              </option>
            ))}
          </select>
          <div className="row g-2">
            <div className="col-md-6">
              <input
                aria-label="Custo planejado da semana"
                className="form-control form-control-sm"
                type="number"
                min="0"
                step="0.01"
                value={newPlannedCost}
                onChange={(event) => setNewPlannedCost(event.target.value)}
                placeholder="Custo planejado (R$)"
              />
            </div>
            <div className="col-md-6">
              <input
                aria-label="Receita planejada da semana"
                className="form-control form-control-sm"
                type="number"
                min="0"
                step="0.01"
                value={newPlannedRevenue}
                onChange={(event) => setNewPlannedRevenue(event.target.value)}
                placeholder="Receita planejada (R$)"
              />
            </div>
          </div>
          <button
            className="btn btn-sm btn-primary"
            type="button"
            disabled={updateObjectives.isPending || !newObjectiveText.trim()}
            onClick={saveNewObjective}
          >
            {updateObjectives.isPending ? "Salvando..." : "Salvar novo"}
          </button>
        </div>
      ) : null}

      <div className="commercial-planning-week-objectives-actions">
        {updateObjectives.isError ? (
          <span className="text-danger">Não foi possível salvar.</span>
        ) : null}
        {updateObjectives.isSuccess ? (
          <span className="text-success">Objetivo inserido.</span>
        ) : null}
      </div>
    </div>
  );
}

function normalizeObjectives(
  objectives?: CommercialPlanWeekObjective[] | null,
): CommercialPlanWeekObjective[] {
  if (Array.isArray(objectives) && objectives.length > 0) {
    return objectives.map((objective, index) => ({
      ...objective,
      sequenceOrder: objective.sequenceOrder ?? index + 1,
      score: objective.score ?? null,
    }));
  }
  return [];
}

function CollapseIndicator() {
  return (
    <span className="commercial-planning-collapse-indicator" aria-hidden="true">
      ▾
    </span>
  );
}

function formatBoolean(value?: boolean | null) {
  return value ? "Sim" : "Não";
}

function experimentProfit(
  experiment: CommercialPlanWeek["experiments"][number],
) {
  return (experiment.revenue ?? 0) - (experiment.totalCost ?? 0);
}

function sortedByAverageTime(experiments: CommercialPlanWeek["experiments"]) {
  return [...experiments].sort((first, second) => {
    const firstTime = first.averageProductViewTimeMs ?? -1;
    const secondTime = second.averageProductViewTimeMs ?? -1;
    if (secondTime !== firstTime) return secondTime - firstTime;
    return first.id - second.id;
  });
}

function ExperimentsTable({
  experiments,
}: {
  experiments: CommercialPlanWeek["experiments"];
}) {
  const orderedExperiments = useMemo(
    () => sortedByAverageTime(experiments),
    [experiments],
  );

  return (
    <div className="commercial-planning-experiment-table-wrap">
      <table className="commercial-planning-experiment-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Nome do experimento</th>
            <th>Nicho</th>
            <th>Hipótese</th>
            <th>Custo</th>
            <th>Receita</th>
            <th>Lucro</th>
            <th>Média de tempo</th>
            <th>Tipo de produto</th>
            <th>Manual</th>
            <th>Teste A/B</th>
          </tr>
        </thead>
        <tbody>
          {orderedExperiments.map((experiment) => (
            <tr key={experiment.id}>
              <td>#{experiment.id}</td>
              <td>
                <Link to={`/experiments/${experiment.id}`}>
                  {experiment.name}
                </Link>
              </td>
              <td>{experiment.nicheName ?? "Nicho não informado"}</td>
              <td>{experiment.hypothesisTitle ?? "Hipótese não informada"}</td>
              <td>{formatExecutedCurrency(experiment.totalCost)}</td>
              <td>{formatExecutedCurrency(experiment.revenue)}</td>
              <td>{formatExecutedCurrency(experimentProfit(experiment))}</td>
              <td>
                {formatAverageViewTime(experiment.averageProductViewTimeMs)}
              </td>
              <td>{experiment.productType ?? "Não definido"}</td>
              <td>{formatBoolean(experiment.manual)}</td>
              <td>{formatBoolean(experiment.abTest)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function TopExperimentsRanking({ weeks }: { weeks: CommercialPlanWeek[] }) {
  const topExperiments = useMemo(
    () =>
      sortedByAverageTime(
        weeks.flatMap((week) => week.experiments ?? []),
      ).slice(0, 5),
    [weeks],
  );

  return (
    <section className="commercial-planning-top-ranking">
      <div className="commercial-planning-top-ranking-header">
        <div>
          <p className="commercial-planning-month-eyebrow mb-1">
            Ranking de experimentos
          </p>
          <h3>Top 5 por tempo médio</h3>
        </div>
        <span>Ordenado pelo maior tempo médio de tela</span>
      </div>

      {topExperiments.length > 0 ? (
        <div className="commercial-planning-ranking-list">
          {topExperiments.map((experiment, index) => (
            <article
              className="commercial-planning-ranking-item"
              key={experiment.id}
            >
              <span className="commercial-planning-ranking-position">
                {index + 1}
              </span>
              <div className="commercial-planning-ranking-main">
                <Link to={`/experiments/${experiment.id}`}>
                  {experiment.name}
                </Link>
                <span>{experiment.nicheName ?? "Nicho não informado"}</span>
              </div>
              <div className="commercial-planning-ranking-metrics">
                <div>
                  <span>Lucro</span>
                  <strong>
                    {formatExecutedCurrency(experimentProfit(experiment))}
                  </strong>
                </div>
                <div>
                  <span>Receita</span>
                  <strong>{formatExecutedCurrency(experiment.revenue)}</strong>
                </div>
                <div>
                  <span>Tempo médio</span>
                  <strong>
                    {formatAverageViewTime(experiment.averageProductViewTimeMs)}
                  </strong>
                </div>
              </div>
            </article>
          ))}
        </div>
      ) : (
        <div className="commercial-planning-empty-week">
          Nenhum experimento disponível para ranking.
        </div>
      )}
    </section>
  );
}

function FunnelStagesPanel({
  stages,
  title,
  compact = false,
}: {
  stages: CommercialPlanFunnelStage[];
  title: string;
  compact?: boolean;
}) {
  const normalizedStages = asArray(stages);
  const bottleneck = findMainFunnelBottleneck(normalizedStages);

  return (
    <section
      className={`commercial-planning-funnel-panel${compact ? " compact" : ""}`}
    >
      <div className="commercial-planning-funnel-header">
        <div>
          <p className="commercial-planning-month-eyebrow mb-1">
            Métricas de funil
          </p>
          <h3>{title}</h3>
        </div>
        <span>
          Gargalo principal: {bottleneck?.name ?? "sem conversão suficiente"}
        </span>
      </div>
      <div className="commercial-planning-funnel-grid">
        {normalizedStages.map((stage) => (
          <article
            className={`commercial-planning-funnel-stage${
              stage.applicable === false ? " muted" : ""
            }`}
            key={stage.code}
            title={stage.evidenceSource ?? undefined}
          >
            <strong>{stage.name}</strong>
            <div>
              <span>Planejado</span>
              <b>{formatFunnelNumber(stage.plannedTotal)}</b>
            </div>
            <div>
              <span>Executado</span>
              <b>{formatFunnelNumber(stage.actualTotal)}</b>
            </div>
            <div>
              <span>Conversão</span>
              <b>{formatPercentage(stage.conversionFromPreviousStep)}</b>
            </div>
            <div>
              <span>Custo por conversão</span>
              <b>
                {stage.costPerConversion == null
                  ? "-"
                  : formatExecutedCurrency(stage.costPerConversion)}
              </b>
            </div>
            {stage.applicable === false ? (
              <small>Etapa sem fonte canônica nesta versão.</small>
            ) : (
              <small>
                Último evento:{" "}
                {stage.lastEventAt ? formatDate(stage.lastEventAt) : "-"}
              </small>
            )}
          </article>
        ))}
      </div>
    </section>
  );
}

function WeeklyExperimentList({
  planId,
  weeks,
  agents,
  planVersionNumber,
}: {
  planId: number;
  weeks: CommercialPlanWeek[];
  agents: Agent[];
  planVersionNumber: number;
}) {
  return (
    <section className="commercial-planning-week-list">
      {weeks.map((week) => (
        <details
          className="commercial-planning-week-card"
          key={week.weekNumber}
        >
          <summary className="commercial-planning-week-card-header">
            <div>
              <div className="commercial-planning-summary-title">
                <CollapseIndicator />
                <h3>Semana {week.weekNumber}</h3>
              </div>
              <p>
                {formatDate(week.startDate)} até {formatDate(week.endDate)}
              </p>
            </div>
            <div className="commercial-planning-week-totals">
              <span>{week.experimentsCreated} experimentos</span>
              <strong>{formatExecutedCurrency(week.totalCost)}</strong>
              <small>{formatExecutedCurrency(week.totalRevenue)} receita</small>
            </div>
          </summary>

          {week.experiments.length > 0 ? (
            <ExperimentsTable experiments={week.experiments} />
          ) : (
            <div className="commercial-planning-empty-week">
              Nenhum experimento criado nesta semana.
            </div>
          )}

          <FunnelStagesPanel
            compact
            stages={asArray(week.funnelStages)}
            title={`Funil da semana ${week.weekNumber}`}
          />

          <WeeklyObjectiveEditor
            planId={planId}
            week={week}
            agents={agents}
            planVersionNumber={planVersionNumber}
          />
        </details>
      ))}
    </section>
  );
}

export default function CommercialPlanningPage() {
  const agentsQuery = useAgents();
  const plansQuery = useCommercialPlans();
  const createPlan = useCreateCommercialPlan();
  const updatePlan = useUpdateCommercialPlan();
  const plans = asArray(plansQuery.data);
  const hasAugustPlan = plans.some(
    (plan) => resolvePlanReferenceMonth(plan) === "2026-08",
  );
  const [selectedPlanId, setSelectedPlanId] = useState<number | null>(null);
  const [isCreatingPlan, setIsCreatingPlan] = useState(false);
  const [newPlanDraft, setNewPlanDraft] =
    useState<SaveCommercialPlanPayload>(emptyCommercialPlan);
  const currentMonthPlan = useMemo<CommercialPlan>(
    () =>
      plans.find((plan) => plan.id === selectedPlanId) ??
      plans.find(
        (plan) => resolvePlanReferenceMonth(plan) === CURRENT_OPERATIONAL_MONTH,
      ) ??
      plans[0] ??
      fallbackMonthPlan(),
    [plans, selectedPlanId],
  );
  const planReferenceMonth = resolvePlanReferenceMonth(currentMonthPlan);
  const planVersionsQuery = useCommercialPlanVersions(
    currentMonthPlan.id > 0 ? currentMonthPlan.id : null,
  );
  const planVersions = asArray(planVersionsQuery.data);
  const agentActivityQuery = useCommercialPlanAgentActivity(
    currentMonthPlan.id > 0 ? currentMonthPlan.id : null,
  );
  const agentActivity = agentActivityQuery.data;
  const chronologicalAgentActivity = useMemo(
    () =>
      [...(agentActivity?.entries ?? [])].sort((left, right) => {
        if (!left.occurredAt) return right.occurredAt ? 1 : 0;
        if (!right.occurredAt) return -1;
        return (
          new Date(right.occurredAt).getTime() -
          new Date(left.occurredAt).getTime()
        );
      }),
    [agentActivity?.entries],
  );
  const revenueProjectionsQuery = useRevenueProjections(
    currentMonthPlan.id > 0 ? currentMonthPlan.id : null,
  );
  const requestRevenueProjection = useRequestRevenueProjection(
    currentMonthPlan.id > 0 ? currentMonthPlan.id : null,
  );
  const revenueProjections = asArray(revenueProjectionsQuery.data);
  const assumptionDefinitionsQuery = useCommercialAssumptionDefinitions(
    currentMonthPlan.id > 0 ? currentMonthPlan.id : null,
  );
  const requestCommercialAssumptions = useRequestCommercialAssumptions(
    currentMonthPlan.id > 0 ? currentMonthPlan.id : null,
  );
  const assumptionDefinitions = asArray(assumptionDefinitionsQuery.data);
  const currentPlanVersion = planVersions[0];
  const [selectedReferenceMonth, setSelectedReferenceMonth] =
    useState(planReferenceMonth);
  const [isEditingPlan, setIsEditingPlan] = useState(false);
  const [planDraft, setPlanDraft] = useState<SaveCommercialPlanPayload>(() =>
    planToPayload(currentMonthPlan),
  );

  useEffect(() => {
    setSelectedReferenceMonth(planReferenceMonth);
  }, [planReferenceMonth]);

  useEffect(() => {
    setPlanDraft(planToPayload(currentMonthPlan));
    setIsEditingPlan(false);
  }, [currentMonthPlan]);

  function updatePlanDraft<K extends keyof SaveCommercialPlanPayload>(
    field: K,
    value: SaveCommercialPlanPayload[K],
  ) {
    setPlanDraft((current) => ({ ...current, [field]: value }));
  }

  function updateNewPlanDraft<K extends keyof SaveCommercialPlanPayload>(
    field: K,
    value: SaveCommercialPlanPayload[K],
  ) {
    setNewPlanDraft((current) => ({ ...current, [field]: value }));
  }

  function submitNewPlan() {
    createPlan.mutate(newPlanDraft, {
      onSuccess: (createdPlan) => {
        setSelectedPlanId(createdPlan.id);
        setNewPlanDraft(emptyCommercialPlan);
        setIsCreatingPlan(false);
      },
    });
  }

  function savePlan() {
    if (currentMonthPlan.id <= 0) return;
    updatePlan.mutate(
      { id: currentMonthPlan.id, payload: planDraft },
      { onSuccess: () => setIsEditingPlan(false) },
    );
  }

  function togglePlanEditing() {
    setIsEditingPlan((current) => {
      const opening = !current;
      if (opening) {
        setPlanDraft(planToPayload(currentMonthPlan));
      }
      return opening;
    });
  }

  const planWeeksQuery = useCommercialPlanWeeks(
    currentMonthPlan.id > 0 ? currentMonthPlan.id : null,
    selectedReferenceMonth,
  );
  const weeks = asArray(planWeeksQuery.data);
  const showingPlanReferenceMonth =
    selectedReferenceMonth === planReferenceMonth;
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
  const monthFunnelStages = useMemo(
    () => aggregateFunnelStages(weeks),
    [weeks],
  );

  return (
    <div className="commercial-planning-page d-flex flex-column gap-4">
      <header className="d-flex flex-column flex-xl-row justify-content-between gap-3">
        <div>
          <PageTitle>Planos comerciais</PageTitle>
          <p className="text-body-secondary mb-0">
            Acompanhe objetivos, versões, execução financeira e tudo que os
            agentes fizeram em cada plano.
          </p>
        </div>
        <div className="d-flex flex-wrap gap-2 align-self-start">
          <button
            className="btn btn-outline-primary"
            type="button"
            onClick={() => setIsCreatingPlan((current) => !current)}
          >
            {isCreatingPlan ? "Fechar novo plano" : "Novo plano comercial"}
          </button>
          {!hasAugustPlan ? (
            <button
              className="btn btn-primary"
              type="button"
              disabled={createPlan.isPending}
              onClick={() => createPlan.mutate(augustRevenuePlan)}
            >
              {createPlan.isPending
                ? "Criando plano de agosto..."
                : "Criar plano de agosto"}
            </button>
          ) : null}
        </div>
      </header>

      {plans.length > 0 ? (
        <div className="card">
          <div className="card-body">
            <label className="form-label" htmlFor="commercial-plan-selector">
              Plano comercial em análise
            </label>
            <select
              id="commercial-plan-selector"
              className="form-select"
              value={currentMonthPlan.id}
              onChange={(event) =>
                setSelectedPlanId(Number(event.target.value))
              }
            >
              {plans.map((plan) => (
                <option key={plan.id} value={plan.id}>
                  {plan.name}
                </option>
              ))}
            </select>
            <div className="d-flex flex-wrap align-items-center gap-2 mt-3">
              <span className="badge text-bg-primary">
                {planVersionsQuery.isLoading
                  ? "Consultando versão..."
                  : currentPlanVersion
                    ? `Contexto oficial v${currentPlanVersion.versionNumber}`
                    : "Contexto ainda não versionado"}
              </span>
              {currentPlanVersion ? (
                <small className="text-body-secondary">
                  {currentPlanVersion.changeReason} · referência para agentes:{" "}
                  <code>
                    commercial-plan:{currentMonthPlan.id}@v
                    {currentPlanVersion.versionNumber}
                  </code>
                </small>
              ) : null}
            </div>
            {planVersions.length > 1 ? (
              <details className="mt-3">
                <summary>Histórico de versões ({planVersions.length})</summary>
                <ol className="small mt-2 mb-0">
                  {planVersions.map((version) => (
                    <li key={version.id}>
                      v{version.versionNumber} — {version.changeReason} —{" "}
                      {new Date(version.createdAt).toLocaleString("pt-BR")}
                    </li>
                  ))}
                </ol>
              </details>
            ) : null}
          </div>
        </div>
      ) : null}

      {currentMonthPlan.id > 0 ? (
        <CommercialPlanBlockersPanel
          plan={currentMonthPlan}
          activity={agentActivity}
        />
      ) : null}

      {currentMonthPlan.id > 0 ? (
        <section
          className="card"
          aria-labelledby="commercial-assumptions-title"
        >
          <div className="card-body d-flex flex-column gap-3">
            <div className="d-flex flex-column flex-lg-row justify-content-between gap-3">
              <div>
                <h2 id="commercial-assumptions-title" className="h5 mb-1">
                  Premissas comerciais por Atena + Plutus
                </h2>
                <p className="text-body-secondary mb-0">
                  Atena pesquisa e propõe; Plutus valida preço, margem, tráfego,
                  conversão e CAC. Os valores são hipóteses versionadas e não
                  liberam gasto.
                </p>
              </div>
              <button
                className="btn btn-primary align-self-start"
                type="button"
                disabled={requestCommercialAssumptions.isPending}
                onClick={() => requestCommercialAssumptions.mutate()}
              >
                {requestCommercialAssumptions.isPending
                  ? "Enviando para Atena..."
                  : "Definir premissas ausentes"}
              </button>
            </div>
            {requestCommercialAssumptions.isError ? (
              <div className="alert alert-danger mb-0" role="alert">
                Não foi possível iniciar a definição das premissas.
              </div>
            ) : assumptionDefinitions.length > 0 ? (
              <div className="d-flex flex-column gap-2">
                {assumptionDefinitions.map((definition) => (
                  <article className="border rounded p-3" key={definition.id}>
                    <strong>Validação conjunta #{definition.id}</strong>{" "}
                    <span className="badge text-bg-light border">
                      {definition.status}
                    </span>
                    <p className="mb-0 mt-2">
                      {definition.dailyReport ??
                        definition.errorMessage ??
                        "Atena concluiu a proposta; Plutus está validando os números."}
                    </p>
                  </article>
                ))}
              </div>
            ) : (
              <p className="text-body-secondary mb-0">
                Nenhuma definição conjunta solicitada para este plano.
              </p>
            )}
          </div>
        </section>
      ) : null}

      {currentMonthPlan.id > 0 ? (
        <section className="card" aria-labelledby="revenue-projection-title">
          <div className="card-body d-flex flex-column gap-3">
            <div className="d-flex flex-column flex-lg-row justify-content-between gap-3">
              <div>
                <h2 id="revenue-projection-title" className="h5 mb-1">
                  Projeções de receita por Plutus
                </h2>
                <p className="text-body-secondary mb-0">
                  Cenários para decidir investimento. Projeções não são vendas e
                  não liberam orçamento automaticamente.
                </p>
              </div>
              <button
                className="btn btn-outline-primary align-self-start"
                type="button"
                disabled={requestRevenueProjection.isPending}
                onClick={() => requestRevenueProjection.mutate(undefined)}
              >
                {requestRevenueProjection.isPending
                  ? "Enviando para Plutus..."
                  : "Solicitar projeção"}
              </button>
            </div>
            {requestRevenueProjection.isError ? (
              <div className="alert alert-danger mb-0" role="alert">
                Não foi possível solicitar a projeção financeira.
              </div>
            ) : null}
            {revenueProjectionsQuery.isLoading ? (
              <p className="mb-0">Consultando projeções...</p>
            ) : revenueProjections.length === 0 ? (
              <p className="text-body-secondary mb-0">
                Nenhuma projeção solicitada para este plano.
              </p>
            ) : (
              <div className="d-flex flex-column gap-2">
                {revenueProjections.map((projection) => (
                  <article className="border rounded p-3" key={projection.id}>
                    <div className="d-flex flex-wrap justify-content-between gap-2">
                      <strong>
                        Projeção #{projection.id} · contexto v
                        {projection.commercialPlanVersion}
                      </strong>
                      <span className="badge text-bg-light border">
                        {projection.status}
                      </span>
                    </div>
                    {projection.dailyReport ? (
                      <p className="mt-2 mb-0">{projection.dailyReport}</p>
                    ) : projection.errorMessage ? (
                      <p className="text-danger mt-2 mb-0">
                        {projection.errorMessage}
                      </p>
                    ) : (
                      <p className="text-body-secondary mt-2 mb-0">
                        Plutus ainda está preparando os cenários.
                      </p>
                    )}
                    <small className="text-body-secondary d-block mt-2">
                      Tarefa #{projection.agentTaskId ?? "—"} · solicitada em{" "}
                      {new Date(projection.createdAt).toLocaleString("pt-BR")}
                    </small>
                  </article>
                ))}
              </div>
            )}
          </div>
        </section>
      ) : null}

      {currentMonthPlan.id > 0 ? (
        <section className="card" aria-labelledby="plan-agent-activity-title">
          <div className="card-body d-flex flex-column gap-3">
            <div className="d-flex flex-column flex-lg-row justify-content-between gap-2">
              <div>
                <h2 id="plan-agent-activity-title" className="h5 mb-1">
                  Atuação dos agentes no plano
                </h2>
                <p className="text-body-secondary mb-0">
                  Trabalhos, decisões, dificuldades e prestação de contas no
                  mesmo objetivo comercial.
                </p>
              </div>
              {agentActivity ? (
                <span className="badge text-bg-primary align-self-start">
                  Contexto v{agentActivity.currentVersion}
                </span>
              ) : null}
            </div>

            {agentActivityQuery.isLoading ? (
              <p className="mb-0">Carregando registros dos agentes...</p>
            ) : agentActivityQuery.isError ? (
              <div className="alert alert-danger mb-0" role="alert">
                Não foi possível carregar a atuação dos agentes neste plano.
              </div>
            ) : agentActivity ? (
              <>
                <div
                  className="row g-3"
                  aria-label="Resumo financeiro dos agentes"
                >
                  <div className="col-md-4 col-xl-2">
                    <small className="text-body-secondary">Teto do plano</small>
                    <strong className="d-block">
                      {formatExecutedCurrency(agentActivity.budgetLimitBrl)}
                    </strong>
                  </div>
                  <div className="col-md-4 col-xl-2">
                    <small className="text-body-secondary">Campanhas</small>
                    <strong className="d-block">
                      {formatExecutedCurrency(agentActivity.campaignCostBrl)}
                    </strong>
                  </div>
                  <div className="col-md-4 col-xl-2">
                    <small className="text-body-secondary">IA</small>
                    <strong className="d-block">
                      {formatExecutedCurrency(agentActivity.aiCostBrl)}
                    </strong>
                  </div>
                  <div className="col-md-4 col-xl-2">
                    <small className="text-body-secondary">Custo total</small>
                    <strong className="d-block">
                      {formatExecutedCurrency(agentActivity.totalCostBrl)}
                    </strong>
                  </div>
                  <div className="col-md-4 col-xl-2">
                    <small className="text-body-secondary">Receita</small>
                    <strong className="d-block">
                      {formatExecutedCurrency(agentActivity.revenueBrl)}
                    </strong>
                  </div>
                  <div className="col-md-4 col-xl-2">
                    <small className="text-body-secondary">Vídeo (USD)</small>
                    <strong className="d-block">
                      US$ {agentActivity.videoKnownCostUsd.toFixed(2)} / US${" "}
                      {agentActivity.videoBudgetLimitUsd.toFixed(2)}
                    </strong>
                  </div>
                </div>

                <div className="d-flex flex-wrap gap-2">
                  <span className="badge text-bg-secondary">
                    {agentActivity.openTasks} tarefas abertas
                  </span>
                  <span
                    className={`badge ${
                      agentActivity.pendingDecisions > 0
                        ? "text-bg-warning"
                        : "text-bg-success"
                    }`}
                  >
                    {agentActivity.pendingDecisions} decisões pendentes
                  </span>
                </div>

                {chronologicalAgentActivity.length > 0 ? (
                  <div className="d-flex flex-column gap-2">
                    <div>
                      <h3 className="h6 mb-1">Histórico cronológico</h3>
                      <p className="small text-body-secondary mb-0">
                        Atuações mais recentes primeiro, preservando data,
                        agente, resultado, bloqueio e referência de origem.
                      </p>
                    </div>
                    <div className="table-responsive">
                      <table className="table align-middle mb-0">
                        <thead>
                          <tr>
                            <th scope="col">Ordem</th>
                            <th>Agente</th>
                            <th>Registro</th>
                            <th>Status</th>
                            <th>Dificuldade / decisão</th>
                            <th>Financeiro</th>
                            <th>Atualização</th>
                          </tr>
                        </thead>
                        <tbody>
                          {chronologicalAgentActivity.map((entry, index) => (
                            <tr
                              key={`${entry.recordType}-${entry.sourceReference}-${index}`}
                            >
                              <td>
                                <span className="badge text-bg-light border">
                                  {index + 1}
                                </span>
                              </td>
                              <td>
                                <strong>{entry.agentNickname}</strong>
                                <small className="d-block text-body-secondary">
                                  {entry.agentKey}
                                </small>
                              </td>
                              <td>
                                <strong>{entry.title}</strong>
                                {entry.detail ? (
                                  <small className="d-block text-body-secondary">
                                    {entry.detail}
                                  </small>
                                ) : null}
                              </td>
                              <td>
                                <span className="badge text-bg-light border">
                                  {entry.status}
                                </span>
                              </td>
                              <td>
                                {entry.externalDecisionRequired ? (
                                  <span className="text-warning-emphasis">
                                    {entry.externalDecision}
                                  </span>
                                ) : entry.difficulty ? (
                                  <span className="text-danger">
                                    {entry.difficulty}
                                  </span>
                                ) : (
                                  <span className="text-body-secondary">
                                    Sem bloqueio
                                  </span>
                                )}
                              </td>
                              <td>
                                {entry.budgetLimitUsd != null ? (
                                  <span>
                                    US$ {(entry.knownCostUsd ?? 0).toFixed(2)} /
                                    US$ {entry.budgetLimitUsd.toFixed(2)}
                                  </span>
                                ) : entry.knownCostUsd != null ? (
                                  <span>
                                    US$ {entry.knownCostUsd.toFixed(2)}
                                  </span>
                                ) : (
                                  <span className="text-body-secondary">—</span>
                                )}
                              </td>
                              <td>
                                {entry.occurredAt
                                  ? new Date(entry.occurredAt).toLocaleString(
                                      "pt-BR",
                                    )
                                  : "—"}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                ) : (
                  <p className="text-body-secondary mb-0">
                    Nenhum registro de agente foi vinculado a este plano ainda.
                  </p>
                )}
              </>
            ) : null}
          </div>
        </section>
      ) : null}

      {isCreatingPlan ? (
        <section
          className="card border-primary"
          aria-label="Novo plano comercial"
        >
          <div className="card-body d-flex flex-column gap-3">
            <div>
              <h2 className="h5 mb-1">Novo plano comercial</h2>
              <p className="text-body-secondary mb-0">
                Defina o teto e os gates antes de atribuir gerações do Estúdio.
              </p>
            </div>
            <div className="row g-3">
              <div className="col-lg-8">
                <label className="form-label" htmlFor="new-plan-name">
                  Nome *
                </label>
                <input
                  id="new-plan-name"
                  className="form-control"
                  required
                  value={newPlanDraft.name}
                  onChange={(event) =>
                    updateNewPlanDraft("name", event.target.value)
                  }
                />
              </div>
              <div className="col-lg-4">
                <label className="form-label" htmlFor="new-plan-status">
                  Status
                </label>
                <select
                  id="new-plan-status"
                  className="form-select"
                  value={newPlanDraft.status}
                  onChange={(event) =>
                    updateNewPlanDraft(
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
              <div className="col-md-4">
                <label className="form-label" htmlFor="new-plan-deadline">
                  Prazo *
                </label>
                <input
                  id="new-plan-deadline"
                  className="form-control"
                  type="date"
                  required
                  value={newPlanDraft.deadline}
                  onChange={(event) =>
                    updateNewPlanDraft("deadline", event.target.value)
                  }
                />
              </div>
              <div className="col-md-4">
                <label className="form-label" htmlFor="new-plan-budget">
                  Teto de produção (R$) *
                </label>
                <input
                  id="new-plan-budget"
                  className="form-control"
                  type="number"
                  min="0"
                  step="0.01"
                  required
                  value={newPlanDraft.maxBudget ?? ""}
                  onChange={(event) =>
                    updateNewPlanDraft(
                      "maxBudget",
                      event.target.value === ""
                        ? null
                        : Number(event.target.value),
                    )
                  }
                />
              </div>
              <div className="col-md-4">
                <label className="form-label" htmlFor="new-plan-revenue">
                  Meta de receita (R$)
                </label>
                <input
                  id="new-plan-revenue"
                  className="form-control"
                  type="number"
                  min="0"
                  step="0.01"
                  value={newPlanDraft.targetRevenue ?? ""}
                  onChange={(event) =>
                    updateNewPlanDraft(
                      "targetRevenue",
                      event.target.value === ""
                        ? null
                        : Number(event.target.value),
                    )
                  }
                />
              </div>
              {(
                [
                  ["offerPriceBrl", "Preço da oferta (R$)", "0.01"],
                  [
                    "variableCostPerSaleBrl",
                    "Custo variável por venda (R$)",
                    "0.01",
                  ],
                  ["expectedMonthlyTraffic", "Tráfego mensal esperado", "1"],
                  [
                    "expectedConversionRatePercent",
                    "Conversão esperada (%)",
                    "0.01",
                  ],
                  ["expectedCacBrl", "CAC esperado (R$)", "0.01"],
                  [
                    "expectedRefundRatePercent",
                    "Reembolso esperado (%)",
                    "0.01",
                  ],
                  [
                    "fixedOperationalCostBrl",
                    "Custo operacional fixo (R$)",
                    "0.01",
                  ],
                ] as const
              ).map(([field, label, step]) => (
                <div className="col-md-4" key={field}>
                  <label className="form-label" htmlFor={`new-plan-${field}`}>
                    {label}
                  </label>
                  <input
                    id={`new-plan-${field}`}
                    className="form-control"
                    type="number"
                    min="0"
                    max={field.includes("Percent") ? "100" : undefined}
                    step={step}
                    value={newPlanDraft[field] ?? ""}
                    onChange={(event) =>
                      updateNewPlanDraft(
                        field,
                        event.target.value === ""
                          ? null
                          : Number(event.target.value),
                      )
                    }
                  />
                </div>
              ))}
            </div>
            {(
              [
                ["commercialObjective", "Objetivo comercial"],
                ["targetAudience", "Público-alvo"],
                ["mainPain", "Dor principal"],
                ["mainOffer", "Oferta principal"],
                ["mainChannel", "Canal principal"],
                ["mainMetric", "Métrica principal"],
                ["successCriteria", "Critério de sucesso"],
                ["stopCriteria", "Critério de parada"],
                ["nextAction", "Próxima ação"],
                ["currentBlocker", "Gargalo atual"],
                ["rootCause", "Causa-raiz"],
              ] as const
            ).map(([field, label]) => (
              <div key={field}>
                <label className="form-label" htmlFor={`new-plan-${field}`}>
                  {label}
                </label>
                <textarea
                  id={`new-plan-${field}`}
                  className="form-control"
                  rows={2}
                  value={newPlanDraft[field] ?? ""}
                  onChange={(event) =>
                    updateNewPlanDraft(field, event.target.value)
                  }
                />
              </div>
            ))}
            <div className="d-flex align-items-center gap-3">
              <button
                className="btn btn-primary"
                type="button"
                disabled={
                  createPlan.isPending ||
                  !newPlanDraft.name.trim() ||
                  !newPlanDraft.deadline ||
                  newPlanDraft.maxBudget == null
                }
                onClick={submitNewPlan}
              >
                {createPlan.isPending ? "Criando..." : "Criar plano comercial"}
              </button>
              {createPlan.isError ? (
                <span className="text-danger">
                  Não foi possível criar o plano comercial.
                </span>
              ) : null}
            </div>
          </div>
        </section>
      ) : null}

      {createPlan.isError ? (
        <div className="alert alert-danger mb-0" role="alert">
          Não foi possível criar o planejamento de agosto.
        </div>
      ) : null}

      {createPlan.isSuccess ? (
        <div className="alert alert-success mb-0" role="status">
          Planejamento de agosto criado com foco em receita e controle de caixa.
        </div>
      ) : null}

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
                {showingPlanReferenceMonth
                  ? "Plano do mês corrente"
                  : "Plano do próximo mês"}
              </p>
              <h2 className="commercial-planning-month-title mb-2">
                {monthLabel(selectedReferenceMonth)}
              </h2>
            </div>
            <div className="commercial-planning-month-actions">
              {showingPlanReferenceMonth && currentMonthPlan.id > 0 ? (
                <button
                  className="btn btn-outline-primary"
                  type="button"
                  onClick={togglePlanEditing}
                >
                  {isEditingPlan ? "Fechar edição" : "Editar plano"}
                </button>
              ) : null}
              {!showingPlanReferenceMonth ? (
                <button
                  className="btn btn-outline-secondary"
                  type="button"
                  onClick={() => setSelectedReferenceMonth(planReferenceMonth)}
                >
                  Mês atual
                </button>
              ) : null}
              <button
                className="btn btn-primary"
                type="button"
                onClick={() =>
                  setSelectedReferenceMonth((current) => addMonths(current, 1))
                }
              >
                Próximo mês
              </button>
            </div>
          </div>

          {isEditingPlan ? (
            <section
              className="card border-primary"
              aria-label="Edição do plano comercial"
            >
              <div className="card-body d-flex flex-column gap-3">
                <div className="row g-3">
                  <div className="col-md-4">
                    <label className="form-label" htmlFor="planning-status">
                      Status
                    </label>
                    <select
                      id="planning-status"
                      className="form-select"
                      value={planDraft.status ?? "DRAFT"}
                      onChange={(event) =>
                        updatePlanDraft(
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
                  <div className="col-md-4">
                    <label className="form-label" htmlFor="planning-deadline">
                      Prazo da meta
                    </label>
                    <input
                      id="planning-deadline"
                      className="form-control"
                      type="date"
                      value={planDraft.deadline ?? ""}
                      onChange={(event) =>
                        updatePlanDraft("deadline", event.target.value)
                      }
                    />
                  </div>
                  <div className="col-md-4">
                    <label
                      className="form-label"
                      htmlFor="planning-revenue-target"
                    >
                      Meta de receita
                    </label>
                    <input
                      id="planning-revenue-target"
                      className="form-control"
                      type="number"
                      min="0"
                      step="0.01"
                      value={planDraft.targetRevenue ?? ""}
                      onChange={(event) =>
                        updatePlanDraft(
                          "targetRevenue",
                          event.target.value === ""
                            ? null
                            : Number(event.target.value),
                        )
                      }
                    />
                  </div>
                  <div className="col-md-4">
                    <label className="form-label" htmlFor="planning-max-budget">
                      Teto total do plano (R$)
                    </label>
                    <input
                      id="planning-max-budget"
                      className="form-control"
                      type="number"
                      min="0"
                      step="0.01"
                      value={planDraft.maxBudget ?? ""}
                      onChange={(event) =>
                        updatePlanDraft(
                          "maxBudget",
                          event.target.value === ""
                            ? null
                            : Number(event.target.value),
                        )
                      }
                    />
                  </div>
                  {(
                    [
                      ["offerPriceBrl", "Preço da oferta (R$)", "0.01"],
                      [
                        "variableCostPerSaleBrl",
                        "Custo variável por venda (R$)",
                        "0.01",
                      ],
                      [
                        "expectedMonthlyTraffic",
                        "Tráfego mensal esperado",
                        "1",
                      ],
                      [
                        "expectedConversionRatePercent",
                        "Conversão esperada (%)",
                        "0.01",
                      ],
                      ["expectedCacBrl", "CAC esperado (R$)", "0.01"],
                      [
                        "expectedRefundRatePercent",
                        "Reembolso esperado (%)",
                        "0.01",
                      ],
                      [
                        "fixedOperationalCostBrl",
                        "Custo operacional fixo (R$)",
                        "0.01",
                      ],
                    ] as const
                  ).map(([field, label, step]) => (
                    <div className="col-md-4" key={field}>
                      <label
                        className="form-label"
                        htmlFor={`planning-${field}`}
                      >
                        {label}
                      </label>
                      <input
                        id={`planning-${field}`}
                        className="form-control"
                        type="number"
                        min="0"
                        max={field.includes("Percent") ? "100" : undefined}
                        step={step}
                        value={planDraft[field] ?? ""}
                        onChange={(event) =>
                          updatePlanDraft(
                            field,
                            event.target.value === ""
                              ? null
                              : Number(event.target.value),
                          )
                        }
                      />
                    </div>
                  ))}
                </div>
                <div
                  className="row g-3"
                  aria-label="Metas numéricas comerciais"
                >
                  {(
                    [
                      ["productsToValidate", "Produtos a validar"],
                      ["productTypesToExplore", "Tipos de PDE a explorar"],
                      ["approachesToTest", "Abordagens a testar"],
                      ["experimentsToCreate", "Experimentos a criar"],
                      ["experimentsToPublish", "Experimentos a publicar"],
                      ["customerConversationsTarget", "Conversas com clientes"],
                    ] as const
                  ).map(([field, label]) => (
                    <div className="col-md-4" key={field}>
                      <label
                        className="form-label"
                        htmlFor={`planning-${field}`}
                      >
                        {label}
                      </label>
                      <input
                        id={`planning-${field}`}
                        className="form-control"
                        type="number"
                        min="0"
                        step="1"
                        value={planDraft[field] ?? ""}
                        onChange={(event) =>
                          updatePlanDraft(
                            field,
                            event.target.value === ""
                              ? null
                              : Number(event.target.value),
                          )
                        }
                      />
                    </div>
                  ))}
                </div>
                <label className="form-label mb-0" htmlFor="planning-objective">
                  Objetivo comercial
                </label>
                <textarea
                  id="planning-objective"
                  className="form-control"
                  rows={2}
                  value={planDraft.commercialObjective ?? ""}
                  onChange={(event) =>
                    updatePlanDraft("commercialObjective", event.target.value)
                  }
                />
                <label className="form-label mb-0" htmlFor="planning-success">
                  Critério de sucesso
                </label>
                <textarea
                  id="planning-success"
                  className="form-control"
                  rows={2}
                  value={planDraft.successCriteria ?? ""}
                  onChange={(event) =>
                    updatePlanDraft("successCriteria", event.target.value)
                  }
                />
                <label className="form-label mb-0" htmlFor="planning-stop">
                  Critério de parada
                </label>
                <textarea
                  id="planning-stop"
                  className="form-control"
                  rows={2}
                  value={planDraft.stopCriteria ?? ""}
                  onChange={(event) =>
                    updatePlanDraft("stopCriteria", event.target.value)
                  }
                />
                <label
                  className="form-label mb-0"
                  htmlFor="planning-next-action"
                >
                  Próxima ação
                </label>
                <textarea
                  id="planning-next-action"
                  className="form-control"
                  rows={2}
                  value={planDraft.nextAction ?? ""}
                  onChange={(event) =>
                    updatePlanDraft("nextAction", event.target.value)
                  }
                />
                <label className="form-label mb-0" htmlFor="planning-blocker">
                  Gargalo atual
                </label>
                <textarea
                  id="planning-blocker"
                  className="form-control"
                  rows={2}
                  value={planDraft.currentBlocker ?? ""}
                  onChange={(event) =>
                    updatePlanDraft("currentBlocker", event.target.value)
                  }
                />
                <label
                  className="form-label mb-0"
                  htmlFor="planning-root-cause"
                >
                  Causa-raiz
                </label>
                <textarea
                  id="planning-root-cause"
                  className="form-control"
                  rows={2}
                  value={planDraft.rootCause ?? ""}
                  onChange={(event) =>
                    updatePlanDraft("rootCause", event.target.value)
                  }
                />
                <div className="d-flex align-items-center gap-3">
                  <button
                    className="btn btn-primary"
                    type="button"
                    disabled={updatePlan.isPending}
                    onClick={savePlan}
                  >
                    {updatePlan.isPending
                      ? "Salvando..."
                      : "Salvar planejamento"}
                  </button>
                  {updatePlan.isError ? (
                    <span className="text-danger">
                      Não foi possível salvar o planejamento.
                    </span>
                  ) : null}
                  {updatePlan.isSuccess ? (
                    <span className="text-success">
                      Planejamento atualizado.
                    </span>
                  ) : null}
                </div>
              </div>
            </section>
          ) : null}

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
            <MonthlyMetricCard
              label="Produtos a validar"
              target={formatNumber(currentMonthPlan.productsToValidate)}
              actual="Medir por evidência"
            />
            <MonthlyMetricCard
              label="Tipos de PDE a explorar"
              target={formatNumber(currentMonthPlan.productTypesToExplore)}
              actual="Medir por evidência"
            />
            <MonthlyMetricCard
              label="Abordagens a testar"
              target={formatNumber(currentMonthPlan.approachesToTest)}
              actual="Medir por evidência"
            />
            <MonthlyMetricCard
              label="Conversas com clientes"
              target={formatNumber(
                currentMonthPlan.customerConversationsTarget,
              )}
              actual="Medir por evidência"
            />
          </div>

          <BudgetDirectionPanel plan={currentMonthPlan} weeks={weeks} />
          <GrowthOperatorPanel
            planId={currentMonthPlan.id}
            defaultObjective={currentMonthPlan.nextAction}
          />
          <ExperimentStrategistPanel
            planId={currentMonthPlan.id}
            defaultQuestion={currentMonthPlan.currentBlocker}
          />
          <FinancialAgentPanel planId={currentMonthPlan.id} />
        </div>
      </section>

      {planWeeksQuery.isError ? (
        <div className="alert alert-danger mb-0" role="alert">
          Não foi possível carregar os experimentos por semana.
        </div>
      ) : null}

      {weeks.length > 0 ? (
        <>
          <FunnelStagesPanel
            stages={monthFunnelStages}
            title="Funil acumulado do mês"
          />
          <WeeklyExperimentList
            planId={currentMonthPlan.id}
            weeks={weeks}
            agents={asArray(agentsQuery.data)}
            planVersionNumber={currentPlanVersion?.versionNumber ?? 1}
          />
          <TopExperimentsRanking weeks={weeks} />
        </>
      ) : null}
    </div>
  );
}
