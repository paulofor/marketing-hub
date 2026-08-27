import {
  AlertTriangle,
  ArrowLeft,
  Bot,
  CheckCircle2,
  CircleDollarSign,
  CircleOff,
  Clock3,
  ListChecks,
  Loader2,
  Workflow,
} from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { useProductProcessActivityExecutions } from "../../api/businessProcess/useProductProcessActivityExecutions";
import type { ProductProcessActivityExecutionGroup } from "../../api/businessProcess/types";
import PageTitle from "../../components/PageTitle";
import BusinessProcessExecutionCard from "../businessProcess/BusinessProcessExecutionCard";
import "../businessProcess/BusinessProcessesPage.css";

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
    return <Loader2 size={17} aria-hidden="true" />;
  }
  if (state === "CANCELLED") {
    return <CircleOff size={17} aria-hidden="true" />;
  }
  return <Clock3 size={17} aria-hidden="true" />;
}

/** Exibe as atividades e tarefas reais de um produto dentro de um processo da cadeia de valor. */
export default function ProductProcessActivityExecutionsPage() {
  const params = useParams();
  const productId = Number(params.productId);
  const processDefinitionId = Number(params.processDefinitionId);
  const validProductId = Number.isSafeInteger(productId) && productId > 0;
  const validProcessId =
    Number.isSafeInteger(processDefinitionId) && processDefinitionId > 0;
  const history = useProductProcessActivityExecutions(
    validProductId ? productId : undefined,
    validProcessId ? processDefinitionId : undefined,
  );
  const data = history.data;
  const productLabel =
    data?.productInternalName ||
    data?.productName ||
    (validProductId ? `Produto ${productId}` : "Produto");
  const firstActivityWithTasks = data?.activities.find(
    (activity) => activity.tasks.length > 0,
  );
  const selectedActivities =
    data?.activities.filter((activity) => activity.selectedVersionActivity) ??
    [];
  const completedActivities = selectedActivities.filter(
    (activity) => activity.objectiveAchieved,
  );
  const remainingActivities = selectedActivities.filter(
    (activity) => !activity.objectiveAchieved,
  );
  const completionPercentage = data?.selectedActivityCount
    ? Math.round(
        (data.completedActivityCount / data.selectedActivityCount) * 100,
      )
    : 0;

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
          <PageTitle>
            {data
              ? `${productLabel} · ${data.processName}`
              : "Atividades e tarefas do produto"}
          </PageTitle>
          <p className="text-body-secondary mb-0">
            {data
              ? `Produto ${data.productName || productLabel} · processo v${data.selectedProcessVersionNumber} · ${data.selectedProcessStatus}`
              : "Carregando o histórico auditável do processo..."}
          </p>
        </div>
        <div className="product-process-activity-executions__actions">
          <Link
            className="btn btn-outline-secondary"
            to={`/products/${productId}/value-chain-history`}
          >
            <ArrowLeft size={17} aria-hidden="true" />
            Voltar ao histórico
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
                  {data.currentActivityName ||
                    (data.objectiveAchieved
                      ? "Todas as atividades foram concluídas"
                      : "Atividade atual ainda não registrada")}
                </strong>
                <small>
                  {data.currentActivityStateReason ||
                    (data.objectiveAchieved
                      ? "Não há atividade pendente nesta versão."
                      : "O backend ainda não registrou uma causa ou próxima atividade.")}
                </small>
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
                        <strong>{activity.activityName}</strong>
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
                          <strong>{activity.activityName}</strong>
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
              >
                <header className="product-process-activity-executions__activity-header">
                  <div>
                    <span className="product-process-activity-executions__eyebrow">
                      Atividade {activity.sequenceNumber}
                      {!activity.selectedVersionActivity
                        ? " · versão histórica"
                        : ""}
                    </span>
                    <h2>{activity.activityName}</h2>
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
                  </div>
                </header>

                {activity.activityObjective ? (
                  <p className="product-process-activity-executions__objective">
                    <strong>Objetivo:</strong> {activity.activityObjective}
                  </p>
                ) : null}

                <p className="product-process-activity-executions__state-reason">
                  <strong>Situação:</strong> {activity.stateReason}
                </p>

                {activity.tasks.length > 0 ? (
                  <div className="d-grid gap-3">
                    {activity.tasks.map((execution, taskIndex) => (
                      <BusinessProcessExecutionCard
                        key={`${activity.activityId}-${execution.taskId}`}
                        execution={execution}
                        defaultOpen={
                          activity.activityId ===
                            firstActivityWithTasks?.activityId &&
                          taskIndex === 0
                        }
                        contentHeadingLevel="h3"
                      />
                    ))}
                  </div>
                ) : (
                  <div className="product-process-activity-executions__empty">
                    <Bot size={24} aria-hidden="true" />
                    <span>Nenhuma tarefa registrada para este produto.</span>
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
