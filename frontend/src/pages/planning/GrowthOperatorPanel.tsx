import { useState } from "react";
import {
  GrowthOperatorExecution,
  useGrowthOperatorExecutions,
  useStartGrowthOperator,
} from "../../api/planning/useGrowthOperator";

function statusLabel(execution: GrowthOperatorExecution) {
  const labels = {
    PENDING: "Na fila",
    RUNNING: "Analisando",
    COMPLETED: "Concluído",
    FAILED: "Falhou",
  };
  return labels[execution.status];
}

function formatReportDateTime(value?: string | null) {
  if (!value) return "Horário não informado";
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "medium",
    timeZone: "America/Sao_Paulo",
  }).format(new Date(value));
}

function sessionEvidence(snapshot?: string | null) {
  if (!snapshot) return null;
  try {
    const parsed = JSON.parse(snapshot);
    const intelligence = parsed.sessionIntelligence;
    if (!intelligence || intelligence.available === false) return null;
    const landing = intelligence.landingAnalytics ?? intelligence;
    const pdeJourneys =
      intelligence.pdeAnalytics?.detailedJourneys?.length ?? 0;
    return {
      data: intelligence,
      label: `${landing.includedEvents ?? 0} de ${landing.totalEventsAvailable ?? 0} eventos de landing e ${pdeJourneys} jornadas PDE`,
    };
  } catch {
    return null;
  }
}

export default function GrowthOperatorPanel({
  planId,
  defaultObjective,
}: {
  planId: number;
  defaultObjective?: string | null;
}) {
  const [objective, setObjective] = useState(defaultObjective ?? "");
  const executionsQuery = useGrowthOperatorExecutions(planId);
  const start = useStartGrowthOperator(planId);
  const executions = executionsQuery.data ?? [];

  return (
    <section
      className="card border-0 shadow-sm mt-4"
      data-testid="growth-operator-panel"
    >
      <div className="card-body d-grid gap-3">
        <div>
          <div className="d-flex align-items-center gap-2 flex-wrap">
            <h2 className="h5 mb-0">Operador de Crescimento</h2>
            <span className="badge text-bg-success">Somente leitura</span>
          </div>
          <p className="text-muted mb-0 mt-1">
            O Codex investiga a meta e o gargalo em sandbox, compara três
            caminhos e registra uma recomendação. Nenhuma ação comercial é
            executada.
          </p>
        </div>

        <div>
          <label className="form-label" htmlFor="growth-operator-objective">
            Objetivo do diagnóstico <span className="text-danger">*</span>
          </label>
          <textarea
            id="growth-operator-objective"
            className="form-control"
            rows={3}
            value={objective}
            onChange={(event) => setObjective(event.target.value)}
          />
        </div>

        <div className="d-flex align-items-center gap-3 flex-wrap">
          <button
            type="button"
            className="btn btn-outline-primary"
            disabled={start.isPending || objective.trim().length === 0}
            onClick={() => start.mutate({ weekNumber: 1, objective })}
          >
            {start.isPending ? (
              <>
                <span className="spinner-border spinner-border-sm me-2" />
                Solicitando...
              </>
            ) : (
              "Executar diagnóstico seguro"
            )}
          </button>
          {start.isError ? (
            <span className="text-danger">
              Não foi possível solicitar o diagnóstico.
            </span>
          ) : null}
          {start.isSuccess ? (
            <span className="text-success">Diagnóstico colocado na fila.</span>
          ) : null}
        </div>

        {executionsQuery.isError ? (
          <div className="alert alert-danger mb-0">
            Não foi possível carregar os diagnósticos.
          </div>
        ) : null}

        {executions.map((execution) => {
          const evidence = sessionEvidence(execution.evidenceSnapshot);
          return (
            <article className="border rounded p-3" key={execution.id}>
              <div className="d-flex justify-content-between gap-3 flex-wrap">
                <strong>Execução #{execution.id}</strong>
                <span className="badge text-bg-secondary">
                  {statusLabel(execution)}
                </span>
              </div>
              <p className="mb-1 mt-2">{execution.objective}</p>
              {execution.recommendedDecision ? (
                <p className="mb-1">
                  <strong>Decisão:</strong> {execution.recommendedDecision}
                </p>
              ) : null}
              {execution.recommendedAction ? (
                <p className="mb-0">
                  <strong>Próxima ação recomendada:</strong>{" "}
                  {execution.recommendedAction}
                </p>
              ) : null}
              {evidence ? (
                <details className="mt-2">
                  <summary>
                    <strong>Inteligência de sessões:</strong> {evidence.label}
                  </summary>
                  <pre className="bg-light border rounded p-2 mt-2 mb-0 small overflow-auto">
                    {JSON.stringify(evidence.data, null, 2)}
                  </pre>
                </details>
              ) : null}
              {execution.dailyReport ? (
                <div className="alert alert-light border mt-2 mb-0">
                  <div className="d-flex justify-content-between gap-2 flex-wrap">
                    <strong>Relatório diário:</strong>
                    <span className="text-muted small">
                      Gerado em{" "}
                      {formatReportDateTime(
                        execution.finishedAt ?? execution.createdAt,
                      )}
                    </span>
                  </div>
                  <p className="mb-0 mt-1" style={{ whiteSpace: "pre-wrap" }}>
                    {execution.dailyReport}
                  </p>
                </div>
              ) : null}
              {execution.errorMessage ? (
                <p className="text-danger mb-0">{execution.errorMessage}</p>
              ) : null}
            </article>
          );
        })}
      </div>
    </section>
  );
}
