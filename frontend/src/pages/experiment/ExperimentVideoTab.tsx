import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { CheckCircle2, XCircle } from "lucide-react";
import { toast } from "react-toastify";
import { useGeraSalesPagePublications } from "../../api/experiment/useGeraSalesPagePublications";
import type { Experiment } from "../../api/experiment/useExperiments";
import {
  ExperimentVideoAsset,
  ExperimentVideoReviewStatus,
  useExperimentVideoAssets,
} from "../../api/experiment/useExperimentVideoAssets";
import { useUpdateExperimentVideoAssetReview } from "../../api/experiment/useUpdateExperimentVideoAssetReview";
import { useExperimentVideoPerformanceDashboard } from "../../api/experiment/useExperimentVideoPerformanceDashboard";
import { useTenantContext } from "../../utils/tenantContext";
import { resolveAssetUrl } from "../../utils/resolveAssetUrl";
import { AdaptiveVideoPlayer } from "../../components/AdaptiveVideoPlayer";
import "./ExperimentVideoTab.css";

interface ExperimentVideoTabProps {
  experiment: Experiment;
  alterationLocked?: boolean;
}

function formatDate(value?: string | null) {
  if (!value) return "—";
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

function formatUsd(value?: number | null) {
  if (value == null) return "—";
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
  }).format(value);
}

function formatInteger(value?: number | null) {
  return new Intl.NumberFormat("pt-BR").format(value ?? 0);
}

function buildExperimentTestUrl(url?: string | null) {
  const trimmedUrl = url?.trim();
  if (!trimmedUrl) {
    return null;
  }

  try {
    const parsedUrl = new URL(trimmedUrl);
    parsedUrl.searchParams.set("mh_test", "1");
    return parsedUrl.toString();
  } catch {
    const separator = trimmedUrl.includes("?") ? "&" : "?";
    return `${trimmedUrl}${separator}mh_test=1`;
  }
}

function getAspectRatioStyle(asset: ExperimentVideoAsset) {
  if (!asset.aspectRatio) {
    return undefined;
  }

  const normalizedRatio = asset.aspectRatio.replace(":", " / ");
  return { aspectRatio: normalizedRatio };
}

function resolveExperimentVideoPlaybackUrl(asset: ExperimentVideoAsset) {
  const hlsUrl = asset.hlsPlaybackUrl?.trim();
  if (hlsUrl) {
    return resolveAssetUrl(hlsUrl);
  }
  return asset.assetUrl ? resolveAssetUrl(asset.assetUrl) : "";
}

function isPdeHeroHlsReady(asset: ExperimentVideoAsset) {
  return asset.slot !== "LANDING_HERO" || Boolean(asset.hlsPlaybackUrl?.trim());
}

function getCommercialVideoRole(asset: ExperimentVideoAsset) {
  const duration = asset.durationSeconds ?? 0;
  const provider = `${asset.provider ?? ""} ${asset.model ?? ""}`.toUpperCase();
  if (provider.includes("MUSA_POST_PRODUCTION")) {
    return "Vídeo final";
  }
  const isLuma = provider.includes("LUMA") || provider.includes("RAY");

  if (asset.slot === "LANDING_HERO" && isLuma && duration >= 25) {
    return "Hero de venda";
  }
  if (asset.slot === "LANDING_HERO" && duration > 0 && duration < 25) {
    return "Cena curta";
  }
  if (asset.slot === "AD" || (duration >= 10 && duration <= 15)) {
    return "Hook de mídia";
  }
  if (asset.slot === "FORM_EXPLAINER") {
    return "Redução de dúvida";
  }
  if (asset.slot === "PRE_CHECKOUT") {
    return "Pré-checkout";
  }
  return "Apoio de funil";
}

function getCommercialVideoUse(asset: ExperimentVideoAsset) {
  const role = getCommercialVideoRole(asset);
  if (role === "Hero de venda") {
    return "Landing";
  }
  if (role === "Vídeo final") {
    return "Landing/Ads";
  }
  if (role === "Cena curta" || role === "Hook de mídia") {
    return "Ads/Reels";
  }
  if (role === "Redução de dúvida") {
    return "Formulário";
  }
  if (role === "Pré-checkout") {
    return "Checkout";
  }
  return "Teste";
}

export default function ExperimentVideoTab({
  experiment,
  alterationLocked = false,
}: ExperimentVideoTabProps) {
  const { data: videoAssets, isLoading } = useExperimentVideoAssets(
    experiment.id,
  );
  const geraSalesPagePublications = useGeraSalesPagePublications(experiment.id);
  const performanceDashboard = useExperimentVideoPerformanceDashboard(
    experiment.id,
  );
  const tenantContext = useTenantContext();
  const updateVideoReview = useUpdateExperimentVideoAssetReview();

  const sortedAssets = useMemo(() => videoAssets ?? [], [videoAssets]);
  const readyHeroAssets = useMemo(
    () =>
      sortedAssets.filter(
        (asset) =>
          asset.slot === "LANDING_HERO" &&
          asset.status === "READY" &&
          Boolean(resolveExperimentVideoPlaybackUrl(asset)) &&
          getCommercialVideoRole(asset) === "Hero de venda",
      ),
    [sortedAssets],
  );
  const approvedHeroAssets = useMemo(
    () => readyHeroAssets.filter((asset) => asset.reviewStatus === "APPROVED"),
    [readyHeroAssets],
  );
  const finishedSalesAssets = useMemo(
    () =>
      sortedAssets.filter(
        (asset) =>
          asset.provider === "MUSA_POST_PRODUCTION" &&
          asset.status === "READY" &&
          Boolean(asset.assetUrl),
      ),
    [sortedAssets],
  );
  const shortTrafficAssets = useMemo(
    () =>
      sortedAssets.filter((asset) =>
        ["Cena curta", "Hook de mídia"].includes(getCommercialVideoRole(asset)),
      ),
    [sortedAssets],
  );
  const landingHeroAsset = useMemo(
    () =>
      sortedAssets.find(
        (asset) =>
          asset.slot === "LANDING_HERO" &&
          asset.status === "READY" &&
          asset.reviewStatus === "APPROVED" &&
          Boolean(resolveExperimentVideoPlaybackUrl(asset)),
      ) ??
      sortedAssets.find(
        (asset) =>
          asset.slot === "LANDING_HERO" &&
          asset.status === "READY" &&
          Boolean(resolveExperimentVideoPlaybackUrl(asset)),
      ) ??
      sortedAssets.find(
        (asset) => asset.status === "READY" && Boolean(resolveExperimentVideoPlaybackUrl(asset)),
      ),
    [sortedAssets],
  );
  const landingHeroVideoUrl = landingHeroAsset
    ? resolveExperimentVideoPlaybackUrl(landingHeroAsset)
    : "";
  const landingHeroPosterUrl = landingHeroAsset?.thumbnailUrl
    ? resolveAssetUrl(landingHeroAsset.thumbnailUrl)
    : "";
  const latestSalesPagePublication =
    geraSalesPagePublications.data?.find((publication) =>
      Boolean(publication.salesPageUrl),
    ) ?? geraSalesPagePublications.data?.[0];
  const salesPagePreviewUrl = buildExperimentTestUrl(
    latestSalesPagePublication?.salesPageUrl,
  );
  const productVideoUrl = "/products/4/sales-videos";
  const performance = performanceDashboard.data;

  async function handleVideoReview(
    video: ExperimentVideoAsset,
    reviewStatus: ExperimentVideoReviewStatus,
    rejectionReason?: string,
  ) {
    await updateVideoReview.mutateAsync({
      experimentId: video.experimentId,
      videoAssetId: video.id,
      reviewStatus,
      rejectionReason,
      reviewedBy: tenantContext.userEmail,
    });
    toast.success(reviewStatus === "APPROVED" ? "Vídeo aprovado." : "Vídeo reprovado com motivo.");
  }

  return (
    <div className="d-flex flex-column gap-3">
      <div className="card experiment-video-performance-card">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap mb-3">
            <div>
              <h5 className="card-title mb-1">Painel consolidado de vídeo</h5>
              <p className="text-muted small mb-0">
                Cruza asset aprovado, criativo Meta e avanço comercial do
                funil.
              </p>
            </div>
            <span className="badge text-bg-light border">
              {performanceDashboard.isLoading
                ? "Carregando"
                : performance?.summary.lastMetricAt
                  ? `Sync ${formatDate(performance.summary.lastMetricAt)}`
                  : "Sem sync Meta"}
            </span>
          </div>

          {performanceDashboard.isError ? (
            <div className="alert alert-warning mb-0">
              Não foi possível carregar o painel consolidado de vídeo.
            </div>
          ) : (
            <>
              <div className="experiment-video-performance-card__metrics">
                <MetricTile
                  label="Vídeos aprovados"
                  value={formatInteger(performance?.summary.approvedAssets)}
                />
                <MetricTile
                  label="Criativos Meta vídeo"
                  value={formatInteger(performance?.summary.metaVideoCreatives)}
                />
                <MetricTile
                  label="Impressões"
                  value={formatInteger(performance?.summary.impressions)}
                />
                <MetricTile
                  label="Cliques"
                  value={formatInteger(performance?.summary.clicks)}
                />
                <MetricTile
                  label="Início diagnóstico"
                  value={formatInteger(performance?.summary.diagnosticStarts)}
                />
                <MetricTile
                  label="Checkout"
                  value={formatInteger(performance?.summary.checkoutAccesses)}
                />
                <MetricTile
                  label="Compra"
                  value={formatInteger(performance?.summary.purchases)}
                />
                <MetricTile
                  label="Gasto"
                  value={formatUsd(performance?.summary.spend)}
                />
              </div>

              <div className="alert alert-light border mt-3 mb-3">
                <div className="fw-semibold">Leitura recomendada</div>
                <div className="small text-muted">
                  {performance?.summary.recommendation ??
                    "Aguardando dados persistidos para orientar o próximo ajuste."}
                </div>
              </div>

              <div className="table-responsive">
                <table className="table table-sm align-middle mb-0">
                  <thead>
                    <tr>
                      <th>Asset</th>
                      <th>Slot</th>
                      <th>Revisão</th>
                      <th>Criativo Meta</th>
                      <th>Atribuição</th>
                      <th>Diagnóstico</th>
                      <th>Checkout</th>
                      <th>Compra</th>
                    </tr>
                  </thead>
                  <tbody>
                    {performanceDashboard.isLoading ? (
                      <tr>
                        <td colSpan={8} className="text-muted">
                          Carregando painel...
                        </td>
                      </tr>
                    ) : !performance || performance.assets.length === 0 ? (
                      <tr>
                        <td colSpan={8} className="text-muted">
                          Nenhum vídeo aprovado ou publicável para consolidar.
                        </td>
                      </tr>
                    ) : (
                      performance.assets.map((asset) => (
                        <tr key={asset.assetId}>
                          <td>
                            <div className="fw-semibold">#{asset.assetId}</div>
                            <div className="text-muted small">
                              {asset.provider || "Provider não registrado"}
                            </div>
                          </td>
                          <td>{asset.slot ?? "—"}</td>
                          <td>{asset.reviewStatus ?? "—"}</td>
                          <td>
                            {asset.metaCreatives.length > 0 ? (
                              asset.metaCreatives.map((creative) => (
                                <div
                                  key={`${creative.creativeId}-${creative.adId}`}
                                  className="experiment-video-performance-card__creative"
                                >
                                  <span>{creative.creativeKind}</span>
                                  <span>{creative.adName || creative.adId}</span>
                                  {creative.metaVideoId ? (
                                    <span>Vídeo Meta {creative.metaVideoId}</span>
                                  ) : null}
                                </div>
                              ))
                            ) : (
                              <span className="text-muted">
                                Sem vínculo direto
                              </span>
                            )}
                          </td>
                          <td>
                            {asset.attributionLevel === "AD"
                              ? "Anúncio"
                              : "Experimento"}
                          </td>
                          <td>{formatInteger(asset.diagnosticStarts)}</td>
                          <td>{formatInteger(asset.checkoutAccesses)}</td>
                          <td>{formatInteger(asset.purchases)}</td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </div>
      </div>

      <div className="card experiment-video-preview-card">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap mb-3">
            <div>
              <h5 className="card-title mb-1">Preview comercial do vídeo</h5>
              <p className="text-muted small mb-0">
                Revisão do vídeo principal e da página de venda publicada.
              </p>
            </div>
            {landingHeroAsset ? (
              <span className="badge text-bg-success">
                {landingHeroAsset.slot} · {landingHeroAsset.status}
              </span>
            ) : (
              <span className="badge text-bg-warning">Sem vídeo pronto</span>
            )}
          </div>

          <div className="row g-3">
            <div className="col-12 col-xl-5">
              <div className="experiment-video-preview-card__player-shell">
                {landingHeroVideoUrl ? (
                  <AdaptiveVideoPlayer
                    className="experiment-video-preview-card__player"
                    src={landingHeroVideoUrl}
                    fallbackSrc={
                      landingHeroAsset?.assetUrl
                        ? resolveAssetUrl(landingHeroAsset.assetUrl)
                        : undefined
                    }
                    poster={landingHeroPosterUrl || undefined}
                    controls
                    playsInline
                    preload="metadata"
                    style={
                      landingHeroAsset
                        ? getAspectRatioStyle(landingHeroAsset)
                        : undefined
                    }
                  />
                ) : (
                  <div className="experiment-video-preview-card__empty">
                    Vídeo ainda não disponível.
                  </div>
                )}
              </div>
              {landingHeroAsset && (
                <div className="experiment-video-preview-card__meta mt-3">
                  <span>{landingHeroAsset.reviewStatus}</span>
                  <span>{formatUsd(landingHeroAsset.cost)}</span>
                  <span>
                    {landingHeroAsset.durationSeconds
                      ? `${landingHeroAsset.durationSeconds}s`
                      : "Duração não registrada"}
                  </span>
                  {landingHeroVideoUrl && (
                    <a
                      href={landingHeroVideoUrl}
                      target="_blank"
                      rel="noreferrer"
                    >
                      Abrir
                    </a>
                  )}
                </div>
              )}
            </div>

            <div className="col-12 col-xl-7">
              <div className="experiment-video-preview-card__sales-page">
                <div className="experiment-video-preview-card__sales-page-header">
                  <div>
                    <div className="fw-semibold">Página de venda</div>
                    <div className="text-muted small">
                      {latestSalesPagePublication?.publishedAt
                        ? formatDate(latestSalesPagePublication.publishedAt)
                        : "Sem publicação registrada"}
                    </div>
                  </div>
                  {salesPagePreviewUrl && (
                    <a
                      href={salesPagePreviewUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="btn btn-sm btn-outline-primary"
                    >
                      Abrir página
                    </a>
                  )}
                </div>
                {salesPagePreviewUrl ? (
                  <iframe
                    className="experiment-video-preview-card__sales-page-frame"
                    title={`Página de venda do experimento ${experiment.id}`}
                    src={salesPagePreviewUrl}
                  />
                ) : (
                  <div className="experiment-video-preview-card__empty">
                    Página de venda ainda não publicada.
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap">
            <div>
              <h5 className="card-title mb-1">Vídeos do experimento</h5>
              <p className="text-muted small mb-0">
                Gestão operacional dos vídeos necessários para campanha e
                página.
              </p>
            </div>
            <div className="d-flex align-items-center gap-2 flex-wrap">
              <Link className="btn btn-sm btn-primary" to={productVideoUrl}>
                Gerenciar no produto
              </Link>
              <span className="badge text-bg-secondary">
                {sortedAssets.length} ativo(s)
              </span>
            </div>
          </div>
          <div className="table-responsive mt-3">
            <div className="alert alert-light border experiment-video-strategy-panel mb-3">
              <div>
                <div className="fw-semibold">Estratégia recomendada</div>
                <div className="small text-muted">
                  A criação e pós-produção de vídeos agora ficam na tela do
                  produto. Esta aba mantém apenas revisão do que o experimento
                  está usando ou herdou historicamente.
                </div>
              </div>
              <div className="experiment-video-strategy-panel__metrics">
                <span>Hero pronto: {readyHeroAssets.length}</span>
                <span>Hero aprovado: {approvedHeroAssets.length}</span>
                <span>Finalizados: {finishedSalesAssets.length}</span>
                <span>Cortes curtos: {shortTrafficAssets.length}</span>
                {alterationLocked ? <span>Alteração bloqueada</span> : null}
              </div>
            </div>
            <table className="table table-sm align-middle">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Slot</th>
                  <th>Papel no funil</th>
                  <th>Uso</th>
                  <th>Status</th>
                  <th>Revisão</th>
                  <th>Provider</th>
                  <th>Custo</th>
                  <th>Profile / Job</th>
                  <th>Obrigatório</th>
                  <th>Atualizado</th>
                  <th>Asset</th>
                  <th>Aprovação</th>
                </tr>
              </thead>
              <tbody>
                {isLoading ? (
                  <tr>
                    <td colSpan={13} className="text-muted">
                      Carregando vídeos...
                    </td>
                  </tr>
                ) : sortedAssets.length === 0 ? (
                  <tr>
                    <td colSpan={13} className="text-muted">
                      Nenhum vídeo registrado para este experimento.
                    </td>
                  </tr>
                ) : (
                  sortedAssets.map((asset) => (
                    <tr key={asset.id}>
                      <td>{asset.id}</td>
                      <td>{asset.slot}</td>
                      <td>{getCommercialVideoRole(asset)}</td>
                      <td>{getCommercialVideoUse(asset)}</td>
                      <td>
                        <span className="badge text-bg-info">
                          {asset.status}
                        </span>
                      </td>
                      <td>{asset.reviewStatus}</td>
                      <td>{asset.provider}</td>
                      <td>{formatUsd(asset.cost)}</td>
                      <td>
                        {asset.salesVideoProfileId ? (
                          <Link
                            to={`/sales-videos/profiles/${asset.salesVideoProfileId}`}
                          >
                            Profile #{asset.salesVideoProfileId}
                          </Link>
                        ) : (
                          "—"
                        )}
                        {asset.salesVideoJobId
                          ? ` · Job #${asset.salesVideoJobId}`
                          : ""}
                      </td>
                      <td>{asset.requiredForRelease ? "Sim" : "Não"}</td>
                      <td>{formatDate(asset.updatedAt)}</td>
                      <td>
                        {asset.assetUrl ? (
                          <a
                            href={resolveAssetUrl(asset.assetUrl)}
                            target="_blank"
                            rel="noreferrer"
                          >
                            Abrir
                          </a>
                        ) : (
                          "—"
                        )}
                      </td>
                      <td>
                        <ExperimentVideoReviewControls
                          asset={asset}
                          onReview={handleVideoReview}
                          pending={updateVideoReview.isPending}
                        />
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div className="alert alert-warning border mb-0">
        A geração de vídeos foi retirada do experimento. Para criar novos
        vídeos, gerar variações, finalizar peças para venda e acompanhar custo,
        use a central única em{" "}
        <Link to={productVideoUrl}>Vídeos do produto</Link>.
      </div>
    </div>
  );
}

function ExperimentVideoReviewControls({
  asset,
  onReview,
  pending,
}: {
  asset: ExperimentVideoAsset;
  onReview: (
    video: ExperimentVideoAsset,
    reviewStatus: ExperimentVideoReviewStatus,
    rejectionReason?: string,
  ) => Promise<void>;
  pending: boolean;
}) {
  const [rejectionReason, setRejectionReason] = useState(asset.rejectionReason ?? "");
  const canApprove =
    asset.status === "READY" &&
    Boolean(resolveExperimentVideoPlaybackUrl(asset)) &&
    isPdeHeroHlsReady(asset);
  const canReject = asset.status === "READY" && rejectionReason.trim().length > 0;

  useEffect(() => {
    setRejectionReason(asset.rejectionReason ?? "");
  }, [asset.id, asset.rejectionReason]);

  return (
    <div className="experiment-video-review">
      {asset.reviewStatus === "REJECTED" && asset.rejectionReason ? (
        <div className="experiment-video-review__reason">
          {asset.rejectionReason}
        </div>
      ) : null}
      <textarea
        value={rejectionReason}
        onChange={(event) => setRejectionReason(event.target.value)}
        rows={2}
        placeholder="Motivo da reprovação"
      />
      <div className="experiment-video-review__actions">
        <button
          className="btn btn-sm btn-success"
          type="button"
          disabled={!canApprove || pending}
          onClick={() => onReview(asset, "APPROVED")}
        >
          <CheckCircle2 size={14} aria-hidden="true" />
          Aprovar
        </button>
        <button
          className="btn btn-sm btn-outline-danger"
          type="button"
          disabled={!canReject || pending}
          onClick={() => onReview(asset, "REJECTED", rejectionReason.trim())}
        >
          <XCircle size={14} aria-hidden="true" />
          Reprovar
        </button>
      </div>
    </div>
  );
}

function MetricTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="experiment-video-performance-card__metric">
      <div>{label}</div>
      <strong>{value}</strong>
    </div>
  );
}
