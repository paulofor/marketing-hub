import { Link, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";
import PageTitle from "../../components/PageTitle";
import hypothesisIcon from "../../assets/icons/hypothesis-icon.svg";

interface StageExecution {
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

interface StageConfig {
  slug: "pain" | "result" | "mechanism" | "offer";
  number: number;
  title: string;
  startLabel: string;
  startedToast: string;
  startErrorToast: string;
  loadingLabel: string;
  emptyMessage: string;
  description: string;
}

const STAGES: StageConfig[] = [
  {
    slug: "pain",
    number: 1,
    title: "Dor do nicho",
    startLabel: "Iniciar construção da dor",
    startedToast: "Etapa Dor iniciada",
    startErrorToast: "Não foi possível iniciar a etapa Dor",
    loadingLabel: "Carregando execuções de dor...",
    emptyMessage:
      "Nenhuma execução de dor iniciada para este nicho. Clique no botão acima para criar o job e acompanhar o resultado.",
    description:
      "Inicie a construção auditável da dor antes de avançar para resultado, mecanismo, prova e oferta.",
  },
  {
    slug: "result",
    number: 2,
    title: "Resultado desejado",
    startLabel: "Iniciar construção do resultado",
    startedToast: "Etapa Resultado iniciada",
    startErrorToast:
      "Não foi possível iniciar a etapa Resultado. Conclua a dor antes de avançar.",
    loadingLabel: "Carregando execuções de resultado...",
    emptyMessage:
      "Nenhuma execução de resultado iniciada para este nicho. Depois de concluir a dor, clique no botão acima para criar o job e acompanhar o resultado.",
    description:
      "Transforme a dor validada em um resultado claro, desejável e plausível antes de avançar para mecanismo, prova e oferta.",
  },
  {
    slug: "mechanism",
    number: 3,
    title: "Mecanismo",
    startLabel: "Iniciar construção do mecanismo",
    startedToast: "Etapa Mecanismo iniciada",
    startErrorToast:
      "Não foi possível iniciar a etapa Mecanismo. Conclua o resultado antes de avançar.",
    loadingLabel: "Carregando execuções de mecanismo...",
    emptyMessage:
      "Nenhuma execução de mecanismo iniciada para este nicho. Depois de concluir o resultado, clique no botão acima para criar o job e acompanhar o mecanismo.",
    description:
      "Converta o resultado desejado em um mecanismo plausível antes de avançar para prova e oferta.",
  },
  {
    slug: "offer",
    number: 5,
    title: "Oferta",
    startLabel: "Iniciar construção da oferta",
    startedToast: "Etapa Oferta iniciada",
    startErrorToast:
      "Não foi possível iniciar a etapa Oferta. Conclua o mecanismo antes de avançar.",
    loadingLabel: "Carregando execuções de oferta...",
    emptyMessage:
      "Nenhuma execução de oferta iniciada para este nicho. Depois de concluir o mecanismo, clique no botão acima para criar o job e acompanhar a oferta.",
    description:
      "Empacote mecanismo, prova prometida e promessa central em uma oferta clara para venda.",
  },
];

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

function StageCard({
  stage,
  nicheId,
}: {
  stage: StageConfig;
  nicheId?: string;
}) {
  const queryClient = useQueryClient();
  const queryKey = ["hypothesis-stage-executions", stage.slug, nicheId];

  const executionsQuery = useQuery({
    queryKey,
    enabled: Boolean(nicheId),
    queryFn: async () => {
      const { data } = await axios.get<StageExecution[]>(
        `/api/niches/${nicheId}/hypothesis-pipeline/${stage.slug}/stage-executions`,
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
        `/api/niches/${nicheId}/hypothesis-pipeline/${stage.slug}/start`,
      );
      return data;
    },
    onSuccess: () => {
      toast.success(stage.startedToast);
      queryClient.invalidateQueries({ queryKey });
      queryClient.invalidateQueries({
        queryKey: ["hypothesis-stage-total-cost", nicheId],
      });
    },
    onError: () => {
      toast.error(stage.startErrorToast);
    },
  });

  const executions = executionsQuery.data ?? [];
  const latest = executions[0];
  const hasRunningExecution = executions.some((item) =>
    RUNNING_STATUSES.has(item.status),
  );

  return (
    <section className="card mb-4">
      <div className="card-header d-flex flex-column flex-lg-row gap-2 align-items-lg-center justify-content-lg-between">
        <div>
          <h2 className="h5 mb-0">
            Etapa {stage.number} — {stage.title}
          </h2>
          <small className="text-muted">{stage.description}</small>
        </div>
        <button
          type="button"
          className="btn btn-primary align-self-start align-self-lg-center"
          disabled={!nicheId || startMutation.isPending || hasRunningExecution}
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
            stage.startLabel
          )}
        </button>
      </div>
      <div className="card-body">
        {executionsQuery.isLoading ? (
          <p className="text-muted mb-0">{stage.loadingLabel}</p>
        ) : executions.length === 0 ? (
          <div className="alert alert-info mb-0">{stage.emptyMessage}</div>
        ) : (
          <div className="d-flex flex-column gap-3">
            <div className="border rounded p-3 bg-light">
              <div className="d-flex flex-column flex-md-row justify-content-between gap-2">
                <div>
                  <strong>Status atual:</strong> {latest.status}
                  <div className="text-muted small">
                    Job:{" "}
                    <Link
                      to={`/niches/${nicheId}/hypothesis-pipeline/${stage.slug}/stage-executions/${latest.jobid}`}
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
                          to={`/niches/${nicheId}/hypothesis-pipeline/${stage.slug}/stage-executions/${execution.jobid}`}
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
  );
}

export default function NewHypothesisPage() {
  const { nicheId } = useParams();
  const totalCostQuery = useQuery({
    queryKey: ["hypothesis-stage-total-cost", nicheId],
    enabled: Boolean(nicheId),
    queryFn: async () => {
      const responses = await Promise.all(
        STAGES.map((stage) =>
          axios.get<StageExecution[]>(
            `/api/niches/${nicheId}/hypothesis-pipeline/${stage.slug}/stage-executions`,
          ),
        ),
      );
      return responses
        .flatMap((response) => response.data)
        .reduce((total, item) => {
          const cost = parseCostUsd(item.costUsd);
          return cost === null ? total : total + cost;
        }, 0);
    },
  });

  return (
    <div className="hypothesis-new-page">
      <PageTitle icon={hypothesisIcon}>Nova hipótese</PageTitle>

      {nicheId && (
        <section className="card mb-4">
          <div className="card-body d-flex flex-column flex-md-row gap-2 justify-content-between">
            <p className="mb-0">
              <strong>Nicho recebido:</strong> #{nicheId}
            </p>
            <p className="mb-0">
              <strong>Custo total geral da criação da hipótese:</strong>{" "}
              {totalCostQuery.isLoading
                ? "Calculando..."
                : formatCostUsd(totalCostQuery.data ?? 0)}
            </p>
          </div>
        </section>
      )}

      {STAGES.map((stage) => (
        <StageCard key={stage.slug} stage={stage} nicheId={nicheId} />
      ))}

      <section className="card">
        <div className="card-body">
          <p className="text-muted mb-3">
            Depois que dor, resultado, mecanismo e oferta estiverem claros, use
            a hipótese completa para avançar nos experimentos seguindo Dor →
            Resultado → Mecanismo → Prova → Oferta.
          </p>
          <Link className="btn btn-outline-secondary" to="/niches">
            Voltar para nichos
          </Link>
        </div>
      </section>
    </div>
  );
}
