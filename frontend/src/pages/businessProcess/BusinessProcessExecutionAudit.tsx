import type { BusinessProcessActivityExecution } from "../../api/businessProcess/types";
import {
  formattedDateTime,
  formattedDuration,
} from "./BusinessProcessExecutionPresentation";
import PromptAuditCards from "./PromptAuditCards";
import "./BusinessProcessesPage.css";

export type BusinessProcessExecutionAuditData = Pick<
  BusinessProcessActivityExecution,
  | "sourceReference"
  | "productInternalName"
  | "processVersionNumber"
  | "status"
  | "createdAt"
  | "startedAt"
  | "finishedAt"
  | "executionMode"
  | "modelCode"
  | "reasoningEffort"
  | "inputTokens"
  | "cachedInputTokens"
  | "outputTokens"
  | "estimatedCostUsd"
  | "assignedAgentNickname"
  | "promptSent"
  | "agentPromptPart"
  | "activityPromptPart"
>;

type BusinessProcessExecutionAuditProps = {
  execution: BusinessProcessExecutionAuditData;
  headingLevel?: "h2" | "h3" | "h4";
};

/** Exibe metadados e prompt persistidos de uma tarefa sem inferir dados no frontend. */
export default function BusinessProcessExecutionAudit({
  execution,
  headingLevel = "h2",
}: BusinessProcessExecutionAuditProps) {
  return (
    <>
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
          <dt>Modo de execução</dt>
          <dd>{execution.executionMode ?? "Não registrado"}</dd>
        </div>
        <div>
          <dt>Modelo ou identificador</dt>
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

      <PromptAuditCards
        executionMode={execution.executionMode}
        agentNickname={execution.assignedAgentNickname}
        agentPromptPart={execution.agentPromptPart}
        activityPromptPart={execution.activityPromptPart}
        promptSent={execution.promptSent}
        headingLevel={headingLevel}
      />
    </>
  );
}
