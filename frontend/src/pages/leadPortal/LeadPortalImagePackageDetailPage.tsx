import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  ChevronLeft,
  ChevronRight,
  Download,
  Image as ImageIcon,
  Loader2,
  ShieldAlert,
} from "lucide-react";
import {
  useLeadPortalImagePackageDetail,
  type LeadPortalImageReference,
} from "../../api/leadPortal/useLeadPortalImagePackageDetail";
import { getStatusDetail } from "./statusDetails";
import type { FlowSubmissionImagePackageStatus } from "../../api/leadPortal/useLeadPortalSubmissions";
import "./LeadPortalImagesPage.css";

interface GalleryItem {
  key: string;
  label: string;
  type: string;
  accessType?: string | null;
  url?: string | null;
  downloadUrl?: string | null;
  prompt?: string | null;
  model?: string | null;
  createdAt?: string | null;
  variant?: "ORIGINAL" | "WATERMARK";
}

function formatDateTime(value?: string | null) {
  if (!value) return "--";
  return new Date(value).toLocaleString("pt-BR");
}

function buildGalleryLabel(image: LeadPortalImageReference, index: number) {
  if (image.type === "ORIGINAL") {
    return "Imagem enviada";
  }
  if (image.accessType === "FREE") {
    return `Gerada #${index} (Livre)`;
  }
  return `Gerada #${index}`;
}

function formatUsd(value?: number | null, currency = "USD") {
  if (typeof value !== "number") {
    return null;
  }
  try {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency,
      minimumFractionDigits: 3,
    }).format(value);
  } catch {
    return `$${value.toFixed(3)}`;
  }
}

function buildGalleryItems(
  status: FlowSubmissionImagePackageStatus,
  original?: LeadPortalImageReference | null,
  generated: LeadPortalImageReference[] = [],
) {
  const items: GalleryItem[] = [];
  if (original?.url) {
    items.push({
      key: "original",
      label: buildGalleryLabel(original, 1),
      type: original.type ?? "ORIGINAL",
      accessType: original.accessType,
      url: original.url,
      downloadUrl: original.downloadUrl ?? original.url,
      prompt: original.prompt,
      model: original.model,
      createdAt: original.createdAt,
    });
  }

  generated
    .filter((image) => Boolean(image.url) || Boolean(image.watermark?.url))
    .forEach((image, index) => {
      const position =
        typeof image.position === "number" ? image.position + 1 : index + 1;
      const baseLabel = buildGalleryLabel(image, position);
      if (image.url) {
        items.push({
          key: `generated-${image.itemId ?? position}-original`,
          label: baseLabel,
          type: image.type ?? "GENERATED",
          accessType: image.accessType,
          url: image.url,
          downloadUrl: image.downloadUrl ?? image.url,
          prompt: image.prompt,
          model: image.model,
          createdAt: image.createdAt,
          variant: "ORIGINAL",
        });
      }
      const watermark = image.watermark;
      if (watermark?.url) {
        items.push({
          key: `generated-${image.itemId ?? position}-watermark`,
          label: `${baseLabel} (Marca d'água)`,
          type: "GENERATED_WATERMARK",
          accessType: image.accessType,
          url: watermark.url,
          downloadUrl: watermark.downloadUrl ?? watermark.url,
          prompt: image.prompt,
          model: image.model,
          createdAt: watermark.createdAt ?? image.createdAt,
          variant: "WATERMARK",
        });
      }
    });

  if (items.length === 0) {
    const detail = getStatusDetail(status);
    items.push({
      key: "placeholder",
      label: detail.label,
      type: "PLACEHOLDER",
      url: undefined,
      downloadUrl: undefined,
      prompt: undefined,
      model: undefined,
    });
  }

  return items;
}

export default function LeadPortalImagePackageDetailPage() {
  const { packageId: packageIdParam } = useParams<{ packageId: string }>();
  const navigate = useNavigate();

  const packageIdNumber = packageIdParam ? Number(packageIdParam) : NaN;
  const hasValidId = Number.isFinite(packageIdNumber) && packageIdNumber > 0;
  const requestId = hasValidId ? packageIdNumber : null;

  const [currentIndex, setCurrentIndex] = useState(0);
  const { data, isLoading, isError } = useLeadPortalImagePackageDetail(requestId);

  useEffect(() => {
    setCurrentIndex(0);
  }, [requestId]);

  const galleryItems = useMemo(() => {
    if (!data) return [];
    return buildGalleryItems(data.status, data.originalImage, data.generatedImages);
  }, [data]);

  const currentItem = galleryItems[currentIndex];
  const hasPrev = currentIndex > 0;
  const hasNext = currentIndex < galleryItems.length - 1;
  const statusDetail = data ? getStatusDetail(data.status) : null;

  const handleBackClick = () => {
    navigate("/lead-portal/images");
  };

  if (!hasValidId) {
    return (
      <div className="lead-portal-image-detail-page">
        <button
          type="button"
          className="btn btn-link lead-portal-image-detail-page__back"
          onClick={handleBackClick}
        >
          <ChevronLeft size={16} />
          <span>Voltar para pacotes</span>
        </button>
        <div className="alert alert-warning mt-3" role="alert">
          Identificador de pacote inválido.
        </div>
      </div>
    );
  }

  return (
    <div className="lead-portal-image-detail-page">
      <button
        type="button"
        className="btn btn-link lead-portal-image-detail-page__back"
        onClick={handleBackClick}
      >
        <ChevronLeft size={16} />
        <span>Voltar para pacotes</span>
      </button>

      <div className="lead-portal-image-detail-page__card">
        <header className="lead-portal-image-detail-page__header">
          <div>
            <p className="lead-portal-images__eyebrow">Lead Portal</p>
            <h1 className="lead-portal-images__title">Pacote #{packageIdNumber}</h1>
            {statusDetail ? (
              <div className="lead-portal-image-detail__status">
                <span className={`badge ${statusDetail.badgeClass}`}>
                  {statusDetail.label}
                </span>
                <span className="text-muted small">
                  Atualizado {formatDateTime(data?.updatedAt)}
                </span>
              </div>
            ) : null}
          </div>
        </header>

        {isLoading ? (
          <div
            className="lead-portal-image-detail__loading"
            role="status"
            aria-live="polite"
          >
            <Loader2 className="spin" size={24} />
            <p className="text-muted mb-0 mt-2">
              Carregando detalhes do pacote…
            </p>
          </div>
        ) : isError || !data ? (
          <div
            className="alert alert-danger d-flex align-items-center"
            role="alert"
          >
            <ShieldAlert className="me-2" />
            <div>
              Não foi possível carregar os detalhes do pacote. Tente novamente mais
              tarde.
            </div>
          </div>
        ) : (
          <div className="lead-portal-image-detail__layout">
            {data.status === "WATERMARK_PENDING" ? (
              <div className="alert alert-info" role="status">
                As variações estão prontas e aguardando a geração das prévias com marca d'água.
              </div>
            ) : null}
            {data.status === "WATERMARKING" ? (
              <div className="alert alert-warning" role="status">
                O serviço de marca d'água está processando as imagens. Esta tela atualiza automaticamente ao concluir.
              </div>
            ) : null}
            {data.status === "COMPLETED" && data.watermarkedImageCount < data.generatedImages.length ? (
              <div className="alert alert-secondary" role="status">
                Algumas imagens originais não possuem prévias com marca d'água. Reprocessar o pacote pode ser necessário.
              </div>
            ) : null}
            <section className="lead-portal-image-detail__info">
              <div className="lead-portal-image-detail__section">
                <h3>Resumo</h3>
                <dl className="lead-portal-image-detail__description-list">
                  <div>
                    <dt>Criado em</dt>
                    <dd>{formatDateTime(data.createdAt)}</dd>
                  </div>
                  <div>
                    <dt>Prompt</dt>
                    <dd className="lead-portal-image-detail__prompt">{data.prompt}</dd>
                  </div>
                  {data.imageModelName ? (
                    <div>
                      <dt>Modelo selecionado</dt>
                      <dd>
                        {data.imageModelName}
                        {data.imageModelQualityName ? ` · ${data.imageModelQualityName}` : ""}
                      </dd>
                    </div>
                  ) : null}
                  {data.model ? (
                    <div>
                      <dt>Modelo</dt>
                      <dd>{data.model}</dd>
                    </div>
                  ) : null}
                  {typeof data.plannedOutputs === "number" ? (
                    <div>
                      <dt>Variações solicitadas</dt>
                      <dd>{data.plannedOutputs}</dd>
                    </div>
                  ) : null}
                  {typeof data.freeImages === "number" && data.freeImages > 0 ? (
                    <div>
                      <dt>Variações gratuitas</dt>
                      <dd>{data.freeImages}</dd>
                    </div>
                  ) : null}
                  <div>
                    <dt>Imagens geradas</dt>
                    <dd>{data.generatedImages.length}</dd>
                  </div>
                  <div>
                    <dt>Prévias com marca d'água</dt>
                    <dd>{data.watermarkedImageCount}</dd>
                  </div>
                  {typeof data.imageTotalPriceUsd === "number" ? (
                    <div>
                      <dt>Custo estimado</dt>
                      <dd>{formatUsd(data.imageTotalPriceUsd, data.imageCurrency ?? undefined) ?? "--"}</dd>
                    </div>
                  ) : null}
                </dl>
              </div>

              <div className="lead-portal-image-detail__section">
                <h3>Lead</h3>
                <dl className="lead-portal-image-detail__description-list">
                  <div>
                    <dt>Nome</dt>
                    <dd>{data.submission.name ?? "Não informado"}</dd>
                  </div>
                  <div>
                    <dt>Email</dt>
                    <dd>{data.submission.email ?? "Não informado"}</dd>
                  </div>
                  <div>
                    <dt>Telefone</dt>
                    <dd>{data.submission.phone ?? "Não informado"}</dd>
                  </div>
                  <div>
                    <dt>Fluxo</dt>
                    <dd>{data.submission.flowSlug ?? "Não informado"}</dd>
                  </div>
                </dl>
              </div>

              {data.failureReason ? (
                <div className="lead-portal-image-detail__section lead-portal-image-detail__section--danger">
                  <h3 className="text-danger">Motivo da falha</h3>
                  <p className="mb-0">{data.failureReason}</p>
                </div>
              ) : null}
            </section>

            <section
              className="lead-portal-image-detail__gallery"
              aria-label="Galeria de imagens do pacote"
            >
              <div className="lead-portal-image-detail__viewer">
                {hasPrev ? (
                  <button
                    type="button"
                    className="btn btn-outline-secondary btn-sm lead-portal-image-detail__nav lead-portal-image-detail__nav--prev"
                    onClick={() => setCurrentIndex((index) => Math.max(0, index - 1))}
                  >
                    <ChevronLeft size={18} />
                    <span className="visually-hidden">Imagem anterior</span>
                  </button>
                ) : null}
                {hasNext ? (
                  <button
                    type="button"
                    className="btn btn-outline-secondary btn-sm lead-portal-image-detail__nav lead-portal-image-detail__nav--next"
                    onClick={() =>
                      setCurrentIndex((index) =>
                        Math.min(galleryItems.length - 1, index + 1),
                      )
                    }
                  >
                    <ChevronRight size={18} />
                    <span className="visually-hidden">Próxima imagem</span>
                  </button>
                ) : null}

                {currentItem?.url ? (
                  <img src={currentItem.url} alt={currentItem.label} loading="lazy" />
                ) : (
                  <div className="lead-portal-image-detail__placeholder">
                    <ImageIcon size={48} aria-hidden="true" />
                    <p className="text-muted mb-0">
                      Nenhuma imagem disponível para este pacote.
                    </p>
                  </div>
                )}
              </div>

              {currentItem ? (
                <div className="lead-portal-image-detail__viewer-meta">
                  <div className="lead-portal-image-detail__viewer-meta-header">
                    <span className="badge text-bg-light">
                      {currentItem.variant === "WATERMARK"
                        ? "Marca d'água"
                        : currentItem.accessType ?? currentItem.type}
                    </span>
                    {currentItem.variant === "WATERMARK" ? (
                      <span className="badge text-bg-info ms-2">
                        {currentItem.accessType ?? "Protegida"}
                      </span>
                    ) : null}
                    {currentItem.createdAt ? (
                      <span className="text-muted small">
                        {formatDateTime(currentItem.createdAt)}
                      </span>
                    ) : null}
                  </div>
                  {currentItem.prompt ? (
                    <p className="lead-portal-image-detail__viewer-text">
                      <strong>Prompt:</strong> {currentItem.prompt}
                    </p>
                  ) : null}
                  {currentItem.model ? (
                    <p className="lead-portal-image-detail__viewer-text">
                      <strong>Modelo:</strong> {currentItem.model}
                    </p>
                  ) : null}
                  {currentItem.variant === "WATERMARK" ? (
                    <p className="lead-portal-image-detail__viewer-text text-muted">
                      Versão demonstrativa protegida com marca d'água automática.
                    </p>
                  ) : null}
                  {currentItem.downloadUrl ? (
                    <div className="lead-portal-image-detail__viewer-actions">
                      <a
                        href={currentItem.downloadUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="btn btn-sm btn-outline-secondary"
                      >
                        <Download size={16} className="me-1" /> Abrir imagem
                      </a>
                    </div>
                  ) : null}
                </div>
              ) : null}

              <div className="lead-portal-image-detail__thumbnails">
                {galleryItems.map((item, index) => (
                  <button
                    key={item.key}
                    type="button"
                    className={`lead-portal-image-detail__thumbnail ${
                      index === currentIndex
                        ? "lead-portal-image-detail__thumbnail--active"
                        : ""
                    }`}
                    onClick={() => setCurrentIndex(index)}
                  >
                    <div className="lead-portal-image-detail__thumbnail-image">
                      {item.url ? (
                        <img src={item.url} alt={item.label} loading="lazy" />
                      ) : (
                        <ImageIcon aria-hidden="true" size={20} />
                      )}
                    </div>
                    <div className="lead-portal-image-detail__thumbnail-text">
                      <strong>{item.label}</strong>
                      <span>{item.accessType ?? item.type}</span>
                    </div>
                  </button>
                ))}
              </div>
            </section>
          </div>
        )}
      </div>
    </div>
  );
}
