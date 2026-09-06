import { useMemo, useState } from "react";
import {
  CheckCircle2,
  ExternalLink,
  RefreshCcw,
  RotateCcw,
  Sparkles,
  Video,
  XCircle,
} from "lucide-react";
import axios from "axios";
import { Link } from "react-router-dom";
import { toast } from "react-toastify";
import PageTitle from "../../components/PageTitle";
import {
  CreativeVideoReview,
  CreativeAgentReviewStatus,
  CreativeVideoReviewStatus,
  useRequestCreativeVideoAgentReview,
  useCreativeVideoReviews,
  useUpdateCreativeVideoReviewStatus,
} from "../../api/creative/useCreativeVideoReviews";
import "./CreativeVideoReviewPage.css";

type ReviewFilter = "ALL" | CreativeVideoReviewStatus;

const REVIEW_FILTERS: Array<{ value: ReviewFilter; label: string }> = [
  { value: "DRAFT", label: "Pendentes" },
  { value: "READY", label: "Aprovados" },
  { value: "REJECTED", label: "Reprovados" },
  { value: "ALL", label: "Todos" },
];

const STATUS_LABELS: Record<CreativeVideoReviewStatus, string> = {
  DRAFT: "Pendente",
  READY: "No portfólio",
  REJECTED: "Reprovado",
};

const SOURCE_LABELS: Record<CreativeVideoReview["sourceType"], string> = {
  CREATIVE: "Criativo de anúncio",
  EXPERIMENT_VIDEO_ASSET: "Vídeo produzido",
};

const USD_TO_BRL_RATE = 5;

const FUNNEL_SLOT_LABELS: Record<
  NonNullable<CreativeVideoReview["funnelSlot"]>,
  string
> = {
  AD: "Anúncio",
  LANDING_HERO: "PDE / hero da página",
  FORM_EXPLAINER: "Explicação do diagnóstico",
  PRE_CHECKOUT: "Pré-checkout",
};

function funnelSlotLabel(video: CreativeVideoReview) {
  if (video.funnelSlot && FUNNEL_SLOT_LABELS[video.funnelSlot]) {
    return FUNNEL_SLOT_LABELS[video.funnelSlot];
  }
  return video.sourceType === "CREATIVE"
    ? FUNNEL_SLOT_LABELS.AD
    : "Não informado";
}

function statusClassName(status: CreativeVideoReviewStatus) {
  if (status === "READY") {
    return "creative-video-review-page__status creative-video-review-page__status--ready";
  }
  if (status === "REJECTED") {
    return "creative-video-review-page__status creative-video-review-page__status--rejected";
  }
  return "creative-video-review-page__status creative-video-review-page__status--draft";
}

function primaryMediaUrl(video: CreativeVideoReview) {
  return video.videoUrl?.trim() ?? "";
}

function reviewKey(video: CreativeVideoReview) {
  return `${video.sourceType}:${video.id}`;
}

const AGENT_REVIEW_LABELS: Record<CreativeAgentReviewStatus, string> = {
  PENDING: "aguardando análise",
  PROCESSING: "analisando agora",
  APPROVED: "aprovado",
  ADJUST: "ajustes necessários",
  REJECTED: "reprovado",
  FAILED: "falha técnica",
};

function canRequestAgentReview(
  status: CreativeAgentReviewStatus | null | undefined,
) {
  return status == null || ["ADJUST", "REJECTED", "FAILED"].includes(status);
}

function apiErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError<{ message?: string }>(error)) {
    const backendMessage = error.response?.data?.message;
    if (backendMessage?.trim()) {
      return backendMessage.trim();
    }
  }
  return error instanceof Error && error.message ? error.message : fallback;
}

function toNumber(value: number | string | null | undefined) {
  if (value == null || value === "") {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

const usdFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "USD",
  minimumFractionDigits: 4,
  maximumFractionDigits: 4,
});

const brlFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const dateTimeFormatter = new Intl.DateTimeFormat("pt-BR", {
  dateStyle: "short",
  timeStyle: "short",
});

function formatUsdWithBrl(value: number | string | null | undefined) {
  const numeric = toNumber(value);
  if (numeric == null) {
    return "Não registrado";
  }
  return `${usdFormatter.format(numeric)} · ${brlFormatter.format(
    numeric * USD_TO_BRL_RATE,
  )}`;
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return "Sem decisão";
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "Data inválida"
    : dateTimeFormatter.format(date);
}

function isInCurrentMonth(value: string | null | undefined) {
  if (!value) {
    return false;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return false;
  }
  const now = new Date();
  return (
    date.getUTCFullYear() === now.getUTCFullYear() &&
    date.getUTCMonth() === now.getUTCMonth()
  );
}

function isInCurrentYear(value: string | null | undefined) {
  if (!value) {
    return false;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return false;
  }
  return date.getUTCFullYear() === new Date().getUTCFullYear();
}

function costReferenceDate(video: CreativeVideoReview) {
  return video.createdAt ?? video.reviewedAt;
}

export default function CreativeVideoReviewPage() {
  const [filter, setFilter] = useState<ReviewFilter>("DRAFT");
  const [rejectionReasons, setRejectionReasons] = useState<
    Record<string, string>
  >({});
  const reviewQuery = useCreativeVideoReviews(filter);
  const summaryQuery = useCreativeVideoReviews("ALL");
  const updateStatus = useUpdateCreativeVideoReviewStatus();
  const requestAgentReview = useRequestCreativeVideoAgentReview();
  const videos = useMemo(() => reviewQuery.data ?? [], [reviewQuery.data]);
  const summaryVideos = useMemo(
    () => summaryQuery.data ?? videos,
    [summaryQuery.data, videos],
  );
  const pendingCount = summaryVideos.filter(
    (video) => video.status === "DRAFT",
  ).length;
  const approvedCount = summaryVideos.filter(
    (video) => video.status === "READY",
  ).length;
  const rejectedCount = summaryVideos.filter(
    (video) => video.status === "REJECTED",
  ).length;
  const totalProductionCost = summaryVideos.reduce(
    (sum, video) => sum + (toNumber(video.totalProductionCostUsd) ?? 0),
    0,
  );
  const monthlyProductionCost = summaryVideos
    .filter((video) => isInCurrentMonth(costReferenceDate(video)))
    .reduce(
      (sum, video) => sum + (toNumber(video.totalProductionCostUsd) ?? 0),
      0,
    );
  const yearlyProductionCost = summaryVideos
    .filter((video) => isInCurrentYear(costReferenceDate(video)))
    .reduce(
      (sum, video) => sum + (toNumber(video.totalProductionCostUsd) ?? 0),
      0,
    );
  const rejectedProductionCost = summaryVideos
    .filter((video) => video.status === "REJECTED")
    .reduce(
      (sum, video) => sum + (toNumber(video.totalProductionCostUsd) ?? 0),
      0,
    );

  const handleStatusChange = async (
    video: CreativeVideoReview,
    status: CreativeVideoReviewStatus,
    rejectionReason?: string,
  ) => {
    try {
      await updateStatus.mutateAsync({
        id: video.id,
        sourceType: video.sourceType,
        status,
        rejectionReason,
      });
      if (status === "READY") {
        toast.success("Vídeo aprovado e liberado para o portfólio do produto");
      } else if (status === "REJECTED") {
        toast.success("Vídeo reprovado com motivo registrado");
      } else {
        toast.success("Vídeo voltou para revisão");
      }
    } catch (error) {
      toast.error(apiErrorMessage(error, "Falha ao atualizar vídeo"));
    }
  };

  const handleAgentReviewRequest = async (video: CreativeVideoReview) => {
    try {
      await requestAgentReview.mutateAsync(video.id);
      toast.success("Anúncio reenviado para a revisão independente de Têmis");
    } catch (error) {
      toast.error(
        apiErrorMessage(error, "Falha ao reenviar o anúncio para Têmis"),
      );
    }
  };

  const handleReject = (video: CreativeVideoReview) => {
    const reason = rejectionReasons[reviewKey(video)]?.trim() ?? "";
    if (!reason) {
      toast.error("Informe o motivo da reprovação.");
      return;
    }
    handleStatusChange(video, "REJECTED", reason);
  };

  return (
    <div className="creative-video-review-page">
      <div className="creative-video-review-page__header">
        <div>
          <PageTitle title="Aprovação de vídeos" />
          <p className="creative-video-review-page__subtitle">
            Fila humana final: só vídeos aprovados aqui entram no portfólio do
            produto para campanhas ou PDEs.
          </p>
        </div>
      </div>

      <section
        className="creative-video-review-page__summary"
        aria-label="Resumo da fila"
      >
        <div className="creative-video-review-page__metric">
          <span>Pendentes</span>
          <strong>{pendingCount}</strong>
        </div>
        <div className="creative-video-review-page__metric">
          <span>Aprovados</span>
          <strong>{approvedCount}</strong>
        </div>
        <div className="creative-video-review-page__metric">
          <span>Reprovados</span>
          <strong>{rejectedCount}</strong>
        </div>
        <div className="creative-video-review-page__metric">
          <span>Custo total</span>
          <strong>{formatUsdWithBrl(totalProductionCost)}</strong>
        </div>
        <div className="creative-video-review-page__metric">
          <span>Custo mês</span>
          <strong>{formatUsdWithBrl(monthlyProductionCost)}</strong>
        </div>
        <div className="creative-video-review-page__metric">
          <span>Custo ano</span>
          <strong>{formatUsdWithBrl(yearlyProductionCost)}</strong>
        </div>
        <div className="creative-video-review-page__metric">
          <span>Custo reprovado</span>
          <strong>{formatUsdWithBrl(rejectedProductionCost)}</strong>
        </div>
      </section>

      <div className="creative-video-review-page__toolbar">
        <div
          className="creative-video-review-page__filters"
          role="group"
          aria-label="Filtro de status"
        >
          {REVIEW_FILTERS.map((option) => (
            <button
              key={option.value}
              type="button"
              className={[
                "creative-video-review-page__filter",
                filter === option.value
                  ? "creative-video-review-page__filter--active"
                  : "",
              ]
                .filter(Boolean)
                .join(" ")}
              onClick={() => setFilter(option.value)}
            >
              {option.label}
            </button>
          ))}
        </div>
        <div className="creative-video-review-page__refresh">
          <button
            type="button"
            className="btn btn-outline-secondary btn-sm"
            onClick={() => reviewQuery.refetch()}
            disabled={reviewQuery.isFetching}
          >
            <RefreshCcw size={16} aria-hidden="true" />
            {reviewQuery.isFetching ? "Atualizando..." : "Atualizar"}
          </button>
        </div>
      </div>

      {reviewQuery.isLoading ? (
        <div className="creative-video-review-page__empty-state">
          Carregando vídeos...
        </div>
      ) : reviewQuery.isError ? (
        <div className="creative-video-review-page__empty-state">
          Não foi possível carregar a fila de vídeos.
        </div>
      ) : videos.length === 0 ? (
        <div className="creative-video-review-page__empty-state">
          Nenhum vídeo encontrado para este filtro.
        </div>
      ) : (
        <section
          className="creative-video-review-page__list"
          aria-label="Vídeos para aprovação"
        >
          {videos.map((video) => {
            const mediaUrl = primaryMediaUrl(video);
            const isPending =
              updateStatus.isPending || requestAgentReview.isPending;
            const key = reviewKey(video);
            return (
              <article className="creative-video-review-page__card" key={key}>
                <div className="creative-video-review-page__preview">
                  {mediaUrl ? (
                    <video
                      src={mediaUrl}
                      controls
                      preload="metadata"
                      playsInline
                    />
                  ) : (
                    <div className="creative-video-review-page__empty-preview">
                      <Video size={28} aria-hidden="true" />
                      <span>Vídeo sem URL pública</span>
                    </div>
                  )}
                </div>
                <div className="creative-video-review-page__body">
                  <div className="creative-video-review-page__topline">
                    <span className={statusClassName(video.status)}>
                      {STATUS_LABELS[video.status]}
                    </span>
                    <span>
                      {SOURCE_LABELS[video.sourceType]} #{video.id}
                    </span>
                  </div>

                  <div className="creative-video-review-page__copy">
                    <h2>{video.headline || "Criativo sem headline"}</h2>
                    {video.primaryText ? <p>{video.primaryText}</p> : null}
                  </div>

                  <dl className="creative-video-review-page__context">
                    <div>
                      <dt>Origem</dt>
                      <dd>{SOURCE_LABELS[video.sourceType]}</dd>
                    </div>
                    <div>
                      <dt>Uso no funil</dt>
                      <dd>{funnelSlotLabel(video)}</dd>
                    </div>
                    <div>
                      <dt>Experimento</dt>
                      <dd>
                        <Link to={`/experiments/${video.experimentId}`}>
                          {video.experimentName}
                        </Link>
                      </dd>
                    </div>
                    <div>
                      <dt>Status do experimento</dt>
                      <dd>{video.experimentStatus}</dd>
                    </div>
                    <div>
                      <dt>Decisão</dt>
                      <dd>{formatDateTime(video.reviewedAt)}</dd>
                    </div>
                    <div>
                      <dt>Hipótese</dt>
                      <dd>{video.hypothesisTitle ?? "Sem hipótese"}</dd>
                    </div>
                    <div>
                      <dt>Nicho</dt>
                      <dd>{video.nicheName ?? "Sem nicho"}</dd>
                    </div>
                    <div>
                      <dt>Destino</dt>
                      <dd>{video.destinationUrl ?? "Sem destino"}</dd>
                    </div>
                    <div>
                      <dt>CTA</dt>
                      <dd>{video.cta ?? "Sem CTA"}</dd>
                    </div>
                    <div>
                      <dt>Origem visual</dt>
                      <dd>{video.visualSourceKey ?? "Não registrada"}</dd>
                    </div>
                    <div>
                      <dt>Tipo da origem</dt>
                      <dd>{video.visualSourceType ?? "Não informado"}</dd>
                    </div>
                  </dl>

                  <dl className="creative-video-review-page__costs">
                    <div>
                      <dt>Vídeo</dt>
                      <dd>{formatUsdWithBrl(video.videoCostUsd)}</dd>
                    </div>
                    <div>
                      <dt>Áudio separado</dt>
                      <dd>{formatUsdWithBrl(video.audioCostUsd)}</dd>
                    </div>
                    <div>
                      <dt>Total produção</dt>
                      <dd>{formatUsdWithBrl(video.totalProductionCostUsd)}</dd>
                    </div>
                  </dl>

                  {video.visualSourceDescription ? (
                    <div className="creative-video-review-page__lineage">
                      <strong>Base do vídeo</strong>
                      <p>{video.visualSourceDescription}</p>
                    </div>
                  ) : null}

                  {video.visualSimilarityOverrideReason ? (
                    <div className="creative-video-review-page__lineage">
                      <strong>Exceção de semelhança visual</strong>
                      <p>{video.visualSimilarityOverrideReason}</p>
                    </div>
                  ) : null}

                  {video.sourceType === "CREATIVE" &&
                  video.agentReviewStatus ? (
                    <div
                      className={`alert py-2 ${
                        video.agentReviewStatus === "APPROVED"
                          ? "alert-success"
                          : "alert-warning"
                      }`}
                      role="status"
                    >
                      <strong>
                        Revisão de Têmis:{" "}
                        {AGENT_REVIEW_LABELS[video.agentReviewStatus]}.
                      </strong>
                      {video.agentReviewSummary ? (
                        <p className="mb-1">{video.agentReviewSummary}</p>
                      ) : null}
                      {video.approvalBlockedReason ? (
                        <p className="mb-0">{video.approvalBlockedReason}</p>
                      ) : null}
                    </div>
                  ) : null}

                  {video.status === "REJECTED" && video.rejectionReason ? (
                    <div className="creative-video-review-page__rejection">
                      <strong>Motivo da reprovação</strong>
                      <p>{video.rejectionReason}</p>
                    </div>
                  ) : null}

                  {video.status === "DRAFT" ? (
                    <label className="creative-video-review-page__reject-reason">
                      <span>
                        Motivo da reprovação <b aria-hidden="true">*</b>
                      </span>
                      <textarea
                        value={rejectionReasons[key] ?? ""}
                        onChange={(event) =>
                          setRejectionReasons((current) => ({
                            ...current,
                            [key]: event.target.value,
                          }))
                        }
                        placeholder="Explique o que precisa ser corrigido antes de liberar para o portfólio."
                        rows={3}
                      />
                    </label>
                  ) : null}

                  <div className="creative-video-review-page__actions">
                    {video.status !== "READY" ? (
                      <button
                        type="button"
                        className="btn btn-success btn-sm"
                        disabled={
                          isPending ||
                          !mediaUrl ||
                          Boolean(video.approvalBlockedReason)
                        }
                        title={video.approvalBlockedReason ?? undefined}
                        onClick={() => handleStatusChange(video, "READY")}
                      >
                        {isPending ? (
                          <span
                            className="spinner-border spinner-border-sm"
                            role="status"
                            aria-hidden="true"
                          />
                        ) : (
                          <CheckCircle2 size={16} aria-hidden="true" />
                        )}
                        {isPending ? "Aprovando..." : "Aprovar para portfólio"}
                      </button>
                    ) : (
                      <button
                        type="button"
                        className="btn btn-outline-warning btn-sm"
                        disabled={isPending}
                        onClick={() => handleStatusChange(video, "DRAFT")}
                      >
                        {isPending ? (
                          <span
                            className="spinner-border spinner-border-sm"
                            role="status"
                            aria-hidden="true"
                          />
                        ) : (
                          <RotateCcw size={16} aria-hidden="true" />
                        )}
                        {isPending ? "Atualizando..." : "Voltar para revisão"}
                      </button>
                    )}
                    {video.sourceType === "CREATIVE" &&
                    canRequestAgentReview(video.agentReviewStatus) ? (
                      <button
                        type="button"
                        className="btn btn-outline-warning btn-sm"
                        disabled={isPending}
                        onClick={() => handleAgentReviewRequest(video)}
                      >
                        {requestAgentReview.isPending ? (
                          <span
                            className="spinner-border spinner-border-sm"
                            role="status"
                            aria-hidden="true"
                          />
                        ) : (
                          <Sparkles size={16} aria-hidden="true" />
                        )}
                        {requestAgentReview.isPending
                          ? "Reenviando..."
                          : "Reavaliar com Têmis"}
                      </button>
                    ) : null}
                    {video.status === "DRAFT" ? (
                      <button
                        type="button"
                        className="btn btn-outline-danger btn-sm"
                        disabled={isPending || !rejectionReasons[key]?.trim()}
                        onClick={() => handleReject(video)}
                      >
                        {isPending ? (
                          <span
                            className="spinner-border spinner-border-sm"
                            role="status"
                            aria-hidden="true"
                          />
                        ) : (
                          <XCircle size={16} aria-hidden="true" />
                        )}
                        {isPending ? "Reprovando..." : "Reprovar"}
                      </button>
                    ) : null}
                    {mediaUrl ? (
                      <a
                        className="btn btn-outline-secondary btn-sm"
                        href={mediaUrl}
                        target="_blank"
                        rel="noreferrer"
                      >
                        <ExternalLink size={16} aria-hidden="true" />
                        Abrir vídeo
                      </a>
                    ) : null}
                  </div>
                </div>
              </article>
            );
          })}
        </section>
      )}
    </div>
  );
}
