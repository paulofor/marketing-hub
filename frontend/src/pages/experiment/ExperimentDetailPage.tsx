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
import { useBreadcrumbs } from "../../app/breadcrumbs";
import * as Tabs from "@radix-ui/react-tabs";
import { useExperimentFacebookCampaigns } from "../../api/experiment/useExperimentFacebookCampaigns";
import { useExperimentReadiness } from "../../api/experiment/useExperimentReadiness";
import {
  useExperimentCampaignReset,
  useExperimentCampaignResetPreview,
  type ExperimentCampaignResetSummary,
} from "../../api/experiment/useExperimentCampaignReset";
import ExperimentFunnelTab from "./ExperimentFunnelTab";
import ExperimentLandingAnalyticsTab from "./ExperimentLandingAnalyticsTab";
import ExperimentContentGenerationTab from "./ExperimentContentGenerationTab";
import { ExperimentAudienceTab } from "./ExperimentAudienceTab";
import ExperimentRunPanel from "./ExperimentRunPanel";
import LandingTab from "./LandingTab";
import CollapsibleJsonViewer from "../../components/CollapsibleJsonViewer";
import { useExperimentFacebookRelease } from "../../api/experiment/useExperimentFacebookRelease";
import {
  useGeraLandingStageExecutionDetail,
  useGeraLandingStageExecutions,
  type GeraLandingStageExecutionItem,
} from "../../api/experiment/useGeraLandingStageExecutions";
import { useFrameworkImageStatuses } from "../../api/experiment/useFrameworkImageStatuses";
import { useFrameworkImageSummary } from "../../api/experiment/useFrameworkImageSummary";
import {
  useGeraLandingStageModels,
  type GeraLandingStageModel,
} from "../../api/pipeline/useGeraLandingStageModels";
import { useExperimentCompleteMarkdownReport } from "../../api/experiment/useExperimentCompleteMarkdownReport";
import { useGeraSalesPagePublications } from "../../api/experiment/useGeraSalesPagePublications";
import { useExperimentPipelineJobHistory } from "../../api/experiment/useExperimentPipelineJobHistory";
import { buildExperimentCostSummary } from "./experimentCostSummary";

function formatPipelineStageModel(stageModel?: GeraLandingStageModel) {
  const name = stageModel?.openAiModelName?.trim();
  const code = stageModel?.openAiModelCode?.trim();
  if (name && code) {
    return `${name} (${code})`;
  }
  return (
    name ||
    code ||
    (stageModel?.openAiModelId ? `Modelo #${stageModel.openAiModelId}` : null)
  );
}

const experimentDetailTabs = [
  { value: "overview", label: "Overview" },
  { value: "funnel", label: "Funil de vendas" },
  { value: "analytics", label: "Analytics" },
  { value: "creatives", label: "Criativos" },
  { value: "landing", label: "Landing" },
  { value: "content-structure", label: "Estrutura de conteúdo" },
  { value: "publico", label: "Público" },
] as const;

function parseCostValue(value?: number | string | null) {
  if (value == null) return null;
  if (typeof value === "number") return Number.isFinite(value) ? value : null;
  const normalizedValue = value.replace(",", ".").trim();
  if (!normalizedValue) return null;
  const parsedValue = Number(normalizedValue);
  return Number.isFinite(parsedValue) ? parsedValue : null;
}

function formatModelCostPerMillion(value?: number | string | null) {
  const parsedValue = parseCostValue(value);
  return parsedValue == null
    ? "—"
    : new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: "USD",
        minimumFractionDigits: 2,
        maximumFractionDigits: 5,
      }).format(parsedValue);
}

function formatGeneratedAssetType(value?: string | null) {
  switch (value) {
    case "texto":
      return "Texto";
    case "imagem":
      return "Imagem";
    case "video":
      return "Vídeo";
    case "audio":
      return "Áudio";
    default:
      return value?.trim() || "Texto";
  }
}

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

function downloadPipelineStage(content: string, filename: string) {
  const blob = new Blob([content], { type: "application/json;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

function sanitizeFilenamePart(value: string) {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/\s+/g, "-")
    .replace(/[^a-z0-9-]/g, "");
}

function resolveJobNumberFromStageContent(content: string) {
  try {
    const parsed = JSON.parse(content) as Record<string, unknown>;
    const jobValue = parsed?.jobNumber ?? parsed?.jobId ?? parsed?.job_id;
    if (jobValue === undefined || jobValue === null) {
      return "sem-job";
    }

    const valueAsString = String(jobValue).trim();
    return valueAsString.length > 0
      ? sanitizeFilenamePart(valueAsString)
      : "sem-job";
  } catch {
    return "sem-job";
  }
}

function hasExecutionWithJobId(
  executions: GeraLandingStageExecutionItem[] | undefined,
  jobId: string,
) {
  return (executions ?? []).some((execution) => execution.idJob === jobId);
}

function mergeOptimisticExecution(
  optimisticExecution: GeraLandingStageExecutionItem | null,
  pendingExecutions: GeraLandingStageExecutionItem[] | undefined,
  completedExecutions: GeraLandingStageExecutionItem[] | undefined,
) {
  if (!optimisticExecution) {
    return pendingExecutions ?? [];
  }
  const persistedExecution =
    hasExecutionWithJobId(pendingExecutions, optimisticExecution.idJob) ||
    hasExecutionWithJobId(completedExecutions, optimisticExecution.idJob);
  if (persistedExecution) {
    return pendingExecutions ?? [];
  }
  return [optimisticExecution, ...(pendingExecutions ?? [])];
}

function normalizeQualityReviewScore(score?: number | string | null) {
  if (score == null) return null;
  if (typeof score === "number") return Number.isFinite(score) ? score : null;
  const normalizedScore = score.replace(",", ".").trim();
  if (!normalizedScore) return null;
  const parsedScore = Number(normalizedScore);
  return Number.isFinite(parsedScore) ? parsedScore : null;
}

function formatQualityReviewScore(score?: number | string | null) {
  const normalizedScore = normalizeQualityReviewScore(score);
  return normalizedScore != null
    ? new Intl.NumberFormat("pt-BR", {
        maximumFractionDigits: 2,
      }).format(normalizedScore)
    : "—";
}

function resolveQualityReviewApprovalLabel(
  execution?: GeraLandingStageExecutionItem,
) {
  if (!execution) return "Sem revisão concluída";
  if (execution.approvedForPublication === true) return "Aprovado";
  if (execution.approvedForPublication === false) return "Não aprovado";
  if (execution.approvalRecommendation === "APPROVE_FOR_PUBLICATION") {
    return "Aprovado";
  }
  if (execution.approvalRecommendation === "REGENERATE_BEFORE_PUBLICATION") {
    return "Não aprovado";
  }
  return "Decisão indisponível";
}

function resolveQualityReviewApprovalBadgeClass(
  execution?: GeraLandingStageExecutionItem,
) {
  const label = resolveQualityReviewApprovalLabel(execution);
  if (label === "Aprovado") return "text-bg-success";
  if (label === "Não aprovado") return "text-bg-danger";
  return "text-bg-secondary";
}

const EXPERIMENT_ALTERATION_LOCK_STATUSES = new Set([
  "RUNNING",
  "PAUSED",
  "USER_STOPPED",
  "VALIDATED",
  "INVALIDATED",
  "INCONCLUSIVE",
  "FINISHED",
]);

function isExperimentAlterationLocked(experiment: {
  status?: string | null;
  facebookReleaseRequestedAt?: string | null;
}) {
  const normalizedStatus = (experiment.status ?? "").trim().toUpperCase();
  if (normalizedStatus === "FAILED") {
    return false;
  }
  return (
    Boolean(experiment.facebookReleaseRequestedAt) ||
    EXPERIMENT_ALTERATION_LOCK_STATUSES.has(normalizedStatus)
  );
}

export default function ExperimentDetailPage() {
  const { id } = useParams();
  const expId = id as string;
  const navigate = useNavigate();
  const { data, isLoading } = useExperiment(expId);
  const geraSalesPagePublications = useGeraSalesPagePublications(expId);
  const experimentPipelineJobHistory = useExperimentPipelineJobHistory({
    experimentId: expId,
    page: 0,
    size: 50,
  });
  const { data: geraLandingStageModels, isLoading: isLoadingStageModels } =
    useGeraLandingStageModels();
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
  const { data: facebookCampaigns, isLoading: isLoadingFacebookCampaigns } =
    useExperimentFacebookCampaigns(expId);
  const [isResetModalOpen, setIsResetModalOpen] = useState(false);
  const [openAuditStageJobId, setOpenAuditStageJobId] = useState<
    string | null
  >(null);
  const [copiedCardKey, setCopiedCardKey] = useState<string | null>(null);
  const [copyingCardKey, setCopyingCardKey] = useState<string | null>(null);
  const [downloadingCardKey, setDownloadingCardKey] = useState<string | null>(
    null,
  );
  const [isStartingWireframe, setIsStartingWireframe] = useState(false);
  const [isStartingCopy, setIsStartingCopy] = useState(false);
  const [isStartingDesignPreset, setIsStartingDesignPreset] = useState(false);
  const [isStartingImagePrompts, setIsStartingImagePrompts] = useState(false);
  const [isStartingQualityReview, setIsStartingQualityReview] = useState(false);
  const [isStartingDeliverables, setIsStartingDeliverables] = useState(false);
  const [isStartingImageGeneration, setIsStartingImageGeneration] =
    useState(false);
  const [resetFrameworkImageCounters, setResetFrameworkImageCounters] =
    useState(false);
  const [isGeneratingLandingHtml, setIsGeneratingLandingHtml] = useState(false);
  const [optimisticWireframeExecution, setOptimisticWireframeExecution] =
    useState<GeraLandingStageExecutionItem | null>(null);
  const [optimisticCopyExecution, setOptimisticCopyExecution] =
    useState<GeraLandingStageExecutionItem | null>(null);
  const [optimisticDesignPresetExecution, setOptimisticDesignPresetExecution] =
    useState<GeraLandingStageExecutionItem | null>(null);
  const [optimisticImagePromptsExecution, setOptimisticImagePromptsExecution] =
    useState<GeraLandingStageExecutionItem | null>(null);
  const [
    optimisticImageGenerationExecution,
    setOptimisticImageGenerationExecution,
  ] = useState<GeraLandingStageExecutionItem | null>(null);
  const [
    optimisticQualityReviewExecution,
    setOptimisticQualityReviewExecution,
  ] = useState<GeraLandingStageExecutionItem | null>(null);
  const [optimisticDeliverablesExecution, setOptimisticDeliverablesExecution] =
    useState<GeraLandingStageExecutionItem | null>(null);
  const hadRunningGeraLandingExecutionRef = useRef(false);
  const hasSentGeraLandingBackgroundNotificationRef = useRef(false);
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
  const {
    data: pendingGeraLandingCopyExecutions,
    isLoading: isLoadingPendingGeraLandingCopyExecutions,
    refetch: refetchPendingGeraLandingCopyExecutions,
  } = useGeraLandingStageExecutions(expId, "landing-page-copy", false);
  const {
    data: completedGeraLandingCopyExecutions,
    isLoading: isLoadingCompletedGeraLandingCopyExecutions,
    refetch: refetchCompletedGeraLandingCopyExecutions,
  } = useGeraLandingStageExecutions(expId, "landing-page-copy", true);
  const {
    data: pendingGeraLandingDesignPresetExecutions,
    isLoading: isLoadingPendingGeraLandingDesignPresetExecutions,
    refetch: refetchPendingGeraLandingDesignPresetExecutions,
  } = useGeraLandingStageExecutions(expId, "landing-page-design-preset", false);
  const {
    data: completedGeraLandingDesignPresetExecutions,
    isLoading: isLoadingCompletedGeraLandingDesignPresetExecutions,
    refetch: refetchCompletedGeraLandingDesignPresetExecutions,
  } = useGeraLandingStageExecutions(expId, "landing-page-design-preset", true);
  const {
    data: pendingGeraLandingImagePromptsExecutions,
    isLoading: isLoadingPendingGeraLandingImagePromptsExecutions,
    refetch: refetchPendingGeraLandingImagePromptsExecutions,
  } = useGeraLandingStageExecutions(
    expId,
    "landing-page-image-planning",
    false,
  );
  const {
    data: completedGeraLandingImagePromptsExecutions,
    isLoading: isLoadingCompletedGeraLandingImagePromptsExecutions,
    refetch: refetchCompletedGeraLandingImagePromptsExecutions,
  } = useGeraLandingStageExecutions(expId, "landing-page-image-planning", true);
  const {
    data: pendingGeraLandingImageGenerationExecutions,
    isLoading: isLoadingPendingGeraLandingImageGenerationExecutions,
    refetch: refetchPendingGeraLandingImageGenerationExecutions,
  } = useGeraLandingStageExecutions(
    expId,
    "landing-page-image-generation",
    false,
  );
  const {
    data: completedGeraLandingImageGenerationExecutions,
    isLoading: isLoadingCompletedGeraLandingImageGenerationExecutions,
    refetch: refetchCompletedGeraLandingImageGenerationExecutions,
  } = useGeraLandingStageExecutions(
    expId,
    "landing-page-image-generation",
    true,
  );
  const {
    data: pendingGeraLandingQualityReviewExecutions,
    isLoading: isLoadingPendingGeraLandingQualityReviewExecutions,
    refetch: refetchPendingGeraLandingQualityReviewExecutions,
  } = useGeraLandingStageExecutions(
    expId,
    "landing-page-quality-review",
    false,
  );
  const {
    data: completedGeraLandingQualityReviewExecutions,
    isLoading: isLoadingCompletedGeraLandingQualityReviewExecutions,
    refetch: refetchCompletedGeraLandingQualityReviewExecutions,
  } = useGeraLandingStageExecutions(expId, "landing-page-quality-review", true);
  const {
    data: pendingGeraLandingDeliverablesExecutions,
    isLoading: isLoadingPendingGeraLandingDeliverablesExecutions,
    refetch: refetchPendingGeraLandingDeliverablesExecutions,
  } = useGeraLandingStageExecutions(expId, "landing-page-deliverables", false);
  const {
    data: completedGeraLandingDeliverablesExecutions,
    isLoading: isLoadingCompletedGeraLandingDeliverablesExecutions,
    refetch: refetchCompletedGeraLandingDeliverablesExecutions,
  } = useGeraLandingStageExecutions(expId, "landing-page-deliverables", true);
  const {
    data: frameworkImageStatuses,
    isLoading: isLoadingFrameworkImageStatuses,
  } = useFrameworkImageStatuses(expId);
  const { data: frameworkImageSummary } = useFrameworkImageSummary(expId);
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
  const completeMarkdownReport = useExperimentCompleteMarkdownReport(expId);
  const frameworkImagePendingCount = useMemo(
    () =>
      (frameworkImageStatuses ?? []).filter((item) => {
        const status = item.status?.toUpperCase();
        return status === "PLANNED" || status === "FAILED";
      }).length,
    [frameworkImageStatuses],
  );
  const canDownloadCompleteReport =
    data?.status === "VALIDATED" || data?.status === "INVALIDATED";

  const handleDownloadCompleteReport = () => {
    if (!completeMarkdownReport.isPending) {
      completeMarkdownReport.mutate();
    }
  };

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
        description:
          "Conteúdo bruto salvo na coluna landing_page_image_planning.",
        rawValue: data?.landingPageImagePlanning,
      },
      {
        key: "design-preset",
        title: "Etapa 6 · Preset Design",
        description:
          "Conteúdo bruto (JSON) salvo na coluna landing_page_design_preset.",
        rawValue: data?.landingPageDesignPreset,
      },
      {
        key: "geralanding-html",
        title: "Etapa 7 · GeraLanding HTML",
        description: "Conteúdo bruto salvo na coluna html_geralanding.",
        rawValue: data?.htmlGeraLanding,
      },
      {
        key: "landing-html",
        title: "Etapa 8 · Landing HTML",
        description: "Conteúdo bruto salvo na coluna landing_page_html.",
        rawValue: data?.landingPageHtml,
      },
      {
        key: "landing-page-deliverables",
        title: "Etapa 9 · Landing Page Deliverables",
        description:
          "JSON final dos entregáveis da amostra e do produto final salvo na coluna landing_page_deliverables.",
        rawValue: data?.landingPageDeliverables,
      },
    ],
    [
      data?.adCopy,
      data?.campaignAngle,
      data?.landingPageCopy,
      data?.landingPageDesignPreset,
      data?.htmlGeraLanding,
      data?.landingPageDeliverables,
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
  const hasEmailSteps = false;

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

  const handleStartCopy = async () => {
    try {
      setIsStartingCopy(true);
      const { data: startResponse } = await axios.post<{
        idJob: string;
        status: string;
      }>(`/api/experiments/${expId}/geralanding/copy/start`);
      const localExecutionRequestedAt = new Date().toISOString();
      setOptimisticCopyExecution({
        idJob: startResponse.idJob,
        status: startResponse.status,
        executionRequestedAt: localExecutionRequestedAt,
      });
      toast.success(
        `Solicitação registrada. Código: ${startResponse.idJob} | Status: ${startResponse.status}`,
      );
      await Promise.all([
        refetchPendingGeraLandingCopyExecutions(),
        refetchCompletedGeraLandingCopyExecutions(),
      ]);
    } catch (error) {
      const message = axios.isAxiosError(error)
        ? (error.response?.data?.message ??
          error.response?.data?.detail ??
          "Não foi possível iniciar o Copy.")
        : "Não foi possível iniciar o Copy.";
      toast.error(message);
    } finally {
      setIsStartingCopy(false);
    }
  };
  const handleStartDesignPreset = async () => {
    try {
      setIsStartingDesignPreset(true);
      const { data: startResponse } = await axios.post<{
        idJob: string;
        status: string;
      }>(`/api/experiments/${expId}/geralanding/design-preset/start`);
      const localExecutionRequestedAt = new Date().toISOString();
      setOptimisticDesignPresetExecution({
        idJob: startResponse.idJob,
        status: startResponse.status,
        executionRequestedAt: localExecutionRequestedAt,
      });
      toast.success(
        `Solicitação registrada. Código: ${startResponse.idJob} | Status: ${startResponse.status}`,
      );
      await Promise.all([
        refetchPendingGeraLandingDesignPresetExecutions(),
        refetchCompletedGeraLandingDesignPresetExecutions(),
      ]);
    } catch (error) {
      const message = axios.isAxiosError(error)
        ? (error.response?.data?.message ??
          error.response?.data?.detail ??
          "Não foi possível iniciar o Gera Preset Design.")
        : "Não foi possível iniciar o Gera Preset Design.";
      toast.error(message);
    } finally {
      setIsStartingDesignPreset(false);
    }
  };

  const handleStartImagePrompts = async () => {
    try {
      setIsStartingImagePrompts(true);
      const { data: startResponse } = await axios.post<{
        idJob: string;
        status: string;
      }>(`/api/experiments/${expId}/geralanding/image-prompts/start`);
      const localExecutionRequestedAt = new Date().toISOString();
      setOptimisticImagePromptsExecution({
        idJob: startResponse.idJob,
        status: startResponse.status,
        executionRequestedAt: localExecutionRequestedAt,
      });
      toast.success(
        `Solicitação registrada. Código: ${startResponse.idJob} | Status: ${startResponse.status}`,
      );
      setResetFrameworkImageCounters(true);
      await Promise.all([
        refetchPendingGeraLandingImagePromptsExecutions(),
        refetchCompletedGeraLandingImagePromptsExecutions(),
      ]);
    } catch (error) {
      const message = axios.isAxiosError(error)
        ? (error.response?.data?.message ??
          error.response?.data?.detail ??
          "Não foi possível iniciar o Gera Prompt Imagem.")
        : "Não foi possível iniciar o Gera Prompt Imagem.";
      toast.error(message);
    } finally {
      setIsStartingImagePrompts(false);
    }
  };

  const handleStartQualityReview = async () => {
    try {
      setIsStartingQualityReview(true);
      const { data: startResponse } = await axios.post<{
        idJob: string;
        status: string;
      }>(`/api/experiments/${expId}/geralanding/quality-review/start`);
      const localExecutionRequestedAt = new Date().toISOString();
      setOptimisticQualityReviewExecution({
        idJob: startResponse.idJob,
        status: startResponse.status,
        executionRequestedAt: localExecutionRequestedAt,
      });
      toast.success(
        `Solicitação registrada. Código: ${startResponse.idJob} | Status: ${startResponse.status}`,
      );
      await Promise.all([
        refetchPendingGeraLandingQualityReviewExecutions(),
        refetchCompletedGeraLandingQualityReviewExecutions(),
      ]);
    } catch (error) {
      const message = axios.isAxiosError(error)
        ? (error.response?.data?.message ??
          error.response?.data?.detail ??
          "Não foi possível iniciar o Quality Review.")
        : "Não foi possível iniciar o Quality Review.";
      toast.error(message);
    } finally {
      setIsStartingQualityReview(false);
    }
  };

  const handleStartDeliverables = async () => {
    try {
      setIsStartingDeliverables(true);
      const { data: startResponse } = await axios.post<{
        idJob: string;
        status: string;
      }>(`/api/experiments/${expId}/geralanding/deliverables/start`);
      const localExecutionRequestedAt = new Date().toISOString();
      setOptimisticDeliverablesExecution({
        idJob: startResponse.idJob,
        status: startResponse.status,
        executionRequestedAt: localExecutionRequestedAt,
      });
      toast.success(
        `Solicitação registrada. Código: ${startResponse.idJob} | Status: ${startResponse.status}`,
      );
      await Promise.all([
        refetchPendingGeraLandingDeliverablesExecutions(),
        refetchCompletedGeraLandingDeliverablesExecutions(),
      ]);
    } catch (error) {
      const message = axios.isAxiosError(error)
        ? (error.response?.data?.message ??
          error.response?.data?.detail ??
          "Não foi possível iniciar o Gera Entregáveis.")
        : "Não foi possível iniciar o Gera Entregáveis.";
      toast.error(message);
    } finally {
      setIsStartingDeliverables(false);
    }
  };

  const handleStartImageGeneration = async () => {
    try {
      setIsStartingImageGeneration(true);
      const { data: startResponse } = await axios.post<{
        idJob: string;
        status: string;
      }>(`/api/experiments/${expId}/geralanding/image-generation/start`);
      const localExecutionRequestedAt = new Date().toISOString();
      setOptimisticImageGenerationExecution({
        idJob: startResponse.idJob,
        status: startResponse.status,
        executionRequestedAt: localExecutionRequestedAt,
      });
      setResetFrameworkImageCounters(false);
      toast.success(
        `Solicitação registrada. Código: ${startResponse.idJob} | Status: ${startResponse.status}`,
      );
      await Promise.all([
        refetchPendingGeraLandingImageGenerationExecutions(),
        refetchCompletedGeraLandingImageGenerationExecutions(),
      ]);
    } catch (error) {
      const message = axios.isAxiosError(error)
        ? (error.response?.data?.message ??
          error.response?.data?.detail ??
          "Não foi possível iniciar o Gera Imagem.")
        : "Não foi possível iniciar o Gera Imagem.";
      toast.error(message);
    } finally {
      setIsStartingImageGeneration(false);
    }
  };

  const handleGenerateLandingHtml = async (jobId: string) => {
    setIsGeneratingLandingHtml(true);
    try {
      await axios.post(
        `/api/experiments/${expId}/geralanding/html/provisional/generate`,
        null,
        { params: { jobId } },
      );
      toast.success("Geração de HTML iniciada com sucesso.");
    } catch (error) {
      const message = axios.isAxiosError(error)
        ? ((error.response?.data?.message as string | undefined) ??
          "Não foi possível iniciar a geração de HTML.")
        : "Não foi possível iniciar a geração de HTML.";
      toast.error(message);
    } finally {
      setIsGeneratingLandingHtml(false);
    }
  };

  const isRunningExecution = (status?: string | null) => {
    const normalizedStatus = (status ?? "").trim().toUpperCase();
    return [
      "AGUARDANDO_RETORNO_OPENAI",
      "EM_PROCESSAMENTO",
      "PROCESSING",
      "RUNNING",
      "IN_PROGRESS",
      "PENDING",
      "INICIADO",
      "STARTED",
    ].includes(normalizedStatus);
  };
  const isCompletedExecution = (status?: string | null) => {
    const normalizedStatus = (status ?? "").trim().toUpperCase();
    return [
      "CONCLUIDO",
      "CONCLUÍDO",
      "COMPLETED",
      "SUCCESS",
      "SUCCEEDED",
      "DONE",
    ].includes(normalizedStatus);
  };
  const mergedPendingGeraLandingExecutions = useMemo(
    () =>
      mergeOptimisticExecution(
        optimisticWireframeExecution,
        pendingGeraLandingExecutions,
        completedGeraLandingExecutions,
      ),
    [
      optimisticWireframeExecution,
      pendingGeraLandingExecutions,
      completedGeraLandingExecutions,
    ],
  );

  useEffect(() => {
    if (!optimisticWireframeExecution) {
      return;
    }
    const persistedExecution =
      hasExecutionWithJobId(
        pendingGeraLandingExecutions,
        optimisticWireframeExecution.idJob,
      ) ||
      hasExecutionWithJobId(
        completedGeraLandingExecutions,
        optimisticWireframeExecution.idJob,
      );
    if (persistedExecution) {
      setOptimisticWireframeExecution(null);
    }
  }, [
    optimisticWireframeExecution,
    pendingGeraLandingExecutions,
    completedGeraLandingExecutions,
  ]);

  const hasRunningGeraLandingExecution =
    mergedPendingGeraLandingExecutions.some((execution) =>
      isRunningExecution(execution.status),
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
      (execution) =>
        isCompletedExecution(execution.status) ||
        hasFailedExecution(execution.status),
    );
    const failedFromPending = (pendingGeraLandingExecutions ?? []).filter(
      (execution) => hasFailedExecution(execution.status),
    );

    const sortedExecutions = [...failedFromPending, ...completedHistory].sort(
      (leftExecution, rightExecution) => {
        const leftTimestamp = Date.parse(
          leftExecution.executionRequestedAt ?? "",
        );
        const rightTimestamp = Date.parse(
          rightExecution.executionRequestedAt ?? "",
        );
        const normalizedLeftTimestamp = Number.isNaN(leftTimestamp)
          ? 0
          : leftTimestamp;
        const normalizedRightTimestamp = Number.isNaN(rightTimestamp)
          ? 0
          : rightTimestamp;

        return normalizedRightTimestamp - normalizedLeftTimestamp;
      },
    );

    return sortedExecutions.filter(
      (execution, index, allExecutions) =>
        allExecutions.findIndex(
          (candidate) => candidate.idJob === execution.idJob,
        ) === index,
    );
  }, [completedGeraLandingExecutions, pendingGeraLandingExecutions]);

  const mergedPendingGeraLandingCopyExecutions = useMemo(
    () =>
      mergeOptimisticExecution(
        optimisticCopyExecution,
        pendingGeraLandingCopyExecutions,
        completedGeraLandingCopyExecutions,
      ),
    [
      optimisticCopyExecution,
      pendingGeraLandingCopyExecutions,
      completedGeraLandingCopyExecutions,
    ],
  );

  useEffect(() => {
    if (!optimisticCopyExecution) {
      return;
    }
    const persistedExecution =
      hasExecutionWithJobId(
        pendingGeraLandingCopyExecutions,
        optimisticCopyExecution.idJob,
      ) ||
      hasExecutionWithJobId(
        completedGeraLandingCopyExecutions,
        optimisticCopyExecution.idJob,
      );
    if (persistedExecution) {
      setOptimisticCopyExecution(null);
    }
  }, [
    optimisticCopyExecution,
    pendingGeraLandingCopyExecutions,
    completedGeraLandingCopyExecutions,
  ]);

  const hasRunningGeraLandingCopyExecution =
    mergedPendingGeraLandingCopyExecutions.some((execution) =>
      isRunningExecution(execution.status),
    );
  const runningGeraLandingCopyExecutions = useMemo(
    () =>
      mergedPendingGeraLandingCopyExecutions.filter((execution) =>
        isRunningExecution(execution.status),
      ),
    [mergedPendingGeraLandingCopyExecutions],
  );
  const historyGeraLandingCopyExecutions = useMemo(() => {
    const completedHistory = (completedGeraLandingCopyExecutions ?? []).filter(
      (execution) =>
        isCompletedExecution(execution.status) ||
        hasFailedExecution(execution.status),
    );
    const failedFromPending = (pendingGeraLandingCopyExecutions ?? []).filter(
      (execution) => hasFailedExecution(execution.status),
    );

    const sortedExecutions = [...failedFromPending, ...completedHistory].sort(
      (leftExecution, rightExecution) => {
        const leftTimestamp = Date.parse(
          leftExecution.executionRequestedAt ?? "",
        );
        const rightTimestamp = Date.parse(
          rightExecution.executionRequestedAt ?? "",
        );
        const normalizedLeftTimestamp = Number.isNaN(leftTimestamp)
          ? 0
          : leftTimestamp;
        const normalizedRightTimestamp = Number.isNaN(rightTimestamp)
          ? 0
          : rightTimestamp;

        return normalizedRightTimestamp - normalizedLeftTimestamp;
      },
    );

    return sortedExecutions.filter(
      (execution, index, allExecutions) =>
        allExecutions.findIndex(
          (candidate) => candidate.idJob === execution.idJob,
        ) === index,
    );
  }, [completedGeraLandingCopyExecutions, pendingGeraLandingCopyExecutions]);
  const mergedPendingGeraLandingDesignPresetExecutions = useMemo(
    () =>
      mergeOptimisticExecution(
        optimisticDesignPresetExecution,
        pendingGeraLandingDesignPresetExecutions,
        completedGeraLandingDesignPresetExecutions,
      ),
    [
      optimisticDesignPresetExecution,
      pendingGeraLandingDesignPresetExecutions,
      completedGeraLandingDesignPresetExecutions,
    ],
  );

  useEffect(() => {
    if (!optimisticDesignPresetExecution) {
      return;
    }
    const persistedExecution =
      hasExecutionWithJobId(
        pendingGeraLandingDesignPresetExecutions,
        optimisticDesignPresetExecution.idJob,
      ) ||
      hasExecutionWithJobId(
        completedGeraLandingDesignPresetExecutions,
        optimisticDesignPresetExecution.idJob,
      );
    if (persistedExecution) {
      setOptimisticDesignPresetExecution(null);
    }
  }, [
    optimisticDesignPresetExecution,
    pendingGeraLandingDesignPresetExecutions,
    completedGeraLandingDesignPresetExecutions,
  ]);

  const hasRunningGeraLandingDesignPresetExecution =
    mergedPendingGeraLandingDesignPresetExecutions.some((execution) =>
      isRunningExecution(execution.status),
    );
  const runningGeraLandingDesignPresetExecutions = useMemo(
    () =>
      mergedPendingGeraLandingDesignPresetExecutions.filter((execution) =>
        isRunningExecution(execution.status),
      ),
    [mergedPendingGeraLandingDesignPresetExecutions],
  );
  const historyGeraLandingDesignPresetExecutions = useMemo(() => {
    const completedHistory = (
      completedGeraLandingDesignPresetExecutions ?? []
    ).filter(
      (execution) =>
        isCompletedExecution(execution.status) ||
        hasFailedExecution(execution.status),
    );
    const failedFromPending = (
      pendingGeraLandingDesignPresetExecutions ?? []
    ).filter((execution) => hasFailedExecution(execution.status));

    const sortedExecutions = [...failedFromPending, ...completedHistory].sort(
      (leftExecution, rightExecution) => {
        const leftTimestamp = Date.parse(
          leftExecution.executionRequestedAt ?? "",
        );
        const rightTimestamp = Date.parse(
          rightExecution.executionRequestedAt ?? "",
        );
        const normalizedLeftTimestamp = Number.isNaN(leftTimestamp)
          ? 0
          : leftTimestamp;
        const normalizedRightTimestamp = Number.isNaN(rightTimestamp)
          ? 0
          : rightTimestamp;

        return normalizedRightTimestamp - normalizedLeftTimestamp;
      },
    );

    return sortedExecutions.filter(
      (execution, index, allExecutions) =>
        allExecutions.findIndex(
          (candidate) => candidate.idJob === execution.idJob,
        ) === index,
    );
  }, [
    completedGeraLandingDesignPresetExecutions,
    pendingGeraLandingDesignPresetExecutions,
  ]);

  const mergedPendingGeraLandingImagePromptsExecutions = useMemo(
    () =>
      mergeOptimisticExecution(
        optimisticImagePromptsExecution,
        pendingGeraLandingImagePromptsExecutions,
        completedGeraLandingImagePromptsExecutions,
      ),
    [
      optimisticImagePromptsExecution,
      pendingGeraLandingImagePromptsExecutions,
      completedGeraLandingImagePromptsExecutions,
    ],
  );

  useEffect(() => {
    if (!optimisticImagePromptsExecution) {
      return;
    }
    const persistedExecution =
      hasExecutionWithJobId(
        pendingGeraLandingImagePromptsExecutions,
        optimisticImagePromptsExecution.idJob,
      ) ||
      hasExecutionWithJobId(
        completedGeraLandingImagePromptsExecutions,
        optimisticImagePromptsExecution.idJob,
      );
    if (persistedExecution) {
      setOptimisticImagePromptsExecution(null);
    }
  }, [
    optimisticImagePromptsExecution,
    pendingGeraLandingImagePromptsExecutions,
    completedGeraLandingImagePromptsExecutions,
  ]);

  const hasRunningGeraLandingImagePromptsExecution =
    mergedPendingGeraLandingImagePromptsExecutions.some((execution) =>
      isRunningExecution(execution.status),
    );
  const runningGeraLandingImagePromptsExecutions = useMemo(
    () =>
      mergedPendingGeraLandingImagePromptsExecutions.filter((execution) =>
        isRunningExecution(execution.status),
      ),
    [mergedPendingGeraLandingImagePromptsExecutions],
  );
  const historyGeraLandingImagePromptsExecutions = useMemo(() => {
    const completedHistory = (
      completedGeraLandingImagePromptsExecutions ?? []
    ).filter(
      (execution) =>
        isCompletedExecution(execution.status) ||
        hasFailedExecution(execution.status),
    );
    const failedFromPending = (
      pendingGeraLandingImagePromptsExecutions ?? []
    ).filter((execution) => hasFailedExecution(execution.status));

    const sortedExecutions = [...failedFromPending, ...completedHistory].sort(
      (leftExecution, rightExecution) => {
        const leftTimestamp = Date.parse(
          leftExecution.executionRequestedAt ?? "",
        );
        const rightTimestamp = Date.parse(
          rightExecution.executionRequestedAt ?? "",
        );
        const normalizedLeftTimestamp = Number.isNaN(leftTimestamp)
          ? 0
          : leftTimestamp;
        const normalizedRightTimestamp = Number.isNaN(rightTimestamp)
          ? 0
          : rightTimestamp;

        return normalizedRightTimestamp - normalizedLeftTimestamp;
      },
    );

    return sortedExecutions.filter(
      (execution, index, allExecutions) =>
        allExecutions.findIndex(
          (candidate) => candidate.idJob === execution.idJob,
        ) === index,
    );
  }, [
    completedGeraLandingImagePromptsExecutions,
    pendingGeraLandingImagePromptsExecutions,
  ]);

  const mergedPendingGeraLandingImageGenerationExecutions = useMemo(
    () =>
      mergeOptimisticExecution(
        optimisticImageGenerationExecution,
        pendingGeraLandingImageGenerationExecutions,
        completedGeraLandingImageGenerationExecutions,
      ),
    [
      optimisticImageGenerationExecution,
      pendingGeraLandingImageGenerationExecutions,
      completedGeraLandingImageGenerationExecutions,
    ],
  );

  useEffect(() => {
    if (!optimisticImageGenerationExecution) {
      return;
    }
    const persistedExecution =
      hasExecutionWithJobId(
        pendingGeraLandingImageGenerationExecutions,
        optimisticImageGenerationExecution.idJob,
      ) ||
      hasExecutionWithJobId(
        completedGeraLandingImageGenerationExecutions,
        optimisticImageGenerationExecution.idJob,
      );
    if (persistedExecution) {
      setOptimisticImageGenerationExecution(null);
    }
  }, [
    optimisticImageGenerationExecution,
    pendingGeraLandingImageGenerationExecutions,
    completedGeraLandingImageGenerationExecutions,
  ]);

  const hasRunningGeraLandingImageGenerationExecution =
    mergedPendingGeraLandingImageGenerationExecutions.some((execution) =>
      isRunningExecution(execution.status),
    );
  const runningGeraLandingImageGenerationExecutions = useMemo(
    () =>
      mergedPendingGeraLandingImageGenerationExecutions.filter((execution) =>
        isRunningExecution(execution.status),
      ),
    [mergedPendingGeraLandingImageGenerationExecutions],
  );
  const historyGeraLandingImageGenerationExecutions = useMemo(() => {
    const completedHistory = (
      completedGeraLandingImageGenerationExecutions ?? []
    ).filter(
      (execution) =>
        isCompletedExecution(execution.status) ||
        hasFailedExecution(execution.status),
    );
    const failedFromPending = (
      pendingGeraLandingImageGenerationExecutions ?? []
    ).filter((execution) => hasFailedExecution(execution.status));

    const sortedExecutions = [...failedFromPending, ...completedHistory].sort(
      (leftExecution, rightExecution) => {
        const leftTimestamp = Date.parse(
          leftExecution.executionRequestedAt ?? "",
        );
        const rightTimestamp = Date.parse(
          rightExecution.executionRequestedAt ?? "",
        );
        const normalizedLeftTimestamp = Number.isNaN(leftTimestamp)
          ? 0
          : leftTimestamp;
        const normalizedRightTimestamp = Number.isNaN(rightTimestamp)
          ? 0
          : rightTimestamp;

        return normalizedRightTimestamp - normalizedLeftTimestamp;
      },
    );

    return sortedExecutions.filter(
      (execution, index, allExecutions) =>
        allExecutions.findIndex(
          (candidate) => candidate.idJob === execution.idJob,
        ) === index,
    );
  }, [
    completedGeraLandingImageGenerationExecutions,
    pendingGeraLandingImageGenerationExecutions,
  ]);
  const mergedPendingGeraLandingQualityReviewExecutions = useMemo(
    () =>
      mergeOptimisticExecution(
        optimisticQualityReviewExecution,
        pendingGeraLandingQualityReviewExecutions,
        completedGeraLandingQualityReviewExecutions,
      ),
    [
      optimisticQualityReviewExecution,
      pendingGeraLandingQualityReviewExecutions,
      completedGeraLandingQualityReviewExecutions,
    ],
  );

  useEffect(() => {
    if (!optimisticQualityReviewExecution) {
      return;
    }
    const persistedExecution =
      hasExecutionWithJobId(
        pendingGeraLandingQualityReviewExecutions,
        optimisticQualityReviewExecution.idJob,
      ) ||
      hasExecutionWithJobId(
        completedGeraLandingQualityReviewExecutions,
        optimisticQualityReviewExecution.idJob,
      );
    if (persistedExecution) {
      setOptimisticQualityReviewExecution(null);
    }
  }, [
    optimisticQualityReviewExecution,
    pendingGeraLandingQualityReviewExecutions,
    completedGeraLandingQualityReviewExecutions,
  ]);

  const hasRunningGeraLandingQualityReviewExecution =
    mergedPendingGeraLandingQualityReviewExecutions.some((execution) =>
      isRunningExecution(execution.status),
    );
  const runningGeraLandingQualityReviewExecutions = useMemo(
    () =>
      mergedPendingGeraLandingQualityReviewExecutions.filter((execution) =>
        isRunningExecution(execution.status),
      ),
    [mergedPendingGeraLandingQualityReviewExecutions],
  );
  const historyGeraLandingQualityReviewExecutions = useMemo(() => {
    const completedHistory = (
      completedGeraLandingQualityReviewExecutions ?? []
    ).filter(
      (execution) =>
        isCompletedExecution(execution.status) ||
        hasFailedExecution(execution.status),
    );
    const failedFromPending = (
      pendingGeraLandingQualityReviewExecutions ?? []
    ).filter((execution) => hasFailedExecution(execution.status));

    const sortedExecutions = [...failedFromPending, ...completedHistory].sort(
      (leftExecution, rightExecution) => {
        const leftTimestamp = Date.parse(
          leftExecution.executionRequestedAt ?? "",
        );
        const rightTimestamp = Date.parse(
          rightExecution.executionRequestedAt ?? "",
        );
        const normalizedLeftTimestamp = Number.isNaN(leftTimestamp)
          ? 0
          : leftTimestamp;
        const normalizedRightTimestamp = Number.isNaN(rightTimestamp)
          ? 0
          : rightTimestamp;

        return normalizedRightTimestamp - normalizedLeftTimestamp;
      },
    );

    return sortedExecutions.filter(
      (execution, index, allExecutions) =>
        allExecutions.findIndex(
          (candidate) => candidate.idJob === execution.idJob,
        ) === index,
    );
  }, [
    completedGeraLandingQualityReviewExecutions,
    pendingGeraLandingQualityReviewExecutions,
  ]);
  const latestCompletedQualityReviewExecution =
    historyGeraLandingQualityReviewExecutions.find((execution) =>
      isCompletedExecution(execution.status),
    );

  const mergedPendingGeraLandingDeliverablesExecutions = useMemo(
    () =>
      mergeOptimisticExecution(
        optimisticDeliverablesExecution,
        pendingGeraLandingDeliverablesExecutions,
        completedGeraLandingDeliverablesExecutions,
      ),
    [
      optimisticDeliverablesExecution,
      pendingGeraLandingDeliverablesExecutions,
      completedGeraLandingDeliverablesExecutions,
    ],
  );

  useEffect(() => {
    if (!optimisticDeliverablesExecution) {
      return;
    }
    const persistedExecution =
      hasExecutionWithJobId(
        pendingGeraLandingDeliverablesExecutions,
        optimisticDeliverablesExecution.idJob,
      ) ||
      hasExecutionWithJobId(
        completedGeraLandingDeliverablesExecutions,
        optimisticDeliverablesExecution.idJob,
      );
    if (persistedExecution) {
      setOptimisticDeliverablesExecution(null);
    }
  }, [
    optimisticDeliverablesExecution,
    pendingGeraLandingDeliverablesExecutions,
    completedGeraLandingDeliverablesExecutions,
  ]);

  const hasRunningGeraLandingDeliverablesExecution =
    mergedPendingGeraLandingDeliverablesExecutions.some((execution) =>
      isRunningExecution(execution.status),
    );
  const runningGeraLandingDeliverablesExecutions = useMemo(
    () =>
      mergedPendingGeraLandingDeliverablesExecutions.filter((execution) =>
        isRunningExecution(execution.status),
      ),
    [mergedPendingGeraLandingDeliverablesExecutions],
  );
  const historyGeraLandingDeliverablesExecutions = useMemo(() => {
    const completedHistory = (
      completedGeraLandingDeliverablesExecutions ?? []
    ).filter(
      (execution) =>
        isCompletedExecution(execution.status) ||
        hasFailedExecution(execution.status),
    );
    const failedFromPending = (
      pendingGeraLandingDeliverablesExecutions ?? []
    ).filter((execution) => hasFailedExecution(execution.status));

    const sortedExecutions = [...failedFromPending, ...completedHistory].sort(
      (leftExecution, rightExecution) => {
        const leftTimestamp = Date.parse(
          leftExecution.executionRequestedAt ?? "",
        );
        const rightTimestamp = Date.parse(
          rightExecution.executionRequestedAt ?? "",
        );
        const normalizedLeftTimestamp = Number.isNaN(leftTimestamp)
          ? 0
          : leftTimestamp;
        const normalizedRightTimestamp = Number.isNaN(rightTimestamp)
          ? 0
          : rightTimestamp;

        return normalizedRightTimestamp - normalizedLeftTimestamp;
      },
    );

    return sortedExecutions.filter(
      (execution, index, allExecutions) =>
        allExecutions.findIndex(
          (candidate) => candidate.idJob === execution.idJob,
        ) === index,
    );
  }, [
    completedGeraLandingDeliverablesExecutions,
    pendingGeraLandingDeliverablesExecutions,
  ]);

  const selectedGeraLandingModelByStage = useMemo(() => {
    return new Map(
      (geraLandingStageModels ?? []).map((stageModel) => [
        stageModel.stageCode,
        stageModel,
      ]),
    );
  }, [geraLandingStageModels]);

  const renderSelectedGeraLandingModel = (stageCode: string) => {
    const selectedStageModel = selectedGeraLandingModelByStage.get(stageCode);
    const selectedModelLabel = formatPipelineStageModel(selectedStageModel);
    const pricingModeLabel = selectedStageModel?.pricingMode
      ? selectedStageModel.pricingMode.toUpperCase()
      : "FLEX";

    return (
      <div className="small text-muted d-flex flex-column gap-1">
        <div>
          Modelo selecionado no pipeline:{" "}
          <span className="fw-semibold text-body">
            {isLoadingStageModels
              ? "Carregando..."
              : (selectedModelLabel ?? "GPT-5.2 (gpt-5.2)")}
          </span>
          {!isLoadingStageModels && selectedStageModel?.defaultModelApplied ? (
            <span className="badge text-bg-secondary ms-2">default</span>
          ) : null}
        </div>
        <div className="d-flex flex-wrap align-items-center gap-2">
          <span className="badge text-bg-light border">
            Gera:{" "}
            {formatGeneratedAssetType(selectedStageModel?.generatedAssetType)}
          </span>
          <span className="badge text-bg-light border">
            Modo: {pricingModeLabel}
          </span>
          <span>
            Entrada:{" "}
            {formatModelCostPerMillion(selectedStageModel?.priceInputFlex)}/1M
            tokens
          </span>
          <span>
            Cache:{" "}
            {formatModelCostPerMillion(
              selectedStageModel?.priceInputCachedFlex,
            )}
            /1M tokens
          </span>
          <span>
            Saída:{" "}
            {formatModelCostPerMillion(selectedStageModel?.priceOutputFlex)}/1M
            tokens
          </span>
        </div>
      </div>
    );
  };

  const latestSuccessfulImagePromptExecution =
    historyGeraLandingImagePromptsExecutions.find((execution) =>
      isCompletedExecution(execution.status),
    );
  const runningGeraLandingJobId = mergedPendingGeraLandingExecutions.find(
    (execution) => isRunningExecution(execution.status),
  )?.idJob;

  const formatCurrencyUsd = (value?: number | null) =>
    value != null
      ? new Intl.NumberFormat("en-US", {
          style: "currency",
          currency: "USD",
        }).format(value)
      : "—";

  const resolveExecutionCostUsd = (
    execution: GeraLandingStageExecutionItem,
  ) => {
    const candidateValues = [
      execution.costUsd,
      (
        execution as GeraLandingStageExecutionItem & {
          totalCostUsd?: number | string | null;
        }
      ).totalCostUsd,
      (
        execution as GeraLandingStageExecutionItem & {
          cost?: number | string | null;
        }
      ).cost,
      (
        execution as GeraLandingStageExecutionItem & {
          totalCost?: number | string | null;
        }
      ).totalCost,
    ];

    for (const candidate of candidateValues) {
      if (candidate == null) continue;
      if (typeof candidate === "number" && Number.isFinite(candidate))
        return candidate;
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
  const totalCompletedGeraLandingCopyCostUsd =
    historyGeraLandingCopyExecutions.reduce(
      (sum, execution) => sum + (resolveExecutionCostUsd(execution) ?? 0),
      0,
    );
  const totalCompletedGeraLandingDesignPresetCostUsd =
    historyGeraLandingDesignPresetExecutions.reduce(
      (sum, execution) => sum + (resolveExecutionCostUsd(execution) ?? 0),
      0,
    );
  const totalCompletedGeraLandingAllStagesCostUsd =
    totalCompletedGeraLandingCostUsd +
    totalCompletedGeraLandingCopyCostUsd +
    totalCompletedGeraLandingDesignPresetCostUsd +
    historyGeraLandingQualityReviewExecutions.reduce(
      (sum, execution) => sum + (resolveExecutionCostUsd(execution) ?? 0),
      0,
    ) +
    historyGeraLandingDeliverablesExecutions.reduce(
      (sum, execution) => sum + (resolveExecutionCostUsd(execution) ?? 0),
      0,
    ) +
    historyGeraLandingImagePromptsExecutions.reduce(
      (sum, execution) => sum + (resolveExecutionCostUsd(execution) ?? 0),
      0,
    ) +
    historyGeraLandingImageGenerationExecutions.reduce(
      (sum, execution) => sum + (resolveExecutionCostUsd(execution) ?? 0),
      0,
    );
  const { data: runningGeraLandingJobDetail } =
    useGeraLandingStageExecutionDetail(
      expId,
      runningGeraLandingJobId,
      undefined,
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
      hasSentGeraLandingBackgroundNotificationRef.current = false;
      return;
    }

    if (!hadRunningGeraLandingExecutionRef.current) return;

    hadRunningGeraLandingExecutionRef.current = false;
    void refetchCompletedGeraLandingExecutions();

    const backgroundTab =
      document.hidden || document.visibilityState !== "visible";
    if (!backgroundTab || hasSentGeraLandingBackgroundNotificationRef.current) {
      return;
    }

    hasSentGeraLandingBackgroundNotificationRef.current = true;
    const title = "Gera WireFrame finalizado";
    const body =
      "A geração da etapa 1 terminou. Volte para revisar o resultado.";

    if ("Notification" in window) {
      if (Notification.permission === "granted") {
        new Notification(title, { body });
        return;
      }
      if (Notification.permission === "default") {
        void Notification.requestPermission().then((permission) => {
          if (permission === "granted") {
            new Notification(title, { body });
          }
        });
      }
    }

    toast.info(`${title}: ${body}`);
  }, [hasRunningGeraLandingExecution, refetchCompletedGeraLandingExecutions]);
  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p>Não encontrado</p>;
  const alterationLocked = isExperimentAlterationLocked(data);
  const hasPublishedFacebookCampaigns = Boolean(facebookCampaigns?.length);
  const showGeraLandingStartButtons = !Boolean(data.facebookReleaseRequestedAt);
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
    alterationLocked ||
    isFetchingResetPreview ||
    !hasItemsToReset ||
    Boolean(previewErrorMessage) ||
    resetCampaigns.isPending;
  const formatCurrency = (n?: number | null) =>
    n != null
      ? new Intl.NumberFormat("pt-BR", {
          style: "currency",
          currency: "BRL",
        }).format(n)
      : "—";
  const formatPercent = (n?: number | null) => (n != null ? `${n}%` : "—");
  const formatCurrencyValue = (value: number, currency: "BRL" | "USD") =>
    new Intl.NumberFormat(currency === "BRL" ? "pt-BR" : "en-US", {
      style: "currency",
      currency,
    }).format(value);
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
  const diagnosticsArtifacts = diagnostics?.artifacts ?? [];
  const shouldShowDiagnostics =
    !!diagnostics &&
    (diagnostics.headline ?? "").trim().toLowerCase() !==
      "nenhuma inconsistência detectada";
  const baseKpi = data.kpiTarget ?? data.kpiTargetCpl;
  const stopLossFactor = preset?.stopLossFactor;
  const stopLossCpl =
    data.stopLossCpl ??
    (baseKpi != null && stopLossFactor != null
      ? baseKpi * stopLossFactor
      : null);
  const salesPagePublications = geraSalesPagePublications.data ?? [];
  const latestSalesPagePublication = salesPagePublications[0];
  const contentPipelineCostUsd =
    experimentPipelineJobHistory.data?.content.reduce(
      (sum, job) => sum + (job.costUsd ?? 0),
      0,
    ) ?? 0;
  const geraSalesPageCostUsd = salesPagePublications.reduce(
    (sum, publication) =>
      sum +
      publication.stages.reduce(
        (stageSum, stage) => stageSum + (stage.costUsd ?? 0),
        0,
      ),
    0,
  );
  const experimentCostSummary = buildExperimentCostSummary({
    experiment: data,
    contentPipelineCostUsd,
    geraLandingCostUsd: totalCompletedGeraLandingAllStagesCostUsd,
    geraSalesPageCostUsd,
  });
  const experimentPage = data.facebookPage;
  const instagramAccount = data.instagramAccount;
  const hasCreativesReady =
    readinessSummary?.hasCreatives ?? data.creativeApproved;
  const readinessCreativeCount = readinessSummary?.creativeCount ?? 0;
  const hasPublisherTargeting = readinessSummary?.hasCompleteTargeting ?? false;
  const openExperimentTab = (targetTab: string) => {
    setTab(targetTab);
    window.requestAnimationFrame(() => {
      tabsSectionRef.current?.scrollIntoView({
        behavior: "smooth",
        block: "start",
      });
    });
  };
  const openLandingActions = () => openExperimentTab("landing");

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
      id: "landing-destination",
      title: "Landing criada e aprovada",
      isMet: Boolean(data.followUpActionUrl),
      isLoading: isLoadingReadiness,
      hint: data.followUpActionUrl
        ? `Landing aprovada e URL de destino ativa: ${data.followUpActionUrl}.`
        : "Aprove uma landing na aba Landing para definir a URL de destino da campanha.",
      action: data.followUpActionUrl ? undefined : openLandingActions,
      actionLabel: data.followUpActionUrl ? undefined : "Ir para Landing",
    },
    {
      id: "publisher-targeting",
      title: "Público salvo para publicação",
      isMet: hasPublisherTargeting,
      isLoading: isLoadingReadiness,
      hint: isLoadingReadiness
        ? "Verificando público salvo para o publicador..."
        : hasPublisherTargeting
          ? "Público salvo atende a regra do Facebook Ads Worker."
          : "Salve na aba Público pelo menos um interesse, cargo ou comportamento válido para este experimento.",
      action: hasPublisherTargeting
        ? undefined
        : () => openExperimentTab("publico"),
      actionLabel: hasPublisherTargeting ? undefined : "Ir para Público",
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
    releaseInProgress ||
    !canReleaseExperiment ||
    isLoadingReadiness ||
    alterationLocked;

  const hasExperimentPipelineContent = Boolean(
    data.campaignAngle ||
    data.adCopy ||
    data.adImageBriefing ||
    data.creativeTextPrompt ||
    data.creativeImagePrompt,
  );
  const hasGeraLandingPipelineReady =
    readinessSummary?.hasGeraLandingPipeline ?? false;
  const geraLandingCompletedStageCount =
    readinessSummary?.geraLandingCompletedStageCount ?? 0;
  const geraLandingRequiredStageCount =
    readinessSummary?.geraLandingRequiredStageCount ?? 7;
  const hasAtLeastThreeApprovedCreatives = readinessCreativeCount >= 3;
  const hasAudienceSelection = hasPublisherTargeting;
  const isLowTicketProduct = data.experimentType === "LOW_TICKET_PRODUCT";
  const experimentTypeLabel = isLowTicketProduct
    ? "Produto low-ticket"
    : "Teste de nicho / lead";
  const productAiSubtypeLabel: Record<string, string> = {
    AI_VISUAL_PREVIEW: "Prévia visual IA",
    AI_PERSONALIZED_SAMPLE: "Amostra personalizada IA",
    AI_TRANSFORMATION_SIMULATOR: "Simulador de transformação IA",
    AI_VISUAL_ASSET_PACK: "Pacote visual IA",
    AI_IDENTITY_AVATAR_PRODUCT: "Identidade/avatar IA",
    AI_REPORT_VISUAL_EVIDENCE: "Relatório com evidência visual IA",
  };
  const campaignObjectiveLabel =
    data.campaignObjective === "LEADS"
      ? "Leads"
      : data.campaignObjective === "SALES"
        ? "Vendas"
        : data.campaignObjective || "—";
  const lowTicketSalePreparationChecklist = [
    {
      id: "commercial-offer",
      title: "Oferta comercial montada",
      detail: "Dor, produto, promessa e CTA de checkout",
      isMet: hasExperimentPipelineContent,
      isLoading: false,
      actionLabel: "Abrir estrutura",
      action: () => openExperimentTab("content-structure"),
    },
    {
      id: "sales-configuration",
      title: "Experimento configurado para venda",
      detail:
        campaignObjectiveLabel === "Vendas"
          ? "Objetivo de campanha marcado como vendas"
          : "Ajuste o objetivo para vendas antes de liberar tráfego",
      isMet: campaignObjectiveLabel === "Vendas",
      isLoading: false,
      actionLabel: "Ver overview",
      action: () => openExperimentTab("overview"),
    },
    {
      id: "sales-page",
      title: "Página de venda cadastrada",
      detail: data.followUpActionUrl
        ? "URL final cadastrada para a campanha"
        : "Cadastre a página de venda com checkout ativo",
      isMet: Boolean(data.followUpActionUrl),
      isLoading: isLoadingReadiness,
      actionLabel: "Abrir landing",
      action: openLandingActions,
    },
    {
      id: "approved-creatives",
      title: "Criativos prontos",
      detail: `${readinessCreativeCount}/3 criativos aprovados`,
      isMet: hasAtLeastThreeApprovedCreatives,
      isLoading: isLoadingReadiness,
      actionLabel: "Abrir criativos",
      action: () => openExperimentTab("creatives"),
    },
    {
      id: "audience-selection",
      title: "Público salvo",
      detail: hasAudienceSelection
        ? "Público salvo atende a regra do publicador"
        : "Salve um público válido para entrar na fila do publicador",
      isMet: hasAudienceSelection,
      isLoading: isLoadingReadiness,
      actionLabel: "Abrir público",
      action: () => openExperimentTab("publico"),
    },
    {
      id: "controlled-release",
      title: "Pronto para campanha controlada",
      detail: isReadyForFacebook
        ? "Checklist mínimo concluído para liberação controlada"
        : "Conclua página, criativos e público antes de liberar",
      isMet: isReadyForFacebook,
      isLoading: isLoadingReadiness,
      actionLabel: "Ver liberação",
      action: () => window.scrollTo({ top: 0, behavior: "smooth" }),
    },
  ];
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
  const rows = [
    {
      label: "Tipo de experimento",
      value: experimentTypeLabel,
    },
    {
      label: "Nicho",
      value: <Link to={`/niches/${data.nicheId}`}>{niche?.name}</Link>,
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
      label: "Dor única",
      value: data.singlePain || "—",
    },
    {
      label: isLowTicketProduct ? "Prova/preview da oferta" : "Isca digital",
      value:
        data.freeReward ||
        (isLowTicketProduct ? "Sem prova/preview informada" : "—"),
    },
    {
      label: "Promessa do funil",
      value: data.funnelPromise || "—",
    },
    {
      label: "CTA principal",
      value: data.primaryCta || "—",
    },
    {
      label: "Objetivo da campanha",
      value: campaignObjectiveLabel,
    },
    { label: "Custo rastreável", value: formatCurrency(data.cost) },
    {
      label: "Criativos aprovados",
      value: `${readinessCreativeCount}/3`,
    },
    {
      label: "Público para publicação",
      value: hasPublisherTargeting ? "Salvo" : "Pendente",
    },
    {
      label: "Página de venda",
      value: data.followUpActionUrl ? (
        <a href={data.followUpActionUrl} target="_blank" rel="noreferrer">
          Abrir página
        </a>
      ) : (
        "Pendente"
      ),
    },
    { label: "Plataforma", value: data.platform },
    { label: "Início", value: data.startDate },
    { label: "Término", value: data.endDate },
  ];
  return (
    <div>
      <div className="d-flex justify-content-between align-items-start">
        <div>
          <PageTitle icon={experimentIcon}>{data.name}</PageTitle>
          {data.creationSource === "MANUAL_FLOW" ? (
            <span className="badge text-bg-warning mb-2">Fluxo manual</span>
          ) : null}
          <p className="text-muted mb-0">{data.hypothesis}</p>
        </div>
        <div className="d-flex align-items-center">
          {alterationLocked ? (
            <span
              className="btn btn-outline-secondary disabled me-2"
              aria-disabled="true"
              title="Alterações desabilitadas após a liberação/publicação do experimento."
            >
              Editar
            </span>
          ) : (
            <Link to="edit" className="btn btn-outline-secondary me-2">
              Editar
            </Link>
          )}
          {canDownloadCompleteReport ? (
            <button
              type="button"
              className="btn btn-success me-2"
              onClick={handleDownloadCompleteReport}
              disabled={completeMarkdownReport.isPending}
            >
              {completeMarkdownReport.isPending ? (
                <>
                  <span
                    className="spinner-border spinner-border-sm me-2"
                    role="status"
                    aria-hidden="true"
                  />
                  Gerando...
                </>
              ) : (
                "Relatório completo (.md)"
              )}
            </button>
          ) : null}
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
      ) : shouldShowDiagnostics ? (
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
                <div
                  className="alert alert-light border small mb-2"
                  role="alert"
                >
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
                      <strong>Job ID:</strong>{" "}
                      {diagnostics.failureDetails.jobId ? (
                        <code>{diagnostics.failureDetails.jobId}</code>
                      ) : (
                        "—"
                      )}
                    </li>
                    <li>
                      <strong>Horário do erro:</strong>{" "}
                      {diagnostics.failureDetails.occurredAt
                        ? formatDateTimeValue(
                            diagnostics.failureDetails.occurredAt,
                          )
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
          {diagnosticsArtifacts.length > 0 ? (
            <>
              <p className="mb-2 mt-2 small">
                <strong>Itens com pendência para corrigir:</strong>
              </p>
              <ul className="mb-0 small ps-3">
                {diagnosticsArtifacts.map((artifact) => (
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
      {false ? (
        <div className="card border-0 shadow-sm rounded-3 mt-3">
          <div className="card-body">
            <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap">
              <div>
                <h5 className="card-title mb-1">
                  Preparação da venda low-ticket
                </h5>
                <p className="text-muted small mb-0">
                  Visão rápida do que já foi montado e checado antes de liberar
                  tráfego para compra direta.
                </p>
              </div>
              <span className="badge text-bg-light border text-body">
                {
                  lowTicketSalePreparationChecklist.filter((item) => item.isMet)
                    .length
                }
                /{lowTicketSalePreparationChecklist.length} concluídos
              </span>
            </div>
            <div className="row g-3 mt-1">
              {lowTicketSalePreparationChecklist.map((item) => (
                <div key={item.id} className="col-12 col-md-6 col-xl-4">
                  <button
                    type="button"
                    className={`w-100 h-100 text-start border rounded-3 p-3 bg-body ${
                      item.isMet
                        ? "border-success-subtle"
                        : "border-warning-subtle"
                    }`}
                    onClick={item.action}
                  >
                    <div className="d-flex align-items-start gap-2">
                      <span
                        className={`badge rounded-pill ${
                          item.isLoading
                            ? "text-bg-secondary"
                            : item.isMet
                              ? "text-bg-success"
                              : "text-bg-warning"
                        }`}
                        aria-label={item.isMet ? "Concluído" : "Pendente"}
                      >
                        {item.isLoading ? "…" : item.isMet ? "✓" : "!"}
                      </span>
                      <span>
                        <span className="fw-semibold d-block">
                          {item.title}
                        </span>
                        <span className="small text-muted d-block mt-1">
                          {item.detail}
                        </span>
                        <span className="small text-primary d-block mt-2">
                          {item.actionLabel}
                        </span>
                      </span>
                    </div>
                  </button>
                </div>
              ))}
            </div>
            <div className="alert alert-light border small text-muted mt-3 mb-0">
              Página de obrigado premium e ZIP de entrega ainda dependem de
              validação operacional fora deste contrato persistido do backend.
            </div>
          </div>
        </div>
      ) : null}
      {false ? (
        <div className="card border-0 shadow-sm rounded-3 mt-3">
          <div className="card-body">
            <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap">
              <div>
                <h5 className="card-title mb-1">
                  Auditoria da página de venda
                </h5>
                <p className="text-muted small mb-0">
                  Versões publicadas pelo GeraSalesPage v1 com prompts, schemas,
                  modelo e request usados em cada etapa.
                </p>
              </div>
              <span className="badge text-bg-light border text-body">
                {geraSalesPagePublications.isLoading
                  ? "Carregando"
                  : `${salesPagePublications.length} versão(ões)`}
              </span>
            </div>
            {geraSalesPagePublications.isLoading ? (
              <div className="text-muted small mt-3">
                Carregando auditoria da página...
              </div>
            ) : latestSalesPagePublication ? (
              <div className="mt-3">
                <div className="row g-3">
                  <div className="col-12 col-lg-4">
                    <div className="border rounded-3 p-3 h-100">
                      <div className="text-muted small">Última publicação</div>
                      <div className="fw-semibold">
                        {formatDateTimeValue(
                          latestSalesPagePublication.publishedAt,
                        )}
                      </div>
                      <div className="text-muted small mt-2">Job final</div>
                      <code className="small text-break">
                        {latestSalesPagePublication.publicationJobId}
                      </code>
                    </div>
                  </div>
                  <div className="col-12 col-lg-4">
                    <div className="border rounded-3 p-3 h-100">
                      <div className="text-muted small">Página de venda</div>
                      {latestSalesPagePublication.salesPageUrl ? (
                        <a
                          href={latestSalesPagePublication.salesPageUrl ?? undefined}
                          target="_blank"
                          rel="noreferrer"
                          className="small text-break"
                        >
                          {latestSalesPagePublication.salesPageUrl}
                        </a>
                      ) : (
                        <span className="text-muted small">Não registrada</span>
                      )}
                    </div>
                  </div>
                  <div className="col-12 col-lg-4">
                    <div className="border rounded-3 p-3 h-100">
                      <div className="text-muted small">Checkout usado</div>
                      {latestSalesPagePublication.checkoutUrl ? (
                        <a
                          href={latestSalesPagePublication.checkoutUrl ?? undefined}
                          target="_blank"
                          rel="noreferrer"
                          className="small text-break"
                        >
                          {latestSalesPagePublication.checkoutUrl}
                        </a>
                      ) : (
                        <span className="text-muted small">Não registrado</span>
                      )}
                    </div>
                  </div>
                </div>
                <div className="accordion mt-3" id="gera-sales-page-audit">
                  {latestSalesPagePublication.stages.map((stage, index) => (
                    <div className="accordion-item" key={stage.idJob}>
                      <h2
                        className="accordion-header"
                        id={`audit-stage-${stage.idJob}`}
                      >
                        <button
                          className={`accordion-button ${
                            openAuditStageJobId === stage.idJob ||
                            (!openAuditStageJobId && index === 0)
                              ? ""
                              : "collapsed"
                          }`}
                          type="button"
                          aria-expanded={
                            openAuditStageJobId === stage.idJob ||
                            (!openAuditStageJobId && index === 0)
                          }
                          aria-controls={`audit-stage-body-${stage.idJob}`}
                          onClick={() => setOpenAuditStageJobId(stage.idJob)}
                        >
                          <span className="fw-semibold me-2">
                            {stage.stageCode}
                          </span>
                          <span className="badge text-bg-light border text-body me-2">
                            {stage.openAiModel || "modelo não registrado"}
                          </span>
                          <span className="text-muted small">
                            {stage.completedAt
                              ? formatDateTimeValue(stage.completedAt)
                              : "sem data"}
                          </span>
                        </button>
                      </h2>
                      <div
                        id={`audit-stage-body-${stage.idJob}`}
                        className={`accordion-collapse collapse ${
                          openAuditStageJobId === stage.idJob ||
                          (!openAuditStageJobId && index === 0)
                            ? "show"
                            : ""
                        }`}
                        aria-labelledby={`audit-stage-${stage.idJob}`}
                      >
                        <div className="accordion-body">
                          <div className="row g-3">
                            <div className="col-12 col-xl-6">
                              <h6>Prompt renderizado</h6>
                              <CollapsibleJsonViewer
                                content={stage.prompt}
                                parseAsJson={false}
                                initiallyCollapsed
                              />
                            </div>
                            <div className="col-12 col-xl-6">
                              <h6>Schema JSON</h6>
                              <CollapsibleJsonViewer
                                content={stage.schemaJson}
                                initiallyCollapsed
                              />
                            </div>
                            <div className="col-12 col-xl-6">
                              <h6>Prompt markdown base</h6>
                              <CollapsibleJsonViewer
                                content={stage.promptMarkdownContent}
                                parseAsJson={false}
                                initiallyCollapsed
                              />
                            </div>
                            <div className="col-12 col-xl-6">
                              <h6>Request enviado</h6>
                              <CollapsibleJsonViewer
                                content={stage.openAiRequestBody}
                                initiallyCollapsed
                              />
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ) : (
              <div className="alert alert-light border small text-muted mt-3 mb-0">
                Ainda não existe página publicada pelo GeraSalesPage v1 com
                snapshot de prompts e schemas. Execute ou refaça o pipeline para
                registrar a auditoria.
              </div>
            )}
          </div>
        </div>
      ) : null}
      <div className="card border-0 shadow-sm rounded-3 mt-3">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-start">
            <h5 className="card-title mb-0">
              {isLowTicketProduct
                ? "Campanha de Facebook Ads para venda"
                : "Campanha de Facebook Ads"}
            </h5>
            <span
              className={`badge ${
                isReadyForFacebook ? "text-bg-success" : "text-bg-warning"
              }`}
            >
              {isReadyForFacebook ? "Pronto" : "Pendente"}
            </span>
          </div>
          <p className="card-text mt-2">
            {isLowTicketProduct
              ? "Checklist consolidado para publicar anúncio, página curta, checkout e entrega com foco na primeira compra."
              : "Checklist consolidado das regras de publicação. Ele reflete o documento interno e o diagnóstico automático do worker."}
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
          ) : null}
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
                ? isLowTicketProduct
                  ? "Ao liberar, o status muda para Planejado e a campanha será preparada para objetivo de vendas."
                  : "Ao liberar, o status muda para Planejado e o funil de vendas é zerado antes da publicação."
                : "Resolva os bloqueios para habilitar a liberação automática."}
              {lastReleaseLabel ? (
                <div className="mt-1">
                  Última liberação: <strong>{lastReleaseLabel}</strong>
                </div>
              ) : null}
            </div>
          </div>
        </div>
      </div>
      {false ? (
      <div className="card border-0 shadow-sm rounded-3 mt-3">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap mb-3">
            <div>
              <h5 className="card-title mb-1">Campanhas e funis publicados</h5>
              <p className="text-muted small mb-0">
                Campanhas, conjuntos, anúncios e etapas de funil atribuídas por
                código de rastreamento.
              </p>
            </div>
            <span className="badge text-bg-light border text-body">
              {isLoadingFacebookCampaigns
                ? "Carregando"
                : `${facebookCampaigns?.length ?? 0} campanha(s)`}
            </span>
          </div>
          {isLoadingFacebookCampaigns ? (
            <div className="text-muted small">Carregando campanhas...</div>
          ) : facebookCampaigns?.length ? (
            <div className="d-flex flex-column gap-3">
              {(facebookCampaigns ?? []).map((campaign) => (
                <div key={campaign.id} className="border rounded-3 p-3">
                  <div className="d-flex justify-content-between gap-3 flex-wrap">
                    <div>
                      <div className="fw-semibold">{campaign.name}</div>
                      <div className="text-muted small">
                        {campaign.objective} · {campaign.status}
                        {campaign.createdAt
                          ? ` · ${formatDateTimeValue(campaign.createdAt)}`
                          : ""}
                      </div>
                      {campaign.metricsLastError ? (
                        <div className="text-danger small mt-1">
                          {campaign.metricsLastError}
                        </div>
                      ) : null}
                    </div>
                    <code className="small text-break">{campaign.id}</code>
                  </div>
                  {campaign.issues?.length ? (
                    <div className="alert alert-warning py-2 small mt-3 mb-0">
                      {campaign.issues.join(" ")}
                    </div>
                  ) : null}
                  <div className="d-flex flex-column gap-2 mt-3">
                    {campaign.adSets.map((adSet) => (
                      <div key={adSet.id} className="border rounded-3 p-3">
                        <div className="d-flex justify-content-between gap-3 flex-wrap">
                          <div>
                            <div className="fw-semibold">{adSet.name}</div>
                            <div className="text-muted small">
                              {adSet.status}
                              {adSet.createdAt
                                ? ` · ${formatDateTimeValue(adSet.createdAt)}`
                                : ""}
                              {adSet.experimentAdSetId
                                ? ` · Público #${adSet.experimentAdSetId}`
                                : ""}
                            </div>
                          </div>
                          <code className="small text-break">{adSet.id}</code>
                        </div>
                        {adSet.issues?.length ? (
                          <div className="alert alert-warning py-2 small mt-2 mb-0">
                            {adSet.issues.join(" ")}
                          </div>
                        ) : null}
                        {adSet.ads.length ? (
                          <div className="table-responsive mt-3">
                            <table className="table table-sm align-middle mb-0">
                              <thead>
                                <tr>
                                  <th>Anúncio</th>
                                  <th>Status</th>
                                  <th>Código</th>
                                  <th>Funil atribuído</th>
                                </tr>
                              </thead>
                              <tbody>
                                {adSet.ads.map((ad) => (
                                  <tr key={ad.id}>
                                    <td>
                                      <div className="fw-semibold">
                                        {ad.name}
                                      </div>
                                      <code className="small text-break">
                                        {ad.id}
                                      </code>
                                    </td>
                                    <td>{ad.status}</td>
                                    <td>
                                      <code className="small text-break">
                                        {ad.trackingCode || "—"}
                                      </code>
                                    </td>
                                    <td>
                                      {ad.funnelStages.length ? (
                                        <div className="d-flex flex-column gap-1">
                                          {ad.funnelStages.map((stage) => (
                                            <div
                                              key={`${ad.id}-${stage.stage}`}
                                              className="d-flex justify-content-between gap-3 small"
                                            >
                                              <span>
                                                {stage.order}. {stage.label}
                                              </span>
                                              <strong>
                                                {stage.totalCount}
                                              </strong>
                                            </div>
                                          ))}
                                        </div>
                                      ) : (
                                        <span className="text-muted small">
                                          Sem eventos atribuídos
                                        </span>
                                      )}
                                    </td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          </div>
                        ) : null}
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="alert alert-light border small text-muted mb-0">
              Nenhuma campanha publicada encontrada para este experimento.
            </div>
          )}
        </div>
      </div>
      ) : null}
      <div ref={tabsSectionRef}>
        <Tabs.Root value={tab} onValueChange={setTab} className="mt-3">
          <Tabs.List className="nav nav-tabs experiment-detail-tabs">
            {experimentDetailTabs.map((item) => (
              <Tabs.Trigger
                key={item.value}
                value={item.value}
                className={`nav-link experiment-detail-tabs__item${
                  tab === item.value ? " active" : ""
                }`}
              >
                {item.label}
              </Tabs.Trigger>
            ))}
          </Tabs.List>
          <Tabs.Content value="overview" asChild>
            <div className="d-flex flex-column gap-3">
              <ExperimentRunPanel experimentId={expId} compact />
              <div className="card">
                <div className="card-body">
                  <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap mb-3">
                    <div>
                      <h5 className="card-title mb-1">Custos do experimento</h5>
                      <p className="text-muted small mb-0">
                        Total rastreável em BRL separado da auditoria técnica em
                        USD.
                      </p>
                    </div>
                    <div className="text-end">
                      <div className="text-muted small">Custo rastreável</div>
                      <div className="fs-4 fw-semibold">
                        {formatCurrencyValue(
                          experimentCostSummary.auditableTotalBrl,
                          "BRL",
                        )}
                      </div>
                    </div>
                  </div>
                  <div className="row g-3">
                    {experimentCostSummary.brlRows.map((costRow) => (
                      <div
                        key={costRow.label}
                        className="col-12 col-md-6 col-xl-4"
                      >
                        <div className="border rounded-3 p-3 h-100">
                          <div className="text-muted small">
                            {costRow.label}
                          </div>
                          <div className="fw-semibold">
                            {formatCurrencyValue(
                              costRow.value,
                              costRow.currency,
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                  <div className="row g-3 mt-1">
                    {experimentCostSummary.technicalRows.map((costRow) => (
                      <div
                        key={costRow.label}
                        className="col-12 col-md-6 col-xl-4"
                      >
                        <div className="border rounded-3 p-3 h-100">
                          <div className="text-muted small">
                            {costRow.label}
                          </div>
                          <div className="fw-semibold">
                            {formatCurrencyValue(
                              costRow.value,
                              costRow.currency,
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                  {experimentCostSummary.unreconciledLegacyCostBrl > 0 ? (
                    <div className="alert alert-warning mt-3 mb-0">
                      <div className="fw-semibold">
                        Custo legado não reconciliado:{" "}
                        {formatCurrencyValue(
                          experimentCostSummary.unreconciledLegacyCostBrl,
                          "BRL",
                        )}
                      </div>
                      <div className="small">
                        Total legado registrado:{" "}
                        {formatCurrencyValue(
                          experimentCostSummary.legacyTotalBrl,
                          "BRL",
                        )}
                        . Esta diferença não é exibida como custo técnico real
                        porque não tem origem auditável nas parcelas atuais.
                      </div>
                    </div>
                  ) : null}
                </div>
              </div>
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
            </div>
          </Tabs.Content>
          <Tabs.Content value="execucao" asChild>
            <div className="d-flex flex-column gap-3">
              <ExperimentRunPanel experimentId={expId} />
            </div>
          </Tabs.Content>
          <Tabs.Content value="funnel" asChild>
            <ExperimentFunnelTab
              experimentId={expId}
              experimentType={data?.experimentType}
              totalSpend={data?.campaignMetric?.spend}
              spendLastSyncedAt={data?.campaignMetric?.lastSyncedAt}
              alterationLocked={alterationLocked}
            />
          </Tabs.Content>
          <Tabs.Content value="analytics" asChild>
            <ExperimentLandingAnalyticsTab experimentId={expId} />
          </Tabs.Content>
          <Tabs.Content value="creatives" asChild>
            <CriativosTab
              experimentId={expId}
              alterationLocked={alterationLocked}
            />
          </Tabs.Content>
          <Tabs.Content value="landing" asChild>
            <LandingTab experiment={data} alterationLocked={alterationLocked} />
          </Tabs.Content>
          <Tabs.Content value="gera-landing" asChild>
            <div className="d-flex flex-column gap-3">
              <div className="card">
                <div className="card-body d-flex flex-wrap justify-content-between align-items-center gap-2">
                  <h5 className="card-title mb-0">
                    Total Gera Landing (todas as etapas)
                  </h5>
                  <span className="badge text-bg-primary fs-6 fw-semibold">
                    {formatCurrencyUsd(
                      totalCompletedGeraLandingAllStagesCostUsd,
                    )}
                  </span>
                </div>
              </div>
              <div className="card">
                <div className="card-body d-flex flex-column gap-3">
                  <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
                    <div>
                      <h5 className="card-title mb-1">1 - Gera WireFrame</h5>
                      {renderSelectedGeraLandingModel("landing-page-wireframe")}
                    </div>
                    <span className="badge text-bg-light border fs-6 fw-semibold">
                      Total execuções:{" "}
                      {formatCurrencyUsd(totalCompletedGeraLandingCostUsd)}
                    </span>
                  </div>
                  <div className="d-flex flex-column gap-3">
                    {showGeraLandingStartButtons ? (
                      <button
                        type="button"
                        className="btn btn-primary align-self-start"
                        onClick={handleStartWireframe}
                        disabled={
                          alterationLocked ||
                          isStartingWireframe ||
                          hasRunningGeraLandingExecution
                        }
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
                    ) : null}
                    {isLoadingPendingGeraLandingExecutions ? (
                      <p className="text-muted mb-0">
                        Carregando jobs da etapa...
                      </p>
                    ) : runningGeraLandingExecutions.length === 0 ? (
                      <p className="text-muted mb-0">
                        Nenhum job pendente ou em execução.
                      </p>
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
                                    to={`/experiments/${expId}/geralanding/stage-executions/${execution.idJob}?stageCode=landing-page-wireframe`}
                                    className="fw-semibold text-decoration-none"
                                  >
                                    {execution.idJob}
                                  </Link>
                                </td>
                                <td>{execution.status}</td>
                                <td>
                                  {formatDateTimeValue(
                                    execution.executionRequestedAt,
                                  )}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}

                    <div className="rounded border bg-light-subtle p-3 d-flex flex-column gap-3">
                      <h6 className="mb-0">Histórico de execuções</h6>
                      {isLoadingCompletedGeraLandingExecutions ? (
                        <p className="text-muted mb-0">
                          Carregando execuções...
                        </p>
                      ) : historyGeraLandingExecutions.length === 0 ? (
                        <p className="text-muted mb-0">
                          Nenhuma execução registrada para esta etapa.
                        </p>
                      ) : (
                        <div className="table-responsive">
                          <table className="table table-sm align-middle mb-0">
                            <thead>
                              <tr>
                                <th scope="col">Job ID</th>
                                <th scope="col">Status</th>
                                <th scope="col">Data-hora</th>
                                <th scope="col" className="text-end">
                                  Custo
                                </th>
                              </tr>
                            </thead>
                            <tbody>
                              {historyGeraLandingExecutions.map((execution) => (
                                <tr key={execution.idJob}>
                                  <td>
                                    <Link
                                      to={`/experiments/${expId}/geralanding/stage-executions/${execution.idJob}?stageCode=landing-page-wireframe`}
                                      className="fw-semibold text-decoration-none"
                                    >
                                      {execution.idJob}
                                    </Link>
                                  </td>
                                  <td>{execution.status}</td>
                                  <td>
                                    {formatDateTimeValue(
                                      execution.executionRequestedAt,
                                    )}
                                  </td>
                                  <td className="text-end">
                                    {formatCurrencyUsd(
                                      resolveExecutionCostUsd(execution),
                                    )}
                                  </td>
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

              <div className="card">
                <div className="card-body d-flex flex-column gap-3">
                  <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
                    <div>
                      <h5 className="card-title mb-1">2 - Gera Copy</h5>
                      {renderSelectedGeraLandingModel("landing-page-copy")}
                    </div>
                    <span className="badge text-bg-light border fs-6 fw-semibold">
                      Total execuções:{" "}
                      {formatCurrencyUsd(totalCompletedGeraLandingCopyCostUsd)}
                    </span>
                  </div>
                  <div className="d-flex flex-column gap-3">
                    {showGeraLandingStartButtons ? (
                      <button
                        type="button"
                        className="btn btn-primary align-self-start"
                        onClick={handleStartCopy}
                        disabled={
                          alterationLocked ||
                          isStartingCopy ||
                          hasRunningGeraLandingCopyExecution
                        }
                      >
                        {isStartingCopy ? (
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
                    ) : null}
                    {isLoadingPendingGeraLandingCopyExecutions ? (
                      <p className="text-muted mb-0">
                        Carregando jobs da etapa...
                      </p>
                    ) : runningGeraLandingCopyExecutions.length === 0 ? (
                      <p className="text-muted mb-0">
                        Nenhum job pendente ou em execução.
                      </p>
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
                            {runningGeraLandingCopyExecutions.map(
                              (execution) => (
                                <tr key={execution.idJob}>
                                  <td>
                                    <Link
                                      to={`/experiments/${expId}/geralanding/stage-executions/${execution.idJob}?stageCode=landing-page-copy`}
                                      className="fw-semibold text-decoration-none"
                                    >
                                      {execution.idJob}
                                    </Link>
                                  </td>
                                  <td>{execution.status}</td>
                                  <td>
                                    {formatDateTimeValue(
                                      execution.executionRequestedAt,
                                    )}
                                  </td>
                                </tr>
                              ),
                            )}
                          </tbody>
                        </table>
                      </div>
                    )}
                    <div className="rounded border bg-light-subtle p-3 d-flex flex-column gap-3">
                      <h6 className="mb-0">Histórico de execuções</h6>
                      {isLoadingCompletedGeraLandingCopyExecutions ? (
                        <p className="text-muted mb-0">
                          Carregando execuções...
                        </p>
                      ) : historyGeraLandingCopyExecutions.length === 0 ? (
                        <p className="text-muted mb-0">
                          Nenhuma execução registrada para esta etapa.
                        </p>
                      ) : (
                        <div className="table-responsive">
                          <table className="table table-sm align-middle mb-0">
                            <thead>
                              <tr>
                                <th scope="col">Job ID</th>
                                <th scope="col">Status</th>
                                <th scope="col">Data-hora</th>
                                <th scope="col" className="text-end">
                                  Custo
                                </th>
                              </tr>
                            </thead>
                            <tbody>
                              {historyGeraLandingCopyExecutions.map(
                                (execution) => (
                                  <tr key={execution.idJob}>
                                    <td>
                                      <Link
                                        to={`/experiments/${expId}/geralanding/stage-executions/${execution.idJob}?stageCode=landing-page-copy`}
                                        className="fw-semibold text-decoration-none"
                                      >
                                        {execution.idJob}
                                      </Link>
                                    </td>
                                    <td>{execution.status}</td>
                                    <td>
                                      {formatDateTimeValue(
                                        execution.executionRequestedAt,
                                      )}
                                    </td>
                                    <td className="text-end">
                                      {formatCurrencyUsd(
                                        resolveExecutionCostUsd(execution),
                                      )}
                                    </td>
                                  </tr>
                                ),
                              )}
                            </tbody>
                          </table>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </div>
              <div className="card">
                <div className="card-body d-flex flex-column gap-3">
                  <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
                    <div>
                      <h5 className="card-title mb-1">
                        3 - Gera Prompt Imagem
                      </h5>
                      {renderSelectedGeraLandingModel(
                        "landing-page-image-planning",
                      )}
                    </div>
                    <span className="badge text-bg-light border fs-6 fw-semibold">
                      Total execuções:{" "}
                      {formatCurrencyUsd(
                        historyGeraLandingImagePromptsExecutions.reduce(
                          (sum, execution) =>
                            sum + (resolveExecutionCostUsd(execution) ?? 0),
                          0,
                        ),
                      )}
                    </span>
                  </div>
                  <div className="rounded border bg-light-subtle p-3 d-flex flex-column gap-3">
                    <h6 className="mb-0">Execução dos prompts de imagem</h6>
                    {showGeraLandingStartButtons ? (
                      <button
                        type="button"
                        className="btn btn-primary align-self-start"
                        onClick={handleStartImagePrompts}
                        disabled={
                          alterationLocked ||
                          isStartingImagePrompts ||
                          hasRunningGeraLandingImagePromptsExecution
                        }
                      >
                        {isStartingImagePrompts ? (
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
                    ) : null}
                    {isLoadingPendingGeraLandingImagePromptsExecutions ? (
                      <p className="text-muted mb-0">
                        Carregando jobs da etapa...
                      </p>
                    ) : runningGeraLandingImagePromptsExecutions.length ===
                      0 ? (
                      <p className="text-muted mb-0">
                        Nenhum job pendente ou em execução.
                      </p>
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
                            {runningGeraLandingImagePromptsExecutions.map(
                              (execution) => (
                                <tr key={execution.idJob}>
                                  <td>
                                    <Link
                                      to={`/experiments/${expId}/geralanding/stage-executions/${execution.idJob}?stageCode=landing-page-image-planning`}
                                      className="fw-semibold text-decoration-none"
                                    >
                                      {execution.idJob}
                                    </Link>
                                  </td>
                                  <td>{execution.status}</td>
                                  <td>
                                    {formatDateTimeValue(
                                      execution.executionRequestedAt,
                                    )}
                                  </td>
                                </tr>
                              ),
                            )}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                  <div className="rounded border bg-light-subtle p-3 d-flex flex-column gap-3">
                    <h6 className="mb-0">Histórico de execuções</h6>
                    {isLoadingCompletedGeraLandingImagePromptsExecutions ? (
                      <p className="text-muted mb-0">Carregando execuções...</p>
                    ) : historyGeraLandingImagePromptsExecutions.length ===
                      0 ? (
                      <p className="text-muted mb-0">
                        Nenhuma execução registrada para esta etapa.
                      </p>
                    ) : (
                      <div className="table-responsive">
                        <table className="table table-sm align-middle mb-0">
                          <thead>
                            <tr>
                              <th scope="col">Job ID</th>
                              <th scope="col">Status</th>
                              <th scope="col">Data-hora</th>
                              <th scope="col" className="text-end">
                                Custo
                              </th>
                              <th scope="col" className="text-end">
                                Ações
                              </th>
                            </tr>
                          </thead>
                          <tbody>
                            {historyGeraLandingImagePromptsExecutions.map(
                              (execution) => (
                                <tr key={execution.idJob}>
                                  <td>
                                    <Link
                                      to={`/experiments/${expId}/geralanding/stage-executions/${execution.idJob}?stageCode=landing-page-image-planning`}
                                      className="fw-semibold text-decoration-none"
                                    >
                                      {execution.idJob}
                                    </Link>
                                  </td>
                                  <td>{execution.status}</td>
                                  <td>
                                    {formatDateTimeValue(
                                      execution.executionRequestedAt,
                                    )}
                                  </td>
                                  <td className="text-end">
                                    {formatCurrencyUsd(
                                      resolveExecutionCostUsd(execution),
                                    )}
                                  </td>
                                  <td className="text-end">
                                    {latestSuccessfulImagePromptExecution?.idJob ===
                                    execution.idJob ? (
                                      <button
                                        type="button"
                                        className="btn btn-primary btn-sm"
                                        onClick={() =>
                                          handleGenerateLandingHtml(
                                            execution.idJob,
                                          )
                                        }
                                        disabled={
                                          alterationLocked ||
                                          isGeneratingLandingHtml ||
                                          frameworkImagePendingCount > 0
                                        }
                                      >
                                        {isGeneratingLandingHtml ? (
                                          <>
                                            <span
                                              className="spinner-border spinner-border-sm me-2"
                                              role="status"
                                              aria-hidden="true"
                                            />
                                            Gerando HTML...
                                          </>
                                        ) : (
                                          "Gerar HTML"
                                        )}
                                      </button>
                                    ) : null}
                                  </td>
                                </tr>
                              ),
                            )}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                </div>
              </div>
              <div className="card">
                <div className="card-body d-flex flex-column gap-3">
                  <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
                    <div>
                      <h5 className="card-title mb-1">4 - Gera Imagem</h5>
                      {renderSelectedGeraLandingModel(
                        "landing-page-image-generation",
                      )}
                    </div>
                    <div className="d-flex flex-wrap gap-2">
                      <span className="badge text-bg-light border fs-6 fw-semibold">
                        Pendentes: {frameworkImagePendingCount}
                      </span>
                      <span className="badge text-bg-light border fs-6 fw-semibold">
                        Total execuções:{" "}
                        {formatCurrencyUsd(
                          historyGeraLandingImageGenerationExecutions.reduce(
                            (sum, execution) =>
                              sum + (resolveExecutionCostUsd(execution) ?? 0),
                            0,
                          ),
                        )}
                      </span>
                    </div>
                  </div>
                  <p className="text-muted mb-0">
                    Esta etapa executa a geração real das imagens em lote na
                    OpenAI usando os prompts produzidos no Gera Prompt Imagem.
                  </p>
                  <div className="d-flex flex-wrap align-items-center gap-2">
                    <Link
                      to={`/experiments/${expId}/framework-images`}
                      className="btn btn-outline-primary btn-sm"
                    >
                      Ver detalhes das imagens
                    </Link>
                    {showGeraLandingStartButtons ? (
                      <button
                        type="button"
                        className="btn btn-primary align-self-start"
                        onClick={handleStartImageGeneration}
                        disabled={
                          alterationLocked ||
                          isStartingImageGeneration ||
                          hasRunningGeraLandingImageGenerationExecution
                        }
                      >
                        {isStartingImageGeneration ? (
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
                    ) : null}
                    <span className="small text-muted">
                      Acompanhe a timeline detalhada na aba{" "}
                      <strong>Conteúdo</strong>, bloco{" "}
                      <strong>Gerar imagens em lote (AI Worker)</strong>.
                    </span>
                  </div>
                  {isLoadingPendingGeraLandingImageGenerationExecutions ? (
                    <p className="text-muted mb-0">
                      Carregando jobs da etapa...
                    </p>
                  ) : runningGeraLandingImageGenerationExecutions.length ===
                    0 ? (
                    <p className="text-muted mb-0">
                      Nenhum job pendente ou em execução.
                    </p>
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
                          {runningGeraLandingImageGenerationExecutions.map(
                            (execution) => (
                              <tr key={execution.idJob}>
                                <td>
                                  <Link
                                    to={`/experiments/${expId}/geralanding/stage-executions/${execution.idJob}?stageCode=landing-page-image-generation`}
                                    className="fw-semibold text-decoration-none"
                                  >
                                    {execution.idJob}
                                  </Link>
                                </td>
                                <td>{execution.status}</td>
                                <td>
                                  {formatDateTimeValue(
                                    execution.executionRequestedAt,
                                  )}
                                </td>
                              </tr>
                            ),
                          )}
                        </tbody>
                      </table>
                    </div>
                  )}
                  <div className="rounded border bg-light-subtle p-3 d-flex flex-column gap-3">
                    <h6 className="mb-0">Histórico de execuções</h6>
                    {isLoadingCompletedGeraLandingImageGenerationExecutions ? (
                      <p className="text-muted mb-0">Carregando execuções...</p>
                    ) : historyGeraLandingImageGenerationExecutions.length ===
                      0 ? (
                      <p className="text-muted mb-0">
                        Nenhuma execução registrada para esta etapa.
                      </p>
                    ) : (
                      <div className="table-responsive">
                        <table className="table table-sm align-middle mb-0">
                          <thead>
                            <tr>
                              <th scope="col">Job ID</th>
                              <th scope="col">Status</th>
                              <th scope="col">Data-hora</th>
                              <th scope="col" className="text-end">
                                Custo
                              </th>
                            </tr>
                          </thead>
                          <tbody>
                            {historyGeraLandingImageGenerationExecutions.map(
                              (execution) => (
                                <tr key={execution.idJob}>
                                  <td>
                                    <Link
                                      to={`/experiments/${expId}/geralanding/stage-executions/${execution.idJob}?stageCode=landing-page-image-generation`}
                                      className="fw-semibold text-decoration-none"
                                    >
                                      {execution.idJob}
                                    </Link>
                                  </td>
                                  <td>{execution.status}</td>
                                  <td>
                                    {formatDateTimeValue(
                                      execution.executionRequestedAt,
                                    )}
                                  </td>
                                  <td className="text-end">
                                    {formatCurrencyUsd(
                                      resolveExecutionCostUsd(execution),
                                    )}
                                  </td>
                                </tr>
                              ),
                            )}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                  {frameworkImageSummary || resetFrameworkImageCounters ? (
                    <div className="row g-2">
                      <div className="col-6 col-md-4">
                        <div className="small border rounded p-2 bg-light">
                          Em processamento:{" "}
                          <strong>
                            {resetFrameworkImageCounters
                              ? 0
                              : (frameworkImageSummary?.processingCount ?? 0)}
                          </strong>
                        </div>
                      </div>
                      <div className="col-6 col-md-4">
                        <div className="small border rounded p-2 bg-light">
                          Aguardando OpenAI batch:{" "}
                          <strong>
                            {resetFrameworkImageCounters
                              ? 0
                              : (frameworkImageSummary?.waitingOpenAiBatchCount ??
                                0)}
                          </strong>
                        </div>
                      </div>
                      <div className="col-6 col-md-4">
                        <div className="small border rounded p-2 bg-light">
                          Concluídas:{" "}
                          <strong>
                            {resetFrameworkImageCounters
                              ? 0
                              : (frameworkImageSummary?.completedCount ?? 0)}
                          </strong>
                        </div>
                      </div>
                      <div className="col-6 col-md-4">
                        <div className="small border rounded p-2 bg-light">
                          Falhas:{" "}
                          <strong>
                            {resetFrameworkImageCounters
                              ? 0
                              : (frameworkImageSummary?.failedCount ?? 0)}
                          </strong>
                        </div>
                      </div>
                      <div className="col-6 col-md-4">
                        <div className="small border rounded p-2 bg-light">
                          Total de itens:{" "}
                          <strong>
                            {resetFrameworkImageCounters
                              ? 0
                              : (frameworkImageSummary?.totalItems ?? 0)}
                          </strong>
                        </div>
                      </div>
                    </div>
                  ) : null}
                  {isLoadingFrameworkImageStatuses ? (
                    <p className="text-muted mb-0">
                      Carregando status do Gera Imagem...
                    </p>
                  ) : null}
                </div>
              </div>
              <div className="card">
                <div className="card-body d-flex flex-column gap-3">
                  <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
                    <div>
                      <h5 className="card-title mb-1">
                        5 - Gera Preset Design
                      </h5>
                      {renderSelectedGeraLandingModel(
                        "landing-page-design-preset",
                      )}
                    </div>
                    <span className="badge text-bg-light border fs-6 fw-semibold">
                      Total execuções:{" "}
                      {formatCurrencyUsd(
                        totalCompletedGeraLandingDesignPresetCostUsd,
                      )}
                    </span>
                  </div>
                  <div className="d-flex flex-column gap-3">
                    {showGeraLandingStartButtons ? (
                      <button
                        type="button"
                        className="btn btn-primary align-self-start"
                        onClick={handleStartDesignPreset}
                        disabled={
                          alterationLocked ||
                          isStartingDesignPreset ||
                          hasRunningGeraLandingDesignPresetExecution
                        }
                      >
                        {isStartingDesignPreset ? (
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
                    ) : null}
                    {isLoadingPendingGeraLandingDesignPresetExecutions ? (
                      <p className="text-muted mb-0">
                        Carregando jobs da etapa...
                      </p>
                    ) : runningGeraLandingDesignPresetExecutions.length ===
                      0 ? (
                      <p className="text-muted mb-0">
                        Nenhum job pendente ou em execução.
                      </p>
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
                            {runningGeraLandingDesignPresetExecutions.map(
                              (execution) => (
                                <tr key={execution.idJob}>
                                  <td>
                                    <Link
                                      to={`/experiments/${expId}/geralanding/stage-executions/${execution.idJob}?stageCode=landing-page-design-preset`}
                                      className="fw-semibold text-decoration-none"
                                    >
                                      {execution.idJob}
                                    </Link>
                                  </td>
                                  <td>{execution.status}</td>
                                  <td>
                                    {formatDateTimeValue(
                                      execution.executionRequestedAt,
                                    )}
                                  </td>
                                </tr>
                              ),
                            )}
                          </tbody>
                        </table>
                      </div>
                    )}
                    <div className="rounded border bg-light-subtle p-3 d-flex flex-column gap-3">
                      <h6 className="mb-0">Histórico de execuções</h6>
                      {isLoadingCompletedGeraLandingDesignPresetExecutions ? (
                        <p className="text-muted mb-0">
                          Carregando execuções...
                        </p>
                      ) : historyGeraLandingDesignPresetExecutions.length ===
                        0 ? (
                        <p className="text-muted mb-0">
                          Nenhuma execução registrada para esta etapa.
                        </p>
                      ) : (
                        <div className="table-responsive">
                          <table className="table table-sm align-middle mb-0">
                            <thead>
                              <tr>
                                <th scope="col">Job ID</th>
                                <th scope="col">Status</th>
                                <th scope="col">Data-hora</th>
                                <th scope="col" className="text-end">
                                  Custo
                                </th>
                              </tr>
                            </thead>
                            <tbody>
                              {historyGeraLandingDesignPresetExecutions.map(
                                (execution) => (
                                  <tr key={execution.idJob}>
                                    <td>
                                      <Link
                                        to={`/experiments/${expId}/geralanding/stage-executions/${execution.idJob}?stageCode=landing-page-design-preset`}
                                        className="fw-semibold text-decoration-none"
                                      >
                                        {execution.idJob}
                                      </Link>
                                    </td>
                                    <td>{execution.status}</td>
                                    <td>
                                      {formatDateTimeValue(
                                        execution.executionRequestedAt,
                                      )}
                                    </td>
                                    <td className="text-end">
                                      {formatCurrencyUsd(
                                        resolveExecutionCostUsd(execution),
                                      )}
                                    </td>
                                  </tr>
                                ),
                              )}
                            </tbody>
                          </table>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </div>

              <div className="card">
                <div className="card-body d-flex flex-column gap-3">
                  <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
                    <div>
                      <h5 className="card-title mb-1">6 - Quality Review</h5>
                      {renderSelectedGeraLandingModel(
                        "landing-page-quality-review",
                      )}
                    </div>
                    <span className="badge text-bg-light border fs-6 fw-semibold">
                      Total execuções:{" "}
                      {formatCurrencyUsd(
                        historyGeraLandingQualityReviewExecutions.reduce(
                          (sum, execution) =>
                            sum + (resolveExecutionCostUsd(execution) ?? 0),
                          0,
                        ),
                      )}
                    </span>
                  </div>
                  <div className="rounded border border-primary-subtle bg-primary-subtle p-3">
                    <div className="d-flex flex-wrap align-items-center justify-content-between gap-3">
                      <div>
                        <div className="small text-uppercase fw-semibold text-primary-emphasis">
                          Score mais recente
                        </div>
                        <div className="display-6 fw-bold text-primary-emphasis mb-0">
                          {formatQualityReviewScore(
                            latestCompletedQualityReviewExecution?.score,
                          )}
                        </div>
                        <div className="small text-primary-emphasis">
                          {latestCompletedQualityReviewExecution
                            ? `Job ${latestCompletedQualityReviewExecution.idJob} · ${formatDateTimeValue(
                                latestCompletedQualityReviewExecution.executionRequestedAt,
                              )}`
                            : "Nenhuma execução concluída ainda."}
                        </div>
                      </div>
                      <span
                        className={`badge fs-6 ${resolveQualityReviewApprovalBadgeClass(
                          latestCompletedQualityReviewExecution,
                        )}`}
                      >
                        {resolveQualityReviewApprovalLabel(
                          latestCompletedQualityReviewExecution,
                        )}
                      </span>
                    </div>
                  </div>
                  {showGeraLandingStartButtons ? (
                    <button
                      type="button"
                      className="btn btn-primary align-self-start"
                      onClick={handleStartQualityReview}
                      disabled={
                        alterationLocked ||
                        isStartingQualityReview ||
                        hasRunningGeraLandingQualityReviewExecution
                      }
                    >
                      {isStartingQualityReview ? (
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
                  ) : null}
                  {isLoadingPendingGeraLandingQualityReviewExecutions ? (
                    <p className="text-muted mb-0">
                      Carregando jobs da etapa...
                    </p>
                  ) : runningGeraLandingQualityReviewExecutions.length === 0 ? (
                    <p className="text-muted mb-0">
                      Nenhum job pendente ou em execução.
                    </p>
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
                          {runningGeraLandingQualityReviewExecutions.map(
                            (execution) => (
                              <tr key={execution.idJob}>
                                <td>
                                  <Link
                                    to={`/experiments/${expId}/geralanding/stage-executions/${execution.idJob}?stageCode=landing-page-quality-review`}
                                    className="fw-semibold text-decoration-none"
                                  >
                                    {execution.idJob}
                                  </Link>
                                </td>
                                <td>{execution.status}</td>
                                <td>
                                  {formatDateTimeValue(
                                    execution.executionRequestedAt,
                                  )}
                                </td>
                              </tr>
                            ),
                          )}
                        </tbody>
                      </table>
                    </div>
                  )}
                  <div className="rounded border bg-light-subtle p-3 d-flex flex-column gap-3">
                    <h6 className="mb-0">Histórico de execuções</h6>
                    {isLoadingCompletedGeraLandingQualityReviewExecutions ? (
                      <p className="text-muted mb-0">Carregando execuções...</p>
                    ) : historyGeraLandingQualityReviewExecutions.length ===
                      0 ? (
                      <p className="text-muted mb-0">
                        Nenhuma execução registrada para esta etapa.
                      </p>
                    ) : (
                      <div className="table-responsive">
                        <table className="table table-sm align-middle mb-0">
                          <thead>
                            <tr>
                              <th scope="col">Job ID</th>
                              <th scope="col">Status</th>
                              <th scope="col">Data-hora</th>
                              <th scope="col" className="text-end">
                                Custo
                              </th>
                            </tr>
                          </thead>
                          <tbody>
                            {historyGeraLandingQualityReviewExecutions.map(
                              (execution) => (
                                <tr key={execution.idJob}>
                                  <td>
                                    <Link
                                      to={`/experiments/${expId}/geralanding/stage-executions/${execution.idJob}?stageCode=landing-page-quality-review`}
                                      className="fw-semibold text-decoration-none"
                                    >
                                      {execution.idJob}
                                    </Link>
                                  </td>
                                  <td>{execution.status}</td>
                                  <td>
                                    {formatDateTimeValue(
                                      execution.executionRequestedAt,
                                    )}
                                  </td>
                                  <td className="text-end">
                                    {formatCurrencyUsd(
                                      resolveExecutionCostUsd(execution),
                                    )}
                                  </td>
                                </tr>
                              ),
                            )}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              <div className="card">
                <div className="card-body d-flex flex-column gap-3">
                  <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
                    <div>
                      <h5 className="card-title mb-1">7 - Gera Entregáveis</h5>
                      {renderSelectedGeraLandingModel(
                        "landing-page-deliverables",
                      )}
                    </div>
                    <span className="badge text-bg-light border fs-6 fw-semibold">
                      Total execuções:{" "}
                      {formatCurrencyUsd(
                        historyGeraLandingDeliverablesExecutions.reduce(
                          (sum, execution) =>
                            sum + (resolveExecutionCostUsd(execution) ?? 0),
                          0,
                        ),
                      )}
                    </span>
                  </div>
                  {showGeraLandingStartButtons ? (
                    <button
                      type="button"
                      className="btn btn-primary align-self-start"
                      onClick={handleStartDeliverables}
                      disabled={
                        alterationLocked ||
                        isStartingDeliverables ||
                        hasRunningGeraLandingDeliverablesExecution
                      }
                    >
                      {isStartingDeliverables ? (
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
                  ) : null}
                  {isLoadingPendingGeraLandingDeliverablesExecutions ? (
                    <p className="text-muted mb-0">
                      Carregando jobs da etapa...
                    </p>
                  ) : runningGeraLandingDeliverablesExecutions.length === 0 ? (
                    <p className="text-muted mb-0">
                      Nenhum job pendente ou em execução.
                    </p>
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
                          {runningGeraLandingDeliverablesExecutions.map(
                            (execution) => (
                              <tr key={execution.idJob}>
                                <td>
                                  <Link
                                    to={`/experiments/${expId}/geralanding/stage-executions/${execution.idJob}?stageCode=landing-page-deliverables`}
                                    className="fw-semibold text-decoration-none"
                                  >
                                    {execution.idJob}
                                  </Link>
                                </td>
                                <td>{execution.status}</td>
                                <td>
                                  {formatDateTimeValue(
                                    execution.executionRequestedAt,
                                  )}
                                </td>
                              </tr>
                            ),
                          )}
                        </tbody>
                      </table>
                    </div>
                  )}
                  <div className="rounded border bg-light-subtle p-3 d-flex flex-column gap-3">
                    <h6 className="mb-0">Histórico de execuções</h6>
                    {isLoadingCompletedGeraLandingDeliverablesExecutions ? (
                      <p className="text-muted mb-0">Carregando execuções...</p>
                    ) : historyGeraLandingDeliverablesExecutions.length ===
                      0 ? (
                      <p className="text-muted mb-0">
                        Nenhuma execução registrada para esta etapa.
                      </p>
                    ) : (
                      <div className="table-responsive">
                        <table className="table table-sm align-middle mb-0">
                          <thead>
                            <tr>
                              <th scope="col">Job ID</th>
                              <th scope="col">Status</th>
                              <th scope="col">Data-hora</th>
                              <th scope="col" className="text-end">
                                Custo
                              </th>
                            </tr>
                          </thead>
                          <tbody>
                            {historyGeraLandingDeliverablesExecutions.map(
                              (execution) => (
                                <tr key={execution.idJob}>
                                  <td>
                                    <Link
                                      to={`/experiments/${expId}/geralanding/stage-executions/${execution.idJob}?stageCode=landing-page-deliverables`}
                                      className="fw-semibold text-decoration-none"
                                    >
                                      {execution.idJob}
                                    </Link>
                                  </td>
                                  <td>{execution.status}</td>
                                  <td>
                                    {formatDateTimeValue(
                                      execution.executionRequestedAt,
                                    )}
                                  </td>
                                  <td className="text-end">
                                    {formatCurrencyUsd(
                                      resolveExecutionCostUsd(execution),
                                    )}
                                  </td>
                                </tr>
                              ),
                            )}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </Tabs.Content>
          <Tabs.Content value="content-structure" asChild>
            <ExperimentContentGenerationTab
              experimentId={expId}
              experimentName={data?.name}
              hypothesis={hyp}
              campaignAngle={data?.campaignAngle}
              adCopy={data?.adCopy}
              alterationLocked={alterationLocked}
              hasPublishedFacebookCampaigns={hasPublishedFacebookCampaigns}
              isCheckingPublishedFacebookCampaigns={isLoadingFacebookCampaigns}
            />
          </Tabs.Content>
          <Tabs.Content value="publico" asChild>
            <ExperimentAudienceTab
              experimentId={Number(expId)}
              nicheId={data?.nicheId}
              alterationLocked={alterationLocked}
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
                          <div className="d-flex align-items-center gap-2">
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
                              {copiedCardKey === card.key
                                ? "Copiado!"
                                : "Copiar etapa"}
                            </button>
                            <button
                              type="button"
                              className="btn btn-outline-primary btn-sm"
                              disabled={downloadingCardKey === card.key}
                              onClick={async () => {
                                try {
                                  setDownloadingCardKey(card.key);
                                  const safeTitle = sanitizeFilenamePart(
                                    card.title,
                                  );
                                  const jobNumber =
                                    resolveJobNumberFromStageContent(
                                      formattedValue,
                                    );
                                  downloadPipelineStage(
                                    formattedValue,
                                    `${safeTitle || card.key}-job-${jobNumber}.json`,
                                  );
                                } catch {
                                  toast.error(
                                    "Não foi possível baixar esta etapa.",
                                  );
                                } finally {
                                  setDownloadingCardKey((current) =>
                                    current === card.key ? null : current,
                                  );
                                }
                              }}
                            >
                              {downloadingCardKey === card.key ? (
                                <span
                                  className="spinner-border spinner-border-sm me-1"
                                  role="status"
                                  aria-hidden="true"
                                />
                              ) : null}
                              Baixar etapa
                            </button>
                          </div>
                        ) : null}
                      </div>
                      <p className="text-muted small mb-3">
                        {card.description}
                      </p>
                      {formattedValue ? (
                        <CollapsibleJsonViewer content={formattedValue} />
                      ) : (
                        <div
                          className="alert alert-secondary mb-0"
                          role="alert"
                        >
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
