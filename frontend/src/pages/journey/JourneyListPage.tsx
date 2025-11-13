import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useJourneys } from "../../api/journey/useJourneys";
import { useJourneyTemplates } from "../../api/journey/useJourneyTemplates";
import { useJourneyMetrics } from "../../api/journey/useJourneyMetrics";
import type { Journey, JourneyStatus } from "../../api/journey/types";
import JourneyStatusBadge from "./JourneyStatusBadge";
import "./JourneyListPage.css";

const STATUS_OPTIONS: { value: JourneyStatus; label: string }[] = [
  { value: "ACTIVE", label: "Ativas" },
  { value: "DRAFT", label: "Rascunhos" },
  { value: "PAUSED", label: "Pausadas" },
  { value: "COMPLETED", label: "Concluídas" },
  { value: "ARCHIVED", label: "Arquivadas" },
];

function formatDateTime(value?: string | null) {
  if (!value) {
    return "—";
  }
  try {
    return new Intl.DateTimeFormat("pt-BR", {
      dateStyle: "short",
      timeStyle: "short",
    }).format(new Date(value));
  } catch (error) {
    return value;
  }
}

function highlightMetadata(metadata: Record<string, string>) {
  return Object.entries(metadata)
    .filter(([key]) => Boolean(key))
    .slice(0, 6);
}

function matchesSearch(journey: Journey, term: string) {
  const normalised = term.trim().toLowerCase();
  if (!normalised) {
    return true;
  }
  return (
    journey.name.toLowerCase().includes(normalised) ||
    journey.templateName.toLowerCase().includes(normalised) ||
    (journey.segmentReference ?? "").toLowerCase().includes(normalised)
  );
}

export default function JourneyListPage() {
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<JourneyStatus | "">("");
  const [templateFilter, setTemplateFilter] = useState<number | "">("");
  const [searchTerm, setSearchTerm] = useState("");
  const pageSize = 12;

  const { data: templatePage } = useJourneyTemplates();
  const templates = templatePage?.content ?? [];

  const journeysQuery = useJourneys({
    page,
    size: pageSize,
    status: statusFilter || undefined,
    templateId: templateFilter ? Number(templateFilter) : undefined,
  });

  const journeysPage = journeysQuery.data;
  const isLoading = journeysQuery.isLoading;

  const { data: metrics, isLoading: metricsLoading } = useJourneyMetrics();

  const journeys: Journey[] = journeysPage?.content ?? [];

  const filteredJourneys = useMemo(
    () => journeys.filter((journey) => matchesSearch(journey, searchTerm)),
    [journeys, searchTerm],
  );

  const totalPages = journeysPage?.totalPages ?? 0;
  const canGoPrevious = page > 0;
  const canGoNext = page + 1 < totalPages;

  const statusBreakdown = metrics?.statusBreakdown ?? {};

  return (
    <div className="journey-page">
      <header className="journey-page__hero">
        <div>
          <PageTitle>Jornadas</PageTitle>
          <p className="journey-page__subtitle">
            Monitore o progresso das jornadas de marketing, acompanhe metas e ajuste rapidamente cada etapa da orquestração.
          </p>
        </div>
        <div className="journey-page__hero-actions">
          <Link className="btn btn-outline-secondary" to="/journey-templates">
            Ver templates
          </Link>
        </div>
      </header>

      <section className="journey-metrics">
        <div className="journey-metric-card journey-metric-card--primary">
          <p className="journey-metric-card__label">Jornadas totais</p>
          <p className="journey-metric-card__value">
            {metricsLoading ? (
              <span
                className="spinner-border spinner-border-sm text-primary"
                role="status"
                aria-hidden="true"
              />
            ) : (
              metrics?.totalJourneys ?? 0
            )}
          </p>
          <p className="journey-metric-card__description">
            Instâncias operacionais prontas para acompanhamento.
          </p>
        </div>
        {STATUS_OPTIONS.map((option) => (
          <div
            key={option.value}
            className={`journey-metric-card journey-metric-card--${option.value.toLowerCase()}`}
          >
            <p className="journey-metric-card__label">{option.label}</p>
            <p className="journey-metric-card__value">
              {metricsLoading ? (
                <span
                  className="spinner-border spinner-border-sm text-primary"
                  role="status"
                  aria-hidden="true"
                />
              ) : (
                statusBreakdown[option.value] ?? 0
              )}
            </p>
            <p className="journey-metric-card__description">
              {option.value === "ACTIVE"
                ? "Jornadas em execução com estímulos ativos."
                : option.value === "DRAFT"
                ? "Configurações em fase de alinhamento."
                : option.value === "PAUSED"
                ? "Instâncias aguardando retomada."
                : option.value === "COMPLETED"
                ? "Fluxos finalizados com sucesso."
                : "Histórico arquivado para consulta."}
            </p>
          </div>
        ))}
      </section>

      <section className="journey-filters">
        <div className="journey-filters__grid">
          <div>
            <label className="journey-filters__label" htmlFor="journey-search">
              Buscar por nome ou referência
            </label>
            <input
              id="journey-search"
              className="form-control"
              placeholder="Digite um termo de busca"
              value={searchTerm}
              onChange={(event) => {
                setSearchTerm(event.target.value);
                setPage(0);
              }}
            />
          </div>
          <div>
            <label className="journey-filters__label" htmlFor="journey-status">
              Status
            </label>
            <select
              id="journey-status"
              className="form-select"
              value={statusFilter}
              onChange={(event) => {
                setStatusFilter(event.target.value as JourneyStatus | "");
                setPage(0);
              }}
            >
              <option value="">Todos</option>
              {STATUS_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="journey-filters__label" htmlFor="journey-template">
              Template
            </label>
            <select
              id="journey-template"
              className="form-select"
              value={templateFilter}
              onChange={(event) => {
                setTemplateFilter(event.target.value ? Number(event.target.value) : "");
                setPage(0);
              }}
            >
              <option value="">Todos</option>
              {templates.map((template) => (
                <option key={template.id} value={template.id}>
                  {template.name}
                </option>
              ))}
            </select>
          </div>
        </div>
        <div className="journey-filters__actions">
          <button
            type="button"
            className="btn btn-outline-secondary"
            onClick={() => {
              setSearchTerm("");
              setStatusFilter("");
              setTemplateFilter("");
              setPage(0);
            }}
          >
            Limpar filtros
          </button>
        </div>
      </section>

      <section>
        {isLoading ? (
          <div className="journey-empty-state">Carregando jornadas...</div>
        ) : filteredJourneys.length === 0 ? (
          <div className="journey-empty-state">
            <p>Nenhuma jornada encontrada com os filtros atuais.</p>
            <p className="journey-empty-state__hint">
              Ajuste os filtros ou crie uma nova jornada personalizada.
            </p>
          </div>
        ) : (
          <div className="journey-grid">
            {filteredJourneys.map((journey) => {
              const metadata = highlightMetadata(journey.metadata);
              return (
                <article key={journey.id} className="journey-card">
                  <header className="journey-card__header">
                    <div>
                      <JourneyStatusBadge status={journey.status} />
                      <p className="journey-card__timestamp">
                        Atualizada em {formatDateTime(journey.updatedAt)}
                      </p>
                    </div>
                    <div className="journey-card__template">
                      <span className="journey-card__template-label">Template</span>
                      <strong>{journey.templateName}</strong>
                    </div>
                  </header>
                  <div className="journey-card__content">
                    <h3>{journey.name}</h3>
                    {journey.description ? (
                      <p className="journey-card__description">{journey.description}</p>
                    ) : null}
                    <dl className="journey-card__details">
                      <div>
                        <dt>Janela</dt>
                        <dd>
                          {formatDateTime(journey.startAt)}
                          <span className="journey-card__details-separator">→</span>
                          {formatDateTime(journey.endAt)}
                        </dd>
                      </div>
                      <div>
                        <dt>Segmento</dt>
                        <dd>{journey.segmentReference ?? "—"}</dd>
                      </div>
                      <div>
                        <dt>Nicho</dt>
                        <dd>{journey.marketNicheId ?? "—"}</dd>
                      </div>
                      <div>
                        <dt>Experimento</dt>
                        <dd>{journey.experimentId ?? "—"}</dd>
                      </div>
                    </dl>
                    {metadata.length ? (
                      <div className="journey-card__metadata">
                        {metadata.map(([key, value]) => (
                          <span key={key} className="journey-card__metadata-chip">
                            <strong>{key}:</strong> {value || "—"}
                          </span>
                        ))}
                      </div>
                    ) : null}
                  </div>
                  <footer className="journey-card__footer">
                    <Link
                      className="btn btn-outline-primary btn-sm"
                      to={`/journeys/${journey.id}`}
                    >
                      Ver detalhes
                    </Link>
                    <Link
                      className="btn btn-secondary btn-sm"
                      to={`/journeys/${journey.id}/edit`}
                    >
                      Editar
                    </Link>
                  </footer>
                </article>
              );
            })}
          </div>
        )}
      </section>

      {totalPages > 1 ? (
        <nav className="journey-pagination" aria-label="Paginação de jornadas">
          <button
            type="button"
            className="btn btn-outline-secondary"
            onClick={() => setPage((current) => Math.max(current - 1, 0))}
            disabled={!canGoPrevious}
          >
            Anterior
          </button>
          <span>
            Página {page + 1} de {totalPages}
          </span>
          <button
            type="button"
            className="btn btn-outline-secondary"
            onClick={() => setPage((current) => (canGoNext ? current + 1 : current))}
            disabled={!canGoNext}
          >
            Próxima
          </button>
        </nav>
      ) : null}
    </div>
  );
}
