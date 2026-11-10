import { Link, useSearchParams } from "react-router-dom";
import axios from "axios";
import {
  useExecutionFinance,
  type ExecutionFinance,
} from "../../api/financial/useExecutionFinance";
import SingleVideoBudgetPanel from "./SingleVideoBudgetPanel";
import ExecutionCostWorksheet from "./ExecutionCostWorksheet";
import ExecutionFinancialAdvice from "./ExecutionFinancialAdvice";
import ExecutionBudgetOverview from "./ExecutionBudgetOverview";
import "./ExecutionFinancePage.css";

const money = (n: number | null | undefined, currency = "USD") =>
  n == null
    ? "Não informado"
    : new Intl.NumberFormat("pt-BR", { style: "currency", currency }).format(n);
const errorText = (error: unknown) =>
  axios.isAxiosError(error)
    ? error.response?.data?.detail ||
      error.response?.data?.message ||
      "Não foi possível concluir. Atualize a leitura e tente novamente."
    : "Não foi possível concluir a consulta.";

export default function ExecutionFinancePage() {
  const [params] = useSearchParams();
  const productId = Number(params.get("productId")),
    chainId = Number(params.get("chainId")),
    cycleId = Number(params.get("cycleId"));
  const { query } = useExecutionFinance(productId, chainId, cycleId);
  const data = query.data;
  return (
    <main className="execution-finance container py-4">
      <h1>Demonstração financeira</h1>
      <p>
        Veja quanto já foi investido, compare o retorno esperado e decida os
        próximos limites.
      </p>
      {![productId, chainId, cycleId].every(
        (n) => Number.isSafeInteger(n) && n > 0,
      ) && (
        <p role="alert">Abra esta análise pelo card de execução do produto.</p>
      )}
      {query.isLoading && (
        <p role="status">
          <span className="spinner-border spinner-border-sm" /> Consultando
          custos e decisões…
        </p>
      )}
      {query.isError && (
        <div className="alert alert-warning" role="alert">
          {errorText(query.error)}{" "}
          <button
            className="btn btn-link"
            disabled={query.isFetching}
            onClick={() => void query.refetch()}
          >
            {query.isFetching && (
              <span className="spinner-border spinner-border-sm" />
            )}{" "}
            Atualizar
          </button>
        </div>
      )}
      {data && (
        <>
          <div className="d-flex flex-wrap gap-3 align-items-center mb-3">
            <h2 className="h4 mb-0">{data.productName}</h2>
            <Link to={data.cycleUrl}>Voltar ao ciclo</Link>
            <button
              className="btn btn-outline-secondary btn-sm"
              disabled={query.isFetching}
              onClick={() => void query.refetch()}
            >
              {query.isFetching && (
                <span className="spinner-border spinner-border-sm" />
              )}{" "}
              Atualizar valores
            </button>
          </div>
          <p>
            Ciclo #{data.cycleId} · Experimento #{data.experimentId} · Versão{" "}
            {data.productVersion}
            <br />
            <small>
              Consulta: {new Date(data.observedAt).toLocaleString("pt-BR")}
            </small>
          </p>
          {data.preparationBalance && (
            <section
              className={`alert ${data.preparationBalance.requiresBudgetReview ? "alert-warning" : "alert-info"}`}
              aria-label="Situação da preparação"
            >
              <h2 className="h5">{data.preparationBalance.title}</h2>
              {data.authorization && (
                <p>
                  Aprovado por {data.authorization.operatorName} em{" "}
                  {new Date(data.authorization.approvedAt).toLocaleString(
                    "pt-BR",
                  )}
                  {" · "}Recibo #{data.authorization.eventId} · Ciclo #
                  {data.authorization.sourceCycleId ?? data.cycleId}
                </p>
              )}
              {data.authorization?.carriedForward && (
                <p>
                  Teto e reserva mantidos do ciclo anterior, com o recibo
                  original. A mudança de ciclo não exige nova aprovação do teto
                  e não renova o saldo.
                </p>
              )}
              <p>{data.preparationBalance.explanation}</p>
              {data.authorization && (
                <p>
                  <strong>
                    {money(data.authorization.totalLimitUsd)} de teto −{" "}
                    {money(data.knownPreparationUsd)} de custos estimados −{" "}
                    {money(data.authorization.reserveUsd)} de reserva ={" "}
                    {money(data.availableUsd)} disponíveis.
                  </strong>
                </p>
              )}
              {data.preparationBalance.status === "NO_BALANCE" && (
                <p>
                  Falta cobrir {money(data.preparationBalance.shortfallUsd)}{" "}
                  para equilibrar custos e reserva. Isso ainda não inclui o
                  valor de uma nova tarefa. Custos pendentes podem alterar a
                  conta.
                </p>
              )}
              {data.canAuthorize &&
                data.preparationBalance.requiresBudgetReview && (
                  <a className="btn btn-outline-primary" href="#ceiling-AGENTS">
                    Rever teto aprovado
                  </a>
                )}
            </section>
          )}
          <SingleVideoBudgetPanel
            key={`${productId}:${cycleId}:${chainId}`}
            productId={productId}
            cycleId={cycleId}
            chainId={chainId}
          />
          <ExecutionCostWorksheet
            key={`worksheet:${data.productId}:${data.cycleId}`}
            data={data}
            disabled={query.isError || query.isFetching}
          />
          <ExecutionBudgetOverview
            key={`overview:${data.productId}:${data.cycleId}`}
            data={data}
            disabled={query.isError || query.isFetching}
          />
          <ExecutionFinancialAdvice data={data} />
          <section className="card p-3 mb-3" aria-label="Preparação e revisão">
            <h2 className="h4">Preparação e revisão · US$</h2>
            <div className="finance-metrics">
              <Metric
                title="Consumo conhecido estimado"
                value={money(data.knownPreparationUsd)}
              />
              <Metric
                title="Limite total de preparação"
                value={money(data.authorization?.totalLimitUsd)}
              />
              <Metric
                title="Reserva para custos pendentes"
                value={money(data.authorization?.reserveUsd)}
              />
              <Metric
                title="Disponível para preparar o produto"
                value={money(data.availableUsd)}
              />
            </div>
            <p className="mt-3 mb-1">{data.guidance}</p>
            <p>
              <strong>
                {data.unreportedTasks} tarefas com custo ainda não informado.
              </strong>{" "}
              O saldo desconta os custos estimados e a reserva do teto aprovado.
              Pode ficar negativo mesmo quando o consumo conhecido é menor que o
              teto; reserva não é cobrança.
            </p>
            {!data.authorization && (
              <div className="alert alert-info">
                Ainda não há teto de preparação aprovado neste ciclo nem nos
                predecessores do mesmo produto e cadeia. Registre a decisão na
                linha Agentes da planilha.
              </div>
            )}
            <CostChart data={data} />
          </section>
          <details className="card p-3">
            <summary>Histórico das decisões ({data.history.length})</summary>
            {data.history.map((receipt) => (
              <article key={receipt.eventId} className="border-bottom py-2">
                <strong>
                  {money(receipt.totalLimitUsd)} ·{" "}
                  {receipt.current ? "Vigente para esta versão" : "Histórico"}
                </strong>
                <p>
                  Reserva: {money(receipt.reserveUsd)} · {receipt.operatorName}{" "}
                  · {new Date(receipt.approvedAt).toLocaleString("pt-BR")}
                </p>
                <p>{receipt.justification}</p>
                <small>
                  Recibo #{receipt.eventId} · {receipt.productVersion}
                </small>
              </article>
            ))}
          </details>
        </>
      )}
    </main>
  );
}

function Metric({ title, value }: { title: string; value: string }) {
  return (
    <div>
      <span>{title}</span>
      <strong className="d-block fs-4">{value}</strong>
    </div>
  );
}
function Meter({
  value,
  max,
  label,
}: {
  value: number | null;
  max: number;
  label: string;
}) {
  return value == null ? (
    <span>Sem valor informado</span>
  ) : (
    <div className="finance-meter" role="img" aria-label={`${label}: ${value}`}>
      <span
        style={{
          width: `${Math.min(100, (Math.max(0, value) * 100) / Math.max(1, max))}%`,
        }}
      />
    </div>
  );
}
function CostChart({ data }: { data: ExecutionFinance }) {
  const max = Math.max(1, ...data.costs.map((c) => c.knownUsd ?? 0));
  return (
    <details>
      <summary>Consumo estimado por experimento · US$</summary>
      <div className="mt-2">
        {data.costs.length ? (
          data.costs.map((c) => (
            <div className="finance-cost-row" key={c.experimentId}>
              <Link to={`/experiments/${c.experimentId}`}>
                #{c.experimentId}
              </Link>
              <Meter
                value={c.knownUsd}
                max={max}
                label={`Experimento ${c.experimentId}`}
              />
              <span>{money(c.knownUsd)}</span>
            </div>
          ))
        ) : (
          <p>Nenhuma estimativa de tarefa vinculada encontrada.</p>
        )}
      </div>
    </details>
  );
}
