import { useState, type FormEvent } from "react";
import type {
  FinancialPlan,
  SaveContributionTarget,
} from "../../api/financial/useFinancialPlans";

export default function FinancialContributionTargetEditor({
  source,
  busy,
  onSubmit,
  onCancel,
}: {
  source: FinancialPlan;
  busy: boolean;
  onSubmit: (request: SaveContributionTarget) => Promise<void>;
  onCancel: () => void;
}) {
  const [target, setTarget] = useState(
    source.assumptions.minimumMarginPercent?.toString() ?? "",
  );
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void onSubmit({
      sourceRevisionId: source.id,
      minimumMarginPercent: Number(target),
    });
  }
  return (
    <section className="card mt-3" aria-label="Decisão da margem mínima">
      <div className="card-body">
        <h2 className="h5">Quanto deve sobrar por venda?</h2>
        <p>
          É a menor sobra aceitável depois de entrega, taxas/impostos e
          aquisição. Essa contribuição ajuda a pagar custos fixos e gerar lucro;
          não é lucro líquido. Neste plano, o percentual divide essa sobra pela
          receita líquida positiva após deduções comerciais, incluindo
          reembolsos, sem contá-los duas vezes.
        </p>
        <p className="small">
          Exemplo fictício: venda de R$ 100, entrega de R$ 20, taxas/impostos de
          R$ 10 e aquisição de R$ 30 deixam R$ 40. São 40% do valor cobrado ou
          44,44% da receita líquida de R$ 90 usada neste campo. O exemplo não
          escolhe sua meta.
        </p>
        <form onSubmit={submit}>
          <fieldset disabled={busy}>
            <label className="form-label" htmlFor="contribution-target">
              Margem mínima proposta (%) *
            </label>
            <input
              id="contribution-target"
              className="form-control"
              aria-label="Margem mínima proposta (%)"
              type="number"
              min="0.01"
              max="99.99"
              step="0.01"
              required
              value={target}
              onChange={(event) => setTarget(event.target.value)}
            />
            <p className="small mt-2">
              Revisão {source.revision} · plano comercial #
              {source.commercialPlanId}. Salvar cria uma revisão e preserva as
              demais premissas. Custos desconhecidos continuam pendentes; a
              decisão não solicita agentes nem autoriza mídia, vídeo pago ou
              cobrança.
            </p>
            <div className="d-flex flex-wrap gap-2">
              <button
                className="btn btn-primary"
                type="submit"
                aria-label="Salvar margem mínima"
              >
                {busy && (
                  <span
                    className="spinner-border spinner-border-sm me-2"
                    role="status"
                    aria-label="Salvando margem"
                  />
                )}
                Salvar margem mínima
              </button>
              <button
                className="btn btn-outline-secondary"
                type="button"
                onClick={onCancel}
              >
                Cancelar
              </button>
            </div>
          </fieldset>
        </form>
      </div>
    </section>
  );
}
