import { useRef, useState, type FormEvent } from "react";
import { createCycleRequestKey } from "../../api/learningCycle/createCycleRequestKey";
import { cycleError } from "../../api/learningCycle/useLearningCycles";
import {
  useAuthorizeVideoBudget,
  type VideoBudget,
} from "../../api/financial/useVideoBudget";

/** Recolhe o teto compartilhado, sem pré-selecionar valor ou autorização. */
export default function VideoBudgetForm({
  budget,
  disabled = false,
}: {
  budget: VideoBudget;
  disabled?: boolean;
}) {
  const mutation = useAuthorizeVideoBudget(
    budget.productId,
    budget.cycleId,
    budget.chainDefinitionId,
  );
  const attempt = useRef({ signature: "", requestKey: "" });
  const saving = useRef(false);
  const [validationError, setValidationError] = useState("");
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (saving.current || disabled) return;
    const form = new FormData(event.currentTarget);
    const amount = String(form.get("budgetLimitUsd") ?? "").trim();
    if (
      !/^\d{1,6}([,.]\d{1,2})?$/.test(amount) ||
      Number(amount.replace(",", ".")) <= 0
    ) {
      setValidationError(
        "Informe um teto positivo em USD, com até duas casas decimais.",
      );
      return;
    }
    const body = {
      expectedRevision: budget.revision,
      chainDefinitionId: budget.chainDefinitionId,
      experimentId: budget.experimentId,
      productVersion: budget.productVersion,
      budgetLimitUsd: amount.replace(",", "."),
      operatorName: String(form.get("operatorName") ?? "").trim(),
      justification: String(form.get("justification") ?? "").trim(),
      confirmed: form.has("confirmed"),
    };
    if (!body.operatorName || !body.justification || !body.confirmed) {
      setValidationError(
        "Informe responsável, justificativa e confirme o escopo.",
      );
      return;
    }
    const signature = JSON.stringify(body);
    if (attempt.current.signature !== signature)
      attempt.current = { signature, requestKey: createCycleRequestKey() };
    setValidationError("");
    saving.current = true;
    try {
      await mutation.mutateAsync({
        ...body,
        requestKey: attempt.current.requestKey,
      });
    } catch {
      // A falha do contrato permanece visível, com os dados disponíveis para nova tentativa.
    } finally {
      saving.current = false;
    }
  }
  return (
    <form
      className="card card-body"
      aria-label="Autorizar teto dos vídeos"
      onSubmit={submit}
    >
      <h2 className="h5">
        {budget.currentAuthorization
          ? "Substituir teto total"
          : "Autorizar teto total"}
      </h2>
      <p>
        Um único limite para o anúncio, a demonstração na entrada e as revisões
        das duas peças.
      </p>
      {budget.currentAuthorization ? (
        <p role="note">
          O novo teto substitui o anterior e inclui todo o consumo já realizado
          nesta preparação. Os valores não se somam; custos e reservas
          anteriores continuam considerados.
        </p>
      ) : null}
      <fieldset disabled={mutation.isPending || disabled}>
        <label className="form-label d-block">
          Teto total autorizado (USD) *
          <div className="input-group mt-1">
            <span className="input-group-text">US$</span>
            <input
              name="budgetLimitUsd"
              className="form-control"
              type="text"
              inputMode="decimal"
              required
              maxLength={9}
              placeholder="Informe o valor"
              aria-describedby="video-budget-help"
            />
          </div>
        </label>
        <p className="small text-body-secondary" id="video-budget-help">
          Valor em dólares, sem conversão automática. Este limite é
          compartilhado; não se repete para cada vídeo.
        </p>
        <label className="form-label d-block">
          Responsável pela autorização *
          <input
            name="operatorName"
            className="form-control"
            required
            maxLength={160}
            autoComplete="name"
          />
        </label>
        <label className="form-label d-block">
          Objetivo e justificativa do investimento *
          <textarea
            name="justification"
            className="form-control"
            required
            maxLength={4000}
            rows={3}
            placeholder="Qual benefício real as peças devem demonstrar e qual resultado queremos testar?"
          />
        </label>
        <label className="form-check my-3">
          <input
            className="form-check-input"
            name="confirmed"
            type="checkbox"
            required
          />
          <span className="form-check-label">
            Autorizo este teto total somente para produzir e revisar as duas
            peças, sujeito à avaliação financeira. Mídia, cobrança e publicação
            comercial exigem autorizações próprias. *
          </span>
        </label>
        <button
          type="submit"
          className="btn btn-primary"
          disabled={mutation.isPending || disabled}
        >
          {mutation.isPending ? (
            <span
              className="spinner-border spinner-border-sm me-2"
              role="status"
              aria-label="Registrando autorização"
            />
          ) : null}
          {budget.currentAuthorization
            ? "Registrar novo teto"
            : "Registrar teto autorizado"}
        </button>
      </fieldset>
      {validationError || mutation.isError ? (
        <p role="alert" className="alert alert-danger mt-3 mb-0">
          {validationError || cycleError(mutation.error)}
        </p>
      ) : null}
    </form>
  );
}
