import PageTitle from "../../components/PageTitle";
import OprmModuleNavigation from "./OprmModuleNavigation";
import { useOprmTopCnaeMarketVolume } from "../../api/oprm/useOprmTopCnaeMarketVolume";
import { useOprmCnaeCatalog } from "../../api/oprm/useOprmCnaeCatalog";
import { useEffect, useMemo, useState } from "react";

function formatNumber(value: number) {
  return value.toLocaleString("pt-BR");
}

export default function OprmCnaeVolumePage() {
  const pageSize = 50;
  const [currentPage, setCurrentPage] = useState(1);
  const { data, isLoading, isError, refetch, isFetching } = useOprmTopCnaeMarketVolume(500);
  const cnaeCatalogQuery = useOprmCnaeCatalog();
  const hasVolumeData = (data ?? []).length > 0;
  const hasCatalogData = (cnaeCatalogQuery.data ?? []).length > 0;
  const sortedVolumeData = useMemo(
    () => [...(data ?? [])].sort((a, b) => b.totalEmpresas - a.totalEmpresas),
    [data],
  );
  const paginatedVolumeData = useMemo(() => {
    const startIndex = (currentPage - 1) * pageSize;
    return sortedVolumeData.slice(startIndex, startIndex + pageSize);
  }, [currentPage, sortedVolumeData]);
  const totalPages = Math.max(1, Math.ceil(sortedVolumeData.length / pageSize));

  useEffect(() => {
    setCurrentPage((prev) => Math.min(prev, totalPages));
  }, [totalPages]);

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <PageTitle>CNAEs com maior volume</PageTitle>
        <p className="text-secondary mb-0">
          Ranking dos principais CNAEs por volume de empresas no snapshot mais recente da ingestão.
        </p>
      </header>

      <OprmModuleNavigation />

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex justify-content-between align-items-center">
          <div>
            <h2 className="h5 mb-1">Top CNAEs por volume</h2>
            <p className="text-secondary mb-0">Exibindo 50 CNAEs por página, ordenados da maior quantidade para a menor.</p>
          </div>
          <button
            type="button"
            className="btn btn-outline-primary"
            onClick={() => refetch()}
            disabled={isFetching}
          >
            {isFetching ? <span className="spinner-border spinner-border-sm" aria-hidden="true" /> : "Atualizar"}
          </button>
        </div>
      </section>

      {isLoading ? <div className="spinner-border text-primary" role="status" aria-label="Carregando" /> : null}
      {isError ? <div className="alert alert-danger">Não foi possível carregar os CNAEs.</div> : null}

      {!isLoading && !isError && !hasVolumeData && hasCatalogData ? (
        <div className="alert alert-warning">
          O catálogo de CNAEs já está disponível, mas as métricas de volume ainda não foram consolidadas pela ingestão.
        </div>
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
                      <th>Quantidade</th>
                      <th>Empresas</th>
                      <th>Empresas MEI</th>
                      <th>Empresas Simples</th>
                      <th>Estab. ativos</th>
                    </>
                  ) : (
                    <th>Status</th>
                  )}
                </tr>
              </thead>
              <tbody>
                {hasVolumeData
                  ? paginatedVolumeData.map((item, index) => (
                      <tr key={`${item.snapshotDate}-${item.cnaeCode}`}>
                        <td>{(currentPage - 1) * pageSize + index + 1}</td>
                        <td>{item.cnaeCode}</td>
                        <td>{item.cnaeDescription ?? "-"}</td>
                        <td>{formatNumber(item.totalEmpresas)}</td>
                        <td>{formatNumber(item.totalEmpresas)}</td>
                        <td>{formatNumber(item.totalEmpresasMei)}</td>
                        <td>{formatNumber(item.totalEmpresasSimples)}</td>
                        <td>{formatNumber(item.totalEstabelecimentosAtivos)}</td>
                      </tr>
                    ))
                  : (cnaeCatalogQuery.data ?? []).slice(0, 25).map((item, index) => (
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
              <span className="small text-secondary">Página {currentPage} de {totalPages}</span>
              <div className="btn-group">
                <button
                  type="button"
                  className="btn btn-sm btn-outline-secondary"
                  onClick={() => setCurrentPage((prev) => Math.max(1, prev - 1))}
                  disabled={currentPage === 1}
                >
                  Anterior
                </button>
                <button
                  type="button"
                  className="btn btn-sm btn-outline-secondary"
                  onClick={() => setCurrentPage((prev) => Math.min(totalPages, prev + 1))}
                  disabled={currentPage === totalPages}
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
