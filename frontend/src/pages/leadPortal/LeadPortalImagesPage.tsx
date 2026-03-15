import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  CheckCircle,
  DollarSign,
  Image as ImageIcon,
  Loader2,
  ShieldAlert,
  Sparkles,
} from "lucide-react";
import {
  useLeadPortalImagePackages,
  type FlowSubmissionImagePackageStatus,
  type LeadPortalImagePackage,
  type LeadPortalImagePackageLifecycleStatus,
} from "../../api/leadPortal/useLeadPortalSubmissions";
import { getStatusDetail } from "./statusDetails";
import {
  estimateImagePackageTotalPriceUsd,
  estimateImagePackageUnitPriceUsd,
} from "../../utils/imagePricing";
import "./LeadPortalImagesPage.css";

type StatusFilter = FlowSubmissionImagePackageStatus | "ALL";

const PAGE_SIZE = 15;

const filterableStatuses: FlowSubmissionImagePackageStatus[] = [
  "RECEIVED",
  "RECENT",
  "PROCESSING",
  "WATERMARK_PENDING",
  "WATERMARKING",
  "COMPLETED",
  "FAILED",
];

function formatDate(value: string) {
  return new Date(value).toLocaleString("pt-BR");
}

function buildLeadLabel(submission: LeadPortalImagePackage) {
  if (submission.name) return submission.name;
  if (submission.email) return submission.email;
  return submission.submissionId;
}

function buildStatusNarrative(status: LeadPortalImagePackageLifecycleStatus) {
  return getStatusDetail(status);
}

function formatUsd(
  value?: number | null,
  currency = "USD",
  options?: { minimumFractionDigits?: number; maximumFractionDigits?: number },
) {
  if (typeof value !== "number") {
    return null;
  }

  const minimumFractionDigits = options?.minimumFractionDigits ?? 3;
  const maximumFractionDigits =
    options?.maximumFractionDigits && options.maximumFractionDigits >= minimumFractionDigits
      ? options.maximumFractionDigits
      : Math.max(minimumFractionDigits, options?.maximumFractionDigits ?? 4);

  try {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency,
      minimumFractionDigits,
      maximumFractionDigits,
    }).format(value);
  } catch {
    const digits = Math.max(
      minimumFractionDigits,
      Math.min(maximumFractionDigits, 6),
    );
    return `$${value.toFixed(digits)}`;
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

  const estimatedUnitPriceUsd = estimateImagePackageUnitPriceUsd(submission);
  const unitCost = formatUsd(
    estimatedUnitPriceUsd,
    submission.imageCurrency ?? "USD",
  );
  if (unitCost) {
    stats.push({ label: "Custo unitário", value: unitCost });
  }

  if (submission.imageModelName) {
    const label = submission.imageModelQualityName
      ? `${submission.imageModelName} · ${submission.imageModelQualityName}`
      : submission.imageModelName;
    stats.push({ label: "Modelo selecionado", value: label });
  }

  const estimatedTotalCostUsd = estimateImagePackageTotalPriceUsd(submission);
  const totalCost = formatUsd(
    estimatedTotalCostUsd,
    submission.imageCurrency ?? "USD",
    { minimumFractionDigits: 2, maximumFractionDigits: 2 },
  );
  if (totalCost) {
    stats.push({ label: "Custo total", value: totalCost });
  }
  return stats;
}

function buildPipelineIcon(status: LeadPortalImagePackageLifecycleStatus) {
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

function buildStatusBadgeClass(status: LeadPortalImagePackageLifecycleStatus) {
  return getStatusDetail(status).badgeClass;
}

export default function LeadPortalImagesPage() {
  const navigate = useNavigate();
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ALL");
  const [page, setPage] = useState(0);

  const statusesParam = statusFilter === "ALL" ? undefined : [statusFilter];
  const { data, isLoading, isError, isFetching } =
    useLeadPortalImagePackages(statusesParam);

  const submissions = useMemo(() => {
    if (!data) return [] as LeadPortalImagePackage[];
    return [...data].sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
    );
  }, [data]);

  const totalPages = Math.ceil(submissions.length / PAGE_SIZE);

  useEffect(() => {
    if (page > 0) {
      if (totalPages === 0) {
        setPage(0);
      } else if (page >= totalPages) {
        setPage(totalPages - 1);
      }
    }
  }, [page, totalPages]);

  const paginatedSubmissions = useMemo(() => {
    const start = page * PAGE_SIZE;
    return submissions.slice(start, start + PAGE_SIZE);
  }, [page, submissions]);

  const totalDisplayed = paginatedSubmissions.length;
  const currentPage = totalPages === 0 ? 0 : page + 1;
  const canGoPrevious = page > 0;
  const canGoNext = page + 1 < totalPages;

  const totalCostSnapshot = useMemo(() => {
    return submissions.reduce<{
      total: number;
      count: number;
    }>((acc, submission) => {
      const total = estimateImagePackageTotalPriceUsd(submission);
      if (typeof total === "number" && Number.isFinite(total)) {
        acc.total += total;
        acc.count += 1;
      }
      return acc;
    }, {
      total: 0,
      count: 0,
    });
  }, [submissions]);

  const aggregatedCurrency =
    submissions.find((submission) => submission.imageCurrency && submission.imageCurrency.trim().length > 0)?.imageCurrency?.toUpperCase() ?? "USD";

  const formattedTotalCost =
    totalCostSnapshot.count > 0
      ? formatUsd(totalCostSnapshot.total, aggregatedCurrency, {
          minimumFractionDigits: 2,
          maximumFractionDigits: 2,
        }) ?? "--"
      : "--";

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
          <div className="lead-portal-images__metrics" aria-live="polite">
            <div className="lead-portal-images__highlight">
              <div className="lead-portal-images__highlight-icon" aria-hidden="true">
                <Sparkles size={18} />
              </div>
              <div>
                <p className="lead-portal-images__highlight-label">Pacotes exibidos</p>
                <p className="lead-portal-images__highlight-value">{totalDisplayed}</p>
              </div>
            </div>
            <div className="lead-portal-images__highlight">
              <div className="lead-portal-images__highlight-icon" aria-hidden="true">
                <DollarSign size={18} />
              </div>
              <div>
                <p className="lead-portal-images__highlight-label">Valor total gasto</p>
                <p className="lead-portal-images__highlight-value">{formattedTotalCost}</p>
              </div>
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
              onChange={(event) => {
                setStatusFilter(event.target.value as StatusFilter);
                setPage(0);
              }}
            >
              <option value="ALL">Todos os status</option>
              {filterableStatuses.map((status) => {
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
        <>
          <div className="lead-portal-images__list" role="list">
            {paginatedSubmissions.map((submission) => {
              const displayStatus: LeadPortalImagePackageLifecycleStatus =
                (submission.lifecycleStatus ?? submission.status) as LeadPortalImagePackageLifecycleStatus;
              const detail = buildStatusNarrative(displayStatus);
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
                        className={`badge d-inline-flex align-items-center gap-1 ${buildStatusBadgeClass(displayStatus)}`}
                      >
                        {buildPipelineIcon(displayStatus)}
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
                        {buildPipelineIcon(displayStatus)}
                      </div>
                      <div>
                        <p className="lead-portal-image-card__pipeline-title">{detail.title}</p>
                        <p className="lead-portal-image-card__pipeline-text">
                          {detail.description}
                        </p>
                        <p className="lead-portal-image-card__pipeline-text text-muted mb-0">
                          Prompt base: {submission.prompt}
                        </p>
                        {submission.emailOpenedAt ? (
                          <p
                            className="lead-portal-image-card__pipeline-text text-success mb-0"
                            aria-label="Registro de abertura do e-mail"
                          >
                            E-mail de amostra aberto em {formatDate(submission.emailOpenedAt)}
                          </p>
                        ) : null}
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

          <div
            className="lead-portal-images__pagination"
            aria-label="Paginação dos pacotes de imagem"
          >
            <p className="lead-portal-images__pagination-text mb-0">
              Exibindo {totalDisplayed} de {submissions.length} pacotes · Página {currentPage}
              de {Math.max(totalPages, 1)}
              {isFetching ? (
                <span className="ms-2 align-middle" role="status" aria-live="polite">
                  <Loader2 className="spin" size={16} aria-hidden="true" />
                </span>
              ) : null}
            </p>
            <div className="btn-group" role="group" aria-label="Navegação de páginas">
              <button
                type="button"
                className="btn btn-outline-secondary"
                disabled={!canGoPrevious}
                onClick={() => {
                  if (canGoPrevious) {
                    setPage((value) => Math.max(0, value - 1));
                  }
                }}
              >
                Anterior
              </button>
              <button
                type="button"
                className="btn btn-outline-secondary"
                disabled={!canGoNext}
                onClick={() => {
                  if (canGoNext) {
                    setPage((value) => value + 1);
                  }
                }}
              >
                Próxima
              </button>
            </div>
          </div>
        </>
      )}

    </div>
  );
}
