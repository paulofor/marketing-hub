import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

type Receipt = {
  estimatedCostUsd: number;
  newProviderRequests: number;
  reconciledJobIds: number[];
  providerInvoiceConfirmed: boolean;
};

/** Conecta consumo conferido ao ledger, sem gerar áudio nem substituir aprovações. */
export default function NarrationCostReconciliationPanel({
  experimentId,
}: {
  experimentId?: number;
}) {
  const cache = useQueryClient();
  const [jobIds, setJobIds] = useState("");
  const [usage, setUsage] = useState("");
  const [reference, setReference] = useState("");
  const [exclusive, setExclusive] = useState(false);
  const [estimate, setEstimate] = useState(false);
  const [validation, setValidation] = useState("");
  const command = useMutation({
    mutationFn: async (body: {
      jobIds: number[];
      usage: unknown;
      evidenceReference: string;
      exclusiveUsageConfirmed: boolean;
      estimateFromReceipts: boolean;
    }) =>
      (
        await axios.post<Receipt>(
          `/api/sales-videos/experiments/${experimentId}/narration-costs/reconcile`,
          body,
        )
      ).data,
    onSuccess: () => {
      void cache.invalidateQueries({ queryKey: ["product-sales-video-jobs"] });
      void cache.invalidateQueries({ queryKey: ["process-automation"] });
    },
  });
  const submit = (event: React.FormEvent) => {
    event.preventDefault();
    setValidation("");
    const ids = jobIds
      .trim()
      .split(/[\s,;]+/)
      .map(Number);
    if (
      !experimentId ||
      ids.some((id) => !Number.isSafeInteger(id) || id <= 0) ||
      new Set(ids).size !== ids.length
    ) {
      setValidation("Informe tentativas únicas e um experimento identificado.");
      return;
    }
    let data: unknown = null;
    try {
      data = !estimate && usage.trim() ? JSON.parse(usage) : null;
    } catch {
      setValidation("O extrato de consumo precisa ser um JSON válido.");
      return;
    }
    command.mutate({
      jobIds: ids,
      usage: data,
      evidenceReference: reference,
      exclusiveUsageConfirmed: !estimate && exclusive,
      estimateFromReceipts: estimate,
    });
  };
  const error = axios.isAxiosError(command.error)
    ? command.error.response?.data?.detail ||
      command.error.response?.data?.message ||
      "A evidência não permitiu conciliar a locução."
    : command.error?.message;
  return (
    <details className="mt-3">
      <summary>Conciliação da locução</summary>
      <p>
        Registre o consumo conferido para que o controle financeiro consiga
        utilizá-lo. Recibos e contexto serão validados. A operação mantém os
        arquivos, o orçamento e as decisões de uso.
      </p>
      <form onSubmit={submit}>
        <fieldset disabled={command.isPending || !experimentId}>
          <p>
            Experimento: {experimentId ?? "selecione um vídeo com contexto"}
          </p>
          <label htmlFor="narration-cost-jobs">
            Tentativas de finalização (IDs) *
          </label>
          <input
            id="narration-cost-jobs"
            className="form-control"
            required
            value={jobIds}
            onChange={(e) => setJobIds(e.target.value)}
          />
          <label className="d-block my-2">
            <input
              type="checkbox"
              checked={estimate}
              onChange={(e) => setEstimate(e.target.checked)}
            />{" "}
            Usar estimativa conservadora dos recibos, sem extrato oficial
          </label>
          {estimate && (
            <p role="note">
              O sistema calculará o custo pela duração medida das locuções e
              pelo modelo auditado. O valor serve ao controle do teto; não
              representa cobrança confirmada. Texto e visão via Codex usam
              estimativa própria e não exigem extrato da API.
            </p>
          )}
          <label htmlFor="narration-cost-usage">
            Extrato de consumo OpenAI (JSON)
          </label>
          <textarea
            id="narration-cost-usage"
            className="form-control"
            rows={5}
            disabled={estimate}
            value={usage}
            onChange={(e) => setUsage(e.target.value)}
          />
          <small>
            Use o extrato de usage/completions. Para somente reutilizações ou
            falhas anteriores à voz, deixe vazio; o backend exige prova dessas
            condições. Este campo não recebe chave de API.
          </small>
          <label htmlFor="narration-cost-reference">Fonte consultada *</label>
          <input
            id="narration-cost-reference"
            className="form-control"
            required
            maxLength={1000}
            value={reference}
            onChange={(e) => setReference(e.target.value)}
          />
          <label className="d-block my-2">
            <input
              type="checkbox"
              disabled={estimate}
              checked={exclusive}
              onChange={(e) => setExclusive(e.target.checked)}
            />{" "}
            Conferi que o período do extrato corresponde exclusivamente às
            chamadas selecionadas.
          </label>
          <button type="submit" className="btn btn-outline-primary">
            {command.isPending ? (
              <>
                <span
                  className="spinner-border spinner-border-sm me-2"
                  aria-hidden="true"
                />
                Conciliando…
              </>
            ) : estimate ? (
              "Registrar estimativa da locução"
            ) : (
              "Registrar conciliação da locução"
            )}
          </button>
        </fieldset>
      </form>
      {(validation || error) && <p role="alert">{validation || error}</p>}
      {command.data && (
        <p role="status">
          Conciliação registrada: US${" "}
          {Number(command.data.estimatedCostUsd).toFixed(6)} estimados, em{" "}
          {command.data.reconciledJobIds.length} tentativas. Nenhuma nova
          geração. O valor por chamada continua sem fatura individual
          confirmada.
        </p>
      )}
    </details>
  );
}
