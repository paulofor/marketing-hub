import { Fragment, useMemo, useState } from "react";
import PageTitle from "../../components/PageTitle";
import { useAiGenerations } from "../../api/ai/useAiGenerations";
import type { AiGeneration } from "../../api/ai/useAiGenerations";
import { useBreadcrumbs } from "../../app/breadcrumbs";

const PAGE_SIZE = 20;

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

function formatCurrency(value?: number | null) {
  if (typeof value !== "number") {
    return "—";
  }
  try {
    return new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "USD",
      minimumFractionDigits: 2,
      maximumFractionDigits: 4,
    }).format(value);
  } catch (error) {
    return value.toFixed(4);
  }
}

function formatTokens(input?: number | null, output?: number | null) {
  const parts = [] as string[];
  if (typeof input === "number") {
    parts.push(`${input} in`);
  }
  if (typeof output === "number") {
    parts.push(`${output} out`);
  }
  return parts.length > 0 ? parts.join(" · ") : "—";
}

export default function AiGenerationListPage() {
  useBreadcrumbs([{ label: "Gerações IA" }]);

  const [page, setPage] = useState(0);
  const [domainFilter, setDomainFilter] = useState("");
  const [expandedId, setExpandedId] = useState<number | null>(null);

  const queryParams = useMemo(
    () => ({
      page,
      size: PAGE_SIZE,
      domain: domainFilter.trim() || undefined,
    }),
    [domainFilter, page],
  );

  const { data, isLoading, isFetching } = useAiGenerations(queryParams);
  const generations: AiGeneration[] = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;
  const canGoPrevious = page > 0;
  const canGoNext = page + 1 < totalPages;

  return (
    <div>
      <PageTitle>Gerações IA</PageTitle>
      <p className="text-body-secondary">
        Consulte todas as execuções do worker de IA, incluindo prompt, modelo utilizado e custo aproximado calculado a partir do uso de tokens.
      </p>

      <div className="card mb-3">
        <div className="card-body">
          <form className="row g-3" onSubmit={(event) => event.preventDefault()}>
            <div className="col-sm-6 col-md-4 col-lg-3">
              <label className="form-label" htmlFor="ai-generation-domain">
                Domínio
              </label>
              <input
                id="ai-generation-domain"
                className="form-control"
                placeholder="Ex.: NICHE_HYPOTHESIS"
                value={domainFilter}
                onChange={(event) => {
                  setDomainFilter(event.target.value);
                  setPage(0);
                }}
              />
            </div>
          </form>
        </div>
      </div>

      {isLoading ? (
        <div className="d-flex align-items-center gap-2">
          <span
            className="spinner-border spinner-border-sm text-primary"
            role="status"
            aria-hidden="true"
          />
          <span>Carregando histórico...</span>
        </div>
      ) : generations.length === 0 ? (
        <p className="text-body-secondary">Nenhuma geração registrada até o momento.</p>
      ) : (
        <div className="table-responsive">
          <table className="table align-middle">
            <thead>
              <tr>
                <th className="text-nowrap">Criado em</th>
                <th>Domínio</th>
                <th>Referência</th>
                <th>Modelo</th>
                <th>Tokens</th>
                <th className="text-nowrap">Custo (USD)</th>
                <th className="text-end">Detalhes</th>
              </tr>
            </thead>
            <tbody>
              {generations.map((generation) => {
                const isExpanded = expandedId === generation.id;
                return (
                  <Fragment key={generation.id}>
                    <tr>
                      <td className="text-nowrap">{formatDateTime(generation.createdAt)}</td>
                      <td>{generation.domain}</td>
                      <td>{generation.referenceId ?? "—"}</td>
                      <td>{generation.model ?? "—"}</td>
                      <td>{formatTokens(generation.inputTokens, generation.outputTokens)}</td>
                      <td className="text-nowrap">{formatCurrency(generation.costUsd)}</td>
                      <td className="text-end">
                        <button
                          type="button"
                          className="btn btn-sm btn-outline-secondary"
                          onClick={() => setExpandedId(isExpanded ? null : generation.id)}
                        >
                          {isExpanded ? "Ocultar" : "Ver"}
                        </button>
                      </td>
                    </tr>
                    {isExpanded ? (
                      <tr className="table-light">
                        <td colSpan={7}>
                          <div className="mb-3">
                            <h6 className="fw-semibold">Prompt</h6>
                            <pre
                              className="mb-0 bg-body-tertiary p-3 rounded text-break"
                              style={{ whiteSpace: "pre-wrap" }}
                            >
                              {generation.prompt ?? "—"}
                            </pre>
                          </div>
                          <div>
                            <h6 className="fw-semibold">Resposta</h6>
                            <pre
                              className="mb-0 bg-body-tertiary p-3 rounded text-break"
                              style={{ whiteSpace: "pre-wrap" }}
                            >
                              {generation.rawResponse ?? "—"}
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
              <span
                className="spinner-border spinner-border-sm text-primary"
                role="status"
                aria-hidden="true"
              />
            </span>
          ) : null}
        </div>
        <div className="btn-group" role="group" aria-label="Paginação das gerações">
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
