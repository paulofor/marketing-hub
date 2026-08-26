import { ArrowLeft, Bot } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { useBusinessProcessActivityExecutions } from "../../api/businessProcess/useBusinessProcessActivityExecutions";
import PageTitle from "../../components/PageTitle";
import {
  formattedDateTime,
  formattedDuration,
  StructuredExecutionContent,
} from "./BusinessProcessExecutionPresentation";
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
              <details
                className="card business-process-document"
                key={execution.taskId}
                open={index === 0}
              >
                <summary className="card-header">
                  <Bot size={18} aria-hidden="true" />
                  <span className="flex-grow-1">
                    <strong>{execution.title}</strong>
                    <small className="d-block text-body-secondary mt-1">
                      Tarefa #{execution.taskId} ·{" "}
                      {execution.assignedAgentNickname} · v
                      {execution.processVersionNumber} · {execution.status}
                    </small>
                  </span>
                </summary>
                <div className="card-body">
                  <dl className="business-process-document__audit mb-3">
                    <div>
                      <dt>Origem</dt>
                      <dd>{execution.sourceReference ?? "Não informada"}</dd>
                    </div>
                    <div>
                      <dt>Produto interno</dt>
                      <dd>
                        {execution.productInternalName ?? "Não vinculado"}
                      </dd>
                    </div>
                    <div>
                      <dt>Versão do processo</dt>
                      <dd>v{execution.processVersionNumber}</dd>
                    </div>
                    <div>
                      <dt>Status</dt>
                      <dd>{execution.status}</dd>
                    </div>
                    <div>
                      <dt>Criação</dt>
                      <dd>{formattedDateTime(execution.createdAt)}</dd>
                    </div>
                    <div>
                      <dt>Início</dt>
                      <dd>{formattedDateTime(execution.startedAt)}</dd>
                    </div>
                    <div>
                      <dt>Término</dt>
                      <dd>{formattedDateTime(execution.finishedAt)}</dd>
                    </div>
                    <div>
                      <dt>Duração</dt>
                      <dd>
                        {formattedDuration(
                          execution.startedAt,
                          execution.finishedAt,
                        )}
                      </dd>
                    </div>
                    <div>
                      <dt>Modelo utilizado</dt>
                      <dd>{execution.modelCode ?? "Não registrado"}</dd>
                    </div>
                    <div>
                      <dt>Tipo de raciocínio</dt>
                      <dd>{execution.reasoningEffort ?? "Não registrado"}</dd>
                    </div>
                    <div>
                      <dt>Tokens</dt>
                      <dd>
                        entrada {execution.inputTokens ?? "—"} · cache{" "}
                        {execution.cachedInputTokens ?? "—"} · saída{" "}
                        {execution.outputTokens ?? "—"}
                      </dd>
                    </div>
                    <div>
                      <dt>Custo estimado</dt>
                      <dd>
                        {execution.estimatedCostUsd === undefined ||
                        execution.estimatedCostUsd === null
                          ? "Indisponível"
                          : `US$ ${Number(execution.estimatedCostUsd).toFixed(8)}`}
                      </dd>
                    </div>
                  </dl>

                  <h2 className="h6">
                    Prompt recebido por {execution.assignedAgentNickname}
                  </h2>
                  <StructuredExecutionContent
                    value={execution.promptSent}
                    emptyText="Prompt não registrado nesta execução legada."
                  />

                  <h2 className="h6 mt-3">
                    Comentários de {execution.assignedAgentNickname}
                  </h2>
                  <StructuredExecutionContent
                    value={execution.comments}
                    emptyText="Nenhum comentário registrado."
                  />

                  {execution.executionError ? (
                    <div className="alert alert-danger mt-3 mb-0">
                      <strong>Falha técnica</strong>
                      <StructuredExecutionContent
                        value={execution.executionError}
                      />
                    </div>
                  ) : null}

                  <details className="mt-3">
                    <summary className="fw-semibold">Evidências</summary>
                    <div className="mt-2">
                      <StructuredExecutionContent
                        value={execution.evidenceJson}
                      />
                    </div>
                  </details>
                </div>
              </details>
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
