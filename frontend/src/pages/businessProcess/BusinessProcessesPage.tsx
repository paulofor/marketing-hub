import { FormEvent, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "react-toastify";
import {
  useBusinessProcesses,
  useBusinessProcessComposition,
  useBusinessProcessExecutionResources,
  useCreateBusinessProcess,
  useDeleteBusinessProcess,
  usePublishBusinessProcess,
  useUpdateBusinessProcess,
} from "../../api/businessProcess/useBusinessProcesses";
import { useBusinessProcessDocumentActivities } from "../../api/businessProcess/useBusinessProcessDocuments";
import type {
  BusinessProcess,
  ProcessDiagram,
} from "../../api/businessProcess/types";
import { useBusinessProcessChainsByProcess } from "../../api/businessProcessChain/useBusinessProcessChains";
import BusinessProcessEntityName from "../../components/BusinessProcessEntityName";
import PageTitle from "../../components/PageTitle";
import BusinessProcessCompositionPanel from "./BusinessProcessCompositionPanel";
import BusinessProcessDiagram from "./BusinessProcessDiagram";
import BusinessProcessEditor from "./BusinessProcessEditor";
import "./BusinessProcessesPage.css";

type BusinessProcessForm = {
  processCode: string;
  name: string;
  purpose: string;
  ownerName: string;
  triggerDescription: string;
  outcomeDescription: string;
  versionNumber: number;
  technicalReference: string;
  processType: "VALUE_PROCESS" | "SUBPROCESS";
  parentProcessCode: string;
  activities: string;
};

const initial: BusinessProcessForm = {
  processCode: "",
  name: "",
  purpose: "",
  ownerName: "",
  triggerDescription: "",
  outcomeDescription: "",
  versionNumber: 1,
  technicalReference: "",
  processType: "VALUE_PROCESS",
  parentProcessCode: "",
  activities: "",
};

function linearDiagram(activities: string): ProcessDiagram {
  const tasks = activities
    .split("\n")
    .map((item) => item.trim())
    .filter(Boolean);
  const nodes = [
    { id: "start", type: "START" as const, label: "Processo iniciado" },
    ...tasks.map((task, index) => ({
      id: `task-${index + 1}`,
      type: "TASK" as const,
      label: task,
    })),
    { id: "end", type: "END" as const, label: "Processo concluído" },
  ];
  return {
    nodes,
    flows: nodes
      .slice(0, -1)
      .map((node, index) => ({ from: node.id, to: nodes[index + 1].id })),
  };
}

type BusinessProcessesPageProps = {
  catalogMode?: "current" | "retired";
};

export default function BusinessProcessesPage({
  catalogMode = "current",
}: BusinessProcessesPageProps) {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const query = useBusinessProcesses();
  const executionResourcesQuery = useBusinessProcessExecutionResources();
  const create = useCreateBusinessProcess();
  const remove = useDeleteBusinessProcess();
  const publish = usePublishBusinessProcess();
  const update = useUpdateBusinessProcess();
  const [editing, setEditing] = useState<"draft" | "new-version">();
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(initial);
  const allProcesses = query.data ?? [];
  const retiredProcesses = allProcesses.filter(
    (item) => item.status === "RETIRED",
  );
  const processes = (
    catalogMode === "retired"
      ? retiredProcesses
      : allProcesses.filter((item) => item.status !== "RETIRED")
  ).sort((left, right) => {
    const typeComparison =
      (left.processType === "SUBPROCESS" ? 1 : 0) -
      (right.processType === "SUBPROCESS" ? 1 : 0);
    return typeComparison || left.name.localeCompare(right.name, "pt-BR");
  });
  const requestedId = Number(searchParams.get("processId"));
  const selectedId =
    Number.isSafeInteger(requestedId) && requestedId > 0
      ? requestedId
      : undefined;
  const selected = useMemo<BusinessProcess | undefined>(
    () =>
      processes.find((item) => item.id === selectedId) ??
      processes.find((item) => item.status === "PUBLISHED") ??
      processes[0],
    [processes, selectedId],
  );
  const compositionQuery = useBusinessProcessComposition(selected?.id);
  const processChainsQuery = useBusinessProcessChainsByProcess(selected?.id);
  const documentActivitiesQuery = useBusinessProcessDocumentActivities(
    selected?.id,
  );

  const selectProcess = (id?: number) => {
    const next = new URLSearchParams(searchParams);
    if (id === undefined) next.delete("processId");
    else next.set("processId", String(id));
    setSearchParams(next, { replace: true });
  };

  const editableValue = selected
    ? {
        processCode: selected.processCode,
        name: selected.name,
        purpose: selected.purpose,
        ownerName: selected.ownerName,
        triggerDescription: selected.triggerDescription,
        outcomeDescription: selected.outcomeDescription,
        versionNumber:
          editing === "new-version"
            ? Math.max(
                ...processes
                  .filter((item) => item.processCode === selected.processCode)
                  .map((item) => item.versionNumber),
              ) + 1
            : selected.versionNumber,
        technicalReference: selected.technicalReference,
        processType: selected.processType ?? "VALUE_PROCESS",
        parentProcessCode: selected.parentProcessCode,
        diagram: structuredClone(selected.diagram),
      }
    : undefined;

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    const saved = await create.mutateAsync({
      processCode: form.processCode,
      name: form.name,
      purpose: form.purpose,
      ownerName: form.ownerName,
      triggerDescription: form.triggerDescription,
      outcomeDescription: form.outcomeDescription,
      versionNumber: form.versionNumber,
      technicalReference: form.technicalReference || undefined,
      processType: form.processType,
      parentProcessCode:
        form.processType === "SUBPROCESS"
          ? form.parentProcessCode || undefined
          : undefined,
      diagram: linearDiagram(form.activities),
    });
    selectProcess(saved.id);
    setForm(initial);
    setShowForm(false);
    toast.success("Processo cadastrado como rascunho.");
  };

  return (
    <div className="business-process-page">
      <header className="d-flex flex-wrap justify-content-between gap-3 align-items-start mb-4">
        <div>
          <PageTitle>
            {catalogMode === "retired"
              ? "Processos aposentados"
              : "Processos do Marketing Hub"}
          </PageTitle>
          <p className="text-body-secondary mb-0">
            {catalogMode === "retired"
              ? "Histórico preservado para consulta, auditoria e criação de uma nova versão."
              : "Fonte de verdade vigente para responsabilidades, decisões e fluxos de negócio."}
          </p>
        </div>
        <div className="d-flex flex-wrap gap-2">
          {catalogMode === "retired" ? (
            <Link className="btn btn-outline-primary" to="/business-processes">
              Voltar aos processos atuais
            </Link>
          ) : (
            <>
              <Link
                className="btn btn-outline-secondary"
                to="/business-processes/retired"
              >
                Processos aposentados ({retiredProcesses.length})
              </Link>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => setShowForm((value) => !value)}
              >
                {showForm ? "Cancelar cadastro" : "Cadastrar processo"}
              </button>
            </>
          )}
        </div>
      </header>

      {catalogMode === "current" && showForm ? (
        <form className="card card-body mb-4" onSubmit={submit}>
          <h2 className="h5">Nova versão de processo</h2>
          <p className="small text-body-secondary">
            O cadastro nasce como rascunho. Publicá-lo torna essa versão a fonte
            de verdade vigente, sem executar o processo.
          </p>
          <div className="row g-3">
            <div className="col-md-4">
              <label className="form-label" htmlFor="process-code">
                Código *
              </label>
              <input
                id="process-code"
                className="form-control"
                required
                pattern="[a-z0-9]+(?:-[a-z0-9]+)*"
                value={form.processCode}
                onChange={(e) =>
                  setForm({ ...form, processCode: e.target.value })
                }
              />
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="process-name">
                Nome *
              </label>
              <input
                id="process-name"
                className="form-control"
                required
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            </div>
            <div className="col-md-4">
              <label className="form-label" htmlFor="process-type">
                Tipo *
              </label>
              <select
                id="process-type"
                className="form-select"
                required
                value={form.processType}
                onChange={(event) => {
                  const processType = event.target.value as
                    "VALUE_PROCESS" | "SUBPROCESS";
                  setForm({
                    ...form,
                    processType,
                    parentProcessCode:
                      processType === "SUBPROCESS"
                        ? form.parentProcessCode
                        : "",
                  });
                }}
              >
                <option value="VALUE_PROCESS">Processo de valor</option>
                <option value="SUBPROCESS">Subprocesso</option>
              </select>
            </div>
            {form.processType === "SUBPROCESS" ? (
              <div className="col-md-8">
                <label className="form-label" htmlFor="parent-process-code">
                  Código do processo de valor pai *
                </label>
                <select
                  id="parent-process-code"
                  className="form-select"
                  required
                  value={form.parentProcessCode}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      parentProcessCode: event.target.value,
                    })
                  }
                >
                  <option value="">Selecione</option>
                  {allProcesses
                    .filter(
                      (item) =>
                        item.status === "PUBLISHED" &&
                        (item.processType ?? "VALUE_PROCESS") ===
                          "VALUE_PROCESS",
                    )
                    .map((item) => (
                      <option value={item.processCode} key={item.id}>
                        {item.name} · v{item.versionNumber}
                      </option>
                    ))}
                </select>
              </div>
            ) : null}
            <div className="col-md-2">
              <label className="form-label" htmlFor="process-version">
                Versão *
              </label>
              <input
                id="process-version"
                type="number"
                min={1}
                className="form-control"
                required
                value={form.versionNumber}
                onChange={(e) =>
                  setForm({ ...form, versionNumber: Number(e.target.value) })
                }
              />
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="process-owner">
                Responsável pelo processo *
              </label>
              <input
                id="process-owner"
                className="form-control"
                required
                value={form.ownerName}
                onChange={(e) =>
                  setForm({ ...form, ownerName: e.target.value })
                }
              />
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="process-reference">
                Referência técnica
              </label>
              <input
                id="process-reference"
                className="form-control"
                value={form.technicalReference}
                onChange={(e) =>
                  setForm({ ...form, technicalReference: e.target.value })
                }
              />
            </div>
            <div className="col-12">
              <label className="form-label" htmlFor="process-purpose">
                Objetivo *
              </label>
              <textarea
                id="process-purpose"
                rows={2}
                className="form-control"
                required
                value={form.purpose}
                onChange={(e) => setForm({ ...form, purpose: e.target.value })}
              />
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="process-trigger">
                Evento de início *
              </label>
              <textarea
                id="process-trigger"
                rows={2}
                className="form-control"
                required
                value={form.triggerDescription}
                onChange={(e) =>
                  setForm({ ...form, triggerDescription: e.target.value })
                }
              />
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="process-outcome">
                Resultado esperado *
              </label>
              <textarea
                id="process-outcome"
                rows={2}
                className="form-control"
                required
                value={form.outcomeDescription}
                onChange={(e) =>
                  setForm({ ...form, outcomeDescription: e.target.value })
                }
              />
            </div>
            <div className="col-12">
              <label className="form-label" htmlFor="process-activities">
                Atividades, uma por linha *
              </label>
              <textarea
                id="process-activities"
                rows={5}
                className="form-control"
                required
                placeholder="Receber briefing\nProduzir material\nRevisar qualidade"
                value={form.activities}
                onChange={(e) =>
                  setForm({ ...form, activities: e.target.value })
                }
              />
            </div>
            <div className="col-12 text-end">
              <button className="btn btn-primary" disabled={create.isPending}>
                {create.isPending ? "Salvando..." : "Salvar rascunho"}
              </button>
            </div>
          </div>
        </form>
      ) : null}

      <div className="business-process-layout">
        <aside
          className="card business-process-list"
          aria-label={
            catalogMode === "retired"
              ? "Histórico de processos aposentados"
              : "Catálogo de processos atuais"
          }
        >
          <div className="card-header fw-semibold">
            {catalogMode === "retired" ? "Histórico" : "Catálogo atual"}
          </div>
          <div className="list-group list-group-flush">
            {catalogMode === "current" ? (
              <>
                <div className="business-process-list__group">
                  Processos de valor e suas especialidades
                </div>
                {processes
                  .filter(
                    (item) =>
                      (item.processType ?? "VALUE_PROCESS") === "VALUE_PROCESS",
                  )
                  .map((item) => {
                    const subprocesses = processes.filter(
                      (candidate) =>
                        candidate.processType === "SUBPROCESS" &&
                        (candidate.parentProcessDefinitionId === item.id ||
                          (!candidate.parentProcessDefinitionId &&
                            candidate.parentProcessCode === item.processCode &&
                            item.status === "PUBLISHED")),
                    );
                    return (
                      <div
                        className="business-process-list__tree"
                        key={item.id}
                      >
                        <button
                          type="button"
                          className={`list-group-item list-group-item-action business-process-list__parent ${selected?.id === item.id ? "active" : ""}`}
                          onClick={() => selectProcess(item.id)}
                        >
                          <span className="business-process-list__kind">
                            Processo de valor
                          </span>
                          <span className="d-block fw-semibold">
                            <BusinessProcessEntityName
                              kind="process"
                              name={item.name}
                              iconSize={17}
                            />
                          </span>
                          <span className="small">
                            v{item.versionNumber} · {item.status}
                          </span>
                        </button>
                        {subprocesses.length > 0 ? (
                          <div
                            className="business-process-list__children"
                            aria-label={`Subprocessos de ${item.name}`}
                          >
                            <span className="business-process-list__children-label">
                              Subprocessos deste processo
                            </span>
                            {subprocesses.map((subprocess) => (
                              <button
                                type="button"
                                key={subprocess.id}
                                className={`list-group-item list-group-item-action business-process-list__child ${selected?.id === subprocess.id ? "active" : ""}`}
                                onClick={() => selectProcess(subprocess.id)}
                              >
                                <span className="d-block fw-semibold">
                                  <BusinessProcessEntityName
                                    kind="process"
                                    name={subprocess.name}
                                    iconSize={16}
                                  />
                                </span>
                                <span className="small">
                                  v{subprocess.versionNumber} ·{" "}
                                  {subprocess.status}
                                </span>
                              </button>
                            ))}
                          </div>
                        ) : null}
                      </div>
                    );
                  })}
                {processes.some(
                  (item) =>
                    item.processType === "SUBPROCESS" &&
                    !processes.some(
                      (parent) =>
                        (parent.processType ?? "VALUE_PROCESS") ===
                          "VALUE_PROCESS" &&
                        (item.parentProcessDefinitionId === parent.id ||
                          (!item.parentProcessDefinitionId &&
                            item.parentProcessCode === parent.processCode &&
                            parent.status === "PUBLISHED")),
                    ),
                ) ? (
                  <div>
                    <div className="business-process-list__group">
                      Subprocessos sem pai vigente no catálogo
                    </div>
                    {processes
                      .filter(
                        (item) =>
                          item.processType === "SUBPROCESS" &&
                          !processes.some(
                            (parent) =>
                              (parent.processType ?? "VALUE_PROCESS") ===
                                "VALUE_PROCESS" &&
                              (item.parentProcessDefinitionId === parent.id ||
                                (!item.parentProcessDefinitionId &&
                                  item.parentProcessCode ===
                                    parent.processCode &&
                                  parent.status === "PUBLISHED")),
                          ),
                      )
                      .map((item) => (
                        <button
                          type="button"
                          key={item.id}
                          className={`list-group-item list-group-item-action ${selected?.id === item.id ? "active" : ""}`}
                          onClick={() => selectProcess(item.id)}
                        >
                          <span className="d-block fw-semibold">
                            <BusinessProcessEntityName
                              kind="process"
                              name={item.name}
                              iconSize={17}
                            />
                          </span>
                          <span className="small">
                            v{item.versionNumber} · {item.status}
                          </span>
                        </button>
                      ))}
                  </div>
                ) : null}
              </>
            ) : (
              processes.map((item, index) => (
                <div key={item.id}>
                  {(index === 0 ||
                    (processes[index - 1].processType ?? "VALUE_PROCESS") !==
                      (item.processType ?? "VALUE_PROCESS")) && (
                    <div className="business-process-list__group">
                      {(item.processType ?? "VALUE_PROCESS") === "SUBPROCESS"
                        ? "Subprocessos especializados"
                        : "Processos de valor"}
                    </div>
                  )}
                  <button
                    type="button"
                    className={`list-group-item list-group-item-action ${selected?.id === item.id ? "active" : ""}`}
                    onClick={() => selectProcess(item.id)}
                  >
                    <span className="d-block fw-semibold">
                      <BusinessProcessEntityName
                        kind="process"
                        name={item.name}
                        iconSize={17}
                      />
                    </span>
                    <span className="small">
                      v{item.versionNumber} · {item.status}
                    </span>
                  </button>
                </div>
              ))
            )}
          </div>
        </aside>

        <main>
          {selected ? (
            editing && editableValue ? (
              <BusinessProcessEditor
                key={`${selected.id}-${editing}`}
                initial={editableValue}
                identityLocked
                saving={create.isPending || update.isPending}
                executionResources={executionResourcesQuery.data ?? []}
                resourcesLoading={executionResourcesQuery.isLoading}
                resourcesUnavailable={executionResourcesQuery.isError}
                onCancel={() => setEditing(undefined)}
                onSave={async (value) => {
                  const saved =
                    editing === "draft"
                      ? await update.mutateAsync({ id: selected.id, value })
                      : await create.mutateAsync(value);
                  if (catalogMode === "retired") {
                    navigate(`/business-processes?processId=${saved.id}`);
                  } else {
                    selectProcess(saved.id);
                  }
                  setEditing(undefined);
                  toast.success(
                    "Rascunho salvo com todos os elementos do processo.",
                  );
                }}
              />
            ) : (
              <>
                <section className="card card-body mb-3">
                  <div className="d-flex flex-wrap justify-content-between gap-3">
                    <div>
                      <span
                        className={`badge ${selected.status === "PUBLISHED" ? "text-bg-success" : "text-bg-secondary"}`}
                      >
                        {selected.status}
                      </span>
                      <span className="badge text-bg-light ms-2">
                        {(selected.processType ?? "VALUE_PROCESS") ===
                        "SUBPROCESS"
                          ? "SUBPROCESSO"
                          : "PROCESSO DE VALOR"}
                      </span>
                      <h2 className="h4 mt-2 mb-1 business-process-detail-title">
                        <BusinessProcessEntityName
                          kind="process"
                          name={`${selected.name} · v${selected.versionNumber}`}
                        />
                      </h2>
                      <p className="mb-2">
                        <strong>Objetivo:</strong> {selected.purpose}
                      </p>
                      {(documentActivitiesQuery.data?.length ?? 0) > 0 ||
                      selected.diagram.nodes.some(
                        (node) => node.type === "TASK" && node.documentOutput,
                      ) ? (
                        <Link
                          className="business-process-objective-documents-link"
                          to={`/business-processes/${selected.id}/documents`}
                        >
                          Ver os 10 últimos documentos gerados
                        </Link>
                      ) : null}
                      <div className="small text-body-secondary">
                        Dono: {selected.ownerName}
                        {selected.technicalReference
                          ? ` · Contrato: ${selected.technicalReference}`
                          : ""}
                      </div>
                      {(processChainsQuery.data?.length ?? 0) > 0 ? (
                        <div
                          className="business-process-chains mt-3"
                          aria-label="Cadeias de valor deste processo"
                        >
                          <strong className="small">Cadeia de valor</strong>
                          <div className="d-flex flex-wrap gap-2 mt-1">
                            {processChainsQuery.data?.map((chain) => (
                              <Link
                                key={chain.id}
                                className="btn btn-sm btn-outline-primary"
                                to={`/business-process-chains?chainId=${chain.id}`}
                              >
                                {chain.name} · v{chain.versionNumber}
                              </Link>
                            ))}
                          </div>
                        </div>
                      ) : null}
                    </div>
                    <div className="d-flex gap-2 align-self-start">
                      <button
                        type="button"
                        className="btn btn-outline-primary"
                        onClick={() =>
                          setEditing(
                            selected.status === "DRAFT"
                              ? "draft"
                              : "new-version",
                          )
                        }
                      >
                        {selected.status === "DRAFT"
                          ? "Editar rascunho"
                          : "Criar versão editável"}
                      </button>
                      {selected.status === "DRAFT" ? (
                        <>
                          <button
                            type="button"
                            className="btn btn-outline-danger"
                            disabled={remove.isPending}
                            onClick={async () => {
                              if (
                                !window.confirm(
                                  `Excluir o rascunho "${selected.name}" v${selected.versionNumber}?`,
                                )
                              )
                                return;
                              await remove.mutateAsync(selected.id);
                              selectProcess();
                              toast.success("Rascunho excluído com segurança.");
                            }}
                          >
                            {remove.isPending
                              ? "Excluindo..."
                              : "Excluir rascunho"}
                          </button>
                          <button
                            type="button"
                            className="btn btn-success"
                            disabled={publish.isPending}
                            onClick={async () => {
                              await publish.mutateAsync(selected.id);
                              toast.success(
                                "Versão publicada como fonte de verdade.",
                              );
                            }}
                          >
                            {publish.isPending
                              ? "Publicando..."
                              : "Publicar definição"}
                          </button>
                        </>
                      ) : null}
                    </div>
                  </div>
                  <div className="process-summary mt-3">
                    <div>
                      <strong>Início</strong>
                      <span>{selected.triggerDescription}</span>
                    </div>
                    <div>
                      <strong>Resultado</strong>
                      <span>{selected.outcomeDescription}</span>
                    </div>
                    <div>
                      <strong>Elementos</strong>
                      <span>
                        {selected.diagram.nodes.length} etapas ·{" "}
                        {
                          selected.diagram.nodes.filter(
                            (n) => n.type === "GATEWAY",
                          ).length
                        }{" "}
                        gates
                      </span>
                    </div>
                  </div>
                </section>
                <BusinessProcessCompositionPanel
                  composition={compositionQuery.data}
                  loading={compositionQuery.isLoading}
                  unavailable={compositionQuery.isError}
                />
                <section className="card card-body">
                  <h2 className="h5 business-process-diagram-title">
                    <BusinessProcessEntityName
                      kind="process"
                      name="Diagrama BPM"
                      iconSize={18}
                    />
                  </h2>
                  {executionResourcesQuery.isError ? (
                    <div className="alert alert-warning" role="alert">
                      O catálogo de recursos especializados está indisponível.
                      Os códigos persistidos continuam visíveis, mas novas
                      versões não podem selecionar um recurso até a recuperação.
                    </div>
                  ) : null}
                  <BusinessProcessDiagram
                    diagram={selected.diagram}
                    executionResources={executionResourcesQuery.data ?? []}
                    processDefinitionId={selected.id}
                    documentActivityIds={documentActivitiesQuery.data ?? []}
                    subprocesses={compositionQuery.data?.subprocesses ?? []}
                  />
                </section>
              </>
            )
          ) : (
            <div className="card card-body text-body-secondary">
              {query.isLoading
                ? "Carregando processos..."
                : catalogMode === "retired"
                  ? "Nenhum processo aposentado."
                  : "Nenhum processo atual cadastrado."}
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
