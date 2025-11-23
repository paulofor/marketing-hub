import PageTitle from "../../components/PageTitle";
import { useLeadPortalExperimentMetrics } from "../../api/leadPortal/useLeadPortalExperimentMetrics";

export default function LeadPortalExperimentMetricsPage() {
  const { data, isLoading, isError } = useLeadPortalExperimentMetrics();

  const metrics = data ?? [];

  return (
    <div className="d-flex flex-column gap-3">
      <header>
        <PageTitle icon="/favicon.ico">
          Funil do Portal do Lead
        </PageTitle>
        <p className="text-muted mb-0">
          Acompanhe quantos leads avançaram para o portal e quantos concluíram o
          envio de imagem em cada experimento.
        </p>
      </header>

      {isLoading ? (
        <p className="text-muted">Carregando métricas...</p>
      ) : isError ? (
        <div className="alert alert-danger" role="alert">
          Não foi possível carregar os dados do funil agora. Tente novamente em
          instantes.
        </div>
      ) : metrics.length === 0 ? (
        <div className="alert alert-info" role="alert">
          Ainda não há experimentos com fluxo do portal registrado. Assim que os
          leads começarem a interagir, você verá os totais aqui.
        </div>
      ) : (
        <div className="card shadow-sm border-0">
          <div className="card-body">
            <div className="table-responsive">
              <table className="table align-middle mb-0">
                <thead>
                  <tr>
                    <th>Experimento</th>
                    <th>Leads que acessaram</th>
                    <th>Leads que enviaram imagem</th>
                    <th>Conversão para imagem</th>
                  </tr>
                </thead>
                <tbody>
                  {metrics.map((row) => {
                    const conversion =
                      row.leadsAccessed > 0
                        ? (row.leadsWithImage / row.leadsAccessed) * 100
                        : 0;

                    return (
                      <tr key={row.experimentId}>
                        <td className="fw-semibold">{row.experimentName}</td>
                        <td>{row.leadsAccessed}</td>
                        <td>{row.leadsWithImage}</td>
                        <td>
                          {conversion.toLocaleString("pt-BR", {
                            maximumFractionDigits: 1,
                            minimumFractionDigits: 0,
                          })}
                          %
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
