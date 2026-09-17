import { useState, type FormEvent } from "react";
import { createCycleRequestKey } from "../../api/learningCycle/createCycleRequestKey";
import {
  cycleError,
  useWindowRevalidation,
  type LearningCycle,
} from "../../api/learningCycle/useLearningCycles";

function isoDate(date: Date) {
  return date.toISOString().slice(0, 10);
}

/** Permite ao operador renovar o período planejado sem ampliar orçamento ou liberar mídia. */
export default function CycleWindowRevalidationForm({
  cycle,
  onUpdated,
}: {
  cycle: LearningCycle;
  onUpdated: (cycle: LearningCycle) => void;
}) {
  const mutation = useWindowRevalidation(cycle.productId, cycle.id);
  const [requestKey] = useState(createCycleRequestKey);
  const today = new Date();
  const end = new Date(today);
  end.setUTCDate(end.getUTCDate() + 6);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    try {
      onUpdated(
        await mutation.mutateAsync({
          requestKey,
          expectedRevision: cycle.revision,
          startDate: String(data.get("startDate")),
          endDate: String(data.get("endDate")),
          reason: String(data.get("reason")),
        }),
      );
    } catch {
      /* O backend informa a causa contratual no próprio formulário. */
    }
  }
  return (
    <form
      className="card card-body mb-3"
      aria-label="Revalidar janela comercial"
      onSubmit={submit}
    >
      <h3 className="h5">Revalidar janela comercial</h3>
      <p>
        O período anterior venceu antes da ativação. Esta ação preserva produto,
        versão e teto de{" "}
        {cycle.budgetLimitBrl.toLocaleString("pt-BR", {
          style: "currency",
          currency: "BRL",
        })}
        ; não publica campanha nem autoriza gasto externo.
      </p>
      <div className="cycle-form-grid">
        <label className="form-label">
          Início *
          <input
            className="form-control"
            name="startDate"
            type="date"
            min={isoDate(today)}
            defaultValue={isoDate(today)}
            required
          />
        </label>
        <label className="form-label">
          Fim *
          <input
            className="form-control"
            name="endDate"
            type="date"
            min={isoDate(today)}
            defaultValue={isoDate(end)}
            required
          />
        </label>
      </div>
      <label className="form-label">
        Motivo *
        <textarea
          className="form-control"
          name="reason"
          required
          defaultValue="Janela anterior venceu durante a preparação comercial; renovar o mesmo teste após corrigir os gates, sem alterar hipótese, público, oferta, preço ou teto."
        />
      </label>
      {mutation.isError ? (
        <p role="alert" className="alert alert-danger">
          {cycleError(mutation.error)}
        </p>
      ) : null}
      <button
        className="btn btn-primary align-self-start"
        disabled={mutation.isPending}
      >
        {mutation.isPending ? "Revalidando…" : "Revalidar janela"}
      </button>
    </form>
  );
}
