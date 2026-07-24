import { useMemo, useState } from "react";
import { CheckCircle2, ExternalLink, RefreshCcw, RotateCcw, Video } from "lucide-react";
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
  { value: "ALL", label: "Todos" },
];

const STATUS_LABELS: Record<CreativeVideoReviewStatus, string> = {
  DRAFT: "Pendente",
  READY: "Aprovado",
};

function statusClassName(status: CreativeVideoReviewStatus) {
  return status === "READY"
    ? "creative-video-review-page__status creative-video-review-page__status--ready"
    : "creative-video-review-page__status creative-video-review-page__status--draft";
}

function primaryMediaUrl(video: CreativeVideoReview) {
  return video.videoUrl?.trim() ?? "";
}

export default function CreativeVideoReviewPage() {
  const [filter, setFilter] = useState<ReviewFilter>("DRAFT");
  const reviewQuery = useCreativeVideoReviews(filter);
  const updateStatus = useUpdateCreativeVideoReviewStatus();
  const videos = useMemo(() => reviewQuery.data ?? [], [reviewQuery.data]);
  const pendingCount = videos.filter((video) => video.status === "DRAFT").length;
  const approvedCount = videos.filter((video) => video.status === "READY").length;
  const musaCount = videos.filter((video) =>
    `${video.experimentName} ${video.hypothesisTitle ?? ""} ${video.nicheName ?? ""}`
      .toLocaleLowerCase("pt-BR")
      .includes("musa"),
  ).length;

  const handleStatusChange = async (
    video: CreativeVideoReview,
    status: CreativeVideoReviewStatus,
  ) => {
    try {
      await updateStatus.mutateAsync({ id: video.id, status });
      toast.success(status === "READY" ? "Vídeo aprovado para campanha" : "Vídeo voltou para revisão");
    } catch (error) {
      const message = error instanceof Error ? error.message : "Falha ao atualizar vídeo";
      toast.error(message);
    }
  };

  return (
    <div className="creative-video-review-page">
      <div className="creative-video-review-page__header">
        <div>
          <PageTitle title="Aprovação de vídeos" />
          <p className="creative-video-review-page__subtitle">
            Fila comercial para revisar vídeos antes de liberar criativos de campanha.
          </p>
        </div>
      </div>

      <section className="creative-video-review-page__summary" aria-label="Resumo da fila">
        <div className="creative-video-review-page__metric">
          <span>Pendentes no filtro</span>
          <strong>{pendingCount}</strong>
        </div>
        <div className="creative-video-review-page__metric">
          <span>Aprovados no filtro</span>
          <strong>{approvedCount}</strong>
        </div>
        <div className="creative-video-review-page__metric">
          <span>Relacionados a MUSA</span>
          <strong>{musaCount}</strong>
        </div>
      </section>

      <div className="creative-video-review-page__toolbar">
        <div className="creative-video-review-page__filters" role="group" aria-label="Filtro de status">
          {REVIEW_FILTERS.map((option) => (
            <button
              key={option.value}
              type="button"
              className={[
                "creative-video-review-page__filter",
                filter === option.value ? "creative-video-review-page__filter--active" : "",
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
        <div className="creative-video-review-page__empty-state">Carregando vídeos...</div>
      ) : reviewQuery.isError ? (
        <div className="creative-video-review-page__empty-state">
          Não foi possível carregar a fila de vídeos.
        </div>
      ) : videos.length === 0 ? (
        <div className="creative-video-review-page__empty-state">
          Nenhum vídeo encontrado para este filtro.
        </div>
      ) : (
        <section className="creative-video-review-page__list" aria-label="Vídeos para aprovação">
          {videos.map((video) => {
            const mediaUrl = primaryMediaUrl(video);
            const isPending = updateStatus.isPending;
            return (
              <article className="creative-video-review-page__card" key={video.id}>
                <div className="creative-video-review-page__preview">
                  {mediaUrl ? (
                    <video src={mediaUrl} controls preload="metadata" playsInline />
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
                    <span>#{video.id}</span>
                  </div>

                  <div className="creative-video-review-page__copy">
                    <h2>{video.headline || "Criativo sem headline"}</h2>
                    {video.primaryText ? <p>{video.primaryText}</p> : null}
                  </div>

                  <dl className="creative-video-review-page__context">
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

                  <div className="creative-video-review-page__actions">
                    {video.status !== "READY" ? (
                      <button
                        type="button"
                        className="btn btn-success btn-sm"
                        disabled={isPending || !mediaUrl}
                        onClick={() => handleStatusChange(video, "READY")}
                      >
                        <CheckCircle2 size={16} aria-hidden="true" />
                        {isPending ? "Aprovando..." : "Aprovar"}
                      </button>
                    ) : (
                      <button
                        type="button"
                        className="btn btn-outline-warning btn-sm"
                        disabled={isPending}
                        onClick={() => handleStatusChange(video, "DRAFT")}
                      >
                        <RotateCcw size={16} aria-hidden="true" />
                        {isPending ? "Atualizando..." : "Voltar para revisão"}
                      </button>
                    )}
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
