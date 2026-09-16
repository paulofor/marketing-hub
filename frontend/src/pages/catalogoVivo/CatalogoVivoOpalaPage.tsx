import { useState } from "react";
import { Link } from "react-router-dom";
import {
  catalogError,
  useCatalogCommand,
  usePromptCatalog,
  type PromptCatalog,
} from "../../api/catalogoVivo/useCatalogoVivo";

export default function CatalogoVivoOpalaPage() {
  const query = usePromptCatalog();
  return (
    <main className="container py-4">
      <h1>Catálogo Vivo — Piloto Opala</h1>
      <p>
        Instruções dos agentes para preparar a operação comercial. Cada tarefa
        conserva a versão com que começou.
      </p>
      <Link to="/agents">Voltar aos agentes</Link>
      {query.isPending && <p role="status">Carregando catálogo…</p>}
      {query.isError && (
        <div className="alert alert-danger" role="alert">
          {catalogError(query.error)}{" "}
          <button
            className="btn btn-outline-secondary"
            disabled={query.isFetching}
            onClick={() => query.refetch()}
          >
            {query.isFetching && (
              <span className="spinner-border spinner-border-sm me-2" />
            )}
            Tentar novamente
          </button>
        </div>
      )}
      {query.data && <CatalogEditor catalog={query.data} />}
    </main>
  );
}

export function CatalogEditor({ catalog }: { catalog: PromptCatalog }) {
  const command = useCatalogCommand();
  const [operatorName, setOperatorName] = useState("");
  const [reason, setReason] = useState("");
  const [selected, setSelected] = useState<Record<number, number>>({});
  const [text, setText] = useState<Record<number, string>>({});
  const [notice, setNotice] = useState("");
  const pending = command.isPending;
  function selectedId(item: PromptCatalog["items"][number]) {
    return (
      selected[item.binding.id] ??
      item.binding.activeVersionId ??
      item.versions[0]?.id
    );
  }
  async function send(path: string, body: unknown, message: string) {
    setNotice("");
    const result = await command.mutateAsync({ path, body });
    setNotice(message);
    return result;
  }
  async function activate() {
    const versions = Object.fromEntries(
      catalog.items.map((item) => [item.binding.id, selectedId(item)]),
    );
    const expectedActiveVersions = Object.fromEntries(
      catalog.items.map(({ binding }) => [binding.id, binding.activeVersionId]),
    );
    try {
      await send(
        "/activation",
        { versions, expectedActiveVersions, operatorName, reason },
        "Conjunto ativado para novas tarefas. As tarefas existentes preservaram suas versões.",
      );
    } catch {
      /* Erro apresentado pelo contrato da mutação. */
    }
  }
  return (
    <>
      <div
        className={`alert mt-3 ${catalog.ready ? "alert-success" : "alert-warning"}`}
        role="status"
      >
        {catalog.ready
          ? "Conjunto de instruções disponível para os agentes."
          : "Há pendências no catálogo. Corrija os itens abaixo."}
        {catalog.issues.length > 0 && (
          <ul>
            {catalog.issues.map((issue, index) => (
              <li key={index}>{issue}</li>
            ))}
          </ul>
        )}
      </div>
      <p>
        Origem: banco de dados · {catalog.pinnedTasks} tarefas com versão fixada
        · {catalog.resolutionFailures} bloqueios de resolução.
      </p>
      <p>
        Revisar ou ativar um texto preserva os controles de orçamento,
        publicação e aprovação comercial.
      </p>
      <div className="row g-3 mb-3">
        <label className="col-md-4">
          Responsável *
          <input
            className="form-control"
            required
            maxLength={160}
            value={operatorName}
            disabled={pending}
            onChange={(e) => setOperatorName(e.target.value)}
          />
        </label>
        <label className="col-md-8">
          Motivo ou parecer *
          <input
            className="form-control"
            required
            maxLength={1000}
            value={reason}
            disabled={pending}
            onChange={(e) => setReason(e.target.value)}
          />
        </label>
      </div>
      {command.isError && (
        <p className="alert alert-danger" role="alert">
          {catalogError(command.error)}
        </p>
      )}
      {notice && (
        <p className="alert alert-info" role="status">
          {notice}
        </p>
      )}
      {catalog.items.map((item) => {
        const binding = item.binding;
        const version = item.versions.find((v) => v.id === selectedId(item));
        const currentText = text[binding.id] ?? version?.text ?? "";
        const dirty = currentText !== version?.text;
        return (
          <section
            id={`binding-${binding.id}`}
            key={binding.id}
            className="card card-body mb-3"
            aria-label={binding.activityName}
          >
            <h2 className="h5">{binding.activityName}</h2>
            <p>
              <Link to={`/agents/${binding.agentId}`}>{binding.agentName}</Link>{" "}
              · Tipo Opala · Ativa: v
              {item.versions.find((v) => v.id === binding.activeVersionId)
                ?.versionNumber ?? "não definida"}
            </p>
            <label>
              Versão para consultar ou ativar
              <select
                className="form-select mb-3"
                value={version?.id ?? ""}
                disabled={pending}
                onChange={(e) => {
                  setSelected({
                    ...selected,
                    [binding.id]: Number(e.target.value),
                  });
                  setText((previous) => {
                    const next = { ...previous };
                    delete next[binding.id];
                    return next;
                  });
                }}
              >
                {item.versions.map((v) => (
                  <option key={v.id} value={v.id}>
                    v{v.versionNumber} ·{" "}
                    {v.status === "REVIEWED" ? "Revisada" : "Rascunho"}
                    {v.id === binding.activeVersionId ? " · ativa" : ""}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Instrução da atividade *
              <textarea
                className="form-control"
                rows={8}
                required
                maxLength={100000}
                value={currentText}
                disabled={pending}
                onChange={(e) =>
                  setText({ ...text, [binding.id]: e.target.value })
                }
              />
            </label>
            <p className="text-muted mt-2">
              Preserve o marcador {"{{TASK_CONTEXT}}"}: ele recebe os dados da
              tarefa. Salvar cria uma nova versão.
            </p>
            <div className="d-flex flex-wrap gap-2">
              <button
                className="btn btn-outline-primary"
                disabled={
                  pending ||
                  !operatorName.trim() ||
                  !reason.trim() ||
                  !currentText.trim()
                }
                onClick={async () => {
                  try {
                    const saved = await send(
                      `/bindings/${binding.id}/drafts`,
                      { text: currentText, operatorName, reason },
                      "Rascunho salvo. Confira o texto e registre sua revisão.",
                    );
                    setSelected((previous) => ({
                      ...previous,
                      [binding.id]: saved.id,
                    }));
                    setText((previous) => {
                      const next = { ...previous };
                      delete next[binding.id];
                      return next;
                    });
                  } catch {
                    /* Erro apresentado pelo contrato da mutação. */
                  }
                }}
              >
                {pending && (
                  <span className="spinner-border spinner-border-sm me-2" />
                )}
                Salvar como nova versão
              </button>
              {version?.status === "DRAFT" && (
                <button
                  className="btn btn-outline-secondary"
                  disabled={
                    pending || dirty || !operatorName.trim() || !reason.trim()
                  }
                  onClick={async () => {
                    try {
                      await send(
                        `/versions/${version.id}/review`,
                        {
                          operatorName,
                          reason,
                          expectedSha256: version.sha256,
                        },
                        "Revisão registrada. O conjunto pode ser ativado após conferir as demais atividades.",
                      );
                    } catch {
                      /* Erro apresentado pelo contrato da mutação. */
                    }
                  }}
                >
                  {pending && (
                    <span className="spinner-border spinner-border-sm me-2" />
                  )}
                  Registrar revisão desta versão
                </button>
              )}
            </div>
            {dirty && <p role="status">Há texto não salvo nesta atividade.</p>}
            <details className="mt-3">
              <summary>Histórico e rastreabilidade</summary>
              <p className="text-break">
                Versão #{version?.id} · SHA-256: {version?.sha256}
              </p>
              <p>
                Criada por {version?.createdBy}. Revisão:{" "}
                {version?.reviewedBy ?? "pendente"}.
              </p>
              <p>{version?.reviewNote}</p>
              <p>Utilizações recentes desta instrução:</p>
              {item.usages?.length ? (
                <ul>
                  {item.usages.map((use) => (
                    <li key={use.taskId}>
                      Tarefa #{use.taskId} · v{use.versionNumber} ·{" "}
                      {use.sourceReference} · {use.status} ·{" "}
                      <Link to={`/agents/${binding.agentId}`}>Ver agente</Link>
                    </li>
                  ))}
                </ul>
              ) : (
                <p>Nenhuma utilização registrada.</p>
              )}
              <ul>
                {item.events.map((event) => (
                  <li key={event.id}>
                    {new Date(event.createdAt).toLocaleString("pt-BR")} ·{" "}
                    {event.operatorName}: {event.note}
                  </li>
                ))}
              </ul>
            </details>
          </section>
        );
      })}
      <button
        className="btn btn-primary"
        disabled={
          pending ||
          !operatorName.trim() ||
          !reason.trim() ||
          catalog.items.some(
            (item) =>
              text[item.binding.id] !== undefined &&
              text[item.binding.id] !==
                item.versions.find((v) => v.id === selectedId(item))?.text,
          )
        }
        onClick={activate}
      >
        {pending && <span className="spinner-border spinner-border-sm me-2" />}
        Ativar conjunto selecionado
      </button>
      <p className="mt-2">
        Para recuperar uma instrução anterior, selecione a versão revisada
        desejada e ative o conjunto. O histórico permanece disponível.
      </p>
    </>
  );
}
