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
import { useFacebookConfigurationStatus } from "../../api/useFacebookConfigurationStatus";
import { useExperimentFacebookCampaigns } from "../../api/experiment/useExperimentFacebookCampaigns";
import { useExperimentReadiness } from "../../api/experiment/useExperimentReadiness";
import {
  useExperimentCampaignReset,
  useExperimentCampaignResetPreview,
  type ExperimentCampaignResetSummary,
} from "../../api/experiment/useExperimentCampaignReset";
import ExperimentFunnelTab from "./ExperimentFunnelTab";
import ExperimentReportPanel from "./ExperimentReportPanel";
import ExperimentLearningPanel from "./ExperimentLearningPanel";
import ExperimentContentGenerationTab from "./ExperimentContentGenerationTab";
import { ExperimentAudienceTab } from "./ExperimentAudienceTab";
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
import { useGenerateFrameworkImages } from "../../api/experiment/useGenerateFrameworkImages";

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
  const { data: facebookConfig, isLoading: isLoadingFacebookConfig } =
    useFacebookConfigurationStatus();
  const { data: facebookCampaigns, isLoading: isLoadingFacebookCampaigns } =
    useExperimentFacebookCampaigns(expId);
  const [isResetModalOpen, setIsResetModalOpen] = useState(false);
  const [copiedCardKey, setCopiedCardKey] = useState<string | null>(null);
  const [copyingCardKey, setCopyingCardKey] = useState<string | null>(null);
  const [isStartingWireframe, setIsStartingWireframe] = useState(false);
  const [isStartingCopy, setIsStartingCopy] = useState(false);
  const [isStartingDesignPreset, setIsStartingDesignPreset] = useState(false);
  const [isStartingImagePrompts, setIsStartingImagePrompts] = useState(false);
  const [isStartingDeliverables, setIsStartingDeliverables] = useState(false);
  const [isStartingImageGeneration, setIsStartingImageGeneration] =
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
  const [optimisticDeliverablesExecution, setOptimisticDeliverablesExecution] =
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
  const generateFrameworkImages = useGenerateFrameworkImages(expId);
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
  const frameworkImagePendingCount = useMemo(
    () =>
      (frameworkImageStatuses ?? []).filter((item) => {
        const status = item.status?.toUpperCase();
        return status === "PLANNED" || status === "FAILED";
      }).length,
    [frameworkImageStatuses],
  );
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
    ],
    [data?.adCopy, data?.campaignAngle],
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
    setIsStartingImageGeneration(true);
    try {
      await generateFrameworkImages.mutateAsync();
      toast.success("Gera Imagem iniciado com sucesso.");
    } catch (error) {
      const message = axios.isAxiosError(error)
        ? ((error.response?.data?.message as string | undefined) ??
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
    return [
      optimisticWireframeExecution,
      ...(pendingGeraLandingExecutions ?? []),
    ];
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

  const mergedPendingGeraLandingCopyExecutions = useMemo(() => {
    if (!optimisticCopyExecution) {
      return pendingGeraLandingCopyExecutions ?? [];
    }
    const alreadyPresent = (pendingGeraLandingCopyExecutions ?? []).some(
      (execution) => execution.idJob === optimisticCopyExecution.idJob,
    );
    if (alreadyPresent) {
      return pendingGeraLandingCopyExecutions ?? [];
    }
    return [
      optimisticCopyExecution,
      ...(pendingGeraLandingCopyExecutions ?? []),
    ];
  }, [optimisticCopyExecution, pendingGeraLandingCopyExecutions]);

  useEffect(() => {
    if (!optimisticCopyExecution) {
      return;
    }
    const persistedExecution = (pendingGeraLandingCopyExecutions ?? []).some(
      (execution) => execution.idJob === optimisticCopyExecution.idJob,
    );
    if (persistedExecution) {
      setOptimisticCopyExecution(null);
    }
  }, [optimisticCopyExecution, pendingGeraLandingCopyExecutions]);

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
  const mergedPendingGeraLandingDesignPresetExecutions = useMemo(() => {
    if (!optimisticDesignPresetExecution) {
      return pendingGeraLandingDesignPresetExecutions ?? [];
    }
    const alreadyPresent = (
      pendingGeraLandingDesignPresetExecutions ?? []
    ).some(
      (execution) => execution.idJob === optimisticDesignPresetExecution.idJob,
    );
    if (alreadyPresent) {
      return pendingGeraLandingDesignPresetExecutions ?? [];
    }
    return [
      optimisticDesignPresetExecution,
      ...(pendingGeraLandingDesignPresetExecutions ?? []),
    ];
  }, [
    optimisticDesignPresetExecution,
    pendingGeraLandingDesignPresetExecutions,
  ]);

  useEffect(() => {
    if (!optimisticDesignPresetExecution) {
      return;
    }
    const persistedExecution = (
      pendingGeraLandingDesignPresetExecutions ?? []
    ).some(
      (execution) => execution.idJob === optimisticDesignPresetExecution.idJob,
    );
    if (persistedExecution) {
      setOptimisticDesignPresetExecution(null);
    }
  }, [
    optimisticDesignPresetExecution,
    pendingGeraLandingDesignPresetExecutions,
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

  const mergedPendingGeraLandingImagePromptsExecutions = useMemo(() => {
    if (!optimisticImagePromptsExecution) {
      return pendingGeraLandingImagePromptsExecutions ?? [];
    }
    const alreadyPresent = (
      pendingGeraLandingImagePromptsExecutions ?? []
    ).some(
      (execution) => execution.idJob === optimisticImagePromptsExecution.idJob,
    );
    if (alreadyPresent) {
      return pendingGeraLandingImagePromptsExecutions ?? [];
    }
    return [
      optimisticImagePromptsExecution,
      ...(pendingGeraLandingImagePromptsExecutions ?? []),
    ];
  }, [
    optimisticImagePromptsExecution,
    pendingGeraLandingImagePromptsExecutions,
  ]);

  useEffect(() => {
    if (!optimisticImagePromptsExecution) {
      return;
    }
    const persistedExecution = (
      pendingGeraLandingImagePromptsExecutions ?? []
    ).some(
      (execution) => execution.idJob === optimisticImagePromptsExecution.idJob,
    );
    if (persistedExecution) {
      setOptimisticImagePromptsExecution(null);
    }
  }, [
    optimisticImagePromptsExecution,
    pendingGeraLandingImagePromptsExecutions,
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
    ).filter((execution) => isCompletedExecution(execution.status));
    const failedFromPending = (
      pendingGeraLandingImagePromptsExecutions ?? []
    ).filter(
      (execution) =>
        !isRunningExecution(execution.status) &&
        !isCompletedExecution(execution.status),
    );
    const merged = [...failedFromPending, ...completedHistory];
    const dedupMap = new Map<string, GeraLandingStageExecutionItem>();
    merged.forEach((execution) => {
      dedupMap.set(execution.idJob, execution);
    });
    return Array.from(dedupMap.values());
  }, [
    completedGeraLandingImagePromptsExecutions,
    pendingGeraLandingImagePromptsExecutions,
  ]);
  const mergedPendingGeraLandingDeliverablesExecutions = useMemo(() => {
    if (!optimisticDeliverablesExecution) {
      return pendingGeraLandingDeliverablesExecutions ?? [];
    }
    const alreadyPresent = (pendingGeraLandingDeliverablesExecutions ?? []).some(
      (execution) => execution.idJob === optimisticDeliverablesExecution.idJob,
    );
    if (alreadyPresent) {
      return pendingGeraLandingDeliverablesExecutions ?? [];
    }
    return [
      optimisticDeliverablesExecution,
      ...(pendingGeraLandingDeliverablesExecutions ?? []),
    ];
  }, [optimisticDeliverablesExecution, pendingGeraLandingDeliverablesExecutions]);

  useEffect(() => {
    if (!optimisticDeliverablesExecution) {
      return;
    }
    const persistedExecution = (pendingGeraLandingDeliverablesExecutions ?? []).some(
      (execution) => execution.idJob === optimisticDeliverablesExecution.idJob,
    );
    if (persistedExecution) {
      setOptimisticDeliverablesExecution(null);
    }
  }, [optimisticDeliverablesExecution, pendingGeraLandingDeliverablesExecutions]);

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
    const completedHistory = (completedGeraLandingDeliverablesExecutions ?? []).filter(
      (execution) =>
        isCompletedExecution(execution.status) || hasFailedExecution(execution.status),
    );
    const failedFromPending = (pendingGeraLandingDeliverablesExecutions ?? []).filter(
      (execution) => hasFailedExecution(execution.status),
    );

    const sortedExecutions = [...failedFromPending, ...completedHistory].sort(
      (leftExecution, rightExecution) => {
        const leftTimestamp = Date.parse(leftExecution.executionRequestedAt ?? "");
        const rightTimestamp = Date.parse(rightExecution.executionRequestedAt ?? "");
        const normalizedLeftTimestamp = Number.isNaN(leftTimestamp) ? 0 : leftTimestamp;
        const normalizedRightTimestamp = Number.isNaN(rightTimestamp) ? 0 : rightTimestamp;

        return normalizedRightTimestamp - normalizedLeftTimestamp;
      },
    );

    return sortedExecutions.filter(
      (execution, index, allExecutions) =>
        allExecutions.findIndex((candidate) => candidate.idJob === execution.idJob) === index,
    );
  }, [completedGeraLandingDeliverablesExecutions, pendingGeraLandingDeliverablesExecutions]);

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
    historyGeraLandingDeliverablesExecutions.reduce(
      (sum, execution) => sum + (resolveExecutionCostUsd(execution) ?? 0),
      0,
    ) +
    historyGeraLandingImagePromptsExecutions.reduce(
      (sum, execution) => sum + (resolveExecutionCostUsd(execution) ?? 0),
      0,
    );
  const { data: runningGeraLandingJobDetail } =
    useGeraLandingStageExecutionDetail(expId, runningGeraLandingJobId, {
      enabled: Boolean(runningGeraLandingJobId),
      refetchInterval: 10000,
    });

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
  const shouldShowDiagnostics =
    !!diagnostics &&
    diagnostics.headline.trim().toLowerCase() !==
      "nenhuma inconsistência detectada";
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
  const hasFacebookPixelRegistered = Boolean(niche?.facebookPixelId);
  const hasCreativesReady =
    readinessSummary?.hasCreatives ?? data.creativeApproved;
  const readinessCreativeCount = readinessSummary?.creativeCount ?? 0;
  const hasDailyBudget = data.dailyBudget != null && data.dailyBudget > 0;
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
      actionLabel: hasFacebookPixelRegistered
        ? undefined
        : "Ver pixel do nicho",
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
      label: "E-mail de amostra",
      value: "Obsoleto",
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
  return (
    <div>
      <div className="d-flex justify-content-between align-items-start">
        <div>
          <PageTitle icon={experimentIcon}>{data.name}</PageTitle>
          <p className="text-muted mb-0">{data.hypothesis}</p>
        </div>
        <div className="d-flex align-items-center">
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
      <div ref={tabsSectionRef}>
        <Tabs.Root value={tab} onValueChange={setTab} className="mt-3">
          <Tabs.List className="nav nav-tabs">
            <Tabs.Trigger value="overview" className="nav-link">
              Overview
            </Tabs.Trigger>
            <Tabs.Trigger value="funnel" className="nav-link">
              Funil de vendas
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
            <Tabs.Trigger value="content-structure" className="nav-link">
              Estrutura de conteúdo
            </Tabs.Trigger>
            <Tabs.Trigger value="conteudo" className="nav-link">
              Conteúdo
            </Tabs.Trigger>
            <Tabs.Trigger value="publico" className="nav-link">
              Público
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
          <Tabs.Content value="creatives" asChild>
            <CriativosTab experimentId={expId} />
          </Tabs.Content>
          <Tabs.Content value="landing" asChild>
            <LandingTab experiment={data} />
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
                    <h5 className="card-title mb-0">
                      1 - Gera WireFrame
                    </h5>
                    <span className="badge text-bg-light border fs-6 fw-semibold">
                      Total execuções:{" "}
                      {formatCurrencyUsd(totalCompletedGeraLandingCostUsd)}
                    </span>
                  </div>
                  <div className="d-flex flex-column gap-3">
                    <button
                      type="button"
                      className="btn btn-primary align-self-start"
                      onClick={handleStartWireframe}
                      disabled={
                        isStartingWireframe || hasRunningGeraLandingExecution
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
                                    to={`/experiments/${expId}/geralanding/stage-executions/${execution.idJob}`}
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
                                      to={`/experiments/${expId}/geralanding/stage-executions/${execution.idJob}`}
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
                    <h5 className="card-title mb-0">2 - Gera Copy</h5>
                    <span className="badge text-bg-light border fs-6 fw-semibold">
                      Total execuções:{" "}
                      {formatCurrencyUsd(totalCompletedGeraLandingCopyCostUsd)}
                    </span>
                  </div>
                  <div className="d-flex flex-column gap-3">
                    <button
                      type="button"
                      className="btn btn-primary align-self-start"
                      onClick={handleStartCopy}
                      disabled={
                        isStartingCopy || hasRunningGeraLandingCopyExecution
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
                                      to={`/experiments/${expId}/geralanding/stage-executions/${execution.idJob}`}
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
                                        to={`/experiments/${expId}/geralanding/stage-executions/${execution.idJob}`}
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
                    <h5 className="card-title mb-0">
                      4 - Gera Preset Design
                    </h5>
                    <span className="badge text-bg-light border fs-6 fw-semibold">
                      Total execuções:{" "}
                      {formatCurrencyUsd(
                        totalCompletedGeraLandingDesignPresetCostUsd,
                      )}
                    </span>
                  </div>
                  <div className="d-flex flex-column gap-3">
                    <button
                      type="button"
                      className="btn btn-primary align-self-start"
                      onClick={handleStartDesignPreset}
                      disabled={
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
                                      to={`/experiments/${expId}/geralanding/stage-executions/${execution.idJob}`}
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
                                        to={`/experiments/${expId}/geralanding/stage-executions/${execution.idJob}`}
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
                    <h5 className="card-title mb-0">
                      5 - Gera Entregáveis
                    </h5>
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
                  <button
                    type="button"
                    className="btn btn-primary align-self-start"
                    onClick={handleStartDeliverables}
                    disabled={
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
                  {isLoadingPendingGeraLandingDeliverablesExecutions ? (
                    <p className="text-muted mb-0">Carregando jobs da etapa...</p>
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
                          {runningGeraLandingDeliverablesExecutions.map((execution) => (
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
                    {isLoadingCompletedGeraLandingDeliverablesExecutions ? (
                      <p className="text-muted mb-0">Carregando execuções...</p>
                    ) : historyGeraLandingDeliverablesExecutions.length === 0 ? (
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
                              <th scope="col" className="text-end">Custo</th>
                            </tr>
                          </thead>
                          <tbody>
                            {historyGeraLandingDeliverablesExecutions.map((execution) => (
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
                                <td className="text-end">
                                  {formatCurrencyUsd(resolveExecutionCostUsd(execution))}
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

              <div className="card">
                <div className="card-body d-flex flex-column gap-3">
                  <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
                    <h5 className="card-title mb-0">3 - Gera Imagem</h5>
                    <span className="badge text-bg-light border fs-6 fw-semibold">
                      Pendentes: {frameworkImagePendingCount}
                    </span>
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
                    <button
                      type="button"
                      className="btn btn-success align-self-start"
                      onClick={handleStartImageGeneration}
                      disabled={
                        isStartingImageGeneration ||
                        generateFrameworkImages.isPending
                      }
                    >
                      {isStartingImageGeneration ||
                      generateFrameworkImages.isPending
                        ? "Iniciando..."
                        : "Iniciar"}
                    </button>
                    <span className="small text-muted">
                      Acompanhe a timeline detalhada na aba{" "}
                      <strong>Conteúdo</strong>, bloco{" "}
                      <strong>Gerar imagens em lote (AI Worker)</strong>.
                    </span>
                  </div>
                  <div className="rounded border bg-light-subtle p-3 d-flex flex-column gap-3">
                    <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
                      <h6 className="mb-0">Gera Prompt Imagem</h6>
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
                    <button
                      type="button"
                      className="btn btn-primary align-self-start"
                      onClick={handleStartImagePrompts}
                      disabled={
                        isStartingImagePrompts ||
                        hasRunningGeraLandingImagePromptsExecution
                      }
                    >
                      {isStartingImagePrompts ? "Iniciando..." : "Iniciar"}
                    </button>
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
                                      to={`/experiments/${expId}/geralanding/stage-executions/${execution.idJob}`}
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
                  {frameworkImageSummary ? (
                    <div className="row g-2">
                      <div className="col-6 col-md-4">
                        <div className="small border rounded p-2 bg-light">
                          Em processamento:{" "}
                          <strong>
                            {frameworkImageSummary.processingCount}
                          </strong>
                        </div>
                      </div>
                      <div className="col-6 col-md-4">
                        <div className="small border rounded p-2 bg-light">
                          Aguardando OpenAI batch:{" "}
                          <strong>
                            {frameworkImageSummary.waitingOpenAiBatchCount}
                          </strong>
                        </div>
                      </div>
                      <div className="col-6 col-md-4">
                        <div className="small border rounded p-2 bg-light">
                          Concluídas:{" "}
                          <strong>
                            {frameworkImageSummary.completedCount}
                          </strong>
                        </div>
                      </div>
                      <div className="col-6 col-md-4">
                        <div className="small border rounded p-2 bg-light">
                          Falhas:{" "}
                          <strong>{frameworkImageSummary.failedCount}</strong>
                        </div>
                      </div>
                      <div className="col-6 col-md-4">
                        <div className="small border rounded p-2 bg-light">
                          Total de itens:{" "}
                          <strong>{frameworkImageSummary.totalItems}</strong>
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
            </div>
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
          <Tabs.Content value="publico" asChild>
            <ExperimentAudienceTab experimentId={Number(expId)} nicheId={data?.nicheId} />
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
                            {copiedCardKey === card.key
                              ? "Copiado!"
                              : "Copiar etapa"}
                          </button>
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
