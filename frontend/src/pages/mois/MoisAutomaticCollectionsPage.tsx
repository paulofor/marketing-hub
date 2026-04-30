import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useMoisCollectedReferences, useMoisCollectionJobs } from "../../api/mois/useMoisCollection";

const WORKSPACE_ID = "workspace-001";

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "-"
    : date.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

export default function MoisAutomaticCollectionsPage() {
  const [sourceFilter, setSourceFilter] = useState("");
  const [confidenceFilter, setConfidenceFilter] = useState("");
  const [minSuccessScore, setMinSuccessScore] = useState<number>(0);

  const jobsQuery = useMoisCollectionJobs(WORKSPACE_ID);
  const jobs = jobsQuery.data ?? [];
  const latestJobId = jobs[0]?.jobId ?? "";

  const filters = useMemo(
    () => ({
      source: sourceFilter || undefined,
      minSuccessScore: minSuccessScore > 0 ? minSuccessScore : undefined,
      confidenceLevel: confidenceFilter || undefined,
    }),
    [confidenceFilter, minSuccessScore, sourceFilter],
  );

  const referencesQuery = useMoisCollectedReferences(latestJobId, filters);
  const references = referencesQuery.data ?? [];

  return (
    <section className="d-flex flex-column gap-4">
      <header className="d-flex justify-content-between align-items-center">
        <div>
          <h1 className="h3 mb-1">MOIS · Coletas automáticas</h1>
          <p className="text-secondary mb-0">Visualize todas as coletas automáticas e suas URLs sem criar novo job manual.</p>
        </div>
        <Link className="btn btn-outline-secondary" to="/mois">
          Voltar ao workspace
        </Link>
      </header>

      <article className="card shadow-sm">
        <div className="card-body">
          <div className="d-flex align-items-center justify-content-between mb-3">
            <h2 className="h5 mb-0">Jobs automáticos ({jobs.length})</h2>
            {latestJobId ? <span className="badge text-bg-light">Último job: {latestJobId}</span> : null}
          </div>

          {jobsQuery.isLoading ? <p className="text-secondary mb-0">Carregando jobs automáticos...</p> : null}
          {jobsQuery.isError ? <p className="text-danger mb-0">Não foi possível carregar os jobs automáticos.</p> : null}
          {!jobsQuery.isLoading && !jobsQuery.isError && jobs.length === 0 ? (
            <p className="text-secondary mb-0">Nenhuma coleta automática encontrada para o workspace.</p>
          ) : null}

          {jobs.length > 0 ? (
            <div className="table-responsive">
              <table className="table table-sm align-middle">
                <thead>
                  <tr>
                    <th>Job</th>
                    <th>Status</th>
                    <th>Nicho</th>
                    <th>Tema</th>
                    <th>Fontes</th>
                    <th>Janela</th>
                    <th>Criado em</th>
                  </tr>
                </thead>
                <tbody>
                  {jobs.map((job) => (
                    <tr key={job.jobId}>
                      <td>{job.jobId}</td>
                      <td>
                        <span className="badge text-bg-light">{job.status}</span>
                      </td>
                      <td>{job.niche}</td>
                      <td>{job.marketTheme || "—"}</td>
                      <td>{job.sources.join(", ")}</td>
                      <td>{job.timeWindow}</td>
                      <td>{formatDate(job.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </div>
      </article>

      <article className="card shadow-sm">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h2 className="h5 mb-0">URLs coletadas (último job)</h2>
          </div>

          <div className="row g-2 mb-3">
            <div className="col-md-4">
              <select className="form-select" value={sourceFilter} onChange={(event) => setSourceFilter(event.target.value)}>
                <option value="">Todas as fontes</option>
                <option value="HOTMART">HOTMART</option>
                <option value="CLICKBANK">CLICKBANK</option>
                <option value="JVZOO">JVZOO</option>
                <option value="META_AD_LIBRARY">META_AD_LIBRARY</option>
              </select>
            </div>
            <div className="col-md-4">
              <select
                className="form-select"
                value={confidenceFilter}
                onChange={(event) => setConfidenceFilter(event.target.value)}
              >
                <option value="">Todas as confianças</option>
                <option value="HIGH">HIGH</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="LOW">LOW</option>
              </select>
            </div>
            <div className="col-md-4">
              <input
                type="number"
                min={0}
                max={100}
                className="form-control"
                value={minSuccessScore}
                onChange={(event) => setMinSuccessScore(Number(event.target.value))}
                placeholder="Score mínimo"
              />
            </div>
          </div>

          {!latestJobId ? <p className="text-secondary mb-0">Sem job disponível para carregar URLs.</p> : null}
          {referencesQuery.isLoading ? <p className="text-secondary mb-0">Carregando URLs...</p> : null}
          {referencesQuery.isError ? <p className="text-danger mb-0">Não foi possível carregar URLs coletadas.</p> : null}
          {references.length === 0 && !referencesQuery.isLoading && !referencesQuery.isError && latestJobId ? (
            <p className="text-secondary mb-0">Nenhuma URL encontrada para os filtros atuais.</p>
          ) : null}

          {references.length > 0 ? (
            <div className="table-responsive">
              <table className="table table-sm align-middle">
                <thead>
                  <tr>
                    <th>Título</th>
                    <th>Fonte</th>
                    <th>URL do produto</th>
                    <th>Score</th>
                    <th>Confiança</th>
                    <th>Coletado em</th>
                  </tr>
                </thead>
                <tbody>
                  {references.map((item) => (
                    <tr key={item.referenceId}>
                      <td>{item.title}</td>
                      <td>{item.source}</td>
                      <td>
                        <a href={item.url} target="_blank" rel="noreferrer" className="small text-break d-inline-block">
                          {item.url}
                        </a>
                      </td>
                      <td>{item.successScore}</td>
                      <td>{item.confidenceLevel}</td>
                      <td>{formatDate(item.collectedAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </div>
      </article>
    </section>
  );
}
