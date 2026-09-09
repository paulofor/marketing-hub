import { useState } from "react";
import { Link } from "react-router-dom";
import {
  useDecisionProposal,
  useDecisionProposalAudit,
  useRetryDecisionProposal,
} from "../../api/learningCycle/useDecisionProposal";
import {
  cycleError,
  type LearningCycle,
  type CycleCatalog,
} from "../../api/learningCycle/useLearningCycles";
import LearningCycleCommandForm from "./LearningCycleCommandForm";

export default function LearningCycleDecisionPanel({
  cycle,
  catalog,
  onUpdated,
}: {
  cycle: LearningCycle;
  catalog: CycleCatalog;
  onUpdated: (cycle: LearningCycle) => void;
}) {
  const query = useDecisionProposal(cycle);
  const retry = useRetryDecisionProposal(cycle);
  const [showAudit, setShowAudit] = useState(false);
  const audit = useDecisionProposalAudit(cycle, showAudit);
  const value = query.data;
  const ready =
    value?.status === "READY" &&
    value.cycleRevision === cycle.revision &&
    value.proposal &&
    cycle.status === "OPEN";
  return (
    <section aria-label="Proposta comercial de Atena">
      <div className="card card-body mb-3">
        <h3 className="h5">Atena prepara; você edita e aprova</h3>
        <p>
          Atividade 6.4 · decisão do ciclo #{cycle.id} · experimento #
          {cycle.experimentId}.
        </p>
        {value?.agentId ? (
          <Link to={`/agents/${value.agentId}`}>Agente responsável: Atena</Link>
        ) : (
          <p>Agente responsável: Atena</p>
        )}
        <p>
          A proposta usa os resultados conciliados. Sua aprovação registra a
          decisão e o retorno no BPM.
        </p>
        {query.isPending ? (
          <p role="status">Carregando a proposta de Atena…</p>
        ) : null}
        {query.isError ? (
          <p role="alert" className="alert alert-danger">
            Não foi possível consultar a proposta. {cycleError(query.error)}
          </p>
        ) : null}
        {value && ["WAITING", "QUEUED", "RUNNING"].includes(value.status) ? (
          <p role="status">
            {value.status === "RUNNING"
              ? "Atena está preenchendo a proposta. O formulário aparecerá para revisão quando ela concluir."
              : value.automaticExecutionEnabled
                ? "A proposta está aguardando a execução automática de Atena."
                : "Atena está em STOP. Retome o agente em sua página para preparar a proposta."}
          </p>
        ) : null}
        {value && ["FAILED", "EXPIRED", "STALE"].includes(value.status) ? (
          <div className="alert alert-warning" role="alert">
            <p>
              {value.error ||
                "A execução venceu ou o ciclo mudou. A proposta precisa ser preparada novamente."}
            </p>
            {value.id && value.status !== "STALE" ? (
              <button
                type="button"
                className="btn btn-outline-primary"
                disabled={retry.isPending}
                onClick={() => retry.mutate(value.id!)}
              >
                Tentar novamente com Atena
              </button>
            ) : null}
          </div>
        ) : null}
        {retry.isError ? <p role="alert">{cycleError(retry.error)}</p> : null}
        {value?.status === "APPROVED" ? (
          <p>
            Proposta #{value.id} aprovada. A decisão final está no histórico do
            ciclo.
          </p>
        ) : null}
        {value?.proposal ? (
          <>
            <p>
              <strong>Limites da evidência:</strong>{" "}
              {value.proposal.evidenceLimits}
            </p>
            <details>
              <summary>Três alternativas avaliadas por Atena</summary>
              <ol>
                {value.proposal.alternatives.map((item, index) => (
                  <li key={index}>
                    <strong>
                      {item.option}
                      {index === value.proposal!.selectedAlternative
                        ? " · recomendada"
                        : ""}
                    </strong>
                    <p>{item.benefit}</p>
                    <p>
                      Risco: {item.risk} · Esforço: {item.effort}
                    </p>
                    <p>Impacto esperado nas vendas: {item.salesImpact}</p>
                  </li>
                ))}
              </ol>
            </details>
          </>
        ) : null}
        {value?.id ? (
          <details onToggle={(event) => setShowAudit(event.currentTarget.open)}>
            <summary>Auditoria da proposta e tentativas</summary>
            {audit.isPending ? (
              <p>Carregando auditoria…</p>
            ) : audit.isError ? (
              <p role="alert">Não foi possível carregar a auditoria.</p>
            ) : (
              <pre style={{ whiteSpace: "pre-wrap", overflowWrap: "anywhere" }}>
                {JSON.stringify(audit.data, null, 2)}
              </pre>
            )}
          </details>
        ) : null}
      </div>
      {ready ? (
        <LearningCycleCommandForm
          key={`${cycle.id}-${cycle.revision}-${value.id}`}
          cycle={cycle}
          catalog={catalog}
          onUpdated={onUpdated}
          decisionProposal={value}
        />
      ) : null}
    </section>
  );
}
