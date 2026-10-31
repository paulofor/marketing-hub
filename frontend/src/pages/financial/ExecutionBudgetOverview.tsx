import { useState } from "react";
import type { ExecutionFinance } from "../../api/financial/useExecutionFinance";
import { useVideoBudget } from "../../api/financial/useVideoBudget";
import { useLearningCycles } from "../../api/learningCycle/useLearningCycles";
import VideoBudgetForm from "./VideoBudgetForm";
import CycleBudgetAuthorizationForm from "../learningCycle/CycleBudgetAuthorizationForm";

const money = (value: number | null, currency: string) =>
  value == null
    ? "Não registrado"
    : new Intl.NumberFormat("pt-BR", { style: "currency", currency }).format(
        value,
      );

/** Apresenta escopos oficiais e abre os contratos existentes sem sair da demonstração. */
export default function ExecutionBudgetOverview({
  data,
  disabled,
}: {
  data: ExecutionFinance;
  disabled: boolean;
}) {
  const [editing, setEditing] = useState<"VIDEO" | "MEDIA" | null>(null);
  return (
    <section
      className="finance-budget-overview mb-4"
      aria-label="Entenda os três orçamentos"
    >
      <h2 className="h4">Três orçamentos, cada um com uma finalidade</h2>
      <p>
        <strong>
          Produzir o vídeo é uma despesa. Pagar para exibi-lo como anúncio é
          outra.
        </strong>{" "}
        A preparação do produto também tem seu próprio limite. Aprovar um deles
        não aumenta os outros.
      </p>
      <div className="finance-budget-grid">
        {data.budgets?.map((budget, index) => (
          <article
            className="card p-3"
            key={budget.code}
            aria-label={budget.title}
          >
            <span className="finance-budget-number" aria-hidden="true">
              {index + 1}
            </span>
            <h3 className="h5 mt-2">{budget.title}</h3>
            <p>{budget.purpose}</p>
            <div className="finance-budget-limit">
              <span>
                {budget.limitLabel} · {budget.currency === "USD" ? "US$" : "R$"}
              </span>
              <strong className="d-block fs-4">
                {money(budget.limit, budget.currency)}
              </strong>
              <span className="small fw-semibold">{budget.status}</span>
            </div>
            <p className="small mt-3">{budget.scope}</p>
            <p className="small">{budget.explanation}</p>
            <details className="small mb-3">
              <summary>O que sabemos sobre o consumo</summary>
              <p className="mt-2">{budget.consumptionNote}</p>
            </details>
            {budget.code === "PREPARATION" ? (
              <a
                className="btn btn-outline-primary mt-auto"
                href="#preparation-decision"
              >
                Revisar limite de preparação nesta tela
              </a>
            ) : (
              <button
                className="btn btn-outline-primary mt-auto"
                type="button"
                disabled={disabled}
                aria-expanded={editing === budget.code}
                aria-controls="other-budget-decision"
                onClick={() =>
                  setEditing(
                    editing === budget.code
                      ? null
                      : (budget.code as "VIDEO" | "MEDIA"),
                  )
                }
              >
                {budget.code === "VIDEO"
                  ? "Ver decisão dos vídeos aqui"
                  : "Ver decisão dos anúncios aqui"}
              </button>
            )}
          </article>
        ))}
      </div>
      <p className="small mt-3 mb-0">
        US$ são dólares; R$ são reais. Não somamos essas moedas sem câmbio.
        Limites são autorizações máximas, não cobranças. Uma revisão já
        contabilizada não deve ser somada novamente como outro gasto.
      </p>
      {editing && (
        <div id="other-budget-decision" className="card p-3 mt-3">
          <button
            type="button"
            className="btn btn-link align-self-end"
            onClick={() => setEditing(null)}
          >
            Fechar ajuste
          </button>
          {editing === "VIDEO" ? (
            <VideoDecision data={data} disabled={disabled} />
          ) : (
            <MediaDecision data={data} disabled={disabled} />
          )}
        </div>
      )}
    </section>
  );
}

/** Consulta a autorização audiovisual da mesma versão e preserva seus bloqueios oficiais. */
function VideoDecision({
  data,
  disabled,
}: {
  data: ExecutionFinance;
  disabled: boolean;
}) {
  const query = useVideoBudget(data.productId, data.cycleId, data.chainId);
  return (
    <section aria-label="Decisão de produção dos vídeos">
      <h3 className="h5">Produção dos vídeos · decisão separada em US$</h3>
      {query.isLoading && (
        <p role="status">
          <span className="spinner-border spinner-border-sm" /> Conferindo
          autorização dos vídeos…
        </p>
      )}
      {query.isError && (
        <p role="alert">
          Não foi possível conferir os vídeos.{" "}
          <button
            className="btn btn-link"
            disabled={query.isFetching}
            onClick={() => void query.refetch()}
          >
            {query.isFetching && (
              <span className="spinner-border spinner-border-sm" />
            )}{" "}
            Tentar novamente
          </button>
        </p>
      )}
      {query.data && (
        <>
          <p>
            {query.data.currentAuthorization
              ? `Teto registrado: ${money(query.data.currentAuthorization.budgetLimitUsd, "USD")}.`
              : "Não há teto de vídeos autorizado para esta versão. Isso não obriga a produzir novos vídeos."}
          </p>
          {query.data.canAuthorize ? (
            <VideoBudgetForm
              key={`${query.data.cycleId}:${query.data.revision}`}
              budget={query.data}
              disabled={disabled || query.isFetching || query.isError}
            />
          ) : (
            <p className="alert alert-info">{query.data.blocker}</p>
          )}
          <details className="mt-3">
            <summary>
              Histórico das autorizações de vídeos ({query.data.history.length})
            </summary>
            {query.data.history.map((item) => (
              <p key={item.eventId}>
                {money(item.budgetLimitUsd, "USD")} ·{" "}
                {item.current ? "Vigente" : "Histórico"} · {item.operatorName} ·{" "}
                {new Date(item.createdAt).toLocaleString("pt-BR")}
                <br />
                {item.justification}
              </p>
            ))}
          </details>
        </>
      )}
    </section>
  );
}

/** Explica a espera comercial sem oferecer aprovação de mídia antes da etapa permitida. */
function MediaDecision({
  data,
  disabled,
}: {
  data: ExecutionFinance;
  disabled: boolean;
}) {
  return (
    <section aria-label="Decisão de anúncios">
      <h3 className="h5">Anúncios · decisão separada em R$</h3>
      <p>
        {data.mediaDecision?.reason ??
          "Atualize a demonstração para consultar a disponibilidade da decisão."}
      </p>
      <p>
        Período registrado:{" "}
        {data.mediaDecision?.windowStart && data.mediaDecision?.windowEnd
          ? `${new Date(data.mediaDecision.windowStart).toLocaleString("pt-BR")} até ${new Date(data.mediaDecision.windowEnd).toLocaleString("pt-BR")}`
          : "ainda não definido neste ciclo. O prazo começa na janela comercial aprovada."}
      </p>
      {data.mediaDecision?.available && (
        <MediaForm data={data} disabled={disabled} />
      )}
    </section>
  );
}

/** Carrega o ciclo exato somente ao abrir a decisão e reutiliza seu formulário canônico. */
function MediaForm({
  data,
  disabled,
}: {
  data: ExecutionFinance;
  disabled: boolean;
}) {
  const query = useLearningCycles(data.productId, data.chainId);
  const cycle = query.data?.find(
    (item) =>
      item.id === data.cycleId &&
      item.productId === data.productId &&
      item.chainDefinitionId === data.chainId,
  );
  return (
    <>
      {query.isLoading && (
        <p role="status">
          <span className="spinner-border spinner-border-sm" /> Conferindo
          limites de anúncios…
        </p>
      )}
      {query.isError && (
        <p role="alert">
          Não foi possível conferir o ciclo.{" "}
          <button
            type="button"
            className="btn btn-link"
            disabled={query.isFetching}
            onClick={() => void query.refetch()}
          >
            {query.isFetching && (
              <span className="spinner-border spinner-border-sm" />
            )}{" "}
            Tentar novamente
          </button>
        </p>
      )}
      {query.isSuccess && !cycle && (
        <p role="alert">
          O ciclo solicitado não consta nesta consulta. Atualize os valores
          antes de decidir.
        </p>
      )}
      {cycle && (
        <fieldset
          disabled={
            disabled ||
            query.isFetching ||
            query.isError ||
            cycle.revision !== data.revision
          }
        >
          <CycleBudgetAuthorizationForm
            key={`${cycle.id}:${cycle.revision}`}
            cycle={cycle}
            onUpdated={() => void query.refetch()}
          />
        </fieldset>
      )}
    </>
  );
}
