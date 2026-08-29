import { StructuredExecutionContent } from "./BusinessProcessExecutionPresentation";

type PromptAuditCardsProps = {
  executionMode?: "MODEL" | "DETERMINISTIC" | "NOT_STARTED";
  agentNickname?: string;
  agentPromptPart?: string;
  activityPromptPart?: string;
  promptSent?: string;
  headingLevel?: "h2" | "h3";
};

/** Apresenta as duas partes persistidas e o prompt integral sem inferir conteúdo no frontend. */
export default function PromptAuditCards({
  executionMode,
  agentNickname,
  agentPromptPart,
  activityPromptPart,
  promptSent,
  headingLevel = "h2",
}: PromptAuditCardsProps) {
  const Heading = headingLevel;
  const notStarted = executionMode === "NOT_STARTED";
  const deterministic = executionMode === "DETERMINISTIC";
  const legacyPartText = notStarted
    ? "A execução foi interrompida antes de enviar um prompt ao modelo."
    : "Parte não registrada nesta execução legada.";
  const completeTitle = deterministic
    ? "Entrada integral da execução determinística"
    : notStarted
      ? "Modelo não iniciado"
      : `Prompt completo enviado ao modelo${agentNickname ? ` por ${agentNickname}` : ""}`;

  return (
    <section
      className="business-process-prompt-audit"
      aria-label="Auditoria do prompt"
    >
      <div className="row g-3">
        <div className="col-12 col-xl-6">
          <article className="card h-100 business-process-prompt-card">
            <div className="card-body">
              <Heading className="h6 card-title">Parte do agente</Heading>
              <StructuredExecutionContent
                value={agentPromptPart}
                emptyText={
                  deterministic
                    ? "Não se aplica à execução determinística."
                    : legacyPartText
                }
              />
            </div>
          </article>
        </div>
        <div className="col-12 col-xl-6">
          <article className="card h-100 business-process-prompt-card">
            <div className="card-body">
              <Heading className="h6 card-title">Parte da atividade</Heading>
              <StructuredExecutionContent
                value={activityPromptPart}
                emptyText={legacyPartText}
              />
            </div>
          </article>
        </div>
        <div className="col-12">
          <article className="card business-process-prompt-card">
            <div className="card-body">
              <Heading className="h6 card-title">{completeTitle}</Heading>
              <StructuredExecutionContent
                value={promptSent}
                emptyText={
                  notStarted
                    ? "A execução foi interrompida antes de enviar um prompt ao modelo."
                    : "Prompt não registrado nesta execução legada."
                }
              />
            </div>
          </article>
        </div>
      </div>
    </section>
  );
}
