import axios from "axios";
import { FormEvent, useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  AlertCircle,
  ArrowRight,
  BookOpen,
  CheckCircle2,
  Clock3,
  ExternalLink,
  Globe2,
  Instagram,
  PackageCheck,
  Play,
  RefreshCw,
  Search,
  Sparkles,
  Target,
  WalletCards,
} from "lucide-react";
import { toast } from "react-toastify";
import {
  useIndependentBusinessProcessCatalog,
  useIndependentBusinessProcessExecutions,
  useStartIndependentBusinessProcessExecution,
} from "../../api/businessProcess/useIndependentBusinessProcessExecutions";
import type {
  IndependentBusinessProcessExecution,
  IndependentBusinessProcessFlowReport,
  IndependentBusinessProcessExecutionSummary,
  IndependentBusinessProcessInputField,
} from "../../api/businessProcess/types";
import PageTitle from "../../components/PageTitle";
import ArgosMetaSupervisedSession from "../productDiscovery/ArgosMetaSupervisedSession";
import BusinessProcessExecutionAudit from "./BusinessProcessExecutionAudit";
import "./IndependentBusinessProcessExecutionsPage.css";

const statusLabels: Record<string, string> = {
  NOT_STARTED: "Não iniciada",
  PENDING: "Na fila",
  IN_PROGRESS: "Em execução",
  BLOCKED: "Bloqueada",
  COMPLETED: "Concluída",
  CANCELLED: "Cancelada",
};

export function isCompletedWithGaps(
  execution: IndependentBusinessProcessExecutionSummary,
) {
  return (
    execution.status === "BLOCKED" &&
    execution.activityCount > 0 &&
    execution.completedActivityCount === execution.activityCount &&
    !execution.latestError?.trim()
  );
}

function executionStatusLabel(
  execution: IndependentBusinessProcessExecutionSummary,
) {
  return isCompletedWithGaps(execution)
    ? "Concluída com lacunas"
    : (statusLabels[execution.status] ?? execution.status);
}

function executionStatusClass(
  execution: IndependentBusinessProcessExecutionSummary,
) {
  return isCompletedWithGaps(execution)
    ? "is-completed-with-gaps"
    : statusClass(execution.status);
}

export function createIndependentExecutionRequestKey() {
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

export function independentExecutionRequestError(error: unknown) {
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
  const navigate = useNavigate();
  const catalogQuery = useIndependentBusinessProcessCatalog();
  const executionsQuery = useIndependentBusinessProcessExecutions();
  const start = useStartIndependentBusinessProcessExecution();
  const [selectedProcessId, setSelectedProcessId] = useState<number>();
  const [requestedByName, setRequestedByName] = useState("Marketing Hub");
  const [input, setInput] = useState<Record<string, string>>({});
  const [requestKey, setRequestKey] = useState(
    createIndependentExecutionRequestKey,
  );
  const catalog = catalogQuery.data ?? [];
  const executions =
    executionsQuery.data?.pages.flatMap((page) => page.items) ?? [];
  const selectedProcess = useMemo(
    () =>
      catalog.find((item) => item.processDefinitionId === selectedProcessId),
    [catalog, selectedProcessId],
  );
  useEffect(() => {
    if (selectedProcessId !== undefined || catalog.length === 0) return;
    const first = catalog.find((item) => item.executionAvailable) ?? catalog[0];
    setSelectedProcessId(first.processDefinitionId);
  }, [catalog, selectedProcessId]);

  useEffect(() => {
    if (!selectedProcess) return;
    setInput(defaultInputs(selectedProcess.inputFields));
    setRequestKey(createIndependentExecutionRequestKey());
  }, [selectedProcess]);

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
      setRequestKey(createIndependentExecutionRequestKey());
      toast.success(
        "Processo iniciado e tarefa encaminhada ao agente responsável.",
      );
      navigate(`/business-process-executions/${result.execution.id}`);
    } catch (error) {
      toast.error(independentExecutionRequestError(error));
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

      {selectedProcess?.processCode === "pde-opportunity-discovery" ? (
        <AutonomousDiscoveryFlow />
      ) : null}

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
                          title={field.helpText}
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
                          title={field.helpText}
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
                          title={field.helpText}
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
              {executionsQuery.isError && executions.length === 0 ? (
                <div className="alert alert-danger">
                  Falha ao carregar o histórico.
                </div>
              ) : null}
              <div className="independent-process-history__list">
                {executions.map((execution) => (
                  <Link
                    key={execution.id}
                    className="independent-process-history__item"
                    to={`/business-process-executions/${execution.id}`}
                  >
                    <span
                      className={`independent-process-status ${executionStatusClass(execution)}`}
                    >
                      {executionStatusLabel(execution)}
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
                    {execution.latestError ? (
                      <span className="independent-process-history__error">
                        <AlertCircle size={16} aria-hidden="true" />
                        <span>
                          <strong>Por que não executou</strong>
                          {execution.latestError}
                        </span>
                      </span>
                    ) : null}
                    <span className="independent-process-history__action">
                      Ver detalhes
                      <ArrowRight size={16} aria-hidden="true" />
                    </span>
                  </Link>
                ))}
                {!executionsQuery.isLoading && executions.length === 0 ? (
                  <p className="text-body-secondary mb-0">
                    Nenhuma execução iniciada por esta tela.
                  </p>
                ) : null}
              </div>
              {executionsQuery.isFetchNextPageError ? (
                <div className="alert alert-warning mt-3 mb-0" role="alert">
                  Não foi possível carregar as execuções anteriores. Tente
                  novamente.
                </div>
              ) : null}
              {executionsQuery.hasNextPage ? (
                <button
                  type="button"
                  className="btn btn-outline-primary w-100 mt-3"
                  disabled={executionsQuery.isFetchingNextPage}
                  onClick={() => void executionsQuery.fetchNextPage()}
                >
                  {executionsQuery.isFetchingNextPage ? (
                    <>
                      <RefreshCw
                        className="independent-process-spin me-2"
                        size={16}
                        aria-hidden="true"
                      />
                      Carregando...
                    </>
                  ) : (
                    "Carregar execuções anteriores"
                  )}
                </button>
              ) : null}
            </div>
          </aside>
        </section>
      ) : null}
    </div>
  );
}

function AutonomousDiscoveryFlow() {
  const steps = [
    {
      title: "Tema amplo",
      owner: "Usuário",
      description: "Você informa apenas o universo que deseja explorar.",
      icon: <Target size={20} aria-hidden="true" />,
    },
    {
      title: "Pesquisa factual",
      owner: "Argos",
      description: "Internet, Instagram, Biblioteca Meta e /pesquisas.",
      icon: <Search size={20} aria-hidden="true" />,
    },
    {
      title: "2–3 candidatas",
      owner: "Backend",
      description: "Dossiês vinculados e rastreáveis, sem inventar demanda.",
      icon: <BookOpen size={20} aria-hidden="true" />,
    },
    {
      title: "Priorização",
      owner: "Atena",
      description: "Seleciona no máximo uma oportunidade por ciclo.",
      icon: <Sparkles size={20} aria-hidden="true" />,
    },
    {
      title: "Economia",
      owner: "Plutus",
      description: "Valida preço, margem, limites e risco financeiro.",
      icon: <WalletCards size={20} aria-hidden="true" />,
    },
    {
      title: "Harness PDE",
      owner: "Dédalo",
      description: "Projeta a experiência sensorial e personalizada com IA.",
      icon: <PackageCheck size={20} aria-hidden="true" />,
    },
    {
      title: "Produto planejado",
      owner: "Backend",
      description: "Cria o cadastro em PLANNED e mantém execução em STOP.",
      icon: <CheckCircle2 size={20} aria-hidden="true" />,
    },
  ];
  return (
    <section
      className="independent-process-flow-overview"
      aria-labelledby="autonomous-flow-title"
    >
      <div className="independent-process-flow-overview__heading">
        <div>
          <span>Fluxo autônomo até o produto</span>
          <h2 id="autonomous-flow-title">Do tema amplo ao PDE planejado</h2>
        </div>
        <div className="independent-process-channel">
          <Instagram size={18} aria-hidden="true" />
          Canal de aquisição: Instagram
        </div>
      </div>
      <p>
        Os agentes derivam público, problema, oferta, economia e experiência. O
        produto só nasce em <strong>PLANNED</strong> após todos os gates; não há
        publicação, campanha ou gasto automático. A priorização deste fluxo
        serve ao planejamento: a validação privada do Momento de Compra continua
        obrigatória antes da priorização comercial final e da ativação.
      </p>
      <div className="independent-process-flow-overview__steps">
        {steps.map((step, index) => (
          <div
            className="independent-process-flow-overview__step-wrap"
            key={step.title}
          >
            <article className="independent-process-flow-overview__step">
              <div>{step.icon}</div>
              <span>{step.owner}</span>
              <strong>{step.title}</strong>
              <small>{step.description}</small>
            </article>
            {index < steps.length - 1 ? (
              <ArrowRight
                className="independent-process-flow-overview__arrow"
                size={18}
                aria-hidden="true"
              />
            ) : null}
          </div>
        ))}
      </div>
    </section>
  );
}

type ExecutionDetailProps = {
  loading: boolean;
  error: boolean;
  detail?: IndependentBusinessProcessExecution;
};

export function IndependentBusinessProcessExecutionDetail({
  loading,
  error,
  detail,
}: ExecutionDetailProps) {
  if (loading && !detail)
    return <div className="card card-body">Carregando execução...</div>;
  if (!detail) {
    return (
      <div className="alert alert-danger">
        {error
          ? "Não foi possível detalhar a execução."
          : "A execução ainda não possui detalhe disponível."}
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
                className={`independent-process-status ${executionStatusClass(execution)}`}
              >
                {executionStatusLabel(execution)}
              </span>
              <h2>
                Execução #{execution.id} · {execution.displayName}
              </h2>
              <p>{execution.sourceReference}</p>
            </div>
            {execution.status === "COMPLETED" ||
            isCompletedWithGaps(execution) ? (
              <CheckCircle2
                className={
                  execution.status === "COMPLETED"
                    ? "text-success"
                    : "text-warning"
                }
                size={32}
                aria-label={
                  execution.status === "COMPLETED"
                    ? "Execução concluída"
                    : "Execução concluída com lacunas"
                }
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

          {detail.processReport ? (
            <PdeOpportunityFlowReport report={detail.processReport} />
          ) : null}

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
                      <BusinessProcessExecutionAudit
                        execution={{
                          ...task,
                          processVersionNumber:
                            task.processVersionNumber ??
                            execution.processVersionNumber,
                          sourceReference:
                            task.sourceReference ?? execution.sourceReference,
                        }}
                        headingLevel="h4"
                      />
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

const maturityLabels: Record<string, string> = {
  SIGNAL: "Sinal inicial",
  RESEARCHABLE: "Precisa aprofundar",
  DOSSIER_READY: "Dossiê pronto",
  HUMAN_REVIEW: "Revisão humana",
  REJECTED: "Descartada",
};

const flowStatusLabels: Record<string, string> = {
  ...statusLabels,
  NOT_SELECTED: "Não priorizada",
  NOT_STARTED: "Não iniciada",
  WAITING: "Aguardando",
  OBSERVED: "Observado",
  MISSING: "Não comprovado",
  UNAVAILABLE: "Não executada",
  AWAITING_OBSERVATION: "Aguardando observação",
  OBSERVED_EMPTY: "Executada sem aderência",
  NO_MATCHING_ACTIVE_ADS: "Executada sem anúncio aderente",
  NO_ACTIVE_ADS: "Sem anúncio ativo",
  NO_RELEVANT_PLATFORM_EVIDENCE: "Sem evidência aderente ao Instagram",
  AWAITING_PUBLIC_BROWSER: "Aguardando navegador público",
  AWAITING_SUPERVISED_OBSERVATION: "Aguardando observação supervisionada",
  AWAITING_OFFICIAL_COLLECTION: "Aguardando coleta oficial",
};

const metaCollectionModeLabels: Record<string, string> = {
  PUBLIC_BROWSER: "Navegador público",
  SUPERVISED: "Observação supervisionada",
  OFFICIAL_API: "API oficial",
  BACKEND_UNAVAILABLE: "Integração indisponível",
  UNKNOWN: "Não iniciada",
};

const expansionOutcomeLabels: Record<string, string> = {
  ADJUST_AND_CONTINUE: "Ampliar e reavaliar",
  DOSSIER_READY_FOUND: "Dossiê pronto encontrado",
  NO_NEW_EVIDENCE: "Sem nova evidência",
  REPEATED_RESEARCH_LENS: "Lente repetida evitada",
  EXPANSION_NOT_APPLICABLE: "Ampliação não aplicável",
  ATTEMPT_LIMIT_REACHED: "Limite alcançado",
};

function expansionOutcomeStatus(outcome?: string) {
  if (outcome === "DOSSIER_READY_FOUND") return "COMPLETED";
  return "PENDING";
}

function PdeOpportunityFlowReport({
  report,
}: {
  report: IndependentBusinessProcessFlowReport;
}) {
  return (
    <section
      className="independent-process-report"
      aria-labelledby="independent-process-report-title"
    >
      <header className="independent-process-report__header">
        <div>
          <span>Relatório de negócio · Argos até produto</span>
          <h3 id="independent-process-report-title">{report.headline}</h3>
        </div>
        <span
          className={`independent-process-status ${statusClass(report.status)}`}
        >
          {flowStatusLabels[report.status] ?? report.status}
        </span>
      </header>

      <div className="independent-process-report__summary">
        <div>
          <span>Canal</span>
          <strong>
            <Instagram size={16} aria-hidden="true" />
            {report.acquisitionChannel}
          </strong>
        </div>
        <div>
          <span>Candidatas factuais</span>
          <strong>{report.candidateCount}</strong>
        </div>
        <div>
          <span>Dossiês prontos</span>
          <strong>{report.dossierReadyCount}</strong>
        </div>
        <div>
          <span>Produtos planejados</span>
          <strong>{report.plannedProductCount}</strong>
        </div>
      </div>

      {report.marketExpansion ? (
        <section className="independent-process-report__expansion">
          <div className="independent-process-report__section-heading">
            <RefreshCw size={20} aria-hidden="true" />
            <div>
              <h4>Ampliação controlada de mercado</h4>
              <p>
                Argos registrou {report.marketExpansion.attemptsCompleted} de{" "}
                {report.marketExpansion.maxAttempts} lentes possíveis na mesma
                execução.
              </p>
            </div>
          </div>
          <div className="independent-process-report__expansion-stop">
            <strong>{report.marketExpansion.finalResearchLens}</strong>
            <span>{report.marketExpansion.stopSummary}</span>
          </div>
          <div className="independent-process-report__attempts">
            {report.marketExpansion.attempts.map((attempt) => (
              <article key={attempt.attemptNumber}>
                <header>
                  <span>Tentativa {attempt.attemptNumber}</span>
                  <span
                    className={`independent-process-status ${statusClass(
                      expansionOutcomeStatus(attempt.outcome),
                    )}`}
                  >
                    {expansionOutcomeLabels[attempt.outcome ?? ""] ??
                      attempt.outcome}
                  </span>
                </header>
                <strong>{attempt.researchLens}</strong>
                {attempt.rationale ? <p>{attempt.rationale}</p> : null}
                <div>
                  <span>+{attempt.newPublicEvidenceCount} Web</span>
                  <span>+{attempt.newComparableOfferCount} ofertas</span>
                  <span>+{attempt.newMetaAdCount} anúncios</span>
                  <span>{attempt.dossierReadyCount} dossiês prontos</span>
                </div>
                {attempt.metaQuery || attempt.metaCoverageStatus ? (
                  <section className="independent-process-report__attempt-meta">
                    <strong>
                      <Instagram size={14} aria-hidden="true" /> Biblioteca Meta
                      / Instagram
                    </strong>
                    {attempt.metaQuery ? (
                      <span>Consulta: {attempt.metaQuery}</span>
                    ) : null}
                    {attempt.metaCoverageStatus ? (
                      <span>
                        Cobertura:{" "}
                        {flowStatusLabels[attempt.metaCoverageStatus] ??
                          attempt.metaCoverageStatus}
                      </span>
                    ) : null}
                    {attempt.metaCollectionMode ? (
                      <span>
                        Modo:{" "}
                        {metaCollectionModeLabels[attempt.metaCollectionMode] ??
                          attempt.metaCollectionMode}
                      </span>
                    ) : null}
                    <span>
                      {attempt.metaAdsObserved ?? 0} anúncio(s) ·{" "}
                      {attempt.metaAdvertisersObserved ?? 0} anunciante(s)
                    </span>
                    {attempt.metaCoverageSummary ? (
                      <small>{attempt.metaCoverageSummary}</small>
                    ) : null}
                    {isHttpUrl(attempt.metaSearchUrl) ? (
                      <a
                        href={attempt.metaSearchUrl}
                        target="_blank"
                        rel="noreferrer"
                      >
                        Abrir consulta oficial
                        <ExternalLink size={13} aria-hidden="true" />
                      </a>
                    ) : null}
                  </section>
                ) : null}
              </article>
            ))}
          </div>
        </section>
      ) : null}

      <div className="independent-process-report__section-heading">
        <Globe2 size={20} aria-hidden="true" />
        <div>
          <h4>Cobertura factual</h4>
          <p>O que Argos conseguiu observar em cada fonte.</p>
        </div>
      </div>
      <div className="independent-process-report__coverage">
        {report.sourceCoverage.map((source) => (
          <article key={source.sourceCode}>
            <span
              className={`independent-process-status ${statusClass(
                source.status === "OBSERVED"
                  ? "COMPLETED"
                  : source.status === "OBSERVED_EMPTY"
                    ? "COMPLETED"
                    : ["MISSING", "UNAVAILABLE"].includes(source.status)
                      ? "BLOCKED"
                      : source.status,
              )}`}
            >
              {flowStatusLabels[source.status] ?? source.status}
            </span>
            <strong>{source.label}</strong>
            <b>{source.itemCount} itens</b>
            <small>{source.summary}</small>
          </article>
        ))}
      </div>

      <div className="independent-process-report__section-heading">
        <Target size={20} aria-hidden="true" />
        <div>
          <h4>Mercados candidatos</h4>
          <p>
            Sinais, fontes e decisões persistidas — intenção e anúncios não são
            tratados como vendas.
          </p>
        </div>
      </div>
      {report.candidates.length === 0 ? (
        <div className="independent-process-report__empty">
          Argos ainda não formou uma candidata factual. Consulte a cobertura e a
          próxima lacuna da pesquisa.
        </div>
      ) : (
        <div className="independent-process-candidates">
          {report.candidates.map((candidate) => (
            <article
              className="independent-process-candidate"
              key={candidate.opportunityId}
            >
              <header>
                <div>
                  <span>Candidata #{candidate.opportunityId}</span>
                  <h4>{candidate.name}</h4>
                  {candidate.primaryAudience ? (
                    <p>{candidate.primaryAudience}</p>
                  ) : null}
                </div>
                <div className="independent-process-candidate__badges">
                  <span className="independent-process-status is-pending">
                    {maturityLabels[candidate.maturity] ?? candidate.maturity}
                  </span>
                  {candidate.score !== undefined ? (
                    <strong>Score factual {candidate.score}</strong>
                  ) : null}
                </div>
              </header>

              <dl className="independent-process-candidate__facts">
                {candidate.rootPain ? (
                  <div>
                    <dt>Dor raiz</dt>
                    <dd>{candidate.rootPain}</dd>
                  </div>
                ) : null}
                {candidate.purchaseSituation ? (
                  <div>
                    <dt>Momento de compra</dt>
                    <dd>{candidate.purchaseSituation}</dd>
                  </div>
                ) : null}
                {candidate.instagramFitEvidence ? (
                  <div>
                    <dt>Aderência ao Instagram</dt>
                    <dd>{candidate.instagramFitEvidence}</dd>
                  </div>
                ) : null}
                {candidate.residualEffort ? (
                  <div>
                    <dt>Esforço que ainda sobra</dt>
                    <dd>{candidate.residualEffort}</dd>
                  </div>
                ) : null}
                {candidate.commercialRisk ? (
                  <div>
                    <dt>Risco comercial</dt>
                    <dd>{candidate.commercialRisk}</dd>
                  </div>
                ) : null}
                <div>
                  <dt>Linhagem</dt>
                  <dd>
                    Dossiê{" "}
                    {candidate.dossierId
                      ? `#${candidate.dossierId}`
                      : "ainda não criado"}
                    {candidate.commercialPlanId
                      ? ` · Plano #${candidate.commercialPlanId}`
                      : ""}
                  </dd>
                </div>
              </dl>

              {candidate.observedLanguage.length > 0 ? (
                <div className="independent-process-candidate__language">
                  <strong>Linguagem observada</strong>
                  <div>
                    {candidate.observedLanguage.map((text) => (
                      <span key={text}>“{text}”</span>
                    ))}
                  </div>
                </div>
              ) : null}

              {candidate.currentAlternatives.length > 0 ? (
                <div className="independent-process-candidate__language">
                  <strong>Alternativas que o público usa hoje</strong>
                  <div>
                    {candidate.currentAlternatives.map((alternative) => (
                      <span key={alternative}>{alternative}</span>
                    ))}
                  </div>
                </div>
              ) : null}

              <div className="independent-process-candidate__sources">
                <strong>
                  Fontes da candidata ({candidate.sources.length})
                </strong>
                {candidate.sources.length === 0 ? (
                  <p>Nenhuma fonte rastreável foi vinculada.</p>
                ) : (
                  <ul>
                    {candidate.sources.map((source, index) => (
                      <li
                        key={`${source.sourceType}-${source.url ?? source.title}-${index}`}
                      >
                        <span>{source.sourceType}</span>
                        {isHttpUrl(source.url) ? (
                          <a href={source.url} target="_blank" rel="noreferrer">
                            {source.title}
                            <ExternalLink size={13} aria-hidden="true" />
                          </a>
                        ) : (
                          <span className="independent-process-candidate__source-reference">
                            <strong>{source.title}</strong>
                            {source.url ? <small>{source.url}</small> : null}
                          </span>
                        )}
                        {source.evidence ? (
                          <small>{source.evidence}</small>
                        ) : null}
                      </li>
                    ))}
                  </ul>
                )}
              </div>

              <div className="independent-process-candidate__stages">
                {candidate.stages.map((stage, index) => (
                  <div
                    className="independent-process-candidate__stage-wrap"
                    key={stage.stageCode}
                  >
                    <article className="independent-process-candidate__stage">
                      <span>{stage.agent}</span>
                      <strong>{stage.label}</strong>
                      <b
                        className={`independent-process-status ${statusClass(
                          stage.status,
                        )}`}
                      >
                        {flowStatusLabels[stage.status] ?? stage.status}
                      </b>
                      {stage.decision ? (
                        <small>Decisão: {stage.decision}</small>
                      ) : null}
                      {stage.taskId ? (
                        <small>Tarefa #{stage.taskId}</small>
                      ) : null}
                      {stage.estimatedCostUsd !== undefined ? (
                        <small>
                          Custo:{" "}
                          {formatCost(stage.estimatedCostUsd, "COMPLETE")}
                        </small>
                      ) : null}
                      {stage.updatedAt ? (
                        <small>Atualizado: {formatDate(stage.updatedAt)}</small>
                      ) : null}
                      {stage.summary ? <p>{stage.summary}</p> : null}
                      {stage.blocker ? (
                        <p className="text-danger">Bloqueio: {stage.blocker}</p>
                      ) : null}
                    </article>
                    {index < candidate.stages.length - 1 ? (
                      <ArrowRight size={16} aria-hidden="true" />
                    ) : null}
                  </div>
                ))}
              </div>

              <div className="independent-process-candidate__next-action">
                <strong>Próxima ação</strong>
                <span>{candidate.nextAction}</span>
                {candidate.productId ? (
                  <div className="independent-process-candidate__product">
                    <small>
                      {candidate.productName} · {candidate.productStatus}
                    </small>
                    <a
                      className="btn btn-sm btn-outline-primary"
                      href={`/products/${candidate.productId}/edit`}
                      target="_blank"
                      rel="noreferrer"
                    >
                      Abrir produto #{candidate.productId}
                      <ExternalLink size={14} aria-hidden="true" />
                    </a>
                  </div>
                ) : null}
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function isHttpUrl(value?: string) {
  return value?.startsWith("https://") || value?.startsWith("http://");
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
