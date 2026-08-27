import { Bot } from "lucide-react";
import type { BusinessProcessActivityExecution } from "../../api/businessProcess/types";
import {
  formattedDateTime,
  formattedDuration,
  StructuredExecutionContent,
} from "./BusinessProcessExecutionPresentation";

type BusinessProcessExecutionCardProps = {
  execution: BusinessProcessActivityExecution;
  defaultOpen?: boolean;
  contentHeadingLevel?: "h2" | "h3";
};

/** Exibe a auditoria completa de uma tarefa real em um cartão reutilizável. */
export default function BusinessProcessExecutionCard({
  execution,
  defaultOpen = false,
  contentHeadingLevel = "h2",
}: BusinessProcessExecutionCardProps) {
  const ContentHeading = contentHeadingLevel;
  return (
    <details className="card business-process-document" open={defaultOpen}>
      <summary className="card-header">
        <Bot size={18} aria-hidden="true" />
        <span className="flex-grow-1">
          <strong>{execution.title}</strong>
          <small className="d-block text-body-secondary mt-1">
            Tarefa #{execution.taskId} · {execution.assignedAgentNickname} · v
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
            <dd>{execution.productInternalName ?? "Não vinculado"}</dd>
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
              {formattedDuration(execution.startedAt, execution.finishedAt)}
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

        <ContentHeading className="h6">
          Prompt recebido por {execution.assignedAgentNickname}
        </ContentHeading>
        <StructuredExecutionContent
          value={execution.promptSent}
          emptyText="Prompt não registrado nesta execução legada."
        />

        <ContentHeading className="h6 mt-3">
          Comentários de {execution.assignedAgentNickname}
        </ContentHeading>
        <StructuredExecutionContent
          value={execution.comments}
          emptyText="Nenhum comentário registrado."
        />

        {execution.executionError ? (
          <div className="alert alert-danger mt-3 mb-0">
            <strong>Falha técnica</strong>
            <StructuredExecutionContent value={execution.executionError} />
          </div>
        ) : null}

        <details className="mt-3">
          <summary className="fw-semibold">Evidências</summary>
          <div className="mt-2">
            <StructuredExecutionContent value={execution.evidenceJson} />
          </div>
        </details>
      </div>
    </details>
  );
}
