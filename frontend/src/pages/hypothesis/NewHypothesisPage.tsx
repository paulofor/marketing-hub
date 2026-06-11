import { Link, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";
import PageTitle from "../../components/PageTitle";
import hypothesisIcon from "../../assets/icons/hypothesis-icon.svg";

interface PainExecution {
  jobid: string;
  marketNicheId: number;
  stageCode: string;
  status: string;
  executionRequestedAt?: string;
  processingStartedAt?: string;
  completedAt?: string;
  costUsd?: number | string | null;
  errorMessage?: string;
}

const RUNNING_STATUSES = new Set([
  "INICIADO",
  "PROCESSANDO",
  "AGUARDANDO_RETORNO_OPENAI",
]);

function formatDate(value?: string) {
  if (!value) return "—";
  return new Date(value).toLocaleString("pt-BR");
}

function parseCostUsd(value?: number | string | null) {
  if (value === null || value === undefined || value === "") return null;
  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? numericValue : null;
}

function formatCostUsd(value?: number | string | null) {
  const numericValue = parseCostUsd(value);
  if (numericValue === null) return "—";
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 4,
    maximumFractionDigits: 6,
  }).format(numericValue);
}

export default function NewHypothesisPage() {
  const { nicheId } = useParams();
  const queryClient = useQueryClient();
  const queryKey = ["hypothesis-pain-stage-executions", nicheId];

  const executionsQuery = useQuery({
    queryKey,
    enabled: Boolean(nicheId),
    queryFn: async () => {
      const { data } = await axios.get<PainExecution[]>(
        `/api/niches/${nicheId}/hypothesis-pipeline/pain/stage-executions`,
      );
      return data;
    },
    refetchInterval: (query) => {
      const items = query.state.data ?? [];
      return items.some((item) => RUNNING_STATUSES.has(item.status))
        ? 5000
        : false;
    },
  });

  const startMutation = useMutation({
    mutationFn: async () => {
      const { data } = await axios.post(
        `/api/niches/${nicheId}/hypothesis-pipeline/pain/start`,
      );
      return data;
    },
    onSuccess: () => {
      toast.success("Etapa Dor iniciada");
      queryClient.invalidateQueries({ queryKey });
    },
    onError: () => {
      toast.error("Não foi possível iniciar a etapa Dor");
    },
  });

  const executions = executionsQuery.data ?? [];
  const latest = executions[0];
  const totalCreationCostUsd = executions.reduce((total, item) => {
    const cost = parseCostUsd(item.costUsd);
    return cost === null ? total : total + cost;
  }, 0);
  const hasRunningExecution = executions.some((item) =>
    RUNNING_STATUSES.has(item.status),
  );

  return (
    <div className="hypothesis-new-page">
      <PageTitle icon={hypothesisIcon}>Nova hipótese</PageTitle>

      <section className="card mb-4">
        <div className="card-header d-flex flex-column flex-lg-row gap-2 align-items-lg-center justify-content-lg-between">
          <div>
            <h2 className="h5 mb-0">Etapa 1 — Dor do nicho</h2>
            <small className="text-muted">
              Inicie a construção auditável da dor antes de avançar para
              resultado, mecanismo, prova e oferta.
            </small>
          </div>
          <button
            type="button"
            className="btn btn-primary align-self-start align-self-lg-center"
            disabled={
              !nicheId || startMutation.isPending || hasRunningExecution
            }
            onClick={() => startMutation.mutate()}
          >
            {startMutation.isPending ? (
              <span className="d-inline-flex align-items-center gap-2">
                <span
                  className="spinner-border spinner-border-sm"
                  aria-hidden="true"
                />
                Iniciando...
              </span>
            ) : hasRunningExecution ? (
              "Execução em andamento"
            ) : (
              "Iniciar construção da dor"
            )}
          </button>
        </div>
        <div className="card-body">
          {nicheId && (
            <div className="d-flex flex-column flex-md-row gap-2 justify-content-between mb-3">
              <p className="mb-0">
                <strong>Nicho recebido:</strong> #{nicheId}
              </p>
              <p className="mb-0">
                <strong>Custo total geral da criação da hipótese:</strong>{" "}
                {formatCostUsd(totalCreationCostUsd)}
              </p>
            </div>
          )}

          {executionsQuery.isLoading ? (
            <p className="text-muted mb-0">Carregando execuções...</p>
          ) : executions.length === 0 ? (
            <div className="alert alert-info mb-0">
              Nenhuma execução de dor iniciada para este nicho. Clique no botão
              acima para criar o job e acompanhar o resultado.
            </div>
          ) : (
            <div className="d-flex flex-column gap-3">
              <div className="border rounded p-3 bg-light">
                <div className="d-flex flex-column flex-md-row justify-content-between gap-2">
                  <div>
                    <strong>Status atual:</strong> {latest.status}
                    <div className="text-muted small">
                      Job:{" "}
                      <Link
                        to={`/niches/${nicheId}/hypothesis-pipeline/pain/stage-executions/${latest.jobid}`}
                      >
                        {latest.jobid}
                      </Link>
                    </div>
                    <div className="text-muted small">
                      Custo da última execução: {formatCostUsd(latest.costUsd)}
                    </div>
                  </div>
                  <div className="text-muted small text-md-end">
                    Solicitado em {formatDate(latest.executionRequestedAt)}
                    <br />
                    Concluído em {formatDate(latest.completedAt)}
                  </div>
                </div>
                {latest.errorMessage && (
                  <div className="alert alert-danger mt-3 mb-0">
                    {latest.errorMessage}
                  </div>
                )}
              </div>

              <div className="table-responsive">
                <table className="table table-sm align-middle mb-0">
                  <thead>
                    <tr>
                      <th>Execução</th>
                      <th>Status</th>
                      <th>Solicitado em</th>
                      <th>Concluído em</th>
                      <th className="text-end">Custo da execução</th>
                    </tr>
                  </thead>
                  <tbody>
                    {executions.map((execution) => (
                      <tr key={execution.jobid}>
                        <td>
                          <Link
                            to={`/niches/${nicheId}/hypothesis-pipeline/pain/stage-executions/${execution.jobid}`}
                          >
                            {execution.jobid}
                          </Link>
                        </td>
                        <td>{execution.status}</td>
                        <td>{formatDate(execution.executionRequestedAt)}</td>
                        <td>{formatDate(execution.completedAt)}</td>
                        <td className="text-end">
                          {formatCostUsd(execution.costUsd)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      </section>

      <section className="card">
        <div className="card-body">
          <p className="text-muted mb-3">
            Depois que a dor estiver clara, avance para construir a hipótese
            completa seguindo Dor → Resultado → Mecanismo → Prova → Oferta.
          </p>
          <Link className="btn btn-outline-secondary" to="/niches">
            Voltar para nichos
          </Link>
        </div>
      </section>
    </div>
  );
}
