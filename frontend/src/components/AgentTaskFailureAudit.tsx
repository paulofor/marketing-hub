import type { AgentTaskFailureAudit as FailureAudit } from "../api/agentTask/types";

function readableJson(value?: string) {
  if (!value) return "Nenhuma saída produzida antes da falha.";
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

/** Exibe o log governado que permite reconstruir uma falha sem abrir logs técnicos. */
export default function AgentTaskFailureAudit({
  audit,
}: {
  audit?: FailureAudit;
}) {
  if (!audit) return null;
  const complete = audit.readiness === "COMPLETE";
  const context = [
    audit.sourceReference,
    audit.processCode,
    audit.activityName || audit.activityId,
  ]
    .filter(Boolean)
    .join(" · ");

  return (
    <details className="border rounded p-2 mb-2">
      <summary className="d-flex flex-wrap align-items-center gap-2">
        <strong>Log governado da falha</strong>
        <span
          className={`badge ${complete ? "text-bg-success" : "text-bg-warning"}`}
        >
          {complete ? "Reconstruível" : "Parcial"}
        </span>
      </summary>
      <div className="small mt-3 d-grid gap-2">
        <div>
          <strong>Trabalho pretendido:</strong> {audit.intendedWork}
        </div>
        {audit.blockerGuidance ? (
          <div className="alert alert-warning mb-0">
            <strong>O que fazer:</strong>{" "}
            {audit.blockerGuidance.recommendedAction}
            <ul className="mb-0 mt-1">
              {audit.blockerGuidance.helpLinks.map((link) => (
                <li key={link.url}>
                  <a href={link.url} target="_blank" rel="noreferrer">
                    {link.label}
                  </a>
                </li>
              ))}
            </ul>
          </div>
        ) : null}
        {(audit.accessedUrls ?? []).length > 0 ? (
          <div>
            <strong>URLs acessadas:</strong>
            <ul className="mb-0 text-break">
              {(audit.accessedUrls ?? []).map((link) => (
                <li key={`${link.url}-${link.accessedAt ?? ""}`}>
                  <div>
                    <a href={link.url} target="_blank" rel="noreferrer">
                      {link.label}
                    </a>
                    {link.accessMethod ? ` · ${link.accessMethod}` : ""}
                  </div>
                  <div className="text-body-secondary text-break">
                    {link.url}
                  </div>
                </li>
              ))}
            </ul>
          </div>
        ) : null}
        {context ? (
          <div>
            <strong>Contexto:</strong> {context}
          </div>
        ) : null}
        {audit.authorityPolicy ? (
          <div>
            <strong>Limite de autoridade:</strong> {audit.authorityPolicy}
          </div>
        ) : null}
        <div>
          <strong>Causa da falha/bloqueio:</strong>{" "}
          {audit.error || "Não informada pelo executor."}
        </div>
        {audit.missingEvidence.length > 0 ? (
          <div className="text-warning-emphasis">
            <strong>Faltou registrar:</strong>{" "}
            {audit.missingEvidence.join(", ")}.
          </div>
        ) : null}
        <details>
          <summary>Evidências e acessos preservados</summary>
          <pre className="small text-break text-wrap mb-0 mt-1">
            {readableJson(audit.accessedEvidenceJson)}
          </pre>
        </details>
        <details>
          <summary>Saída produzida antes da falha</summary>
          <pre className="small text-break text-wrap mb-0 mt-1">
            {readableJson(audit.producedOutputJson)}
          </pre>
        </details>
      </div>
    </details>
  );
}
