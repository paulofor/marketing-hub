import PageTitle from "../../components/PageTitle";
import { Fragment } from "react";
import {
  LeadPortalExperimentLead,
  useLeadPortalExperimentMetrics,
} from "../../api/leadPortal/useLeadPortalExperimentMetrics";

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
                    const uniqueLeads = row.uniqueLeads ?? [];
                    const conversion =
                      row.leadsAccessed > 0
                        ? (row.leadsWithImage / row.leadsAccessed) * 100
                        : 0;
                    const leadsWithImage = uniqueLeads.filter(
                      (lead) => lead.sentImage,
                    );

                    return (
                      <Fragment key={row.experimentId}>
                        <tr>
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
                        <tr className="table-light">
                          <td colSpan={4}>
                            <div className="d-flex flex-column gap-3">
                              <div>
                                <p className="text-muted fw-semibold small mb-2">
                                  Leads únicos que acessaram o fluxo
                                </p>
                                <div className="d-flex flex-wrap gap-2">
                                  {uniqueLeads.length === 0 ? (
                                    <span className="text-muted small">
                                      Nenhum acesso registrado para este
                                      experimento.
                                    </span>
                                  ) : (
                                    uniqueLeads.map((lead) => (
                                      <LeadBadge key={buildLeadKey(lead)} lead={lead} />
                                    ))
                                  )}
                                </div>
                              </div>

                              <div>
                                <p className="text-muted fw-semibold small mb-2">
                                  Enviaram imagem
                                </p>
                                <div className="d-flex flex-wrap gap-2">
                                  {leadsWithImage.length === 0 ? (
                                    <span className="text-muted small">
                                      Ainda não há envios de imagem.
                                    </span>
                                  ) : (
                                    leadsWithImage.map((lead) => (
                                      <LeadBadge key={buildLeadKey(lead)} lead={lead} highlight />
                                    ))
                                  )}
                                </div>
                              </div>
                            </div>
                          </td>
                        </tr>
                      </Fragment>
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

function LeadBadge({
  lead,
  highlight = false,
}: {
  lead: LeadPortalExperimentLead;
  highlight?: boolean;
}) {
  return (
    <span
      className={`badge rounded-pill d-flex align-items-center gap-2 py-2 px-3 ${
        highlight || lead.sentImage ? "text-bg-success" : "text-bg-secondary"
      }`}
      title={lead.sentImage ? "Enviou imagem" : "Sem envio de imagem"}
    >
      <span className="fw-semibold">{lead.displayName}</span>
      {(lead.email || lead.phone) && (
        <span className="small text-light">
          {[lead.email, lead.phone].filter(Boolean).join(" · ")}
        </span>
      )}
    </span>
  );
}

function buildLeadKey(lead: LeadPortalExperimentLead) {
  return [lead.displayName, lead.email, lead.phone].filter(Boolean).join("-");
}
