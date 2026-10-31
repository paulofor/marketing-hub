import { useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import axios from "axios";
import {
  useExecutionFinance,
  type ExecutionFinance,
  type PreparationBudgetDecision,
} from "../../api/financial/useExecutionFinance";
import ExecutionFinancialAdvice from "./ExecutionFinancialAdvice";
import ExecutionBudgetOverview from "./ExecutionBudgetOverview";
import { createCycleRequestKey } from "../../api/learningCycle/createCycleRequestKey";
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
  const { query, approve } = useExecutionFinance(productId, chainId, cycleId);
  const [saved, setSaved] = useState(false);
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
              disabled={query.isFetching || approve.isPending}
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
          <ExecutionBudgetOverview
            key={`${data.productId}:${data.cycleId}`}
            data={data}
            disabled={query.isError || query.isFetching || approve.isPending}
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
              Saldo negativo indica consumo conhecido acima do limite; reserva
              não é cobrança.
            </p>
            {!data.authorization && (
              <div className="alert alert-info">
                Ainda não há limite de preparação registrado neste formulário
                para esta versão. Decisões anteriores ficam preservadas no
                histórico e não são aplicadas automaticamente a outro ciclo.
              </div>
            )}
            <CostChart data={data} />
          </section>
          <section
            className="card p-3 mb-3"
            id="preparation-decision"
            aria-label="Ajustar e aprovar preparação"
          >
            <h2 className="h4">Ajustar e aprovar preparação</h2>
            {saved && (
              <div role="status" className="alert alert-success">
                Decisão registrada com recibo. O teto será entregue aos agentes
                neste ciclo. Nenhuma tarefa ou campanha foi iniciada por esta
                aprovação.
              </div>
            )}
            {data.canAuthorize ? (
              <BudgetForm
                key={`${data.productId}:${data.cycleId}:${data.revision}`}
                data={data}
                pending={approve.isPending}
                disabled={query.isError || query.isFetching}
                error={approve.isError ? errorText(approve.error) : null}
                onSubmit={async (decision) => {
                  await approve.mutateAsync(decision);
                  setSaved(true);
                }}
              />
            ) : (
              <p>{data.authorizationBlocker}</p>
            )}
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
function BudgetForm({
  data,
  pending,
  disabled,
  error,
  onSubmit,
}: {
  data: ExecutionFinance;
  pending: boolean;
  disabled: boolean;
  error: string | null;
  onSubmit: (d: PreparationBudgetDecision) => Promise<void>;
}) {
  const [total, setTotal] = useState(
    data.authorization?.totalLimitUsd.toString() ?? "",
  );
  const [reserve, setReserve] = useState(
    data.authorization?.reserveUsd.toString() ?? "",
  );
  const [actor, setActor] = useState("");
  const [reason, setReason] = useState("");
  const [confirmed, setConfirmed] = useState(false);
  const attempt = useRef<{ payload: string; key: string } | null>(null);
  return (
    <form
      onSubmit={async (e) => {
        e.preventDefault();
        if (pending || disabled || !confirmed) return;
        const fields = {
          expectedRevision: data.revision,
          chainDefinitionId: data.chainId,
          experimentId: data.experimentId,
          productVersion: data.productVersion,
          totalLimitUsd: Number(total),
          reserveUsd: Number(reserve),
          operatorName: actor,
          justification: reason,
          confirmed,
        };
        const payload = JSON.stringify(fields);
        if (attempt.current?.payload !== payload)
          attempt.current = { payload, key: createCycleRequestKey() };
        try {
          await onSubmit({ ...fields, requestKey: attempt.current.key });
        } catch {
          /* Erro apresentado pelo estado da mutação; preserva recibo na retentativa. */
        }
      }}
    >
      <p>
        Defina o máximo total em dólares para os agentes prepararem e revisarem
        o produto. Esse valor inclui o que já foi consumido; não é uma verba
        nova a somar ao limite anterior. Produzir vídeos e pagar anúncios têm
        decisões separadas nos blocos acima.
      </p>
      <fieldset disabled={pending || disabled}>
        <div className="row g-3">
          <div className="col-md-6">
            <label htmlFor="prep-limit" className="form-label">
              Teto total acumulado (US$) *
            </label>
            <input
              id="prep-limit"
              className="form-control"
              type="number"
              required
              min="0"
              max="999999.99"
              step="0.01"
              value={total}
              onChange={(e) => {
                setTotal(e.target.value);
                setConfirmed(false);
              }}
            />
          </div>
          <div className="col-md-6">
            <label htmlFor="prep-reserve" className="form-label">
              Reserva para custos pendentes (US$) *
            </label>
            <input
              id="prep-reserve"
              className="form-control"
              type="number"
              required
              min="0"
              max="999999.99"
              step="0.01"
              value={reserve}
              onChange={(e) => {
                setReserve(e.target.value);
                setConfirmed(false);
              }}
            />
            <small>
              Parte do limite guardada para tarefas cujo custo ainda não chegou.
              Não é cobrança nem dinheiro extra. Informe 0 se decidir não
              reservar e explique na justificativa.
            </small>
          </div>
        </div>
        <label htmlFor="prep-actor" className="form-label mt-3">
          Responsável pela decisão *
        </label>
        <input
          id="prep-actor"
          required
          maxLength={160}
          className="form-control"
          value={actor}
          onChange={(e) => setActor(e.target.value)}
        />
        <label htmlFor="prep-reason" className="form-label mt-3">
          Justificativa e fontes consideradas *
        </label>
        <textarea
          id="prep-reason"
          required
          maxLength={4000}
          className="form-control"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
        />
        <label className="d-flex gap-2 my-3">
          <input
            type="checkbox"
            required
            checked={confirmed}
            onChange={(e) => setConfirmed(e.target.checked)}
          />
          <span>
            Confirmo este teto cumulativo para o ciclo #{data.cycleId}, contando
            custos anteriores. Esta decisão não autoriza produzir vídeos nem
            pagar anúncios. *
          </span>
        </label>
        {error && (
          <p className="alert alert-danger" role="alert">
            {error}
          </p>
        )}
        <button
          className="btn btn-primary"
          type="submit"
          disabled={pending || disabled || !confirmed}
        >
          {pending && (
            <span className="spinner-border spinner-border-sm me-2" />
          )}
          {pending ? "Registrando…" : "Aprovar limite de preparação"}
        </button>
      </fieldset>
    </form>
  );
}
