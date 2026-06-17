import { Activity, Globe2, Sparkles } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  useOprmCnaeScore,
  useOprmCnaeVolume,
} from "../../api/oprm/useOprmCnaeDetail";
import {
  type OprmRoutineResearchCycleSummary,
  useOprmCnaePipelineCycles,
  useStartOprmCnaePipeline,
} from "../../api/oprm/useOprmCnaePipeline";
import {
  type OprmQualityNotes,
  useOprmRoutineQualityGateDetail,
} from "../../api/oprm/useOprmRoutineQualityGateDetail";
import { useOprmGeneratedEnrichedNichesByCnae } from "../../api/oprm/useOprmGeneratedEnrichedNichesByCnae";
import PageTitle from "../../components/PageTitle";
import OprmModuleNavigation from "./OprmModuleNavigation";
import { useBreadcrumbs } from "../../app/breadcrumbs";

const pipelineStages = [
  {
    code: "cycle",
    title: "1. Ciclo",
    description: "Ciclo pai criado para pesquisar a rotina real do CNAE.",
  },
  {
    code: "seed",
    title: "2. Seed",
    description: "Transforma CNAE em nicho operacional e queries de pesquisa.",
    usesAiModel: true,
  },
  {
    code: "search",
    title: "3. Busca",
    description:
      "Procura fontes públicas sobre rotina, tarefas e dificuldades.",
    usesInternetResearch: true,
  },
  {
    code: "fetch",
    title: "4. Coleta",
    description: "Coleta metadados e trechos úteis das fontes selecionadas.",
    usesInternetResearch: true,
  },
  {
    code: "signals",
    title: "5. Sinais",
    description: "Extrai sinais estruturados sobre dores, rotina e linguagem.",
  },
  {
    code: "synthesis",
    title: "6. Síntese",
    description: "Monta o cartão de rotina do nicho com evidências.",
  },
  {
    code: "mei",
    title: "7. MEI",
    description: "Define o público MEI/autônomo dono-operador do nicho.",
    usesAiModel: true,
  },
  {
    code: "quality",
    title: "8. Qualidade",
    description:
      "Valida se a pesquisa é específica, recente e sem solução pronta.",
  },
  {
    code: "materialization",
    title: "9. Materialização",
    description:
      "Grava o nicho enriquecido para alimentar ofertas e experimentos.",
  },
];

function formatNumber(value?: number | null) {
  if (value === null || value === undefined) {
    return "Sem dado";
  }
  return Number(value).toLocaleString("pt-BR");
}

function formatScore(value?: number | null) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return "Sem score";
  }
  return Number(value).toLocaleString("pt-BR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function formatUsd(value?: number | string | null) {
  if (value === null || value === undefined || value === "") {
    return "US$ 0,0000";
  }
  const numericValue = Number(value);
  if (Number.isNaN(numericValue)) {
    return "US$ 0,0000";
  }
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 4,
    maximumFractionDigits: 4,
  }).format(numericValue);
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "Ainda não finalizado";
  }
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

const statusLabels: Record<string, string> = {
  RUNNING: "Em execução",
  COMPLETED: "Concluído",
  READY_FOR_HYPOTHESIS: "Pronto",
  LIGHTLY_RESEARCHED: "Pesquisa inicial concluída",
  ROUTINE_SYNTHESIZED: "Rotina sintetizada",
  MEI_AUDIENCE_SEGMENTED: "Perfil MEI/autônomo criado",
  MEI_AUDIENCE_READY: "Público MEI/autônomo pronto",
  ENRICHED_NICHE_CREATED: "Nicho enriquecido disponível",
  FAILED: "Falhou",
  STALLED: "Parado sem progresso",
  CANCELLED_BY_MANUAL_RESTART: "Cancelado por reinício",
  NEEDS_MORE_RESEARCH: "Precisa aprofundar",
  NEEDS_MORE_MEI_RESEARCH: "Precisa de mais MEI",
  OUTDATED_SOURCES: "Fontes antigas",
  TOO_CORPORATE: "Corporativo demais",
  SOLUTION_CONTAMINATED: "Contaminado por solução",
  NEEDS_EXECUTOR_ROUTINE_EVIDENCE: "Precisa evidência da execução real",
  GENERIC: "Genérico",
  ENRICHED_NICHE_FAILED: "Falha na materialização",
};

const AUTO_QUALITY_REPROCESS_TRIGGER = "AUTO_QUALITY_REPROCESS";
const MAX_AUTO_REPROCESS_PER_CANDIDATE = 3;

function isAutomaticReprocessCycle(
  cycle?: OprmRoutineResearchCycleSummary | null,
) {
  return cycle?.triggerSource === AUTO_QUALITY_REPROCESS_TRIGGER;
}

function countAutomaticReprocessCycles(
  cycles?: OprmRoutineResearchCycleSummary[],
) {
  return (cycles ?? []).filter(isAutomaticReprocessCycle).length;
}

function findPreviousCycle(
  cycles: OprmRoutineResearchCycleSummary[] | undefined,
  latestCycle?: OprmRoutineResearchCycleSummary | null,
) {
  return (cycles ?? []).find(
    (cycle) => cycle.researchCycleId !== latestCycle?.researchCycleId,
  );
}

function automaticProcessMessage(
  latestCycle?: OprmRoutineResearchCycleSummary | null,
  previousCycle?: OprmRoutineResearchCycleSummary | null,
  automaticAttempts = 0,
) {
  if (!latestCycle) {
    return null;
  }
  if (isAutomaticReprocessCycle(latestCycle)) {
    return {
      title: "Reprocessamento automático em andamento",
      text: previousCycle
        ? `O ciclo #${latestCycle.researchCycleId} foi criado automaticamente depois que o ciclo #${previousCycle.researchCycleId} terminou como ${statusLabel(previousCycle.status)}. O sistema reaproveitou o subnicho aprendido e as notas do gate anterior para tentar corrigir a causa sem novo clique.`
        : `O ciclo #${latestCycle.researchCycleId} foi criado automaticamente. O sistema está reaproveitando o aprendizado do gate anterior para continuar sem novo clique.`,
      className: "alert alert-primary border mt-3 mb-0",
    };
  }
  if (isQualityBlockedStatus(latestCycle.status)) {
    const reachedLimit = automaticAttempts >= MAX_AUTO_REPROCESS_PER_CANDIDATE;
    return {
      title: reachedLimit
        ? "Limite automático atingido"
        : "Aguardando reprocessamento automático",
      text: reachedLimit
        ? `O sistema já usou ${automaticAttempts}/${MAX_AUTO_REPROCESS_PER_CANDIDATE} tentativas automáticas para este candidato. Revise o detalhe da etapa de qualidade antes de gastar novo ciclo manual.`
        : `O gate indicou ${statusLabel(latestCycle.status)}. O backend deve abrir a próxima tentativa automaticamente e carregar este aprendizado para o seed do novo ciclo.`,
      className: reachedLimit
        ? "alert alert-warning border mt-3 mb-0"
        : "alert alert-info border mt-3 mb-0",
    };
  }
  return null;
}

const qualityBlockedStatuses = new Set([
  "NEEDS_MORE_RESEARCH",
  "NEEDS_MORE_MEI_RESEARCH",
  "OUTDATED_SOURCES",
  "TOO_CORPORATE",
  "SOLUTION_CONTAMINATED",
  "NEEDS_EXECUTOR_ROUTINE_EVIDENCE",
  "GENERIC",
]);

function statusLabel(status?: string | null) {
  return status ? (statusLabels[status] ?? status) : "Aguardando";
}

function isQualityBlockedStatus(status?: string | null) {
  return Boolean(status && qualityBlockedStatuses.has(status));
}

function isCycleRunningStatus(cycle?: OprmRoutineResearchCycleSummary | null) {
  return Boolean(
    cycle &&
    !cycle.finishedAt &&
    !isCycleStoppedStatus(cycle.status) &&
    cycle.status !== "ENRICHED_NICHE_CREATED",
  );
}

function isCycleStoppedStatus(status?: string | null) {
  return Boolean(
    status &&
    (isQualityBlockedStatus(status) ||
      [
        "FAILED",
        "STALLED",
        "CANCELLED_BY_MANUAL_RESTART",
        "ENRICHED_NICHE_FAILED",
      ].includes(status)),
  );
}

function isSolutionContaminationFailure(
  status?: string | null,
  errorMessage?: string | null,
) {
  const normalized = `${status ?? ""} ${errorMessage ?? ""}`.toLowerCase();
  return (
    status === "SOLUTION_CONTAMINATED" ||
    normalized.includes("segmentação contaminada por linguagem de solução") ||
    normalized.includes("contaminada por linguagem de solução") ||
    normalized.includes("contaminado por solução") ||
    normalized.includes("linguagem de solução")
  );
}

function buildBusinessRecommendation(params: {
  status?: string | null;
  errorMessage?: string | null;
  difficultyEvidenceScore?: number | null;
  routineEvidenceScore?: number | null;
  recommendedSubniche?: string | null;
}) {
  if (isSolutionContaminationFailure(params.status, params.errorMessage)) {
    return "A pesquisa pulou cedo para produto/oferta; reprocessar com foco em rotina e público.";
  }
  if (params.difficultyEvidenceScore === 0) {
    return "Ainda não há dor operacional suficiente para transformar em nicho vendável.";
  }
  if (
    (params.routineEvidenceScore ?? 0) > 0 &&
    params.recommendedSubniche?.trim()
  ) {
    return `Subnicho recomendado: ${params.recommendedSubniche.trim()}.`;
  }
  return null;
}

function getQualityNoteText(
  notes: OprmQualityNotes | null | undefined,
  key: string,
) {
  const value = notes?.[key];
  return typeof value === "string" && value.trim() ? value.trim() : null;
}

function describeRejectedSituations(notes?: OprmQualityNotes | null) {
  if (!notes) {
    return [];
  }
  const rejected: string[] = [];
  const numberValue = (key: string) =>
    typeof notes[key] === "number" ? Number(notes[key]) : undefined;
  const booleanValue = (key: string) =>
    typeof notes[key] === "boolean" ? Boolean(notes[key]) : undefined;

  const solutionRisk = numberValue("riscoLinguagemSolucao");
  const textualSolutionRisk = numberValue("riscoTextualSolucao");
  if (booleanValue("dominadoPorSolucao") === true || (solutionRisk ?? 0) > 35) {
    rejected.push(
      `Contaminação por solução: risco ${solutionRisk ?? "não informado"}%${
        textualSolutionRisk !== undefined
          ? ` e risco textual ${textualSolutionRisk}%`
          : ""
      }. Rejeitado porque apareceu produto, software, app, automação, curso, template ou oferta antes de validar a rotina real.`,
    );
  }

  const outdatedRisk = numberValue("riscoFonteAntiga");
  if (
    booleanValue("fontesRecentesSuficientes") === false ||
    (outdatedRisk ?? 0) > 45
  ) {
    rejected.push(
      `Atualidade das fontes: risco ${outdatedRisk ?? "não informado"}%. Rejeitado porque as evidências são antigas, sem data ou pouco recentes para sustentar decisão comercial.`,
    );
  }

  const corporateRisk = numberValue("riscoEmpresaEstruturada");
  if ((corporateRisk ?? 0) > 45) {
    rejected.push(
      `Desvio para empresa estruturada: risco ${corporateRisk}%. Rejeitado porque a pesquisa ficou mais parecida com empresa/corporação do que com MEI ou autônomo dono-operador.`,
    );
  }

  if (booleanValue("rotinaRevelaTarefasReaisExecutor") === false) {
    rejected.push(
      "Rotina do executor insuficiente: faltam tarefas manuais concretas do profissional, como atendimento real, materiais, procedimentos, entrega e retrabalho.",
    );
  }

  if (booleanValue("mixMinimoMeiAutonomo") === false) {
    rejected.push(
      "Mix mínimo MEI/autônomo incompleto: a pesquisa ainda não juntou rotina, aquisição/canais, dor prática e dor emocional/sonho/medo no mesmo cartão.",
    );
  }

  if (
    booleanValue(
      "faltaEvidenciaAquisicaoCanaisRecorrenciaOuComportamentoClientes",
    ) === true
  ) {
    rejected.push(
      "Aquisição e recorrência fracas: faltam evidências claras de como o profissional consegue clientes, fideliza, reativa, agenda, cobra ou gera recorrência.",
    );
  }

  const practicalPain = numberValue("dorPratica");
  if (practicalPain !== undefined && practicalPain < 1) {
    rejected.push(
      "Dor prática ausente: o gate não encontrou problema operacional concreto suficiente para virar oportunidade vendável.",
    );
  }

  return rejected;
}

function buildQualityResultSummary(notes?: OprmQualityNotes | null) {
  if (!notes) {
    return null;
  }
  const parts = [
    typeof notes.fontes === "number" ? `${notes.fontes} fontes` : null,
    typeof notes.sinais === "number" ? `${notes.sinais} sinais` : null,
    typeof notes.tarefasConcretasDistintas === "number"
      ? `${notes.tarefasConcretasDistintas} tarefas concretas distintas`
      : null,
    typeof notes.aquisicaoOuCanal === "number"
      ? `${notes.aquisicaoOuCanal} sinais de aquisição/canal`
      : null,
    typeof notes.dorPratica === "number"
      ? `${notes.dorPratica} dores práticas`
      : null,
  ].filter(Boolean);
  return parts.length > 0 ? parts.join(" · ") : null;
}

function qualityBlockedMessage(status?: string | null) {
  const messages: Record<string, string> = {
    NEEDS_MORE_RESEARCH:
      "A pesquisa chegou ao gate de qualidade, mas ainda precisa de mais evidências práticas antes de virar nicho enriquecido.",
    NEEDS_MORE_MEI_RESEARCH:
      "A pesquisa chegou ao gate de qualidade, mas faltam sinais mais fortes do público MEI/autônomo dono-operador.",
    OUTDATED_SOURCES:
      "A pesquisa chegou ao gate de qualidade, mas foi bloqueada por fontes antigas ou sem atualidade suficiente.",
    TOO_CORPORATE:
      "A pesquisa chegou ao gate de qualidade, mas as evidências estão corporativas demais para MEI/autônomo dono-operador.",
    SOLUTION_CONTAMINATED:
      "A pesquisa chegou ao gate de qualidade, mas foi contaminada por linguagem de solução/oferta antes da hora.",
    NEEDS_EXECUTOR_ROUTINE_EVIDENCE:
      "O nicho tem sinais de venda, mas falta comprovar a rotina manual executada pelo profissional. Próximo movimento: pesquisar tarefas reais, materiais, tempo, deslocamento, retrabalho e problemas práticos do atendimento.",
    GENERIC:
      "A pesquisa chegou ao gate de qualidade, mas ficou genérica demais para sustentar uma oferta vendável.",
  };
  return status ? messages[status] : undefined;
}

function pipelineAlertClass(status?: string | null) {
  if (isQualityBlockedStatus(status)) {
    return "alert alert-warning mb-0";
  }
  if (
    status === "FAILED" ||
    status === "STALLED" ||
    status === "ENRICHED_NICHE_FAILED"
  ) {
    return "alert alert-danger mb-0";
  }
  return "alert alert-info mb-0";
}

function getCompletedStageIndex(cycle: OprmRoutineResearchCycleSummary) {
  if ((cycle.totalExtractedSignals ?? 0) > 0) {
    return 4;
  }
  if ((cycle.totalSourceSnapshots ?? 0) > 0) {
    return 3;
  }
  if ((cycle.totalSourceCandidates ?? 0) > 0) {
    return 2;
  }
  if ((cycle.totalQueries ?? 0) > 0) {
    return 1;
  }
  return 0;
}

function inferFailureStageIndex(errorMessage?: string | null) {
  const normalized = errorMessage?.toLowerCase() ?? "";
  if (!normalized) {
    return undefined;
  }
  if (normalized.includes("mei-audience-segmenter")) {
    return 6;
  }
  if (normalized.includes("seed") || normalized.includes("etapa dois")) {
    return 1;
  }
  if (
    normalized.includes("source-search") ||
    normalized.includes("source search") ||
    normalized.includes("busca")
  ) {
    return 2;
  }
  if (
    normalized.includes("source-fetch") ||
    normalized.includes("source fetch") ||
    normalized.includes("coleta")
  ) {
    return 3;
  }
  if (
    normalized.includes("signal") ||
    normalized.includes("sinal") ||
    normalized.includes("extração")
  ) {
    return 4;
  }
  if (
    normalized.includes("synthesis") ||
    normalized.includes("synthesizer") ||
    normalized.includes("síntese")
  ) {
    return 5;
  }
  if (normalized.includes("mei") || normalized.includes("audience segmenter")) {
    return 6;
  }
  if (normalized.includes("quality") || normalized.includes("gate")) {
    return 7;
  }
  if (
    normalized.includes("materialization") ||
    normalized.includes("materializer") ||
    normalized.includes("nicho enriquecido")
  ) {
    return 8;
  }
  return undefined;
}

function stageCardClassName(stateClassName: string) {
  if (stateClassName.includes("border-success")) {
    return "border-success bg-success text-white shadow-sm";
  }
  if (stateClassName.includes("border-primary")) {
    return `${stateClassName} bg-primary-subtle border-2`;
  }
  if (stateClassName.includes("border-warning")) {
    return `${stateClassName} bg-warning-subtle border-2`;
  }
  if (stateClassName.includes("border-danger")) {
    return `${stateClassName} bg-danger-subtle border-2`;
  }
  return `${stateClassName} bg-body-tertiary`;
}

function stageDescriptionClassName(stateClassName: string) {
  if (stateClassName.includes("border-success")) {
    return "small text-white-50 mb-3";
  }
  return "small text-secondary mb-3";
}

function stageDetailButtonClassName(stateClassName: string) {
  if (stateClassName.includes("border-success")) {
    return "btn btn-light btn-sm";
  }
  return "btn btn-outline-primary btn-sm";
}

function inferStageState(
  stageIndex: number,
  cycle?: OprmRoutineResearchCycleSummary,
) {
  if (!cycle) {
    return {
      label: "Aguardando início",
      className: "border-secondary text-secondary",
    };
  }

  const completedStageIndex = getCompletedStageIndex(cycle);
  const failed = cycle.status === "FAILED" || cycle.status?.includes("FAILED");
  const failureStageIndex = failed
    ? inferFailureStageIndex(cycle.errorMessage)
    : undefined;

  if (isQualityBlockedStatus(cycle.status)) {
    if (stageIndex <= 6) {
      return { label: "Concluído", className: "border-success text-success" };
    }
    if (stageIndex === 7) {
      return {
        label: statusLabel(cycle.status),
        className: "border-warning text-warning",
      };
    }
    return { label: "Bloqueado", className: "border-secondary text-secondary" };
  }

  if (cycle.status === "ROUTINE_SYNTHESIZED") {
    if (stageIndex <= 5) {
      return { label: "Concluído", className: "border-success text-success" };
    }
    if (stageIndex === 6) {
      return { label: "Em execução", className: "border-primary text-primary" };
    }
    return { label: "Na fila", className: "border-secondary text-secondary" };
  }

  if (cycle.status === "MEI_AUDIENCE_SEGMENTED") {
    if (stageIndex <= 6) {
      return { label: "Concluído", className: "border-success text-success" };
    }
    if (stageIndex === 7) {
      return { label: "Em execução", className: "border-primary text-primary" };
    }
    return { label: "Na fila", className: "border-secondary text-secondary" };
  }

  if (
    cycle.status === "MEI_AUDIENCE_READY" ||
    cycle.status === "LIGHTLY_RESEARCHED"
  ) {
    if (stageIndex <= 7) {
      return { label: "Concluído", className: "border-success text-success" };
    }
    return { label: "Em execução", className: "border-primary text-primary" };
  }

  if (cycle.status === "ENRICHED_NICHE_CREATED") {
    return { label: "Concluído", className: "border-success text-success" };
  }

  if (cycle.status === "STALLED") {
    if (stageIndex <= completedStageIndex) {
      return { label: "Concluído", className: "border-success text-success" };
    }
    if (stageIndex === completedStageIndex + 1) {
      return { label: "Parado", className: "border-danger text-danger" };
    }
    return { label: "Bloqueado", className: "border-secondary text-secondary" };
  }

  if (cycle.status === "CANCELLED_BY_MANUAL_RESTART") {
    if (stageIndex <= completedStageIndex) {
      return { label: "Concluído", className: "border-success text-success" };
    }
    return { label: "Cancelado", className: "border-secondary text-secondary" };
  }

  if (failed && stageIndex === failureStageIndex) {
    return { label: "Falhou", className: "border-danger text-danger" };
  }
  if (!failed && cycle.finishedAt) {
    return { label: "Concluído", className: "border-success text-success" };
  }
  if (stageIndex <= completedStageIndex) {
    return { label: "Concluído", className: "border-success text-success" };
  }
  if (failed) {
    return { label: "Bloqueado", className: "border-secondary text-secondary" };
  }
  if (
    !isCycleStoppedStatus(cycle.status) &&
    stageIndex === completedStageIndex + 1
  ) {
    return { label: "Em execução", className: "border-primary text-primary" };
  }
  return { label: "Na fila", className: "border-secondary text-secondary" };
}

export default function OprmCnaeDetailPlaceholderPage() {
  const { cnaeCode, researchCycleId } = useParams();
  const navigate = useNavigate();
  const decodedCnaeCode = cnaeCode ? decodeURIComponent(cnaeCode) : "CNAE";
  const selectedResearchCycleId = researchCycleId
    ? Number(researchCycleId)
    : null;
  const [showPipeline, setShowPipeline] = useState(
    Boolean(selectedResearchCycleId),
  );
  const volumeQuery = useOprmCnaeVolume(decodedCnaeCode);
  const scoreQuery = useOprmCnaeScore(decodedCnaeCode);
  const cyclesQuery = useOprmCnaePipelineCycles(decodedCnaeCode);
  const generatedNichesQuery =
    useOprmGeneratedEnrichedNichesByCnae(decodedCnaeCode);
  const startPipelineMutation = useStartOprmCnaePipeline(decodedCnaeCode);
  const latestCycle = selectedResearchCycleId
    ? cyclesQuery.data?.find(
        (cycle) => cycle.researchCycleId === selectedResearchCycleId,
      )
    : cyclesQuery.data?.[0];
  const previousCycle = findPreviousCycle(cyclesQuery.data, latestCycle);
  const automaticAttempts = countAutomaticReprocessCycles(cyclesQuery.data);
  const automaticProcess = automaticProcessMessage(
    latestCycle,
    previousCycle,
    automaticAttempts,
  );
  const qualityGateQuery = useOprmRoutineQualityGateDetail(
    latestCycle?.researchCycleId,
  );
  const businessRecommendation = latestCycle
    ? buildBusinessRecommendation({
        status: latestCycle.status,
        errorMessage: latestCycle.errorMessage,
        difficultyEvidenceScore: qualityGateQuery.data?.difficultyEvidenceScore,
        routineEvidenceScore: qualityGateQuery.data?.routineEvidenceScore,
        recommendedSubniche:
          latestCycle.neutralNicheName ?? latestCycle.nicheName,
      })
    : null;
  const qualityRejectedSituations = describeRejectedSituations(
    qualityGateQuery.data?.qualityNotes,
  );
  const qualityResultSummary = buildQualityResultSummary(
    qualityGateQuery.data?.qualityNotes,
  );
  const qualityNextMove = getQualityNoteText(
    qualityGateQuery.data?.qualityNotes,
    "proximoMovimento",
  );
  const cnaeDescription =
    volumeQuery.data?.cnaeDescription ??
    scoreQuery.data?.cnaeDescription ??
    "Descrição ainda não encontrada";

  useBreadcrumbs([
    { label: "OPRM", to: "/oprm" },
    {
      label: selectedResearchCycleId
        ? "Pipeline do subnicho"
        : "Subnichos do CNAE",
    },
  ]);

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <PageTitle>
          {selectedResearchCycleId
            ? "Pipeline do subnicho"
            : "Subnichos do CNAE"}
        </PageTitle>
        <p className="text-secondary mb-0">
          {selectedResearchCycleId
            ? "Acompanhe somente os jobs, custos e etapas do subnicho recém-criado, sem misturar com execuções antigas do mesmo CNAE."
            : "Confira todos os subnichos já criados para este CNAE e gere um novo apenas quando houver espaço comercial para uma oportunidade diferente."}
        </p>
      </header>

      <OprmModuleNavigation />

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <div className="d-flex flex-wrap justify-content-between gap-3">
            <div>
              <span className="badge text-bg-primary mb-3">
                CNAE {decodedCnaeCode}
              </span>
              <h2 className="h4 mb-2">{cnaeDescription}</h2>
              <p className="text-secondary mb-0">
                Score e volume ajudam a priorizar mercados com maior chance de
                virar produto digital vendável sem perder a visão de rotina
                real.
              </p>
            </div>
            <div className="d-flex align-items-start gap-2">
              <Link className="btn btn-outline-secondary" to="/oprm">
                Voltar para CNAEs
              </Link>
            </div>
          </div>
          {startPipelineMutation.isSuccess ? (
            <div className="alert alert-success mb-0" role="status">
              Novo subnicho iniciado para o CNAE {decodedCnaeCode}. Ciclo #
              {startPipelineMutation.data.researchCycleId} criado para
              acompanhamento dedicado.
            </div>
          ) : null}
          {startPipelineMutation.isError ? (
            <div className="alert alert-warning mb-0" role="alert">
              {startPipelineMutation.error.message}
            </div>
          ) : null}
        </div>
      </section>

      <section className="row g-3">
        <div className="col-md-3">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <span className="text-secondary small">Score OPRM</span>
              <div className="display-6 fw-semibold">
                {formatScore(
                  scoreQuery.data?.opportunityScore ??
                    volumeQuery.data?.opportunityScore,
                )}
              </div>
              <span className="small text-secondary">
                {scoreQuery.data?.scoreStatus ??
                  volumeQuery.data?.scoreStatus ??
                  "Status não informado"}
              </span>
            </div>
          </div>
        </div>
        <div className="col-md-3">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <span className="text-secondary small">Estabelecimentos</span>
              <div className="display-6 fw-semibold">
                {formatNumber(volumeQuery.data?.totalEstabelecimentos)}
              </div>
              <span className="small text-secondary">
                Ativos:{" "}
                {formatNumber(volumeQuery.data?.totalEstabelecimentosAtivos)}
              </span>
            </div>
          </div>
        </div>
        <div className="col-md-3">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <span className="text-secondary small">Empresas MEI</span>
              <div className="display-6 fw-semibold">
                {formatNumber(volumeQuery.data?.totalEmpresasMei)}
              </div>
              <span className="small text-secondary">
                Empresas: {formatNumber(volumeQuery.data?.totalEmpresas)}
              </span>
            </div>
          </div>
        </div>
        <div className="col-md-3">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <span className="text-secondary small">Componentes do score</span>
              <div className="small d-flex flex-column gap-1 mt-2">
                <span>
                  Volume: {formatScore(scoreQuery.data?.marketVolumeScore)}
                </span>
                <span>
                  MEI: {formatScore(scoreQuery.data?.meiDensityScore)}
                </span>
                <span>
                  Digital: {formatScore(scoreQuery.data?.digitalFitScore)}
                </span>
                <span>
                  Dor: {formatScore(scoreQuery.data?.painClarityScore)}
                </span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <div className="d-flex flex-wrap justify-content-between gap-3 align-items-start">
            <div>
              <h2 className="h5 mb-1">Subnichos deste CNAE</h2>
              <p className="text-secondary mb-0">
                Esta é a segunda etapa do fluxo: o usuário escolhe um subnicho
                existente ou solicita a criação de um novo subnicho com
                potencial de venda.
              </p>
            </div>
            <button
              type="button"
              className="btn btn-primary"
              disabled={startPipelineMutation.isPending}
              onClick={() =>
                startPipelineMutation.mutate(undefined, {
                  onSuccess: (result) => {
                    setShowPipeline(true);
                    if (result.researchCycleId) {
                      navigate(
                        `/oprm/cnaes/${encodeURIComponent(decodedCnaeCode)}/subnichos/${result.researchCycleId}`,
                      );
                    }
                  },
                })
              }
            >
              {startPipelineMutation.isPending ? (
                <>
                  <span
                    className="spinner-border spinner-border-sm me-2"
                    aria-hidden="true"
                  />
                  Gerando...
                </>
              ) : (
                "Criar novo subnicho"
              )}
            </button>
          </div>

          {generatedNichesQuery.isLoading ? (
            <div className="alert alert-light border mb-0" role="status">
              Carregando subnichos do CNAE...
            </div>
          ) : null}
          {generatedNichesQuery.isError ? (
            <div className="alert alert-warning mb-0" role="alert">
              Não foi possível carregar os subnichos deste CNAE.
            </div>
          ) : null}
          {generatedNichesQuery.data?.length ? (
            <div className="table-responsive">
              <table className="table table-sm align-middle mb-0">
                <thead>
                  <tr>
                    <th scope="col">Subnicho</th>
                    <th scope="col">Qualidade</th>
                    <th scope="col">Ciclo</th>
                    <th scope="col">Evidências</th>
                    <th scope="col">Gerado em</th>
                    <th scope="col" className="text-end">
                      Ação
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {generatedNichesQuery.data.map((niche) => (
                    <tr key={niche.enrichedNicheProfileId}>
                      <td>
                        <div className="fw-semibold">{niche.nicheName}</div>
                        <span className="small text-secondary">
                          Nicho #{niche.marketNicheId} · Perfil #
                          {niche.enrichedNicheProfileId}
                        </span>
                      </td>
                      <td>{statusLabel(niche.qualityStatus)}</td>
                      <td>#{niche.researchCycleId}</td>
                      <td>
                        Rotina {niche.routineEvidenceScore} · Dor{" "}
                        {niche.difficultyEvidenceScore} · Fontes{" "}
                        {niche.sourceDiversityScore}
                      </td>
                      <td>{formatDateTime(niche.materializedAt)}</td>
                      <td className="text-end">
                        <Link
                          className="btn btn-outline-primary btn-sm"
                          to={`/oprm/cnaes/${encodeURIComponent(decodedCnaeCode)}/subnichos/${niche.researchCycleId}`}
                        >
                          Acompanhar
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
          {!generatedNichesQuery.isLoading &&
          !generatedNichesQuery.isError &&
          !generatedNichesQuery.data?.length ? (
            <div className="alert alert-secondary mb-0">
              Ainda não existe subnicho criado para este CNAE. Use “Criar novo
              subnicho” para iniciar a análise.
            </div>
          ) : null}
        </div>
      </section>

      {showPipeline ? (
        <section className="card border-0 shadow-sm">
          <div className="card-body d-flex flex-column gap-3">
            <div className="d-flex flex-wrap justify-content-between gap-3 align-items-start">
              <div>
                <h2 className="h5 mb-1">Execução do pipeline NichoCNAE</h2>
                <p className="text-secondary mb-0">
                  Cards por fase para acompanhar onde a pesquisa está antes de
                  transformar o CNAE em nicho enriquecido.
                </p>
              </div>
              <div className="d-flex flex-wrap gap-2 align-items-start justify-content-end">
                <div className="border rounded-3 bg-light px-3 py-2 text-end">
                  <span className="d-block small text-secondary">
                    Custo total do subnicho
                  </span>
                  <strong className="fs-5">
                    {formatUsd(latestCycle?.executionCostUsd)}
                  </strong>
                </div>
                <span className="badge text-bg-light align-self-start">
                  Ciclo selecionado:{" "}
                  {latestCycle ? `#${latestCycle.researchCycleId}` : "nenhum"}
                </span>
              </div>
            </div>

            {latestCycle ? (
              <div className={pipelineAlertClass(latestCycle.status)}>
                <div className="d-flex flex-wrap align-items-center gap-3">
                  {isCycleRunningStatus(latestCycle) ? (
                    <div
                      className="pipeline-running-illustration"
                      aria-label="Pipeline em execução"
                      role="img"
                    >
                      <span className="pipeline-running-illustration__orbit" />
                      <span className="pipeline-running-illustration__core">
                        <Activity size={26} aria-hidden="true" />
                      </span>
                      <span className="pipeline-running-illustration__dot pipeline-running-illustration__dot--one" />
                      <span className="pipeline-running-illustration__dot pipeline-running-illustration__dot--two" />
                    </div>
                  ) : null}
                  <div>
                    Status atual:{" "}
                    <strong>{statusLabel(latestCycle.status)}</strong> ·
                    iniciado em {formatDateTime(latestCycle.startedAt)} · sinais
                    extraídos: {formatNumber(latestCycle.totalExtractedSignals)}{" "}
                    · custo do job atual:{" "}
                    <strong>{formatUsd(latestCycle.executionCostUsd)}</strong>
                    {isCycleRunningStatus(latestCycle) ? (
                      <span className="d-block small fw-semibold text-primary mt-1">
                        Processando automaticamente. A tela acompanha o avanço
                        pelas etapas abaixo.
                      </span>
                    ) : null}
                  </div>
                </div>
                {qualityBlockedMessage(latestCycle.status) ? (
                  <div className="mt-2">
                    <p className="mb-2">
                      {qualityBlockedMessage(latestCycle.status)} O pipeline não
                      está em execução agora; pesquise novamente após corrigir a
                      causa do bloqueio.
                    </p>
                    {qualityResultSummary ? (
                      <p className="mb-2 small">
                        <span className="fw-semibold">Resultado apurado:</span>{" "}
                        {qualityResultSummary}.
                      </p>
                    ) : null}
                    {qualityNextMove ? (
                      <div className="alert alert-info border mb-2 py-2 px-3">
                        <span className="fw-semibold d-block mb-1">
                          Próximo movimento automático
                        </span>
                        <span>{qualityNextMove}.</span>
                      </div>
                    ) : null}
                    {qualityRejectedSituations.length > 0 ? (
                      <div className="alert alert-light border mb-0 py-2 px-3">
                        <span className="fw-semibold d-block mb-1">
                          Situações rejeitadas pelo gate
                        </span>
                        <ul className="mb-0 ps-3">
                          {qualityRejectedSituations.map((situation) => (
                            <li key={situation}>{situation}</li>
                          ))}
                        </ul>
                      </div>
                    ) : null}
                  </div>
                ) : null}
                {automaticProcess ? (
                  <div className={automaticProcess.className} role="status">
                    <span className="fw-semibold d-block mb-1">
                      {automaticProcess.title}
                    </span>
                    <span>{automaticProcess.text}</span>
                    <span className="d-block small text-secondary mt-1">
                      Tentativas automáticas usadas: {automaticAttempts}/
                      {MAX_AUTO_REPROCESS_PER_CANDIDATE}.
                    </span>
                  </div>
                ) : null}
                {businessRecommendation ? (
                  <div className="alert alert-light border mt-3 mb-0">
                    <span className="fw-semibold d-block">
                      Recomendação de negócio
                    </span>
                    {businessRecommendation}
                    <span className="d-block small text-secondary mt-1">
                      Comando recomendado: Reprocessar com subnicho operacional.
                    </span>
                  </div>
                ) : null}
                {latestCycle.errorMessage ? (
                  <div className="mt-2 text-danger">
                    Falha registrada: {latestCycle.errorMessage}
                  </div>
                ) : null}
              </div>
            ) : (
              <div className="alert alert-secondary mb-0">
                Nenhum ciclo encontrado para este CNAE. Use o botão de disparo
                quando já existir candidato pendente de NichoCNAE para esse
                CNAE.
              </div>
            )}

            <div className="row g-3">
              {pipelineStages.map((stage, index) => {
                const state = inferStageState(index, latestCycle);
                return (
                  <div className="col-md-4" key={stage.title}>
                    <div
                      className={`card h-100 ${stageCardClassName(state.className)}`}
                    >
                      <div className="card-body">
                        <div className="d-flex justify-content-between gap-2 mb-2">
                          <div className="d-flex align-items-center gap-2">
                            <h3 className="h6 mb-0">{stage.title}</h3>
                            {stage.usesAiModel ? (
                              <span
                                className="badge text-bg-primary d-inline-flex align-items-center gap-1"
                                title="Etapa com uso direto de IA"
                                aria-label="Etapa com uso direto de IA"
                              >
                                <Sparkles size={14} aria-hidden="true" />
                                IA
                              </span>
                            ) : null}
                            {stage.usesInternetResearch ? (
                              <span
                                className="badge text-bg-info d-inline-flex align-items-center gap-1"
                                title="Etapa que acessa a internet para pesquisar fontes"
                                aria-label="Etapa que acessa a internet para pesquisar fontes"
                              >
                                <Globe2 size={14} aria-hidden="true" />
                                Web
                              </span>
                            ) : null}
                          </div>
                          <span className="badge text-bg-light d-inline-flex align-items-center gap-1">
                            {state.label === "Em execução" ? (
                              <span
                                className="pipeline-stage-running-dot"
                                aria-hidden="true"
                              />
                            ) : null}
                            {state.label}
                          </span>
                        </div>
                        <p
                          className={stageDescriptionClassName(state.className)}
                        >
                          {stage.description}
                        </p>
                        {latestCycle ? (
                          <Link
                            className={stageDetailButtonClassName(
                              state.className,
                            )}
                            to={`/oprm/cnaes/${encodeURIComponent(decodedCnaeCode)}/pipeline/${latestCycle.researchCycleId}/stages/${stage.code}`}
                          >
                            Ver detalhes
                          </Link>
                        ) : (
                          <button
                            type="button"
                            className="btn btn-outline-secondary btn-sm"
                            disabled
                          >
                            Ver detalhes
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>

            <div className="card border-0 bg-light">
              <div className="card-body">
                <div className="d-flex flex-wrap justify-content-between gap-2 mb-3">
                  <div>
                    <h3 className="h6 mb-1">Jobs deste subnicho</h3>
                    <p className="small text-secondary mb-0">
                      Histórico restrito ao ciclo do subnicho selecionado para
                      controlar gasto operacional sem misturar execuções antigas
                      do CNAE.
                    </p>
                  </div>
                  <strong>
                    Total: {formatUsd(latestCycle?.executionCostUsd)}
                  </strong>
                </div>
                <div className="table-responsive">
                  <table className="table table-sm align-middle mb-0">
                    <thead>
                      <tr>
                        <th scope="col">Job</th>
                        <th scope="col">Status</th>
                        <th scope="col">Início</th>
                        <th scope="col">Fim</th>
                        <th scope="col" className="text-end">
                          Custo total
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {latestCycle ? (
                        [latestCycle].map((cycle) => (
                          <tr key={cycle.researchCycleId}>
                            <td>#{cycle.researchCycleId}</td>
                            <td>{statusLabel(cycle.status)}</td>
                            <td>{formatDateTime(cycle.startedAt)}</td>
                            <td>{formatDateTime(cycle.finishedAt)}</td>
                            <td className="text-end fw-semibold">
                              {formatUsd(cycle.executionCostUsd)}
                            </td>
                          </tr>
                        ))
                      ) : (
                        <tr>
                          <td
                            colSpan={5}
                            className="text-secondary text-center py-3"
                          >
                            Nenhum job executado para este subnicho.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </section>
      ) : null}
    </div>
  );
}
