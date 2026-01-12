import { useEffect, useMemo, useState } from "react";
import { AgentItem, AgentPayload, AgentTheme } from "../../api/agent/types";

interface AgentFormProps {
  initialValue: AgentPayload;
  themes: AgentTheme[];
  onSubmit: (payload: AgentPayload) => void;
  isSubmitting?: boolean;
  submitLabel?: string;
}

type SectionKey = "inputs" | "outputs" | "internalFunctions";

const MODE_SUGGESTIONS = [
  "STANDARD",
  "BATCH",
  "HYBRID",
  "DECISION_GATE",
  "EXTERNAL",
];

function normalizeItems(items?: AgentItem[]): AgentItem[] {
  return (items ?? []).map((item, index) => ({
    ...item,
    orderIndex: index,
  }));
}

export default function AgentForm({
  initialValue,
  themes,
  onSubmit,
  isSubmitting,
  submitLabel = "Salvar",
}: AgentFormProps) {
  const [form, setForm] = useState<AgentPayload>({
    ...initialValue,
    inputs: normalizeItems(initialValue.inputs),
    outputs: normalizeItems(initialValue.outputs),
    internalFunctions: normalizeItems(initialValue.internalFunctions),
  });

  useEffect(() => {
    setForm({
      ...initialValue,
      inputs: normalizeItems(initialValue.inputs),
      outputs: normalizeItems(initialValue.outputs),
      internalFunctions: normalizeItems(initialValue.internalFunctions),
    });
  }, [initialValue]);

  useEffect(() => {
    if (!form.themeId && themes.length > 0) {
      setForm((current) => ({ ...current, themeId: themes[0].id }));
    }
  }, [themes, form.themeId]);

  const canSubmit = useMemo(
    () => Boolean(form.name && form.executionMode && form.themeId),
    [form.executionMode, form.name, form.themeId],
  );

  const updateItem = (
    section: SectionKey,
    index: number,
    field: keyof AgentItem,
    value: string,
  ) => {
    setForm((current) => {
      const list = [...(current[section] ?? [])];
      list[index] = {
        ...list[index],
        [field]: value,
      } as AgentItem;
      return {
        ...current,
        [section]: normalizeItems(list),
      } as AgentPayload;
    });
  };

  const addItem = (section: SectionKey) => {
    setForm((current) => {
      const list = [...(current[section] ?? []), { name: "", type: "", description: "" }];
      return {
        ...current,
        [section]: normalizeItems(list),
      } as AgentPayload;
    });
  };

  const removeItem = (section: SectionKey, index: number) => {
    setForm((current) => {
      const list = [...(current[section] ?? [])];
      list.splice(index, 1);
      return {
        ...current,
        [section]: normalizeItems(list),
      } as AgentPayload;
    });
  };

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    if (!canSubmit) return;

    const payload: AgentPayload = {
      ...form,
      inputs: normalizeItems(form.inputs),
      outputs: normalizeItems(form.outputs),
      internalFunctions: normalizeItems(form.internalFunctions),
    };

    onSubmit(payload);
  };

  const renderSection = (
    title: string,
    section: SectionKey,
    helper?: string,
    emptyCta?: string,
  ) => {
    const items = form[section] ?? [];
    return (
      <div className="card mb-3">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-center mb-2">
            <div>
              <div className="fw-semibold">{title}</div>
              {helper ? (
                <div className="text-body-secondary small">{helper}</div>
              ) : null}
            </div>
            <button
              className="btn btn-outline-primary btn-sm"
              type="button"
              onClick={() => addItem(section)}
            >
              Adicionar
            </button>
          </div>
          {items.length === 0 ? (
            <p className="text-body-secondary small mb-0">
              {emptyCta || "Nenhum item adicionado ainda."}
            </p>
          ) : (
            <div className="vstack gap-3">
              {items.map((item, index) => (
                <div className="border rounded p-3" key={`${section}-${index}`}>
                  <div className="d-flex justify-content-between align-items-start gap-3 mb-2">
                    <div className="flex-grow-1">
                      <label className="form-label small mb-1">Nome</label>
                      <input
                        className="form-control"
                        value={item.name}
                        onChange={(e) =>
                          updateItem(section, index, "name", e.target.value)
                        }
                        placeholder="Ex: Trigger diário, entrada batch"
                      />
                    </div>
                    <div style={{ width: "200px" }}>
                      <label className="form-label small mb-1">Tipo</label>
                      <input
                        className="form-control"
                        value={item.type ?? ""}
                        onChange={(e) =>
                          updateItem(section, index, "type", e.target.value)
                        }
                        placeholder="Standard, Batch, Tool, Gate..."
                      />
                    </div>
                  </div>
                  <label className="form-label small mb-1">Descrição / formato</label>
                  <textarea
                    className="form-control"
                    rows={3}
                    value={item.description ?? ""}
                    onChange={(e) =>
                      updateItem(section, index, "description", e.target.value)
                    }
                    placeholder="Detalhe o que entra ou sai nesse ponto ou como a função interna se comporta"
                  />
                  <div className="d-flex justify-content-between align-items-center mt-2">
                    <span className="text-body-secondary small">
                      Ordem: {index + 1}
                    </span>
                    <button
                      className="btn btn-outline-danger btn-sm"
                      type="button"
                      onClick={() => removeItem(section, index)}
                    >
                      Remover
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    );
  };

  return (
    <form onSubmit={handleSubmit} className="vstack gap-3">
      <div className="row g-3">
        <div className="col-md-6">
          <label className="form-label">
            Nome do agente <span className="text-danger">*</span>
          </label>
          <input
            className="form-control"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            placeholder="Ex: SignalMiner Standard"
          />
        </div>
        <div className="col-md-6">
          <label className="form-label">
            Modo de execução <span className="text-danger">*</span>
          </label>
          <input
            className="form-control"
            value={form.executionMode}
            onChange={(e) =>
              setForm({ ...form, executionMode: e.target.value })
            }
            list="execution-mode-suggestions"
            placeholder="STANDARD, BATCH, HYBRID, DECISION_GATE..."
          />
          <datalist id="execution-mode-suggestions">
            {MODE_SUGGESTIONS.map((mode) => (
              <option key={mode} value={mode} />
            ))}
          </datalist>
        </div>
        <div className="col-md-6">
          <label className="form-label">
            Tema <span className="text-danger">*</span>
          </label>
          <select
            className="form-select"
            value={form.themeId ?? ""}
            onChange={(e) =>
              setForm({ ...form, themeId: Number(e.target.value) })
            }
          >
            <option value="" disabled>
              Selecione um tema
            </option>
            {themes.map((theme) => (
              <option key={theme.id} value={theme.id}>
                {theme.name}
              </option>
            ))}
          </select>
        </div>
        <div className="col-12">
          <label className="form-label">Descrição</label>
          <textarea
            className="form-control"
            rows={3}
            value={form.description ?? ""}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            placeholder="Qual problema o agente resolve, restrições e notas gerais"
          />
        </div>
      </div>

      {renderSection(
        "Entradas do agente",
        "inputs",
        "Liste as fontes e formatos esperados: triggers, decisões, lotes ou sinais externos.",
        "Nenhuma entrada cadastrada. Clique em Adicionar para registrar.",
      )}

      {renderSection(
        "Saídas do agente",
        "outputs",
        "Resultados estruturados, destinos ou persistência esperada para o fluxo.",
        "Nenhuma saída cadastrada ainda.",
      )}

      {renderSection(
        "Funções internas",
        "internalFunctions",
        "Ferramentas internas ou chamadas de tool calling que o agente usa.",
        "Adicione as ferramentas internas, decisions ou web/file search usadas pelo agente.",
      )}

      <div className="d-flex gap-2">
        <button className="btn btn-primary" type="submit" disabled={!canSubmit || isSubmitting}>
          {isSubmitting && (
            <span
              className="spinner-border spinner-border-sm me-2"
              role="status"
              aria-hidden="true"
            />
          )}
          {isSubmitting ? "Salvando..." : submitLabel}
        </button>
        <p className="mb-0 text-body-secondary small align-self-center">
          Itens são salvos na ordem exibida para facilitar o desenho do fluxo do agente.
        </p>
      </div>
    </form>
  );
}
