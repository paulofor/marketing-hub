import { useMemo, useState } from "react";
import {
  CheckCircle2,
  ExternalLink,
  RefreshCcw,
  RotateCcw,
  Video,
  XCircle,
} from "lucide-react";
import { Link } from "react-router-dom";
import { toast } from "react-toastify";
import PageTitle from "../../components/PageTitle";
import {
  CreativeVideoReview,
  CreativeVideoReviewStatus,
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
  CREATIVE: "Criativo",
  EXPERIMENT_VIDEO_ASSET: "Vídeo do experimento",
};

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

export default function CreativeVideoReviewPage() {
  const [filter, setFilter] = useState<ReviewFilter>("DRAFT");
  const [rejectionReasons, setRejectionReasons] = useState<
    Record<string, string>
  >({});
  const reviewQuery = useCreativeVideoReviews(filter);
  const summaryQuery = useCreativeVideoReviews("ALL");
  const updateStatus = useUpdateCreativeVideoReviewStatus();
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
  const musaCount = summaryVideos.filter((video) =>
    `${video.experimentName} ${video.hypothesisTitle ?? ""} ${video.nicheName ?? ""}`
      .toLocaleLowerCase("pt-BR")
      .includes("musa"),
  ).length;

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
      const message =
        error instanceof Error ? error.message : "Falha ao atualizar vídeo";
      toast.error(message);
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
          <span>Relacionados a MUSA</span>
          <strong>{musaCount}</strong>
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
            const isPending = updateStatus.isPending;
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
                  </dl>

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
                        disabled={isPending || !mediaUrl}
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
