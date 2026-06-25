import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useStartOprmNichoCnaeV3Job } from "../../api/oprm/useStartOprmNichoCnaeV3Job";
import { useOprmNichoCnaeV3Progress } from "../../api/oprm/useOprmNichoCnaeV3Progress";

const v3Stages = [
  {
    code: "cnae-intake",
    title: "Entrada do CNAE",
    activity: "Lendo o CNAE e abrindo a execução.",
  },
  {
    code: "persona-candidate-generator",
    title: "Geração de personas candidatas",
    activity: "Gerando hipóteses de personas vendáveis.",
  },
  {
    code: "persona-tournament",
    title: "Torneio de personas",
    activity: "Comparando e priorizando as melhores personas.",
  },
  {
    code: "routine-query-planner",
    title: "Planejamento de buscas da rotina",
    activity: "Planejando buscas sobre rotina e tarefas reais.",
  },
  {
    code: "source-searcher",
    title: "Busca de fontes",
    activity: "Procurando fontes úteis para entender a rotina.",
  },
  {
    code: "source-fetcher",
    title: "Coleta de fontes",
    activity: "Coletando conteúdos das fontes selecionadas.",
  },
  {
    code: "routine-signal-extractor",
    title: "Extração de sinais da rotina",
    activity: "Extraindo dores, esforço e tarefas recorrentes.",
  },
  {
    code: "daily-tasks-synthesizer",
    title: "Síntese de tarefas diárias",
    activity: "Organizando tarefas diárias em padrões claros.",
  },
  {
    code: "quality-gate",
    title: "Quality gate",
    activity: "Validando qualidade antes de materializar.",
  },
  {
    code: "persona-routine-materializer",
    title: "Materialização de persona e rotina",
    activity: "Montando a persona e a rotina final para uso comercial.",
  },
];

export default function OprmNichoCnaeV3PipelinePage() {
  const { cnaeCode } = useParams();
  const decodedCnaeCode = decodeURIComponent(cnaeCode ?? "");
  const startJob = useStartOprmNichoCnaeV3Job(decodedCnaeCode);
  const progress = useOprmNichoCnaeV3Progress(decodedCnaeCode);
  const stagesByCode = new Map(
    (progress.data?.stages ?? []).map((stage) => [stage.stageCode, stage]),
  );

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
          <div className="d-flex flex-wrap justify-content-between gap-2 align-items-center mb-3">
            <div>
              <h2 className="h5 mb-1">Etapas do pipeline v3</h2>
              <p className="text-muted small mb-0">
                {progress.data?.jobId
                  ? `Acompanhando agora: ${progress.data.jobId}`
                  : "O status fica salvo no backend e será recuperado ao voltar para esta tela."}
              </p>
            </div>
            {progress.isFetching ? (
              <span className="badge rounded-pill text-bg-light border">
                <span
                  className="spinner-border spinner-border-sm me-2"
                  aria-hidden="true"
                />
                Atualizando
              </span>
            ) : null}
          </div>
          <div className="row g-3">
            {v3Stages.map((stage, index) => {
              const stageProgress = stagesByCode.get(stage.code);
              const status = stageProgress?.status ?? "WAITING";
              const isActive = status === "PENDING" || status === "RUNNING";
              const statusLabel =
                {
                  WAITING: "Aguardando",
                  PENDING: "Na fila",
                  RUNNING: "Em execução",
                  COMPLETED: "Concluído",
                  FAILED: "Falhou",
                  CANCELED: "Cancelado",
                }[status] ?? status;
              const statusClass =
                {
                  WAITING: "text-bg-secondary",
                  PENDING: "text-bg-warning",
                  RUNNING: "text-bg-primary",
                  COMPLETED: "text-bg-success",
                  FAILED: "text-bg-danger",
                  CANCELED: "text-bg-secondary",
                }[status] ?? "text-bg-secondary";

              return (
                <div className="col-12 col-md-6 col-xl-4" key={stage.code}>
                  <div
                    className={`border rounded-3 p-3 h-100 ${
                      isActive ? "bg-primary-subtle border-primary" : "bg-light"
                    }`}
                  >
                    <div className="d-flex justify-content-between gap-2 align-items-start mb-2">
                      <span className="badge text-bg-primary">{index + 1}</span>
                      <span className={`badge rounded-pill ${statusClass}`}>
                        {isActive ? (
                          <span
                            className="spinner-border spinner-border-sm me-1"
                            aria-hidden="true"
                          />
                        ) : null}
                        {statusLabel}
                      </span>
                    </div>
                    <h3 className="h6 mb-2">{stage.title}</h3>
                    <p className="small text-muted mb-0">
                      {stageProgress
                        ? stage.activity
                        : "Ainda não chegou nesta etapa."}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </section>
    </div>
  );
}
