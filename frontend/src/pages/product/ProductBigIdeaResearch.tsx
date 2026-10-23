import { useState } from "react";
import axios from "axios";
import {
  useBigIdeaResearch,
  useBigIdeaResearchAction,
} from "../../api/product/useProductBigIdea";

export default function ProductBigIdeaResearch({
  productId,
}: {
  productId: number;
}) {
  const query = useBigIdeaResearch(productId);
  const action = useBigIdeaResearchAction(productId);
  const [brief, setBrief] = useState("");
  const data = query.data;
  const labels = {
    NOT_REQUESTED: "Pronta para solicitar",
    PENDING: "Na fila de Atena",
    RUNNING: "Atena está pesquisando",
    COMPLETED: "Pesquisa concluída",
    FAILED: "Pesquisa interrompida",
  };
  const error =
    action.error && axios.isAxiosError(action.error)
      ? action.error.response?.data?.detail
      : null;
  return (
    <section className="card mb-4" aria-label="Criação com Atena">
      <div className="card-body">
        <h2 className="h5">Criar com Atena</h2>
        <p>
          Atena pesquisa o desejo do público, compara três ideias e propõe uma
          direção com fontes e limites. A pesquisa usa Codex e registra custo
          estimado; não exige extrato da API.
        </p>
        {query.isLoading && <p role="status">Consultando pesquisa…</p>}
        {query.isError && (
          <div role="alert">
            Não foi possível consultar Atena.{" "}
            <button
              type="button"
              className="btn btn-link"
              disabled={query.isFetching}
              onClick={() => query.refetch()}
            >
              Consultar novamente
            </button>
          </div>
        )}
        {data && (
          <>
            <p role="status">
              <strong>{labels[data.status]}</strong>
              {data.executionId ? ` · Pesquisa #${data.executionId}` : ""}
            </p>
            <p>{data.guidance}</p>
            {data.canRequest && (
              <>
                <details className="mb-3">
                  <summary>Acrescentar contexto e fontes</summary>
                  <label
                    className="form-label mt-2"
                    htmlFor="big-idea-research-brief"
                  >
                    Orientação para Atena (opcional)
                  </label>
                  <textarea
                    id="big-idea-research-brief"
                    className="form-control"
                    rows={5}
                    maxLength={12000}
                    value={brief}
                    onChange={(e) => setBrief(e.target.value)}
                    disabled={action.isPending}
                  />
                </details>
                <button
                  type="button"
                  className="btn btn-primary"
                  disabled={action.isPending}
                  onClick={() => action.mutate({ brief })}
                >
                  {action.isPending && (
                    <span className="spinner-border spinner-border-sm me-2" />
                  )}
                  {action.isPending ? "Solicitando…" : "Pedir Big Idea a Atena"}
                </button>
              </>
            )}
            {data.proposal && (
              <>
                <h3 className="h6">Proposta de Atena</h3>
                <p>{data.proposal.bigIdea.centralIdea}</p>
                <p>{data.proposal.selectionReason}</p>
                <details className="mb-3">
                  <summary>Ver pesquisa, alternativas e limites</summary>
                  <dl>
                    {Object.entries(data.proposal.bigIdea).map(
                      ([key, value]) => (
                        <div key={key}>
                          <dt>
                            {
                              (
                                {
                                  centralIdea: "Ideia central",
                                  audience: "Para quem",
                                  desire: "Desejo",
                                  promise: "Transformação",
                                  mechanism: "Mecanismo",
                                  evidence: "Provas e hipóteses",
                                  limits: "Limites",
                                } as Record<string, string>
                              )[key]
                            }
                          </dt>
                          <dd>{value}</dd>
                        </div>
                      ),
                    )}
                  </dl>
                  <p>{data.proposal.demandAssessment.validationPlan}</p>
                  <ul>
                    {data.proposal.alternatives.map((option, index) => (
                      <li key={index}>
                        <strong>{option.idea}</strong> — {option.benefit}{" "}
                        {option.risk}
                      </li>
                    ))}
                  </ul>
                  <ul>
                    {data.proposal.sources.map((source, index) => (
                      <li key={index}>
                        <a
                          href={
                            /^https?:\/\//.test(source.url)
                              ? source.url
                              : undefined
                          }
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          {source.title}
                        </a>{" "}
                        — {source.learning}
                      </li>
                    ))}
                  </ul>
                </details>
                <p className="small">
                  Custo estimado:{" "}
                  {data.estimatedCostUsd == null
                    ? "ainda não informado"
                    : `US$ ${data.estimatedCostUsd.toFixed(4)}`}{" "}
                  · Modelo: {data.modelName ?? "não informado"}
                </p>
              </>
            )}
            {data.canAdopt && data.executionId && (
              <button
                type="button"
                className="btn btn-success"
                disabled={action.isPending}
                onClick={() =>
                  action.mutate({ executionId: data.executionId! })
                }
              >
                {action.isPending && (
                  <span className="spinner-border spinner-border-sm me-2" />
                )}
                {action.isPending
                  ? "Registrando…"
                  : "Aplicar Big Idea de Atena"}
              </button>
            )}
          </>
        )}
        {action.isError && (
          <div className="alert alert-danger mt-3" role="alert">
            {typeof error === "string"
              ? error
              : "Não foi possível concluir a ação. Consulte a pesquisa antes de tentar novamente."}
          </div>
        )}
      </div>
    </section>
  );
}
