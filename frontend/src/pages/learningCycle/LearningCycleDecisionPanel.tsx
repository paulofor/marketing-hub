import { useState } from "react";
import { Link } from "react-router-dom";
import {
  useDecisionProposal,
  useDecisionProposalAudit,
  useRetryDecisionProposal,
  usePrepareCycleSuccessor,
} from "../../api/learningCycle/useDecisionProposal";
import {
  cycleError,
  type LearningCycle,
  type CycleCatalog,
} from "../../api/learningCycle/useLearningCycles";
import LearningCycleCommandForm from "./LearningCycleCommandForm";

const scopeLabels = {
  KEEP_FOCUS: "Manter o foco atual",
  ADJACENT_SEGMENTS: "Expandir para segmentos próximos",
  BROAD_PROBLEM: "Atender pela dor compartilhada",
  INSUFFICIENT_EVIDENCE: "Evidência insuficiente para redirecionar",
};
const readinessLabels = {
  SUPPORTED: "Compatibilidade sustentada pelas evidências",
  REQUIRES_ADAPTATION: "Precisa adaptar a entrega",
  UNKNOWN: "Capacidade ainda não demonstrada",
};

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
  const preparation = usePrepareCycleSuccessor(cycle);
  const [showAudit, setShowAudit] = useState(false);
  const audit = useDecisionProposalAudit(cycle, showAudit);
  const value = query.data;
  const market = value?.proposal?.marketReview;
  const ready =
    value?.status === "READY" &&
    value.cycleRevision === cycle.revision &&
    value.proposal &&
    cycle.status === "OPEN";
  return (
    <section aria-label="Proposta comercial de Atena">
      <div className="card card-body mb-3">
        <h3 className="h5">
          {value?.preparationAvailable
            ? "Continuidade preparada pelo sistema"
            : "Atena prepara; você edita e aprova"}
        </h3>
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
          {value?.preparationAvailable
            ? "O backend preserva o aprendizado e prepara um único sucessor com mídia zero, sem janela comercial herdada. Novas ocorrências seguem automaticamente; esta ação recupera propostas anteriores sem repetir Atena."
            : "A proposta usa os resultados conciliados. Sua aprovação registra a decisão e o retorno no BPM."}
        </p>
        {value?.preparationAvailable ? (
          <div>
            <button
              type="button"
              className="btn btn-primary mb-3"
              disabled={preparation.isPending}
              onClick={async () => {
                try {
                  onUpdated(await preparation.mutateAsync());
                } catch {
                  /* A causa persistida é apresentada abaixo, sem repetir o modelo. */
                }
              }}
            >
              {preparation.isPending
                ? "Preparando…"
                : "Preparar continuidade sem gasto"}
            </button>
            {preparation.isError ? (
              <p role="alert">{cycleError(preparation.error)}</p>
            ) : null}
            {value.error ? <p role="status">{value.error}</p> : null}
          </div>
        ) : null}
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
            {market ? (
              <section aria-label="Avaliação de mercado" className="mb-3">
                <h4 className="h6">Mercado e possível redirecionamento</h4>
                <p>
                  <strong>{scopeLabels[market.recommendedScope]}</strong>
                </p>
                <dl>
                  <dt>Público atual</dt>
                  <dd>{market.currentAudience}</dd>
                  <dt>Público proposto</dt>
                  <dd>{market.proposedAudience}</dd>
                  <dt>Problema compartilhado</dt>
                  <dd>{market.sharedProblem}</dd>
                  <dt>Capacidade de entrega</dt>
                  <dd>{readinessLabels[market.deliveryReadiness]}</dd>
                  <dt>Adaptações necessárias</dt>
                  <dd>{market.requiredAdaptations}</dd>
                  <dt>Públicos excluídos</dt>
                  <dd>{market.excludedAudiences}</dd>
                  <dt>Limites e atribuição</dt>
                  <dd>{market.evidenceLimits}</dd>
                  <dt>Métrica principal</dt>
                  <dd>
                    Contribuição líquida após aquisição, com vendas
                    reconciliadas e valor entregue.
                  </dd>
                  <dt>Continuar quando</dt>
                  <dd>{market.continueWhen}</dd>
                  <dt>Ajustar quando</dt>
                  <dd>{market.adjustWhen}</dd>
                  <dt>Parar quando</dt>
                  <dd>{market.stopWhen}</dd>
                </dl>
                {market.requiresNewCycle ? (
                  <p>
                    Novo ciclo e novo experimento obrigatórios. A proposta não
                    autoriza campanha, orçamento ou publicação.
                  </p>
                ) : null}
              </section>
            ) : (
              <p>
                Proposta histórica: esta versão ainda não exigia avaliação
                estruturada de mercado.
              </p>
            )}
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
                    {item.marketScope ? (
                      <p>{scopeLabels[item.marketScope]}</p>
                    ) : null}
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
      {ready && !value.preparationAvailable ? (
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
