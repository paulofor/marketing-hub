import { useMemo, useState } from "react";
import { Sparkles } from "lucide-react";
import PageTitle from "../../components/PageTitle";
import "./PipelineCrudPage.css";
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
  OfficialPipelineMetadata,
} from "../../api/pipeline/types";
import {
  useRebuildOfficialPipelineStages,
  useSyncOfficialPipeline,
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

function diagnosticPanelClass(status: PipelineDiagnostics["status"]) {
  if (status === "OK") return "pipeline-contract-ok";
  if (status === "BLOQUEADO") return "pipeline-contract-bloqueado";
  return "pipeline-contract-atencao";
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

type PipelineImplementationSummary = {
  implementationModules: string[];
  backendPackages: string[];
  modulePackages: string[];
};

function uniqueText(values: Array<string | null | undefined>) {
  return Array.from(
    new Set(values.map((value) => value?.trim()).filter(Boolean) as string[]),
  ).sort((first, second) => first.localeCompare(second));
}

function findOfficialPipeline(
  pipeline: Pipeline,
  officialPipelines: OfficialPipelineMetadata[],
) {
  return officialPipelines.find(
    (official) =>
      normalizeCode(official.code) === normalizeCode(pipeline.code) ||
      official.aliases.some(
        (alias) => normalizeCode(alias) === normalizeCode(pipeline.code),
      ),
  );
}

function pipelineImplementationSummary(
  pipeline: Pipeline,
  officialPipelines: OfficialPipelineMetadata[],
): PipelineImplementationSummary {
  const official = findOfficialPipeline(pipeline, officialPipelines);
  return {
    implementationModules: uniqueText([
      ...(official?.implementationModules ?? []),
      ...pipeline.stages.map((stage) => stage.executionModule),
    ]),
    backendPackages: uniqueText([
      ...(official?.backendPackages ?? []),
      ...pipeline.stages.map((stage) => stage.rootPackage),
    ]),
    modulePackages: uniqueText([
      ...(official?.modulePackages ?? []),
      ...(official?.stages.map((stage) => stage.modulePackage) ?? []),
    ]),
  };
}

function PipelineImplementationDetails({
  summary,
}: {
  summary: PipelineImplementationSummary;
}) {
  const implementationModules =
    summary.implementationModules.length > 0
      ? summary.implementationModules
      : ["Backend principal"];
  const backendPackages =
    summary.backendPackages.length > 0
      ? summary.backendPackages
      : ["Não informado"];
  const modulePackages =
    summary.modulePackages.length > 0
      ? summary.modulePackages
      : ["Não informado"];

  return (
    <dl className="pipeline-implementation-details">
      <div>
        <dt>Módulo que implementa</dt>
        <dd>
          {implementationModules.map((module) => (
            <code key={module}>{module}</code>
          ))}
        </dd>
      </div>
      <div>
        <dt>Nome do pacote no backend</dt>
        <dd>
          {backendPackages.map((packageName) => (
            <code key={packageName}>{packageName}</code>
          ))}
        </dd>
      </div>
      <div>
        <dt>Nome do pacote no módulo</dt>
        <dd>
          {modulePackages.map((packageName) => (
            <code key={packageName}>{packageName}</code>
          ))}
        </dd>
      </div>
    </dl>
  );
}
function stageUsesOpenAi(
  stage: PipelineStage,
  stageDefinition?: { requiresOpenAiModel?: boolean | null },
) {
  if (stageDefinition?.requiresOpenAiModel) return true;
  if (stage.openAiModelId || stage.openAiModelName || stage.openAiModelCode) {
    return true;
  }
  const executionContext = `${stage.executionModule ?? ""} ${stage.rootPackage ?? ""}`;
  return /\b(ai|openai)\b/i.test(executionContext);
}

export default function PipelineCrudPage() {
  const { data, isLoading, isError } = usePipelines();
  const { data: metadata, isLoading: isLoadingMetadata } =
    usePipelineMetadata();
  const { data: openAiModels, isLoading: isLoadingOpenAiModels } =
    useOpenAiModels();
  const updatePipeline = useUpdatePipeline();
  const updateStage = useUpdatePipelineStage();
  const rebuildOfficialStages = useRebuildOfficialPipelineStages();
  const syncOfficialPipeline = useSyncOfficialPipeline();

  const pipelines = useMemo(() => data ?? [], [data]);
  const diagnosticsQueries = usePipelineDiagnostics(pipelines);
  const modelOptions = useMemo(() => openAiModels ?? [], [openAiModels]);
  const officialPipelines = useMemo(
    () => metadata?.officialPipelines ?? [],
    [metadata],
  );
  const missingOfficialPipelines = useMemo(
    () =>
      officialPipelines.filter(
        (official) =>
          !pipelines.some((pipeline) =>
            findOfficialPipeline(pipeline, [official]),
          ),
      ),
    [officialPipelines, pipelines],
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
  const [syncingOfficialCode, setSyncingOfficialCode] = useState<string | null>(
    null,
  );

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

  const syncMissingOfficialPipeline = (official: OfficialPipelineMetadata) => {
    setSyncingOfficialCode(official.code);
    syncOfficialPipeline.mutate(official.code, {
      onSuccess: (result) => {
        if (result.pipelineId) {
          setSelectedPipelineId(result.pipelineId);
        }
      },
      onSettled: () => setSyncingOfficialCode(null),
    });
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

  const confirmOpenAiModelChange = (
    pipelineId: number,
    stage: PipelineStage,
    openAiModelId: number | null,
  ) => {
    const selectedModel = modelOptions.find(
      (model) => model.id === openAiModelId,
    );
    const targetLabel = selectedModel
      ? `${selectedModel.name} (${selectedModel.code})`
      : "Sem modelo fixo";

    if (
      !confirm(
        `Deseja alterar o modelo OpenAI da etapa "${stage.name}" para "${targetLabel}"?`,
      )
    ) {
      return false;
    }

    updateStage.mutate({
      pipelineId,
      stageId: stage.id,
      payload: {
        ...stageToPayload(stage),
        openAiModelId,
      },
    });
    return true;
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
      <div className="pipeline-stage-page">
        <div className="pipeline-stage-toolbar">
          <div>
            <PageTitle>Etapas do pipeline</PageTitle>
            <p className="pipeline-stage-subtitle mb-0">
              Ordem visual clara: primeiro o contrato, depois o fluxo das etapas
              e por último os detalhes técnicos de execução.
            </p>
          </div>
          <button
            type="button"
            className="btn btn-outline-secondary pipeline-stage-back-button"
            onClick={returnToPipelineList}
          >
            ← Voltar para lista
          </button>
        </div>

        <section className="pipeline-overview-card mb-4">
          <div className="pipeline-overview-body">
            <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
              <div>
                <div className="pipeline-eyebrow">Pipeline selecionado</div>
                <h2 className="pipeline-overview-title">{pipeline.name}</h2>
                <div className="pipeline-badge-row">
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
              <p className="pipeline-overview-description">
                {pipeline.description}
              </p>
            ) : null}
            <PipelineImplementationDetails
              summary={pipelineImplementationSummary(
                pipeline,
                officialPipelines,
              )}
            />
          </div>
        </section>

        {diagnostic ? (
          <div
            className={`pipeline-contract-panel ${diagnosticPanelClass(diagnostic.status)}`}
          >
            <div className="pipeline-contract-kicker">Contrato operacional</div>
            <div className="pipeline-contract-grid">
              <div>
                <div className="pipeline-contract-status">
                  {diagnostic.status}
                </div>
                <div className="pipeline-contract-caption">
                  Status atual do fluxo
                </div>
              </div>
              <div className="pipeline-contract-counts">
                <span>
                  <strong>{diagnostic.expectedStages}</strong> esperadas no
                  código
                </span>
                <span>
                  <strong>{diagnostic.configuredStages}</strong> configuradas no
                  banco
                </span>
              </div>
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

        <section className="pipeline-stage-section mb-4">
          <div className="pipeline-section-header">
            <div>
              <div className="pipeline-eyebrow">Fluxo operacional</div>
              <h3 className="pipeline-section-title">Etapas configuradas</h3>
            </div>
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
            <div className="pipeline-stage-grid">
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
                const showExecutionModule = Boolean(
                  stage.executionModule?.trim(),
                );
                const showOpenAiModel = stageUsesOpenAi(stage, rowDefinition);

                return (
                  <div className="pipeline-stage-grid-item" key={stage.id}>
                    <article
                      className={`pipeline-stage-card ${showOpenAiModel ? "pipeline-stage-card-ai" : "pipeline-stage-card-manual"}`}
                    >
                      <div className="pipeline-stage-card-body">
                        <div className="pipeline-stage-card-header">
                          <div className="pipeline-stage-title-block">
                            <div
                              className="pipeline-stage-number"
                              aria-label={`Etapa ${stage.position}`}
                            >
                              {stage.position}
                            </div>
                            <div>
                              <div className="pipeline-stage-label">
                                Etapa {stage.position}
                              </div>
                              <h4 className="pipeline-stage-title">
                                {stage.name}
                              </h4>
                            </div>
                          </div>
                          {showOpenAiModel ? (
                            <span
                              className="pipeline-stage-ai-indicator"
                              title="Esta etapa usa IA"
                              aria-label="Esta etapa usa IA"
                            >
                              <Sparkles size={16} aria-hidden="true" />
                              <span>IA</span>
                            </span>
                          ) : null}
                        </div>
                        <div className="pipeline-stage-badges">
                          <span className="badge text-bg-light border">
                            {stage.code}
                          </span>
                        </div>
                        <div className="pipeline-stage-meta">
                          {showExecutionModule ? (
                            <div>
                              <span>Módulo executor</span>
                              <strong>{stage.executionModule}</strong>
                            </div>
                          ) : null}
                          <div>
                            <span>Pacote raiz</span>
                            <strong>
                              {stage.rootPackage || "Não informado"}
                            </strong>
                          </div>
                          {showOpenAiModel ? (
                            <div className="pipeline-stage-model-control">
                              <label htmlFor={`stage-model-${stage.id}`}>
                                Modelo OpenAI
                              </label>
                              <select
                                id={`stage-model-${stage.id}`}
                                className="form-select form-select-sm"
                                disabled={
                                  isLoadingOpenAiModels ||
                                  (updateStage.isPending &&
                                    updateStage.variables?.stageId === stage.id)
                                }
                                value={stage.openAiModelId ?? ""}
                                onChange={(event) => {
                                  const confirmed = confirmOpenAiModelChange(
                                    pipeline.id,
                                    stage,
                                    event.target.value
                                      ? Number(event.target.value)
                                      : null,
                                  );
                                  if (!confirmed) {
                                    event.currentTarget.value = String(
                                      stage.openAiModelId ?? "",
                                    );
                                  }
                                }}
                              >
                                <option value="">
                                  {isLoadingOpenAiModels
                                    ? "Carregando modelos..."
                                    : "Sem modelo fixo"}
                                </option>
                                {modelOptions.map((model) => (
                                  <option key={model.id} value={model.id}>
                                    {model.name} ({model.code})
                                    {model.acceptsImageInput
                                      ? " · aceita imagem"
                                      : ""}
                                  </option>
                                ))}
                              </select>
                            </div>
                          ) : null}
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

      {missingOfficialPipelines.length > 0 ? (
        <section className="card mb-4 border-primary-subtle">
          <div className="card-header bg-white">
            <h2 className="h5 mb-1">Pipelines oficiais disponíveis</h2>
            <p className="text-body-secondary small mb-0">
              Ative aqui os fluxos canônicos que já existem no backend, mas
              ainda não foram gravados no banco para operação.
            </p>
          </div>
          <div className="list-group list-group-flush">
            {missingOfficialPipelines.map((official) => {
              const isSyncing =
                syncOfficialPipeline.isPending &&
                syncingOfficialCode === official.code;

              return (
                <article className="list-group-item" key={official.code}>
                  <div className="pipeline-list-row">
                    <div className="pipeline-list-content">
                      <div className="d-flex flex-wrap align-items-center gap-2 mb-1">
                        <h3 className="h5 mb-0">{official.name}</h3>
                        <span className="badge text-bg-light border">
                          {official.module}
                        </span>
                      </div>
                      <div className="d-flex flex-wrap gap-2 small text-body-secondary">
                        <code>{official.code}</code>
                        <span>•</span>
                        <span>
                          {official.stages.length} etapa
                          {official.stages.length === 1 ? "" : "s"}
                        </span>
                      </div>
                      <PipelineImplementationDetails
                        summary={{
                          implementationModules:
                            official.implementationModules ?? [],
                          backendPackages: official.backendPackages ?? [],
                          modulePackages: official.modulePackages ?? [],
                        }}
                      />
                    </div>
                    <div className="pipeline-list-action">
                      <button
                        type="button"
                        className="btn btn-outline-primary btn-sm pipeline-list-stage-button"
                        disabled={syncOfficialPipeline.isPending}
                        onClick={() => syncMissingOfficialPipeline(official)}
                      >
                        {isSyncing ? (
                          <span
                            className="spinner-border spinner-border-sm me-2"
                            aria-hidden="true"
                          />
                        ) : null}
                        Ativar pipeline
                      </button>
                    </div>
                  </div>
                </article>
              );
            })}
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
              return (
                <article className="list-group-item" key={pipeline.id}>
                  <div className="pipeline-list-row">
                    <div className="pipeline-list-content">
                      <div className="d-flex flex-wrap align-items-center gap-2 mb-1">
                        <h3 className="h5 mb-0">{pipeline.name}</h3>
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
                      <PipelineImplementationDetails
                        summary={pipelineImplementationSummary(
                          pipeline,
                          officialPipelines,
                        )}
                      />
                    </div>
                    <div className="pipeline-list-action">
                      <button
                        type="button"
                        className="btn btn-primary btn-sm pipeline-list-stage-button"
                        onClick={() => openPipelineStages(pipeline.id)}
                      >
                        Ver etapas
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
