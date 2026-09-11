import {
  AlertTriangle,
  Bot,
  CheckCircle2,
  CircleDollarSign,
  CircleOff,
  Clock3,
  ListChecks,
  ListTree,
  Loader2,
  PlayCircle,
  RotateCcw,
  Target,
  Workflow,
} from "lucide-react";
import axios from "axios";
import { useEffect, useState } from "react";
import {
  Link,
  useLocation,
  useParams,
  useSearchParams,
} from "react-router-dom";
import {
  useProductProcessActivityExecutions,
  useRequestProductProcessActivityExecution,
} from "../../api/businessProcess/useProductProcessActivityExecutions";
import type { ProductProcessActivityExecutionGroup } from "../../api/businessProcess/types";
import { useProductValueChainPosition } from "../../api/product/useProductValueChainPositions";
import BusinessProcessEntityName from "../../components/BusinessProcessEntityName";
import PageTitle from "../../components/PageTitle";
import BusinessProcessExecutionCard from "../businessProcess/BusinessProcessExecutionCard";
import "../businessProcess/BusinessProcessesPage.css";
import ProductProcessActivityExecutionPanel from "./ProductProcessActivityExecutionPanel";
import DirectContactSamplePanel from "./DirectContactSamplePanel";
import { SalesFlowTransitions } from "../../components/ProductSalesFlow";
import { salesActivityStateLabels } from "../../api/learningCycle/salesFlow";
import { useCycleProcessContext } from "../../api/learningCycle/useCycleProcessContext";
import ProductLearningCycleContext from "./ProductLearningCycleContext";
import ProductActivityContextCopyButton from "./ProductActivityContextCopyButton";

const usdFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "USD",
  minimumFractionDigits: 2,
  maximumFractionDigits: 8,
});

const coverageLabels = {
  NO_EXECUTIONS: "Nenhuma execução",
  NOT_REPORTED: "Custo não reportado",
  PARTIAL: "Cobertura parcial",
  COMPLETE: "Cobertura completa",
} as const;

type ActivityOperationalState =
  ProductProcessActivityExecutionGroup["operationalState"];

const activityStateLabels: Record<ActivityOperationalState, string> = {
  HISTORICAL: salesActivityStateLabels.HISTORICAL,
  NOT_APPLICABLE: salesActivityStateLabels.NOT_APPLICABLE,
  RECORDED: salesActivityStateLabels.RECORDED,
  WAITING: salesActivityStateLabels.WAITING,
  NOT_STARTED: "Não iniciada",
  PENDING: "Pendente",
  IN_PROGRESS: "Em execução",
  BLOCKED: "Bloqueada",
  COMPLETED: "Concluída",
  CANCELLED: "Cancelada",
};

const processStateLabels = {
  NOT_RECORDED: "Sem atividades registradas",
  NOT_STARTED: "Não iniciado",
  PENDING: "Aguardando execução",
  IN_PROGRESS: "Em andamento",
  BLOCKED: "Bloqueado",
  COMPLETED: "Concluído",
  CANCELLED: "Cancelado",
} as const;

/** Escolhe o ícone semântico do estado já calculado pelo backend. */
function ActivityStateIcon({ state }: { state: ActivityOperationalState }) {
  if (state === "COMPLETED") {
    return <CheckCircle2 size={17} aria-hidden="true" />;
  }
  if (state === "BLOCKED") {
    return <AlertTriangle size={17} aria-hidden="true" />;
  }
  if (state === "IN_PROGRESS") {
    return (
      <Loader2
        className="product-process-situation__running-icon"
        size={17}
        aria-hidden="true"
      />
    );
  }
  if (state === "CANCELLED") {
    return <CircleOff size={17} aria-hidden="true" />;
  }
  return <Clock3 size={17} aria-hidden="true" />;
}

/** Exibe as atividades e tarefas reais de um produto dentro de um processo da cadeia de valor. */
export default function ProductProcessActivityExecutionsPage() {
  const params = useParams();
  const { hash } = useLocation();
  const [search] = useSearchParams();
  const cycleParam = Number(search.get("learningCycleId"));
  const learningCycleId =
    Number.isSafeInteger(cycleParam) && cycleParam > 0 ? cycleParam : undefined;
  const chainParam = Number(search.get("chainId"));
  const chainId =
    Number.isSafeInteger(chainParam) && chainParam > 0 ? chainParam : undefined;
  const productId = Number(params.productId);
  const processDefinitionId = Number(params.processDefinitionId);
  const validProductId = Number.isSafeInteger(productId) && productId > 0;
  const validProcessId =
    Number.isSafeInteger(processDefinitionId) && processDefinitionId > 0;
  const cycleContext = useCycleProcessContext(
    validProductId ? productId : undefined,
    validProcessId ? processDefinitionId : undefined,
    learningCycleId,
    chainId,
  );
  const effectiveCycleId = cycleContext.data?.cycleId ?? learningCycleId;
  const effectiveChainId = cycleContext.data?.chainDefinitionId ?? chainId;
  const cycleContextUnavailable =
    cycleContext.isLoading ||
    (cycleContext.isError && cycleContext.data === undefined);
  const history = useProductProcessActivityExecutions(
    validProductId && !cycleContextUnavailable ? productId : undefined,
    validProcessId ? processDefinitionId : undefined,
    effectiveCycleId,
    effectiveChainId,
  );
  const valueChainPosition = useProductValueChainPosition(
    validProductId && validProcessId ? productId : undefined,
  );
  const requestExecution = useRequestProductProcessActivityExecution(
    productId,
    processDefinitionId,
    effectiveCycleId,
  );
  const [requestOrigin, setRequestOrigin] = useState<string>();
  useEffect(() => {
    setRequestOrigin(undefined);
    requestExecution.reset();
  }, [productId, processDefinitionId, effectiveCycleId]);
  const data = cycleContextUnavailable ? undefined : history.data;
  const loaded = Boolean(data);
  useEffect(() => {
    if (loaded && hash.startsWith("#activity-"))
      document
        .getElementById(hash.slice(1))
        ?.scrollIntoView?.({ block: "start" });
  }, [loaded, hash]);
  const productLabel =
    data?.productInternalName ||
    data?.productName ||
    (validProductId ? `Produto ${productId}` : "Produto");
  const selectedActivities =
    data?.activities.filter((activity) => activity.selectedVersionActivity) ??
    [];
  const completedActivities = selectedActivities.filter(
    (activity) => activity.objectiveAchieved,
  );
  const remainingActivities = selectedActivities.filter(
    (activity) =>
      !activity.objectiveAchieved &&
      !["HISTORICAL", "NOT_APPLICABLE", "RECORDED"].includes(
        activity.operationalState,
      ),
  );
  const completionPercentage = data?.selectedActivityCount
    ? Math.round(
        (data.completedActivityCount / data.selectedActivityCount) * 100,
      )
    : 0;
  const directContactExperimentId =
    data?.processCode === "operacao-otimizacao-experimento" &&
    data.currentExecutionReference?.match(/^experiment:([1-9][0-9]*)$/)
      ? Number(data.currentExecutionReference.split(":")[1])
      : null;
  const selectedProcessSequence = data
    ? [
        ...(valueChainPosition.data?.processMeasurements ?? []),
        ...(valueChainPosition.data?.subprocessPosition?.measurements ?? []),
      ].find(
        (measurement) =>
          measurement.processDefinitionId === data.selectedProcessDefinitionId,
      )?.sequenceLabel ||
      (valueChainPosition.data?.processDefinitionId ===
      data.selectedProcessDefinitionId
        ? valueChainPosition.data.sequenceNumber?.toString()
        : undefined)
    : undefined;
  const selectedProcessName = data
    ? `${selectedProcessSequence ? `Processo ${selectedProcessSequence} — ` : ""}${data.processName}`
    : "";

  if (!validProductId || !validProcessId) {
    return (
      <div className="alert alert-danger" role="alert">
        Produto ou processo inválido.
      </div>
    );
  }

  return (
    <div className="product-process-activity-executions">
      <header className="business-process-documents-toolbar mb-4">
        <div>
          {effectiveCycleId ? (
            <Link
              className="btn btn-outline-primary mb-3"
              to={
                cycleContext.data?.cycleUrl ??
                `/business-process-chains/learning-cycles?productId=${productId}&cycleId=${effectiveCycleId}`
              }
            >
              Voltar ao ciclo #{effectiveCycleId}
            </Link>
          ) : null}
          <PageTitle>
            {data ? (
              <span className="product-process-activity-executions__title">
                <span>{productLabel}</span>
                <span>·</span>
                <BusinessProcessEntityName
                  kind="process"
                  name={selectedProcessName}
                />
              </span>
            ) : (
              "Atividades e tarefas do produto"
            )}
          </PageTitle>
          <p className="text-body-secondary mb-0">
            {data
              ? `Produto ${data.productName || productLabel} · processo v${data.selectedProcessVersionNumber} · ${data.selectedProcessStatus}`
              : "Carregando o histórico auditável do processo..."}
          </p>
        </div>
        <div className="product-process-activity-executions__actions">
          {data?.commercialPlanId ? (
            <Link
              className="btn btn-primary"
              to={`/planning/${data.commercialPlanId}`}
              title={data.commercialPlanName || "Plano comercial do produto"}
            >
              <Target size={17} aria-hidden="true" />
              Plano comercial
            </Link>
          ) : null}
          <Link
            className="btn btn-outline-secondary"
            to={`/products/${productId}/value-chain-history`}
          >
            <ListTree size={17} aria-hidden="true" />
            Histórico de atividades
          </Link>
          <Link
            className="btn btn-outline-primary"
            to={`/business-processes?processId=${processDefinitionId}`}
          >
            <Workflow size={17} aria-hidden="true" />
            Abrir BPM
          </Link>
        </div>
      </header>

      {cycleContext.isError ? (
        <div className="alert alert-danger" role="alert">
          Não foi possível identificar o ciclo desta atividade. Atualize a
          página antes de executar; o histórico não substitui o contexto atual.
        </div>
      ) : null}
      {cycleContext.isLoading ? (
        <p role="status">Identificando o ciclo e o aprendizado anterior...</p>
      ) : null}
      {cycleContext.data ? (
        <ProductLearningCycleContext context={cycleContext.data} />
      ) : null}

      {history.isLoading ? (
        <div
          className="business-process-documents-loading"
          aria-label="Carregando atividades e tarefas"
        >
          <span className="spinner-border text-primary" aria-hidden="true" />
        </div>
      ) : null}

      {history.isError ? (
        <div className="alert alert-danger" role="alert">
          Não foi possível consultar as atividades e tarefas deste produto no
          processo.
        </div>
      ) : null}

      {requestExecution.isSuccess && !requestOrigin ? (
        <div
          className={`alert ${requestExecution.data.objectiveAchieved ? "alert-success" : requestExecution.data.operationalState === "BLOCKED" ? "alert-warning" : "alert-success"}`}
          role="status"
        >
          {requestExecution.data.message ||
            "Atividade solicitada. Todas as tarefas responsáveis foram abertas no mesmo ciclo auditável."}
        </div>
      ) : null}

      {requestExecution.isError && !requestOrigin ? (
        <div className="alert alert-danger" role="alert">
          {axios.isAxiosError(requestExecution.error)
            ? requestExecution.error.response?.data?.message ||
              "Não foi possível executar a atividade. Verifique a situação registrada e tente novamente."
            : "Não foi possível executar a atividade. Verifique a situação registrada e tente novamente."}
        </div>
      ) : null}

      {data ? (
        <>
          <section
            className={`product-process-situation product-process-situation--${data.operationalState.toLowerCase()}`}
            aria-label="Situação do processo"
          >
            <header className="product-process-situation__header">
              <div>
                <span className="product-process-situation__eyebrow">
                  Situação do processo
                </span>
                <h2>O que já foi feito e o que falta concluir</h2>
              </div>
              <span
                className={`product-process-situation__process-state product-process-situation__process-state--${data.operationalState.toLowerCase()}`}
              >
                {data.operationalState === "BLOCKED" ? (
                  <AlertTriangle size={17} aria-hidden="true" />
                ) : data.operationalState === "COMPLETED" ? (
                  <CheckCircle2 size={17} aria-hidden="true" />
                ) : (
                  <Clock3 size={17} aria-hidden="true" />
                )}
                {processStateLabels[data.operationalState]}
              </span>
            </header>

            <div className="product-process-situation__overview">
              <article>
                <span>Progresso das atividades</span>
                <strong>
                  {data.completedActivityCount} de {data.selectedActivityCount}{" "}
                  atividades concluídas
                </strong>
                <div
                  className="product-process-situation__progress"
                  role="progressbar"
                  aria-label="Progresso das atividades"
                  aria-valuemin={0}
                  aria-valuemax={data.selectedActivityCount}
                  aria-valuenow={data.completedActivityCount}
                >
                  <span style={{ width: `${completionPercentage}%` }} />
                </div>
                <small>
                  {data.objectiveAchieved
                    ? "Objetivo do processo atingido."
                    : `${data.remainingActivityCount} atividade${data.remainingActivityCount === 1 ? "" : "s"} ainda sem objetivo comprovado.`}
                </small>
                {data.salesFlow ? (
                  <p className="small text-muted mb-0">
                    Referências históricas e atividades não aplicáveis ficam
                    identificadas abaixo; não contam como conclusão comprovada.
                  </p>
                ) : null}
              </article>
              <article>
                <span>
                  {data.operationalState === "BLOCKED"
                    ? "Atividade que exige correção"
                    : data.objectiveAchieved
                      ? "Resultado"
                      : "Atividade atual"}
                </span>
                <strong>
                  {data.currentActivityName ? (
                    <BusinessProcessEntityName
                      kind="activity"
                      name={data.currentActivityName}
                    />
                  ) : data.objectiveAchieved ? (
                    "Todas as atividades foram concluídas"
                  ) : (
                    "Atividade atual ainda não registrada"
                  )}
                </strong>
                <small>
                  {data.currentActivityStateReason ||
                    (data.objectiveAchieved
                      ? "Não há atividade pendente nesta versão."
                      : "O backend ainda não registrou uma causa ou próxima atividade.")}
                </small>
                {data.currentActivityId ? (
                  <a
                    className="btn btn-outline-primary btn-sm mt-2"
                    href={`#activity-${data.currentActivityId}`}
                  >
                    Ir para a atividade atual
                  </a>
                ) : null}
              </article>
            </div>

            <div className="product-process-situation__columns">
              <section aria-labelledby="completed-activities-title">
                <h3 id="completed-activities-title">
                  <CheckCircle2 size={18} aria-hidden="true" /> Já concluído
                </h3>
                {completedActivities.length > 0 ? (
                  <ol>
                    {completedActivities.map((activity) => (
                      <li key={`completed-${activity.activityId}`}>
                        <strong>
                          <BusinessProcessEntityName
                            kind="activity"
                            name={activity.activityName}
                          />
                        </strong>
                        <small>{activity.stateReason}</small>
                      </li>
                    ))}
                  </ol>
                ) : (
                  <p>Nenhuma atividade possui conclusão comprovada.</p>
                )}
              </section>
              <section aria-labelledby="remaining-activities-title">
                <h3 id="remaining-activities-title">
                  <ListChecks size={18} aria-hidden="true" /> Falta concluir
                </h3>
                {remainingActivities.length > 0 ? (
                  <ol>
                    {remainingActivities.map((activity) => (
                      <li key={`remaining-${activity.activityId}`}>
                        <div>
                          <strong>
                            <BusinessProcessEntityName
                              kind="activity"
                              name={activity.activityName}
                            />
                          </strong>
                          <span
                            className={`product-process-situation__activity-state product-process-situation__activity-state--${activity.operationalState.toLowerCase()}`}
                          >
                            <ActivityStateIcon
                              state={activity.operationalState}
                            />
                            {activityStateLabels[activity.operationalState]}
                          </span>
                        </div>
                        <small>{activity.stateReason}</small>
                      </li>
                    ))}
                  </ol>
                ) : (
                  <p>Nenhuma atividade pendente.</p>
                )}
              </section>
            </div>
          </section>

          <section
            className="product-process-activity-executions__summary"
            aria-label="Resumo das atividades e tarefas"
          >
            <article>
              <span>
                <ListChecks size={17} aria-hidden="true" /> Atividades
              </span>
              <strong>
                {data.completedActivityCount}/{data.selectedActivityCount}
              </strong>
              <small>
                {data.activitiesWithTasksCount} com tarefas reais ·{" "}
                {data.remainingActivityCount} a concluir
              </small>
            </article>
            <article>
              <span>
                <Bot size={17} aria-hidden="true" /> Tarefas únicas
              </span>
              <strong>{data.uniqueTaskCount}</strong>
              <small>Execuções sem duplicar tarefas compostas</small>
            </article>
            <article>
              <span>
                <CircleDollarSign size={17} aria-hidden="true" /> Custo
                conhecido
              </span>
              <strong>
                {usdFormatter.format(Number(data.knownEstimatedCostUsd))}
              </strong>
              <small>{coverageLabels[data.costCoverage]}</small>
            </article>
          </section>

          <section
            className="product-process-activity-executions__activities"
            aria-label="Atividades e tarefas do produto"
          >
            {data.activities.map((activity) => (
              <article
                className="product-process-activity-executions__activity"
                key={activity.activityId}
                id={`activity-${activity.activityId}`}
              >
                <header className="product-process-activity-executions__activity-header">
                  <div>
                    <span className="product-process-activity-executions__eyebrow">
                      Atividade{" "}
                      {selectedProcessSequence
                        ? `${selectedProcessSequence}.`
                        : ""}
                      {activity.sequenceNumber}
                      {activity.executionControl?.interactionType ===
                      "SUBPROCESS"
                        ? " · Abre subprocesso"
                        : ""}
                      {!activity.selectedVersionActivity
                        ? " · versão histórica"
                        : ""}
                    </span>
                    <div className="product-process-activity-executions__activity-heading">
                      <h2>
                        <BusinessProcessEntityName
                          kind="activity"
                          name={activity.activityName}
                        />
                      </h2>
                      <ProductActivityContextCopyButton
                        key={`${data.productId}/${data.selectedProcessDefinitionId}/${effectiveCycleId}/${effectiveChainId}/${activity.activityId}`}
                        history={data}
                        activity={activity}
                        processSequence={selectedProcessSequence}
                        cycle={cycleContext.data}
                        cycleId={effectiveCycleId}
                        chainId={
                          effectiveChainId ??
                          valueChainPosition.data?.chainDefinitionId ??
                          undefined
                        }
                        loading={valueChainPosition.isLoading}
                      />
                    </div>
                    {activity.activityOwnerName ? (
                      <small>Responsável: {activity.activityOwnerName}</small>
                    ) : null}
                  </div>
                  <div className="product-process-activity-executions__activity-statuses">
                    <span
                      className={`product-process-situation__activity-state product-process-situation__activity-state--${activity.operationalState.toLowerCase()}`}
                    >
                      <ActivityStateIcon state={activity.operationalState} />
                      {activityStateLabels[activity.operationalState]}
                    </span>
                    <span className="badge text-bg-light border">
                      {activity.taskCount} tarefa
                      {activity.taskCount === 1 ? "" : "s"}
                    </span>
                    {!activity.executionControl &&
                    activity.executionRequestAvailable ? (
                      <button
                        className="btn btn-primary btn-sm"
                        type="button"
                        disabled={requestExecution.isPending}
                        onClick={() =>
                          requestExecution.mutate({
                            activityId: activity.activityId,
                          })
                        }
                      >
                        {requestExecution.isPending &&
                        requestExecution.variables?.activityId ===
                          activity.activityId ? (
                          <Loader2
                            className="spinner-border spinner-border-sm"
                            size={16}
                            aria-hidden="true"
                          />
                        ) : activity.operationalState === "BLOCKED" ? (
                          <RotateCcw size={16} aria-hidden="true" />
                        ) : (
                          <PlayCircle size={16} aria-hidden="true" />
                        )}
                        {requestExecution.isPending &&
                        requestExecution.variables?.activityId ===
                          activity.activityId
                          ? activity.activityId === "integration"
                            ? "Validando..."
                            : activity.operationalState === "BLOCKED"
                              ? "Reiniciando..."
                              : "Iniciando..."
                          : activity.activityId === "integration"
                            ? activity.operationalState === "BLOCKED"
                              ? "Revalidar integração"
                              : "Validar integração"
                            : activity.operationalState === "BLOCKED"
                              ? "Reiniciar tarefa"
                              : "Executar atividade"}
                      </button>
                    ) : null}
                  </div>
                </header>

                {activity.activityObjective ? (
                  <p className="product-process-activity-executions__objective">
                    <strong>Objetivo:</strong> {activity.activityObjective}
                  </p>
                ) : null}

                {activity.recoveryAction ? (
                  <details className="product-process-activity-executions__state-reason">
                    <summary>Ver motivo do bloqueio</summary>
                    <p className="mt-2 mb-0">{activity.stateReason}</p>
                  </details>
                ) : (
                  <p className="product-process-activity-executions__state-reason">
                    <strong>Situação:</strong> {activity.stateReason}
                  </p>
                )}

                {activity.activityId === "task-2" &&
                directContactExperimentId ? (
                  <DirectContactSamplePanel
                    experimentId={directContactExperimentId}
                    productId={productId}
                    processDefinitionId={processDefinitionId}
                  />
                ) : null}

                {!activity.executionRequestAvailable &&
                !activity.executionControl &&
                activity.executionRequestReason ? (
                  <p className="text-body-secondary small mb-3">
                    {activity.executionRequestReason}
                  </p>
                ) : null}

                <ProductProcessActivityExecutionPanel
                  activity={activity}
                  productId={productId}
                  processSequence={selectedProcessSequence}
                  pending={requestExecution.isPending}
                  pendingActivityId={requestExecution.variables?.activityId}
                  onExecute={(command) => {
                    setRequestOrigin(activity.activityId);
                    requestExecution.mutate(command);
                  }}
                  trackingError={
                    history.trackingError ||
                    history.isRefetchError ||
                    cycleContext.isRefetchError
                  }
                  currentTask={(() => {
                    const task = activity.tasks.find(
                      (item) =>
                        item.sourceReference ===
                          data.currentExecutionReference &&
                        item.processDefinitionId ===
                          data.selectedProcessDefinitionId,
                    );
                    return task
                      ? {
                          taskId: task.taskId,
                          status: task.status,
                          agentName: task.assignedAgentNickname,
                          createdAt: task.createdAt,
                          startedAt: task.startedAt,
                          finishedAt: task.finishedAt,
                          executionError: task.executionError,
                          recommendedAction:
                            task.blockerGuidance?.recommendedAction,
                        }
                      : null;
                  })()}
                  feedback={
                    requestOrigin === activity.activityId
                      ? {
                          error: requestExecution.isError
                            ? axios.isAxiosError(requestExecution.error)
                              ? requestExecution.error.response?.data
                                  ?.message ||
                                "Não foi possível criar a tarefa. Tente novamente."
                              : "Não foi possível criar a tarefa. Tente novamente."
                            : undefined,
                          message: requestExecution.isSuccess
                            ? requestExecution.data.message
                            : undefined,
                          taskIds: requestExecution.isSuccess
                            ? requestExecution.data.tasks.map((task) => task.id)
                            : undefined,
                        }
                      : undefined
                  }
                />

                {activity.activityId === "learningCycle" && data.salesFlow ? (
                  <SalesFlowTransitions flow={data.salesFlow} />
                ) : null}

                {activity.tasks.length > 0 ? (
                  <div className="d-grid gap-3">
                    {activity.tasks.map((execution) => (
                      <BusinessProcessExecutionCard
                        key={`${activity.activityId}-${execution.taskId}`}
                        execution={execution}
                        contentHeadingLevel="h3"
                      />
                    ))}
                  </div>
                ) : activity.executionControl?.interactionType ===
                  "SUBPROCESS" ? (
                  <p className="text-body-secondary small mb-0">
                    Acompanhe as etapas, os resultados e as evidências dentro do
                    subprocesso.
                  </p>
                ) : (
                  <div className="product-process-activity-executions__empty">
                    <Bot size={24} aria-hidden="true" />
                    <span>Nenhuma tarefa registrada para esta atividade.</span>
                  </div>
                )}
              </article>
            ))}
          </section>
        </>
      ) : null}
    </div>
  );
}
