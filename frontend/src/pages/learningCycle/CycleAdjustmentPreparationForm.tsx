import { useState, type FormEvent } from "react";
import {
  useAdjustmentPreparation,
  usePrepareAdjustmentSuccessor,
} from "../../api/learningCycle/useDecisionProposal";
import {
  cycleError,
  type LearningCycle,
} from "../../api/learningCycle/useLearningCycles";

/** Prepara somente o sucessor elegível, reutilizando aprendizado sem impor datas comerciais. */
export default function CycleAdjustmentPreparationForm({
  cycle,
  onUpdated,
}: {
  cycle: LearningCycle;
  onUpdated: (cycle: LearningCycle) => void;
}) {
  const availability = useAdjustmentPreparation(cycle);
  const mutation = usePrepareAdjustmentSuccessor(cycle);
  const [version, setVersion] = useState("");
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      onUpdated(await mutation.mutateAsync(version.trim()));
    } catch {
      /* A falha permanece disponível no formulário, sem repetir a preparação. */
    }
  }
  if (cycle.status !== "ADJUSTED" || cycle.successorCycleId) return null;
  if (availability.isError)
    return (
      <p role="alert">
        Não foi possível conferir a preparação. Atualize a leitura; nenhum
        sucessor foi criado.
      </p>
    );
  if (!availability.data?.available) return null;
  return (
    <section className="card card-body mb-3">
      <h3 className="h5">Preparar a versão corrigida</h3>
      <p>{availability.data.reason}</p>
      <p>
        O sistema reutiliza o aprendizado e cria um único ciclo e experimento,
        com mídia zerada e datas a definir. A preparação não inicia agentes. A
        implementação e as revisões independentes ainda precisam ser
        comprovadas.
      </p>
      <form aria-label="Preparar sucessor do ajuste privado" onSubmit={submit}>
        <label className="form-label">
          Identificação da nova versão *
          <input
            className="form-control"
            name="productVersion"
            required
            maxLength={64}
            pattern="[a-z0-9][a-z0-9._-]{2,63}"
            value={version}
            onChange={(event) => setVersion(event.target.value)}
          />
        </label>
        {mutation.isError && (
          <p role="alert" className="alert alert-danger">
            {cycleError(mutation.error)}
          </p>
        )}
        <div>
          <button
            className="btn btn-primary"
            type="submit"
            disabled={
              mutation.isPending || version.trim() === cycle.productVersion
            }
          >
            {mutation.isPending
              ? "Preparando…"
              : "Preparar versão corrigida sem gasto"}
          </button>
        </div>
      </form>
    </section>
  );
}
