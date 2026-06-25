import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useStartOprmNichoCnaeV3Job } from "../../api/oprm/useStartOprmNichoCnaeV3Job";

const v3Stages = [
  "Entrada do CNAE",
  "Geração de personas candidatas",
  "Torneio de personas",
  "Planejamento de buscas da rotina",
  "Busca de fontes",
  "Coleta de fontes",
  "Extração de sinais da rotina",
  "Síntese de tarefas diárias",
  "Quality gate",
  "Materialização de persona e rotina",
];

export default function OprmNichoCnaeV3PipelinePage() {
  const { cnaeCode } = useParams();
  const decodedCnaeCode = decodeURIComponent(cnaeCode ?? "");
  const startJob = useStartOprmNichoCnaeV3Job(decodedCnaeCode);

  const handleStart = () => {
    if (!decodedCnaeCode || startJob.isPending) {
      return;
    }
    startJob.mutate();
  };

  return (
    <div className="d-flex flex-column gap-4">
      <div className="d-flex flex-wrap justify-content-between gap-3 align-items-start">
        <div>
          <PageTitle>Pipeline NichoCNAE v3</PageTitle>
          <p className="text-muted mb-1">CNAE {decodedCnaeCode}</p>
          <p className="mb-0">
            Fluxo v3 focado em transformar o CNAE em personas, rotina real e
            tarefas diárias para encontrar dores vendáveis com mais precisão.
          </p>
        </div>
        <div className="d-flex gap-2">
          <Link className="btn btn-outline-secondary" to="/oprm">
            Voltar para CNAEs
          </Link>
          <button
            type="button"
            className="btn btn-primary"
            onClick={handleStart}
            disabled={!decodedCnaeCode || startJob.isPending}
          >
            {startJob.isPending ? (
              <>
                <span
                  className="spinner-border spinner-border-sm me-2"
                  aria-hidden="true"
                />
                Iniciando v3...
              </>
            ) : (
              "Iniciar novo job v3"
            )}
          </button>
        </div>
      </div>

      {startJob.isSuccess ? (
        <div className="alert alert-success" role="status">
          Job v3 iniciado: <strong>{startJob.data.jobId}</strong>. A primeira
          etapa criada foi <strong>{startJob.data.stageCode}</strong> com status{" "}
          <strong>{startJob.data.status}</strong>.
        </div>
      ) : null}

      {startJob.isError ? (
        <div className="alert alert-danger" role="alert">
          {(startJob.error as Error).message}
        </div>
      ) : null}

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <h2 className="h5 mb-3">Etapas do pipeline v3</h2>
          <div className="row g-3">
            {v3Stages.map((stage, index) => (
              <div className="col-12 col-md-6 col-xl-4" key={stage}>
                <div className="border rounded-3 p-3 h-100 bg-light">
                  <span className="badge text-bg-primary mb-2">
                    {index + 1}
                  </span>
                  <h3 className="h6 mb-0">{stage}</h3>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
