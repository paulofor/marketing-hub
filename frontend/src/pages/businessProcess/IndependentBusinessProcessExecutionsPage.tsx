import axios from "axios";
import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  AlertCircle,
  CheckCircle2,
  Clock3,
  Play,
  RefreshCw,
} from "lucide-react";
import { toast } from "react-toastify";
import {
  useIndependentBusinessProcessCatalog,
  useIndependentBusinessProcessExecution,
  useIndependentBusinessProcessExecutions,
  useStartIndependentBusinessProcessExecution,
} from "../../api/businessProcess/useIndependentBusinessProcessExecutions";
import type {
  IndependentBusinessProcessExecutionSummary,
  IndependentBusinessProcessInputField,
} from "../../api/businessProcess/types";
import PageTitle from "../../components/PageTitle";
import ArgosMetaSupervisedSession from "../productDiscovery/ArgosMetaSupervisedSession";
import "./IndependentBusinessProcessExecutionsPage.css";

const statusLabels: Record<string, string> = {
  NOT_STARTED: "Não iniciada",
  PENDING: "Na fila",
  IN_PROGRESS: "Em execução",
  BLOCKED: "Bloqueada",
  COMPLETED: "Concluída",
  CANCELLED: "Cancelada",
};

function createRequestKey() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID();
  const random =
    `${Date.now().toString(16)}${Math.random().toString(16).slice(2)}`
      .padEnd(32, "0")
      .slice(0, 32);
  return `${random.slice(0, 8)}-${random.slice(8, 12)}-4${random.slice(13, 16)}-8${random.slice(17, 20)}-${random.slice(20)}`;
}

function statusClass(status: string) {
  if (status === "COMPLETED") return "is-completed";
  if (status === "BLOCKED") return "is-blocked";
  if (status === "IN_PROGRESS") return "is-running";
  return "is-pending";
}

function formatDate(value?: string) {
  return value
    ? new Intl.DateTimeFormat("pt-BR", {
        dateStyle: "short",
        timeStyle: "short",
      }).format(new Date(value))
    : "Ainda não registrado";
}

function formatCost(value?: number, coverage?: string) {
  if (value === undefined || coverage === "NOT_REPORTED") {
    return "Custo não informado";
  }
  return `${new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 4,
  }).format(value)}${coverage === "PARTIAL" ? " (parcial)" : ""}`;
}

function requestError(error: unknown) {
  if (!axios.isAxiosError(error)) return "Não foi possível iniciar o processo.";
  const data = error.response?.data as
    { detail?: string; message?: string; error?: string } | undefined;
  return (
    data?.detail ??
    data?.message ??
    data?.error ??
    "Não foi possível iniciar o processo."
  );
}

function defaultInputs(fields: IndependentBusinessProcessInputField[]) {
  return Object.fromEntries(
    fields.map((field) => [field.key, field.defaultValue ?? ""]),
  );
}

export default function IndependentBusinessProcessExecutionsPage() {
  const catalogQuery = useIndependentBusinessProcessCatalog();
  const executionsQuery = useIndependentBusinessProcessExecutions();
  const start = useStartIndependentBusinessProcessExecution();
  const [selectedProcessId, setSelectedProcessId] = useState<number>();
  const [selectedExecutionId, setSelectedExecutionId] = useState<number>();
  const [requestedByName, setRequestedByName] = useState("Marketing Hub");
  const [input, setInput] = useState<Record<string, string>>({});
  const [requestKey, setRequestKey] = useState(createRequestKey);
  const catalog = catalogQuery.data ?? [];
  const executions = executionsQuery.data ?? [];
  const selectedProcess = useMemo(
    () =>
      catalog.find((item) => item.processDefinitionId === selectedProcessId),
    [catalog, selectedProcessId],
  );
  const detailQuery =
    useIndependentBusinessProcessExecution(selectedExecutionId);

  useEffect(() => {
    if (selectedProcessId !== undefined || catalog.length === 0) return;
    const first = catalog.find((item) => item.executionAvailable) ?? catalog[0];
    setSelectedProcessId(first.processDefinitionId);
  }, [catalog, selectedProcessId]);

  useEffect(() => {
    if (!selectedProcess) return;
    setInput(defaultInputs(selectedProcess.inputFields));
    setRequestKey(createRequestKey());
  }, [selectedProcess]);

  useEffect(() => {
    if (selectedExecutionId !== undefined || executions.length === 0) return;
    setSelectedExecutionId(executions[0].id);
  }, [executions, selectedExecutionId]);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!selectedProcess) return;
    try {
      const result = await start.mutateAsync({
        requestKey,
        processDefinitionId: selectedProcess.processDefinitionId,
        requestedByName,
        input: Object.fromEntries(
          Object.entries(input).filter(([, value]) => value.trim() !== ""),
        ),
      });
      setSelectedExecutionId(result.execution.id);
      setRequestKey(createRequestKey());
      toast.success(
        "Processo iniciado e tarefa encaminhada ao agente responsável.",
      );
    } catch (error) {
      toast.error(requestError(error));
    }
  };

  return (
    <div className="independent-process-page">
      <header className="independent-process-hero">
        <div>
          <span className="independent-process-eyebrow">
            Operação pré-produto
          </span>
          <PageTitle>Executar processos independentes</PageTitle>
          <p>
            Inicie pesquisas e rotinas que ainda não pertencem a um produto. O
            backend encaminha o trabalho ao agente correto e registra toda a
            execução.
          </p>
        </div>
        <div className="independent-process-hero__principle">
          <strong>Evidência antes do produto</strong>
          <span>
            Nenhum produto ou experimento fictício é criado para disparar o
            fluxo.
          </span>
        </div>
      </header>

      {catalogQuery.isError ? (
        <div className="alert alert-danger" role="alert">
          Não foi possível carregar os processos disponíveis.
        </div>
      ) : null}

      <section
        className="independent-process-catalog"
        aria-label="Processos disponíveis"
      >
        {catalogQuery.isLoading ? <p>Carregando processos...</p> : null}
        {catalog.map((process) => (
          <button
            type="button"
            key={process.processDefinitionId}
            className={`independent-process-catalog__item ${
              process.processDefinitionId === selectedProcessId
                ? "is-selected"
                : ""
            }`}
            onClick={() => setSelectedProcessId(process.processDefinitionId)}
          >
            <span className="independent-process-catalog__status">
              {process.executionAvailable ? "Disponível" : "Indisponível"}
            </span>
            <strong>{process.name}</strong>
            <span>
              v{process.versionNumber} · {process.ownerName}
            </span>
          </button>
        ))}
        {!catalogQuery.isLoading && catalog.length === 0 ? (
          <div className="alert alert-info mb-0">
            Nenhum processo publicado declarou execução independente.
          </div>
        ) : null}
      </section>

      {selectedProcess ? (
        <section className="independent-process-workspace">
          <form className="card independent-process-form" onSubmit={submit}>
            <div className="card-body">
              <div className="independent-process-section-heading">
                <div>
                  <span>Nova execução</span>
                  <h2>{selectedProcess.name}</h2>
                </div>
                <Play size={24} aria-hidden="true" />
              </div>
              <p className="independent-process-purpose">
                {selectedProcess.purpose}
              </p>
              <dl className="independent-process-contract">
                <div>
                  <dt>Início</dt>
                  <dd>{selectedProcess.triggerDescription}</dd>
                </div>
                <div>
                  <dt>Entrega esperada</dt>
                  <dd>{selectedProcess.outcomeDescription}</dd>
                </div>
              </dl>

              <div className="mb-3">
                <label
                  className="form-label"
                  htmlFor="independent-requested-by"
                >
                  Solicitante *
                </label>
                <input
                  id="independent-requested-by"
                  className="form-control"
                  required
                  maxLength={100}
                  value={requestedByName}
                  onChange={(event) => setRequestedByName(event.target.value)}
                />
              </div>

              <div className="independent-process-fields">
                {selectedProcess.inputFields.map((field) => {
                  const controlId = `independent-field-${field.key}`;
                  return (
                    <div className="independent-process-field" key={field.key}>
                      <label className="form-label" htmlFor={controlId}>
                        {field.label}
                        {field.required ? " *" : ""}
                      </label>
                      {field.controlType === "TEXTAREA" ? (
                        <textarea
                          id={controlId}
                          className="form-control"
                          rows={3}
                          required={field.required}
                          maxLength={field.maxLength}
                          value={input[field.key] ?? ""}
                          onChange={(event) =>
                            setInput({
                              ...input,
                              [field.key]: event.target.value,
                            })
                          }
                        />
                      ) : field.controlType === "SELECT" ? (
                        <select
                          id={controlId}
                          className="form-select"
                          required={field.required}
                          value={input[field.key] ?? ""}
                          onChange={(event) =>
                            setInput({
                              ...input,
                              [field.key]: event.target.value,
                            })
                          }
                        >
                          {!field.required ? (
                            <option value="">Não informado</option>
                          ) : null}
                          {(field.options ?? []).map((option) => (
                            <option key={option.value} value={option.value}>
                              {option.label}
                            </option>
                          ))}
                        </select>
                      ) : (
                        <input
                          id={controlId}
                          className="form-control"
                          required={field.required}
                          maxLength={field.maxLength}
                          value={input[field.key] ?? ""}
                          onChange={(event) =>
                            setInput({
                              ...input,
                              [field.key]: event.target.value,
                            })
                          }
                        />
                      )}
                      {field.helpText ? (
                        <div className="form-text">{field.helpText}</div>
                      ) : null}
                    </div>
                  );
                })}
              </div>

              {!selectedProcess.executionAvailable ? (
                <div className="alert alert-warning mt-3 mb-0" role="alert">
                  {selectedProcess.executionAvailabilityReason}
                </div>
              ) : null}
              <button
                className="btn btn-primary independent-process-submit"
                disabled={
                  !selectedProcess.executionAvailable || start.isPending
                }
              >
                {start.isPending ? (
                  <>
                    <RefreshCw className="independent-process-spin" size={18} />
                    Iniciando...
                  </>
                ) : (
                  <>
                    <Play size={18} />
                    Iniciar processo
                  </>
                )}
              </button>
            </div>
          </form>

          <aside className="card independent-process-history">
            <div className="card-body">
              <div className="independent-process-section-heading">
                <div>
                  <span>Acompanhamento</span>
                  <h2>Execuções recentes</h2>
                </div>
                <Clock3 size={24} aria-hidden="true" />
              </div>
              {executionsQuery.isLoading ? (
                <p>Carregando histórico...</p>
              ) : null}
              {executionsQuery.isError ? (
                <div className="alert alert-danger">
                  Falha ao carregar o histórico.
                </div>
              ) : null}
              <div className="independent-process-history__list">
                {executions.map((execution) => (
                  <button
                    type="button"
                    key={execution.id}
                    className={`independent-process-history__item ${
                      execution.id === selectedExecutionId ? "is-selected" : ""
                    }`}
                    onClick={() => setSelectedExecutionId(execution.id)}
                  >
                    <span
                      className={`independent-process-status ${statusClass(execution.status)}`}
                    >
                      {statusLabels[execution.status] ?? execution.status}
                    </span>
                    <strong>
                      #{execution.id} · {execution.displayName}
                    </strong>
                    <span>
                      {execution.processName} ·{" "}
                      {formatDate(execution.createdAt)}
                    </span>
                    <small>
                      {execution.completedActivityCount}/
                      {execution.activityCount} atividades
                    </small>
                  </button>
                ))}
                {!executionsQuery.isLoading && executions.length === 0 ? (
                  <p className="text-body-secondary mb-0">
                    Nenhuma execução iniciada por esta tela.
                  </p>
                ) : null}
              </div>
            </div>
          </aside>
        </section>
      ) : null}

      {selectedExecutionId !== undefined ? (
        <ExecutionDetail
          loading={detailQuery.isLoading}
          error={detailQuery.isError}
          detail={detailQuery.data}
        />
      ) : null}
    </div>
  );
}

type ExecutionDetailProps = {
  loading: boolean;
  error: boolean;
  detail?: ReturnType<typeof useIndependentBusinessProcessExecution>["data"];
};

function ExecutionDetail({ loading, error, detail }: ExecutionDetailProps) {
  if (loading)
    return <div className="card card-body">Carregando execução...</div>;
  if (error || !detail) {
    return (
      <div className="alert alert-danger">
        Não foi possível detalhar a execução.
      </div>
    );
  }
  const execution: IndependentBusinessProcessExecutionSummary =
    detail.execution;
  const supervisedMetaCycleId = findSupervisedMetaCycleId(detail);
  return (
    <>
      <section className="card independent-process-detail">
        <div className="card-body">
          <div className="independent-process-detail__header">
            <div>
              <span
                className={`independent-process-status ${statusClass(execution.status)}`}
              >
                {statusLabels[execution.status] ?? execution.status}
              </span>
              <h2>
                Execução #{execution.id} · {execution.displayName}
              </h2>
              <p>{execution.sourceReference}</p>
            </div>
            {execution.status === "COMPLETED" ? (
              <CheckCircle2
                className="text-success"
                size={32}
                aria-label="Execução concluída"
              />
            ) : execution.status === "BLOCKED" ? (
              <AlertCircle
                className="text-danger"
                size={32}
                aria-label="Execução bloqueada"
              />
            ) : (
              <Clock3
                className="text-primary"
                size={32}
                aria-label="Execução em andamento"
              />
            )}
          </div>

          {execution.latestError ? (
            <div className="alert alert-danger">
              <strong>Causa registrada:</strong> {execution.latestError}
            </div>
          ) : null}

          <div className="independent-process-metrics">
            <div>
              <span>Progresso</span>
              <strong>
                {execution.completedActivityCount}/{execution.activityCount}
              </strong>
            </div>
            <div>
              <span>Início</span>
              <strong>
                {formatDate(execution.startedAt ?? execution.createdAt)}
              </strong>
            </div>
            <div>
              <span>Tokens</span>
              <strong>
                {execution.inputTokens === undefined &&
                execution.outputTokens === undefined
                  ? "Não informado"
                  : (execution.inputTokens ?? 0) +
                    (execution.outputTokens ?? 0)}
              </strong>
            </div>
            <div>
              <span>Custo do modelo</span>
              <strong>
                {formatCost(execution.estimatedCostUsd, execution.costCoverage)}
              </strong>
            </div>
          </div>

          <details className="independent-process-input">
            <summary>Ver entrada enviada</summary>
            <dl>
              {Object.entries(execution.input).map(([key, value]) => (
                <div key={key}>
                  <dt>{key}</dt>
                  <dd>{value}</dd>
                </div>
              ))}
            </dl>
          </details>

          <div className="independent-process-activities">
            {detail.activities.map((activity) => (
              <article
                key={activity.activityId}
                className="independent-process-activity"
              >
                <header>
                  <div>
                    <span>Atividade</span>
                    <h3>{activity.activityName}</h3>
                  </div>
                  <span
                    className={`independent-process-status ${statusClass(activity.status)}`}
                  >
                    {statusLabels[activity.status] ?? activity.status}
                  </span>
                </header>
                {activity.tasks.map((task) => (
                  <details
                    key={task.taskId}
                    className="independent-process-attempt"
                  >
                    <summary>
                      Tarefa #{task.taskId} · {task.assignedAgentNickname} ·{" "}
                      {statusLabels[task.status] ?? task.status}
                    </summary>
                    <div className="independent-process-attempt__body">
                      <p>
                        <strong>Modelo:</strong>{" "}
                        {task.modelCode ?? "Não informado"}
                      </p>
                      <p>
                        <strong>Iniciada:</strong> {formatDate(task.startedAt)}
                      </p>
                      {task.executionError ? (
                        <p className="text-danger">
                          <strong>Erro:</strong> {task.executionError}
                        </p>
                      ) : null}
                      {task.result !== undefined ? (
                        <JsonPayload label="Resultado" value={task.result} />
                      ) : null}
                      {task.evidence !== undefined ? (
                        <JsonPayload label="Evidências" value={task.evidence} />
                      ) : null}
                    </div>
                  </details>
                ))}
              </article>
            ))}
          </div>
        </div>
      </section>
      {supervisedMetaCycleId !== undefined ? (
        <ArgosMetaSupervisedSession cycleId={supervisedMetaCycleId} />
      ) : null}
    </>
  );
}

function findSupervisedMetaCycleId(
  detail: NonNullable<ExecutionDetailProps["detail"]>,
) {
  const match = detail.execution.sourceReference.match(
    /^product-discovery-cycle:(\d+)$/,
  );
  if (!match) return undefined;
  const hasSession = detail.activities.some((activity) =>
    activity.tasks.some((task) => {
      const evidence = objectValue(task.evidence);
      const report = objectValue(evidence?.researchEvidenceReport);
      const coverages = report?.metaCoverage;
      return (
        Array.isArray(coverages) &&
        coverages.some((coverage) => {
          const item = objectValue(coverage);
          return (
            item?.publisherPlatform === "INSTAGRAM" &&
            typeof item.investigationId === "number"
          );
        })
      );
    }),
  );
  return hasSession ? Number(match[1]) : undefined;
}

function objectValue(value: unknown): Record<string, unknown> | undefined {
  return value !== null && typeof value === "object" && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : undefined;
}

function JsonPayload({ label, value }: { label: string; value: unknown }) {
  return (
    <div className="independent-process-json">
      <strong>{label}</strong>
      <pre>
        {typeof value === "string" ? value : JSON.stringify(value, null, 2)}
      </pre>
    </div>
  );
}
