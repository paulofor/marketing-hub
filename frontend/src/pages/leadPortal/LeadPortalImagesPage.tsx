import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  CheckCircle,
  Image as ImageIcon,
  Loader2,
  ShieldAlert,
  Sparkles,
} from "lucide-react";
import {
  useLeadPortalImagePackages,
  type FlowSubmissionImagePackageStatus,
  type LeadPortalImagePackage,
} from "../../api/leadPortal/useLeadPortalSubmissions";
import { getStatusDetail, statusDetails } from "./statusDetails";
import "./LeadPortalImagesPage.css";

type StatusFilter = FlowSubmissionImagePackageStatus | "ALL";

function formatDate(value: string) {
  return new Date(value).toLocaleString("pt-BR");
}

function buildLeadLabel(submission: LeadPortalImagePackage) {
  if (submission.name) return submission.name;
  if (submission.email) return submission.email;
  return submission.submissionId;
}

function buildStatusNarrative(status: FlowSubmissionImagePackageStatus) {
  return getStatusDetail(status);
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

function buildStats(submission: LeadPortalImagePackage) {
  const stats: { label: string; value: string }[] = [];
  if (submission.model) {
    stats.push({ label: "Modelo", value: submission.model });
  }
  if (typeof submission.plannedOutputs === "number") {
    stats.push({ label: "Solicitadas", value: String(submission.plannedOutputs) });
  }
  if (typeof submission.freeImages === "number" && submission.freeImages > 0) {
    stats.push({ label: "Grátis", value: String(submission.freeImages) });
  }
  stats.push({ label: "Geradas", value: String(submission.generatedImageCount) });
  stats.push({ label: "Prévias", value: String(submission.watermarkedImageCount) });
  const unitCost = formatUsd(
    submission.imageUnitPriceUsd,
    submission.imageCurrency ?? "USD",
  );
  if (unitCost) {
    stats.push({ label: "Custo unitário", value: unitCost });
  }
  if (submission.imageModelName) {
    const label = submission.imageModelQualityName
      ? `${submission.imageModelName} · ${submission.imageModelQualityName}`
      : submission.imageModelName;
    stats.push({ label: "Modelo", value: label });
  }
  const totalCost = formatUsd(submission.imageTotalPriceUsd, submission.imageCurrency ?? "USD");
  if (totalCost) {
    stats.push({ label: "Custo total", value: totalCost });
  }
  return stats;
}

function buildPipelineIcon(status: FlowSubmissionImagePackageStatus) {
  const detail = getStatusDetail(status);
  switch (detail.icon) {
    case "loader":
      return <Loader2 size={18} className="spin" />;
    case "check":
      return <CheckCircle size={18} />;
    case "alert":
      return <ShieldAlert size={18} />;
    default:
      return <Sparkles size={18} />;
  }
}

function buildStatusBadgeClass(status: FlowSubmissionImagePackageStatus) {
  return getStatusDetail(status).badgeClass;
}

export default function LeadPortalImagesPage() {
  const navigate = useNavigate();
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ALL");

  const statusesParam = statusFilter === "ALL" ? undefined : [statusFilter];
  const { data, isLoading, isError } = useLeadPortalImagePackages(statusesParam);

  const submissions = useMemo(() => {
    if (!data) return [] as LeadPortalImagePackage[];
    return [...data].sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
    );
  }, [data]);

  const totalDisplayed = submissions.length;

  return (
    <div className="lead-portal-images">
      <header className="lead-portal-images__header">
        <div className="lead-portal-images__intro">
          <p className="lead-portal-images__eyebrow">Lead Portal</p>
          <h1 className="lead-portal-images__title">Pacotes de imagem</h1>
          <p className="lead-portal-images__subtitle">
            Acompanhe todos os pacotes submetidos pelo portal, filtre por status e
            visualize rapidamente os detalhes antes de priorizar o envio ao
            pipeline de geração.
          </p>
        </div>
        <div className="lead-portal-images__actions">
          <div className="lead-portal-images__highlight" aria-live="polite">
            <div className="lead-portal-images__highlight-icon" aria-hidden="true">
              <Sparkles size={18} />
            </div>
            <div>
              <p className="lead-portal-images__highlight-label">Pacotes exibidos</p>
              <p className="lead-portal-images__highlight-value">{totalDisplayed}</p>
            </div>
          </div>
          <div className="lead-portal-images__filter">
            <label className="lead-portal-images__filter-label" htmlFor="lead-portal-status-filter">
              Status
            </label>
            <select
              id="lead-portal-status-filter"
              className="form-select form-select-sm"
              value={statusFilter}
              onChange={(event) =>
                setStatusFilter(event.target.value as StatusFilter)
              }
            >
              <option value="ALL">Todos os status</option>
              {Object.keys(statusDetails).map((statusKey) => {
                const status = statusKey as FlowSubmissionImagePackageStatus;
                return (
                  <option key={status} value={status}>
                    {getStatusDetail(status).label}
                  </option>
                );
              })}
            </select>
          </div>
        </div>
      </header>

      {isLoading ? (
        <div className="lead-portal-images__loading" role="status" aria-live="polite">
          <div className="spinner-border text-primary" />
          <p className="text-muted mt-2 mb-0">Carregando pacotes de imagens…</p>
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
          {submissions.map((submission) => {
            const detail = buildStatusNarrative(submission.status);
            const stats = buildStats(submission);

            return (
              <article
                key={submission.id}
                className="lead-portal-image-card"
                role="listitem"
                tabIndex={0}
                aria-label={`Pacote ${detail.label} do lead ${buildLeadLabel(submission)}`}
                onClick={() => navigate(`/lead-portal/images/${submission.id}`)}
                onKeyDown={(event) => {
                  if (event.key === "Enter" || event.key === " ") {
                    event.preventDefault();
                    navigate(`/lead-portal/images/${submission.id}`);
                  }
                }}
              >
                <div className="lead-portal-image-card__body">
                  <div className="lead-portal-image-card__status">
                    <span
                      className={`badge d-inline-flex align-items-center gap-1 ${buildStatusBadgeClass(submission.status)}`}
                    >
                      {buildPipelineIcon(submission.status)}
                      {detail.label}
                    </span>
                    <span className="text-muted small">
                      Atualizado {formatDate(submission.updatedAt)}
                    </span>
                  </div>

                  <div className="lead-portal-image-card__meta">
                    <div>
                      <p className="lead-portal-image-card__lead">{buildLeadLabel(submission)}</p>
                      <h2 className="lead-portal-image-card__title">
                        {submission.flowSlug
                          ? `Fluxo ${submission.flowSlug}`
                          : "Fluxo não informado"}
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

                  <div className="lead-portal-image-card__stats">
                    {stats.map((stat) => (
                      <span key={stat.label} className="lead-portal-image-card__stat">
                        <strong>{stat.label}:</strong> {stat.value}
                      </span>
                    ))}
                  </div>

                  <div className="lead-portal-image-card__pipeline">
                    <div className="lead-portal-image-card__pipeline-icon" aria-hidden="true">
                      {buildPipelineIcon(submission.status)}
                    </div>
                    <div>
                      <p className="lead-portal-image-card__pipeline-title">{detail.title}</p>
                      <p className="lead-portal-image-card__pipeline-text">
                        {detail.description}
                      </p>
                      <p className="lead-portal-image-card__pipeline-text text-muted mb-0">
                        Prompt base: {submission.prompt}
                      </p>
                      {submission.failureReason ? (
                        <p className="lead-portal-image-card__failure">
                          Falha: {submission.failureReason}
                        </p>
                      ) : null}
                    </div>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      )}

    </div>
  );
}
