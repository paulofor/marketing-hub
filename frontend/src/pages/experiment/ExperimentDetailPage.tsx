import { Fragment, useEffect, useMemo, useRef, useState } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import axios from "axios";
import { toast } from "react-toastify";
import { useExperiment } from "../../api/experiment/useExperiment";
import type { ExperimentStage } from "../../api/experiment/useExperiments";
import { useExperimentDiagnostics } from "../../api/experiment/useExperimentDiagnostics";
import { useMetricPresets } from "../../api/experiment/useMetricPresets";
import { useNiche } from "../../api/niche/useNiche";
import { useHypothesis } from "../../api/hypothesis/useHypothesis";
import PageTitle from "../../components/PageTitle";
import experimentIcon from "../../assets/icons/experiment-icon.svg";
import { getExperimentStageLabel } from "./stageLabels";
import nicheIcon from "../../assets/icons/niche-icon.svg";
import hypothesisIcon from "../../assets/icons/hypothesis-icon.svg";
import CriativosTab from "./CriativosTab";
import SampleEmailsTab from "./SampleEmailsTab";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import * as Tabs from "@radix-ui/react-tabs";
import { useFacebookConfigurationStatus } from "../../api/useFacebookConfigurationStatus";
import { useJourneyTemplate } from "../../api/journey/useJourneyTemplate";
import { useExperimentJourneyAssignments } from "../../api/experiment/useExperimentJourneyAssignments";
import { useRebuildExperimentJourney } from "../../api/experiment/useRebuildExperimentJourney";
import { useExperimentFacebookCampaigns } from "../../api/experiment/useExperimentFacebookCampaigns";
import { useExperimentReadiness } from "../../api/experiment/useExperimentReadiness";
import {
  useExperimentCampaignReset,
  useExperimentCampaignResetPreview,
  type ExperimentCampaignResetSummary,
} from "../../api/experiment/useExperimentCampaignReset";
import type { JourneyAssignment, JourneyStep } from "../../api/journey/types";
import LeadPortalFlowTab from "./LeadPortalFlowTab";
import TargetingTab from "./TargetingTab";
import ExperimentFunnelTab from "./ExperimentFunnelTab";
import ExperimentReportPanel from "./ExperimentReportPanel";
import ExperimentLearningPanel from "./ExperimentLearningPanel";
import ExperimentContentGenerationTab from "./ExperimentContentGenerationTab";
import LandingTab from "./LandingTab";
import CollapsibleJsonViewer from "../../components/CollapsibleJsonViewer";
import { useExperimentAdSetWorkflow } from "../../api/experiment/useExperimentAdSetWorkflow";
import { useExperimentFacebookRelease } from "../../api/experiment/useExperimentFacebookRelease";
import {
  useGeraLandingStageExecutionDetail,
  useGeraLandingStageExecutions,
  type GeraLandingStageExecutionItem,
} from "../../api/experiment/useGeraLandingStageExecutions";

type ChecklistItem = {
  id: string;
  title: string;
  isMet: boolean;
  hint?: string;
  isLoading?: boolean;
  action?: () => void;
  actionLabel?: string;
  actionDisabled?: boolean;
  actionLoading?: boolean;
};

type PipelineContentCard = {
  key: string;
  title: string;
  description: string;
  rawValue?: string | null;
};

const formatPipelineJson = (rawValue?: string | null) => {
  if (!rawValue || rawValue.trim().length === 0) {
    return null;
  }

  const trimmed = rawValue.trim();
  try {
    return JSON.stringify(JSON.parse(trimmed), null, 2);
  } catch {
    return trimmed;
  }
};

async function copyToClipboard(content: string) {
  if (typeof navigator !== "undefined" && navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(content);
    return;
  }

  if (typeof document === "undefined") {
    throw new Error("Clipboard indisponível neste ambiente.");
  }

  const textarea = document.createElement("textarea");
  textarea.value = content;
  textarea.setAttribute("readonly", "");
  textarea.style.position = "fixed";
  textarea.style.top = "-1000px";
  textarea.style.left = "-1000px";
  document.body.appendChild(textarea);
  textarea.focus();
  textarea.select();

  const copied = document.execCommand("copy");
  document.body.removeChild(textarea);

  if (!copied) {
    throw new Error("Falha ao copiar conteúdo.");
  }
}

export default function ExperimentDetailPage() {
  const { id } = useParams();
  const expId = id as string;
  const navigate = useNavigate();
  const { data, isLoading } = useExperiment(expId);
  const {
    data: diagnostics,
    isLoading: isLoadingDiagnostics,
    dataUpdatedAt: diagnosticsUpdatedAt,
  } = useExperimentDiagnostics(expId);
  const { data: niche } = useNiche(data?.nicheId ?? 0);
  const { data: hyp } = useHypothesis(
    data ? String(data.nicheId) : undefined,
    data ? String(data.hypothesisId) : undefined,
  );
  const { data: presets } = useMetricPresets();
  const [tab, setTab] = useState("overview");
  const tabsSectionRef = useRef<HTMLDivElement | null>(null);
  const [journeyError, setJourneyError] = useState<string | null>(null);
  const { data: facebookConfig, isLoading: isLoadingFacebookConfig } =
    useFacebookConfigurationStatus();
  const { data: journeyAssignments, isLoading: isLoadingJourneyAssignments } =
    useExperimentJourneyAssignments(expId);
  const { data: template } = useJourneyTemplate(
    data?.journeyTemplateId ?? undefined,
  );
  const rebuildJourney = useRebuildExperimentJourney(expId);
  const { data: adSetWorkflow, isLoading: isLoadingAdSetWorkflow } =
    useExperimentAdSetWorkflow(expId);
  const { data: facebookCampaigns, isLoading: isLoadingFacebookCampaigns } =
    useExperimentFacebookCampaigns(expId);
  const [isResetModalOpen, setIsResetModalOpen] = useState(false);
  const [copiedCardKey, setCopiedCardKey] = useState<string | null>(null);
  const [copyingCardKey, setCopyingCardKey] = useState<string | null>(null);
  const [isStartingWireframe, setIsStartingWireframe] = useState(false);
  const [optimisticWireframeExecution, setOptimisticWireframeExecution] =
    useState<GeraLandingStageExecutionItem | null>(null);
  const hadRunningGeraLandingExecutionRef = useRef(false);
  const {
    data: pendingGeraLandingExecutions,
    isLoading: isLoadingPendingGeraLandingExecutions,
    refetch: refetchPendingGeraLandingExecutions,
  } = useGeraLandingStageExecutions(expId, "landing-page-wireframe", false);
  const {
    data: completedGeraLandingExecutions,
    isLoading: isLoadingCompletedGeraLandingExecutions,
    refetch: refetchCompletedGeraLandingExecutions,
  } = useGeraLandingStageExecutions(expId, "landing-page-wireframe", true);
  const { data: readinessSummary, isLoading: isLoadingReadiness } =
    useExperimentReadiness(expId);
  const {
    data: resetPreviewData,
    isFetching: isFetchingResetPreview,
    isError: isResetPreviewError,
    error: resetPreviewError,
    refetch: refetchResetPreview,
  } = useExperimentCampaignResetPreview(expId);
  const resetCampaigns = useExperimentCampaignReset(expId);
  const releaseExperiment = useExperimentFacebookRelease(expId);
  const hasCompleteTargeting = readinessSummary?.hasCompleteTargeting ?? false;
  const pipelineContentCards = useMemo<PipelineContentCard[]>(
    () => [
      {
        key: "campaign-angle",
        title: "Etapa 1 · Campaign Angle",
        description: "Conteúdo bruto salvo na coluna campaign_angle.",
        rawValue: data?.campaignAngle,
      },
      {
        key: "ad-copy",
        title: "Etapa 2 · Ad Copy",
        description: "Conteúdo bruto salvo na coluna ad_copy.",
        rawValue: data?.adCopy,
      },
      {
        key: "wireframe",
        title: "Etapa 3 · Landing Wireframe",
        description: "Conteúdo bruto salvo na coluna landing_page_wireframe.",
        rawValue: data?.landingPageWireframe,
      },
      {
        key: "copy",
        title: "Etapa 4 · Landing Copy",
        description: "Conteúdo bruto salvo na coluna landing_page_copy.",
        rawValue: data?.landingPageCopy,
      },
      {
        key: "image-planning",
        title: "Etapa 5 · Planejamento de Imagens",
        description: "Conteúdo bruto salvo na coluna landing_page_image_planning.",
        rawValue: data?.landingPageImagePlanning,
      },
      {
        key: "landing-html",
        title: "Etapa 6 · Landing HTML",
        description: "Conteúdo bruto salvo na coluna landing_page_html.",
        rawValue: data?.landingPageHtml,
      },
    ],
    [
      data?.adCopy,
      data?.campaignAngle,
      data?.landingPageCopy,
      data?.landingPageHtml,
      data?.landingPageImagePlanning,
      data?.landingPageWireframe,
    ],
  );
  useBreadcrumbs([
    {
      label: niche?.name || "...",
      to: `/niches/${data?.nicheId}`,
      icon: nicheIcon,
    },
    {
      label: hyp?.title || "...",
      to: `/niches/${data?.nicheId}/hypotheses/${data?.hypothesisId}`,
      icon: hypothesisIcon,
    },
    { label: data?.name || "...", icon: experimentIcon },
  ]);
  const templateSteps = template?.steps ?? [];
  const hasEmailSteps = templateSteps.some(
    (step) => step.stimulusType === "EMAIL",
  );

  useEffect(() => {
    if (tab === "instant-form") {
      setTab("overview");
    }
    if (tab === "emails" && !hasEmailSteps) {
      setTab("overview");
    }
  }, [tab, hasEmailSteps]);

  const assignmentsWithSteps = useMemo(() => {
    const assignments = journeyAssignments?.assignments ?? [];
    if (assignments.length === 0) {
      return [] as { assignment: JourneyAssignment; step?: JourneyStep }[];
    }
    const stepIndex = new Map<number, JourneyStep>(
      templateSteps.map((step) => [step.id, step]),
    );
    const pairs = assignments.map((assignment) => ({
      assignment,
      step: assignment.nextStepId
        ? stepIndex.get(assignment.nextStepId)
        : undefined,
    }));
    pairs.sort((a, b) => {
      const posA = a.step?.position ?? Number.MAX_SAFE_INTEGER;
      const posB = b.step?.position ?? Number.MAX_SAFE_INTEGER;
      if (posA !== posB) return posA - posB;
      return a.assignment.id - b.assignment.id;
    });
    return pairs;
  }, [journeyAssignments?.assignments, templateSteps]);

  const openResetModal = () => setIsResetModalOpen(true);

  useEffect(() => {
    if (!isResetModalOpen) return;
    void refetchResetPreview();
  }, [isResetModalOpen, refetchResetPreview]);

  const closeResetModal = () => {
    if (resetCampaigns.isPending) return;
    setIsResetModalOpen(false);
  };

  const handleConfirmReset = async () => {
    try {
      const summary = await resetCampaigns.mutateAsync();
      if (summary.campaigns > 0) {
        toast.success(
          `Reset concluído: ${summary.campaigns} campanha(s), ${summary.adSets} conjunto(s), ${summary.ads} anúncio(s) e ${summary.creatives} criativo(s) foram removidos.`,
        );
      } else {
        toast.info("Não havia campanhas pendentes para remover.");
      }
      setIsResetModalOpen(false);
    } catch (error) {
      const message = axios.isAxiosError(error)
        ? (error.response?.data?.message ??
          error.response?.data?.detail ??
          "Não foi possível resetar as campanhas pendentes.")
        : "Não foi possível resetar as campanhas pendentes.";
      toast.error(message);
    }
  };
  const handleFacebookRelease = async () => {
    try {
      await releaseExperiment.mutateAsync();
      toast.success(
        "Experimento liberado para o Facebook Ads Worker. Funil reiniciado.",
      );
    } catch (error) {
      const message = axios.isAxiosError(error)
        ? (error.response?.data?.message ??
          error.response?.data?.detail ??
          "Não foi possível liberar o experimento para o Facebook.")
        : "Não foi possível liberar o experimento para o Facebook.";
      toast.error(message);
    }
  };

  const handleStartWireframe = async () => {
    try {
      setIsStartingWireframe(true);
      const { data: startResponse } = await axios.post<{
        idJob: string;
        status: string;
      }>(`/api/experiments/${expId}/geralanding/wireframe/start`);
      const localExecutionRequestedAt = new Date().toISOString();
      setOptimisticWireframeExecution({
        idJob: startResponse.idJob,
        status: startResponse.status,
        executionRequestedAt: localExecutionRequestedAt,
      });
      toast.success(
        `Solicitação registrada. Código: ${startResponse.idJob} | Status: ${startResponse.status}`,
      );
      await Promise.all([
        refetchPendingGeraLandingExecutions(),
        refetchCompletedGeraLandingExecutions(),
      ]);
    } catch (error) {
      const message = axios.isAxiosError(error)
        ? (error.response?.data?.message ??
          error.response?.data?.detail ??
          "Não foi possível iniciar o WireFrame.")
        : "Não foi possível iniciar o WireFrame.";
      toast.error(message);
    } finally {
      setIsStartingWireframe(false);
    }
  };

  const isRunningExecution = (status?: string | null) => {
    const normalizedStatus = (status ?? "").trim().toUpperCase();
    return ["AGUARDANDO_RETORNO_OPENAI", "EM_PROCESSAMENTO", "PROCESSING", "RUNNING", "IN_PROGRESS", "PENDING", "INICIADO", "STARTED"].includes(
      normalizedStatus,
    );
  };
  const isCompletedExecution = (status?: string | null) => {
    const normalizedStatus = (status ?? "").trim().toUpperCase();
    return ["CONCLUIDO", "CONCLUÍDO", "COMPLETED", "SUCCESS", "SUCCEEDED", "DONE"].includes(
      normalizedStatus,
    );
  };
  const mergedPendingGeraLandingExecutions = useMemo(() => {
    if (!optimisticWireframeExecution) {
      return pendingGeraLandingExecutions ?? [];
    }
    const alreadyPresent = (pendingGeraLandingExecutions ?? []).some(
      (execution) => execution.idJob === optimisticWireframeExecution.idJob,
    );
    if (alreadyPresent) {
      return pendingGeraLandingExecutions ?? [];
    }
    return [optimisticWireframeExecution, ...(pendingGeraLandingExecutions ?? [])];
  }, [optimisticWireframeExecution, pendingGeraLandingExecutions]);

  useEffect(() => {
    if (!optimisticWireframeExecution) {
      return;
    }
    const persistedExecution = (pendingGeraLandingExecutions ?? []).some(
      (execution) => execution.idJob === optimisticWireframeExecution.idJob,
    );
    if (persistedExecution) {
      setOptimisticWireframeExecution(null);
    }
  }, [optimisticWireframeExecution, pendingGeraLandingExecutions]);

  const hasRunningGeraLandingExecution = mergedPendingGeraLandingExecutions.some(
    (execution) => isRunningExecution(execution.status),
  );
  const runningGeraLandingExecutions = useMemo(
    () =>
      mergedPendingGeraLandingExecutions.filter((execution) =>
        isRunningExecution(execution.status),
      ),
    [mergedPendingGeraLandingExecutions],
  );
  const hasFailedExecution = (status?: string | null) => {
    const normalizedStatus = (status ?? "").trim().toUpperCase();
    return ["FALHA", "FAILED", "ERROR", "ERRO"].includes(normalizedStatus);
  };
  const historyGeraLandingExecutions = useMemo(() => {
    const completedHistory = (completedGeraLandingExecutions ?? []).filter(
      (execution) => isCompletedExecution(execution.status) || hasFailedExecution(execution.status),
    );
    const failedFromPending = (pendingGeraLandingExecutions ?? []).filter((execution) =>
      hasFailedExecution(execution.status),
    );

    return [...failedFromPending, ...completedHistory].sort((leftExecution, rightExecution) => {
      const leftTimestamp = Date.parse(leftExecution.executionRequestedAt ?? "");
      const rightTimestamp = Date.parse(rightExecution.executionRequestedAt ?? "");
      const normalizedLeftTimestamp = Number.isNaN(leftTimestamp) ? 0 : leftTimestamp;
      const normalizedRightTimestamp = Number.isNaN(rightTimestamp) ? 0 : rightTimestamp;

      return normalizedRightTimestamp - normalizedLeftTimestamp;
    });
  }, [completedGeraLandingExecutions, pendingGeraLandingExecutions]);
  const runningGeraLandingJobId = mergedPendingGeraLandingExecutions.find((execution) =>
    isRunningExecution(execution.status),
  )?.idJob;

  const formatCurrencyUsd = (value?: number | null) =>
    value != null
      ? new Intl.NumberFormat("en-US", {
          style: "currency",
          currency: "USD",
        }).format(value)
      : "—";

  const resolveExecutionCostUsd = (execution: GeraLandingStageExecutionItem) => {
    const candidateValues = [
      execution.costUsd,
      (execution as GeraLandingStageExecutionItem & { totalCostUsd?: number | string | null })
        .totalCostUsd,
      (execution as GeraLandingStageExecutionItem & { cost?: number | string | null }).cost,
      (execution as GeraLandingStageExecutionItem & { totalCost?: number | string | null })
        .totalCost,
    ];

    for (const candidate of candidateValues) {
      if (candidate == null) continue;
      if (typeof candidate === "number" && Number.isFinite(candidate)) return candidate;
      if (typeof candidate === "string") {
        const normalizedCandidate = candidate.replace(",", ".").trim();
        if (!normalizedCandidate) continue;
        const parsedCandidate = Number(normalizedCandidate);
        if (Number.isFinite(parsedCandidate)) return parsedCandidate;
      }
    }

    return null;
  };

  const totalCompletedGeraLandingCostUsd = historyGeraLandingExecutions.reduce(
    (sum, execution) => sum + (resolveExecutionCostUsd(execution) ?? 0),
    0,
  );
  const { data: runningGeraLandingJobDetail } = useGeraLandingStageExecutionDetail(
    expId,
    runningGeraLandingJobId,
    {
      enabled: Boolean(runningGeraLandingJobId),
      refetchInterval: 10000,
    },
  );

  useEffect(() => {
    if (!runningGeraLandingJobId || !runningGeraLandingJobDetail) return;
    if (isCompletedExecution(runningGeraLandingJobDetail.status)) {
      void Promise.all([
        refetchPendingGeraLandingExecutions(),
        refetchCompletedGeraLandingExecutions(),
      ]);
    }
  }, [
    runningGeraLandingJobDetail,
    runningGeraLandingJobId,
    refetchPendingGeraLandingExecutions,
    refetchCompletedGeraLandingExecutions,
  ]);

  useEffect(() => {
    if (hasRunningGeraLandingExecution) {
      hadRunningGeraLandingExecutionRef.current = true;
      return;
    }

    if (!hadRunningGeraLandingExecutionRef.current) return;

    hadRunningGeraLandingExecutionRef.current = false;
    void refetchCompletedGeraLandingExecutions();
  }, [hasRunningGeraLandingExecution, refetchCompletedGeraLandingExecutions]);
  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p>Não encontrado</p>;
  const preset = presets?.find((p) => p.id === data.metricPresetId);
  const resetPreviewSummary: ExperimentCampaignResetSummary =
    resetPreviewData ?? { campaigns: 0, adSets: 0, ads: 0, creatives: 0 };
  const totalItemsToReset =
    resetPreviewSummary.campaigns +
    resetPreviewSummary.adSets +
    resetPreviewSummary.ads +
    resetPreviewSummary.creatives;
  const hasItemsToReset = totalItemsToReset > 0;
  const previewErrorMessage = isResetPreviewError
    ? resetPreviewError && resetPreviewError instanceof Error
      ? resetPreviewError.message
      : "Não foi possível carregar a prévia do reset."
    : null;
  const confirmResetDisabled =
    isFetchingResetPreview ||
    !hasItemsToReset ||
    Boolean(previewErrorMessage) ||
    resetCampaigns.isPending;
  const readinessIssues = readinessSummary?.issues ?? [];
  const hasReadinessIssues = readinessIssues.length > 0;
  const formatCurrency = (n?: number | null) =>
    n != null
      ? new Intl.NumberFormat("pt-BR", {
          style: "currency",
          currency: "BRL",
        }).format(n)
      : "—";
  const formatPercent = (n?: number | null) => (n != null ? `${n}%` : "—");
  const formatDateTimeValue = (value?: string | null) => {
    if (!value) return "—";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }
    return date.toLocaleString("pt-BR", {
      dateStyle: "short",
      timeStyle: "short",
    });
  };
  const diagnosticsTimestamp =
    diagnosticsUpdatedAt > 0
      ? formatDateTimeValue(new Date(diagnosticsUpdatedAt).toISOString())
      : null;
  const baseKpi = data.kpiTarget ?? data.kpiTargetCpl;
  const stopLossFactor = preset?.stopLossFactor;
  const stopLossCpl =
    data.stopLossCpl ??
    (baseKpi != null && stopLossFactor != null
      ? baseKpi * stopLossFactor
      : null);
  const hasConfiguredFacebookPage = facebookConfig?.hasConfiguredPages ?? false;
  const experimentPage = data.facebookPage;
  const hasExperimentPage = Boolean(experimentPage?.pageId);
  const instagramAccount = data.instagramAccount;
  const hasInstagramAccount = Boolean(instagramAccount);
  const facebookWorker = facebookConfig?.worker;
  const hasFacebookWorkerAccount = facebookWorker?.hasAccount ?? false;
  const isFacebookWorkerReady = facebookWorker?.ready ?? false;
  const facebookAccountLabel =
    facebookWorker?.accountName ?? facebookWorker?.accountId ?? null;
  const hasLeadPortalFlow =
    readinessSummary?.hasLeadPortalFlow ??
    Boolean(data.leadPortalFlowId ?? data.leadPortalFlowName);
  const hasFacebookPixelRegistered = Boolean(niche?.facebookPixelId);
  const hasCreativesReady =
    readinessSummary?.hasCreatives ?? data.creativeApproved;
  const readinessCreativeCount = readinessSummary?.creativeCount ?? 0;
  const hasDailyBudget = data.dailyBudget != null && data.dailyBudget > 0;
  const leadPortalFlowLabel =
    data.leadPortalFlowName ??
    data.leadPortalFlowSlug ??
    (data.leadPortalFlowId ? `#${data.leadPortalFlowId}` : null);

  const blockingChecklist: ChecklistItem[] = [
    {
      id: "creatives",
      title: "Criativos aprovados",
      isMet: hasCreativesReady,
      isLoading: isLoadingReadiness,
      hint: isLoadingReadiness
        ? "Verificando criativos aprovados..."
        : hasCreativesReady
          ? readinessCreativeCount > 0
            ? `${readinessCreativeCount} criativo${readinessCreativeCount === 1 ? "" : "s"} pronto${readinessCreativeCount === 1 ? "" : "s"} para o Meta.`
            : "Os criativos já estão aprovados e prontos para o Meta."
          : "Revise e aprove pelo menos um criativo na aba Criativos.",
      action: hasCreativesReady ? undefined : () => setTab("creatives"),
      actionLabel: hasCreativesReady ? undefined : "Ir para Criativos",
    },
    {
      id: "lead-portal-flow",
      title: "Fluxo do Portal do Lead",
      isMet: hasLeadPortalFlow,
      isLoading: isLoadingReadiness,
      hint: hasLeadPortalFlow
        ? leadPortalFlowLabel
          ? `Fluxo ${leadPortalFlowLabel} vinculado ao experimento.`
          : "Já existe um fluxo do Portal do Lead vinculado."
        : "Solicite ou vincule um fluxo aprovado na aba Portal do Lead.",
      action: hasLeadPortalFlow ? undefined : () => setTab("lead-portal"),
      actionLabel: hasLeadPortalFlow ? undefined : "Ir para Portal do Lead",
    },
    {
      id: "targeting",
      title: "Público completo",
      isMet: hasCompleteTargeting,
      isLoading: isLoadingReadiness,
      hint: isLoadingReadiness
        ? "Verificando elementos aprovados..."
        : hasCompleteTargeting
          ? "Público salvo na aba Segmentação."
          : "Selecione e salve pelo menos uma segmentação com ID da Meta na aba Segmentação.",
      action: hasCompleteTargeting ? undefined : () => setTab("targeting"),
      actionLabel: hasCompleteTargeting ? undefined : "Ir para Segmentação",
    },
  ];

  const isReadyForFacebook = blockingChecklist.every((c) => c.isMet);
  const releaseInProgress = releaseExperiment.isPending;
  const lastReleaseAt = data.facebookReleaseRequestedAt;
  const lastReleaseLabel = lastReleaseAt
    ? formatDateTimeValue(lastReleaseAt)
    : null;
  const canReleaseExperiment =
    isReadyForFacebook && data.platform === "FACEBOOK";
  const releaseButtonDisabled =
    releaseInProgress || !canReleaseExperiment || isLoadingReadiness;
  const openLandingActions = () => {
    setTab("landing");
    window.requestAnimationFrame(() => {
      tabsSectionRef.current?.scrollIntoView({
        behavior: "smooth",
        block: "start",
      });
    });
  };

  const configurationChecklist: ChecklistItem[] = [
    {
      id: "facebook-account",
      title: "Conta do Facebook Ads conectada",
      isMet: isFacebookWorkerReady,
      isLoading: isLoadingFacebookConfig,
      hint: isLoadingFacebookConfig
        ? "Verificando credenciais conectadas..."
        : isFacebookWorkerReady
          ? facebookAccountLabel
            ? `Conta ${facebookAccountLabel} pronta para publicar.`
            : "Conta conectada e pronta para publicar."
          : hasFacebookWorkerAccount
            ? "Existe uma conta conectada, mas o worker precisa ser reautorizado. Abra Contas do Facebook."
            : "Nenhuma conta conectada. Acesse Contas do Facebook e conecte a conta do Meta Ads.",
      action: isFacebookWorkerReady
        ? undefined
        : () => navigate("/accounts/facebook"),
      actionLabel: isFacebookWorkerReady
        ? undefined
        : "Abrir Contas do Facebook",
    },
    {
      id: "experiment-page",
      title: "Página do Facebook definida",
      isMet: hasExperimentPage,
      hint: hasExperimentPage
        ? `Este experimento usa a página ${experimentPage?.name ?? experimentPage?.pageId}.`
        : hasConfiguredFacebookPage
          ? "Escolha a página do experimento na tela de edição."
          : "Nenhuma página conectada ao hub. Configure nas Contas do Facebook antes de editar o experimento.",
      action: hasExperimentPage
        ? undefined
        : hasConfiguredFacebookPage
          ? () => navigate(`/experiments/${expId}/edit`)
          : () => navigate("/accounts/facebook"),
      actionLabel: hasExperimentPage
        ? undefined
        : hasConfiguredFacebookPage
          ? "Editar experimento"
          : "Configurar páginas",
    },
    {
      id: "instagram-account",
      title: "Conta do Instagram vinculada",
      isMet: hasInstagramAccount,
      hint: hasInstagramAccount
        ? `Este experimento usa a conta ${instagramAccount?.handle}.`
        : "Associe uma conta do Instagram ao experimento para liberar as campanhas.",
      action: hasInstagramAccount
        ? undefined
        : () => navigate(`/experiments/${expId}/edit`),
      actionLabel: hasInstagramAccount ? undefined : "Editar experimento",
    },
    {
      id: "daily-budget",
      title: "Valor diário definido",
      isMet: hasDailyBudget,
      hint: hasDailyBudget
        ? `Investimento diário de ${formatCurrency(data.dailyBudget)}.`
        : "Defina o orçamento diário para orientar o worker de mídia.",
      action: hasDailyBudget
        ? undefined
        : () => navigate(`/experiments/${expId}/edit`),
      actionLabel: hasDailyBudget ? undefined : "Editar experimento",
    },
  ];

  const operationalChecklist: ChecklistItem[] = [
    {
      id: "landing-destination",
      title: "Landing aprovada como destino da campanha",
      isMet: Boolean(data.followUpActionUrl),
      hint: data.followUpActionUrl
        ? `URL de destino ativa: ${data.followUpActionUrl}.`
        : "Aprove uma landing na aba Landing para definir a URL de destino da campanha.",
      action: data.followUpActionUrl ? undefined : openLandingActions,
      actionLabel: data.followUpActionUrl ? undefined : "Ir para Landing",
    },
    {
      id: "platform",
      title: "Plataforma configurada para Facebook Ads",
      isMet: data.platform === "FACEBOOK",
      hint:
        data.platform === "FACEBOOK"
          ? "Este experimento já usa a plataforma do Facebook."
          : `Plataforma atual: ${data.platform}. Ajuste para Facebook Ads para liberar a campanha.`,
    },
    {
      id: "status",
      title: "Status marcado como Planejado",
      isMet: data.status === "PLANNED",
      hint:
        data.status === "PLANNED"
          ? lastReleaseLabel
            ? `Liberado para o worker em ${lastReleaseLabel}.`
            : "Status Planejado. Use o botão acima para liberar novamente se precisar reiniciar o funil."
          : "Use o botão de liberação para marcar como Planejado e disparar a publicação do worker.",
      action: data.status === "PLANNED" ? undefined : handleFacebookRelease,
      actionLabel: data.status === "PLANNED" ? undefined : "Liberar agora",
      actionDisabled:
        data.status === "PLANNED" ? undefined : releaseButtonDisabled,
      actionLoading: data.status === "PLANNED" ? undefined : releaseInProgress,
    },
    {
      id: "facebook-pixel",
      title: "Pixel do nicho",
      isMet: hasFacebookPixelRegistered,
      hint: hasFacebookPixelRegistered
        ? niche?.facebookPixelId
          ? `Pixel ${niche.facebookPixelId} disponível para este nicho.`
          : "Pixel disponível."
        : "Abra o nicho para acompanhar quando o worker gerar o pixel automaticamente.",
      action: hasFacebookPixelRegistered
        ? undefined
        : () => navigate(`/niches/${data.nicheId}#niche-facebook-pixel`),
      actionLabel: hasFacebookPixelRegistered ? undefined : "Ver pixel do nicho",
    },
  ];
  const checklistGroups = [
    {
      id: "blocking",
      title: "Bloqueios de publicação",
      description:
        "Itens que impedem a publicação automática. Mesma lógica do diagnóstico cinza.",
      items: blockingChecklist,
    },
    {
      id: "configuration",
      title: "Configurações do experimento",
      description:
        "Verificações previstas no documento de publicação (conta, página, Instagram e orçamento).",
      items: configurationChecklist,
    },
    {
      id: "operational",
      title: "Fluxo operacional do Meta",
      description:
        "Ajustes que ajudam o worker a executar o plano end-to-end no Meta Ads.",
      items: operationalChecklist,
    },
  ].filter((group) => group.items.length > 0);
  const diagnosticsVariant: Record<string, string> = {
    ERROR: "danger",
    WARNING: "warning",
    INFO: "secondary",
  };

  const workflowStatusVariant: Record<string, string> = {
    NOT_STARTED: "secondary",
    RUNNING: "info",
    COMPLETED: "success",
    FAILED: "danger",
  };
  const workflowStatusLabel: Record<string, string> = {
    NOT_STARTED: "Não iniciado",
    RUNNING: "Em andamento",
    COMPLETED: "Concluído",
    FAILED: "Com erro",
  };

  const selectedEmailOverview = data.selectedSampleEmailSubject ? (
    <div className="d-flex flex-column">
      <span>{data.selectedSampleEmailSubject}</span>
      {data.selectedSampleEmailUpdatedAt ? (
        <span className="text-muted small">
          Atualizado em {formatDateTimeValue(data.selectedSampleEmailUpdatedAt)}
        </span>
      ) : null}
      <button
        type="button"
        className="btn btn-link btn-sm p-0 mt-1 align-self-start"
        onClick={() => setTab("sample-emails")}
      >
        Ver e-mails
      </button>
    </div>
  ) : (
    <button
      type="button"
      className="btn btn-link btn-sm p-0"
      onClick={() => setTab("sample-emails")}
    >
      Escolher e-mail
    </button>
  );

  const rows = [
    {
      label: "Nicho",
      value: <Link to={`/niches/${data.nicheId}/edit`}>{niche?.name}</Link>,
    },
    {
      label: "Hipótese",
      value: (
        <Link to={`/niches/${data.nicheId}/hypotheses/${data.hypothesisId}`}>
          {hyp?.title || data.hypothesis}
        </Link>
      ),
    },
    {
      label: "Etapa priorizada",
      value: getExperimentStageLabel(data.stage as ExperimentStage),
    },
    {
      label: "Variável principal",
      value: data.primaryVariable || "—",
    },
    {
      label: "Métrica principal",
      value: data.primaryMetric || "—",
    },
    {
      label: "Página do Facebook",
      value: experimentPage
        ? `${experimentPage.name} (${experimentPage.pageId})`
        : "—",
    },
    {
      label: "Conta do Instagram",
      value: instagramAccount
        ? `${instagramAccount.name} (${instagramAccount.handle})`
        : "—",
    },
    ...(data.journeyTemplateName
      ? [
          {
            label: "Template de Jornada",
            value: data.journeyTemplateId ? (
              <Link
                to={`/journey-templates/${data.journeyTemplateId}`}
                className="btn btn-link p-0 align-baseline"
              >
                {data.journeyTemplateName}
              </Link>
            ) : (
              data.journeyTemplateName
            ),
          },
        ]
      : []),
    { label: "Preset de Métricas", value: preset?.name || "—" },
    {
      label: "Sample size",
      value: data.sampleSize ?? preset?.sampleSize ?? "—",
    },
    { label: "Criativos a gerar", value: data.creativesToGenerate ?? "—" },
    { label: "E-mails a gerar", value: data.emailsToGenerate ?? "—" },
    {
      label: "E-mails de amostra a gerar",
      value: data.sampleEmailsToGenerate ?? "—",
    },
    {
      label: "E-mail de amostra selecionado",
      value: selectedEmailOverview,
    },
    {
      label: "Fluxo de portal do lead",
      value: data.leadPortalFlowName ? (
        <div className="d-flex flex-column">
          <span>{data.leadPortalFlowName}</span>
          {data.leadPortalFlowSlug ? (
            <span className="text-muted small">
              Slug: {data.leadPortalFlowSlug}
            </span>
          ) : null}
        </div>
      ) : (
        "—"
      ),
    },
    {
      label: "Modelo de imagem",
      value: data.imageModelName
        ? `${data.imageModelName}${data.imageModelQualityName ? " · " + data.imageModelQualityName : ""}`
        : "—",
    },
    {
      label: "MDE (p.p.)",
      value: data.mdePercent ?? preset?.defaultMdePp ?? "—",
    },
    {
      label: "Stop-loss factor",
      value: preset?.stopLossFactor ? `${preset.stopLossFactor}×` : "—",
    },
    {
      label: "CPL-meta",
      value: formatCurrency(data.kpiTarget ?? data.kpiTargetCpl),
    },
    { label: "Custo", value: formatCurrency(data.cost) },
    { label: "Despesa", value: formatCurrency(data.expense) },
    { label: "Stop-loss CPL", value: formatCurrency(stopLossCpl) },
    { label: "Baseline CVR", value: formatPercent(data.baselineCvr) },
    { label: "Target CVR", value: formatPercent(data.targetCvr) },
    { label: "Plataforma", value: data.platform },
    { label: "Início", value: data.startDate },
    { label: "Término", value: data.endDate },
  ];
  const handleCreateJourney = async () => {
    setJourneyError(null);
    try {
      await rebuildJourney.mutateAsync();
    } catch (error) {
      console.error("Failed to rebuild journey assignments", error);
      setJourneyError("Não foi possível criar a jornada. Tente novamente.");
    }
  };

  const isJourneyActionDisabled =
    rebuildJourney.isPending || !data?.journeyTemplateId || isLoading;
  return (
    <div>
      <div className="d-flex justify-content-between align-items-start">
        <div>
          <PageTitle icon={experimentIcon}>{data.name}</PageTitle>
          <p className="text-muted mb-0">{data.hypothesis}</p>
        </div>
        <div className="d-flex align-items-center">
          <button
            type="button"
            className="btn btn-primary me-2"
            onClick={handleCreateJourney}
            disabled={isJourneyActionDisabled}
          >
            {rebuildJourney.isPending ? (
              <>
                <span
                  className="spinner-border spinner-border-sm me-2"
                  role="status"
                />
                Criando jornada...
              </>
            ) : (
              "Criar jornada"
            )}
          </button>
          <Link to="edit" className="btn btn-outline-secondary me-2">
            Editar
          </Link>
          <Link to="adset-workflow" className="btn btn-outline-primary me-2">
            Playbook de Ad Sets
          </Link>
          <Link to="facebook-api-logs" className="btn btn-outline-info me-2">
            Chamadas Meta
          </Link>
          <Link to="pipeline-jobs" className="btn btn-outline-dark me-2">
            Jobs do pipeline
          </Link>
          <button
            type="button"
            className="btn btn-outline-danger me-2"
            onClick={openResetModal}
            disabled={resetCampaigns.isPending}
          >
            {resetCampaigns.isPending ? (
              <>
                <span
                  className="spinner-border spinner-border-sm me-2"
                  role="status"
                  aria-hidden="true"
                />
                Resetando...
              </>
            ) : (
              "Resetar pendências"
            )}
          </button>
          <span className="badge bg-secondary">{data.status}</span>
        </div>
      </div>
      {isLoadingDiagnostics ? (
        <div
          className="alert alert-light d-flex align-items-center gap-2 mt-3"
          role="status"
        >
          <span
            className="spinner-border spinner-border-sm"
            role="status"
            aria-hidden="true"
          />
          <span>Carregando diagnóstico da publicação...</span>
        </div>
      ) : diagnostics ? (
        <div
          className={`alert alert-${diagnosticsVariant[diagnostics.severity] ?? "secondary"} mt-3`}
          role="alert"
        >
          <div className="d-flex justify-content-between gap-3">
            <div>
              <h6 className="alert-heading mb-1">{diagnostics.headline}</h6>
              {diagnosticsTimestamp ? (
                <p className="mb-2 small text-body-secondary">
                  <strong>Data e hora da mensagem:</strong>{" "}
                  {diagnosticsTimestamp}
                </p>
              ) : null}
              <p className="mb-2">{diagnostics.description}</p>
              {diagnostics.failureDetails ? (
                <div className="alert alert-light border small mb-2" role="alert">
                  <div className="fw-semibold mb-1">
                    Último erro retornado pelo worker / Meta Ads
                  </div>
                  <ul className="mb-0 ps-3">
                    <li>
                      <strong>Mensagem:</strong>{" "}
                      {diagnostics.failureDetails.message ??
                        "Sem mensagem detalhada no log."}
                    </li>
                    <li>
                      <strong>Endpoint:</strong>{" "}
                      <code>{diagnostics.failureDetails.endpoint ?? "—"}</code>
                    </li>
                    <li>
                      <strong>Status HTTP:</strong>{" "}
                      {diagnostics.failureDetails.statusCode ?? "—"}
                    </li>
                    <li>
                      <strong>Origem:</strong>{" "}
                      {diagnostics.failureDetails.source ?? "—"}
                    </li>
                    <li>
                      <strong>Horário do erro:</strong>{" "}
                      {diagnostics.failureDetails.occurredAt
                        ? formatDateTimeValue(diagnostics.failureDetails.occurredAt)
                        : "—"}
                    </li>
                  </ul>
                  <Link
                    to="facebook-api-logs"
                    className="btn btn-sm btn-outline-danger mt-2"
                  >
                    Abrir Chamadas Meta com payload completo
                  </Link>
                </div>
              ) : null}
              {diagnostics.resolution ? (
                <p className="mb-2">
                  <strong>O que deve ser corrigido:</strong>{" "}
                  {diagnostics.resolution}
                </p>
              ) : null}
            </div>
            <span
              className={`badge text-bg-${diagnosticsVariant[diagnostics.severity] ?? "secondary"} align-self-start`}
            >
              {diagnostics.severity}
            </span>
          </div>
          {diagnostics.artifacts.length > 0 ? (
            <>
              <p className="mb-2 mt-2 small">
                <strong>Itens com pendência para corrigir:</strong>
              </p>
              <ul className="mb-0 small ps-3">
                {diagnostics.artifacts.map((artifact) => (
                  <li key={`${artifact.type}-${artifact.id}`}>
                    <strong>{artifact.type}</strong> ·{" "}
                    {artifact.name || artifact.id} — ID interno: {artifact.id}
                    {artifact.externalId
                      ? ` · ID Meta: ${artifact.externalId}`
                      : ""}
                  </li>
                ))}
              </ul>
            </>
          ) : null}
        </div>
      ) : null}
      <div className="card border-0 shadow-sm rounded-3 mt-3">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-start">
            <h5 className="card-title mb-0">Campanha de Facebook Ads</h5>
            <span
              className={`badge ${
                isReadyForFacebook ? "text-bg-success" : "text-bg-warning"
              }`}
            >
              {isReadyForFacebook ? "Pronto" : "Pendente"}
            </span>
          </div>
          <p className="card-text mt-2">
            Checklist consolidado das regras de publicação. Ele reflete o
            documento interno e o diagnóstico automático do worker.
          </p>
          {isLoadingReadiness ? (
            <div
              className="alert alert-light d-flex align-items-center gap-2 mt-3"
              role="status"
            >
              <span
                className="spinner-border spinner-border-sm"
                role="status"
                aria-hidden="true"
              />
              <span>Carregando pendências básicas...</span>
            </div>
          ) : hasReadinessIssues ? (
            <div className="alert alert-warning mt-3" role="alert">
              <h6 className="alert-heading mb-2">
                Pendências antes da publicação
              </h6>
              <ul className="mb-0 ps-3">
                {readinessIssues.map((issue, index) => (
                  <li key={`${issue.type}-${index}`} className="mb-2">
                    <strong>{issue.title}</strong>: {issue.description}
                    {issue.recommendation ? (
                      <div className="small text-body-secondary">
                        {issue.recommendation}
                      </div>
                    ) : null}
                  </li>
                ))}
              </ul>
            </div>
          ) : (
            <div className="alert alert-secondary mt-3" role="status">
              Nenhuma inconsistência detectada pelo worker. Este experimento
              pode ser publicado quando você liberar o status e demais
              automações.
            </div>
          )}
          <div className="mt-3 d-flex flex-column flex-lg-row align-items-start gap-3">
            <button
              type="button"
              className="btn btn-primary"
              onClick={handleFacebookRelease}
              disabled={releaseButtonDisabled}
            >
              {releaseInProgress
                ? "Liberando..."
                : "Liberar para Facebook Ads Worker"}
            </button>
            <div className="small text-body-secondary">
              {isReadyForFacebook
                ? "Ao liberar, o status muda para Planejado e o funil de vendas é zerado antes da publicação."
                : "Resolva os bloqueios para habilitar a liberação automática."}
              {lastReleaseLabel ? (
                <div className="mt-1">
                  Última liberação: <strong>{lastReleaseLabel}</strong>
                </div>
              ) : null}
            </div>
          </div>
          {checklistGroups.map((group, index) => (
            <div key={group.id} className={index === 0 ? "mt-3" : "mt-4"}>
              <h6 className="text-uppercase small text-muted mb-1">
                {group.title}
              </h6>
              {group.description ? (
                <p className="text-muted small mb-2">{group.description}</p>
              ) : null}
              <ul className="list-unstyled mb-0 d-flex flex-column gap-2">
                {group.items.map((check) => (
                  <li
                    key={check.id}
                    className="d-flex align-items-start gap-3 p-3 bg-body-tertiary rounded-3"
                  >
                    <span
                      className={`badge flex-shrink-0 ${
                        check.isLoading
                          ? "text-bg-secondary"
                          : check.isMet
                            ? "text-bg-success"
                            : "text-bg-warning"
                      }`}
                    >
                      {check.isLoading
                        ? "Verificando"
                        : check.isMet
                          ? "Pronto"
                          : "Pendente"}
                    </span>
                    <div className="flex-grow-1">
                      <div className="fw-semibold text-body">{check.title}</div>
                      {check.hint ? (
                        <div className="text-muted small mt-1">
                          {check.hint}
                        </div>
                      ) : null}
                      {!check.isMet && check.action && check.actionLabel ? (
                        <button
                          type="button"
                          className="btn btn-link btn-sm p-0 align-baseline mt-2"
                          onClick={check.action}
                          disabled={check.actionDisabled}
                        >
                          {check.actionLoading ? (
                            <span
                              className="spinner-border spinner-border-sm me-2"
                              role="status"
                            />
                          ) : null}
                          {check.actionLabel}
                        </button>
                      ) : null}
                    </div>
                  </li>
                ))}
              </ul>
            </div>
          ))}
          <div className="mt-4">
            <h6 className="text-uppercase small text-muted">
              Execução registrada
            </h6>
            {isLoadingFacebookCampaigns ? (
              <p className="text-muted small mb-0">
                Carregando campanhas publicadas...
              </p>
            ) : !facebookCampaigns?.length ? (
              <p className="text-muted small mb-0">
                Nenhuma publicação registrada para este experimento.
              </p>
            ) : (
              <div className="d-flex flex-column gap-3">
                {facebookCampaigns.map((campaign) => (
                  <div key={campaign.id} className="border rounded-3 p-3">
                    <div className="d-flex justify-content-between flex-wrap gap-2">
                      <div>
                        <div className="fw-semibold">{campaign.name}</div>
                        <div className="text-muted small">
                          Objetivo: {campaign.objective ?? "—"} · Criada em{" "}
                          {formatDateTimeValue(campaign.createdAt)}
                        </div>
                      </div>
                      <span
                        className={`badge text-bg-${campaign.status === "PAUSED" ? "secondary" : "success"}`}
                      >
                        {campaign.status}
                      </span>
                    </div>
                    {campaign.issues?.length ? (
                      <div
                        className="alert alert-warning mt-3 mb-2"
                        role="alert"
                      >
                        <ul className="mb-0 ps-3">
                          {campaign.issues.map((issue) => (
                            <li key={`${campaign.id}-${issue}`}>{issue}</li>
                          ))}
                        </ul>
                      </div>
                    ) : null}
                    {campaign.adSets.length ? (
                      <div className="d-flex flex-column gap-3 mt-3">
                        {campaign.adSets.map((adSet) => (
                          <div
                            key={adSet.id}
                            className="border rounded-3 p-3 bg-body-tertiary"
                          >
                            <div className="d-flex justify-content-between flex-wrap gap-2">
                              <div>
                                <div className="fw-semibold">{adSet.name}</div>
                                <div className="text-muted small">
                                  {adSet.experimentAdSetId
                                    ? `Segmento aprovado #${adSet.experimentAdSetId}`
                                    : "Sem vínculo com segmento aprovado"}
                                  · {adSet.ads.length} anúncio
                                  {adSet.ads.length === 1 ? "" : "s"}
                                </div>
                              </div>
                              <span
                                className={`badge text-bg-${adSet.experimentAdSetId ? "success" : "warning"}`}
                              >
                                {adSet.status}
                              </span>
                            </div>
                            {adSet.issues?.length ? (
                              <ul className="text-warning small mb-0 mt-2 ps-3">
                                {adSet.issues.map((issue) => (
                                  <li key={`${adSet.id}-${issue}`}>{issue}</li>
                                ))}
                              </ul>
                            ) : null}
                            {adSet.ads.length ? (
                              <div className="mt-3 d-flex flex-column gap-2">
                                {adSet.ads.map((ad) => (
                                  <div
                                    key={ad.id}
                                    className="border rounded-3 p-3 bg-white"
                                  >
                                    <div className="d-flex justify-content-between flex-wrap gap-2">
                                      <div>
                                        <div className="fw-semibold">
                                          {ad.name}
                                        </div>
                                        <div className="text-muted small">
                                          ID: {ad.id}
                                        </div>
                                        <div className="text-muted small">
                                          Referência de rastreio:{" "}
                                          {ad.trackingCode ?? "—"}
                                        </div>
                                      </div>
                                      <span
                                        className={`badge text-bg-${ad.status === "PAUSED" ? "secondary" : "success"}`}
                                      >
                                        {ad.status}
                                      </span>
                                    </div>
                                    {ad.funnelStages &&
                                    ad.funnelStages.length ? (
                                      <div className="table-responsive mt-3">
                                        <table className="table table-sm align-middle mb-0">
                                          <thead>
                                            <tr>
                                              <th>Etapa</th>
                                              <th className="text-end">
                                                Conversões
                                              </th>
                                            </tr>
                                          </thead>
                                          <tbody>
                                            {ad.funnelStages.map((stage) => (
                                              <tr
                                                key={`${ad.id}-${stage.stage}`}
                                              >
                                                <td>
                                                  {stage.order}. {stage.label}
                                                </td>
                                                <td className="text-end">
                                                  {stage.totalCount}
                                                </td>
                                              </tr>
                                            ))}
                                          </tbody>
                                        </table>
                                      </div>
                                    ) : (
                                      <div className="text-muted small mt-2">
                                        Nenhuma conversão atribuída a este
                                        anúncio ainda.
                                      </div>
                                    )}
                                  </div>
                                ))}
                              </div>
                            ) : (
                              <div className="small text-muted mt-2">
                                Nenhum anúncio publicado neste conjunto.
                              </div>
                            )}
                          </div>
                        ))}
                      </div>
                    ) : (
                      <p className="text-muted small mb-0 mt-3">
                        Nenhum conjunto de anúncios registrado.
                      </p>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
      <div className="card border-0 shadow-sm rounded-3 mt-3">
        <div className="card-body">
          <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
            <div>
              <h5 className="card-title mb-1">Pipeline de Públicos</h5>
              <p className="text-muted small mb-0">
                Geração automatizada de segmentações validadas com a Meta Ads
              </p>
            </div>
            <div className="d-flex flex-wrap gap-2">
              <Link
                to="adset-workflow"
                className="btn btn-outline-primary btn-sm"
              >
                Ver playbook
              </Link>
              <Link
                to="facebook-api-logs"
                className="btn btn-outline-secondary btn-sm"
              >
                Logs da Graph API
              </Link>
              <Link
                to="pipeline-jobs"
                className="btn btn-outline-dark btn-sm"
              >
                Jobs do pipeline
              </Link>
            </div>
          </div>
          {isLoadingAdSetWorkflow ? (
            <p className="text-muted small mt-3 mb-0">Carregando pipeline...</p>
          ) : adSetWorkflow ? (
            <>
              <div className="d-flex flex-wrap gap-3 align-items-center mt-3">
                <span
                  className={`badge text-bg-${workflowStatusVariant[adSetWorkflow.status ?? "NOT_STARTED"] ?? "secondary"}`}
                >
                  {workflowStatusLabel[adSetWorkflow.status ?? "NOT_STARTED"]}
                </span>
                <div className="small text-muted">
                  Specs prontas:{" "}
                  {adSetWorkflow.specs?.filter(
                    (spec) => spec.reachStatus?.toUpperCase() === "READY",
                  ).length ?? 0}{" "}
                  / {adSetWorkflow.specs?.length ?? 0}
                </div>
                <div className="small text-muted">
                  Última atualização:{" "}
                  {formatDateTimeValue(
                    adSetWorkflow.updatedAt ??
                      adSetWorkflow.completedAt ??
                      adSetWorkflow.createdAt,
                  )}
                </div>
              </div>
              {adSetWorkflow.lastError ? (
                <div className="alert alert-warning mt-3 mb-0" role="alert">
                  <strong>Último erro:</strong> {adSetWorkflow.lastError}
                </div>
              ) : null}
            </>
          ) : (
            <p className="text-muted small mt-3 mb-0">
              Este experimento ainda não iniciou o playbook de públicos. Use o
              botão acima para configurar.
            </p>
          )}
        </div>
      </div>

      {data.journeyTemplateId ? (
        <div className="card border-0 shadow-sm rounded-3 mt-3">
          <div className="card-body">
            <div className="d-flex justify-content-between align-items-start">
              <div>
                <h5 className="card-title mb-0">Jornada</h5>
                <p className="text-muted mb-0">
                  Template associado: {data.journeyTemplateName ?? "—"}
                </p>
              </div>
              {journeyAssignments?.journeyId ? (
                <span className="badge text-bg-secondary">
                  Jornada #{journeyAssignments.journeyId}
                </span>
              ) : null}
            </div>
            {journeyError ? (
              <div className="alert alert-danger mt-3" role="alert">
                {journeyError}
              </div>
            ) : null}
            {isLoadingJourneyAssignments ? (
              <div className="text-muted small mt-3">Carregando jornada...</div>
            ) : assignmentsWithSteps.length > 0 ? (
              <ul className="list-group list-group-flush mt-3">
                {assignmentsWithSteps.map(({ assignment, step }) => (
                  <li key={assignment.id} className="list-group-item px-0">
                    <div className="d-flex justify-content-between align-items-start">
                      <div>
                        <div className="fw-semibold">
                          {step?.name ??
                            step?.phase ??
                            `Passo ${assignment.nextStepId ?? "—"}`}
                        </div>
                        <div className="text-muted small">
                          {step?.phase ?? "—"} · {step?.stimulusType ?? "—"}
                        </div>
                      </div>
                      <span className="badge text-bg-light text-dark">
                        {assignment.status}
                      </span>
                    </div>
                  </li>
                ))}
              </ul>
            ) : (
              <div className="text-muted small mt-3">
                Nenhuma jornada criada ainda. Clique em "Criar jornada" para
                gerar os passos do template.
              </div>
            )}
          </div>
        </div>
      ) : null}
      <div ref={tabsSectionRef}>
        <Tabs.Root value={tab} onValueChange={setTab} className="mt-3">
        <Tabs.List className="nav nav-tabs">
          <Tabs.Trigger value="overview" className="nav-link">
            Overview
          </Tabs.Trigger>
          <Tabs.Trigger value="funnel" className="nav-link">
            Funil de vendas
          </Tabs.Trigger>
          <Tabs.Trigger value="targeting" className="nav-link">
            Segmentação
          </Tabs.Trigger>
          <Tabs.Trigger value="creatives" className="nav-link">
            Criativos
          </Tabs.Trigger>
          <Tabs.Trigger value="landing" className="nav-link">
            Landing
          </Tabs.Trigger>
          <Tabs.Trigger value="gera-landing" className="nav-link">
            Gera landing
          </Tabs.Trigger>
          <Tabs.Trigger value="sample-emails" className="nav-link">
            E-mails de amostra
          </Tabs.Trigger>
          <Tabs.Trigger value="lead-portal" className="nav-link">
            Portal do Lead
          </Tabs.Trigger>
          <Tabs.Trigger value="content-structure" className="nav-link">
            Estrutura de conteúdo
          </Tabs.Trigger>
          <Tabs.Trigger value="conteudo" className="nav-link">
            Conteúdo
          </Tabs.Trigger>
        </Tabs.List>
        <Tabs.Content value="overview" asChild>
          <div className="d-flex flex-column gap-3">
            <div className="card">
              <div className="card-body p-0">
                <dl className="row mb-0">
                  {rows.map((r, idx) => (
                    <Fragment key={r.label}>
                      <dt
                        className={`col-sm-3 py-2${idx % 2 === 0 ? " bg-light" : ""}`}
                      >
                        {r.label}
                      </dt>
                      <dd
                        className={`col-sm-9 py-2${idx % 2 === 0 ? " bg-light" : ""}`}
                      >
                        {r.value}
                      </dd>
                    </Fragment>
                  ))}
                </dl>
              </div>
            </div>
            <ExperimentLearningPanel experimentId={expId} />
            <ExperimentReportPanel experimentId={expId} />
          </div>
        </Tabs.Content>
        <Tabs.Content value="funnel" asChild>
          <ExperimentFunnelTab
            experimentId={expId}
            totalSpend={data?.campaignMetric?.spend}
            spendLastSyncedAt={data?.campaignMetric?.lastSyncedAt}
          />
        </Tabs.Content>
        <Tabs.Content value="targeting" asChild>
          <TargetingTab
            nicheId={data.nicheId}
            hypothesisId={data.hypothesisId}
            experimentId={Number(expId)}
            nicheName={niche?.name}
            hypothesisTitle={hyp?.title}
          />
        </Tabs.Content>
        <Tabs.Content value="creatives" asChild>
          <CriativosTab experimentId={expId} />
        </Tabs.Content>
        <Tabs.Content value="landing" asChild>
          <LandingTab experiment={data} />
        </Tabs.Content>
        <Tabs.Content value="gera-landing" asChild>
          <div className="d-flex flex-column gap-3">
            <div className="card">
              <div className="card-body d-flex flex-column gap-3">
                <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
                  <h5 className="card-title mb-0">Gera WireFrame</h5>
                  <span className="badge text-bg-light border fs-6 fw-semibold">
                    Total execuções: {formatCurrencyUsd(totalCompletedGeraLandingCostUsd)}
                  </span>
                </div>
                <div className="d-flex flex-column gap-3">
                  <button
                    type="button"
                    className="btn btn-primary align-self-start"
                    onClick={handleStartWireframe}
                    disabled={isStartingWireframe || hasRunningGeraLandingExecution}
                  >
                    {isStartingWireframe ? (
                      <>
                        <span
                          className="spinner-border spinner-border-sm me-2"
                          role="status"
                          aria-hidden="true"
                        />
                        Iniciando...
                      </>
                    ) : (
                      "Iniciar"
                    )}
                  </button>
                  {isLoadingPendingGeraLandingExecutions ? (
                    <p className="text-muted mb-0">Carregando jobs da etapa...</p>
                  ) : runningGeraLandingExecutions.length === 0 ? (
                    <p className="text-muted mb-0">Nenhum job pendente ou em execução.</p>
                  ) : (
                    <div className="table-responsive">
                      <table className="table table-sm align-middle mb-0">
                        <thead>
                          <tr>
                            <th scope="col">Job ID</th>
                            <th scope="col">Status</th>
                            <th scope="col">Data-hora</th>
                          </tr>
                        </thead>
                        <tbody>
                          {runningGeraLandingExecutions.map((execution) => (
                            <tr key={execution.idJob}>
                              <td>
                                <Link
                                  to={`/experiments/${expId}/geralanding/stage-executions/${execution.idJob}`}
                                  className="fw-semibold text-decoration-none"
                                >
                                  {execution.idJob}
                                </Link>
                              </td>
                              <td>{execution.status}</td>
                              <td>{formatDateTimeValue(execution.executionRequestedAt)}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}

                  <div className="rounded border bg-light-subtle p-3 d-flex flex-column gap-3">
                    <h6 className="mb-0">Histórico de execuções</h6>
                    {isLoadingCompletedGeraLandingExecutions ? (
                      <p className="text-muted mb-0">Carregando execuções...</p>
                    ) : historyGeraLandingExecutions.length === 0 ? (
                      <p className="text-muted mb-0">Nenhuma execução registrada para esta etapa.</p>
                    ) : (
                      <div className="table-responsive">
                        <table className="table table-sm align-middle mb-0">
                          <thead>
                            <tr>
                              <th scope="col">Job ID</th>
                              <th scope="col">Status</th>
                              <th scope="col">Data-hora</th>
                              <th scope="col" className="text-end">Custo</th>
                            </tr>
                          </thead>
                          <tbody>
                            {historyGeraLandingExecutions.map((execution) => (
                              <tr key={execution.idJob}>
                                <td>
                                  <Link
                                    to={`/experiments/${expId}/geralanding/stage-executions/${execution.idJob}`}
                                    className="fw-semibold text-decoration-none"
                                  >
                                    {execution.idJob}
                                  </Link>
                                </td>
                                <td>{execution.status}</td>
                                <td>{formatDateTimeValue(execution.executionRequestedAt)}</td>
                                <td className="text-end">{formatCurrencyUsd(resolveExecutionCostUsd(execution))}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </Tabs.Content>
        <Tabs.Content value="sample-emails" asChild>
          <SampleEmailsTab
            experimentId={expId}
            requestedSampleEmails={data.sampleEmailsToGenerate}
            selectedSampleEmailId={data.selectedSampleEmailId}
            selectedSampleEmailSubject={data.selectedSampleEmailSubject}
            selectedSampleEmailUpdatedAt={data.selectedSampleEmailUpdatedAt}
          />
        </Tabs.Content>
        <Tabs.Content value="lead-portal" asChild>
          <LeadPortalFlowTab experiment={data} />
        </Tabs.Content>
        <Tabs.Content value="content-structure" asChild>
          <ExperimentContentGenerationTab
            experimentId={expId}
            experimentName={data?.name}
            hypothesis={hyp}
            campaignAngle={data?.campaignAngle}
            adCopy={data?.adCopy}
          />
        </Tabs.Content>
        <Tabs.Content value="conteudo" asChild>
          <div className="d-flex flex-column gap-3">
            {pipelineContentCards.map((card) => {
              const formattedValue = formatPipelineJson(card.rawValue);
              return (
                <div className="card" key={card.key}>
                  <div className="card-body">
                    <div className="d-flex flex-wrap align-items-start justify-content-between gap-2">
                      <h5 className="card-title mb-1">{card.title}</h5>
                      {formattedValue ? (
                        <button
                          type="button"
                          className="btn btn-outline-secondary btn-sm"
                          disabled={copyingCardKey === card.key}
                          onClick={async () => {
                            try {
                              setCopyingCardKey(card.key);
                              await copyToClipboard(formattedValue);
                              setCopiedCardKey(card.key);
                              window.setTimeout(() => {
                                setCopiedCardKey((current) =>
                                  current === card.key ? null : current,
                                );
                              }, 1600);
                            } catch {
                              toast.error(
                                "Não foi possível copiar esta etapa para a área de transferência.",
                              );
                            } finally {
                              setCopyingCardKey((current) =>
                                current === card.key ? null : current,
                              );
                            }
                          }}
                        >
                          {copyingCardKey === card.key ? (
                            <span
                              className="spinner-border spinner-border-sm me-1"
                              role="status"
                              aria-hidden="true"
                            />
                          ) : null}
                          {copiedCardKey === card.key ? "Copiado!" : "Copiar etapa"}
                        </button>
                      ) : null}
                    </div>
                    <p className="text-muted small mb-3">{card.description}</p>
                    {formattedValue ? (
                      <CollapsibleJsonViewer content={formattedValue} />
                    ) : (
                      <div className="alert alert-secondary mb-0" role="alert">
                        Sem conteúdo salvo para esta etapa.
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </Tabs.Content>
        </Tabs.Root>
      </div>
      {isResetModalOpen ? (
        <div
          className="modal d-block"
          tabIndex={-1}
          role="dialog"
          aria-modal="true"
        >
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">Resetar campanhas pendentes</h5>
                <button
                  type="button"
                  className="btn-close"
                  aria-label="Fechar"
                  onClick={closeResetModal}
                  disabled={resetCampaigns.isPending}
                />
              </div>
              <div className="modal-body">
                <p>
                  Este reset apaga campanhas, conjuntos e anúncios que foram
                  salvos apenas localmente e ainda não possuem ID do Meta.
                  Utilize a ação quando quiser recomeçar a publicação do
                  experimento do zero.
                </p>
                {previewErrorMessage ? (
                  <div className="alert alert-danger" role="alert">
                    {previewErrorMessage}
                  </div>
                ) : isFetchingResetPreview ? (
                  <div className="d-flex align-items-center gap-2">
                    <span
                      className="spinner-border spinner-border-sm"
                      role="status"
                      aria-hidden="true"
                    />
                    <span>Calculando itens que serão removidos...</span>
                  </div>
                ) : hasItemsToReset ? (
                  <table className="table table-sm mb-3">
                    <tbody>
                      <tr>
                        <td>Campanhas locais sem ID do Meta</td>
                        <td className="text-end fw-semibold">
                          {resetPreviewSummary.campaigns}
                        </td>
                      </tr>
                      <tr>
                        <td>Conjuntos de anúncios</td>
                        <td className="text-end fw-semibold">
                          {resetPreviewSummary.adSets}
                        </td>
                      </tr>
                      <tr>
                        <td>Anúncios</td>
                        <td className="text-end fw-semibold">
                          {resetPreviewSummary.ads}
                        </td>
                      </tr>
                      <tr>
                        <td>Criativos vinculados</td>
                        <td className="text-end fw-semibold">
                          {resetPreviewSummary.creatives}
                        </td>
                      </tr>
                    </tbody>
                  </table>
                ) : (
                  <p className="text-muted mb-0">
                    Nenhuma campanha sem ID do Meta foi encontrada para este
                    experimento.
                  </p>
                )}
                <p className="text-muted small mt-3 mb-0">
                  Somente ativos sem ID do Meta serão excluídos. Tudo que já foi
                  publicado permanece intacto.
                </p>
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={closeResetModal}
                  disabled={resetCampaigns.isPending}
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  className="btn btn-danger"
                  onClick={handleConfirmReset}
                  disabled={confirmResetDisabled}
                >
                  {resetCampaigns.isPending ? (
                    <>
                      <span
                        className="spinner-border spinner-border-sm me-2"
                        role="status"
                        aria-hidden="true"
                      />
                      Resetando...
                    </>
                  ) : (
                    "Confirmar reset"
                  )}
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
