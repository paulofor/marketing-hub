import { useMemo } from "react";
import { Image as ImageIcon, ShieldAlert, Sparkles } from "lucide-react";
import { useLeadPortalSubmissions } from "../../api/leadPortal/useLeadPortalSubmissions";
import type { LeadPortalSubmission } from "../../api/leadPortal/useLeadPortalSubmissions";
import "./LeadPortalImagesPage.css";

function formatDate(value: string) {
  return new Date(value).toLocaleString("pt-BR");
}

export default function LeadPortalImagesPage() {
  const { data, isLoading, isError } = useLeadPortalSubmissions();

  const submissions = useMemo(() => {
    if (!data) return [] as LeadPortalSubmission[];
    return [...data].sort(
      (a, b) =>
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
            Acompanhe cada imagem enviada pelos leads e confirme rapidamente se
            o material foi recebido para processamento.
          </p>
        </div>
        <div className="lead-portal-images__highlight">
          <div className="lead-portal-images__highlight-icon" aria-hidden="true">
            <Sparkles size={18} />
          </div>
          <div>
            <p className="lead-portal-images__highlight-label">Envios com imagem</p>
            <p className="lead-portal-images__highlight-value">{submissions.length}</p>
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
      ) : submissions.length === 0 ? (
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
          {submissions.map((submission) => (
            <article key={submission.id} className="lead-portal-image-card">
              <div
                className="lead-portal-image-card__media"
                aria-hidden={!submission.imageUrl}
              >
                {submission.imageUrl ? (
                  <img
                    src={submission.imageUrl}
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
                  <span className="badge text-bg-primary" aria-label="Envio recebido">
                    Envio recebido
                  </span>
                  <span className="text-muted small">
                    Recebido {formatDate(submission.createdAt)}
                  </span>
                </div>

                <div className="lead-portal-image-card__meta">
                  <h2 className="lead-portal-image-card__title">
                    Fluxo {submission.flowSlug}
                  </h2>
                  <p className="lead-portal-image-card__lead">
                    {submission.name}
                    {submission.email ? ` · ${submission.email}` : ""}
                  </p>
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
