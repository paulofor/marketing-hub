import { FormEvent, useMemo, useState } from "react";
import { toast } from "react-toastify";
import {
  useBusinessProcesses,
  useCreateBusinessProcess,
  usePublishBusinessProcess,
  useUpdateBusinessProcess,
} from "../../api/businessProcess/useBusinessProcesses";
import type {
  BusinessProcess,
  ProcessDiagram,
} from "../../api/businessProcess/types";
import PageTitle from "../../components/PageTitle";
import BusinessProcessDiagram from "./BusinessProcessDiagram";
import BusinessProcessEditor from "./BusinessProcessEditor";
import "./BusinessProcessesPage.css";

const initial = {
  processCode: "",
  name: "",
  purpose: "",
  ownerName: "",
  triggerDescription: "",
  outcomeDescription: "",
  versionNumber: 1,
  technicalReference: "",
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

export default function BusinessProcessesPage() {
  const query = useBusinessProcesses();
  const create = useCreateBusinessProcess();
  const publish = usePublishBusinessProcess();
  const update = useUpdateBusinessProcess();
  const [selectedId, setSelectedId] = useState<number>();
  const [editing, setEditing] = useState<"draft" | "new-version">();
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(initial);
  const processes = query.data ?? [];
  const selected = useMemo<BusinessProcess | undefined>(
    () =>
      processes.find((item) => item.id === selectedId) ??
      processes.find((item) => item.status === "PUBLISHED") ??
      processes[0],
    [processes, selectedId],
  );

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
      diagram: linearDiagram(form.activities),
    });
    setSelectedId(saved.id);
    setForm(initial);
    setShowForm(false);
    toast.success("Processo cadastrado como rascunho.");
  };

  return (
    <div className="business-process-page">
      <header className="d-flex flex-wrap justify-content-between gap-3 align-items-start mb-4">
        <div>
          <PageTitle>Processos do Marketing Hub</PageTitle>
          <p className="text-body-secondary mb-0">
            Fonte de verdade versionada para responsabilidades, decisões e
            fluxos de negócio.
          </p>
        </div>
        <button
          type="button"
          className="btn btn-primary"
          onClick={() => setShowForm((value) => !value)}
        >
          {showForm ? "Cancelar cadastro" : "Cadastrar processo"}
        </button>
      </header>

      {showForm ? (
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
          aria-label="Catálogo de processos"
        >
          <div className="card-header fw-semibold">Catálogo</div>
          <div className="list-group list-group-flush">
            {processes.map((item) => (
              <button
                key={item.id}
                type="button"
                className={`list-group-item list-group-item-action ${selected?.id === item.id ? "active" : ""}`}
                onClick={() => setSelectedId(item.id)}
              >
                <span className="d-block fw-semibold">{item.name}</span>
                <span className="small">
                  v{item.versionNumber} · {item.status}
                </span>
              </button>
            ))}
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
                onCancel={() => setEditing(undefined)}
                onSave={async (value) => {
                  const saved =
                    editing === "draft"
                      ? await update.mutateAsync({ id: selected.id, value })
                      : await create.mutateAsync(value);
                  setSelectedId(saved.id);
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
                      <h2 className="h4 mt-2 mb-1">
                        {selected.name} · v{selected.versionNumber}
                      </h2>
                      <p className="mb-2">{selected.purpose}</p>
                      <div className="small text-body-secondary">
                        Dono: {selected.ownerName}
                        {selected.technicalReference
                          ? ` · Contrato: ${selected.technicalReference}`
                          : ""}
                      </div>
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
                <section className="card card-body">
                  <h2 className="h5">Diagrama BPM</h2>
                  <BusinessProcessDiagram diagram={selected.diagram} />
                </section>
              </>
            )
          ) : (
            <div className="card card-body text-body-secondary">
              {query.isLoading
                ? "Carregando processos..."
                : "Nenhum processo cadastrado."}
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
