import PageTitle from "../../components/PageTitle";
import OprmModuleNavigation from "./OprmModuleNavigation";
import { useOprmTopCnaeMarketVolume } from "../../api/oprm/useOprmTopCnaeMarketVolume";

function formatNumber(value: number) {
  return value.toLocaleString("pt-BR");
}

export default function OprmCnaeVolumePage() {
  const { data, isLoading, isError, refetch, isFetching } = useOprmTopCnaeMarketVolume(25);

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
            <p className="text-secondary mb-0">Exibindo os 25 CNAEs com mais empresas.</p>
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

      {!isLoading && !isError ? (
        <section className="card border-0 shadow-sm">
          <div className="table-responsive">
            <table className="table table-striped align-middle mb-0">
              <thead>
                <tr>
                  <th>#</th>
                  <th>CNAE</th>
                  <th>Descrição</th>
                  <th>Empresas</th>
                  <th>Empresas MEI</th>
                  <th>Empresas Simples</th>
                  <th>Estab. ativos</th>
                </tr>
              </thead>
              <tbody>
                {(data ?? []).map((item, index) => (
                  <tr key={`${item.snapshotDate}-${item.cnaeCode}`}>
                    <td>{index + 1}</td>
                    <td>{item.cnaeCode}</td>
                    <td>{item.cnaeDescription ?? "-"}</td>
                    <td>{formatNumber(item.totalEmpresas)}</td>
                    <td>{formatNumber(item.totalEmpresasMei)}</td>
                    <td>{formatNumber(item.totalEmpresasSimples)}</td>
                    <td>{formatNumber(item.totalEstabelecimentosAtivos)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      ) : null}
    </div>
  );
}
