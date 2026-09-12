import { History, RefreshCw } from "lucide-react";
import { Link } from "react-router-dom";
import type { SalesFlow } from "../api/learningCycle/salesFlow";
import { useCycleProcessContext } from "../api/learningCycle/useCycleProcessContext";
import ProductNextProcessLink from "./ProductNextProcessLink";
import "./ProductValueChainCycleSummary.css";

const cycleStatuses: Record<string, string> = {
  ADJUSTED: "Encerrado para ajuste",
  CLOSED: "Encerrado",
  INCONCLUSIVE: "Inconclusivo",
};

/** Apresenta o trabalho oficial da passagem e a memória, sem inferir avanço do status comercial. */
export default function ProductValueChainCycleSummary({
  flow,
  processNumber,
  processName,
  isPositionError = false,
}: {
  flow: SalesFlow;
  processNumber?: number | null;
  processName?: string | null;
  isPositionError?: boolean;
}) {
  const query = useCycleProcessContext(
    flow.productId,
    flow.modelProcessDefinitionId,
    flow.cycleId,
    flow.chainDefinitionId,
  );
  const context = query.data;
  const consistent =
    context?.cycleId === flow.cycleId &&
    context?.experimentId === flow.experimentId &&
    context?.chainDefinitionId === flow.chainDefinitionId;
  const unavailable = query.isError || isPositionError || !consistent;
  const current = context?.status === "OPEN";
  const work = current ? context?.nextWork : null;
  const previousExperiments = [
    ...new Set(
      context?.previousLearning.map((item) => item.experimentId) ?? [],
    ),
  ];
  const parentUrl = `/products/${flow.productId}/value-chain-history/processes/${flow.modelProcessDefinitionId}/activities?learningCycleId=${flow.cycleId}&chainId=${flow.chainDefinitionId}${flow.currentActivityId ? `#activity-${flow.currentActivityId}` : ""}`;

  return (
    <div className="product-cycle-summary">
      <div className="product-value-chain-position__heading">
        <span>
          <RefreshCw size={16} aria-hidden="true" />
          {!unavailable && !current
            ? "Último ciclo de vendas"
            : "Ciclo atual de vendas"}
        </span>
      </div>
      <h3 className="product-cycle-summary__title">
        {consistent
          ? `${context.cycleNumber}º ciclo`
          : `Ciclo #${flow.cycleId}`}{" "}
        · Experimento #{flow.experimentId}
      </h3>

      {query.isLoading && !query.isFetched ? (
        <p role="status">
          Consultando o processo e o aprendizado deste ciclo...
        </p>
      ) : unavailable ? (
        <div role="alert" className="product-cycle-summary__work">
          <p>Não foi possível confirmar o trabalho atual deste ciclo.</p>
          <button
            type="button"
            className="btn btn-sm btn-outline-primary"
            onClick={() => void query.refetch()}
            disabled={query.isFetching || isPositionError}
          >
            {query.isFetching ? (
              <span
                className="spinner-border spinner-border-sm me-1"
                aria-hidden="true"
              />
            ) : null}
            {query.isFetching ? "Consultando..." : "Tentar novamente"}
          </button>
          {isPositionError ? (
            <small>
              Atualize a página para consultar a posição do produto.
            </small>
          ) : null}
        </div>
      ) : (
        <>
          {work ? (
            <ProductNextProcessLink {...work} />
          ) : current ? (
            <>
              <ProductNextProcessLink
                processNumber={processNumber}
                processName={processName || "Coordenação do ciclo de vendas"}
                url={parentUrl}
                cycleProcess
              />
              <p>
                A próxima ação está na etapa do ciclo. Consulte os critérios e
                as decisões.
              </p>
            </>
          ) : (
            <p>
              Esta passagem está encerrada. Consulte a decisão e a continuidade
              no histórico do ciclo.
            </p>
          )}
          <p className="product-cycle-summary__stage">
            {current
              ? context.stageLabel
              : cycleStatuses[context.status] || "Ciclo encerrado"}
          </p>
          <details className="product-cycle-summary__details">
            <summary>Melhoria e hipótese desta passagem</summary>
            <p>{context.mainChange || "Melhoria ainda não registrada."}</p>
            <p>
              <strong>Hipótese:</strong>{" "}
              {context.hypothesis || "Ainda não registrada."}
            </p>
            <small>Versão alvo: {context.productVersion}</small>
          </details>
          <details className="product-cycle-summary__details">
            <summary>
              Aprendizado dos ciclos anteriores
              {previousExperiments.length
                ? ` · ${previousExperiments.length === 1 ? "experimento" : "experimentos"} ${previousExperiments.map((id) => `#${id}`).join(", ")}`
                : ""}
            </summary>
            {context.previousLearning.length ? (
              context.previousLearning.map((learning, index) => (
                <div
                  className="product-cycle-summary__learning"
                  key={`${learning.cycleId}-${learning.action}-${index}`}
                >
                  <strong>
                    Ciclo #{learning.cycleId} · Experimento #
                    {learning.experimentId}
                  </strong>
                  <p>{learning.learning || learning.summary}</p>
                  {learning.limitation ? (
                    <p>
                      <strong>Limites da conclusão:</strong>{" "}
                      {learning.limitation}
                    </p>
                  ) : null}
                  <details>
                    <summary>Evidência registrada</summary>
                    <small>
                      {learning.evidenceReference ||
                        "Sem referência registrada."}
                    </small>
                  </details>
                </div>
              ))
            ) : (
              <p>Ainda não há aprendizado herdado registrado.</p>
            )}
          </details>
        </>
      )}

      <Link
        className="product-cycle-summary__cycle-link"
        to={consistent ? context.cycleUrl : flow.navigationUrl}
      >
        <History size={15} aria-hidden="true" />
        {consistent && !unavailable && !work && current
          ? "Abrir etapa do ciclo"
          : "Ver ciclo e decisões"}
      </Link>
      <small className="product-cycle-summary__coordination">
        Coordenação na cadeia:{" "}
        <Link to={parentUrl}>
          {processNumber != null
            ? `Processo ${processNumber}`
            : "Processo coordenador"}
          {flow.currentActivitySequenceNumber != null
            ? ` · atividade ${processNumber != null ? `${processNumber}.` : ""}${flow.currentActivitySequenceNumber}`
            : ""}
        </Link>
        . O ciclo reúne decisões e retornos entre os processos.
      </small>
    </div>
  );
}
