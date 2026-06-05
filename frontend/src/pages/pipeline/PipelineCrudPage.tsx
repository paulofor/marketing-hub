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
  useCreatePipeline,
  useCreatePipelineStage,
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
  const createPipeline = useCreatePipeline();
  const updatePipeline = useUpdatePipeline();
  const deletePipeline = useDeletePipeline();
  const createStage = useCreatePipelineStage();
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
  const editingOfficialPipeline = officialPipelines.find(
    (official) =>
      normalizeCode(official.code) === normalizeCode(pipelineForm.code),
  );

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
      executionModule: payload.executionModule?.trim() || null,
      rootPackage: payload.rootPackage?.trim() || null,
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
                disabled={Boolean(editingOfficialPipeline) || isLoadingMetadata}
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
                  Pipeline oficial protegido: código e módulo são estruturais e
                  só o backend pode redefinir. A tela edita apenas configuração
                  operacional segura.
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
          const diagnostic = diagnosticsByPipelineId.get(pipeline.id);
          const officialPipeline = officialPipelines.find(
            (official) =>
              normalizeCode(official.code) === normalizeCode(pipeline.code),
          );
          const stageDefinition = officialPipeline?.stages.find(
            (stage) =>
              normalizeCode(stage.operationalCode) ===
                normalizeCode(stageForm.code) ||
              normalizeCode(stage.canonicalCode) ===
                normalizeCode(stageForm.code) ||
              stage.aliases.some(
                (alias) =>
                  normalizeCode(alias) === normalizeCode(stageForm.code),
              ),
          );
          const isOfficialPipeline = Boolean(officialPipeline);
          const isEditingOfficialStage = Boolean(
            editingStageId && stageDefinition,
          );
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
                    <span
                      className={`badge ${statusBadgeClass(diagnostic?.status)}`}
                    >
                      Contrato: {diagnostic?.status ?? "ATENÇÃO"}
                    </span>
                    {isOfficialPipeline ? (
                      <span className="badge text-bg-primary">Oficial</span>
                    ) : null}
                  </div>
                  <div className="small text-body-secondary">
                    {pipeline.module} · {pipeline.code} ·{" "}
                    {pipeline.stages.length} etapas
                    {diagnostic ? (
                      <>
                        {" "}
                        · esperado no código: {diagnostic.expectedStages} ·
                        configurado no banco: {diagnostic.configuredStages}
                      </>
                    ) : null}
                  </div>
                  {pipeline.description ? (
                    <p className="mb-0 mt-2">{pipeline.description}</p>
                  ) : null}
                </div>
                <div className="d-flex flex-wrap gap-2">
                  {isOfficialPipeline ? (
                    <button
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
                    disabled={deletePipeline.isPending || isOfficialPipeline}
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
                {diagnostic ? (
                  <div
                    className={`alert ${diagnostic.status === "BLOQUEADO" ? "alert-danger" : diagnostic.status === "OK" ? "alert-success" : "alert-warning"}`}
                  >
                    <div className="fw-semibold">
                      Status do contrato operacional: {diagnostic.status}
                    </div>
                    <div>
                      Etapas esperadas no código: {diagnostic.expectedStages} ·
                      etapas configuradas no banco:{" "}
                      {diagnostic.configuredStages}
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
                            : {issue.message} Causa-raiz: {issue.rootCause}{" "}
                            Ação: {issue.recommendedAction}
                          </li>
                        ))}
                      </ul>
                    ) : null}
                  </div>
                ) : null}
                <div className="mb-4">
                  <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
                    <h3 className="h6 mb-0">Etapas configuradas</h3>
                    <span className="badge text-bg-light border">
                      {pipeline.stages.length} etapa
                      {pipeline.stages.length === 1 ? "" : "s"}
                    </span>
                  </div>

                  {pipeline.stages.length === 0 ? (
                    <div className="alert alert-info mb-0">
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
                                normalizeCode(alias) ===
                                normalizeCode(stage.code),
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
                                      <span
                                        className={`badge ${stage.active ? "text-bg-success" : "text-bg-secondary"}`}
                                      >
                                        {stage.active ? "Ativa" : "Inativa"}
                                      </span>
                                      <span
                                        className={`badge ${stage.required ? "text-bg-warning" : "text-bg-light border"}`}
                                      >
                                        {stage.required
                                          ? "Obrigatória"
                                          : "Opcional"}
                                      </span>
                                      <span className="badge text-bg-light border">
                                        {protectionLabel}
                                      </span>
                                    </div>
                                  </div>

                                  <div className="d-flex gap-2">
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
                                      disabled={
                                        deleteStage.isPending ||
                                        Boolean(rowDefinition?.required)
                                      }
                                      onClick={() => {
                                        if (
                                          confirm("Deseja remover esta etapa?")
                                        ) {
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

                                <p className="mb-0 text-body-secondary">
                                  {stage.description ||
                                    "Sem descrição operacional cadastrada."}
                                </p>

                                <div className="row g-2 small">
                                  <div className="col-md-6">
                                    <div className="text-body-secondary">
                                      Código banco
                                    </div>
                                    <code>{stage.code}</code>
                                  </div>
                                  <div className="col-md-6">
                                    <div className="text-body-secondary">
                                      Código canônico
                                    </div>
                                    {rowDefinition ? (
                                      <code>{rowDefinition.canonicalCode}</code>
                                    ) : (
                                      <span>—</span>
                                    )}
                                  </div>
                                  <div className="col-md-6">
                                    <div className="text-body-secondary">
                                      Módulo executor
                                    </div>
                                    {stage.executionModule ? (
                                      <code>{stage.executionModule}</code>
                                    ) : (
                                      <span>Backend</span>
                                    )}
                                  </div>
                                  <div className="col-md-6">
                                    <div className="text-body-secondary">
                                      Modelo OpenAI
                                    </div>
                                    {stage.openAiModelCode ? (
                                      <span
                                        title={
                                          stage.openAiModelName ?? undefined
                                        }
                                      >
                                        {stage.openAiModelName ??
                                          stage.openAiModelCode}{" "}
                                        (<code>{stage.openAiModelCode}</code>)
                                      </span>
                                    ) : (
                                      <span>—</span>
                                    )}
                                  </div>
                                  <div className="col-12">
                                    <div className="text-body-secondary">
                                      Pacote raiz
                                    </div>
                                    {stage.rootPackage ? (
                                      <code className="text-break">
                                        {stage.rootPackage}
                                      </code>
                                    ) : (
                                      <span>—</span>
                                    )}
                                  </div>
                                </div>
                              </div>
                            </article>
                          </div>
                        );
                      })}
                    </div>
                  )}
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
                        disabled={isEditingOfficialStage}
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
                        disabled={isEditingOfficialStage}
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
                      {isOfficialPipeline && officialPipeline ? (
                        <select
                          className="form-select"
                          value={stageForm.code}
                          disabled={isEditingOfficialStage}
                          onChange={(event) => {
                            const selected = officialPipeline.stages.find(
                              (stage) =>
                                stage.operationalCode === event.target.value,
                            );
                            setStageForms((current) => ({
                              ...current,
                              [pipeline.id]: {
                                ...stageForm,
                                code: event.target.value,
                                name: selected?.name ?? stageForm.name,
                                position:
                                  selected?.position ?? stageForm.position,
                                required:
                                  selected?.required ?? stageForm.required,
                                executionModule:
                                  selected?.executionModule ??
                                  stageForm.executionModule,
                                rootPackage:
                                  selected?.rootPackage ??
                                  stageForm.rootPackage,
                                active: true,
                              },
                            }));
                          }}
                        >
                          <option value="">Selecione etapa oficial</option>
                          {officialPipeline.stages.map((stage) => (
                            <option
                              key={stage.operationalCode}
                              value={stage.operationalCode}
                            >
                              {stage.position}. {stage.name} (
                              {stage.operationalCode})
                            </option>
                          ))}
                        </select>
                      ) : (
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
                      )}
                    </div>
                    <div className="col-md-3">
                      <label className="form-label">Módulo da etapa</label>
                      <input
                        className="form-control"
                        disabled={Boolean(stageDefinition)}
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
                        placeholder="Ex.: ai-worker (vazio = backend)"
                      />
                      <div className="form-text">
                        Preencha somente quando a etapa for executada fora do
                        backend.
                      </div>
                    </div>
                    <div className="col-md-5">
                      <label className="form-label">Pacote raiz</label>
                      <input
                        className="form-control"
                        disabled={Boolean(stageDefinition)}
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
                        placeholder="Ex.: com.marketinghub.experiment.pipeline"
                      />
                      <div className="form-text">
                        Informe o pacote raiz no backend ou no módulo executor.
                      </div>
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
