import { FormEvent, useEffect, useMemo, useState } from "react";
import axios from "axios";
import { useQueryClient } from "@tanstack/react-query";

import { useLeadPortalFlows } from "../../api/leadPortal/useLeadPortalFlows";
import { useLeadPortalImagePromptMetadata } from "../../api/leadPortal/useLeadPortalImagePromptMetadata";
import {
  useUpdateLeadPortalImagePrompt,
  type UpdateLeadPortalImagePromptPayload,
} from "../../api/leadPortal/useUpdateLeadPortalImagePrompt";

import "./LeadPortalImagePromptPage.css";

interface FormState {
  template: string;
  batchSize: number | null;
}

interface FeedbackState {
  variant: "success" | "error";
  message: string;
}

export default function LeadPortalImagePromptPage() {
  const { data: flows, isLoading, isError, error } = useLeadPortalFlows();
  const { data: metadata, isLoading: isLoadingMetadata } =
    useLeadPortalImagePromptMetadata();
  const updatePrompt = useUpdateLeadPortalImagePrompt();
  const queryClient = useQueryClient();

  const [search, setSearch] = useState("");
  const [selectedFlowId, setSelectedFlowId] = useState<number | null>(null);
  const [formState, setFormState] = useState<FormState>({
    template: "",
    batchSize: null,
  });
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);

  const flowList = flows ?? [];
  const filteredFlows = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return flowList;
    return flowList.filter((flow) =>
      [flow.name, flow.slug]
        .filter(Boolean)
        .some((value) => value!.toLowerCase().includes(query)),
    );
  }, [flowList, search]);

  const selectedFlow = useMemo(
    () => flowList.find((flow) => flow.id === selectedFlowId) ?? null,
    [flowList, selectedFlowId],
  );

  const isExperimentManaged = Boolean(selectedFlow?.experimentId);
  const resolvedModel =
    selectedFlow?.imagePromptModel ?? metadata?.defaultModel ?? "";

  useEffect(() => {
    if (!metadata) return;
    if (!selectedFlow) {
      setFormState({
        template: metadata.defaultTemplate,
        batchSize: metadata.defaultBatchSize,
      });
    }
  }, [metadata, selectedFlow]);

  useEffect(() => {
    if (!metadata || !selectedFlow) return;
    setFormState({
      template:
        selectedFlow.imagePromptTemplate ?? metadata.defaultTemplate ?? "",
      batchSize:
        selectedFlow.imagePromptBatchSize ?? metadata.defaultBatchSize ?? null,
    });
  }, [selectedFlow, metadata]);

  const handleFlowSelection = (flowId: number) => {
    setSelectedFlowId(flowId);
    setFeedback(null);
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedFlow) {
      setFeedback({
        variant: "error",
        message: "Selecione um fluxo para editar o prompt.",
      });
      return;
    }

    setFeedback(null);
    try {
      const trimmedTemplate = formState.template.trim();
      const normalizedBatch =
        formState.batchSize && formState.batchSize > 0
          ? formState.batchSize
          : null;
      const payload: UpdateLeadPortalImagePromptPayload = {
        imagePromptTemplate: trimmedTemplate || null,
      };
      if (!isExperimentManaged) {
        payload.imagePromptBatchSize = normalizedBatch;
      }
      await updatePrompt.mutateAsync({
        id: selectedFlow.id,
        payload,
      });
      await queryClient.invalidateQueries({ queryKey: ["lead-portal-flows"] });
      setFeedback({
        variant: "success",
        message: "Prompt atualizado com sucesso.",
      });
    } catch (err) {
      const message = axios.isAxiosError(err)
        ? err.response?.data?.message ?? "Não foi possível salvar o prompt."
        : "Não foi possível salvar o prompt.";
      setFeedback({ variant: "error", message });
    }
  };

  const handleRestoreTemplate = () => {
    if (!metadata) return;
    setFormState((current) => ({
      ...current,
      template: metadata.defaultTemplate,
    }));
  };

  const handleRestoreDefaults = () => {
    if (!metadata) return;
    setFormState({
      template: metadata.defaultTemplate,
      batchSize: metadata.defaultBatchSize,
    });
  };

  return (
    <div className="lp-image-prompt-page">
      <header className="lp-image-prompt-header">
        <div>
          <h1>Prompt de geração de imagens</h1>
          <p>
            Personalize o texto enviado ao worker de IA e acompanhe o modelo e
            o tamanho do lote definidos em cada fluxo publicado no Lead Portal.
          </p>
        </div>
        <div className="lp-image-prompt-search">
          <input
            type="search"
            placeholder="Buscar fluxo pelo nome ou slug"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>
      </header>

      <div className="lp-image-prompt-layout">
        <section className="lp-image-prompt-flows">
          <div className="lp-card">
            <div className="lp-card-header">
              <h2>Fluxos simples</h2>
              <span>{filteredFlows.length} itens</span>
            </div>
            {isLoading ? (
              <p className="lp-muted">Carregando fluxos...</p>
            ) : isError ? (
              <p className="lp-error">{String(error)}</p>
            ) : filteredFlows.length === 0 ? (
              <p className="lp-muted">Nenhum fluxo encontrado.</p>
            ) : (
              <ul className="lp-flow-list">
                {filteredFlows.map((flow) => (
                  <li
                    key={flow.id}
                    className={
                      flow.id === selectedFlowId ? "is-selected" : undefined
                    }
                  >
                    <button
                      type="button"
                      onClick={() => handleFlowSelection(flow.id)}
                    >
                      <span className="lp-flow-name">{flow.name}</span>
                      <span className="lp-flow-meta">{flow.slug}</span>
                      <span className="lp-flow-meta">
                        Prompt {flow.imagePromptTemplate ? "customizado" : "padrão"}
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </section>

        <section className="lp-image-prompt-editor">
          <div className="lp-card">
            <div className="lp-card-header">
              <h2>Edição do prompt</h2>
              {selectedFlow ? (
                <span className="lp-flow-meta">{selectedFlow.slug}</span>
              ) : null}
            </div>

            {!selectedFlow ? (
              <p className="lp-muted">
                Escolha um fluxo na lista ao lado para editar o prompt.
              </p>
            ) : (
              <form onSubmit={handleSubmit} className="lp-form">
                {feedback ? (
                  <div className={`lp-feedback is-${feedback.variant}`}>
                    {feedback.message}
                  </div>
                ) : null}

                <div className="lp-field">
                  <span>Modelo de imagem</span>
                  <div className="lp-readonly-value">
                    <strong>
                      {resolvedModel || metadata?.defaultModel || "gpt-image-1"}
                    </strong>
                    {isExperimentManaged ? (
                      <span className="lp-badge">Controlado pelo experimento</span>
                    ) : (
                      <small>
                        Defina o modelo diretamente na tela do experimento.
                        Fluxos sem experimento usam o padrão informado acima.
                      </small>
                    )}
                  </div>
                </div>

                <label className="lp-field">
                  <span>Tamanho do lote</span>
                  <input
                    type="number"
                    min={1}
                    max={20}
                    value={formState.batchSize ?? ""}
                    onChange={(event) => {
                      const value = Number(event.target.value);
                      setFormState((current) => ({
                        ...current,
                        batchSize: Number.isNaN(value) ? null : value,
                      }));
                    }}
                    placeholder={`${metadata?.defaultBatchSize ?? 6}`}
                    disabled={isExperimentManaged}
                  />
                  {isExperimentManaged ? (
                    <small>O experimento controla o número de variações.</small>
                  ) : (
                    <small>
                      Mantemos o modo batch para reduzir custo. Valores entre 1 e
                      20 imagens.
                    </small>
                  )}
                </label>

                <label className="lp-field">
                  <span>Template do prompt</span>
                  <textarea
                    rows={16}
                    value={formState.template}
                    onChange={(event) =>
                      setFormState((current) => ({
                        ...current,
                        template: event.target.value,
                      }))
                    }
                  />
                  <small>
                    Use os placeholders abaixo para inserir respostas do fluxo.
                    Se o campo ficar vazio o sistema volta para o texto padrão.
                  </small>
                </label>

                <div className="lp-editor-actions">
                  <button
                    type="button"
                    className="secondary"
                    onClick={handleRestoreTemplate}
                    disabled={!metadata}
                  >
                    Restaurar template padrão
                  </button>
                  <button
                    type="button"
                    className="secondary"
                    onClick={handleRestoreDefaults}
                    disabled={!metadata}
                  >
                    Restaurar lote padrão
                  </button>
                  <button
                    type="submit"
                    className="primary"
                    disabled={updatePrompt.isPending}
                  >
                    {updatePrompt.isPending ? (
                      <>
                        <span
                          className="spinner-border spinner-border-sm me-2"
                          role="status"
                          aria-hidden="true"
                        />
                        Salvando...
                      </>
                    ) : (
                      "Salvar alterações"
                    )}
                  </button>
                </div>
              </form>
            )}
          </div>

          <div className="lp-card">
            <div className="lp-card-header">
              <h3>Placeholders disponíveis</h3>
            </div>
            {isLoadingMetadata ? (
              <p className="lp-muted">Carregando metadados...</p>
            ) : !metadata ? (
              <p className="lp-error">
                Não foi possível carregar a lista de placeholders.
              </p>
            ) : (
              <ul className="lp-placeholder-list">
                {metadata.placeholders.map((item) => (
                  <li key={item.token}>
                    <code>{item.token}</code>
                    <p>{item.description}</p>
                    {item.example ? (
                      <small>Ex.: {item.example}</small>
                    ) : null}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}
