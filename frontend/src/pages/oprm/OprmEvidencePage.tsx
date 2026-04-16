import { AlertCircle } from "lucide-react";
import { useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useOprmInsightsWorkspaceData } from "../../api/oprm/useOprmInsightsWorkspaceData";
import OprmModuleNavigation from "./OprmModuleNavigation";

function formatDate(value: unknown): string {
  if (typeof value !== "string") {
    return "-";
  }

  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime())
    ? value
    : parsed.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

export default function OprmEvidencePage() {
  const { occupationSeedRef } = useParams();
  const insightsQuery = useOprmInsightsWorkspaceData(occupationSeedRef);

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <PageTitle>OPRM · Evidências</PageTitle>
        <p className="text-secondary mb-0">
          Audite a linha de geração dos artefatos e as fontes que sustentam a rotina inferida.
        </p>
      </header>

      <OprmModuleNavigation occupationSeedRef={occupationSeedRef} />

      {insightsQuery.isLoading ? (
        <div className="d-flex justify-content-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Carregando evidências do OPRM...</span>
          </div>
        </div>
      ) : null}

      {insightsQuery.isError ? (
        <div className="alert alert-danger d-flex gap-2 mb-0" role="alert">
          <AlertCircle size={18} className="mt-1" aria-hidden="true" />
          <div>
            <strong>Não foi possível carregar as evidências.</strong>
            <p className="mb-0">Verifique a disponibilidade do backend e tente novamente.</p>
          </div>
        </div>
      ) : null}

      {!insightsQuery.isLoading && !insightsQuery.isError ? (
        <>
          <section className="card border-0 shadow-sm">
            <div className="card-body d-flex flex-column gap-3">
              <h2 className="h5 mb-0">Timeline de geração</h2>
              {insightsQuery.data?.timeline?.length ? (
                <div className="table-responsive">
                  <table className="table table-sm align-middle mb-0">
                    <thead>
                      <tr>
                        <th>Artefato</th>
                        <th>Status</th>
                        <th>Correlation ID</th>
                        <th>Gerado em</th>
                      </tr>
                    </thead>
                    <tbody>
                      {insightsQuery.data.timeline.map((artifact) => (
                        <tr key={artifact.artifactId}>
                          <td>{artifact.artifactType}</td>
                          <td><span className="badge text-bg-secondary">{artifact.artifactStatus}</span></td>
                          <td className="font-monospace small">{artifact.correlationId}</td>
                          <td>{formatDate(artifact.createdAt)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="alert alert-secondary mb-0" role="status">Nenhuma evidência publicada para esta ocupação.</div>
              )}
            </div>
          </section>

          <section className="row g-3">
            <div className="col-12 col-xl-6">
              <div className="card border-0 shadow-sm h-100">
                <div className="card-body d-flex flex-column gap-2">
                  <h3 className="h6 mb-0">Fontes estruturadas</h3>
                  {insightsQuery.data?.sources?.length ? (
                    <ul className="list-group list-group-flush">
                      {insightsQuery.data.sources.map((source, index) => (
                        <li className="list-group-item px-0" key={`source-${index}`}>
                          <span className="fw-semibold">{String(source.title ?? source.url ?? `Fonte ${index + 1}`)}</span>
                          <div className="small text-secondary">{String(source.url ?? source.domain ?? "Origem não informada")}</div>
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p className="text-secondary mb-0">Sem fontes explícitas no payload atual.</p>
                  )}
                </div>
              </div>
            </div>

            <div className="col-12 col-xl-6">
              <div className="card border-0 shadow-sm h-100">
                <div className="card-body d-flex flex-column gap-2">
                  <h3 className="h6 mb-0">Excerpts relevantes</h3>
                  {insightsQuery.data?.excerpts?.length ? (
                    <ul className="list-group list-group-flush">
                      {insightsQuery.data.excerpts.map((excerpt, index) => (
                        <li className="list-group-item px-0" key={`excerpt-${index}`}>
                          {String(excerpt.excerpt ?? excerpt.summary ?? excerpt.text ?? `Excerpt ${index + 1}`)}
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p className="text-secondary mb-0">Sem excerpts publicados para a rotina atual.</p>
                  )}
                </div>
              </div>
            </div>
          </section>
        </>
      ) : null}
    </div>
  );
}
