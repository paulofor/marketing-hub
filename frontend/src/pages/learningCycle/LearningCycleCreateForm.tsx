import { createCycleRequestKey } from "../../api/learningCycle/createCycleRequestKey";
import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import {
  cycleError,
  useCycleMutation,
  type CycleCatalog,
  type LearningCycle,
} from "../../api/learningCycle/useLearningCycles";

const textFields = [
  ["productVersion", "Versão exata do produto"],
  ["hypothesis", "Hipótese verificável"],
  ["mainChange", "Variável principal que será alterada"],
  ["successCriterion", "Resultado esperado e critério de decisão"],
  ["audience", "Público"],
  ["offer", "Oferta e preço"],
  ["acquisition", "Canal e criativo"],
  ["operatorName", "Responsável pelo registro"],
];
export default function LearningCycleCreateForm({
  productId,
  chainId,
  catalog,
  predecessor,
  onCreated,
  onCancel,
}: {
  productId: number;
  chainId: number;
  catalog: CycleCatalog;
  predecessor?: LearningCycle;
  onCreated: (cycle: LearningCycle) => void;
  onCancel: () => void;
}) {
  const mutation = useCycleMutation(productId);
  const [requestKey] = useState(() => createCycleRequestKey());
  const [experimentId, setExperimentId] = useState("");
  const option = catalog.experiments.find(
    (experiment) => experiment.id === Number(experimentId),
  );
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const body: Record<string, unknown> = Object.fromEntries(form);
    Object.assign(body, {
      requestKey,
      chainDefinitionId: chainId,
      experimentId: Number(experimentId),
      previousCycleId: predecessor?.id ?? null,
      baseline: option?.baseline ?? false,
      budgetLimitBrl: Number(form.get("budgetLimitBrl")),
      sampleTarget: Number(form.get("sampleTarget")),
      minimumNetSales: Number(form.get("minimumNetSales")),
      windowStart: new Date(String(form.get("windowStart"))).toISOString(),
      windowEnd: new Date(String(form.get("windowEnd"))).toISOString(),
    });
    try {
      onCreated(await mutation.mutateAsync(body));
    } catch {
      /* O erro é apresentado pelo estado da mutação. */
    }
  }
  return (
    <form
      className="card card-body mb-3"
      onSubmit={submit}
      aria-label="Abrir ciclo de aprendizado"
    >
      <h2 className="h5">
        {predecessor
          ? `Sucessor do ciclo #${predecessor.id} · experimento #${predecessor.experimentId}`
          : "Abrir ciclo de aprendizado"}
      </h2>
      <p>
        Escolha explicitamente um experimento deste produto. Um sucessor usa um
        novo experimento planejado; uma referência histórica começa pela
        conciliação.
      </p>
      <Link
        className="mb-3"
        to="/experiments/manual/new"
        target="_blank"
        rel="noopener noreferrer"
      >
        Criar novo experimento pelo cadastro oficial
      </Link>
      <label className="form-label">
        Experimento *
        <select
          required
          className="form-select"
          name="experimentId"
          value={experimentId}
          onChange={(event) => setExperimentId(event.target.value)}
        >
          <option value="">Selecione</option>
          {catalog.experiments.map((experiment) => (
            <option
              key={experiment.id}
              value={experiment.id}
              disabled={
                !experiment.available || (!!predecessor && experiment.baseline)
              }
            >
              {experiment.name} · #{experiment.id} · {experiment.status}
            </option>
          ))}
        </select>
      </label>
      {option ? <p className="small">{option.reason}</p> : null}
      <div className="cycle-form-grid">
        {textFields.map(([name, label]) => (
          <label className="form-label" key={name}>
            {label} *
            <input
              className="form-control"
              name={name}
              required
              maxLength={
                name === "productVersion" || name === "operatorName" ? 160 : 500
              }
            />
          </label>
        ))}
        <label className="form-label">
          Teto total de mídia (R$) *
          <input
            className="form-control"
            name="budgetLimitBrl"
            required
            type="number"
            min="0"
            step="0.01"
          />
        </label>
        <label className="form-label">
          Amostra mínima de sessões *
          <input
            className="form-control"
            name="sampleTarget"
            required
            type="number"
            min="1"
            max="10000000"
          />
        </label>
        <label className="form-label">
          Mínimo de vendas líquidas para avaliar escala *
          <input
            className="form-control"
            name="minimumNetSales"
            required
            type="number"
            min="1"
            max="1000000"
          />
        </label>
        <label className="form-label">
          Início da janela *
          <input
            className="form-control"
            name="windowStart"
            required
            type="datetime-local"
          />
        </label>
        <label className="form-label">
          Fim da janela *
          <input
            className="form-control"
            name="windowEnd"
            required
            type="datetime-local"
          />
        </label>
      </div>
      {mutation.isError ? (
        <p className="alert alert-danger" role="alert">
          {cycleError(mutation.error)}
        </p>
      ) : null}
      <div className="d-flex gap-2">
        <button
          type="submit"
          className="btn btn-primary"
          disabled={mutation.isPending || !option?.available}
        >
          {mutation.isPending ? (
            <span
              className="spinner-border spinner-border-sm me-2"
              role="status"
              aria-label="Salvando"
            />
          ) : null}
          Abrir ciclo
        </button>
        <button
          className="btn btn-outline-secondary"
          type="button"
          disabled={mutation.isPending}
          onClick={onCancel}
        >
          Cancelar
        </button>
      </div>
    </form>
  );
}
