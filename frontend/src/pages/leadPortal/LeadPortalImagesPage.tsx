import { useMemo } from "react";
import {
  CheckCircle2,
  Image as ImageIcon,
  ShieldAlert,
  Sparkles,
} from "lucide-react";
import {
  useImageDeliverablePackages,
} from "../../api/imageDeliverable/useImageDeliverablePackages";
import type {
  ImageDeliverablePackage,
  ImageDeliverableStatus,
} from "../../api/imageDeliverable/types";
import "./LeadPortalImagesPage.css";

const STATUS_LABELS: Record<ImageDeliverableStatus, string> = {
  RECEIVED: "Recebidas",
  PROCESSED: "Processadas",
  GENERATION_WITH_WATERMARK: "Geração com marca d'água",
  PURCHASED: "Comprada",
  GENERATION_NO_WATERMARK: "Geração sem marca d'água",
  FAILED: "Falha",
};

const STATUS_VARIANTS: Record<ImageDeliverableStatus, string> = {
  RECEIVED: "text-bg-secondary",
  PROCESSED: "text-bg-primary",
  GENERATION_WITH_WATERMARK: "text-bg-info",
  PURCHASED: "text-bg-success",
  GENERATION_NO_WATERMARK: "text-bg-success",
  FAILED: "text-bg-danger",
};

function formatDate(value: string) {
  return new Date(value).toLocaleString("pt-BR");
}

function buildAccessLabel(pkg: ImageDeliverablePackage) {
  if (!pkg.items.length) {
    return "Nenhuma imagem gerada ainda";
  }
  const premium = pkg.items.filter((item) => item.accessType === "PREMIUM").length;
  const free = pkg.items.filter((item) => item.accessType === "FREE").length;
  const parts = [] as string[];
  if (premium) {
    parts.push(`${premium} premium`);
  }
  if (free) {
    parts.push(`${free} liberada(s)`);
  }
  return parts.join(" · ");
}

export default function LeadPortalImagesPage() {
  const { data, isLoading, isError } = useImageDeliverablePackages();

  const packages = useMemo(() => {
    if (!data) return [] as ImageDeliverablePackage[];
    return [...data].sort((a, b) =>
      new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
    );
  }, [data]);

  return (
    <div className="lead-portal-images">
      <header className="lead-portal-images__header">
        <div>
          <p className="lead-portal-images__eyebrow">Lead Portal</p>
          <h1 className="lead-portal-images__title">Envio de imagens</h1>
          <p className="lead-portal-images__subtitle">
            Acompanhe cada pacote recebido, o estágio de processamento e quais
            arquivos já estão disponíveis para entrega.
          </p>
        </div>
        <div className="lead-portal-images__highlight">
          <div className="lead-portal-images__highlight-icon" aria-hidden="true">
            <Sparkles size={18} />
          </div>
          <div>
            <p className="lead-portal-images__highlight-label">Pacotes ativos</p>
            <p className="lead-portal-images__highlight-value">{packages.length}</p>
          </div>
        </div>
      </header>

      {isLoading ? (
        <div className="lead-portal-images__loading" role="status" aria-live="polite">
          <div className="spinner-border text-primary" />
          <p className="text-muted mt-2 mb-0">Carregando imagens do portal…</p>
        </div>
      ) : isError ? (
        <div className="alert alert-danger d-flex align-items-center" role="alert">
          <ShieldAlert className="me-2" />
          <div>
            Não foi possível carregar as imagens. Tente novamente ou verifique a
            conexão com o backend.
          </div>
        </div>
      ) : packages.length === 0 ? (
        <div className="lead-portal-images__empty" role="status" aria-live="polite">
          <div className="lead-portal-images__empty-icon" aria-hidden="true">
            <ImageIcon size={28} />
          </div>
          <p className="lead-portal-images__empty-title">Nenhum envio encontrado</p>
          <p className="lead-portal-images__empty-subtitle">
            Assim que o portal receber novas imagens, elas aparecerão aqui com o
            status de processamento.
          </p>
        </div>
      ) : (
        <div className="lead-portal-images__grid">
          {packages.map((pkg) => (
            <article key={pkg.id} className="lead-portal-image-card">
              <div className="lead-portal-image-card__media" aria-hidden={!pkg.inputAssetUrl}>
                {pkg.inputAssetUrl ? (
                  <img
                    src={pkg.inputAssetUrl}
                    alt="Imagem enviada pelo lead"
                    loading="lazy"
                    className="lead-portal-image-card__image"
                  />
                ) : (
                  <div className="lead-portal-image-card__placeholder">
                    <ImageIcon aria-hidden="true" size={24} />
                    <span>Pré-visualização indisponível</span>
                  </div>
                )}
              </div>

              <div className="lead-portal-image-card__body">
                <div className="lead-portal-image-card__status">
                  <span
                    className={`badge ${STATUS_VARIANTS[pkg.status]}`}
                    aria-label={`Status: ${STATUS_LABELS[pkg.status]}`}
                  >
                    {STATUS_LABELS[pkg.status]}
                  </span>
                  <span className="text-muted small">
                    Atualizado {formatDate(pkg.updatedAt)}
                  </span>
                </div>

                <div className="lead-portal-image-card__meta">
                  <h2 className="lead-portal-image-card__title">
                    Pacote #{pkg.id}
                  </h2>
                  <p className="lead-portal-image-card__lead">Lead {pkg.leadId}</p>
                </div>

                <div className="lead-portal-image-card__chips">
                  <span className="chip">
                    Entrada planejada: {pkg.plannedOutputs ?? "—"}
                  </span>
                  <span className="chip chip-emphasis">
                    Grátis liberadas: {pkg.freeImages}
                  </span>
                  {pkg.model ? <span className="chip chip-muted">{pkg.model}</span> : null}
                </div>

                <div className="lead-portal-image-card__items">
                  <div className="lead-portal-image-card__items-header">
                    <p className="lead-portal-image-card__items-title">
                      <CheckCircle2 size={16} className="me-2" aria-hidden="true" />
                      {buildAccessLabel(pkg)}
                    </p>
                    <span className="badge text-bg-light text-dark">
                      {pkg.items.length} imagem(ns)
                    </span>
                  </div>
                  {pkg.items.length > 0 ? (
                    <div className="lead-portal-image-card__items-grid">
                      {pkg.items.map((item) => (
                        <div key={item.id} className="lead-portal-image-card__item">
                          {item.assetUrl ? (
                            <img
                              src={item.assetUrl}
                              alt="Variação gerada"
                              loading="lazy"
                              className="lead-portal-image-card__item-image"
                            />
                          ) : (
                            <div className="lead-portal-image-card__item-placeholder" aria-hidden="true">
                              <ImageIcon size={16} />
                            </div>
                          )}
                          <div className="lead-portal-image-card__item-meta">
                            <span className="lead-portal-image-card__item-label">
                              Variação {item.position + 1}
                            </span>
                            <span className="badge text-bg-secondary">
                              {item.accessType === "FREE" ? "Liberada" : "Premium"}
                            </span>
                            <span className="text-muted small">
                              {formatDate(item.createdAt)}
                            </span>
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-muted small mb-0">
                      Nenhuma variação gerada para este pacote ainda.
                    </p>
                  )}
                </div>

                <details className="lead-portal-image-card__prompt">
                  <summary>Prompt usado na geração</summary>
                  <pre>{pkg.prompt}</pre>
                </details>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
