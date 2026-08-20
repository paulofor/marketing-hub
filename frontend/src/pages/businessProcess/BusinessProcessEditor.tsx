import { FormEvent, useState } from "react";
import type {
  BusinessProcessExecutionResource,
  CreateBusinessProcess,
  ProcessFlow,
  ProcessNode,
  ProcessNodeType,
} from "../../api/businessProcess/types";

const nodeTypes: { value: ProcessNodeType; label: string }[] = [
  { value: "START", label: "Início" },
  { value: "TASK", label: "Atividade" },
  { value: "GATEWAY", label: "Decisão" },
  { value: "END", label: "Fim" },
];

type Props = {
  initial: CreateBusinessProcess;
  identityLocked: boolean;
  saving: boolean;
  executionResources: BusinessProcessExecutionResource[];
  resourcesLoading: boolean;
  resourcesUnavailable: boolean;
  onCancel: () => void;
  onSave: (value: CreateBusinessProcess) => Promise<void>;
};

/** Edita metadados, elementos e conexões do grafo BPM estruturado. */
export default function BusinessProcessEditor({
  initial,
  identityLocked,
  saving,
  executionResources,
  resourcesLoading,
  resourcesUnavailable,
  onCancel,
  onSave,
}: Props) {
  const [value, setValue] = useState(initial);

  const updateNode = (index: number, patch: Partial<ProcessNode>) =>
    setValue((current) => ({
      ...current,
      diagram: {
        ...current.diagram,
        nodes: current.diagram.nodes.map((node, position) =>
          position === index ? { ...node, ...patch } : node,
        ),
      },
    }));

  const removeNode = (index: number) => {
    const id = value.diagram.nodes[index].id;
    setValue((current) => ({
      ...current,
      diagram: {
        nodes: current.diagram.nodes.filter(
          (_, position) => position !== index,
        ),
        flows: current.diagram.flows.filter(
          (flow) => flow.from !== id && flow.to !== id,
        ),
      },
    }));
  };

  const updateFlow = (index: number, patch: Partial<ProcessFlow>) =>
    setValue((current) => ({
      ...current,
      diagram: {
        ...current.diagram,
        flows: current.diagram.flows.map((flow, position) =>
          position === index ? { ...flow, ...patch } : flow,
        ),
      },
    }));

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    await onSave(value);
  };

  return (
    <form className="card card-body process-editor" onSubmit={submit}>
      <div className="d-flex justify-content-between gap-3 align-items-start mb-3">
        <div>
          <h2 className="h5 mb-1">Editar definição do processo</h2>
          <p className="small text-body-secondary mb-0">
            Edite propriedades, atividades, decisões, responsáveis e fluxos.
          </p>
        </div>
        <button
          type="button"
          className="btn btn-outline-secondary"
          onClick={onCancel}
        >
          Cancelar
        </button>
      </div>

      <div className="row g-3">
        {[
          ["Código", "processCode", 4],
          ["Nome", "name", 6],
          ["Versão", "versionNumber", 2],
          ["Responsável pelo processo", "ownerName", 6],
          ["Referência técnica", "technicalReference", 6],
        ].map(([label, field, width]) => (
          <div className={`col-md-${width}`} key={String(field)}>
            <label className="form-label">
              {label}
              {field !== "technicalReference" ? " *" : ""}
            </label>
            <input
              className="form-control"
              required={field !== "technicalReference"}
              disabled={
                identityLocked &&
                (field === "processCode" || field === "versionNumber")
              }
              type={field === "versionNumber" ? "number" : "text"}
              min={field === "versionNumber" ? 1 : undefined}
              value={String(value[field as keyof CreateBusinessProcess] ?? "")}
              onChange={(event) =>
                setValue({
                  ...value,
                  [field]:
                    field === "versionNumber"
                      ? Number(event.target.value)
                      : event.target.value,
                })
              }
            />
          </div>
        ))}
        {[
          ["Objetivo", "purpose"],
          ["Evento de início", "triggerDescription"],
          ["Resultado esperado", "outcomeDescription"],
        ].map(([label, field]) => (
          <div
            className={field === "purpose" ? "col-12" : "col-md-6"}
            key={field}
          >
            <label className="form-label">{label} *</label>
            <textarea
              className="form-control"
              rows={2}
              required
              value={String(value[field as keyof CreateBusinessProcess])}
              onChange={(event) =>
                setValue({ ...value, [field]: event.target.value })
              }
            />
          </div>
        ))}
      </div>

      <div className="d-flex justify-content-between align-items-center mt-4 mb-2">
        <h3 className="h6 mb-0">Elementos BPM</h3>
        <button
          type="button"
          className="btn btn-sm btn-outline-primary"
          onClick={() =>
            setValue((current) => ({
              ...current,
              diagram: {
                ...current.diagram,
                nodes: [
                  ...current.diagram.nodes,
                  {
                    id: `element-${Date.now()}`,
                    type: "TASK",
                    label: "Nova atividade",
                  },
                ],
              },
            }))
          }
        >
          Adicionar elemento
        </button>
      </div>
      <div className="process-editor__items">
        {value.diagram.nodes.map((node, index) => (
          <div className="process-editor__item" key={node.id}>
            <select
              className="form-select"
              value={node.type}
              onChange={(e) => {
                const type = e.target.value as ProcessNodeType;
                updateNode(index, {
                  type,
                  executionResourceCode:
                    type === "TASK" ? node.executionResourceCode : undefined,
                });
              }}
            >
              {nodeTypes.map((type) => (
                <option value={type.value} key={type.value}>
                  {type.label}
                </option>
              ))}
            </select>
            <input
              className="form-control"
              aria-label="Identificador"
              required
              value={node.id}
              onChange={(e) => updateNode(index, { id: e.target.value })}
            />
            <input
              className="form-control"
              aria-label="Nome do elemento"
              required
              value={node.label}
              onChange={(e) => updateNode(index, { label: e.target.value })}
            />
            <input
              className="form-control"
              aria-label="Responsável"
              placeholder="Responsável"
              value={node.owner ?? ""}
              onChange={(e) =>
                updateNode(index, { owner: e.target.value || undefined })
              }
            />
            <input
              className="form-control"
              aria-label="Descrição"
              placeholder="Descrição e critério"
              value={node.description ?? ""}
              onChange={(e) =>
                updateNode(index, { description: e.target.value || undefined })
              }
            />
            <button
              type="button"
              className="btn btn-outline-danger"
              onClick={() => removeNode(index)}
              aria-label={`Excluir ${node.label}`}
            >
              Excluir
            </button>
            {node.type === "TASK" ? (
              <div className="process-editor__resource">
                <div>
                  <label
                    className="form-label small fw-semibold"
                    htmlFor={`execution-resource-${node.id}`}
                  >
                    Recurso especializado (opcional)
                  </label>
                  <select
                    id={`execution-resource-${node.id}`}
                    className="form-select"
                    aria-label={`Recurso especializado de ${node.label}`}
                    disabled={resourcesLoading || resourcesUnavailable}
                    value={node.executionResourceCode ?? ""}
                    onChange={(event) =>
                      updateNode(index, {
                        executionResourceCode: event.target.value || undefined,
                      })
                    }
                  >
                    <option value="">
                      {resourcesLoading
                        ? "Carregando recursos..."
                        : resourcesUnavailable
                          ? "Catálogo de recursos indisponível"
                          : "Nenhum recurso especializado"}
                    </option>
                    {node.executionResourceCode &&
                    !executionResources.some(
                      (item) =>
                        item.resourceCode === node.executionResourceCode,
                    ) ? (
                      <option value={node.executionResourceCode}>
                        {node.executionResourceCode} (indisponível)
                      </option>
                    ) : null}
                    {executionResources.map((resource) => (
                      <option
                        value={resource.resourceCode}
                        key={resource.resourceCode}
                      >
                        {resource.name}
                      </option>
                    ))}
                  </select>
                </div>
                {executionResources.find(
                  (item) => item.resourceCode === node.executionResourceCode,
                ) ? (
                  <p className="small text-body-secondary mb-0">
                    {
                      executionResources.find(
                        (item) =>
                          item.resourceCode === node.executionResourceCode,
                      )?.description
                    }{" "}
                    Executor:{" "}
                    <code>
                      {
                        executionResources.find(
                          (item) =>
                            item.resourceCode === node.executionResourceCode,
                        )?.executorReference
                      }
                    </code>
                  </p>
                ) : null}
              </div>
            ) : null}
          </div>
        ))}
      </div>

      <div className="d-flex justify-content-between align-items-center mt-4 mb-2">
        <h3 className="h6 mb-0">Fluxos e condições</h3>
        <button
          type="button"
          className="btn btn-sm btn-outline-primary"
          onClick={() =>
            setValue((current) => ({
              ...current,
              diagram: {
                ...current.diagram,
                flows: [
                  ...current.diagram.flows,
                  {
                    from: current.diagram.nodes[0]?.id ?? "",
                    to: current.diagram.nodes[1]?.id ?? "",
                  },
                ],
              },
            }))
          }
        >
          Adicionar fluxo
        </button>
      </div>
      <div className="process-editor__flows">
        {value.diagram.flows.map((flow, index) => (
          <div
            className="process-editor__flow"
            key={`${index}-${flow.from}-${flow.to}`}
          >
            <select
              className="form-select"
              aria-label="Origem"
              value={flow.from}
              onChange={(e) => updateFlow(index, { from: e.target.value })}
            >
              {value.diagram.nodes.map((node) => (
                <option key={node.id} value={node.id}>
                  {node.label}
                </option>
              ))}
            </select>
            <span aria-hidden="true">→</span>
            <select
              className="form-select"
              aria-label="Destino"
              value={flow.to}
              onChange={(e) => updateFlow(index, { to: e.target.value })}
            >
              {value.diagram.nodes.map((node) => (
                <option key={node.id} value={node.id}>
                  {node.label}
                </option>
              ))}
            </select>
            <input
              className="form-control"
              aria-label="Condição do fluxo"
              placeholder="Condição (opcional)"
              value={flow.label ?? ""}
              onChange={(e) =>
                updateFlow(index, { label: e.target.value || undefined })
              }
            />
            <button
              type="button"
              className="btn btn-outline-danger"
              onClick={() =>
                setValue((current) => ({
                  ...current,
                  diagram: {
                    ...current.diagram,
                    flows: current.diagram.flows.filter(
                      (_, position) => position !== index,
                    ),
                  },
                }))
              }
            >
              Excluir
            </button>
          </div>
        ))}
      </div>

      <div className="text-end mt-4">
        <button className="btn btn-primary" disabled={saving}>
          {saving ? "Salvando..." : "Salvar rascunho"}
        </button>
      </div>
    </form>
  );
}
