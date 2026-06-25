import { FormEvent } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { useMutation, useQueries, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";
import PageTitle from "../../components/PageTitle";
import hypothesisIcon from "../../assets/icons/hypothesis-icon.svg";
import {
  HYPOTHESIS_PIPELINE_STAGES,
  type HypothesisPipelineStageConfig,
} from "./hypothesisPipelineStages";

interface StageExecution {
  jobid: string;
  marketNicheId: number;
  stageCode: string;
  status: string;
  executionRequestedAt?: string;
  processingStartedAt?: string;
  completedAt?: string;
  openAiModel?: string | null;
  costUsd?: number | string | null;
  errorMessage?: string;
}

interface StageExecutionDetail extends StageExecution {
  promptContent?: string | null;
  prompt?: string | null;
  promptMarkdownContent?: string | null;
  openAiRequestBody?: string | null;
  rawResponse?: string | null;
  modelResponse?: string | null;
}

const STAGES = HYPOTHESIS_PIPELINE_STAGES;

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

function formatModel(value?: string | null) {
  return value && value.trim().length > 0 ? value : "Aguardando IA";
}

function safeReportValue(value?: string | null) {
  return value && value.trim().length > 0 ? value : "Não registrado.";
}

function buildHypothesisAuditReport(
  nicheId: string,
  details: Array<{
    stage: HypothesisPipelineStageConfig;
    detail: StageExecutionDetail;
  }>,
) {
  const generatedAt = new Date().toLocaleString("pt-BR");
  const lines = [
    `# Relatório auditável da criação da hipótese`,
    ``,
    `Nicho: #${nicheId}`,
    `Gerado em: ${generatedAt}`,
    ``,
  ];

  details.forEach(({ stage, detail }) => {
    lines.push(
      `## Etapa ${stage.number} — ${stage.title}`,
      ``,
      `- Job: ${detail.jobid}`,
      `- Status: ${detail.status}`,
      `- Modelo usado: ${formatModel(detail.openAiModel)}`,
      `- Custo: ${formatCostUsd(detail.costUsd)}`,
      ``,
      `### Prompt usado`,
      "```text",
      safeReportValue(detail.prompt ?? detail.promptContent),
      "```",
      ``,
      `### Request cru enviado para OpenAI`,
      "```json",
      safeReportValue(detail.openAiRequestBody),
      "```",
      ``,
      `### Response cru recebido da OpenAI`,
      "```json",
      safeReportValue(detail.rawResponse ?? detail.modelResponse),
      "```",
      ``,
      `### Informação final guardada no banco de dados`,
      "```json",
      safeReportValue(detail.modelResponse),
      "```",
      ``,
    );
  });

  return lines.join("\n");
}

function downloadTextFile(filename: string, content: string) {
  const blob = new Blob([content], { type: "text/markdown;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

function StageCard({
  stage,
  nicheId,
  executionsQuery,
  blockedMessage,
  readOnly = false,
}: {
  stage: HypothesisPipelineStageConfig;
  nicheId?: string;
  executionsQuery: {
    data?: StageExecution[];
    isLoading: boolean;
  };
  blockedMessage?: string;
  readOnly?: boolean;
}) {
  const queryClient = useQueryClient();
  const queryKey = ["hypothesis-stage-executions", stage.slug, nicheId];

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
  const isBlockedByPreviousStage = Boolean(blockedMessage);
  const isStartDisabled =
    readOnly ||
    !nicheId ||
    startMutation.isPending ||
    hasRunningExecution ||
    isBlockedByPreviousStage;

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
          disabled={isStartDisabled}
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
          ) : readOnly ? (
            "Execuções da hipótese"
          ) : hasRunningExecution ? (
            "Execução em andamento"
          ) : (
            stage.startLabel
          )}
        </button>
      </div>
      <div className="card-body">
        {blockedMessage && (
          <div className="alert alert-warning py-2 mb-3" role="alert">
            {blockedMessage}
          </div>
        )}
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
                    Modelo usado: {formatModel(latest.openAiModel)}
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
                    <th>Modelo usado</th>
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
                      <td>{formatModel(execution.openAiModel)}</td>
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

function stageIsCompleted(executions?: StageExecution[]) {
  return executions?.[0]?.status === "CONCLUIDO";
}

export default function NewHypothesisPage() {
  const { nicheId } = useParams();
  const [searchParams] = useSearchParams();
  const hypothesisId = searchParams.get("hypothesisId")?.trim() || undefined;
  const queryClient = useQueryClient();
  const stageQueries = useQueries({
    queries: STAGES.map((stage) => ({
      queryKey: [
        "hypothesis-stage-executions",
        stage.slug,
        nicheId,
        hypothesisId,
      ],
      enabled: Boolean(nicheId),
      queryFn: async () => {
        const { data } = await axios.get<StageExecution[]>(
          `/api/niches/${nicheId}/hypothesis-pipeline/${stage.slug}/stage-executions`,
          { params: hypothesisId ? { hypothesisId } : undefined },
        );
        return data;
      },
      refetchInterval: 5000,
    })),
  });
  const totalCostLoading = stageQueries.some((query) => query.isLoading);
  const totalCost = stageQueries
    .flatMap((query) => query.data ?? [])
    .reduce((total, item) => {
      const cost = parseCostUsd(item.costUsd);
      return cost === null ? total : total + cost;
    }, 0);
  const hasRunningExecution = stageQueries.some((query) =>
    (query.data ?? []).some((item) => RUNNING_STATUSES.has(item.status)),
  );
  const allStagesCompleted = STAGES.every((_, index) =>
    stageIsCompleted(stageQueries[index]?.data),
  );

  const fullFlowMutation = useMutation({
    mutationFn: async () => {
      const { data } = await axios.post(
        `/api/niches/${nicheId}/hypothesis-pipeline/full-flow/start`,
      );
      return data;
    },
    onSuccess: () => {
      toast.success("Fluxo completo da hipótese iniciado");
      STAGES.forEach((stage) => {
        queryClient.invalidateQueries({
          queryKey: [
            "hypothesis-stage-executions",
            stage.slug,
            nicheId,
            hypothesisId,
          ],
        });
      });
      queryClient.invalidateQueries({
        queryKey: ["hypothesis-stage-total-cost", nicheId],
      });
    },
    onError: () => {
      toast.error("Não foi possível iniciar o fluxo completo da hipótese");
    },
  });

  const finalizeMutation = useMutation({
    mutationFn: async () => {
      const { data } = await axios.post(
        `/api/niches/${nicheId}/hypothesis-pipeline/finalize`,
        { name: "" },
      );
      return data;
    },
    onSuccess: () => {
      toast.success("Hipótese fechada e disponível para gerar experimento.");
      queryClient.invalidateQueries({
        queryKey: ["niche-hypotheses", nicheId, "BACKLOG"],
      });
    },
    onError: () => {
      toast.error("Não foi possível fechar a hipótese.");
    },
  });

  function handleFinalizeSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    finalizeMutation.mutate();
  }

  const reportMutation = useMutation({
    mutationFn: async () => {
      if (!nicheId) {
        throw new Error("Nicho não informado.");
      }
      const completedStages = STAGES.map((stage, index) => ({
        stage,
        latest: stageQueries[index]?.data?.[0],
      })).filter(({ latest }) => latest?.status === "CONCLUIDO");

      if (completedStages.length === 0) {
        throw new Error("Nenhuma etapa concluída para gerar relatório.");
      }

      const details = await Promise.all(
        completedStages.map(async ({ stage, latest }) => {
          const { data } = await axios.get<StageExecutionDetail>(
            `/api/niches/${nicheId}/hypothesis-pipeline/${stage.slug}/stage-executions/${latest?.jobid}`,
          );
          return { stage, detail: data };
        }),
      );

      return buildHypothesisAuditReport(nicheId, details);
    },
    onSuccess: (content) => {
      downloadTextFile(`relatorio-hipotese-nicho-${nicheId}.md`, content);
      toast.success("Relatório gerado para download.");
    },
    onError: (error) => {
      const message =
        error instanceof Error ? error.message : "Falha ao gerar relatório.";
      toast.error(message);
    },
  });

  return (
    <div className="hypothesis-new-page">
      <PageTitle icon={hypothesisIcon}>
        {hypothesisId ? "Execuções da hipótese" : "Nova hipótese"}
      </PageTitle>

      {nicheId && (
        <section className="card mb-4">
          <div className="card-body d-flex flex-column flex-md-row gap-2 justify-content-between">
            <p className="mb-0">
              <strong>Nicho recebido:</strong> #{nicheId}
              {hypothesisId && (
                <span className="ms-2 text-muted">
                  Hipótese: {hypothesisId}
                </span>
              )}
            </p>
            <div className="d-flex flex-column flex-md-row gap-2 align-items-md-center">
              <p className="mb-0">
                <strong>Custo total geral da criação da hipótese:</strong>{" "}
                {totalCostLoading ? "Calculando..." : formatCostUsd(totalCost)}
              </p>
              <button
                type="button"
                className="btn btn-primary btn-sm"
                disabled={
                  Boolean(hypothesisId) ||
                  !nicheId ||
                  fullFlowMutation.isPending ||
                  hasRunningExecution ||
                  allStagesCompleted
                }
                onClick={() => fullFlowMutation.mutate()}
              >
                {fullFlowMutation.isPending ? (
                  <span className="d-inline-flex align-items-center gap-2">
                    <span
                      className="spinner-border spinner-border-sm"
                      aria-hidden="true"
                    />
                    Iniciando fluxo...
                  </span>
                ) : hasRunningExecution ? (
                  "Fluxo em execução"
                ) : allStagesCompleted ? (
                  "Fluxo concluído"
                ) : (
                  "Gerar fluxo completo"
                )}
              </button>
              <button
                type="button"
                className="btn btn-outline-primary btn-sm"
                disabled={!nicheId || reportMutation.isPending}
                onClick={() => reportMutation.mutate()}
              >
                {reportMutation.isPending ? (
                  <span className="d-inline-flex align-items-center gap-2">
                    <span
                      className="spinner-border spinner-border-sm"
                      aria-hidden="true"
                    />
                    Gerando...
                  </span>
                ) : (
                  "Baixar relatório auditável"
                )}
              </button>
            </div>
          </div>
        </section>
      )}

      {STAGES.map((stage, index) => {
        const previousStagesCompleted = STAGES.slice(0, index).every(
          (_, previousIndex) =>
            stageIsCompleted(stageQueries[previousIndex]?.data),
        );
        const blockedMessage = previousStagesCompleted
          ? undefined
          : stage.blockedMessage;

        return (
          <StageCard
            key={stage.slug}
            stage={stage}
            nicheId={nicheId}
            executionsQuery={stageQueries[index]}
            blockedMessage={blockedMessage}
            readOnly={Boolean(hypothesisId)}
          />
        );
      })}

      <section className="card">
        <div className="card-body">
          <p className="text-muted mb-3">
            Depois que dor, resultado, mecanismo, prova e oferta estiverem
            claros, dê um nome para fechar a hipótese. Ela entrará no backlog e
            ficará disponível na tela de criação de experimento.
          </p>
          <form
            className="row g-2 align-items-end mb-3"
            onSubmit={handleFinalizeSubmit}
          >
            <div className="col-12 col-lg-8">
              <div className="alert alert-info mb-0">
                O backend vai nomear automaticamente com a sigla do nicho e a
                próxima numeração da hipótese.
              </div>
            </div>
            <div className="col-12 col-lg-4">
              <button
                type="submit"
                className="btn btn-success w-100"
                disabled={
                  Boolean(hypothesisId) ||
                  !allStagesCompleted ||
                  finalizeMutation.isPending
                }
              >
                {finalizeMutation.isPending ? (
                  <span className="d-inline-flex align-items-center gap-2">
                    <span
                      className="spinner-border spinner-border-sm"
                      aria-hidden="true"
                    />
                    Fechando...
                  </span>
                ) : (
                  "Fechar hipótese"
                )}
              </button>
            </div>
            {!hypothesisId && !allStagesCompleted && (
              <div className="col-12">
                <small className="text-muted">
                  Conclua todas as cinco etapas para liberar o fechamento da
                  hipótese.
                </small>
              </div>
            )}
          </form>
          <div className="d-flex flex-wrap gap-2">
            <Link
              className="btn btn-primary"
              to={`/niches/${nicheId}/hypothesis-pipeline/summary`}
            >
              Resumo do framework
            </Link>
            <Link className="btn btn-outline-secondary" to="/niches">
              Voltar para nichos
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
