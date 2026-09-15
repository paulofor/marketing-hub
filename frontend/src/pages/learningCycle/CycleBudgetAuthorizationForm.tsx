import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { createCycleRequestKey } from "../../api/learningCycle/createCycleRequestKey";
import {
  cycleError,
  useCycleMutation,
  type LearningCycle,
} from "../../api/learningCycle/useLearningCycles";

export default function CycleBudgetAuthorizationForm({
  cycle,
  onUpdated,
}: {
  cycle: LearningCycle;
  onUpdated: (cycle: LearningCycle) => void;
}) {
  const mutation = useCycleMutation(cycle.productId, cycle.id);
  const [requestKey] = useState(createCycleRequestKey);
  const [error, setError] = useState("");
  const command = cycle.commands.find((item) => item.action === "COMPLETE");
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!command?.available || mutation.isPending) return;
    const data = new FormData(event.currentTarget);
    const dailyBudgetBrl = Number(data.get("dailyBudgetBrl"));
    const budgetLimitBrl = Number(data.get("budgetLimitBrl"));
    if (!(
      dailyBudgetBrl > 0 &&
      budgetLimitBrl > 0 &&
      dailyBudgetBrl <= budgetLimitBrl
    )) {
      setError(
        "Informe valores positivos. O diário não pode ultrapassar o total.",
      );
      return;
    }
    setError("");
    try {
      onUpdated(
        await mutation.mutateAsync({
          budgetAuthorization: true,
          requestKey,
          expectedRevision: cycle.revision,
          dailyBudgetBrl,
          budgetLimitBrl,
        }),
      );
    } catch {
      /* O erro do backend permanece visível para correção. */
    }
  }
  return (
    <form
      id="cycle-decision"
      aria-label="Decisão do ciclo"
      className="card card-body mb-3"
      onSubmit={submit}
    >
      <h3 className="h5">Aprovar orçamento</h3>
      <p>
        Confira a sugestão ou ajuste os valores. Ao aprovar, você confirma estes
        limites de mídia para o experimento #{cycle.experimentId}. A ativação da
        campanha continua dependendo da preparação e da autorização final.
      </p>
      <div className="cycle-form-grid" key={cycle.revision}>
        <label className="form-label">
          Orçamento diário (R$)
          <input
            name="dailyBudgetBrl"
            className="form-control"
            type="number"
            min="0.01"
            step="0.01"
            required
            defaultValue={cycle.authorizationReview?.dailyBudgetBrl ?? ""}
          />
        </label>
        <label className="form-label">
          Orçamento total (R$)
          <input
            name="budgetLimitBrl"
            className="form-control"
            type="number"
            min="0.01"
            step="0.01"
            required
            defaultValue={cycle.budgetLimitBrl > 0 ? cycle.budgetLimitBrl : ""}
          />
        </label>
      </div>
      {!command?.available && (
        <p role="alert" className="alert alert-warning">
          {command?.reason || cycle.nextAction}
        </p>
      )}
      {cycle.commercialPreparation &&
        !cycle.commercialPreparation.readyForReview && (
          <p>
            {cycle.commercialPreparation.guidance}{" "}
            <Link to={cycle.commercialPreparation.experimentUrl}>
              Ver preparação do experimento
            </Link>
          </p>
        )}
      {(error || mutation.isError) && (
        <p role="alert" className="alert alert-danger">
          {error || cycleError(mutation.error)}
        </p>
      )}
      <button
        type="submit"
        className="btn btn-primary align-self-start"
        disabled={mutation.isPending || !command?.available}
      >
        {mutation.isPending ? "Registrando…" : "Aprovar orçamento"}
      </button>
    </form>
  );
}
