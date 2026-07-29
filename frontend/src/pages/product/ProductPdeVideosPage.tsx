import { useMemo } from "react";
import { Link, useParams } from "react-router-dom";
import { Film, PlayCircle } from "lucide-react";
import { useAllExperimentVideoAssets } from "../../api/experiment/useExperimentVideoAssets";
import type { ExperimentVideoAsset } from "../../api/experiment/useExperimentVideoAssets";
import { useProductPdeProductionSlots } from "../../api/product/usePdeProductionSlots";
import { useProduct } from "../../api/product/useProduct";
import type { PostDeployPdeProductionSlot } from "../../api/experiment/usePostDeployMonitor";
import { AdaptiveVideoPlayer } from "../../components/AdaptiveVideoPlayer";
import PageTitle from "../../components/PageTitle";

function formatDuration(value?: number | null) {
  if (!value || value <= 0) return "—";
  const minutes = Math.floor(value / 60);
  const seconds = value % 60;
  return minutes > 0 ? `${minutes}m ${seconds}s` : `${seconds}s`;
}

function isHlsVideo(asset: ExperimentVideoAsset) {
  return Boolean(asset.hlsPlaybackUrl?.trim().includes(".m3u8"));
}

function resolvePlaybackUrl(
  slot: PostDeployPdeProductionSlot,
  asset: ExperimentVideoAsset,
) {
  const rawUrl = asset.hlsPlaybackUrl?.trim();
  if (!rawUrl) return "";
  if (/^https?:\/\//i.test(rawUrl)) return rawUrl;

  try {
    return new URL(rawUrl, slot.publicUrl).toString();
  } catch {
    return rawUrl;
  }
}

function describeVideoFunction(index: number) {
  const videoFunctions = [
    "Abertura / hero",
    "Prova visual",
    "Mecanismo",
    "Objeção",
    "Reforço de CTA",
  ];
  return videoFunctions[index] ?? "Função complementar";
}

function findSlotVideos(
  slot: PostDeployPdeProductionSlot,
  assets: ExperimentVideoAsset[],
) {
  if (!slot.sourceExperimentId) return [];
  return assets
    .filter(
      (asset) =>
        asset.experimentId === slot.sourceExperimentId &&
        asset.slot === "LANDING_HERO" &&
        isHlsVideo(asset),
    )
    .sort((current, next) => {
      if (current.reviewStatus !== next.reviewStatus) {
        return current.reviewStatus === "APPROVED" ? -1 : 1;
      }
      if (current.status !== next.status) {
        return current.status === "READY" ? -1 : 1;
      }
      return next.id - current.id;
    });
}

export default function ProductPdeVideosPage() {
  const { productId } = useParams();
  const productQuery = useProduct(productId);
  const slotsQuery = useProductPdeProductionSlots(productId);
  const videoAssetsQuery = useAllExperimentVideoAssets();
  const product = productQuery.data;
  const slots = useMemo(
    () =>
      [...(slotsQuery.data ?? [])].sort((current, next) =>
        current.slotCode.localeCompare(next.slotCode, "pt-BR", {
          numeric: true,
        }),
      ),
    [slotsQuery.data],
  );
  const assets = videoAssetsQuery.data ?? [];

  if (
    productQuery.isLoading ||
    slotsQuery.isLoading ||
    videoAssetsQuery.isLoading
  ) {
    return <p className="text-muted">Carregando vídeos HLS das versões PDE...</p>;
  }

  if (!product) {
    return <div className="alert alert-danger">Produto não encontrado.</div>;
  }

  return (
    <div>
      <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-4">
        <div>
          <PageTitle>Vídeos das versões PDE</PageTitle>
          <p className="text-muted mb-0">
            {product.name || product.slug} · cada versão PDE pode ter mais de
            um vídeo HLS com funções comerciais complementares.
          </p>
        </div>
        <div className="d-flex flex-wrap gap-2">
          <Link
            className="btn btn-outline-primary"
            to={`/products/${productId}/pde-versions`}
          >
            Versões PDE
          </Link>
          <Link className="btn btn-outline-secondary" to="/products">
            Voltar para produtos
          </Link>
        </div>
      </div>

      {slots.length === 0 ? (
        <div className="alert alert-light border">
          Nenhuma versão PDE cadastrada para este produto.
        </div>
      ) : (
        <div className="row g-3">
          {slots.map((slot) => {
            const slotVideos = findSlotVideos(slot, assets);
            const primaryVideo = slotVideos[0];
            const playbackUrl = primaryVideo
              ? resolvePlaybackUrl(slot, primaryVideo)
              : "";

            return (
              <div className="col-12 col-xl-6" key={slot.id}>
                <article className="card h-100">
                  <div className="card-body">
                    <div className="d-flex flex-wrap align-items-start justify-content-between gap-2 mb-3">
                      <div>
                        <div className="d-flex align-items-center gap-2">
                          <Film size={18} className="text-primary" />
                          <h2 className="h5 mb-0">{slot.slotCode}</h2>
                        </div>
                        <div className="small text-muted font-monospace mt-1">
                          {slot.experienceVersion}
                        </div>
                        <div className="small text-muted mt-1">
                          {slotVideos.length} vídeo
                          {slotVideos.length === 1 ? "" : "s"} HLS vinculado
                          {slotVideos.length === 1 ? "" : "s"}
                        </div>
                      </div>
                      <span className="badge text-bg-light">{slot.status}</span>
                    </div>

                    {primaryVideo && playbackUrl ? (
                      <>
                        <div className="alert alert-info py-2 mb-3">
                          A versão PDE aceita múltiplos vídeos com funções
                          diferentes, como abertura, prova, mecanismo, objeções
                          e reforço de CTA. O player abaixo destaca o primeiro
                          aprovado para prévia; a tabela preserva todos os HLS
                          vinculados ao experimento origem.
                        </div>
                        <div className="ratio ratio-16x9 bg-dark rounded overflow-hidden mb-3">
                          <AdaptiveVideoPlayer
                            src={playbackUrl}
                            poster={primaryVideo.thumbnailUrl ?? undefined}
                            className="w-100 h-100"
                            controls
                          />
                        </div>
                        <div className="table-responsive">
                          <table className="table table-sm mb-0">
                            <thead>
                              <tr>
                                <th scope="col">Função</th>
                                <th scope="col">Ativo</th>
                                <th scope="col">Status</th>
                                <th scope="col">Duração</th>
                                <th scope="col">HLS</th>
                              </tr>
                            </thead>
                            <tbody>
                              {slotVideos.map((video, index) => {
                                const videoPlaybackUrl = resolvePlaybackUrl(
                                  slot,
                                  video,
                                );
                                return (
                                  <tr key={video.id}>
                                    <th scope="row">
                                      {describeVideoFunction(index)}
                                    </th>
                                    <td>#{video.id}</td>
                                    <td>
                                      {video.status} · {video.reviewStatus}
                                    </td>
                                    <td>
                                      {formatDuration(video.durationSeconds)}
                                    </td>
                                    <td className="font-monospace small">
                                      <a
                                        href={videoPlaybackUrl}
                                        target="_blank"
                                        rel="noreferrer"
                                      >
                                        {videoPlaybackUrl}
                                      </a>
                                    </td>
                                  </tr>
                                );
                              })}
                            </tbody>
                          </table>
                        </div>
                        <div className="small text-muted mt-2">
                          Experimento origem: {slot.sourceExperimentId}
                        </div>
                      </>
                    ) : (
                      <div className="alert alert-warning mb-3">
                        Esta versão ainda não tem vídeos HLS LANDING_HERO
                        vinculados ao experimento origem.
                      </div>
                    )}

                    <div className="d-flex flex-wrap gap-2 mt-3">
                      <a
                        className="btn btn-outline-primary btn-sm"
                        href={slot.publicUrl}
                        target="_blank"
                        rel="noreferrer"
                      >
                        <PlayCircle size={16} /> Abrir PDE
                      </a>
                      {slot.sourceExperimentId && (
                        <Link
                          className="btn btn-outline-secondary btn-sm"
                          to={`/experiments/${slot.sourceExperimentId}`}
                        >
                          Ver experimento
                        </Link>
                      )}
                    </div>
                  </div>
                </article>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
