import { ArrowLeft, Bot } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { useBusinessProcessActivityExecutions } from "../../api/businessProcess/useBusinessProcessActivityExecutions";
import PageTitle from "../../components/PageTitle";
import BusinessProcessExecutionCard from "./BusinessProcessExecutionCard";
import "./BusinessProcessesPage.css";

/** Exibe as dez tarefas mais recentes de uma atividade com auditoria completa do Argos ou responsável. */
export default function BusinessProcessActivityExecutionsPage() {
  const params = useParams();
  const processDefinitionId = Number(params.processDefinitionId);
  const activityId = params.activityId;
  const history = useBusinessProcessActivityExecutions(
    Number.isSafeInteger(processDefinitionId) && processDefinitionId > 0
      ? processDefinitionId
      : undefined,
    activityId,
  );
  const backPath =
    history.data?.selectedProcessStatus === "RETIRED"
      ? `/business-processes/retired?processId=${processDefinitionId}`
      : `/business-processes?processId=${processDefinitionId}`;

  if (!Number.isSafeInteger(processDefinitionId) || processDefinitionId <= 0) {
    return <div className="alert alert-danger">Processo inválido.</div>;
  }

  return (
    <div className="business-process-documents-page">
      <header className="business-process-documents-toolbar mb-4">
        <div>
          <PageTitle>
            {history.data
              ? `${history.data.processName} · ${history.data.activityName}`
              : "Execuções da atividade"}
          </PageTitle>
          <p className="text-body-secondary mb-0">
            10 tarefas mais recentes em todas as versões do processo
            {history.data?.activityOwnerName
              ? ` · responsável: ${history.data.activityOwnerName}`
              : ""}
          </p>
        </div>
        <Link className="btn btn-outline-primary" to={backPath}>
          <ArrowLeft size={17} aria-hidden="true" />
          Voltar ao BPM
        </Link>
      </header>

      {history.isLoading ? (
        <div
          className="business-process-documents-loading"
          aria-label="Carregando execuções"
        >
          <span className="spinner-border text-primary" aria-hidden="true" />
        </div>
      ) : null}
      {history.isError ? (
        <div className="alert alert-danger" role="alert">
          Não foi possível consultar as execuções desta atividade.
        </div>
      ) : null}
      {!history.isLoading && !history.isError ? (
        <section aria-label="Execuções mais recentes da atividade">
          <div className="d-grid gap-3">
            {(history.data?.executions ?? []).map((execution, index) => (
              <BusinessProcessExecutionCard
                key={execution.taskId}
                execution={execution}
                defaultOpen={index === 0}
              />
            ))}
          </div>
          {(history.data?.executions ?? []).length === 0 ? (
            <div className="business-process-documents-empty">
              <Bot size={32} aria-hidden="true" />
              <strong>Nenhuma execução registrada</strong>
              <span>
                Esta atividade ainda não possui tarefas em nenhuma versão do
                processo.
              </span>
            </div>
          ) : null}
        </section>
      ) : null}
    </div>
  );
}
