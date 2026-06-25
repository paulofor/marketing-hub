import { Link } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import OprmModuleNavigation from "./OprmModuleNavigation";
import { useOprmTopCnaeMarketVolume } from "../../api/oprm/useOprmTopCnaeMarketVolume";
import { useOprmCnaeCatalog } from "../../api/oprm/useOprmCnaeCatalog";
import { useOprmCnaeCycles } from "../../api/oprm/useOprmCnaeCycles";
import { useOprmEnrichedNicheCandidates } from "../../api/oprm/useOprmEnrichedNicheCandidates";
import { useState } from "react";

function formatNumber(value: number) {
  return value.toLocaleString("pt-BR");
}

function formatScore(value: number | undefined) {
  return value == null
    ? "Pendente"
    : value.toLocaleString("pt-BR", { maximumFractionDigits: 2 });
}

function formatCurrencyUsd(value: number | null | undefined) {
  return (value ?? 0).toLocaleString("pt-BR", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 4,
    maximumFractionDigits: 4,
  });
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return "-";
  }
  return new Date(value).toLocaleString("pt-BR");
}

function RunningProcessingIcon() {
  return (
    <span
      className="d-inline-flex align-items-center gap-1 text-primary"
      title="Processamento em execução neste CNAE"
      aria-label="Processamento em execução neste CNAE"
    >
      <span className="spinner-border spinner-border-sm" aria-hidden="true" />
      <span aria-hidden="true">⚙️</span>
    </span>
  );
}

export default function OprmCnaeVolumePage() {
  const pageSize = 50;
  const [currentPage, setCurrentPage] = useState(1);
  const [showEnrichedNiches, setShowEnrichedNiches] = useState(false);
  const { data, isLoading, isError, refetch, isFetching } =
    useOprmTopCnaeMarketVolume(currentPage - 1, pageSize);
  const cnaeCatalogQuery = useOprmCnaeCatalog();
  const cycleQuery = useOprmCnaeCycles(5);
  const enrichedNichesQuery = useOprmEnrichedNicheCandidates(
    100,
    showEnrichedNiches,
  );
  const hasVolumeData = (data ?? []).length > 0;
  const hasCatalogData = (cnaeCatalogQuery.data ?? []).length > 0;
  const volumeData = data ?? [];
  const hasNextPage = volumeData.length === pageSize;

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <PageTitle>CNAEs por Score OPRM</PageTitle>
        <p className="text-secondary mb-0">
          Ranking dos principais CNAEs por Score OPRM no snapshot mais recente
          da ingestão, com volume de mercado como contexto.
        </p>
      </header>

      <OprmModuleNavigation />

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex justify-content-between align-items-center">
          <div>
            <h2 className="h5 mb-1">Top CNAEs por Score OPRM</h2>
            <p className="text-secondary mb-0">
              Exibindo 50 CNAEs por página, ordenados pelo Score OPRM em ordem
              decrescente. O score é gerado automaticamente pelo OPRM; o usuário
              apenas acompanha ciclos, candidatos e decisões.
            </p>
          </div>
          <div className="d-flex gap-2">
            <button
              type="button"
              className="btn btn-outline-primary"
              onClick={() => setShowEnrichedNiches((current) => !current)}
              disabled={enrichedNichesQuery.isFetching}
            >
              {enrichedNichesQuery.isFetching ? (
                <span
                  className="spinner-border spinner-border-sm"
                  aria-hidden="true"
                />
              ) : showEnrichedNiches ? (
                "Ocultar enriquecidos"
              ) : (
                "Ver nichos enriquecidos"
              )}
            </button>
            <button
              type="button"
              className="btn btn-outline-primary"
              onClick={() => refetch()}
              disabled={isFetching}
            >
              {isFetching ? (
                <span
                  className="spinner-border spinner-border-sm"
                  aria-hidden="true"
                />
              ) : (
                "Atualizar"
              )}
            </button>
          </div>
        </div>
      </section>

      {isLoading ? (
        <div
          className="spinner-border text-primary"
          role="status"
          aria-label="Carregando"
        />
      ) : null}
      {isError ? (
        <div className="alert alert-danger">
          Não foi possível carregar os CNAEs.
        </div>
      ) : null}

      {!isLoading && !isError && !hasVolumeData && hasCatalogData ? (
        <div className="alert alert-warning">
          O catálogo de CNAEs já está disponível, mas as métricas de volume
          ainda não foram consolidadas pela ingestão.
        </div>
      ) : null}

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <h2 className="h5 mb-3">Últimos ciclos automáticos OPRM</h2>
          {cycleQuery.isLoading ? (
            <div
              className="spinner-border spinner-border-sm text-primary"
              role="status"
              aria-label="Carregando ciclos"
            />
          ) : null}
          {cycleQuery.isError ? (
            <div className="alert alert-warning mb-0">
              Não foi possível carregar os ciclos automáticos de score e
              enriquecimento.
            </div>
          ) : null}
          {!cycleQuery.isLoading && !cycleQuery.isError ? (
            <div className="table-responsive">
              <table className="table table-sm align-middle mb-0">
                <thead>
                  <tr>
                    <th>Ciclo</th>
                    <th>Tipo</th>
                    <th>Status</th>
                    <th>Processados</th>
                    <th>Falhas</th>
                    <th>Início</th>
                  </tr>
                </thead>
                <tbody>
                  {(cycleQuery.data ?? []).length > 0 ? (
                    (cycleQuery.data ?? []).map((cycle) => (
                      <tr key={cycle.cycleId}>
                        <td>{cycle.cycleId}</td>
                        <td>{cycle.cycleType}</td>
                        <td>{cycle.status}</td>
                        <td>{formatNumber(cycle.processedCount)}</td>
                        <td>{formatNumber(cycle.failedCount)}</td>
                        <td>{formatDateTime(cycle.startedAt)}</td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan={6} className="text-secondary">
                        Nenhum ciclo automático registrado ainda.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          ) : null}
        </div>
      </section>

      {showEnrichedNiches ? (
        <section className="card border-0 shadow-sm">
          <div className="card-body">
            <div className="d-flex justify-content-between align-items-start gap-3 mb-3">
              <div>
                <h2 className="h5 mb-1">Nichos já enriquecidos</h2>
                <p className="text-secondary mb-0">
                  Lista dos candidatos de nicho que já receberam enriquecimento
                  do OPRM, ordenada pelos maiores scores, com dor, resultado e
                  mecanismo para decisão do usuário.
                </p>
              </div>
              <button
                type="button"
                className="btn btn-sm btn-outline-secondary"
                onClick={() => enrichedNichesQuery.refetch()}
                disabled={enrichedNichesQuery.isFetching}
              >
                {enrichedNichesQuery.isFetching ? (
                  <span
                    className="spinner-border spinner-border-sm"
                    aria-hidden="true"
                  />
                ) : (
                  "Atualizar nichos"
                )}
              </button>
            </div>
            {enrichedNichesQuery.isError ? (
              <div className="alert alert-warning mb-0">
                Não foi possível carregar os nichos enriquecidos.
              </div>
            ) : null}
            {!enrichedNichesQuery.isError ? (
              <div className="table-responsive">
                <table className="table table-sm align-middle mb-0">
                  <thead>
                    <tr>
                      <th>Nicho</th>
                      <th>CNAE</th>
                      <th>Dor</th>
                      <th>Resultado</th>
                      <th>Mecanismo</th>
                      <th>Score</th>
                      <th>Status</th>
                      <th>Enriquecido em</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(enrichedNichesQuery.data ?? []).length > 0 ? (
                      (enrichedNichesQuery.data ?? []).map((candidate) => (
                        <tr key={candidate.id}>
                          <td>{candidate.candidateNicheName}</td>
                          <td>
                            <strong>{candidate.cnaeCode}</strong>
                            <br />
                            <span className="small text-secondary">
                              {candidate.cnaeDescription}
                            </span>
                          </td>
                          <td>{candidate.painHypothesis ?? "-"}</td>
                          <td>{candidate.desiredOutcome ?? "-"}</td>
                          <td>{candidate.mechanismHypothesis ?? "-"}</td>
                          <td>{formatScore(candidate.opportunityScore)}</td>
                          <td>{candidate.status}</td>
                          <td>{formatDateTime(candidate.createdAt)}</td>
                        </tr>
                      ))
                    ) : (
                      <tr>
                        <td colSpan={8} className="text-secondary">
                          Nenhum nicho enriquecido registrado ainda.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            ) : null}
          </div>
        </section>
      ) : null}

      {!isLoading && !isError ? (
        <section className="card border-0 shadow-sm">
          <div className="table-responsive">
            <table className="table table-striped align-middle mb-0">
              <thead>
                <tr>
                  <th>#</th>
                  <th>CNAE</th>
                  <th>Descrição</th>
                  {hasVolumeData ? (
                    <>
                      <th>Score OPRM ↓</th>
                      <th>Estab. total</th>
                      <th>Simples</th>
                      <th>MEI</th>
                      <th>Subnichos</th>
                      <th>Pendente materializar</th>
                      <th>Custo</th>
                      <th>Pesquisa</th>
                      <th>Pipeline v2</th>
                    </>
                  ) : (
                    <th>Status</th>
                  )}
                </tr>
              </thead>
              <tbody>
                {hasVolumeData
                  ? volumeData.map((item, index) => (
                      <tr key={`${item.snapshotDate}-${item.cnaeCode}`}>
                        <td>
                          <span className="d-inline-flex align-items-center gap-2">
                            <span>{(currentPage - 1) * pageSize + index + 1}</span>
                            {item.nicheResearchRunning ? (
                              <RunningProcessingIcon />
                            ) : null}
                          </span>
                        </td>
                        <td>{item.cnaeCode}</td>
                        <td>
                          <Link
                            to={`/oprm/cnaes/${encodeURIComponent(item.cnaeCode)}`}
                            className="fw-semibold text-decoration-none"
                            title="Abrir detalhe do nicho CNAE"
                          >
                            {item.cnaeDescription ?? "Nicho sem descrição"}
                          </Link>
                        </td>
                        <td>
                          {formatScore(item.opportunityScore ?? undefined)}
                        </td>
                        <td>{formatNumber(item.totalEstabelecimentos)}</td>
                        <td>{formatNumber(item.totalEmpresasSimples)}</td>
                        <td>{formatNumber(item.totalEmpresasMei)}</td>
                        <td>{formatNumber(item.subnicheCount)}</td>
                        <td>
                          {item.pendingMaterializationCount > 0 ? (
                            <span className="badge text-bg-warning">
                              {formatNumber(item.pendingMaterializationCount)}{" "}
                              pendente
                            </span>
                          ) : (
                            <span className="text-secondary">-</span>
                          )}
                        </td>
                        <td>{formatCurrencyUsd(item.researchCostUsd)}</td>
                        <td>
                          {item.nicheResearchRunning ? (
                            <RunningProcessingIcon />
                          ) : (
                            <span
                              className="text-secondary"
                              aria-label="Sem pesquisa em execução"
                            >
                              -
                            </span>
                          )}
                        </td>
                        <td>
                          <Link
                            className="btn btn-sm btn-outline-primary text-nowrap"
                            to={`/oprm/cnaes/${encodeURIComponent(item.cnaeCode)}/pipeline-v2`}
                            title="Abrir design da v2 do pipeline NichoCNAE"
                          >
                            Ver v2
                          </Link>
                        </td>
                      </tr>
                    ))
                  : (cnaeCatalogQuery.data ?? [])
                      .slice(0, 25)
                      .map((item, index) => (
                        <tr key={item.cnaeCode}>
                          <td>{index + 1}</td>
                          <td>{item.cnaeCode}</td>
                          <td>
                            <Link
                              to={`/oprm/cnaes/${encodeURIComponent(item.cnaeCode)}`}
                              className="fw-semibold text-decoration-none"
                              title="Abrir detalhe do nicho CNAE"
                            >
                              {item.description ?? "Nicho sem descrição"}
                            </Link>
                          </td>
                          <td>{item.active ? "Importado" : "Inativo"}</td>
                        </tr>
                      ))}
              </tbody>
            </table>
          </div>
          {hasVolumeData ? (
            <div className="d-flex justify-content-between align-items-center px-3 py-2 border-top">
              <span className="small text-secondary">Página {currentPage}</span>
              <div className="btn-group">
                <button
                  type="button"
                  className="btn btn-sm btn-outline-secondary"
                  onClick={() =>
                    setCurrentPage((prev) => Math.max(1, prev - 1))
                  }
                  disabled={currentPage === 1}
                >
                  Anterior
                </button>
                <button
                  type="button"
                  className="btn btn-sm btn-outline-secondary"
                  onClick={() => setCurrentPage((prev) => prev + 1)}
                  disabled={!hasNextPage || isFetching}
                >
                  Próxima
                </button>
              </div>
            </div>
          ) : null}
        </section>
      ) : null}
    </div>
  );
}
