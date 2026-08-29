import { Link, useParams } from "react-router-dom";
import { ArrowLeft, FileText } from "lucide-react";
import {
  useBusinessProcessActivityDocuments,
  useBusinessProcessDocuments,
} from "../../api/businessProcess/useBusinessProcessDocuments";
import { useBusinessProcesses } from "../../api/businessProcess/useBusinessProcesses";
import PageTitle from "../../components/PageTitle";
import {
  formattedDateTime,
  formattedDuration,
  StructuredExecutionContent,
} from "./BusinessProcessExecutionPresentation";
import "./BusinessProcessesPage.css";

/** Exibe os dez documentos mais recentes de uma atividade, com sua auditoria de execução. */
export default function BusinessProcessActivityDocumentsPage() {
  const params = useParams();
  const processDefinitionId = Number(params.processDefinitionId);
  const activityId = params.activityId;
  const processes = useBusinessProcesses();
  const activityDocuments = useBusinessProcessActivityDocuments(
    Number.isSafeInteger(processDefinitionId) && processDefinitionId > 0
      ? processDefinitionId
      : undefined,
    activityId,
  );
  const processDocuments = useBusinessProcessDocuments(
    !activityId &&
      Number.isSafeInteger(processDefinitionId) &&
      processDefinitionId > 0
      ? processDefinitionId
      : undefined,
  );
  const documents = activityId ? activityDocuments : processDocuments;
  const process = (processes.data ?? []).find(
    (item) => item.id === processDefinitionId,
  );
  const activity = process?.diagram.nodes.find(
    (node) => node.id === activityId && node.type === "TASK",
  );
  const backPath =
    process?.status === "RETIRED"
      ? `/business-processes/retired?processId=${processDefinitionId}`
      : `/business-processes?processId=${processDefinitionId}`;
  const screenTitle = `${process?.name ?? "Processo"} · ${
    activityId ? (activity?.label ?? activityId) : "Objetivos documentais"
  }`;

  if (!Number.isSafeInteger(processDefinitionId) || processDefinitionId <= 0) {
    return <div className="alert alert-danger">Processo inválido.</div>;
  }

  return (
    <div className="business-process-documents-page">
      <header className="business-process-documents-toolbar mb-4">
        <div>
          <PageTitle>{screenTitle}</PageTitle>
          <p className="text-body-secondary mb-0">
            Últimas 10 execuções documentadas
          </p>
        </div>
        <Link className="btn btn-outline-primary" to={backPath}>
          <ArrowLeft size={17} aria-hidden="true" />
          Voltar ao BPM
        </Link>
      </header>

      {documents.isLoading || processes.isLoading ? (
        <div
          className="business-process-documents-loading"
          aria-label="Carregando documentos"
        >
          <span className="spinner-border text-primary" aria-hidden="true" />
        </div>
      ) : null}
      {documents.isError ? (
        <div className="alert alert-danger" role="alert">
          Não foi possível consultar os documentos desta atividade.
        </div>
      ) : null}
      {!documents.isLoading && !documents.isError ? (
        <section aria-label="Documentos mais recentes da atividade">
          <div className="d-grid gap-3">
            {(documents.data ?? []).map((document, index) => (
              <details
                className="card business-process-document"
                key={document.taskId}
                open={index === 0}
              >
                <summary className="card-header">
                  <FileText size={18} aria-hidden="true" />
                  <span className="flex-grow-1">
                    <strong>{document.title}</strong>
                    <small className="d-block text-body-secondary mt-1">
                      Tarefa #{document.taskId} ·{" "}
                      {document.assignedAgentNickname} · encerrada em{" "}
                      {formattedDateTime(document.finishedAt)}
                    </small>
                  </span>
                </summary>
                <div className="card-body">
                  <dl className="business-process-document__audit mb-3">
                    <div>
                      <dt>Origem</dt>
                      <dd>{document.sourceReference ?? "Não informada"}</dd>
                    </div>
                    <div>
                      <dt>Início</dt>
                      <dd>{formattedDateTime(document.startedAt)}</dd>
                    </div>
                    <div>
                      <dt>Término</dt>
                      <dd>{formattedDateTime(document.finishedAt)}</dd>
                    </div>
                    <div>
                      <dt>Duração</dt>
                      <dd>
                        {formattedDuration(
                          document.startedAt,
                          document.finishedAt,
                        )}
                      </dd>
                    </div>
                    <div>
                      <dt>Modo de execução</dt>
                      <dd>{document.executionMode ?? "Não registrado"}</dd>
                    </div>
                    <div>
                      <dt>Modelo ou identificador</dt>
                      <dd>{document.modelCode ?? "Não registrado"}</dd>
                    </div>
                    <div>
                      <dt>Raciocínio</dt>
                      <dd>{document.reasoningEffort ?? "Não registrado"}</dd>
                    </div>
                    <div>
                      <dt>Produto interno</dt>
                      <dd>{document.productInternalName ?? "Não vinculado"}</dd>
                    </div>
                    <div>
                      <dt>Tokens</dt>
                      <dd>
                        entrada {document.inputTokens ?? "—"} · cache{" "}
                        {document.cachedInputTokens ?? "—"} · saída{" "}
                        {document.outputTokens ?? "—"}
                      </dd>
                    </div>
                    <div>
                      <dt>Custo estimado</dt>
                      <dd>
                        {document.estimatedCostUsd === undefined ||
                        document.estimatedCostUsd === null
                          ? "Indisponível"
                          : `US$ ${Number(document.estimatedCostUsd).toFixed(8)}`}
                      </dd>
                    </div>
                  </dl>
                  <h2 className="h6">
                    {document.executionMode === "DETERMINISTIC"
                      ? "Entrada integral da execução determinística"
                      : document.executionMode === "NOT_STARTED"
                        ? "Modelo não iniciado"
                        : "Prompt enviado ao modelo"}
                  </h2>
                  <StructuredExecutionContent
                    value={document.promptSent}
                    emptyText={
                      document.executionMode === "NOT_STARTED"
                        ? "A execução foi interrompida antes de enviar um prompt ao modelo."
                        : "Prompt não registrado nesta execução legada."
                    }
                  />
                  {document.assignedAgentKey === "customer-agent" ||
                  (document.accessedUrls ?? []).length > 0 ? (
                    <section aria-label="URLs acessadas pelo agente">
                      <h2 className="h6 mt-3">
                        {document.assignedAgentKey === "customer-agent"
                          ? "URLs acessadas por Psique"
                          : "URLs acessadas pelo agente"}
                      </h2>
                      {(document.accessedUrls ?? []).length > 0 ? (
                        <ul className="text-break">
                          {(document.accessedUrls ?? []).map((link) => (
                            <li key={`${link.url}-${link.accessedAt ?? ""}`}>
                              <div>
                                <a
                                  href={link.url}
                                  target="_blank"
                                  rel="noreferrer"
                                >
                                  {link.label}
                                </a>
                                {link.accessMethod
                                  ? ` · ${link.accessMethod}`
                                  : ""}
                              </div>
                              <div className="small text-body-secondary text-break">
                                {link.url}
                              </div>
                            </li>
                          ))}
                        </ul>
                      ) : (
                        <p className="text-body-secondary">
                          Nenhuma URL foi aberta por Psique nesta execução.
                        </p>
                      )}
                    </section>
                  ) : null}
                  <h2 className="h6">Documento gerado</h2>
                  <StructuredExecutionContent value={document.resultJson} />
                  <details className="mt-3">
                    <summary className="fw-semibold">Evidências</summary>
                    <div className="mt-2">
                      <StructuredExecutionContent
                        value={document.evidenceJson}
                      />
                    </div>
                  </details>
                </div>
              </details>
            ))}
          </div>
          {(documents.data ?? []).length === 0 ? (
            <div className="business-process-documents-empty">
              <FileText size={32} aria-hidden="true" />
              <strong>Nenhum documento concluído</strong>
              <span>Esta atividade ainda não possui documento concluído.</span>
            </div>
          ) : null}
        </section>
      ) : null}
    </div>
  );
}
