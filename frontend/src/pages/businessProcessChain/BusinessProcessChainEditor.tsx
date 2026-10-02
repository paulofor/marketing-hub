import { FormEvent, useState } from "react";
import { useBusinessProcesses } from "../../api/businessProcess/useBusinessProcesses";
import type {
  BusinessProcessChainDetail,
  BusinessProcessChainSave,
} from "../../api/businessProcessChain/types";

/** Edita o rascunho usando versões publicadas, sem alterar execuções históricas. */
export default function BusinessProcessChainEditor({
  chain,
  saving,
  onSave,
  onCancel,
}: {
  chain: BusinessProcessChainDetail;
  saving: boolean;
  onSave: (value: BusinessProcessChainSave) => Promise<void>;
  onCancel: () => void;
}) {
  const catalog = useBusinessProcesses();
  const [value, setValue] = useState<BusinessProcessChainSave>({
    name: chain.name,
    purpose: chain.purpose,
    outcomeDescription: chain.outcomeDescription,
    primaryMetric: chain.primaryMetric,
    processes: chain.processes.map(
      ({ processDefinitionId, valueContribution }) => ({
        processDefinitionId,
        valueContribution,
      }),
    ),
  });
  const choices = (catalog.data ?? []).filter(
    (p) => p.status === "PUBLISHED" && p.processType === "VALUE_PROCESS",
  );
  const valid =
    value.processes.length > 0 &&
    value.processes.every((p) =>
      choices.some((c) => c.id === p.processDefinitionId),
    );
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (valid && !saving) await onSave(value);
  }
  return (
    <form
      className="card card-body mb-3"
      onSubmit={submit}
      aria-label="Editar rascunho da cadeia"
    >
      <h2 className="h5">Editar cadeia · v{chain.versionNumber}</h2>
      <p>
        Salvar altera apenas este rascunho. Publicar preserva as versões e
        execuções anteriores.
      </p>
      <fieldset disabled={saving}>
        {(
          [
            ["name", "Nome da cadeia", 160],
            ["purpose", "Propósito da cadeia", undefined],
            ["outcomeDescription", "Resultado da cadeia", 500],
            ["primaryMetric", "Métrica principal", 200],
          ] as const
        ).map(([field, label, maxLength]) => (
          <label className="form-label d-block" key={field}>
            {label}
            <textarea
              className="form-control"
              required
              maxLength={maxLength}
              rows={field === "purpose" ? 5 : 2}
              value={value[field]}
              onChange={(e) => setValue({ ...value, [field]: e.target.value })}
            />
          </label>
        ))}
        {catalog.isError ? (
          <p role="alert">
            Não foi possível consultar os processos.{" "}
            <button type="button" onClick={() => catalog.refetch()}>
              Tentar novamente
            </button>
          </p>
        ) : null}
        {value.processes.map((item, index) => (
          <fieldset className="border rounded p-3 mb-3" key={index}>
            <legend className="h6">Processo {index + 1}</legend>
            <label className="form-label d-block">
              Versão do processo {index + 1}
              <select
                className="form-select"
                required
                disabled={catalog.isLoading || catalog.isError}
                value={item.processDefinitionId}
                onChange={(e) =>
                  setValue({
                    ...value,
                    processes: value.processes.map((p, i) =>
                      i === index
                        ? { ...p, processDefinitionId: Number(e.target.value) }
                        : p,
                    ),
                  })
                }
              >
                {!choices.some((p) => p.id === item.processDefinitionId) ? (
                  <option value={item.processDefinitionId}>
                    Selecione uma versão publicada
                  </option>
                ) : null}
                {choices.map((p) => (
                  <option value={p.id} key={p.id}>
                    {p.name} · v{p.versionNumber}
                  </option>
                ))}
              </select>
            </label>
            <label className="form-label d-block">
              Contribuição do processo {index + 1}
              <textarea
                className="form-control"
                required
                maxLength={500}
                rows={3}
                value={item.valueContribution}
                onChange={(e) =>
                  setValue({
                    ...value,
                    processes: value.processes.map((p, i) =>
                      i === index
                        ? { ...p, valueContribution: e.target.value }
                        : p,
                    ),
                  })
                }
              />
            </label>
          </fieldset>
        ))}
        <div className="d-flex gap-2">
          <button
            className="btn btn-primary"
            disabled={!valid || catalog.isLoading || catalog.isError}
          >
            {saving ? "Salvando..." : "Salvar rascunho da cadeia"}
          </button>
          <button
            className="btn btn-outline-secondary"
            type="button"
            onClick={onCancel}
          >
            Cancelar edição
          </button>
        </div>
      </fieldset>
    </form>
  );
}
