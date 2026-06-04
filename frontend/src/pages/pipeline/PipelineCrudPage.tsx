import { useMemo, useState } from "react";
import PageTitle from "../../components/PageTitle";
import { useOpenAiModels } from "../../api/openAiModel/useOpenAiModels";
import { usePipelines } from "../../api/pipeline/usePipelines";
import type {
  Pipeline,
  PipelinePayload,
  PipelineStage,
  PipelineStagePayload,
} from "../../api/pipeline/types";
import {
  useCreatePipeline,
  useCreatePipelineStage,
  useDeletePipeline,
  useDeletePipelineStage,
  useUpdatePipeline,
  useUpdatePipelineStage,
} from "../../api/pipeline/usePipelineMutations";

const emptyPipeline: PipelinePayload = {
  name: "",
  code: "",
  module: "EXPERIMENT",
  description: "",
  active: true,
};

const emptyStage: PipelineStagePayload = {
  position: 1,
  name: "",
  code: "",
  description: "",
  required: true,
  active: true,
  openAiModelId: null,
};

function pipelineToPayload(pipeline: Pipeline): PipelinePayload {
  return {
    name: pipeline.name,
    code: pipeline.code,
    module: pipeline.module,
    description: pipeline.description ?? "",
    active: pipeline.active,
  };
}

function stageToPayload(stage: PipelineStage): PipelineStagePayload {
  return {
    position: stage.position,
    name: stage.name,
    code: stage.code,
    description: stage.description ?? "",
    required: stage.required,
    active: stage.active,
    openAiModelId: stage.openAiModelId ?? null,
  };
}

export default function PipelineCrudPage() {
  const { data, isLoading, isError } = usePipelines();
  const { data: openAiModels, isLoading: isLoadingOpenAiModels } =
    useOpenAiModels();
  const createPipeline = useCreatePipeline();
  const updatePipeline = useUpdatePipeline();
  const deletePipeline = useDeletePipeline();
  const createStage = useCreatePipelineStage();
  const updateStage = useUpdatePipelineStage();
  const deleteStage = useDeletePipelineStage();

  const pipelines = useMemo(() => data ?? [], [data]);
  const modelOptions = useMemo(() => openAiModels ?? [], [openAiModels]);
  const [pipelineForm, setPipelineForm] =
    useState<PipelinePayload>(emptyPipeline);
  const [editingPipelineId, setEditingPipelineId] = useState<number | null>(
    null,
  );
  const [stageForms, setStageForms] = useState<
    Record<number, PipelineStagePayload>
  >({});
  const [editingStageId, setEditingStageId] = useState<number | null>(null);

  const isSavingPipeline = createPipeline.isPending || updatePipeline.isPending;
  const isSavingStage = createStage.isPending || updateStage.isPending;

  const submitPipeline = () => {
    const payload = {
      ...pipelineForm,
      code: pipelineForm.code.trim(),
      name: pipelineForm.name.trim(),
    };
    if (editingPipelineId) {
      updatePipeline.mutate(
        { id: editingPipelineId, payload },
        { onSuccess: () => resetPipelineForm() },
      );
      return;
    }

    createPipeline.mutate(payload, { onSuccess: () => resetPipelineForm() });
  };

  const resetPipelineForm = () => {
    setPipelineForm(emptyPipeline);
    setEditingPipelineId(null);
  };

  const submitStage = (pipelineId: number) => {
    const payload = stageForms[pipelineId] ?? emptyStage;
    const normalizedPayload = {
      ...payload,
      code: payload.code.trim(),
      name: payload.name.trim(),
    };
    if (editingStageId) {
      updateStage.mutate(
        { pipelineId, stageId: editingStageId, payload: normalizedPayload },
        { onSuccess: () => resetStageForm(pipelineId) },
      );
      return;
    }

    createStage.mutate(
      { pipelineId, payload: normalizedPayload },
      { onSuccess: () => resetStageForm(pipelineId) },
    );
  };

  const resetStageForm = (pipelineId: number) => {
    setStageForms((current) => ({ ...current, [pipelineId]: emptyStage }));
    setEditingStageId(null);
  };

  if (isLoading) return <p>Carregando pipelines...</p>;
  if (isError)
    return (
      <p className="text-danger">Não foi possível carregar os pipelines.</p>
    );

  return (
    <div>
      <PageTitle>Pipelines e etapas</PageTitle>
      <p className="text-body-secondary">
        Configure os pipelines operacionais que transformam oportunidades em
        produtos digitais vendáveis. Exemplo: Pipeline de Experimento com etapas
        como Campaign Angle, Ad Copy e Landing Wireframe.
      </p>

      <section className="card mb-4">
        <div className="card-body">
          <h2 className="h5">
            {editingPipelineId ? "Editar pipeline" : "Novo pipeline"}
          </h2>
          <div className="row g-3">
            <div className="col-md-4">
              <label className="form-label">Nome *</label>
              <input
                className="form-control"
                value={pipelineForm.name}
                onChange={(event) =>
                  setPipelineForm((current) => ({
                    ...current,
                    name: event.target.value,
                  }))
                }
                placeholder="Pipeline de Experimento"
              />
            </div>
            <div className="col-md-3">
              <label className="form-label">Código *</label>
              <input
                className="form-control"
                value={pipelineForm.code}
                onChange={(event) =>
                  setPipelineForm((current) => ({
                    ...current,
                    code: event.target.value,
                  }))
                }
                placeholder="experiment-pipeline"
              />
            </div>
            <div className="col-md-3">
              <label className="form-label">Módulo *</label>
              <input
                className="form-control"
                value={pipelineForm.module}
                onChange={(event) =>
                  setPipelineForm((current) => ({
                    ...current,
                    module: event.target.value,
                  }))
                }
                placeholder="EXPERIMENT"
              />
            </div>
            <div className="col-md-2 d-flex align-items-end">
              <div className="form-check form-switch mb-2">
                <input
                  className="form-check-input"
                  type="checkbox"
                  checked={pipelineForm.active}
                  onChange={(event) =>
                    setPipelineForm((current) => ({
                      ...current,
                      active: event.target.checked,
                    }))
                  }
                  id="pipeline-active"
                />
                <label className="form-check-label" htmlFor="pipeline-active">
                  Ativo
                </label>
              </div>
            </div>
            <div className="col-12">
              <label className="form-label">Descrição</label>
              <textarea
                className="form-control"
                rows={2}
                value={pipelineForm.description ?? ""}
                onChange={(event) =>
                  setPipelineForm((current) => ({
                    ...current,
                    description: event.target.value,
                  }))
                }
                placeholder="Objetivo comercial e resultado esperado do pipeline."
              />
            </div>
          </div>
          <div className="d-flex gap-2 mt-3">
            <button
              className="btn btn-primary"
              onClick={submitPipeline}
              disabled={
                isSavingPipeline ||
                !pipelineForm.name.trim() ||
                !pipelineForm.code.trim() ||
                !pipelineForm.module.trim()
              }
            >
              {isSavingPipeline ? (
                <span
                  className="spinner-border spinner-border-sm me-2"
                  aria-hidden="true"
                />
              ) : null}
              {editingPipelineId ? "Salvar pipeline" : "Criar pipeline"}
            </button>
            {editingPipelineId ? (
              <button
                className="btn btn-outline-secondary"
                onClick={resetPipelineForm}
                disabled={isSavingPipeline}
              >
                Cancelar edição
              </button>
            ) : null}
          </div>
        </div>
      </section>

      <div className="d-grid gap-4">
        {pipelines.map((pipeline) => {
          const stageForm = stageForms[pipeline.id] ?? emptyStage;
          return (
            <section className="card" key={pipeline.id}>
              <div className="card-header d-flex flex-wrap justify-content-between align-items-start gap-3">
                <div>
                  <div className="d-flex align-items-center gap-2">
                    <h2 className="h5 mb-0">{pipeline.name}</h2>
                    <span
                      className={`badge ${pipeline.active ? "text-bg-success" : "text-bg-secondary"}`}
                    >
                      {pipeline.active ? "Ativo" : "Inativo"}
                    </span>
                  </div>
                  <div className="small text-body-secondary">
                    {pipeline.module} · {pipeline.code} ·{" "}
                    {pipeline.stages.length} etapas
                  </div>
                  {pipeline.description ? (
                    <p className="mb-0 mt-2">{pipeline.description}</p>
                  ) : null}
                </div>
                <div className="d-flex gap-2">
                  <button
                    className="btn btn-sm btn-outline-primary"
                    onClick={() => {
                      setEditingPipelineId(pipeline.id);
                      setPipelineForm(pipelineToPayload(pipeline));
                    }}
                  >
                    Editar
                  </button>
                  <button
                    className="btn btn-sm btn-outline-danger"
                    disabled={deletePipeline.isPending}
                    onClick={() => {
                      if (
                        confirm(
                          "Deseja remover este pipeline e todas as etapas?",
                        )
                      ) {
                        deletePipeline.mutate(pipeline.id);
                      }
                    }}
                  >
                    {deletePipeline.isPending ? "Excluindo..." : "Excluir"}
                  </button>
                </div>
              </div>
              <div className="card-body">
                <div className="table-responsive mb-4">
                  <table className="table align-middle">
                    <thead>
                      <tr>
                        <th style={{ width: 90 }}>Etapa</th>
                        <th>Nome</th>
                        <th>Código</th>
                        <th>Descrição</th>
                        <th>Obrigatória</th>
                        <th>Modelo OpenAI</th>
                        <th>Status</th>
                        <th>Ações</th>
                      </tr>
                    </thead>
                    <tbody>
                      {pipeline.stages.map((stage) => (
                        <tr key={stage.id}>
                          <td>Etapa {stage.position}</td>
                          <td>{stage.name}</td>
                          <td>
                            <code>{stage.code}</code>
                          </td>
                          <td>{stage.description || "-"}</td>
                          <td>{stage.required ? "Sim" : "Não"}</td>
                          <td>
                            {stage.openAiModelCode ? (
                              <span title={stage.openAiModelName ?? undefined}>
                                {stage.openAiModelName ?? stage.openAiModelCode}{" "}
                                (<code>{stage.openAiModelCode}</code>)
                              </span>
                            ) : (
                              "—"
                            )}
                          </td>
                          <td>{stage.active ? "Ativa" : "Inativa"}</td>
                          <td className="d-flex gap-2">
                            <button
                              className="btn btn-sm btn-outline-primary"
                              onClick={() => {
                                setEditingStageId(stage.id);
                                setStageForms((current) => ({
                                  ...current,
                                  [pipeline.id]: stageToPayload(stage),
                                }));
                              }}
                            >
                              Editar
                            </button>
                            <button
                              className="btn btn-sm btn-outline-danger"
                              disabled={deleteStage.isPending}
                              onClick={() => {
                                if (confirm("Deseja remover esta etapa?")) {
                                  deleteStage.mutate({
                                    pipelineId: pipeline.id,
                                    stageId: stage.id,
                                  });
                                }
                              }}
                            >
                              {deleteStage.isPending
                                ? "Excluindo..."
                                : "Excluir"}
                            </button>
                          </td>
                        </tr>
                      ))}
                      {pipeline.stages.length === 0 ? (
                        <tr>
                          <td
                            colSpan={8}
                            className="text-center text-body-secondary"
                          >
                            Nenhuma etapa cadastrada para este pipeline.
                          </td>
                        </tr>
                      ) : null}
                    </tbody>
                  </table>
                </div>

                <div className="border rounded p-3 bg-light">
                  <h3 className="h6">
                    {editingStageId ? "Editar etapa" : "Nova etapa"}
                  </h3>
                  <div className="row g-3">
                    <div className="col-md-1">
                      <label className="form-label">Ordem *</label>
                      <input
                        type="number"
                        min={1}
                        className="form-control"
                        value={stageForm.position}
                        onChange={(event) =>
                          setStageForms((current) => ({
                            ...current,
                            [pipeline.id]: {
                              ...stageForm,
                              position: Number(event.target.value),
                            },
                          }))
                        }
                      />
                    </div>
                    <div className="col-md-3">
                      <label className="form-label">Nome *</label>
                      <input
                        className="form-control"
                        value={stageForm.name}
                        onChange={(event) =>
                          setStageForms((current) => ({
                            ...current,
                            [pipeline.id]: {
                              ...stageForm,
                              name: event.target.value,
                            },
                          }))
                        }
                        placeholder="Campaign Angle"
                      />
                    </div>
                    <div className="col-md-3">
                      <label className="form-label">Código *</label>
                      <input
                        className="form-control"
                        value={stageForm.code}
                        onChange={(event) =>
                          setStageForms((current) => ({
                            ...current,
                            [pipeline.id]: {
                              ...stageForm,
                              code: event.target.value,
                            },
                          }))
                        }
                        placeholder="campaign-angle"
                      />
                    </div>
                    <div className="col-md-4">
                      <label className="form-label">
                        Modelo OpenAI da etapa
                      </label>
                      <select
                        className="form-select"
                        value={stageForm.openAiModelId ?? ""}
                        disabled={isLoadingOpenAiModels}
                        onChange={(event) =>
                          setStageForms((current) => ({
                            ...current,
                            [pipeline.id]: {
                              ...stageForm,
                              openAiModelId: event.target.value
                                ? Number(event.target.value)
                                : null,
                            },
                          }))
                        }
                      >
                        <option value="">
                          {isLoadingOpenAiModels
                            ? "Carregando modelos..."
                            : "Sem modelo fixo"}
                        </option>
                        {modelOptions.map((model) => (
                          <option key={model.id} value={model.id}>
                            {model.name} ({model.code})
                          </option>
                        ))}
                      </select>
                      <div className="form-text">
                        Use o modelo recomendado para manter foco em venda por
                        etapa.
                      </div>
                    </div>
                    <div className="col-md-2 d-flex align-items-end">
                      <div className="form-check form-switch mb-2">
                        <input
                          className="form-check-input"
                          type="checkbox"
                          checked={stageForm.required}
                          onChange={(event) =>
                            setStageForms((current) => ({
                              ...current,
                              [pipeline.id]: {
                                ...stageForm,
                                required: event.target.checked,
                              },
                            }))
                          }
                          id={`stage-required-${pipeline.id}`}
                        />
                        <label
                          className="form-check-label"
                          htmlFor={`stage-required-${pipeline.id}`}
                        >
                          Obrigatória
                        </label>
                      </div>
                    </div>
                    <div className="col-md-2 d-flex align-items-end">
                      <div className="form-check form-switch mb-2">
                        <input
                          className="form-check-input"
                          type="checkbox"
                          checked={stageForm.active}
                          onChange={(event) =>
                            setStageForms((current) => ({
                              ...current,
                              [pipeline.id]: {
                                ...stageForm,
                                active: event.target.checked,
                              },
                            }))
                          }
                          id={`stage-active-${pipeline.id}`}
                        />
                        <label
                          className="form-check-label"
                          htmlFor={`stage-active-${pipeline.id}`}
                        >
                          Ativa
                        </label>
                      </div>
                    </div>
                    <div className="col-12">
                      <label className="form-label">Descrição</label>
                      <textarea
                        className="form-control"
                        rows={2}
                        value={stageForm.description ?? ""}
                        onChange={(event) =>
                          setStageForms((current) => ({
                            ...current,
                            [pipeline.id]: {
                              ...stageForm,
                              description: event.target.value,
                            },
                          }))
                        }
                        placeholder="Explique o objetivo prático desta etapa."
                      />
                    </div>
                  </div>
                  <div className="d-flex gap-2 mt-3">
                    <button
                      className="btn btn-outline-primary"
                      disabled={
                        isSavingStage ||
                        !stageForm.name.trim() ||
                        !stageForm.code.trim() ||
                        stageForm.position < 1
                      }
                      onClick={() => submitStage(pipeline.id)}
                    >
                      {isSavingStage ? (
                        <span
                          className="spinner-border spinner-border-sm me-2"
                          aria-hidden="true"
                        />
                      ) : null}
                      {editingStageId ? "Salvar etapa" : "Adicionar etapa"}
                    </button>
                    {editingStageId ? (
                      <button
                        className="btn btn-outline-secondary"
                        disabled={isSavingStage}
                        onClick={() => resetStageForm(pipeline.id)}
                      >
                        Cancelar edição
                      </button>
                    ) : null}
                  </div>
                </div>
              </div>
            </section>
          );
        })}
        {pipelines.length === 0 ? (
          <div className="alert alert-info">
            Nenhum pipeline cadastrado ainda. Crie o primeiro pipeline acima.
          </div>
        ) : null}
      </div>
    </div>
  );
}
