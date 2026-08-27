import {
  ArrowLeft,
  Bot,
  CircleDollarSign,
  ListChecks,
  Workflow,
} from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { useProductProcessActivityExecutions } from "../../api/businessProcess/useProductProcessActivityExecutions";
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
            className="product-process-activity-executions__summary"
            aria-label="Resumo das atividades e tarefas"
          >
            <article>
              <span>
                <ListChecks size={17} aria-hidden="true" /> Atividades
              </span>
              <strong>{data.activityCount}</strong>
              <small>{data.activitiesWithTasksCount} com tarefas reais</small>
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
                  <span className="badge text-bg-light border">
                    {activity.taskCount} tarefa
                    {activity.taskCount === 1 ? "" : "s"}
                  </span>
                </header>

                {activity.activityObjective ? (
                  <p className="product-process-activity-executions__objective">
                    <strong>Objetivo:</strong> {activity.activityObjective}
                  </p>
                ) : null}

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
