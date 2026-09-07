import type { ExperimentFacebookCampaignDto } from "../../api/experiment/useExperimentFacebookCampaigns";

type ExperimentFacebookCampaignTabProps = {
  campaigns?: ExperimentFacebookCampaignDto[];
  isLoading: boolean;
};

const BRAZIL_OPERATIONAL_TIME_ZONE = "America/Sao_Paulo";

/** Formata os marcos da campanha no fuso usado pela operação brasileira. */
function formatDate(value?: string | null) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    timeZone: BRAZIL_OPERATIONAL_TIME_ZONE,
  });
}

/** Resolve a cor do status sem inferir um estado diferente do informado pelo backend. */
function statusBadgeClass(status?: string | null) {
  if (status === "ACTIVE") return "text-bg-success";
  if (status === "PAUSED") return "text-bg-warning";
  if (status === "FAILED" || status === "ERROR") return "text-bg-danger";
  return "text-bg-secondary";
}

/** Exibe a estrutura Meta publicada e as etapas consolidadas no funil central. */
export default function ExperimentFacebookCampaignTab({
  campaigns,
  isLoading,
}: ExperimentFacebookCampaignTabProps) {
  if (isLoading) {
    return (
      <div className="card mt-3">
        <div className="card-body text-muted" role="status">
          <span
            className="spinner-border spinner-border-sm me-2"
            aria-hidden="true"
          />
          Carregando campanha publicada...
        </div>
      </div>
    );
  }

  if (!campaigns?.length) {
    return (
      <div className="alert alert-info mt-3" role="status">
        Nenhuma campanha Meta foi vinculada a este experimento. Quando a
        publicação ocorrer, campanha, conjunto e anúncios aparecerão aqui.
      </div>
    );
  }

  return (
    <div className="d-flex flex-column gap-3 mt-3">
      <div className="creative-toolbar align-items-start">
        <div>
          <h5 className="mb-1">Campanha Meta publicada</h5>
          <p className="text-muted small mb-0">
            Estrutura e estados persistidos pelo backend. Esta aba é somente de
            leitura e não republica nem altera orçamento.
          </p>
          <p className="text-muted small mb-0 mt-1">
            A jornada do PDE fica em Comportamento PDE; o funil abaixo mostra
            somente etapas já consolidadas no banco central.
          </p>
        </div>
        <span className="badge text-bg-light border">
          {campaigns.length} campanha(s)
        </span>
      </div>

      {campaigns.map((campaign) => (
        <section className="card" key={campaign.id}>
          <div className="card-body">
            <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap">
              <div>
                <h6 className="mb-1">{campaign.name}</h6>
                <div className="text-muted small">
                  {campaign.objective} · criada em{" "}
                  {formatDate(campaign.createdAt)}
                </div>
                <code className="small text-break d-block mt-1">
                  {campaign.id}
                </code>
              </div>
              <span className={`badge ${statusBadgeClass(campaign.status)}`}>
                {campaign.status}
              </span>
            </div>

            {campaign.metricsLastError ? (
              <div className="alert alert-warning small mt-3 mb-0">
                Falha atual na sincronização: {campaign.metricsLastError}
              </div>
            ) : campaign.metricsLastSyncedAt ? (
              <div className="text-muted small mt-2">
                Métricas sincronizadas em{" "}
                {formatDate(campaign.metricsLastSyncedAt)}.
              </div>
            ) : null}

            {campaign.issues.length ? (
              <div className="alert alert-warning small mt-3 mb-0">
                {campaign.issues.join(" ")}
              </div>
            ) : null}

            <div className="d-flex flex-column gap-3 mt-3">
              {campaign.adSets.map((adSet) => (
                <article className="border rounded-3 p-3" key={adSet.id}>
                  <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap">
                    <div>
                      <strong>{adSet.name}</strong>
                      <code className="small text-break d-block">
                        {adSet.id}
                      </code>
                      {adSet.experimentAdSetId ? (
                        <span className="text-muted small">
                          Público #{adSet.experimentAdSetId}
                        </span>
                      ) : null}
                    </div>
                    <span className={`badge ${statusBadgeClass(adSet.status)}`}>
                      {adSet.status}
                    </span>
                  </div>

                  {adSet.issues.length ? (
                    <div className="alert alert-warning small mt-2 mb-0">
                      {adSet.issues.join(" ")}
                    </div>
                  ) : null}

                  {adSet.ads.length ? (
                    <div className="table-responsive mt-3">
                      <table className="table table-sm align-middle mb-0">
                        <thead>
                          <tr>
                            <th>Anúncio</th>
                            <th>Status</th>
                            <th>Rastreamento</th>
                            <th>Funil central</th>
                          </tr>
                        </thead>
                        <tbody>
                          {adSet.ads.map((ad) => (
                            <tr key={ad.id}>
                              <td>
                                <strong className="d-block">{ad.name}</strong>
                                <code className="small text-break">
                                  {ad.id}
                                </code>
                              </td>
                              <td>
                                <span
                                  className={`badge ${statusBadgeClass(ad.status)}`}
                                >
                                  {ad.status}
                                </span>
                              </td>
                              <td>
                                <code className="small text-break">
                                  {ad.trackingCode || "—"}
                                </code>
                              </td>
                              <td>
                                {ad.funnelStages.length ? (
                                  <div className="d-flex flex-column gap-1">
                                    {ad.funnelStages.map((stage) => (
                                      <span
                                        className="small"
                                        key={`${ad.id}-${stage.stage}`}
                                      >
                                        {stage.label}:{" "}
                                        <strong>{stage.totalCount}</strong>
                                      </span>
                                    ))}
                                  </div>
                                ) : (
                                  <span className="text-muted small">
                                    Sem etapa consolidada no funil central
                                  </span>
                                )}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  ) : (
                    <p className="text-muted small mt-3 mb-0">
                      Nenhum anúncio persistido neste conjunto.
                    </p>
                  )}
                </article>
              ))}
            </div>
          </div>
        </section>
      ))}
    </div>
  );
}
