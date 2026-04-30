import { Link, useParams } from "react-router-dom";
import { useMoisCollectedReferences, useMoisCollectionJobs } from "../../api/mois/useMoisCollection";

const WORKSPACE_ID = "workspace-001";

export default function MoisCollectionJobDetailPage() {
  const { jobId = "" } = useParams();
  const jobsQuery = useMoisCollectionJobs(WORKSPACE_ID);
  const job = (jobsQuery.data ?? []).find((item) => item.jobId === jobId);
  const referencesQuery = useMoisCollectedReferences(jobId, {});

  const detailPayload = {
    job,
    references: referencesQuery.data ?? [],
  };

  return (
    <section className="d-flex flex-column gap-4">
      <header className="d-flex justify-content-between align-items-center">
        <div>
          <h1 className="h3 mb-1">MOIS · Detalhes do job</h1>
          <p className="text-secondary mb-0">Visualize os dados completos do job em JSON formatado.</p>
        </div>
        <Link className="btn btn-outline-secondary" to="/mois/automatic-collections">
          Voltar para jobs
        </Link>
      </header>

      <article className="card shadow-sm">
        <div className="card-body">
          {jobsQuery.isLoading ? <p className="text-secondary mb-0">Carregando detalhes do job...</p> : null}
          {jobsQuery.isError ? <p className="text-danger mb-0">Não foi possível carregar os detalhes do job.</p> : null}
          {!jobsQuery.isLoading && !jobsQuery.isError && !job ? (
            <p className="text-secondary mb-0">Job não encontrado para o identificador informado.</p>
          ) : null}

          {job ? (
            <pre className="bg-light border rounded p-3 mb-0" style={{ maxHeight: "70vh", overflow: "auto" }}>
              <code>{JSON.stringify(detailPayload, null, 2)}</code>
            </pre>
          ) : null}
        </div>
      </article>
    </section>
  );
}
