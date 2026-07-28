import { Link, useParams } from "react-router-dom";
import {
  ArrowLeft,
  BadgeCheck,
  Image as ImageIcon,
  Megaphone,
  PlaySquare,
} from "lucide-react";
import { useProductAdLibrary } from "../../api/product/useProductAdLibrary";
import PageTitle from "../../components/PageTitle";

function label(value?: string | null) {
  return value ? value.replace(/_/g, " ") : "Sem status";
}

function formatKind(value?: string | null) {
  if (!value) return "Imagem";
  return value.replace(/_/g, " ");
}

function isVideoAd(value?: string | null) {
  return value?.toUpperCase() === "VIDEO";
}

function formatReviewDate(value?: string | null) {
  if (!value) return "Sem revisão";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Sem revisão";
  return date.toLocaleString("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  });
}

export default function ProductAdsPage() {
  const { productId } = useParams();
  const libraryQuery = useProductAdLibrary(productId);
  const library = libraryQuery.data;

  if (libraryQuery.isLoading) {
    return <p className="text-muted">Carregando anúncios do produto...</p>;
  }

  if (libraryQuery.isError || !library) {
    return (
      <div>
        <Link className="btn btn-outline-secondary mb-3" to="/products">
          <ArrowLeft size={16} aria-hidden="true" />
          Voltar para produtos
        </Link>
        <div className="alert alert-danger">
          Não foi possível carregar os anúncios do produto.
        </div>
      </div>
    );
  }

  const readyAds = library.ads.filter(
    (ad) => ad.status?.toUpperCase() === "READY",
  ).length;
  const videoAds = library.ads.filter((ad) => isVideoAd(ad.format)).length;

  return (
    <div>
      <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-4">
        <div>
          <PageTitle>Anúncios</PageTitle>
          <p className="text-muted mb-0">
            {library.productName ||
              library.productSlug ||
              `Produto ${library.productId}`}{" "}
            · {label(library.commercialStatus)}
          </p>
        </div>
        <Link className="btn btn-outline-secondary" to="/products">
          <ArrowLeft size={16} aria-hidden="true" />
          Voltar para produtos
        </Link>
      </div>

      <section className="product-comparison-decision mb-3">
        <div>
          <span>Uso recomendado</span>
          <strong>{library.mainRecommendation}</strong>
        </div>
      </section>

      <div className="product-comparison-summary mb-3">
        <section>
          <Megaphone size={18} aria-hidden="true" />
          <span>Anúncios</span>
          <strong>{library.ads.length}</strong>
        </section>
        <section>
          <BadgeCheck size={18} aria-hidden="true" />
          <span>Prontos</span>
          <strong>{readyAds}</strong>
        </section>
        <section>
          <PlaySquare size={18} aria-hidden="true" />
          <span>Vídeo</span>
          <strong>{videoAds}</strong>
        </section>
      </div>

      {library.ads.length === 0 ? (
        <div className="alert alert-warning">
          Nenhum anúncio gerado para este produto ainda.
        </div>
      ) : (
        <div className="row g-3">
          {library.ads.map((ad) => {
            const MediaIcon = isVideoAd(ad.format) ? PlaySquare : ImageIcon;
            return (
              <div className="col-12 col-xl-6" key={ad.creativeId}>
                <section className="card h-100">
                  <div className="card-body">
                    <div className="d-flex flex-wrap align-items-start justify-content-between gap-2 mb-3">
                      <div>
                        <span className="badge text-bg-light border">
                          {label(ad.status)}
                        </span>
                        <h2 className="h5 mt-2 mb-1">
                          {ad.headline || `Anúncio ${ad.creativeId}`}
                        </h2>
                        <small className="text-muted">
                          Experimento #{ad.experimentId} ·{" "}
                          {ad.experimentName || "Sem nome"}
                        </small>
                      </div>
                      <span className="badge text-bg-secondary">
                        <MediaIcon size={14} aria-hidden="true" />{" "}
                        {formatKind(ad.format)}
                      </span>
                    </div>

                    {ad.primaryText && (
                      <p className="mb-2">{ad.primaryText}</p>
                    )}
                    {ad.description && (
                      <p className="small text-muted mb-2">{ad.description}</p>
                    )}
                    <dl className="product-catalog-card__facts mb-3">
                      <div>
                        <dt>CTA</dt>
                        <dd>{ad.cta || "Não definido"}</dd>
                      </div>
                      <div>
                        <dt>Destino</dt>
                        <dd>
                          {ad.destinationUrl ? (
                            <a
                              href={ad.destinationUrl}
                              target="_blank"
                              rel="noreferrer"
                            >
                              {ad.destinationUrl}
                            </a>
                          ) : (
                            "Não informado"
                          )}
                        </dd>
                      </div>
                      <div>
                        <dt>Revisão</dt>
                        <dd>{formatReviewDate(ad.reviewedAt)}</dd>
                      </div>
                    </dl>

                    {(ad.imageUrl || ad.videoUrl) && (
                      <div className="d-flex flex-wrap gap-2 mb-3">
                        {ad.imageUrl && (
                          <a
                            className="btn btn-sm btn-outline-secondary"
                            href={ad.imageUrl}
                            target="_blank"
                            rel="noreferrer"
                          >
                            Abrir imagem
                          </a>
                        )}
                        {ad.videoUrl && (
                          <a
                            className="btn btn-sm btn-outline-secondary"
                            href={ad.videoUrl}
                            target="_blank"
                            rel="noreferrer"
                          >
                            Abrir vídeo
                          </a>
                        )}
                      </div>
                    )}

                    <p className="small fw-semibold mb-0">
                      {ad.reuseRecommendation}
                    </p>
                  </div>
                </section>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
