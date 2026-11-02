import { useEffect, useRef, useState } from "react";
import axios from "axios";
import {
  useCostWorksheet,
  type CostWorksheet,
  type CostWorksheetRequest,
} from "../../api/financial/useCostWorksheet";
import type { ExecutionFinance } from "../../api/financial/useExecutionFinance";
import { createCycleRequestKey } from "../../api/learningCycle/createCycleRequestKey";

const money = (value: number | null, currency = "BRL") =>
  value == null
    ? "Não informado"
    : new Intl.NumberFormat("pt-BR", { style: "currency", currency }).format(
        value,
      );
const amount = (value: string) => (value.trim() === "" ? null : Number(value));
const failure = (error: unknown) =>
  axios.isAxiosError(error)
    ? error.response?.data?.detail ||
      error.response?.data?.message ||
      "Não foi possível salvar. Preserve os dados e tente novamente."
    : "Não foi possível consultar a planilha.";

/** Edita a planilha contextual e apresenta somente cálculos e aprovações vindos do backend. */
export default function ExecutionCostWorksheet({
  data,
  disabled,
}: {
  data: ExecutionFinance;
  disabled: boolean;
}) {
  const { query, save } = useCostWorksheet(
    data.productId,
    data.chainId,
    data.cycleId,
  );
  const lastRevision = useRef(data.revision);
  useEffect(() => {
    if (lastRevision.current !== data.revision) {
      lastRevision.current = data.revision;
      void query.refetch();
    }
  }, [data.revision, query.refetch]);
  return (
    <section
      className="card p-3 mb-4 finance-worksheet"
      aria-label="Planilha de custos"
    >
      <div className="d-flex flex-wrap justify-content-between gap-2">
        <div>
          <h2 className="h4">Planilha de custos, orçamentos e previsões</h2>
          <p>
            Seis grupos para acompanhar o investimento e planejar o resultado.
          </p>
        </div>
        <button
          type="button"
          className="btn btn-outline-secondary btn-sm align-self-start"
          disabled={query.isFetching || save.isPending}
          onClick={() => void query.refetch()}
        >
          {query.isFetching && (
            <span className="spinner-border spinner-border-sm me-1" />
          )}
          Atualizar planilha
        </button>
      </div>
      {query.isLoading && (
        <p role="status">
          <span className="spinner-border spinner-border-sm" /> Consultando
          planilha…
        </p>
      )}
      {query.isError && (
        <p role="alert" className="alert alert-warning">
          {failure(query.error)} Atualize a planilha antes de gravar.
        </p>
      )}
      {query.data && (
        <WorksheetForm
          key={`${data.productId}:${data.cycleId}:${query.data.eventId ?? "new"}`}
          sheet={query.data}
          context={data}
          pending={save.isPending}
          disabled={disabled || query.isFetching || query.isError}
          error={save.isError ? failure(save.error) : null}
          onSave={(request) => save.mutateAsync(request).then(() => undefined)}
        />
      )}
    </section>
  );
}

/** Mantém o rascunho até salvar, inclusive após falhas, e reutiliza a chave em retentativas. */
function WorksheetForm({
  sheet,
  context,
  pending,
  disabled,
  error,
  onSave,
}: {
  sheet: CostWorksheet;
  context: ExecutionFinance;
  pending: boolean;
  disabled: boolean;
  error: string | null;
  onSave: (request: CostWorksheetRequest) => Promise<void>;
}) {
  const [draft, setDraft] = useState(() => ({
    periodStart: sheet.saved?.periodStart ?? "",
    periodEnd: sheet.saved?.periodEnd ?? "",
    expectedRevenueBrl: sheet.saved?.expectedRevenueBrl?.toString() ?? "",
    usdBrl: sheet.saved?.usdBrl?.toString() ?? "",
    exchangeSource: sheet.saved?.exchangeSource ?? "",
    exchangeCheckedOn: sheet.saved?.exchangeCheckedOn ?? "",
    operatorName: "",
    justification: sheet.saved?.justification ?? "",
    lines: sheet.rows.map((row) => ({
      code: row.code,
      planned: row.planned?.toString() ?? "",
      reportedCost: row.reportedCost?.toString() ?? "",
      forecast: row.forecast?.toString() ?? "",
      source: row.source ?? "",
    })),
  }));
  const [approve, setApprove] = useState(false);
  const [dirty, setDirty] = useState(false);
  const attempt = useRef<{ payload: string; key: string } | null>(null);
  const update = (
    field: Exclude<keyof typeof draft, "lines">,
    value: string,
  ) => {
    setDirty(true);
    setDraft((old) => ({ ...old, [field]: value }));
  };
  const updateLine = (
    index: number,
    field: "planned" | "reportedCost" | "forecast" | "source",
    value: string,
  ) => {
    setDirty(true);
    setDraft((old) => ({
      ...old,
      lines: old.lines.map((row, i) =>
        i === index ? { ...row, [field]: value } : row,
      ),
    }));
  };
  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (pending || disabled || !sheet.editable) return;
    const fields = {
      expectedRevision: sheet.revision,
      chainDefinitionId: context.chainId,
      experimentId: context.experimentId,
      productVersion: context.productVersion,
      periodStart: draft.periodStart,
      periodEnd: draft.periodEnd,
      expectedRevenueBrl: amount(draft.expectedRevenueBrl),
      usdBrl: amount(draft.usdBrl),
      exchangeSource: draft.exchangeSource || null,
      exchangeCheckedOn: draft.exchangeCheckedOn || null,
      operatorName: draft.operatorName,
      justification: draft.justification,
      approveOperatingBudgets: approve,
      lines: draft.lines.map((row) => ({
        code: row.code,
        planned: amount(row.planned),
        reportedCost: amount(row.reportedCost),
        forecast: amount(row.forecast),
        source: row.source || null,
      })),
    };
    const payload = JSON.stringify(fields);
    if (attempt.current?.payload !== payload)
      attempt.current = { payload, key: createCycleRequestKey() };
    try {
      await onSave({ ...fields, requestKey: attempt.current.key });
    } catch {
      /* A mutação apresenta o erro e preserva o rascunho e a chave. */
    }
  };
  return (
    <>
      {sheet.eventId != null && (
        <p className="small text-success" role="status">
          Planilha salva · Registro #{sheet.eventId}. Consulte autor, data e
          revisões no histórico.
        </p>
      )}
      {!sheet.editable && (
        <p className="alert alert-info">
          Ciclo encerrado: custos, previsões e decisões preservados para
          consulta.
        </p>
      )}
      <p className="small">
        Orçado é o que você propõe gastar. Aprovado é o limite registrado. Custo
        informado é o total conferido por você, com fonte. Previsão é o custo
        total esperado até o fim do período, incluindo o que já foi gasto.
        Valores vazios permanecem desconhecidos.
      </p>
      <form onSubmit={submit}>
        <fieldset disabled={pending || disabled || !sheet.editable}>
          <div className="row g-3 mb-3">
            <div className="col-sm-6 col-lg-3">
              <label htmlFor="worksheet-start" className="form-label">
                Início do período *
              </label>
              <input
                id="worksheet-start"
                type="date"
                required
                className="form-control"
                value={draft.periodStart}
                onChange={(e) => update("periodStart", e.target.value)}
              />
            </div>
            <div className="col-sm-6 col-lg-3">
              <label htmlFor="worksheet-end" className="form-label">
                Fim do período *
              </label>
              <input
                id="worksheet-end"
                type="date"
                min={draft.periodStart}
                required
                className="form-control"
                value={draft.periodEnd}
                onChange={(e) => update("periodEnd", e.target.value)}
              />
            </div>
            <div className="col-lg-6">
              <label htmlFor="worksheet-revenue" className="form-label">
                Receita bruta prevista no período (R$)
              </label>
              <input
                id="worksheet-revenue"
                type="number"
                min="0"
                max="999999999.99"
                step="0.01"
                className="form-control"
                value={draft.expectedRevenueBrl}
                onChange={(e) => update("expectedRevenueBrl", e.target.value)}
              />
              <small>
                Antes de taxas, impostos e reembolsos. Registre essas deduções
                somente na linha própria.
              </small>
            </div>
          </div>
          <p className="small mb-2">
            Todos os campos monetários abaixo são totais, não valores por venda.
            No celular, deslize a tabela para consultar as colunas.
          </p>
          <div
            className="finance-sheet-scroll"
            role="region"
            aria-label="Tabela dos seis grupos"
            tabIndex={0}
          >
            <table className="table table-bordered align-middle finance-sheet-table">
              <caption>
                Planejamento do ciclo #{context.cycleId} · US$ e R$ separados ·
                diferenças calculadas ao salvar
              </caption>
              <thead>
                <tr>
                  <th scope="col">Grupo / fonte</th>
                  <th scope="col">Orçado</th>
                  <th scope="col">Aprovado / escopo</th>
                  <th scope="col">Custo informado</th>
                  <th scope="col">Previsão total</th>
                  <th scope="col">Aprovado − previsão</th>
                </tr>
              </thead>
              <tbody>
                {sheet.rows.map((row, index) => (
                  <tr key={row.code}>
                    <th scope="row">
                      <span className="d-block">
                        {row.label}{" "}
                        <small>({row.currency === "USD" ? "US$" : "R$"})</small>
                      </span>
                      <small className="d-block fw-normal my-2">
                        {row.description}
                        <br />
                        {row.basis}
                      </small>
                      <label
                        htmlFor={`source-${row.code}`}
                        className="small fw-normal"
                      >
                        Fonte e premissas{" "}
                        {[
                          draft.lines[index].planned,
                          draft.lines[index].reportedCost,
                          draft.lines[index].forecast,
                        ].some((v) => v !== "")
                          ? "*"
                          : ""}
                      </label>
                      <textarea
                        id={`source-${row.code}`}
                        required={[
                          draft.lines[index].planned,
                          draft.lines[index].reportedCost,
                          draft.lines[index].forecast,
                        ].some((v) => v !== "")}
                        aria-label={`Fonte — ${row.label}`}
                        className="form-control form-control-sm"
                        rows={2}
                        maxLength={2000}
                        value={draft.lines[index].source}
                        onChange={(e) =>
                          updateLine(index, "source", e.target.value)
                        }
                      />
                    </th>
                    <td>
                      <input
                        type="number"
                        aria-label={`Orçado — ${row.label}`}
                        min="0"
                        max="999999999.99"
                        step="0.01"
                        className="form-control"
                        value={draft.lines[index].planned}
                        onChange={(e) =>
                          updateLine(index, "planned", e.target.value)
                        }
                      />
                    </td>
                    <td>
                      <strong>{money(row.approved, row.currency)}</strong>
                      <small className="d-block mt-1">
                        {row.approvalStatus}
                      </small>
                      {row.code === "AGENTS" && (
                        <a href="#preparation-decision" className="small">
                          Rever aprovação
                        </a>
                      )}
                      {["PRODUCTION", "MEDIA"].includes(row.code) && (
                        <a href="#operating-approvals" className="small">
                          Ver decisão nesta página
                        </a>
                      )}
                    </td>
                    <td>
                      <input
                        type="number"
                        aria-label={`Custo informado — ${row.label}`}
                        min="0"
                        max="999999999.99"
                        step="0.01"
                        className="form-control"
                        value={draft.lines[index].reportedCost}
                        onChange={(e) =>
                          updateLine(index, "reportedCost", e.target.value)
                        }
                      />
                      {row.estimatedCost != null && (
                        <small className="d-block mt-2">
                          <strong>
                            Estimativa automática:{" "}
                            {money(row.estimatedCost, row.currency)}
                          </strong>
                        </small>
                      )}
                      <small className="d-block mt-1">
                        {row.estimateSource}
                      </small>
                    </td>
                    <td>
                      <input
                        type="number"
                        aria-label={`Previsão total — ${row.label}`}
                        min="0"
                        max="999999999.99"
                        step="0.01"
                        className="form-control"
                        value={draft.lines[index].forecast}
                        onChange={(e) =>
                          updateLine(index, "forecast", e.target.value)
                        }
                      />
                    </td>
                    <td
                      className={
                        row.variance != null && row.variance < 0
                          ? "text-danger"
                          : ""
                      }
                    >
                      {money(row.variance, row.currency)}
                      <small className="d-block">
                        Diferença em relação ao limite mostrado; confira seu
                        escopo.
                      </small>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className="small">
            A estimativa automática é uma referência separada: não será somada
            novamente ao custo informado. O orçamento de produção reúne imagens,
            áudio e vídeo; o recibo operacional de vídeos cobre apenas as peças
            autorizadas. Reservas e limites não são despesas.
          </p>
          <details className="mb-3">
            <summary>
              Conversão para calcular o resultado previsto em reais
            </summary>
            <div className="row g-3 mt-1">
              <div className="col-md-3">
                <label htmlFor="worksheet-fx" className="form-label">
                  R$ por US$ 1
                </label>
                <input
                  id="worksheet-fx"
                  type="number"
                  min="0.000001"
                  max="10000"
                  step="0.000001"
                  className="form-control"
                  value={draft.usdBrl}
                  onChange={(e) => update("usdBrl", e.target.value)}
                />
              </div>
              <div className="col-md-6">
                <label htmlFor="worksheet-fx-source" className="form-label">
                  Fonte do câmbio {draft.usdBrl ? "*" : ""}
                </label>
                <input
                  id="worksheet-fx-source"
                  required={Boolean(draft.usdBrl)}
                  maxLength={1000}
                  className="form-control"
                  value={draft.exchangeSource}
                  onChange={(e) => update("exchangeSource", e.target.value)}
                />
              </div>
              <div className="col-md-3">
                <label htmlFor="worksheet-fx-date" className="form-label">
                  Data da consulta {draft.usdBrl ? "*" : ""}
                </label>
                <input
                  id="worksheet-fx-date"
                  type="date"
                  required={Boolean(draft.usdBrl)}
                  className="form-control"
                  value={draft.exchangeCheckedOn}
                  onChange={(e) => update("exchangeCheckedOn", e.target.value)}
                />
              </div>
            </div>
          </details>
          <div className="row g-3 mb-3">
            <div className="col-md-4">
              <label htmlFor="worksheet-operator" className="form-label">
                Responsável pela planilha *
              </label>
              <input
                id="worksheet-operator"
                required
                maxLength={160}
                className="form-control"
                value={draft.operatorName}
                onChange={(e) => update("operatorName", e.target.value)}
              />
            </div>
            <div className="col-md-8">
              <label htmlFor="worksheet-reason" className="form-label">
                Justificativa e premissas gerais *
              </label>
              <textarea
                id="worksheet-reason"
                required
                maxLength={4000}
                className="form-control"
                value={draft.justification}
                onChange={(e) => update("justification", e.target.value)}
                placeholder="Explique volume de vendas previsto, rateios, fontes e valores zero."
              />
            </div>
          </div>
          <label className="d-flex gap-2 mb-3">
            <input
              type="checkbox"
              checked={approve}
              onChange={(e) => setApprove(e.target.checked)}
            />
            <span>
              Aprovo os valores orçados de entrega e suporte, despesas e
              deduções da venda e operação fixa para este planejamento. Esta
              decisão não libera execução ou gasto. Agentes, vídeos e anúncios
              mantêm seus próprios aceites.
            </span>
          </label>
          {error && (
            <p role="alert" className="alert alert-danger">
              {error}
            </p>
          )}
          <button
            type="submit"
            className="btn btn-primary"
            disabled={pending || disabled || !sheet.editable}
          >
            {pending && (
              <span className="spinner-border spinner-border-sm me-2" />
            )}
            {pending
              ? "Salvando…"
              : approve
                ? "Salvar e aprovar os três orçamentos operacionais"
                : "Salvar custos e previsões"}
          </button>
          <p className="small mt-2">
            Cada gravação cria uma revisão. Editar a planilha sem confirmar os
            três orçamentos deixa essa aprovação pendente novamente.
          </p>
        </fieldset>
      </form>
      <div className="mt-4" aria-label="Resultado da planilha">
        <h3 className="h5">Resumo da última gravação</h3>
        {dirty && (
          <p role="status" className="text-warning">
            Há alterações não salvas. Os cálculos abaixo correspondem à última
            gravação.
          </p>
        )}
        <div className="row g-3">
          {sheet.totals.map((total) => (
            <div className="col-md-6" key={total.currency}>
              <div className="border rounded p-3">
                <h4 className="h6">
                  {total.currency === "USD" ? "Dólares (US$)" : "Reais (R$)"}
                </h4>
                <dl className="finance-sheet-totals">
                  <dt>Orçado conhecido</dt>
                  <dd>
                    {money(total.planned, total.currency)} ·{" "}
                    {total.missingPlanned} lacunas
                  </dd>
                  <dt>Custo informado conhecido</dt>
                  <dd>
                    {money(total.reportedCost, total.currency)} ·{" "}
                    {total.missingReported} lacunas
                  </dd>
                  <dt>Previsão conhecida</dt>
                  <dd>
                    {money(total.forecast, total.currency)} ·{" "}
                    {total.missingForecast} lacunas
                  </dd>
                </dl>
              </div>
            </div>
          ))}
        </div>
        <p className="mt-3 mb-1">
          <strong>
            Resultado previsto após os seis grupos:{" "}
            {money(sheet.projectedResultBrl)}
          </strong>
        </p>
        <p className="small">
          Receita bruta prevista menos custos totais previstos em R$ e custos em
          US$ convertidos pelo câmbio informado. Inclui recuperação do
          investimento acumulado dos agentes. É projeção, não lucro realizado
          nem contribuição por venda.
        </p>
        {sheet.gaps.length > 0 && (
          <details>
            <summary>
              O que falta para a previsão completa ({sheet.gaps.length})
            </summary>
            <ul>
              {sheet.gaps.map((gap) => (
                <li key={gap}>{gap}</li>
              ))}
            </ul>
          </details>
        )}
      </div>
      <details className="mt-3">
        <summary>Histórico da planilha ({sheet.history.length})</summary>
        {sheet.history.map((receipt) => (
          <article className="border-top py-3" key={receipt.eventId}>
            <p>
              <strong>
                Registro #{receipt.eventId} ·{" "}
                {receipt.current ? "Atual" : "Histórico"}
              </strong>
              <br />
              {receipt.operatorName} ·{" "}
              {new Date(receipt.recordedAt).toLocaleString("pt-BR")} ·{" "}
              {receipt.productVersion}
              <br />
              {receipt.operatingBudgetsApproved
                ? "Orçamentos operacionais aprovados para planejamento"
                : "Sem aprovação dos orçamentos operacionais"}
            </p>
            <p>
              {receipt.snapshot.periodStart} a {receipt.snapshot.periodEnd} ·
              Receita prevista: {money(receipt.snapshot.expectedRevenueBrl)}
              <br />
              {receipt.snapshot.justification}
            </p>
            <div className="table-responsive">
              <table className="table table-sm">
                <thead>
                  <tr>
                    <th scope="col">Grupo</th>
                    <th scope="col">Orçado</th>
                    <th scope="col">Custo informado</th>
                    <th scope="col">Previsão</th>
                    <th scope="col">Fonte</th>
                  </tr>
                </thead>
                <tbody>
                  {receipt.snapshot.lines.map((line) => {
                    const group = sheet.rows.find(
                      (row) => row.code === line.code,
                    );
                    return (
                      <tr key={line.code}>
                        <th scope="row">{group?.label ?? line.code}</th>
                        <td>{money(line.planned, group?.currency)}</td>
                        <td>{money(line.reportedCost, group?.currency)}</td>
                        <td>{money(line.forecast, group?.currency)}</td>
                        <td>{line.source || "Não informada"}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </article>
        ))}
      </details>
    </>
  );
}
