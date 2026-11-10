import { useEffect, useRef, useState, type FormEvent } from "react";
import axios from "axios";
import {
  useCostWorksheet,
  type CostWorksheet,
  type ApproveCostCeilingRequest,
} from "../../api/financial/useCostWorksheet";
import type { ExecutionFinance } from "../../api/financial/useExecutionFinance";
import { createCycleRequestKey } from "../../api/learningCycle/createCycleRequestKey";

const money = (value: number | null, currency = "BRL") =>
  value == null
    ? "Não disponível"
    : new Intl.NumberFormat("pt-BR", { style: "currency", currency }).format(
        value,
      );
const failure = (error: unknown) =>
  axios.isAxiosError(error)
    ? error.response?.data?.detail ||
      error.response?.data?.message ||
      "Não foi possível gravar. Seus dados foram preservados."
    : "Não foi possível concluir. Seus dados foram preservados.";

/** Apresenta fontes do sistema e concentra a edição monetária nos tetos aprovados. */
export default function ExecutionCostWorksheet({
  data,
  disabled,
}: {
  data: ExecutionFinance;
  disabled: boolean;
}) {
  const { query, approve } = useCostWorksheet(
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
          <h2 className="h4">Planilha de custos e tetos aprovados</h2>
          <p>
            Informe somente o teto na coluna Aprovado. Custos e previsões vêm
            das fontes do sistema. Todas as decisões de teto ficam nesta
            planilha, inclusive as de produção e mídia antes da liberação de
            execução. Os tetos acumulados continuam entre ciclos; consumo e
            reservas anteriores não são liberados pela mudança de ciclo.
          </p>
        </div>
        <button
          type="button"
          className="btn btn-outline-secondary btn-sm align-self-start"
          disabled={query.isFetching || approve.isPending}
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
          {failure(query.error)} Atualize a leitura antes de aprovar.
        </p>
      )}
      {query.data && (
        <Worksheet
          key={`${data.productId}:${data.cycleId}`}
          sheet={query.data}
          context={data}
          pending={approve.isPending}
          disabled={disabled || query.isFetching || query.isError}
          error={approve.isError ? failure(approve.error) : null}
          onApprove={(request) =>
            approve.mutateAsync(request).then(() => undefined)
          }
        />
      )}
    </section>
  );
}

/** Preserva o rascunho e a identidade da retentativa sem apresentar edição como aprovação salva. */
function Worksheet({
  sheet,
  context,
  pending,
  disabled,
  error,
  onApprove,
}: {
  sheet: CostWorksheet;
  context: ExecutionFinance;
  pending: boolean;
  disabled: boolean;
  error: string | null;
  onApprove: (request: ApproveCostCeilingRequest) => Promise<void>;
}) {
  const [drafts, setDrafts] = useState<Record<string, string>>({});
  const [selected, setSelected] = useState<string | null>(null);
  const [actor, setActor] = useState("");
  const [reason, setReason] = useState("");
  const [confirmed, setConfirmed] = useState(false);
  const [saved, setSaved] = useState<string | null>(null);
  const attempt = useRef<{
    signature: string;
    request: ApproveCostCeilingRequest;
  } | null>(null);
  const submitting = useRef(false);
  const confirmation = useRef<HTMLFormElement>(null);
  const row = sheet.rows.find((item) => item.code === selected);
  useEffect(() => {
    setConfirmed(false);
  }, [sheet.revision, row?.approvalScope]);
  const value = (code: string) =>
    drafts[code] ??
    sheet.rows.find((item) => item.code === code)?.approved?.toString() ??
    "";
  const valid = (code: string) =>
    /^\d{1,6}([,.]\d{1,2})?$/.test(value(code).trim());
  useEffect(() => {
    if (selected)
      confirmation.current?.scrollIntoView?.({
        behavior: "smooth",
        block: "nearest",
      });
  }, [selected]);
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (
      !row ||
      !confirmed ||
      !valid(row.code) ||
      pending ||
      disabled ||
      !row.canApprove ||
      submitting.current
    )
      return;
    const fields = {
      chainDefinitionId: context.chainId,
      experimentId: context.experimentId,
      productVersion: context.productVersion,
      code: row.code,
      approved: Number(value(row.code).replace(",", ".")),
      operatorName: actor.trim(),
      justification: reason.trim(),
      confirmed,
    };
    const signature = JSON.stringify(fields);
    if (attempt.current?.signature !== signature)
      attempt.current = {
        signature,
        request: {
          ...fields,
          expectedRevision: sheet.revision,
          requestKey: createCycleRequestKey(),
        },
      };
    submitting.current = true;
    try {
      await onApprove(attempt.current.request);
      setDrafts((old) => {
        const next = { ...old };
        delete next[row.code];
        return next;
      });
      setSaved(row.label);
      setSelected(null);
      setConfirmed(false);
      attempt.current = null;
    } catch {
      /* A mutação mantém erro, rascunho e a mesma chave para recuperação. */
    } finally {
      submitting.current = false;
    }
  }
  return (
    <>
      {!sheet.editable && (
        <p className="alert alert-info">
          Ciclo encerrado: custos e decisões preservados para consulta.
        </p>
      )}
      {saved && (
        <p role="status" className="alert alert-success">
          Teto de {saved} registrado. Confira o recibo e a situação do saldo;
          nenhuma tarefa ou campanha foi iniciada.
        </p>
      )}
      <p className="small">
        Teto é o máximo aprovado, incluindo custos anteriores no escopo
        indicado. Campo vazio significa ausência de aprovação. Um valor digitado
        só passa a valer após a confirmação e a gravação.
      </p>
      <div
        className="finance-sheet-scroll"
        role="region"
        aria-label="Tabela dos seis grupos"
        tabIndex={0}
      >
        <table className="table table-bordered align-middle finance-sheet-table">
          <caption>
            Ciclo #{context.cycleId} · US$ e R$ separados · no celular, deslize
            a tabela.
          </caption>
          <thead>
            <tr>
              <th scope="col">Grupo / fonte</th>
              <th scope="col">Aprovado / teto</th>
              <th scope="col">Custo conhecido</th>
              <th scope="col">Previsão total</th>
            </tr>
          </thead>
          <tbody>
            {sheet.rows.map((item) => (
              <tr key={item.code}>
                <th scope="row">
                  <span className="d-block">
                    {item.label}{" "}
                    <small>({item.currency === "USD" ? "US$" : "R$"})</small>
                  </span>
                  <small className="d-block fw-normal my-2">
                    {item.description}
                    <br />
                    {item.basis}
                  </small>
                  {item.source && (
                    <small className="d-block fw-normal">
                      Fonte registrada: {item.source}
                    </small>
                  )}
                </th>
                <td id={`ceiling-${item.code}`}>
                  <label
                    className="form-label small"
                    htmlFor={`approved-${item.code}`}
                  >
                    Teto ({item.currency === "USD" ? "US$" : "R$"})
                  </label>
                  <input
                    id={`approved-${item.code}`}
                    type="text"
                    inputMode="decimal"
                    maxLength={9}
                    aria-label={`Aprovado — ${item.label}`}
                    className="form-control"
                    disabled={
                      pending || disabled || !sheet.editable || !item.canApprove
                    }
                    value={value(item.code)}
                    onChange={(event) => {
                      setDrafts((old) => ({
                        ...old,
                        [item.code]: event.target.value,
                      }));
                      if (selected === item.code) setConfirmed(false);
                      setSaved(null);
                    }}
                  />
                  <small className="d-block mt-2">
                    Registrado: {money(item.approved, item.currency)}
                  </small>
                  <small className="d-block mt-1">{item.approvalStatus}</small>
                  {item.code === "AGENTS" && (
                    <small className="d-block mt-1">
                      Reserva preservada:{" "}
                      {money(context.authorization?.reserveUsd ?? 0, "USD")}. A
                      reserva faz parte do teto.
                    </small>
                  )}
                  {item.canApprove && (
                    <button
                      type="button"
                      className="btn btn-outline-primary btn-sm mt-2"
                      disabled={
                        pending ||
                        disabled ||
                        !sheet.editable ||
                        !valid(item.code)
                      }
                      onClick={() => {
                        setSelected(item.code);
                        setConfirmed(false);
                      }}
                      aria-label={`Aprovar teto — ${item.label}`}
                    >
                      Aprovar teto
                    </button>
                  )}
                  {item.executionBlocker && !item.approvalBlocker && (
                    <small className="d-block mt-2">
                      {item.executionBlocker}
                    </small>
                  )}
                  {item.approvalBlocker && (
                    <small className="d-block mt-2">
                      {item.approvalBlocker}
                    </small>
                  )}
                </td>
                <td>
                  <strong>
                    {money(
                      item.reportedCost ?? item.estimatedCost,
                      item.currency,
                    )}
                  </strong>
                  <small className="d-block mt-2">
                    {item.reportedCost != null
                      ? "Custo registrado com fonte; não somado novamente à estimativa."
                      : item.estimateSource}
                  </small>
                </td>
                <td>
                  {money(item.forecast, item.currency)}
                  <small className="d-block mt-2">
                    {item.forecast == null
                      ? "O sistema ainda não possui previsão com fonte para este grupo."
                      : "Previsão registrada; inclui os custos já considerados no período."}
                  </small>
                  {item.variance != null && (
                    <small
                      className={`d-block mt-2 ${item.variance < 0 ? "text-danger" : ""}`}
                    >
                      Teto menos previsão: {money(item.variance, item.currency)}
                    </small>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {row && (
        <form
          ref={confirmation}
          className="border rounded p-3 mt-3"
          aria-label={`Confirmar teto — ${row.label}`}
          onSubmit={submit}
        >
          <h3 className="h5">
            Confirmar teto de {row.label}:{" "}
            {money(Number(value(row.code).replace(",", ".")), row.currency)}
          </h3>
          <p>
            {row.approvalScope === "PLANNING" &&
            ["PRODUCTION", "MEDIA"].includes(row.code)
              ? "Esta decisão registra somente o teto de planejamento. Não libera produção, anúncios ou gasto. A aplicação operacional será confirmada nesta mesma planilha quando o processo permitir, usando o valor salvo."
              : row.code === "AGENTS"
                ? "Inclui o consumo acumulado dos agentes e preserva a reserva registrada. Sem reserva anterior, não acrescenta reserva. Não autoriza produzir peças nem pagar anúncios."
                : row.code === "PRODUCTION"
                  ? "O teto operacional cobre o anúncio, a demonstração na entrada e suas revisões. Imagens avulsas, mídia e publicação comercial exigem escopo próprio."
                  : row.code === "MEDIA"
                    ? "Teto total de anúncios do experimento na janela registrada. O limite diário é calculado pelo sistema e limitado a este total. A ativação depende da preparação e da autorização final."
                    : "Teto de planejamento para este grupo, ciclo e versão. Não autoriza execução, compras ou cobrança."}
          </p>
          <fieldset disabled={pending || disabled}>
            <label className="form-label d-block">
              Responsável pela decisão *
              <input
                className="form-control"
                required
                maxLength={160}
                value={actor}
                onChange={(event) => {
                  setActor(event.target.value);
                  setConfirmed(false);
                }}
              />
            </label>
            <label className="form-label d-block">
              Justificativa da decisão *
              <textarea
                className="form-control"
                required
                maxLength={4000}
                value={reason}
                onChange={(event) => {
                  setReason(event.target.value);
                  setConfirmed(false);
                }}
              />
            </label>
            <label className="d-flex gap-2 my-3">
              <input
                type="checkbox"
                required
                checked={confirmed}
                onChange={(event) => setConfirmed(event.target.checked)}
              />
              <span>
                Confirmo este teto e o escopo informado para o ciclo #
                {context.cycleId}. *
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
              disabled={pending || disabled || !confirmed}
            >
              {pending && (
                <span className="spinner-border spinner-border-sm me-2" />
              )}
              {pending ? "Registrando…" : "Confirmar e registrar teto"}
            </button>
            <button
              type="button"
              className="btn btn-link"
              disabled={pending}
              onClick={() => setSelected(null)}
            >
              Cancelar
            </button>
          </fieldset>
        </form>
      )}
      <p className="small mt-3">
        Custos sem fonte permanecem desconhecidos. Tetos e reservas não são
        despesas. As aprovações de cada grupo são independentes e respeitam a
        etapa do processo. Tetos antecipados ficam salvos aqui; a aplicação
        operacional exige confirmação nesta mesma planilha quando estiver
        disponível.
      </p>
      <details className="mt-3">
        <summary>Previsão e fontes disponíveis</summary>
        <p>
          Resultado previsto após os seis grupos:{" "}
          <strong>{money(sheet.projectedResultBrl)}</strong>. É projeção, não
          lucro realizado.
        </p>
        {sheet.gaps.length > 0 && (
          <ul>
            {sheet.gaps.map((gap) => (
              <li key={gap}>{gap}</li>
            ))}
          </ul>
        )}
      </details>
      <details className="mt-3">
        <summary>Histórico dos tetos ({sheet.ceilingHistory.length})</summary>
        {sheet.ceilingHistory.map((receipt) => {
          const group = sheet.rows.find((item) => item.code === receipt.code);
          return (
            <article className="border-top py-2" key={receipt.eventId}>
              <strong>
                {group?.label} · {money(receipt.approved, group?.currency)} ·{" "}
                {receipt.current ? "Teto acumulado vigente" : "Histórico"}
              </strong>
              <p>
                Recibo #{receipt.eventId} · {receipt.operatorName} ·{" "}
                {new Date(receipt.recordedAt).toLocaleString("pt-BR")}
                {receipt.sourceCycleId != null && (
                  <> · Ciclo de origem #{receipt.sourceCycleId}</>
                )}
                <br />
                {receipt.justification}
              </p>
            </article>
          );
        })}
      </details>
      <details className="mt-3">
        <summary>
          Histórico de custos e previsões ({sheet.history.length})
        </summary>
        {sheet.history.map((receipt) => (
          <article className="border-top py-2" key={receipt.eventId}>
            <strong>
              Registro #{receipt.eventId} · {receipt.operatorName} ·{" "}
              {new Date(receipt.recordedAt).toLocaleString("pt-BR")}
            </strong>
            <p>
              {receipt.snapshot.periodStart} a {receipt.snapshot.periodEnd} ·{" "}
              {receipt.snapshot.justification}
            </p>
            {receipt.snapshot.lines.map((line) => {
              const group = sheet.rows.find((item) => item.code === line.code);
              return (
                <p key={line.code}>
                  {group?.label}: custo{" "}
                  {money(line.reportedCost, group?.currency)}; previsão{" "}
                  {money(line.forecast, group?.currency)}. Fonte:{" "}
                  {line.source ?? "não disponível"}.
                </p>
              );
            })}
          </article>
        ))}
      </details>
    </>
  );
}
