import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import {
  ExperimentAdSetWorkflowDto,
  ExperimentAdSetSpec,
  ExperimentAdSetJob,
  useExperimentAdSetWorkflow,
  useStartExperimentAdSetWorkflow,
} from "../../api/experiment/useExperimentAdSetWorkflow";

const STATUS_VARIANT: Record<string, string> = {
  NOT_STARTED: "secondary",
  RUNNING: "info",
  COMPLETED: "success",
  FAILED: "danger",
};

export default function ExperimentAdSetWorkflowPage() {
  const { id } = useParams();
  const experimentId = id ?? "";
  const { data, isLoading } = useExperimentAdSetWorkflow(experimentId);
  const startWorkflow = useStartExperimentAdSetWorkflow(experimentId);

  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p>Workflow não encontrado.</p>;

  const statusVariant = STATUS_VARIANT[data.status] ?? "secondary";
  const canStart = data.status === "NOT_STARTED";
  const canRestart = data.status === "FAILED" || data.status === "COMPLETED";
  const buttonLabel = canStart ? "Iniciar roteiro" : "Reiniciar roteiro";

  return (
    <div className="container-fluid">
      <div className="d-flex align-items-center justify-content-between mb-4">
        <PageTitle
          title="Playbook de Ad Sets"
          subtitle={
            <span>
              Experimento <Link to={`/experiments/${experimentId}`}>#{experimentId}</Link>
            </span>
          }
        />
        <div className="d-flex gap-2">
          <span className={`badge text-bg-${statusVariant} align-self-center px-3 py-2`}>
            {data.status}
          </span>
          <button
            type="button"
            className="btn btn-primary"
            disabled={startWorkflow.isPending || (!canStart && !canRestart) || data.status === "RUNNING"}
            onClick={() => startWorkflow.mutate(canRestart)}
          >
            {startWorkflow.isPending ? "Processando..." : buttonLabel}
          </button>
        </div>
      </div>

      {data.lastError ? (
        <div className="alert alert-danger" role="alert">
          <strong>Último erro:</strong> {data.lastError}
        </div>
      ) : null}

      <div className="row g-4 mb-4">
        <div className="col-12 col-lg-4">
          <SeedCard workflow={data} />
        </div>
        <div className="col-12 col-lg-8">
          <SpecsCard specs={data.specs} />
        </div>
      </div>

      <JobsCard jobs={data.jobs} />
    </div>
  );
}

function SeedCard({ workflow }: { workflow: ExperimentAdSetWorkflowDto }) {
  return (
    <div className="card h-100">
      <div className="card-header">Seed atual</div>
      <div className="card-body">
        <dl className="row mb-0">
          <dt className="col-sm-5">Palavra-chave</dt>
          <dd className="col-sm-7">{workflow.seedKeyword ?? "—"}</dd>
          <dt className="col-sm-5">Locale</dt>
          <dd className="col-sm-7">{workflow.seedLocale ?? "—"}</dd>
          <dt className="col-sm-5">Interesse</dt>
          <dd className="col-sm-7">{workflow.seedInterestName ?? "—"}</dd>
          <dt className="col-sm-5">Audience (lower)</dt>
          <dd className="col-sm-7">{formatNumber(workflow.seedAudienceLower)}</dd>
          <dt className="col-sm-5">Audience (upper)</dt>
          <dd className="col-sm-7">{formatNumber(workflow.seedAudienceUpper)}</dd>
        </dl>
        {workflow.aiNotes ? (
          <details className="mt-3">
            <summary>Notas da etapa de inteligência</summary>
            <pre className="mt-2 small bg-light p-2 rounded overflow-auto">
              {workflow.aiNotes}
            </pre>
          </details>
        ) : null}
      </div>
    </div>
  );
}

function SpecsCard({ specs }: { specs: ExperimentAdSetSpec[] }) {
  if (!specs?.length) {
    return (
      <div className="card h-100">
        <div className="card-header">Targeting specs</div>
        <div className="card-body text-muted">Ainda não gerado.</div>
      </div>
    );
  }
  return (
    <div className="card h-100">
      <div className="card-header">Targeting specs</div>
      <div className="card-body">
        <div className="row g-3">
          {specs.map((spec) => (
            <div key={spec.id} className="col-12 col-lg-4">
              <div className="border rounded h-100 p-3">
                <div className="d-flex justify-content-between align-items-center mb-2">
                  <strong>{spec.slot}</strong>
                  <span className={`badge text-bg-${statusToVariant(spec.validationStatus)}`}>
                    {spec.validationStatus ?? "PENDENTE"}
                  </span>
                </div>
                <div className="small text-muted mb-2">{spec.label ?? "—"}</div>
                <dl className="row mb-0 small">
                  <dt className="col-6">Idade mínima</dt>
                  <dd className="col-6">{spec.ageMin ?? "—"}</dd>
                  <dt className="col-6">Idade máxima</dt>
                  <dd className="col-6">{spec.ageMax ?? "—"}</dd>
                  <dt className="col-6">Reach</dt>
                  <dd className="col-6">
                    {formatNumber(spec.reachLowerBound)} - {formatNumber(spec.reachUpperBound)}
                  </dd>
                </dl>
                {spec.targetingSpec ? (
                  <details className="mt-2">
                    <summary>Ver JSON</summary>
                    <pre className="small bg-light p-2 rounded mt-1 overflow-auto" style={{ maxHeight: 200 }}>
                      {spec.targetingSpec}
                    </pre>
                  </details>
                ) : null}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function JobsCard({ jobs }: { jobs: ExperimentAdSetJob[] }) {
  if (!jobs?.length) {
    return (
      <div className="card">
        <div className="card-header">Histórico de jobs</div>
        <div className="card-body text-muted">Nenhum job criado ainda.</div>
      </div>
    );
  }
  return (
    <div className="card">
      <div className="card-header">Histórico de jobs</div>
      <div className="table-responsive">
        <table className="table table-sm mb-0">
          <thead>
            <tr>
              <th>Tipo</th>
              <th>Worker</th>
              <th>Status</th>
              <th>Início</th>
              <th>Término</th>
              <th>Erro</th>
              <th>Detalhe</th>
            </tr>
          </thead>
          <tbody>
            {jobs
              .slice()
              .sort((a, b) => (a.id ?? 0) - (b.id ?? 0))
              .map((job) => (
                <tr key={job.id}>
                  <td>{job.type}</td>
                  <td>{job.worker}</td>
                  <td>
                    <span className={`badge text-bg-${statusToVariant(job.status)}`}>{job.status}</span>
                  </td>
                  <td>{formatDate(job.startedAt)}</td>
                  <td>{formatDate(job.finishedAt)}</td>
                  <td className="text-danger">{job.errorMessage ?? ""}</td>
                  <td>
                    <Link to={`jobs/${job.id}`} className="btn btn-link btn-sm px-0">
                      Detalhe
                    </Link>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function formatNumber(value?: number | null) {
  if (value == null) return "—";
  return new Intl.NumberFormat("pt-BR").format(value);
}

function formatDate(value?: string | null) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

function statusToVariant(status?: string | null) {
  switch (status) {
    case "SUCCEEDED":
    case "COMPLETED":
    case "VALID":
      return "success";
    case "FAILED":
    case "INVALID":
      return "danger";
    case "RUNNING":
      return "info";
    default:
      return "secondary";
  }
}
