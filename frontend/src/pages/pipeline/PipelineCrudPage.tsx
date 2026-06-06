import { useMemo, useState } from "react";
import PageTitle from "../../components/PageTitle";
import { useOpenAiModels } from "../../api/openAiModel/useOpenAiModels";
import { usePipelines } from "../../api/pipeline/usePipelines";
import { usePipelineDiagnostics } from "../../api/pipeline/usePipelineDiagnostics";
import { usePipelineMetadata } from "../../api/pipeline/usePipelineMetadata";
import type {
  Pipeline,
  PipelinePayload,
  PipelineStage,
  PipelineDiagnostics,
  PipelineStagePayload,
} from "../../api/pipeline/types";
import {
  useDeletePipeline,
  useDeletePipelineStage,
  useRebuildOfficialPipelineStages,
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
  executionModule: "",
  rootPackage: "",
  required: true,
  active: true,
  openAiModelId: null,
};

function normalizeCode(code?: string | null) {
  return (code ?? "").trim().toLowerCase().replace(/_/g, "-");
}

function statusBadgeClass(status?: PipelineDiagnostics["status"]) {
  if (status === "OK") return "text-bg-success";
  if (status === "BLOQUEADO") return "text-bg-danger";
  return "text-bg-warning";
}

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
    executionModule: stage.executionModule ?? "",
    rootPackage: stage.rootPackage ?? "",
    required: stage.required,
    active: stage.active,
    openAiModelId: stage.openAiModelId ?? null,
  };
}

export default function PipelineCrudPage() {
  const { data, isLoading, isError } = usePipelines();
  const { data: metadata, isLoading: isLoadingMetadata } =
    usePipelineMetadata();
  const { data: openAiModels, isLoading: isLoadingOpenAiModels } =
    useOpenAiModels();
  const updatePipeline = useUpdatePipeline();
  const deletePipeline = useDeletePipeline();
  const updateStage = useUpdatePipelineStage();
  const deleteStage = useDeletePipelineStage();
  const rebuildOfficialStages = useRebuildOfficialPipelineStages();

  const pipelines = useMemo(() => data ?? [], [data]);
  const diagnosticsQueries = usePipelineDiagnostics(pipelines);
  const modelOptions = useMemo(() => openAiModels ?? [], [openAiModels]);
  const officialPipelines = useMemo(
    () => metadata?.officialPipelines ?? [],
    [metadata],
  );
  const validModules = useMemo(
    () => metadata?.validModules ?? ["EXPERIMENT"],
    [metadata],
  );
  const diagnosticsByPipelineId = useMemo(() => {
    const entries = diagnosticsQueries
      .map((query) => query.data)
      .filter((diagnostic): diagnostic is PipelineDiagnostics =>
        Boolean(diagnostic),
      )
      .map((diagnostic) => [diagnostic.pipelineId, diagnostic] as const);
    return new Map(entries);
  }, [diagnosticsQueries]);
  const [selectedPipelineId, setSelectedPipelineId] = useState<number | null>(
    null,
  );
  const [pipelineForm, setPipelineForm] =
    useState<PipelinePayload>(emptyPipeline);
  const [editingPipelineId, setEditingPipelineId] = useState<number | null>(
    null,
  );
  const [stageForms, setStageForms] = useState<
    Record<number, PipelineStagePayload>
  >({});
  const [editingStageId, setEditingStageId] = useState<number | null>(null);

  const selectedPipeline = pipelines.find(
    (pipeline) => pipeline.id === selectedPipelineId,
  );
  const isSavingPipeline = updatePipeline.isPending;
  const isSavingStage = updateStage.isPending;
  const editingOfficialPipeline = officialPipelines.find(
    (official) =>
      normalizeCode(official.code) === normalizeCode(pipelineForm.code),
  );

  const submitPipeline = () => {
    if (!editingPipelineId) return;

    const payload = {
      ...pipelineForm,
      code: pipelineForm.code.trim(),
      name: pipelineForm.name.trim(),
    };
    updatePipeline.mutate(
      { id: editingPipelineId, payload },
      { onSuccess: () => resetPipelineForm() },
    );
  };

  const resetPipelineForm = () => {
    setPipelineForm(emptyPipeline);
    setEditingPipelineId(null);
  };

  const startPipelineEdit = (pipeline: Pipeline) => {
    setSelectedPipelineId(null);
    setEditingPipelineId(pipeline.id);
    setPipelineForm(pipelineToPayload(pipeline));
  };

  const openPipelineStages = (pipelineId: number) => {
    resetPipelineForm();
    setEditingStageId(null);
    setSelectedPipelineId(pipelineId);
  };

  const returnToPipelineList = () => {
    setSelectedPipelineId(null);
    setEditingStageId(null);
  };

  const submitStage = (pipelineId: number) => {
    if (!editingStageId) return;

    const payload = stageForms[pipelineId] ?? emptyStage;
    const normalizedPayload = {
      ...payload,
      code: payload.code.trim(),
      name: payload.name.trim(),
      executionModule: payload.executionModule?.trim() || null,
      rootPackage: payload.rootPackage?.trim() || null,
    };
    updateStage.mutate(
      { pipelineId, stageId: editingStageId, payload: normalizedPayload },
      { onSuccess: () => resetStageForm(pipelineId) },
    );
  };

  const resetStageForm = (pipelineId: number) => {
    setStageForms((current) => ({ ...current, [pipelineId]: emptyStage }));
    setEditingStageId(null);
  };

  const confirmOfficialStageRebuild = (pipeline: Pipeline) => {
    if (
      confirm(
        "Deseja excluir as etapas atuais e recriar as etapas oficiais deste pipeline? Configurações compatíveis, como modelo OpenAI e descrição, serão reaproveitadas quando possível.",
      )
    ) {
      rebuildOfficialStages.mutate(pipeline.id);
    }
  };

  if (isLoading) return <p>Carregando pipelines...</p>;
  if (isError)
    return (
      <p className="text-danger">Não foi possível carregar os pipelines.</p>
    );

  if (selectedPipeline) {
    const pipeline = selectedPipeline;
    const stageForm = stageForms[pipeline.id] ?? emptyStage;
    const diagnostic = diagnosticsByPipelineId.get(pipeline.id);
    const officialPipeline = officialPipelines.find(
      (official) =>
        normalizeCode(official.code) === normalizeCode(pipeline.code),
    );
    const stageDefinition = officialPipeline?.stages.find(
      (stage) =>
        normalizeCode(stage.operationalCode) ===
          normalizeCode(stageForm.code) ||
        stage.aliases.some(
          (alias) => normalizeCode(alias) === normalizeCode(stageForm.code),
        ),
    );
    const isEditingOfficialStage = Boolean(
      editingStageId && stageDefinition?.required,
    );
    const isOfficialPipeline = Boolean(officialPipeline);

    return (
      <div>
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-3">
          <div>
            <PageTitle>Etapas do pipeline</PageTitle>
            <p className="text-body-secondary mb-0">
              Tela focada nas etapas de um único pipeline para reduzir poluição
              visual e manter o fluxo operacional claro.
            </p>
          </div>
          <button
            type="button"
            className="btn btn-outline-secondary"
            onClick={returnToPipelineList}
          >
            ← Voltar para lista
          </button>
        </div>

        <section className="card mb-4 border-primary">
          <div className="card-body">
            <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
              <div>
                <div className="small text-uppercase text-body-secondary fw-semibold">
                  Pipeline selecionado
                </div>
                <h2 className="h4 mb-1">{pipeline.name}</h2>
                <div className="d-flex flex-wrap gap-2">
                  <span className="badge text-bg-light border">
                    {pipeline.module}
                  </span>
                  <span className="badge text-bg-light border">
                    {pipeline.code}
                  </span>
                  <span
                    className={`badge ${pipeline.active ? "text-bg-success" : "text-bg-secondary"}`}
                  >
                    {pipeline.active ? "Ativo" : "Inativo"}
                  </span>
                  <span className="badge text-bg-light border">
                    {pipeline.stages.length} etapa
                    {pipeline.stages.length === 1 ? "" : "s"}
                  </span>
                  {diagnostic ? (
                    <span
                      className={`badge ${statusBadgeClass(diagnostic.status)}`}
                    >
                      Contrato {diagnostic.status}
                    </span>
                  ) : null}
                </div>
              </div>
              <button
                type="button"
                className="btn btn-outline-primary btn-sm"
                onClick={() => startPipelineEdit(pipeline)}
              >
                Editar dados do pipeline
              </button>
            </div>
            {pipeline.description ? (
              <p className="text-body-secondary mt-3 mb-0">
                {pipeline.description}
              </p>
            ) : null}
          </div>
        </section>

        {diagnostic ? (
          <div
            className={`alert ${diagnostic.status === "BLOQUEADO" ? "alert-danger" : diagnostic.status === "OK" ? "alert-success" : "alert-warning"}`}
          >
            <div className="fw-semibold">
              Status do contrato operacional: {diagnostic.status}
            </div>
            <div>
              Etapas esperadas no código: {diagnostic.expectedStages} · etapas
              configuradas no banco: {diagnostic.configuredStages}
            </div>
            {diagnostic.issues.length > 0 ? (
              <ul className="mb-0 mt-2">
                {diagnostic.issues.map((issue, index) => (
                  <li key={`${issue.stageCode ?? "pipeline"}-${index}`}>
                    <strong>{issue.severity}</strong> ·{" "}
                    {issue.stageCode ? (
                      <code>{issue.stageCode}</code>
                    ) : (
                      "pipeline"
                    )}
                    {issue.canonicalCode ? (
                      <>
                        {" "}
                        → <code>{issue.canonicalCode}</code>
                      </>
                    ) : null}
                    : {issue.message} Causa-raiz: {issue.rootCause} Ação:{" "}
                    {issue.recommendedAction}
                  </li>
                ))}
              </ul>
            ) : null}
          </div>
        ) : null}

        <section className="mb-4">
          <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
            <h3 className="h5 mb-0">Etapas configuradas</h3>
            {isOfficialPipeline ? (
              <button
                type="button"
                className="btn btn-sm btn-outline-warning"
                disabled={
                  rebuildOfficialStages.isPending ||
                  !diagnostic ||
                  diagnostic.status === "OK"
                }
                onClick={() => confirmOfficialStageRebuild(pipeline)}
              >
                {rebuildOfficialStages.isPending &&
                rebuildOfficialStages.variables === pipeline.id ? (
                  <span
                    className="spinner-border spinner-border-sm me-2"
                    aria-hidden="true"
                  />
                ) : null}
                Ajustar etapas oficiais
              </button>
            ) : null}
          </div>

          {pipeline.stages.length === 0 ? (
            <div className="alert alert-info">
              Nenhuma etapa cadastrada para este pipeline.
            </div>
          ) : (
            <div className="row g-3">
              {pipeline.stages.map((stage) => {
                const rowDefinition = officialPipeline?.stages.find(
                  (definition) =>
                    normalizeCode(definition.operationalCode) ===
                      normalizeCode(stage.code) ||
                    definition.aliases.some(
                      (alias) =>
                        normalizeCode(alias) === normalizeCode(stage.code),
                    ),
                );
                const protectionLabel = rowDefinition?.required
                  ? "Estrutural"
                  : isOfficialPipeline
                    ? "Não mapeada"
                    : "Editável";

                return (
                  <div className="col-12 col-xl-6" key={stage.id}>
                    <article className="card h-100 border shadow-sm">
                      <div className="card-body d-flex flex-column gap-3">
                        <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
                          <div>
                            <div className="small fw-semibold text-primary text-uppercase">
                              Etapa {stage.position}
                            </div>
                            <h4 className="h5 mb-1">{stage.name}</h4>
                            <div className="d-flex flex-wrap gap-2">
                              <span className="badge text-bg-light border">
                                {stage.code}
                              </span>
                              <span
                                className={`badge ${stage.active ? "text-bg-success" : "text-bg-secondary"}`}
                              >
                                {stage.active ? "Ativa" : "Inativa"}
                              </span>
                              {stage.required ? (
                                <span className="badge text-bg-primary">
                                  Obrigatória
                                </span>
                              ) : null}
                              <span className="badge text-bg-light border">
                                {protectionLabel}
                              </span>
                            </div>
                          </div>
                          <div className="d-flex gap-2">
                            <button
                              type="button"
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
                              type="button"
                              className="btn btn-sm btn-outline-danger"
                              disabled={
                                deleteStage.isPending ||
                                Boolean(rowDefinition?.required)
                              }
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
                          </div>
                        </div>
                        {stage.description ? (
                          <p className="text-body-secondary mb-0">
                            {stage.description}
                          </p>
                        ) : null}
                        <div className="small text-body-secondary">
                          <div>
                            <strong>Módulo executor:</strong>{" "}
                            {stage.executionModule || "Não informado"}
                          </div>
                          <div>
                            <strong>Pacote raiz:</strong>{" "}
                            {stage.rootPackage || "Não informado"}
                          </div>
                          <div>
                            <strong>Modelo OpenAI:</strong>{" "}
                            {stage.openAiModelName
                              ? `${stage.openAiModelName} (${stage.openAiModelCode})`
                              : "Sem modelo fixo"}
                          </div>
                        </div>
                      </div>
                    </article>
                  </div>
                );
              })}
            </div>
          )}
        </section>

        {editingStageId ? (
          <section className="card">
            <div className="card-body">
              <h3 className="h5">Editar etapa</h3>
              {stageDefinition?.required ? (
                <div className="alert alert-warning">
                  Etapa oficial estrutural: código, posição, módulo executor,
                  pacote raiz e obrigatoriedade ficam protegidos para evitar
                  divergência do contrato operacional.
                </div>
              ) : null}
              {isOfficialPipeline && officialPipeline ? (
                <div className="mb-3">
                  <label className="form-label">Modelo de etapa oficial</label>
                  <select
                    className="form-select"
                    value={stageDefinition?.operationalCode ?? ""}
                    onChange={(event) => {
                      const selected = officialPipeline.stages.find(
                        (stage) => stage.operationalCode === event.target.value,
                      );
                      if (!selected) return;
                      setStageForms((current) => ({
                        ...current,
                        [pipeline.id]: {
                          ...stageForm,
                          position: selected.position,
                          name: selected.name,
                          code: selected.operationalCode,
                          executionModule: selected.executionModule ?? "",
                          rootPackage: selected.rootPackage ?? "",
                          required: selected.required,
                        },
                      }));
                    }}
                  >
                    <option value="">Etapa personalizada</option>
                    {officialPipeline.stages.map((stage) => (
                      <option
                        key={stage.operationalCode}
                        value={stage.operationalCode}
                      >
                        {stage.position}. {stage.name} ({stage.operationalCode})
                      </option>
                    ))}
                  </select>
                </div>
              ) : null}
              <div className="row g-3">
                <div className="col-md-2">
                  <label className="form-label">Posição *</label>
                  <input
                    className="form-control"
                    type="number"
                    min={1}
                    value={stageForm.position}
                    disabled={Boolean(stageDefinition?.required)}
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
                <div className="col-md-4">
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
                    disabled={Boolean(stageDefinition?.required)}
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
                <div className="col-md-3">
                  <label className="form-label">Módulo executor</label>
                  <input
                    className="form-control"
                    disabled={Boolean(stageDefinition?.required)}
                    value={stageForm.executionModule ?? ""}
                    onChange={(event) =>
                      setStageForms((current) => ({
                        ...current,
                        [pipeline.id]: {
                          ...stageForm,
                          executionModule: event.target.value,
                        },
                      }))
                    }
                    placeholder="ai-worker"
                  />
                </div>
                <div className="col-md-4">
                  <label className="form-label">Pacote raiz</label>
                  <input
                    className="form-control"
                    disabled={Boolean(stageDefinition?.required)}
                    value={stageForm.rootPackage ?? ""}
                    onChange={(event) =>
                      setStageForms((current) => ({
                        ...current,
                        [pipeline.id]: {
                          ...stageForm,
                          rootPackage: event.target.value,
                        },
                      }))
                    }
                    placeholder="com.marketinghub.worker.openai.core"
                  />
                </div>
                <div className="col-md-4">
                  <label className="form-label">Modelo OpenAI</label>
                  <select
                    className="form-select"
                    value={stageForm.openAiModelId ?? ""}
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
                        {model.acceptsImageInput ? " · aceita imagem" : ""}
                      </option>
                    ))}
                  </select>
                  <div className="form-text">
                    Use modelos com imagem para etapas visuais, como Quality
                    Review, e mantenha foco em venda por etapa.
                  </div>
                </div>
                <div className="col-md-2 d-flex align-items-end">
                  <div className="form-check form-switch mb-2">
                    <input
                      className="form-check-input"
                      type="checkbox"
                      checked={stageForm.required}
                      disabled={
                        isEditingOfficialStage ||
                        Boolean(stageDefinition?.required)
                      }
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
                      disabled={Boolean(stageDefinition?.required)}
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
                  Salvar etapa
                </button>
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  disabled={isSavingStage}
                  onClick={() => resetStageForm(pipeline.id)}
                >
                  Cancelar edição
                </button>
              </div>
            </div>
          </section>
        ) : null}
      </div>
    );
  }

  return (
    <div>
      <PageTitle>Pipelines</PageTitle>
      <p className="text-body-secondary">
        Lista limpa dos pipelines operacionais. Clique em{" "}
        <strong>Ver etapas</strong> para abrir a tela focada somente no fluxo
        daquele pipeline. A criação de pipelines e etapas é feita somente pelo
        backend conforme contrato canônico.
      </p>

      {editingPipelineId ? (
        <section className="card mb-4">
          <div className="card-body">
            <h2 className="h5">Editar pipeline</h2>
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
                  disabled={Boolean(editingOfficialPipeline)}
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
                <select
                  className="form-select"
                  value={pipelineForm.module}
                  disabled={
                    Boolean(editingOfficialPipeline) || isLoadingMetadata
                  }
                  onChange={(event) =>
                    setPipelineForm((current) => ({
                      ...current,
                      module: event.target.value,
                    }))
                  }
                >
                  {validModules.map((module) => (
                    <option key={module} value={module}>
                      {module}
                    </option>
                  ))}
                </select>
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
              {editingOfficialPipeline ? (
                <div className="col-12">
                  <div className="alert alert-info mb-0">
                    Pipeline oficial protegido: código e módulo são estruturais
                    e só o backend pode redefinir. A tela edita apenas
                    configuração operacional segura.
                  </div>
                </div>
              ) : null}
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
                Salvar pipeline
              </button>
              <button
                type="button"
                className="btn btn-outline-secondary"
                onClick={resetPipelineForm}
                disabled={isSavingPipeline}
              >
                Cancelar edição
              </button>
            </div>
          </div>
        </section>
      ) : null}

      {pipelines.length === 0 ? (
        <div className="alert alert-info">
          Nenhum pipeline cadastrado pelo backend até o momento.
        </div>
      ) : (
        <section className="card">
          <div className="card-header bg-white d-flex flex-wrap justify-content-between align-items-center gap-2">
            <h2 className="h5 mb-0">Lista de pipelines</h2>
            <span className="badge text-bg-light border">
              {pipelines.length} pipeline{pipelines.length === 1 ? "" : "s"}
            </span>
          </div>
          <div className="list-group list-group-flush">
            {pipelines.map((pipeline) => {
              const diagnostic = diagnosticsByPipelineId.get(pipeline.id);
              const isOfficialPipeline = officialPipelines.some(
                (official) =>
                  normalizeCode(official.code) === normalizeCode(pipeline.code),
              );

              return (
                <article className="list-group-item" key={pipeline.id}>
                  <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
                    <div className="flex-grow-1">
                      <div className="d-flex flex-wrap align-items-center gap-2 mb-1">
                        <h3 className="h5 mb-0">{pipeline.name}</h3>
                        {isOfficialPipeline ? (
                          <span className="badge text-bg-primary">Oficial</span>
                        ) : null}
                        <span
                          className={`badge ${pipeline.active ? "text-bg-success" : "text-bg-secondary"}`}
                        >
                          {pipeline.active ? "Ativo" : "Inativo"}
                        </span>
                        {diagnostic ? (
                          <span
                            className={`badge ${statusBadgeClass(diagnostic.status)}`}
                          >
                            Contrato {diagnostic.status}
                          </span>
                        ) : null}
                      </div>
                      <div className="d-flex flex-wrap gap-2 small text-body-secondary">
                        <span>{pipeline.module}</span>
                        <span>•</span>
                        <code>{pipeline.code}</code>
                        <span>•</span>
                        <span>
                          {pipeline.stages.length} etapa
                          {pipeline.stages.length === 1 ? "" : "s"}
                        </span>
                      </div>
                      {pipeline.description ? (
                        <p className="text-body-secondary mb-0 mt-2">
                          {pipeline.description}
                        </p>
                      ) : null}
                    </div>
                    <div className="d-flex flex-wrap gap-2">
                      <button
                        type="button"
                        className="btn btn-primary btn-sm"
                        onClick={() => openPipelineStages(pipeline.id)}
                      >
                        Ver etapas
                      </button>
                      <button
                        className="btn btn-sm btn-outline-primary"
                        onClick={() => startPipelineEdit(pipeline)}
                      >
                        Editar
                      </button>
                      <button
                        className="btn btn-sm btn-outline-danger"
                        disabled={
                          deletePipeline.isPending || isOfficialPipeline
                        }
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
                </article>
              );
            })}
          </div>
        </section>
      )}
    </div>
  );
}
