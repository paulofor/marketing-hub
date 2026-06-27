import { useEffect, useMemo, useState } from "react";
import { Creative, useCreatives } from "../../api/creative/useCreatives";
import { useUpdateCreative } from "../../api/creative/useUpdateCreative";
import { useDeleteCreative } from "../../api/creative/useDeleteCreative";
import { useExperiment } from "../../api/experiment/useExperiment";
import { useUpdateExperiment } from "../../api/experiment/useUpdateExperiment";
import type { UpdateExperiment } from "../../api/experiment/useUpdateExperiment";
import { useAllFacebookPages } from "../../api/useAllFacebookPages";
import { useInstagramAccounts } from "../../api/useInstagramAccounts";
import InstagramAdPreview from "../../components/InstagramAdPreview";
import { resolveAssetUrl } from "../../utils/resolveAssetUrl";
import {
  AlertTriangle,
  CheckCircle2,
  Edit3,
  Eye,
  Sparkles,
  Trash2,
  X,
  XCircle,
} from "lucide-react";
import "./CriativosTab.css";
import { hasAdCopyContent, parseAdCopyPayload } from "./adCopyParser";
import {
  hasImagePromptContent,
  parseImagePromptPayload,
} from "./imageBriefingParser";
import { useRequestPipelineCreatives } from "../../api/experiment/useRequestPipelineCreatives";

interface Props {
  experimentId: string;
  alterationLocked?: boolean;
}

const ICON_SIZE = 16;

type FeedbackVariant = "success" | "warning" | "error";

interface FeedbackState {
  variant: FeedbackVariant;
  title: string;
  description?: string;
}

interface PromptSourceField {
  label: string;
  source: string;
  markers: string[];
}

const PIPELINE_PROMPT_SOURCE_FIELDS: PromptSourceField[] = [
  {
    label: "Regras de mensagem",
    source: "buildPipelineImagePrompt (base)",
    markers: ["A mensagem deve:"],
  },
  {
    label: "Direção de arte",
    source: "buildPipelineImagePrompt (base)",
    markers: ["Você é um diretor de arte"],
  },
  {
    label: "Formato por placement",
    source: "plan.format",
    markers: ["Formato feed 1080x1350", "Formato vertical 1080x1920"],
  },
  {
    label: "Briefing visual",
    source: "plan.imageBriefing.visualBriefing",
    markers: ["Briefing visual:"],
  },
  {
    label: "Hierarquia sugerida",
    source: "plan.imageBriefing.hierarchy",
    markers: ["Hierarquia sugerida:"],
  },
  {
    label: "Margens de segurança",
    source: "plan.imageBriefing.safeMargins",
    markers: ["Margens de segurança:"],
  },
  {
    label: "Adaptação desejada",
    source: "plan.imageBriefing.formatByPlacement",
    markers: ["Adaptação desejada:"],
  },
  {
    label: "Mensagem obrigatória",
    source: "plan.imageBriefing.messageMatchNotes",
    markers: ["Mensagem obrigatória:"],
  },
  {
    label: "Notas de compliance",
    source: "plan.imageBriefing.complianceNotes",
    markers: ["Notas de compliance:"],
  },
  {
    label: "Palavras-chave de apoio",
    source: "plan.imageBriefing.supportingKeywords",
    markers: ["Palavras-chave de apoio:"],
  },
  {
    label: "Limite de palavras na imagem",
    source: "plan.imageBriefing.imageTextMaxWords",
    markers: ["Limite máximo de palavras sobre a imagem:"],
  },
  {
    label: "Ângulo da variação",
    source: "plan.variantKey",
    markers: ["Ângulo da variação:"],
  },
  {
    label: "Headline de referência",
    source: "plan.headline",
    markers: ["Headline de referência:"],
  },
  {
    label: "Texto principal",
    source: "plan.primaryText",
    markers: ["Texto principal orientado para dor/promessa:"],
  },
  {
    label: "Complemento/contexto",
    source: "plan.description",
    markers: ["Complemento/contexto:"],
  },
  {
    label: "CTA textual visível",
    source: "plan.ctaText",
    markers: ["CTA textual visível:"],
  },
  {
    label: "Destino digital",
    source: "request.destinationUrl",
    markers: ["Representar a ideia de destino digital"],
  },
  {
    label: "Promessa da hipótese",
    source: "experiment.hypothesisRef.promise",
    markers: ["Promessa central da hipótese:"],
  },
  {
    label: "Modelo do Worker IA",
    source: "buildPipelineImagePrompt (base)",
    markers: ["Lembre-se de que o Worker AI usará o modelo gpt-image-2."],
  },
  {
    label: "Restrição de logos e rostos",
    source: "buildPipelineImagePrompt (base)",
    markers: ["Não inclua logos das plataformas"],
  },
];

const resolvePromptSourceStatus = (prompt?: string | null) => {
  const normalizedPrompt = prompt?.trim() ?? "";
  const allMarkers = PIPELINE_PROMPT_SOURCE_FIELDS.flatMap(
    (field) => field.markers,
  );

  const extractContentValue = (markers: string[]) => {
    if (!normalizedPrompt) {
      return null;
    }

    for (const marker of markers) {
      const startIndex = normalizedPrompt.indexOf(marker);
      if (startIndex === -1) {
        continue;
      }

      const contentStart = startIndex + marker.length;
      const nextMarkerIndex = allMarkers
        .map((nextMarker) => normalizedPrompt.indexOf(nextMarker, contentStart))
        .filter((index) => index !== -1)
        .sort((a, b) => a - b)[0];

      const rawValue = normalizedPrompt.slice(
        contentStart,
        nextMarkerIndex === undefined
          ? normalizedPrompt.length
          : nextMarkerIndex,
      );

      const cleanedValue = rawValue
        .replace(/^[\s:\-•]+/, "")
        .replace(/\s+/g, " ")
        .trim();

      if (cleanedValue.length > 0) {
        return cleanedValue;
      }
    }

    return null;
  };

  return PIPELINE_PROMPT_SOURCE_FIELDS.map((field) => ({
    ...field,
    hasContent:
      normalizedPrompt.length > 0 &&
      field.markers.some((marker) => normalizedPrompt.includes(marker)),
    contentValue: extractContentValue(field.markers),
  }));
};

const statusVariant = (status: string) => {
  switch (status) {
    case "READY":
      return "text-bg-success";
    case "DRAFT":
      return "text-bg-secondary";
    default:
      return "text-bg-warning";
  }
};

const statusLabel = (status: string) => {
  switch (status) {
    case "READY":
      return "Aprovado";
    case "DRAFT":
      return "Rascunho";
    default:
      return status;
  }
};

export default function CriativosTab({
  experimentId,
  alterationLocked = false,
}: Props) {
  const { data, isLoading } = useCreatives(experimentId);
  const creatives = Array.isArray(data) ? data : [];
  const { data: experiment } = useExperiment(experimentId);
  const pipelineRequest = useRequestPipelineCreatives(experimentId);
  const adCopyContent = useMemo(
    () => parseAdCopyPayload(experiment?.adCopy ?? null),
    [experiment?.adCopy],
  );
  const imageBriefingContent = useMemo(
    () => parseImagePromptPayload(experiment?.adImageBriefing ?? null),
    [experiment?.adImageBriefing],
  );
  const pipelineHasAdCopy = hasAdCopyContent(adCopyContent);
  const pipelineHasBriefing = hasImagePromptContent(imageBriefingContent);
  const pipelineVariantCount = pipelineHasAdCopy
    ? adCopyContent.primaryTextVariants.length
    : 0;
  const pipelineBriefingCount = pipelineHasBriefing
    ? imageBriefingContent.briefings.length
    : 0;
  const pipelinePairs = Math.min(pipelineVariantCount, pipelineBriefingCount);
  const pipelineAvailable = pipelinePairs > 0;
  const pendingCreativeRequests = experiment?.creativesToGenerate ?? 0;
  const creativeGenerationStatus =
    experiment?.creativeGenerationStatus ?? "IDLE";
  const pipelineInProgress =
    creativeGenerationStatus === "REQUESTED" ||
    creativeGenerationStatus === "PROCESSING";
  const pipelineHasRecoverableFailure =
    !pipelineRequest.isPending &&
    (creativeGenerationStatus === "FAILED" ||
      creativeGenerationStatus === "TIMEOUT");
  const pipelineStatusLabel =
    creativeGenerationStatus === "REQUESTED"
      ? "Worker AI aguardando a fila de imagens"
      : creativeGenerationStatus === "PROCESSING"
        ? "Worker AI processando imagens com gpt-image-2"
        : creativeGenerationStatus === "FAILED"
          ? "Geração falhou; revise a causa e tente novamente"
          : creativeGenerationStatus === "TIMEOUT"
            ? "Geração excedeu o tempo operacional; tente novamente"
            : "Worker AI processando imagens com gpt-image-2";
  const pipelineButtonDisabled =
    alterationLocked ||
    pipelineRequest.isPending ||
    pipelineInProgress ||
    pendingCreativeRequests > 0 ||
    !pipelineAvailable;
  const updateExperimentMutation = useUpdateExperiment(experimentId);
  const [editing, setEditing] = useState<Creative | null>(null);
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);
  const [experimentPageId, setExperimentPageId] = useState("");
  const [experimentInstagramAccountId, setExperimentInstagramAccountId] =
    useState("");
  const closeEdit = () => {
    setEditing(null);
  };
  const update = useUpdateCreative(experimentId);
  const del = useDeleteCreative(experimentId);
  const [showPreview, setShowPreview] = useState(false);
  const [processingCreativeId, setProcessingCreativeId] = useState<
    number | null
  >(null);
  const [expandedPromptByCreativeId, setExpandedPromptByCreativeId] = useState<
    Record<number, boolean>
  >({});
  const [
    expandedPreviousPromptByCreativeId,
    setExpandedPreviousPromptByCreativeId,
  ] = useState<Record<number, boolean>>({});
  const [
    expandedIntermediatePromptByCreativeId,
    setExpandedIntermediatePromptByCreativeId,
  ] = useState<Record<number, boolean>>({});
  const { data: facebookPages, isLoading: isLoadingFacebookPages } =
    useAllFacebookPages();
  const { data: instagramAccounts, isLoading: isLoadingInstagramAccounts } =
    useInstagramAccounts();
  const noInstagramAccounts =
    !isLoadingInstagramAccounts &&
    Array.isArray(instagramAccounts) &&
    instagramAccounts.length === 0;

  useEffect(() => {
    if (!feedback) return;
    const timeout = window.setTimeout(() => {
      setFeedback(null);
    }, 8000);
    return () => {
      window.clearTimeout(timeout);
    };
  }, [feedback]);

  const dismissFeedback = () => setFeedback(null);

  useEffect(() => {
    setExperimentPageId(
      experiment?.facebookPage?.id ? String(experiment.facebookPage.id) : "",
    );
  }, [experiment?.facebookPage?.id]);

  useEffect(() => {
    setExperimentInstagramAccountId(
      experiment?.instagramAccount?.id
        ? String(experiment.instagramAccount.id)
        : "",
    );
  }, [experiment?.instagramAccount?.id]);

  const buildBaseExperimentUpdate = (): UpdateExperiment => {
    if (!experiment) {
      throw new Error("experiment-unavailable");
    }
    const kpiTargetValue = experiment.kpiTarget ?? experiment.kpiTargetCpl;
    if (kpiTargetValue == null || !experiment.metricPresetId) {
      throw new Error("missing-metrics");
    }
    return {
      name: experiment.name,
      hypothesis: experiment.hypothesis,
      kpiTarget: Number(kpiTargetValue),
      metricPresetId: experiment.metricPresetId,
      sampleSize: experiment.sampleSize ?? undefined,
      mde: experiment.mdePercent ?? undefined,
      startDate: experiment.startDate ?? undefined,
      endDate: experiment.endDate ?? undefined,
      creativesToGenerate: experiment.creativesToGenerate ?? undefined,
    };
  };

  const handleSavePageId = async () => {
    if (!experiment) {
      return;
    }
    let basePayload: UpdateExperiment;
    try {
      basePayload = buildBaseExperimentUpdate();
    } catch (error) {
      setFeedback({
        variant: "error",
        title: "Não foi possível salvar a página",
        description:
          error instanceof Error && error.message === "missing-metrics"
            ? "Defina a meta de KPI e o preset de métricas antes de configurar a página do experimento."
            : "Tente novamente em instantes.",
      });
      return;
    }
    if (noInstagramAccounts) {
      setFeedback({
        variant: "error",
        title: "Cadastre uma conta do Instagram",
        description:
          "Cadastre uma conta de Instagram na tela Contas do Instagram antes de salvar as configurações.",
      });
      return;
    }
    if (!experimentInstagramAccountId) {
      setFeedback({
        variant: "error",
        title: "Selecione a conta do Instagram",
        description:
          "Relacione uma conta de Instagram para que o worker possa publicar os criativos.",
      });
      return;
    }
    const trimmedPageId = experimentPageId.trim();
    const parsedPageId = trimmedPageId === "" ? null : Number(trimmedPageId);
    if (parsedPageId !== null && Number.isNaN(parsedPageId)) {
      setFeedback({
        variant: "error",
        title: "ID da página inválido",
        description: "Selecione uma página válida da lista.",
      });
      return;
    }
    try {
      await updateExperimentMutation.mutateAsync({
        ...basePayload,
        facebookPageId: parsedPageId,
        instagramAccountId: Number(experimentInstagramAccountId),
      });
      const selectedPage =
        parsedPageId === null
          ? null
          : (facebookPages?.find((page) => page.id === parsedPageId) ?? null);
      const selectedInstagramAccount = Array.isArray(instagramAccounts)
        ? (instagramAccounts.find(
            (account) => account.id === Number(experimentInstagramAccountId),
          ) ?? null)
        : null;
      const rawHandle =
        selectedInstagramAccount?.handle ??
        experiment.instagramAccount?.handle ??
        "";
      const formattedHandle = rawHandle
        ? rawHandle.startsWith("@")
          ? rawHandle
          : `@${rawHandle}`
        : null;
      const instagramDescription = formattedHandle
        ? `a conta ${formattedHandle}`
        : "a conta do Instagram selecionada";
      setFeedback({
        variant: "success",
        title: "Configurações atualizadas",
        description: selectedPage
          ? `Os criativos publicarão na página ${selectedPage.name} com ${instagramDescription}.`
          : `Sem página definida o worker utilizará a página padrão do Facebook, mantendo ${instagramDescription}.`,
      });
    } catch {
      setFeedback({
        variant: "error",
        title: "Não foi possível salvar a página",
        description: "Tente novamente em instantes.",
      });
    }
  };

  const isSavingPageId = updateExperimentMutation.isPending;

  const openEdit = (c: Creative) => {
    setEditing(c);
    setShowPreview(true);
  };

  const startPreview = (c: Creative) => {
    setEditing(c);
    setShowPreview(true);
  };

  const remove = async (c: Creative) => {
    if (!confirm("Excluir criativo?")) return;
    setProcessingCreativeId(c.id);
    try {
      await del.mutateAsync(c.id);
    } catch {
      setFeedback({
        variant: "error",
        title: "Não foi possível excluir o criativo",
        description: "Tente novamente em instantes.",
      });
    } finally {
      setProcessingCreativeId(null);
    }
  };

  const approve = async (c: Creative) => {
    setProcessingCreativeId(c.id);
    try {
      await update.mutateAsync({
        id: c.id,
        format: c.format || "LINK",
        headline: c.headline,
        primaryText: c.primaryText,
        imageUrl: c.imageUrl,
        description: c.description || "",
        cta: c.cta || "LEARN_MORE",
        destinationUrl: c.destinationUrl || "",
        leadGenFormId: c.leadGenFormId || "",
        instagramUserId: c.instagramUserId || "",
        status: "READY",
      });
    } catch {
      setFeedback({
        variant: "error",
        title: "Não foi possível aprovar o criativo",
        description: "Tente novamente em instantes.",
      });
    } finally {
      setProcessingCreativeId(null);
    }
  };

  const totalCreatives = creatives.length;
  const handlePipelineRequest = async () => {
    try {
      await pipelineRequest.mutateAsync();
      setFeedback({
        variant: "success",
        title: "Solicitação enviada",
        description: "Geraremos até 3 anúncios com o pipeline do experimento.",
      });
    } catch {
      setFeedback({
        variant: "error",
        title: "Erro ao gerar anúncios do pipeline",
        description: "Verifique se o pipeline está completo e tente novamente.",
      });
    }
  };
  const readyCreatives = creatives.filter((c) => c.status === "READY");
  const pendingCreatives = creatives.filter((c) => c.status !== "READY");
  const creativeSections = [
    {
      id: "approved",
      title: "Aprovados",
      badgeClass: "text-bg-success",
      creatives: readyCreatives,
    },
    {
      id: "pending",
      title: "Aguardando aprovação",
      badgeClass: "text-bg-warning",
      creatives: pendingCreatives,
    },
  ].filter((section) => section.creatives.length > 0);

  const renderCreativeCard = (c: Creative) => {
    const imageUrl = c.imageUrl ? resolveAssetUrl(c.imageUrl) : undefined;
    const isProcessing = processingCreativeId === c.id;
    const hasImagePrompt = Boolean(c.imagePrompt?.trim());
    const isPromptExpanded = Boolean(expandedPromptByCreativeId[c.id]);
    const previousImagePrompt = experiment?.creativeImagePrompt?.trim() ?? "";
    const hasPreviousImagePrompt = Boolean(previousImagePrompt);
    const isPreviousPromptExpanded = Boolean(
      expandedPreviousPromptByCreativeId[c.id],
    );
    const hasIntermediateImagePrompt = Boolean(
      c.imageIntermediatePrompt?.trim(),
    );
    const isIntermediatePromptExpanded = Boolean(
      expandedIntermediatePromptByCreativeId[c.id],
    );
    const promptSourceStatus = resolvePromptSourceStatus(c.imagePrompt);

    const togglePromptVisibility = () => {
      setExpandedPromptByCreativeId((current) => ({
        ...current,
        [c.id]: !current[c.id],
      }));
    };
    const togglePreviousPromptVisibility = () => {
      setExpandedPreviousPromptByCreativeId((current) => ({
        ...current,
        [c.id]: !current[c.id],
      }));
    };
    const toggleIntermediatePromptVisibility = () => {
      setExpandedIntermediatePromptByCreativeId((current) => ({
        ...current,
        [c.id]: !current[c.id],
      }));
    };

    return (
      <article
        key={c.id}
        className="creative-card"
        aria-busy={isProcessing}
        aria-live={isProcessing ? "polite" : undefined}
      >
        {isProcessing && (
          <div className="creative-card-processing">
            <div className="spinner-border text-primary" role="status">
              <span className="visually-hidden">Processando criativo...</span>
            </div>
          </div>
        )}
        {imageUrl ? (
          <img
            src={imageUrl}
            alt={c.headline || "Criativo"}
            className="creative-card-img"
          />
        ) : (
          <div className="creative-card-placeholder">
            <span className="text-muted">Imagem não disponível</span>
          </div>
        )}
        <div className="creative-card-body">
          <div className="d-flex flex-wrap align-items-center justify-content-between gap-2">
            <span className={`badge rounded-pill ${statusVariant(c.status)}`}>
              {statusLabel(c.status)}
            </span>
            {c.format && (
              <span className="badge rounded-pill text-bg-light text-uppercase text-muted">
                {c.format}
              </span>
            )}
          </div>
          <h3 className="creative-card-headline">
            {c.headline || "Sem headline"}
          </h3>
          <p className="creative-card-text mb-0">{c.primaryText}</p>
          {(c.cta || c.destinationUrl || c.leadGenFormId) && (
            <div className="creative-card-meta small text-muted">
              {c.cta && <span className="me-2">CTA: {c.cta}</span>}
              {c.destinationUrl && (
                <a
                  href={c.destinationUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="text-decoration-none text-muted text-truncate d-block"
                  title={c.destinationUrl}
                >
                  {c.destinationUrl}
                </a>
              )}
              {c.leadGenFormId && (
                <span className="d-block mt-1">
                  Formulário: {c.leadGenFormId}
                </span>
              )}
            </div>
          )}
        </div>
        <div className="creative-card-footer">
          <div className="creative-card-actions">
            <button
              type="button"
              className="btn btn-outline-primary btn-sm d-flex align-items-center justify-content-center gap-1"
              onClick={() => openEdit(c)}
              disabled={isProcessing || alterationLocked}
            >
              <Edit3 size={ICON_SIZE} />
              <span>Editar</span>
            </button>
            <button
              type="button"
              className="btn btn-outline-danger btn-sm d-flex align-items-center justify-content-center gap-1"
              onClick={() => remove(c)}
              disabled={isProcessing || alterationLocked}
            >
              <Trash2 size={ICON_SIZE} />
              <span>Excluir</span>
            </button>
            {c.status !== "READY" && (
              <button
                type="button"
                className="btn btn-outline-success btn-sm d-flex align-items-center justify-content-center gap-1"
                onClick={() => approve(c)}
                disabled={isProcessing || alterationLocked}
              >
                {isProcessing ? (
                  <span
                    className="spinner-border spinner-border-sm"
                    role="status"
                    aria-hidden
                  />
                ) : (
                  <CheckCircle2 size={ICON_SIZE} />
                )}
                <span>{isProcessing ? "Aprovando..." : "Aprovar"}</span>
              </button>
            )}
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm d-flex align-items-center justify-content-center gap-1"
              onClick={() => startPreview(c)}
              aria-label="Preview"
              disabled={isProcessing}
            >
              <Eye size={ICON_SIZE} />
              <span>Preview</span>
            </button>
          </div>
          {hasImagePrompt && (
            <div className="creative-card-prompt-container">
              <button
                type="button"
                className="btn btn-outline-info btn-sm w-100"
                onClick={togglePromptVisibility}
                disabled={isProcessing}
              >
                {isPromptExpanded
                  ? "Ocultar prompt da imagem"
                  : "Ver prompt da imagem"}
              </button>
              {isPromptExpanded && (
                <>
                  <p className="creative-card-prompt-text mb-0 mt-2">
                    {c.imagePrompt?.trim()}
                  </p>
                  <div className="creative-card-prompt-source mt-2">
                    <p className="creative-card-prompt-source-title mb-2">
                      Origem dos trechos do prompt
                    </p>
                    <ul className="creative-card-prompt-source-list mb-0">
                      {promptSourceStatus.map((field) => (
                        <li key={field.label}>
                          <span>{field.label}</span>
                          <code>{field.source}</code>
                          <span
                            className={`badge rounded-pill ${
                              field.hasContent
                                ? "text-bg-success"
                                : "text-bg-secondary"
                            }`}
                          >
                            {field.hasContent ? "Com conteúdo" : "Sem conteúdo"}
                          </span>
                          {field.hasContent && (
                            <small className="creative-card-prompt-source-value">
                              {field.contentValue ??
                                "Trecho identificado no prompt (sem valor explícito)."}
                            </small>
                          )}
                        </li>
                      ))}
                    </ul>
                  </div>
                </>
              )}
            </div>
          )}
          {hasPreviousImagePrompt && (
            <div className="creative-card-prompt-container">
              <button
                type="button"
                className="btn btn-outline-info btn-sm w-100"
                onClick={togglePreviousPromptVisibility}
                disabled={isProcessing}
              >
                {isPreviousPromptExpanded
                  ? "Ocultar prompt anterior da imagem"
                  : "Ver prompt anterior da imagem"}
              </button>
              {isPreviousPromptExpanded && (
                <p className="creative-card-prompt-text mb-0 mt-2">
                  {previousImagePrompt}
                </p>
              )}
            </div>
          )}
          {hasIntermediateImagePrompt && (
            <div className="creative-card-prompt-container">
              <button
                type="button"
                className="btn btn-outline-info btn-sm w-100"
                onClick={toggleIntermediatePromptVisibility}
                disabled={isProcessing}
              >
                {isIntermediatePromptExpanded
                  ? "Ocultar prompt intermediário"
                  : "Ver prompt intermediário"}
              </button>
              {isIntermediatePromptExpanded && (
                <p className="creative-card-prompt-text mb-0 mt-2">
                  {c.imageIntermediatePrompt?.trim()}
                </p>
              )}
            </div>
          )}
        </div>
      </article>
    );
  };

  return (
    <div className="mt-3">
      {feedback && (
        <div
          className={`creative-feedback creative-feedback-${feedback.variant}`}
          role="alert"
          aria-live="polite"
        >
          <div className="creative-feedback-icon" aria-hidden>
            {feedback.variant === "success" ? (
              <CheckCircle2 size={20} />
            ) : feedback.variant === "warning" ? (
              <AlertTriangle size={20} />
            ) : (
              <XCircle size={20} />
            )}
          </div>
          <div className="creative-feedback-content">
            <p className="creative-feedback-title">{feedback.title}</p>
            {feedback.description && (
              <p className="creative-feedback-description">
                {feedback.description}
              </p>
            )}
          </div>
          <button
            type="button"
            className="creative-feedback-close"
            onClick={dismissFeedback}
            aria-label="Dispensar aviso"
          >
            <X size={16} />
          </button>
        </div>
      )}
      <div className="mb-4">
        {alterationLocked ? (
          <div className="alert alert-secondary" role="status">
            Criativos e configurações de publicação bloqueados para alteração
            porque o experimento já foi liberado ou está em execução.
          </div>
        ) : null}
        <label className="form-label" htmlFor="experiment-instagram-id">
          Conta do Instagram <span className="text-danger">*</span>
        </label>
        <select
          id="experiment-instagram-id"
          className="form-select mb-2"
          value={experimentInstagramAccountId}
          onChange={(e) => setExperimentInstagramAccountId(e.target.value)}
          disabled={
            alterationLocked ||
            isSavingPageId ||
            isLoadingInstagramAccounts ||
            noInstagramAccounts
          }
        >
          <option value="">
            {isLoadingInstagramAccounts
              ? "Carregando contas cadastradas..."
              : noInstagramAccounts
                ? "Cadastre uma conta para continuar"
                : "Selecione uma conta"}
          </option>
          {Array.isArray(instagramAccounts) &&
            instagramAccounts.map((account) => (
              <option key={account.id} value={account.id}>
                {account.name} ({account.handle})
              </option>
            ))}
        </select>
        <div className="form-text mb-2">
          O worker utilizará esta conta como identidade do Instagram nas
          campanhas.
        </div>
        {noInstagramAccounts && (
          <div className="alert alert-warning" role="alert">
            Nenhuma conta do Instagram está cadastrada. Cadastre uma conta antes
            de publicar criativos neste experimento.
            <div className="mt-2">
              <a
                className="btn btn-outline-primary btn-sm"
                href="/accounts/instagram"
              >
                Abrir Contas do Instagram
              </a>
            </div>
          </div>
        )}
        <label className="form-label" htmlFor="experiment-page-id">
          Página do Facebook deste experimento
        </label>
        <div className="d-flex flex-wrap gap-2">
          <select
            id="experiment-page-id"
            className="form-select"
            value={experimentPageId}
            onChange={(e) => setExperimentPageId(e.target.value)}
            disabled={
              isSavingPageId || isLoadingFacebookPages || alterationLocked
            }
          >
            <option value="">
              {isLoadingFacebookPages
                ? "Carregando páginas cadastradas..."
                : "Nenhuma página selecionada"}
            </option>
            {Array.isArray(facebookPages) &&
              facebookPages.map((page) => (
                <option key={page.id} value={page.id}>
                  {page.name} ({page.pageId})
                </option>
              ))}
          </select>
          <button
            type="button"
            className="btn btn-primary d-flex align-items-center gap-2"
            onClick={handleSavePageId}
            disabled={
              isSavingPageId ||
              !experiment ||
              noInstagramAccounts ||
              alterationLocked
            }
          >
            {isSavingPageId ? (
              <>
                <span
                  className="spinner-border spinner-border-sm"
                  role="status"
                  aria-hidden
                />
                <span>Salvando...</span>
              </>
            ) : (
              <span>Salvar página</span>
            )}
          </button>
        </div>
        <div className="form-text">
          Todos os criativos aprovados publicarão na página selecionada. Deixe
          em branco para usar a página padrão configurada no worker.
        </div>
      </div>
      {pipelineAvailable ? (
        <div className="alert alert-primary d-flex flex-column flex-lg-row align-items-lg-center justify-content-between gap-3 mt-3">
          <div>
            <h4 className="h6 mb-1">Anúncios do pipeline prontos</h4>
            <p className="mb-0 small text-muted">
              Encontramos {pipelinePairs}{" "}
              {pipelinePairs === 1 ? "variação" : "variações"} com texto e
              briefing estruturados. O Worker AI usará o modelo gpt-image-2 para
              gerar as imagens alinhadas ao experimento.
            </p>
          </div>
          <div className="d-flex flex-column align-items-lg-end gap-2 w-100 w-lg-auto">
            <button
              type="button"
              className="btn btn-outline-primary d-flex align-items-center justify-content-center gap-2"
              onClick={handlePipelineRequest}
              disabled={pipelineButtonDisabled}
            >
              {pipelineRequest.isPending ? (
                <span
                  className="spinner-border spinner-border-sm"
                  role="status"
                />
              ) : (
                <Sparkles size={ICON_SIZE} />
              )}
              <span>
                {pipelineInProgress
                  ? "Gerando anúncios..."
                  : "Gerar anúncios do pipeline"}
              </span>
            </button>
            {(pipelineInProgress || pipelineHasRecoverableFailure) && (
              <span
                className={
                  pipelineHasRecoverableFailure
                    ? "badge text-bg-warning text-dark"
                    : "badge text-bg-info-subtle text-info-emphasis"
                }
              >
                {pipelineStatusLabel}
              </span>
            )}
            {pipelineHasRecoverableFailure &&
              experiment?.creativeGenerationError && (
                <span className="small text-muted text-lg-end">
                  {experiment.creativeGenerationError}
                </span>
              )}
          </div>
        </div>
      ) : pipelineInProgress ? (
        <div className="alert alert-info mt-3">
          <strong>Worker AI em produção.</strong> Estamos finalizando os
          anúncios solicitados com os ativos do pipeline.
        </div>
      ) : pipelineHasRecoverableFailure ? (
        <div className="alert alert-warning mt-3">
          <strong>Geração de anúncios liberada para nova tentativa.</strong>{" "}
          {experiment?.creativeGenerationError ||
            "A solicitação anterior não foi concluída pelo Worker AI."}
        </div>
      ) : null}

      {isLoading ? (
        <div className="d-flex justify-content-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Carregando...</span>
          </div>
        </div>
      ) : totalCreatives === 0 ? (
        <div className="creative-empty-state">
          <div className="creative-empty-icon" aria-hidden>
            🎨
          </div>
          <h3 className="h6 fw-semibold mb-1">Nenhum criativo cadastrado</h3>
          <p className="text-muted mb-2">
            Gere anúncios pelo pipeline para começar a testar variações e
            construir seu acervo criativo.
          </p>
        </div>
      ) : creativeSections.length === 0 ? (
        <div className="creative-grid">{creatives.map(renderCreativeCard)}</div>
      ) : (
        <div className="creative-sections">
          {creativeSections.map((section) => (
            <section
              key={section.id}
              className="creative-section"
              aria-labelledby={`${section.id}-title`}
            >
              <div className="creative-section-header">
                <h3
                  id={`${section.id}-title`}
                  className="creative-section-title"
                >
                  {section.title}
                </h3>
                <span className={`badge rounded-pill ${section.badgeClass}`}>
                  {`${section.creatives.length} ${
                    section.creatives.length === 1 ? "item" : "itens"
                  }`}
                </span>
              </div>
              <div className="creative-grid">
                {section.creatives.map(renderCreativeCard)}
              </div>
            </section>
          ))}
        </div>
      )}
      {showPreview && editing && (
        <div className="modal d-block" tabIndex={-1}>
          <div className="modal-dialog">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">Preview</h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => {
                    setShowPreview(false);
                    closeEdit();
                  }}
                />
              </div>
              <div className="modal-body">
                <InstagramAdPreview creative={editing} />
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
