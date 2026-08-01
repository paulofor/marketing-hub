import { Link } from "react-router-dom";
import { ExternalLink, PlayCircle } from "lucide-react";
import {
  type ExperimentVideoAsset,
  useAllExperimentVideoAssets,
} from "../../api/experiment/useExperimentVideoAssets";
import PageTitle from "../../components/PageTitle";
import { getStudioCommercialLabel } from "./audioVideoStudioLabels";
import "./AudioVideoStudioPage.css";

function formatDate(value?: string | null) {
  if (!value) {
    return "Sem data";
  }

  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function formatDuration(value?: number | null) {
  if (!value) {
    return "Duracao nao informada";
  }

  const minutes = Math.floor(value / 60);
  const seconds = value % 60;
  return `${minutes}:${seconds.toString().padStart(2, "0")}`;
}

function getPlaybackUrl(video: ExperimentVideoAsset) {
  return video.hlsPlaybackUrl || video.assetUrl || "";
}

function getLearningPriority(video: ExperimentVideoAsset) {
  if (video.status === "READY" && video.reviewStatus === "APPROVED") {
    return "Aprender com gancho, ritmo, prova e CTA.";
  }

  if (video.reviewStatus === "REJECTED") {
    return "Aprender com a causa da rejeicao antes de refazer.";
  }

  return "Analisar antes de usar em campanha ou PDE.";
}

export default function AudioVideoStudioVideosAnalysisPage() {
  const videoLibraryQuery = useAllExperimentVideoAssets();
  const videos = videoLibraryQuery.data ?? [];

  return (
    <div className="audio-video-studio-page">
      <PageTitle
        title="Videos para analise"
        subtitle="Lista de videos comerciais para revisar, comparar e transformar em aprendizados de criativo, funil e oferta."
      />

      <section className="audio-video-studio-page__section">
        <div className="audio-video-studio-page__section-heading audio-video-studio-page__section-heading--actions">
          <div>
            <h2>Biblioteca de aprendizado</h2>
            <p>
              Use esta fila para escolher videos que merecem analise comercial:
              o que prende atencao, o que prova valor e o que precisa virar nova
              variacao.
            </p>
          </div>
          <Link
            className="audio-video-studio-page__secondary-action"
            to="/audio-video-studio"
          >
            <PlayCircle size={18} aria-hidden="true" />
            Novo projeto
          </Link>
        </div>

        {videoLibraryQuery.isLoading ? (
          <article className="audio-video-studio-page__project-card">
            Carregando videos para analise...
          </article>
        ) : videoLibraryQuery.isError ? (
          <article className="audio-video-studio-page__project-card">
            Nao foi possivel carregar os videos agora.
          </article>
        ) : videos.length === 0 ? (
          <article className="audio-video-studio-page__project-card">
            Nenhum video comercial encontrado para analise.
          </article>
        ) : (
          <div className="audio-video-studio-page__project-table-wrapper">
            <table className="audio-video-studio-page__project-table">
              <thead>
                <tr>
                  <th>Video</th>
                  <th>Status</th>
                  <th>Papel no funil</th>
                  <th>Aprendizado</th>
                  <th>Criado em</th>
                  <th>Acesso</th>
                </tr>
              </thead>
              <tbody>
                {videos.map((video) => {
                  const playbackUrl = getPlaybackUrl(video);

                  return (
                    <tr key={video.id}>
                      <td>
                        <strong>#{video.id}</strong>
                        <span>{video.objective}</span>
                        <small>
                          {video.provider} · {video.model} ·{" "}
                          {formatDuration(video.durationSeconds)}
                        </small>
                      </td>
                      <td>
                        {getStudioCommercialLabel(video.status)}
                        <small>
                          Revisao:{" "}
                          {getStudioCommercialLabel(video.reviewStatus)}
                        </small>
                      </td>
                      <td>
                        {getStudioCommercialLabel(video.slot)}
                        <small>
                          Metrica:{" "}
                          {getStudioCommercialLabel(video.primaryMetric)}
                        </small>
                      </td>
                      <td>
                        <span>{getLearningPriority(video)}</span>
                        {video.rejectionReason ? (
                          <small>{video.rejectionReason}</small>
                        ) : null}
                      </td>
                      <td>{formatDate(video.createdAt)}</td>
                      <td>
                        {playbackUrl ? (
                          <a
                            className="audio-video-studio-page__project-open-link"
                            href={playbackUrl}
                            target="_blank"
                            rel="noreferrer"
                          >
                            <ExternalLink size={16} aria-hidden="true" />
                            <span>Abrir video</span>
                          </a>
                        ) : (
                          <small>Sem URL publica</small>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
