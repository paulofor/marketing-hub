import PageTitle from "../../components/PageTitle";
import OprmModuleNavigation from "./OprmModuleNavigation";
import { useOprmTopCnaeMarketVolume } from "../../api/oprm/useOprmTopCnaeMarketVolume";
import { useOprmCnaeCatalog } from "../../api/oprm/useOprmCnaeCatalog";
import { useOprmCnaeOpportunityScores } from "../../api/oprm/useOprmCnaeOpportunityScores";
import { useOprmCnaeCycles } from "../../api/oprm/useOprmCnaeCycles";
import { useMemo, useState } from "react";

function formatNumber(value: number) {
  return value.toLocaleString("pt-BR");
}

function formatScore(value: number | undefined) {
  return value == null
    ? "Pendente"
    : value.toLocaleString("pt-BR", { maximumFractionDigits: 2 });
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return "-";
  }
  return new Date(value).toLocaleString("pt-BR");
}

export default function OprmCnaeVolumePage() {
  const pageSize = 50;
  const [currentPage, setCurrentPage] = useState(1);
  const { data, isLoading, isError, refetch, isFetching } =
    useOprmTopCnaeMarketVolume(currentPage - 1, pageSize);
  const cnaeCatalogQuery = useOprmCnaeCatalog();
  const scoreQuery = useOprmCnaeOpportunityScores(500);
  const cycleQuery = useOprmCnaeCycles(5);
  const scoreByCnae = useMemo(() => {
    return new Map(
      (scoreQuery.data ?? []).map((score) => [score.cnaeCode, score]),
    );
  }, [scoreQuery.data]);
  const hasVolumeData = (data ?? []).length > 0;
  const hasCatalogData = (cnaeCatalogQuery.data ?? []).length > 0;
  const volumeData = data ?? [];
  const hasNextPage = volumeData.length === pageSize;

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <PageTitle>CNAEs com maior volume</PageTitle>
        <p className="text-secondary mb-0">
          Ranking dos principais CNAEs por volume de empresas no snapshot mais
          recente da ingestão.
        </p>
      </header>

      <OprmModuleNavigation />

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex justify-content-between align-items-center">
          <div>
            <h2 className="h5 mb-1">Top CNAEs por volume</h2>
            <p className="text-secondary mb-0">
              Exibindo 50 CNAEs por página, ordenados da maior quantidade de
              empresas MEI para a menor. O score é gerado automaticamente pelo
              OPRM; o usuário apenas acompanha ciclos, candidatos e decisões.
            </p>
          </div>
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
                      <th>Empresas MEI ↓</th>
                      <th>Estab. total</th>
                      <th>Empresas</th>
                      <th>Empresas Simples</th>
                      <th>Estab. ativos</th>
                      <th>Score OPRM</th>
                      <th>Status score</th>
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
                        <td>{(currentPage - 1) * pageSize + index + 1}</td>
                        <td>{item.cnaeCode}</td>
                        <td>{item.cnaeDescription ?? "-"}</td>
                        <td>{formatNumber(item.totalEmpresasMei)}</td>
                        <td>{formatNumber(item.totalEstabelecimentos)}</td>
                        <td>{formatNumber(item.totalEmpresas)}</td>
                        <td>{formatNumber(item.totalEmpresasSimples)}</td>
                        <td>
                          {formatNumber(item.totalEstabelecimentosAtivos)}
                        </td>
                        <td>
                          {formatScore(
                            scoreByCnae.get(item.cnaeCode)?.opportunityScore,
                          )}
                        </td>
                        <td>
                          {scoreByCnae.get(item.cnaeCode)?.scoreStatus ??
                            "Aguardando scheduler"}
                        </td>
                      </tr>
                    ))
                  : (cnaeCatalogQuery.data ?? [])
                      .slice(0, 25)
                      .map((item, index) => (
                        <tr key={item.cnaeCode}>
                          <td>{index + 1}</td>
                          <td>{item.cnaeCode}</td>
                          <td>{item.description ?? "-"}</td>
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
