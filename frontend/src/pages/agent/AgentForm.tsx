import { useEffect, useMemo, useState } from "react";
import { AgentItem, AgentPayload, AgentTheme } from "../../api/agent/types";
import { uploadAgentPortrait } from "../../api/agent/uploadAgentPortrait";
import { resolveAssetUrl } from "../../utils/resolveAssetUrl";

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
  const [isUploadingPortrait, setIsUploadingPortrait] = useState(false);
  const [portraitError, setPortraitError] = useState("");

  const handlePortraitUpload = async (file?: File) => {
    if (!file) return;
    setPortraitError("");
    setIsUploadingPortrait(true);
    try {
      const uploaded = await uploadAgentPortrait(file);
      setForm((current) => ({
        ...current,
        portraitAssetId: uploaded.assetId,
        portraitUrl: uploaded.url,
      }));
    } catch {
      setPortraitError(
        "Não foi possível enviar a imagem. Confira o formato e o tamanho.",
      );
    } finally {
      setIsUploadingPortrait(false);
    }
  };

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
    () =>
      Boolean(
        form.name && form.nickname.trim() && form.executionMode && form.themeId,
      ),
    [form.executionMode, form.name, form.nickname, form.themeId],
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
      const list = [
        ...(current[section] ?? []),
        { name: "", type: "", description: "" },
      ];
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
                  <label className="form-label small mb-1">
                    Descrição / formato
                  </label>
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
        <div className="col-12">
          <div className="card bg-body-tertiary border-0">
            <div className="card-body d-flex flex-column flex-sm-row align-items-sm-center gap-3">
              {form.portraitUrl ? (
                <img
                  src={resolveAssetUrl(form.portraitUrl)}
                  alt={`Figura mitológica de ${form.nickname || form.name || "agente"}`}
                  className="rounded-circle border bg-white object-fit-cover"
                  width={88}
                  height={88}
                />
              ) : (
                <div
                  className="rounded-circle border bg-white d-flex align-items-center justify-content-center text-body-secondary"
                  style={{ width: 88, height: 88, fontSize: 32, flexShrink: 0 }}
                  aria-label="Agente sem imagem"
                >
                  ◇
                </div>
              )}
              <div className="flex-grow-1">
                <label
                  className="form-label fw-semibold"
                  htmlFor="agent-portrait"
                >
                  Figura mitológica
                </label>
                <input
                  id="agent-portrait"
                  className="form-control"
                  type="file"
                  accept="image/png,image/jpeg,image/webp"
                  disabled={isUploadingPortrait}
                  onChange={(event) =>
                    void handlePortraitUpload(event.target.files?.[0])
                  }
                />
                <div className="form-text">
                  PNG, JPEG ou WebP, até 5 MB. Recomendado: imagem quadrada.
                </div>
                {isUploadingPortrait ? (
                  <div className="small text-primary mt-1">
                    Enviando imagem...
                  </div>
                ) : null}
                {portraitError ? (
                  <div className="small text-danger mt-1" role="alert">
                    {portraitError}
                  </div>
                ) : null}
              </div>
              {form.portraitAssetId ? (
                <button
                  type="button"
                  className="btn btn-sm btn-outline-secondary"
                  onClick={() =>
                    setForm({
                      ...form,
                      portraitAssetId: undefined,
                      portraitUrl: undefined,
                    })
                  }
                >
                  Remover
                </button>
              ) : null}
            </div>
          </div>
        </div>
        <div className="col-md-5">
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
        <div className="col-md-3">
          <label className="form-label" htmlFor="agent-nickname">
            Apelido <span className="text-danger">*</span>
          </label>
          <input
            id="agent-nickname"
            className="form-control"
            value={form.nickname}
            maxLength={60}
            required
            onChange={(e) => setForm({ ...form, nickname: e.target.value })}
            placeholder="Ex: Closer"
          />
          <div className="form-text">
            Nome curto e exclusivo para falar com o agente.
          </div>
        </div>
        <div className="col-md-4">
          <label className="form-label">Chave canônica</label>
          <input
            className="form-control"
            value={form.agentKey ?? ""}
            onChange={(e) => setForm({ ...form, agentKey: e.target.value })}
            placeholder="growth-operator"
          />
        </div>
        <div className="col-md-4">
          <label className="form-label">Status</label>
          <select
            className="form-select"
            value={form.status}
            onChange={(e) => setForm({ ...form, status: e.target.value })}
          >
            {["DRAFT", "TEST", "ACTIVE", "PAUSED", "BLOCKED"].map((status) => (
              <option key={status}>{status}</option>
            ))}
          </select>
        </div>
        <div className="col-md-4">
          <label className="form-label">Responsável</label>
          <input
            className="form-control"
            value={form.ownerName ?? ""}
            onChange={(e) => setForm({ ...form, ownerName: e.target.value })}
          />
        </div>
        <div className="col-md-6">
          <label className="form-label">Objetivo de negócio</label>
          <textarea
            className="form-control"
            rows={3}
            value={form.businessObjective ?? ""}
            onChange={(e) =>
              setForm({ ...form, businessObjective: e.target.value })
            }
          />
        </div>
        <div className="col-md-6">
          <label className="form-label">Métricas de sucesso</label>
          <textarea
            className="form-control"
            rows={3}
            value={form.successMetrics ?? ""}
            onChange={(e) =>
              setForm({ ...form, successMetrics: e.target.value })
            }
          />
        </div>
        <div className="col-md-4">
          <label className="form-label">Modelo</label>
          <input
            className="form-control"
            value={form.modelName ?? ""}
            onChange={(e) => setForm({ ...form, modelName: e.target.value })}
            placeholder="gpt-5.6-sol"
          />
        </div>
        <div className="col-md-8">
          <label className="form-label">Gatilhos de execução</label>
          <input
            className="form-control"
            value={form.triggerPolicy ?? ""}
            onChange={(e) =>
              setForm({ ...form, triggerPolicy: e.target.value })
            }
          />
        </div>
        <div className="col-12">
          <label className="form-label" htmlFor="agent-responsibility-contract">
            Responsabilidade do agente
          </label>
          <textarea
            id="agent-responsibility-contract"
            className="form-control"
            rows={4}
            value={form.responsibilityContract ?? ""}
            onChange={(e) =>
              setForm({ ...form, responsibilityContract: e.target.value })
            }
            placeholder="O que este agente deve resolver, por quem e até onde vai sua responsabilidade"
          />
        </div>
        <div className="col-12">
          <label className="form-label" htmlFor="agent-orchestrator-policy">
            Regras para o Orquestrador
          </label>
          <textarea
            id="agent-orchestrator-policy"
            className="form-control"
            rows={4}
            value={form.orchestratorPolicy ?? ""}
            onChange={(e) =>
              setForm({ ...form, orchestratorPolicy: e.target.value })
            }
            placeholder="Quando acionar, pré-condições, prioridade, bloqueios e quando encaminhar para decisão humana"
          />
          <div className="form-text">
            Estas regras coordenam o agente, mas não ampliam sua autoridade.
          </div>
        </div>
        <div className="col-md-6">
          <label className="form-label" htmlFor="agent-analysis-policy">
            O que deve analisar
          </label>
          <textarea
            id="agent-analysis-policy"
            className="form-control"
            rows={4}
            value={form.analysisPolicy ?? ""}
            onChange={(e) =>
              setForm({ ...form, analysisPolicy: e.target.value })
            }
            placeholder="Evidências, comparações, critérios, riscos e perguntas que precisam ser respondidas"
          />
        </div>
        <div className="col-md-6">
          <label className="form-label" htmlFor="agent-offering-policy">
            O que deve oferecer
          </label>
          <textarea
            id="agent-offering-policy"
            className="form-control"
            rows={4}
            value={form.offeringPolicy ?? ""}
            onChange={(e) =>
              setForm({ ...form, offeringPolicy: e.target.value })
            }
            placeholder="Recomendação, decisão, artefato ou próximo passo que deve entregar ao fluxo"
          />
        </div>
        <div className="col-12">
          <label className="form-label">
            Política de autoridade e aprovações
          </label>
          <textarea
            className="form-control"
            rows={3}
            value={form.authorityPolicy ?? ""}
            onChange={(e) =>
              setForm({ ...form, authorityPolicy: e.target.value })
            }
          />
        </div>
        <div className="col-md-6">
          <label className="form-label">Contrato do prompt</label>
          <input
            className="form-control"
            value={form.promptContractPath ?? ""}
            onChange={(e) =>
              setForm({ ...form, promptContractPath: e.target.value })
            }
          />
        </div>
        <div className="col-md-6">
          <label className="form-label">Contrato do schema</label>
          <input
            className="form-control"
            value={form.schemaContractPath ?? ""}
            onChange={(e) =>
              setForm({ ...form, schemaContractPath: e.target.value })
            }
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
        "Informações que o agente deve receber",
        "inputs",
        "Liste fontes, contexto obrigatório, evidências e formatos esperados.",
        "Nenhuma entrada cadastrada. Clique em Adicionar para registrar.",
      )}

      {renderSection(
        "Saídas e entregáveis do agente",
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
        <button
          className="btn btn-primary"
          type="submit"
          disabled={!canSubmit || isSubmitting || isUploadingPortrait}
        >
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
          Itens são salvos na ordem exibida para facilitar o desenho do fluxo do
          agente.
        </p>
      </div>
    </form>
  );
}
