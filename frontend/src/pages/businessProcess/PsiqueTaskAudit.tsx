import type {
  AgentTaskVisualEvidence,
  PsiquePurchaseEmotion,
  PsiqueVisualAudit,
} from "../../api/agentTask/types";
import { formattedDateTime } from "./BusinessProcessExecutionPresentation";

type PsiqueTaskAuditProps = {
  assignedAgentKey: string;
  visualEvidence?: AgentTaskVisualEvidence[];
  visualAudit?: PsiqueVisualAudit;
  purchaseEmotion?: PsiquePurchaseEmotion;
  headingLevel?: "h2" | "h3";
};

/** Formata o peso do snapshot sem esconder o valor auditável. */
function formattedBytes(value: number) {
  if (value < 1024) return `${value} bytes`;
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
  return `${(value / (1024 * 1024)).toFixed(1)} MB`;
}

/** Ordena a visão completa antes das dobras e preserva a sequência da jornada. */
function orderedEvidence(values: AgentTaskVisualEvidence[]) {
  return [...values].sort((left, right) => {
    if (left.pageNumber !== right.pageNumber) {
      return left.pageNumber - right.pageNumber;
    }
    if (left.evidenceType !== right.evidenceType) {
      return left.evidenceType === "FULL_PAGE" ? -1 : 1;
    }
    return (left.foldNumber ?? 0) - (right.foldNumber ?? 0);
  });
}

/** Expõe pixels, análise por dobra e antecipação emocional registradas por Psique. */
export default function PsiqueTaskAudit({
  assignedAgentKey,
  visualEvidence = [],
  visualAudit,
  purchaseEmotion,
  headingLevel = "h2",
}: PsiqueTaskAuditProps) {
  if (
    assignedAgentKey !== "customer-agent" &&
    visualEvidence.length === 0 &&
    !visualAudit &&
    !purchaseEmotion
  ) {
    return null;
  }
  const Heading = headingLevel;
  const foldAnalysisById = new Map(
    (visualAudit?.foldAnalyses ?? []).map((analysis) => [
      analysis.artifactId,
      analysis,
    ]),
  );

  return (
    <>
      <section className="mt-3" aria-label="Antecipação emocional de Psique">
        <Heading className="h6">Antes e depois imaginados pela cliente</Heading>
        {purchaseEmotion ? (
          <dl className="business-process-document__audit mb-0">
            <div>
              <dt>Expectativa ao adquirir</dt>
              <dd>{purchaseEmotion.acquisitionExpectation}</dd>
            </div>
            <div>
              <dt>Ansiedade antes da compra</dt>
              <dd>{purchaseEmotion.acquisitionAnxiety}</dd>
            </div>
            <div>
              <dt>Como imagina se sentir depois</dt>
              <dd>{purchaseEmotion.expectedPostDeliveryFeeling}</dd>
            </div>
            <div>
              <dt>Tensão emocional</dt>
              <dd>{purchaseEmotion.emotionalTension}</dd>
            </div>
            <div>
              <dt>Limite da evidência</dt>
              <dd>{purchaseEmotion.evidenceBoundary}</dd>
            </div>
          </dl>
        ) : (
          <p className="mb-0 text-body-secondary">
            Esta execução legada não registrou expectativa, ansiedade e sensação
            pós-entrega.
          </p>
        )}
      </section>

      <section
        className="mt-3"
        aria-label="Prova visual e análise por dobra de Psique"
      >
        <Heading className="h6">Snapshots mobile e análise por dobra</Heading>
        {visualEvidence.length > 0 ? (
          <>
            <div className="row g-3">
              {orderedEvidence(visualEvidence).map((evidence) => {
                const analysis = foldAnalysisById.get(evidence.id);
                return (
                  <article className="col-12 col-lg-6" key={evidence.id}>
                    <div className="card h-100">
                      <a
                        href={evidence.contentUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="d-block bg-body-tertiary text-center"
                      >
                        <img
                          src={evidence.contentUrl}
                          alt={`Snapshot de Psique — ${evidence.label}`}
                          className="img-fluid rounded-top psique-visual-evidence__image"
                          loading="lazy"
                        />
                      </a>
                      <div className="card-body">
                        <strong>{evidence.label}</strong>
                        <p className="small text-body-secondary mb-2">
                          {evidence.deviceProfile} · {evidence.viewportWidth}×
                          {evidence.viewportHeight}px · página{" "}
                          {evidence.pageHeightPx}px · rolagem {evidence.scrollY}
                          px · {formattedBytes(evidence.sizeBytes)}
                          <br />
                          Capturado em {formattedDateTime(
                            evidence.capturedAt,
                          )}{" "}
                          · SHA-256 <code>{evidence.sha256.slice(0, 12)}…</code>
                        </p>
                        {analysis ? (
                          <dl className="mb-0">
                            <dt>Estética</dt>
                            <dd>{analysis.aestheticAssessment}</dd>
                            <dt>Hierarquia visual</dt>
                            <dd>{analysis.visualHierarchy}</dd>
                            <dt>Legibilidade</dt>
                            <dd>{analysis.legibility}</dd>
                            <dt>Emoção evocada</dt>
                            <dd>{analysis.emotionEvoked}</dd>
                            <dt>CTA nesta dobra</dt>
                            <dd className="mb-0">{analysis.ctaVisibility}</dd>
                          </dl>
                        ) : evidence.evidenceType === "FOLD" ? (
                          <p className="alert alert-warning mb-0 py-2">
                            Esta dobra não recebeu análise estética registrada.
                          </p>
                        ) : null}
                      </div>
                    </div>
                  </article>
                );
              })}
            </div>
            {visualAudit ? (
              <dl className="alert alert-info business-process-document__audit mt-3 mb-0">
                <div>
                  <dt>Avaliação estética geral</dt>
                  <dd>{visualAudit.overallAestheticAssessment}</dd>
                </div>
                <div>
                  <dt>Continuidade da página completa</dt>
                  <dd>{visualAudit.fullPageContinuity}</dd>
                </div>
                <div>
                  <dt>Captura mobile-first</dt>
                  <dd>
                    {visualAudit.mobileFirst ? "confirmada" : "não confirmada"}
                  </dd>
                </div>
              </dl>
            ) : (
              <p className="alert alert-warning mt-3 mb-0">
                Há snapshots, mas a análise visual estruturada não foi
                registrada.
              </p>
            )}
          </>
        ) : (
          <p className="mb-0 text-body-secondary">
            Esta execução não possui snapshots persistidos por Psique.
          </p>
        )}
      </section>
    </>
  );
}
