import { Bot } from "lucide-react";
import type { BusinessProcessActivityExecution } from "../../api/businessProcess/types";
import BusinessProcessExecutionAudit from "./BusinessProcessExecutionAudit";
import {
  formattedDateTime,
  StructuredExecutionContent,
} from "./BusinessProcessExecutionPresentation";
import PsiqueTaskAudit from "./PsiqueTaskAudit";

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
  const guidance = execution.blockerGuidance;
  const functionalBlock =
    guidance != null && guidance.category !== "TECHNICAL_FAILURE";
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
        <BusinessProcessExecutionAudit
          execution={execution}
          headingLevel={contentHeadingLevel}
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
              <StructuredExecutionContent value={execution.executionError} />
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
      </div>
    </details>
  );
}
