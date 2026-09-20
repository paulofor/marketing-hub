import { type FormEvent } from "react";
import {
  useFinancialPlanPreparation,
  type PlanEnvironment,
  type PlanPreparation,
  type PrepareFinancialPlan,
} from "../../api/financial/useFinancialPlans";

export default function FinancialPlanPreparationEditor({
  ownerId,
  environment,
  busy,
  onSubmit,
  onCancel,
}: {
  ownerId: number;
  environment: PlanEnvironment;
  busy: boolean;
  onSubmit: (request: PrepareFinancialPlan) => Promise<void>;
  onCancel: () => void;
}) {
  const context = useFinancialPlanPreparation(ownerId, environment);
  return (
    <section className="card">
      <div className="card-body d-grid gap-3">
        <h2 className="h5">Prepare o plano em duas escolhas</h2>
        <p className="mb-0">
          As referências financeiras já cadastradas serão reaproveitadas. Custos
          ainda desconhecidos ficam pendentes de revisão.
        </p>
        {context.isLoading && <p role="status">Carregando sugestões...</p>}
        {context.isError && (
          <p className="alert alert-danger" role="alert">
            Não foi possível carregar as sugestões. Tente atualizar as
            referências.
          </p>
        )}
        {context.data && (
          <PreparationForm
            key={`${context.data.expectedRevision}:${context.data.commercialPlanVersion}:${context.data.productVersion}`}
            context={context.data}
            busy={busy || context.isFetching}
            onSubmit={onSubmit}
          />
        )}
        <div className="d-flex flex-wrap gap-2">
          <button
            type="button"
            className="btn btn-outline-secondary"
            disabled={busy || context.isFetching}
            onClick={() => void context.refetch()}
          >
            {context.isFetching ? (
              <>
                <span className="spinner-border spinner-border-sm me-2" />
                Atualizando...
              </>
            ) : (
              "Atualizar referências"
            )}
          </button>
          <button
            type="button"
            className="btn btn-outline-secondary"
            disabled={busy}
            onClick={onCancel}
          >
            Cancelar edição
          </button>
        </div>
      </div>
    </section>
  );
}

function PreparationForm({
  context,
  busy,
  onSubmit,
}: {
  context: PlanPreparation;
  busy: boolean;
  onSubmit: (request: PrepareFinancialPlan) => Promise<void>;
}) {
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    void onSubmit({
      expectedRevision: context.expectedRevision,
      commercialPlanId: context.commercialPlanId,
      commercialPlanVersion: context.commercialPlanVersion,
      productVersion: context.productVersion,
      supportDays: Number(data.get("supportDays")),
      personalizedAi: data.get("personalizedAi") === "true",
    });
  }
  return (
    <form onSubmit={submit}>
      {context.blocker && (
        <p className="alert alert-warning" role="alert">
          {context.blocker}
        </p>
      )}
      <fieldset disabled={busy || !context.canPrepare} className="d-grid gap-3">
        <label>
          Período de suporte (dias) *
          <input
            className="form-control"
            aria-label="Período de suporte (dias)"
            aria-describedby="support-suggestion"
            type="number"
            name="supportDays"
            min={1}
            max={3660}
            step={1}
            required
            defaultValue={context.supportDays}
          />
          <span className="form-text" id="support-suggestion">
            {context.suggestion}
          </span>
        </label>
        <label>
          Geração personalizada com IA *
          <select
            className="form-select"
            aria-label="Geração personalizada com IA"
            name="personalizedAi"
            required
            defaultValue={String(context.personalizedAi)}
          >
            <option value="true">Sim</option>
            <option value="false">Não</option>
          </select>
        </label>
        <p className="small text-muted mb-0">
          Versão {context.productVersion ?? "pendente"} · plano comercial #
          {context.commercialPlanId ?? "pendente"}. Salvar preserva o histórico
          e não altera a oferta publicada.
        </p>
        <button
          className="btn btn-primary"
          type="submit"
          disabled={busy || !context.canPrepare}
        >
          {busy ? (
            <>
              <span className="spinner-border spinner-border-sm me-2" />
              Salvando...
            </>
          ) : (
            "Salvar preparação e calcular"
          )}
        </button>
      </fieldset>
    </form>
  );
}
