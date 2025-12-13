import { Fragment, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useMicroserviceExceptions } from "../../api/microservice/useMicroserviceExceptions";
import { useMicroservices } from "../../api/microservice/useMicroservices";
import type { MicroserviceException } from "../../api/microservice/useMicroserviceExceptions";
import { useBreadcrumbs } from "../../app/breadcrumbs";

const PAGE_SIZE = 20;

function formatDateTime(value?: string | null) {
  if (!value) return "—";
  try {
    return new Intl.DateTimeFormat("pt-BR", {
      dateStyle: "short",
      timeStyle: "short",
    }).format(new Date(value));
  } catch (error) {
    return value;
  }
}

function severityBadgeClass(severity?: string | null) {
  const normalized = severity?.toUpperCase();
  if (normalized === "WARN") return "badge text-bg-warning";
  if (normalized === "INFO") return "badge text-bg-info";
  return "badge text-bg-danger";
}

function prettyContext(context?: string | null) {
  if (!context) return null;
  try {
    const parsed = JSON.parse(context);
    return JSON.stringify(parsed, null, 2);
  } catch (error) {
    return context;
  }
}

export default function MicroserviceExceptionListPage() {
  useBreadcrumbs([{ label: "Erros de microserviço" }]);

  const [searchParams] = useSearchParams();
  const initialMicroserviceId = searchParams.get("microserviceId");

  const [page, setPage] = useState(0);
  const [microserviceId, setMicroserviceId] = useState<string>(
    initialMicroserviceId || "",
  );
  const [severity, setSeverity] = useState("");
  const [expandedId, setExpandedId] = useState<number | null>(null);

  useEffect(() => {
    if (initialMicroserviceId) {
      setMicroserviceId(initialMicroserviceId);
    }
  }, [initialMicroserviceId]);

  const queryParams = useMemo(
    () => ({
      page,
      size: PAGE_SIZE,
      microserviceId: microserviceId ? Number(microserviceId) : undefined,
      severity: severity.trim() || undefined,
    }),
    [microserviceId, page, severity],
  );

  const { data, isLoading, isFetching } = useMicroserviceExceptions(queryParams);
  const { data: microservices } = useMicroservices();

  const exceptions: MicroserviceException[] = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;
  const canGoPrevious = page > 0;
  const canGoNext = page + 1 < totalPages;

  return (
    <div>
      <PageTitle>Erros de microserviço</PageTitle>
      <p className="text-body-secondary">
        Consulte as exceções recebidas dos workers em background. Use os filtros para focar em um microserviço específico ou por severidade.
      </p>

      <div className="card mb-3">
        <div className="card-body">
          <form className="row g-3" onSubmit={(event) => event.preventDefault()}>
            <div className="col-sm-6 col-md-4 col-lg-3">
              <label className="form-label" htmlFor="microservice-select">
                Microserviço
              </label>
              <select
                id="microservice-select"
                className="form-select"
                value={microserviceId}
                onChange={(event) => {
                  setMicroserviceId(event.target.value);
                  setPage(0);
                }}
              >
                <option value="">Todos</option>
                {microservices?.map((service) => (
                  <option key={service.id} value={service.id}>
                    {service.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-sm-6 col-md-3 col-lg-2">
              <label className="form-label" htmlFor="microservice-severity">
                Severidade
              </label>
              <select
                id="microservice-severity"
                className="form-select"
                value={severity}
                onChange={(event) => {
                  setSeverity(event.target.value);
                  setPage(0);
                }}
              >
                <option value="">Todas</option>
                <option value="ERROR">Erro</option>
                <option value="WARN">Aviso</option>
                <option value="INFO">Info</option>
              </select>
            </div>
            <div className="col-sm-12 col-md-3 col-lg-2 align-self-end">
              <button
                type="button"
                className="btn btn-outline-secondary"
                onClick={() => {
                  setMicroserviceId("");
                  setSeverity("");
                  setPage(0);
                }}
              >
                Limpar filtros
              </button>
            </div>
          </form>
        </div>
      </div>

      {isLoading ? (
        <div className="d-flex align-items-center gap-2">
          <span className="spinner-border spinner-border-sm text-primary" aria-hidden="true" />
          <span>Carregando erros...</span>
        </div>
      ) : exceptions.length === 0 ? (
        <p className="text-body-secondary mb-0">Nenhuma exceção registrada.</p>
      ) : (
        <div className="table-responsive">
          <table className="table align-middle">
            <thead>
              <tr>
                <th className="text-nowrap">Registrado em</th>
                <th>Microserviço</th>
                <th>Severidade</th>
                <th>Mensagem</th>
                <th className="text-nowrap">Origem</th>
                <th className="text-end">Detalhes</th>
              </tr>
            </thead>
            <tbody>
              {exceptions.map((item) => {
                const isExpanded = expandedId === item.id;
                const context = prettyContext(item.context);
                return (
                  <Fragment key={item.id}>
                    <tr>
                      <td className="text-nowrap">{formatDateTime(item.occurredAt || item.createdAt)}</td>
                      <td>
                        <div className="fw-semibold">{item.microserviceName}</div>
                        <div className="text-body-secondary small">ID {item.microserviceId}</div>
                      </td>
                      <td>
                        <span className={severityBadgeClass(item.severity)}>
                          {item.severity ?? "ERROR"}
                        </span>
                      </td>
                      <td style={{ maxWidth: 360 }}>
                        <div className="text-truncate" title={item.message}>
                          {item.message}
                        </div>
                        <div className="text-body-secondary small text-truncate" title={item.exceptionType ?? undefined}>
                          {item.exceptionType ?? "Exception"}
                        </div>
                      </td>
                      <td>
                        <div className="text-body-secondary small">
                          {item.hostname ? `Host: ${item.hostname}` : "Host não informado"}
                        </div>
                        <div className="text-body-secondary small">
                          {item.serviceVersion ? `Versão ${item.serviceVersion}` : "Versão não informada"}
                        </div>
                      </td>
                      <td className="text-end">
                        <button
                          type="button"
                          className="btn btn-sm btn-outline-secondary"
                          onClick={() => setExpandedId(isExpanded ? null : item.id)}
                        >
                          {isExpanded ? "Ocultar" : "Ver"}
                        </button>
                      </td>
                    </tr>
                    {isExpanded ? (
                      <tr className="table-light">
                        <td colSpan={6}>
                          <div className="mb-3">
                            <h6 className="fw-semibold mb-1">Stack trace</h6>
                            <pre className="bg-body-tertiary p-3 rounded text-break" style={{ whiteSpace: "pre-wrap" }}>
                              {item.stackTrace || "—"}
                            </pre>
                          </div>
                          <div className="mb-3">
                            <h6 className="fw-semibold mb-1">Contexto</h6>
                            <pre className="bg-body-tertiary p-3 rounded text-break" style={{ whiteSpace: "pre-wrap" }}>
                              {context || "—"}
                            </pre>
                          </div>
                          <div>
                            <h6 className="fw-semibold mb-1">Payload bruto</h6>
                            <pre className="bg-body-tertiary p-3 rounded text-break" style={{ whiteSpace: "pre-wrap" }}>
                              {item.message}
                            </pre>
                          </div>
                        </td>
                      </tr>
                    ) : null}
                  </Fragment>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      <div className="d-flex align-items-center justify-content-between mt-3">
        <div className="text-body-secondary">
          Página {totalPages === 0 ? 0 : page + 1} de {totalPages}
          {isFetching ? (
            <span className="ms-2">
              <span className="spinner-border spinner-border-sm text-primary" aria-hidden="true" />
            </span>
          ) : null}
        </div>
        <div className="btn-group" role="group" aria-label="Paginação de exceções">
          <button
            type="button"
            className="btn btn-outline-secondary"
            disabled={!canGoPrevious}
            onClick={() => {
              if (canGoPrevious) {
                setPage((value) => Math.max(0, value - 1));
                setExpandedId(null);
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
                setExpandedId(null);
              }
            }}
          >
            Próxima
          </button>
        </div>
      </div>
    </div>
  );
}
