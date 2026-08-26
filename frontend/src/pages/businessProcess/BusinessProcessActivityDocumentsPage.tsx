import { type ReactNode } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, FileText } from "lucide-react";
import {
  useBusinessProcessActivityDocuments,
  useBusinessProcessDocuments,
} from "../../api/businessProcess/useBusinessProcessDocuments";
import { useBusinessProcesses } from "../../api/businessProcess/useBusinessProcesses";
import PageTitle from "../../components/PageTitle";
import "./BusinessProcessesPage.css";

/** Formata a data vinda do backend sem alterar sua semântica operacional. */
function formattedDateTime(value?: string) {
  if (!value) return "Não informado";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Não informado";
  return date.toLocaleString("pt-BR");
}

/** Calcula uma duração legível somente quando o backend informou os dois marcos da execução. */
function formattedDuration(startedAt?: string, finishedAt?: string) {
  if (!startedAt || !finishedAt) return "Não informado";
  const durationMs = new Date(finishedAt).getTime() - new Date(startedAt).getTime();
  if (!Number.isFinite(durationMs) || durationMs < 0) return "Não informado";
  const seconds = Math.floor(durationMs / 1000);
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const remainingSeconds = seconds % 60;
  return [
    hours ? `${hours}h` : null,
    minutes ? `${minutes}min` : null,
    `${remainingSeconds}s`,
  ]
    .filter(Boolean)
    .join(" ");
}

/** Renderiza valores simples de um JSON na árvore expansível. */
function JsonValue({ value }: { value: unknown }) {
  if (value === null) return <span className="business-process-json-tree__value">nulo</span>;
  if (typeof value === "string") {
    return <span className="business-process-json-tree__value">&quot;{value}&quot;</span>;
  }
  return <span className="business-process-json-tree__value">{String(value)}</span>;
}

/** Exibe objetos e listas JSON de forma progressiva, sem esconder o conteúdo original. */
function JsonTree({ value, level = 0 }: { value: unknown; level?: number }): ReactNode {
  if (value === null || typeof value !== "object") return <JsonValue value={value} />;
  const entries = Array.isArray(value)
    ? value.map((item, index) => [String(index), item] as const)
    : Object.entries(value);
  return (
    <details className="business-process-json-tree" open={level === 0}>
      <summary>{Array.isArray(value) ? `Lista (${entries.length})` : `Objeto (${entries.length})`}</summary>
      <ul>
        {entries.map(([key, nestedValue]) => (
          <li key={key}>
            <strong>{key}:</strong> <JsonTree value={nestedValue} level={level + 1} />
          </li>
        ))}
      </ul>
    </details>
  );
}

/** Mostra JSON como árvore sob demanda e preserva texto puro das execuções legadas. */
function DocumentContent({ value }: { value?: string }) {
  if (!value) return <span className="text-body-secondary">Não informado.</span>;
  try {
    const parsed = JSON.parse(value);
    return (
      <details className="business-process-json-viewer">
        <summary>Visualizar JSON em árvore</summary>
        <div className="business-process-json-viewer__tree">
          <JsonTree value={parsed} />
        </div>
      </details>
    );
  } catch {
    return <pre className="business-process-document__content">{value}</pre>;
  }
}

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
    !activityId && Number.isSafeInteger(processDefinitionId) && processDefinitionId > 0
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
    activityId ? activity?.label ?? activityId : "Objetivos documentais"
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
                      Tarefa #{document.taskId} · {document.assignedAgentNickname} ·{" "}
                      encerrada em {formattedDateTime(document.finishedAt)}
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
                      <dd>{formattedDuration(document.startedAt, document.finishedAt)}</dd>
                    </div>
                    <div>
                      <dt>Modelo utilizado</dt>
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
                  <h2 className="h6">Prompt enviado ao modelo</h2>
                  <DocumentContent value={document.promptSent} />
                  <h2 className="h6">Documento gerado</h2>
                  <DocumentContent value={document.resultJson} />
                  <details className="mt-3">
                    <summary className="fw-semibold">Evidências</summary>
                    <div className="mt-2">
                      <DocumentContent value={document.evidenceJson} />
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
              <span>
                Esta atividade ainda não possui documento concluído.
              </span>
            </div>
          ) : null}
        </section>
      ) : null}
    </div>
  );
}
