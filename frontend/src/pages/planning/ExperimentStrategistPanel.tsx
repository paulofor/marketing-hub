import { useState } from "react";
import {
  useExperimentStrategistExecutions,
  useStartExperimentStrategist,
} from "../../api/planning/useExperimentStrategist";
import CodexExecutionTelemetry from "../../components/CodexExecutionTelemetry";

function parse(value?: string | null) {
  if (!value) return null;
  try {
    return JSON.parse(value) as unknown;
  } catch {
    return value;
  }
}

export default function ExperimentStrategistPanel({
  planId,
  defaultQuestion,
}: {
  planId: number;
  defaultQuestion?: string | null;
}) {
  const [question, setQuestion] = useState(
    defaultQuestion ||
      "Qual oferta tem maior potencial de gerar vendas com as evidências atuais?",
  );
  const executions = useExperimentStrategistExecutions(planId);
  const start = useStartExperimentStrategist(planId);

  return (
    <section
      className="card border-0 shadow-sm mt-4"
      data-testid="experiment-strategist-panel"
    >
      <div className="card-body d-grid gap-3">
        <div>
          <div className="d-flex align-items-center gap-2 flex-wrap">
            <h2 className="h5 mb-0">Agente Estrategista</h2>
            <span className="badge text-bg-success">Somente leitura</span>
          </div>
          <p className="text-muted mb-0 mt-1">
            Pesquisa três alternativas, compara potencial comercial, risco e
            esforço e recomenda um experimento mensurável.
          </p>
        </div>
        <div>
          <label
            className="form-label"
            htmlFor="experiment-strategist-question"
          >
            Pergunta comercial <span className="text-danger">*</span>
          </label>
          <textarea
            id="experiment-strategist-question"
            className="form-control"
            rows={3}
            value={question}
            onChange={(event) => setQuestion(event.target.value)}
          />
        </div>
        <div>
          <button
            className="btn btn-outline-primary"
            type="button"
            disabled={start.isPending || !question.trim()}
            onClick={() => start.mutate(question.trim())}
          >
            {start.isPending ? (
              <>
                <span className="spinner-border spinner-border-sm me-2" />
                Solicitando...
              </>
            ) : (
              "Solicitar parecer estratégico"
            )}
          </button>
          {start.isError ? (
            <span className="text-danger ms-2">
              Não foi possível solicitar o parecer.
            </span>
          ) : null}
        </div>
        {(executions.data ?? []).map((execution) => (
          <article className="border rounded p-3" key={execution.id}>
            <div className="d-flex justify-content-between gap-2 flex-wrap">
              <strong>Pesquisa #{execution.id}</strong>
              <span className="badge text-bg-secondary">
                {execution.status}
              </span>
            </div>
            <p className="mb-2 mt-2">
              <strong>Pergunta:</strong> {execution.researchQuestion}
            </p>
            <CodexExecutionTelemetry
              agentType="EXPERIMENT_STRATEGIST"
              executionId={execution.id}
            />
            {execution.recommendationJson ? (
              <div className="alert alert-success mb-2">
                <strong>Recomendação</strong>
                <pre className="mb-0 mt-1 text-wrap">
                  {JSON.stringify(parse(execution.recommendationJson), null, 2)}
                </pre>
              </div>
            ) : null}
            {execution.alternativesJson ? (
              <details className="mb-2">
                <summary>Ver três alternativas</summary>
                <pre className="text-wrap mt-2">
                  {JSON.stringify(parse(execution.alternativesJson), null, 2)}
                </pre>
              </details>
            ) : null}
            {execution.publicSourcesJson ? (
              <details className="mb-2">
                <summary>Ver fontes públicas</summary>
                <pre className="text-wrap mt-2">
                  {JSON.stringify(parse(execution.publicSourcesJson), null, 2)}
                </pre>
              </details>
            ) : null}
            {execution.errorMessage ? (
              <details className="text-danger">
                <summary>Ver causa detalhada da falha</summary>
                <pre className="text-wrap mt-2">{execution.errorMessage}</pre>
              </details>
            ) : null}
          </article>
        ))}
      </div>
    </section>
  );
}
