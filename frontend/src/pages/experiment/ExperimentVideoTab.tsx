import { useMemo } from "react";
import { Link } from "react-router-dom";
import { useGeraSalesPagePublications } from "../../api/experiment/useGeraSalesPagePublications";
import type { Experiment } from "../../api/experiment/useExperiments";
import {
  ExperimentVideoAsset,
  useExperimentVideoAssets,
} from "../../api/experiment/useExperimentVideoAssets";
import { resolveAssetUrl } from "../../utils/resolveAssetUrl";
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

  const sortedAssets = useMemo(() => videoAssets ?? [], [videoAssets]);
  const readyHeroAssets = useMemo(
    () =>
      sortedAssets.filter(
        (asset) =>
          asset.slot === "LANDING_HERO" &&
          asset.status === "READY" &&
          Boolean(asset.assetUrl) &&
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
          Boolean(asset.assetUrl),
      ) ??
      sortedAssets.find(
        (asset) =>
          asset.slot === "LANDING_HERO" &&
          asset.status === "READY" &&
          Boolean(asset.assetUrl),
      ) ??
      sortedAssets.find(
        (asset) => asset.status === "READY" && Boolean(asset.assetUrl),
      ),
    [sortedAssets],
  );
  const landingHeroVideoUrl = landingHeroAsset?.assetUrl
    ? resolveAssetUrl(landingHeroAsset.assetUrl)
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

  return (
    <div className="d-flex flex-column gap-3">
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
                  <video
                    className="experiment-video-preview-card__player"
                    src={landingHeroVideoUrl}
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
                  {landingHeroAsset.assetUrl && (
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
                </tr>
              </thead>
              <tbody>
                {isLoading ? (
                  <tr>
                    <td colSpan={12} className="text-muted">
                      Carregando vídeos...
                    </td>
                  </tr>
                ) : sortedAssets.length === 0 ? (
                  <tr>
                    <td colSpan={12} className="text-muted">
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
