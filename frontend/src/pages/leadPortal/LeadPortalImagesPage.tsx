import { useMemo } from "react";
import { Image as ImageIcon, ShieldAlert, Sparkles } from "lucide-react";
import { useLeadPortalImagePackages } from "../../api/leadPortal/useLeadPortalSubmissions";
import type { LeadPortalImagePackage } from "../../api/leadPortal/useLeadPortalSubmissions";
import "./LeadPortalImagesPage.css";

const statusLabels: Record<string, string> = {
  RECEIVED: "Pronto para pipeline",
  PROCESSED: "Processado",
  GENERATION_WITH_WATERMARK: "Gerando com marca d'água",
  GENERATION_NO_WATERMARK: "Gerando sem marca d'água",
  PURCHASED: "Comprado",
  FAILED: "Falha ao processar",
};

function formatDate(value: string) {
  return new Date(value).toLocaleString("pt-BR");
}

function buildLeadLabel(submission: LeadPortalImagePackage) {
  if (submission.name) return submission.name;
  if (submission.email) return submission.email;
  return submission.leadId;
}

export default function LeadPortalImagesPage() {
  const { data, isLoading, isError } = useLeadPortalImagePackages();

  const submissions = useMemo(() => {
    if (!data) return [] as LeadPortalImagePackage[];
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
          <h1 className="lead-portal-images__title">Pacotes de imagem</h1>
          <p className="lead-portal-images__subtitle">
            Acompanhe os pacotes que ainda precisam entrar no pipeline de
            criação de novas imagens e confirme rapidamente os dados recebidos.
          </p>
        </div>
        <div className="lead-portal-images__highlight">
          <div className="lead-portal-images__highlight-icon" aria-hidden="true">
            <Sparkles size={18} />
          </div>
          <div>
            <p className="lead-portal-images__highlight-label">Aguardando pipeline</p>
            <p className="lead-portal-images__highlight-value">{submissions.length}</p>
          </div>
        </div>
      </header>

      {isLoading ? (
        <div className="lead-portal-images__loading" role="status" aria-live="polite">
          <div className="spinner-border text-primary" />
          <p className="text-muted mt-2 mb-0">Carregando pacotes do portal…</p>
        </div>
      ) : isError ? (
        <div className="alert alert-danger d-flex align-items-center" role="alert">
          <ShieldAlert className="me-2" />
          <div>
            Não foi possível carregar os pacotes. Tente novamente ou verifique a
            conexão com o backend.
          </div>
        </div>
      ) : submissions.length === 0 ? (
        <div className="lead-portal-images__empty" role="status" aria-live="polite">
          <div className="lead-portal-images__empty-icon" aria-hidden="true">
            <ImageIcon size={28} />
          </div>
          <p className="lead-portal-images__empty-title">Nenhum pacote encontrado</p>
          <p className="lead-portal-images__empty-subtitle">
            Assim que o portal receber novos pacotes, eles aparecerão aqui com o
            status de processamento.
          </p>
        </div>
      ) : (
        <div className="lead-portal-images__list" role="list">
          {submissions.map((submission) => (
            <article
              key={submission.id}
              className="lead-portal-image-card"
              role="listitem"
              aria-label="Pacote aguardando pipeline"
            >
              <div className="lead-portal-image-card__body">
                <div className="lead-portal-image-card__status">
                  <span className="badge text-bg-primary d-inline-flex align-items-center gap-1" aria-label="Pronto para pipeline">
                    <Sparkles size={16} aria-hidden="true" />
                    {statusLabels[submission.status] ?? submission.status}
                  </span>
                  <span className="text-muted small">
                    Recebido {formatDate(submission.createdAt)}
                  </span>
                </div>

                <div className="lead-portal-image-card__meta">
                  <div>
                    <p className="lead-portal-image-card__lead">{buildLeadLabel(submission)}</p>
                    <h2 className="lead-portal-image-card__title">
                      {submission.flowSlug ? `Fluxo ${submission.flowSlug}` : "Fluxo não informado"}
                    </h2>
                  </div>
                  <div className="lead-portal-image-card__contacts" aria-label="Contatos do lead">
                    {submission.email ? (
                      <span className="lead-portal-image-card__contact" aria-label="Email do lead">
                        {submission.email}
                      </span>
                    ) : null}
                    {submission.phone ? (
                      <span className="lead-portal-image-card__contact" aria-label="Telefone do lead">
                        {submission.phone}
                      </span>
                    ) : null}
                  </div>
                </div>

                <div className="lead-portal-image-card__pipeline">
                  <div className="lead-portal-image-card__pipeline-icon" aria-hidden="true">
                    <ImageIcon size={18} />
                  </div>
                  <div>
                    <p className="lead-portal-image-card__pipeline-title">Aguardando criação de variantes</p>
                    <p className="lead-portal-image-card__pipeline-text">
                      Pacote pronto para entrar no pipeline de geração de novas imagens.
                      Confirme os dados acima antes de priorizar este lead na fila.
                    </p>
                    <p className="lead-portal-image-card__pipeline-text text-muted mb-0">
                      Prompt base: {submission.prompt}
                    </p>
                  </div>
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
