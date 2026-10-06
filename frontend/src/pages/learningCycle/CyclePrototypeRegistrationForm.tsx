import { useState, type FormEvent } from "react";
import { createCycleRequestKey } from "../../api/learningCycle/createCycleRequestKey";
import {
  cycleError,
  useCycleMutation,
  type LearningCycle,
} from "../../api/learningCycle/useLearningCycles";

const checks = [
  ["desktopValidated", "Desktop validado"],
  ["mobileValidated", "Celular validado"],
  ["firstResultValidated", "Primeiro resultado aplicável validado"],
  ["resumeValidated", "Salvar e retomar validados"],
  ["failuresValidated", "Falhas e recuperação validadas"],
  ["testDataExcluded", "Dados de teste segregados"],
  ["noExternalSideEffects", "Sem cobrança, campanha ou gasto de mídia"],
];

/** Registra somente a prova inicial da versão planejada quando o backend disponibiliza o comando. */
export default function CyclePrototypeRegistrationForm({
  cycle,
  onUpdated,
}: {
  cycle: LearningCycle;
  onUpdated: (cycle: LearningCycle) => void;
}) {
  const mutation = useCycleMutation(cycle.productId, cycle.id);
  const [requestKey] = useState(createCycleRequestKey);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    try {
      onUpdated(
        await mutation.mutateAsync({
          prototypeRegistration: true,
          requestKey,
          expectedRevision: cycle.revision,
          operatorName: form.get("operatorName"),
          privatePrototype: {
            prototypeVersion: cycle.productVersion,
            privateAccessUrl: form.get("privateAccessUrl"),
            image: form.get("prototypeImage"),
            evidenceReference: form.get("prototypeEvidence"),
            observedAt: new Date(
              String(form.get("prototypeObservedAt")),
            ).toISOString(),
            ...Object.fromEntries(checks.map(([key]) => [key, form.has(key)])),
          },
        }),
      );
    } catch {
      /* O erro contratual permanece visível para correção. */
    }
  }
  if (!cycle.prototypeRegistration?.available) return null;
  return (
    <details className="card card-body mb-3">
      <summary>Registrar implementação já testada</summary>
      <p>{cycle.prototypeRegistration.reason}</p>
      <p>
        Versão declarada: <strong>{cycle.productVersion}</strong>. As revisões
        independentes continuam obrigatórias.
      </p>
      <form
        onSubmit={submit}
        aria-label="Registrar prova da implementação privada"
      >
        <div className="cycle-form-grid">
          <label className="form-label">
            Responsável pelo registro *
            <input
              className="form-control"
              name="operatorName"
              required
              maxLength={160}
            />
          </label>
          <label className="form-label">
            URL privada sem parâmetros *
            <input
              className="form-control"
              type="url"
              name="privateAccessUrl"
              required
            />
          </label>
          <label className="form-label">
            Imagem Docker testada *
            <input className="form-control" name="prototypeImage" required />
          </label>
          <label className="form-label">
            Relatório dos testes *
            <textarea
              className="form-control"
              name="prototypeEvidence"
              required
              minLength={20}
              maxLength={1200}
            />
          </label>
          <label className="form-label">
            Data da verificação *
            <input
              className="form-control"
              type="datetime-local"
              name="prototypeObservedAt"
              required
            />
          </label>
          {checks.map(([key, label]) => (
            <label className="form-check" key={key}>
              <input
                type="checkbox"
                name={key}
                required
                className="form-check-input"
              />
              {label} *
            </label>
          ))}
        </div>
        {mutation.isError && (
          <p role="alert" className="alert alert-danger">
            {cycleError(mutation.error)}
          </p>
        )}
        <button
          className="btn btn-primary"
          type="submit"
          disabled={mutation.isPending}
        >
          {mutation.isPending && (
            <span
              className="spinner-border spinner-border-sm me-2"
              aria-hidden="true"
            />
          )}
          {mutation.isPending ? "Registrando…" : "Registrar prova privada"}
        </button>
      </form>
    </details>
  );
}
