import { Bot } from "lucide-react";
import { useState } from "react";
import { useProductProcessTaskAudit } from "../../api/businessProcess/useProductProcessActivityExecutions";
import type { BusinessProcessActivityExecution } from "../../api/businessProcess/types";
import BusinessProcessExecutionAudit from "./BusinessProcessExecutionAudit";
import {
  formattedDateTime,
  StructuredExecutionContent,
} from "./BusinessProcessExecutionPresentation";
import PsiqueTaskAudit from "./PsiqueTaskAudit";
import type { TaskPromptAuditRequest } from "./DeferredTaskPromptAudit";

type BusinessProcessExecutionCardProps = {
  execution: BusinessProcessActivityExecution;
  defaultOpen?: boolean;
  contentHeadingLevel?: "h2" | "h3";
  promptAudit?: TaskPromptAuditRequest;
  auditRequest?: {
    url: string;
  };
};

/** Exibe a auditoria completa de uma tarefa real em um cartão reutilizável. */
export default function BusinessProcessExecutionCard({
  execution: initialExecution,
  defaultOpen = false,
  contentHeadingLevel = "h2",
  promptAudit,
  auditRequest,
}: BusinessProcessExecutionCardProps) {
  const [expanded, setExpanded] = useState(defaultOpen);
  const audit = useProductProcessTaskAudit(auditRequest?.url, expanded);
  const execution = audit.data ?? initialExecution;
  const ContentHeading = contentHeadingLevel;
  const guidance = execution.blockerGuidance;
  const functionalBlock =
    guidance != null && guidance.category !== "TECHNICAL_FAILURE";
  return (
    <details
      className="card business-process-document"
      open={expanded}
      onToggle={(event) => setExpanded(event.currentTarget.open)}
    >
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
        {auditRequest && expanded && audit.isFetching ? (
          <p className="mb-0" role="status">
            Carregando as provas e a auditoria desta tarefa...
          </p>
        ) : null}
        {auditRequest && expanded && audit.isError ? (
          <div className="alert alert-danger mb-0" role="alert">
            Não foi possível carregar a auditoria desta tarefa.
            <button
              type="button"
              className="btn btn-outline-danger btn-sm ms-2"
              onClick={() => void audit.refetch()}
            >
              Tentar novamente
            </button>
          </div>
        ) : null}
        {!(auditRequest && expanded && (audit.isFetching || audit.isError)) ? (
          <>
            <BusinessProcessExecutionAudit
              execution={execution}
              headingLevel={contentHeadingLevel}
              promptAudit={promptAudit}
            />

            <ContentHeading className="h6 mt-3">
              Comentários de {execution.assignedAgentNickname}
            </ContentHeading>
            <StructuredExecutionContent
              value={execution.comments}
              emptyText="Nenhum comentário registrado."
            />

            {execution.executionError || guidance ? (
              <div
                className={`alert ${functionalBlock ? "alert-warning" : "alert-danger"} mt-3 mb-0`}
              >
                <strong>
                  {functionalBlock ? "Avanço bloqueado" : "Falha técnica"}
                </strong>
                {execution.executionError ? (
                  <StructuredExecutionContent
                    value={execution.executionError}
                  />
                ) : null}
                {guidance ? (
                  <div className="mt-2">
                    <strong>O que fazer:</strong> {guidance.recommendedAction}
                    <ul className="mb-0 mt-2">
                      {guidance.helpLinks.map((link) => (
                        <li key={link.url}>
                          <a href={link.url} target="_blank" rel="noreferrer">
                            {link.label}
                          </a>
                        </li>
                      ))}
                    </ul>
                  </div>
                ) : null}
              </div>
            ) : null}

            {execution.assignedAgentKey === "customer-agent" ||
            (execution.accessedUrls ?? []).length > 0 ? (
              <section className="mt-3" aria-label="URLs acessadas pelo agente">
                <ContentHeading className="h6">
                  {execution.assignedAgentKey === "customer-agent"
                    ? "URLs acessadas por Psique"
                    : "URLs acessadas pelo agente"}
                </ContentHeading>
                {(execution.accessedUrls ?? []).length > 0 ? (
                  <ul className="mb-0 text-break">
                    {(execution.accessedUrls ?? []).map((link) => (
                      <li key={`${link.url}-${link.accessedAt ?? ""}`}>
                        <div>
                          <a href={link.url} target="_blank" rel="noreferrer">
                            {link.label}
                          </a>
                          {link.accessMethod ? ` · ${link.accessMethod}` : ""}
                          {link.accessedAt
                            ? ` · ${formattedDateTime(link.accessedAt)}`
                            : ""}
                        </div>
                        <div className="small text-body-secondary text-break">
                          {link.url}
                        </div>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="mb-0 text-body-secondary">
                    Nenhuma URL foi aberta por Psique nesta execução.
                  </p>
                )}
              </section>
            ) : null}

            <PsiqueTaskAudit
              assignedAgentKey={execution.assignedAgentKey}
              visualEvidence={execution.visualEvidence}
              visualAudit={execution.visualAudit}
              purchaseEmotion={execution.purchaseEmotion}
              headingLevel={contentHeadingLevel}
            />

            <details className="mt-3">
              <summary className="fw-semibold">Evidências</summary>
              <div className="mt-2">
                <StructuredExecutionContent value={execution.evidenceJson} />
              </div>
            </details>
          </>
        ) : null}
      </div>
    </details>
  );
}
