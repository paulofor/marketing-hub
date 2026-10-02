import LearningCycleBlueprint from "../learningCycle/LearningCycleBlueprint";
import { useCycleCatalog } from "../../api/learningCycle/useLearningCycles";
import { useState } from "react";
import axios from "axios";
import { Link, useSearchParams } from "react-router-dom";
import {
  useBusinessProcessChain,
  useBusinessProcessChainCatalog,
  useCreateBusinessProcessChainDraft,
  useSaveBusinessProcessChain,
  usePublishBusinessProcessChain,
} from "../../api/businessProcessChain/useBusinessProcessChains";
import BusinessProcessChainEditor from "./BusinessProcessChainEditor";
import PageTitle from "../../components/PageTitle";
import "./BusinessProcessChainsPage.css";

export default function BusinessProcessChainsPage() {
  const chains = useBusinessProcessChainCatalog();
  const createDraft = useCreateBusinessProcessChainDraft();
  const save = useSaveBusinessProcessChain();
  const publish = usePublishBusinessProcessChain();
  const [editing, setEditing] = useState(false);
  const [error, setError] = useState<string>();
  const [notice, setNotice] = useState<string>();
  const [searchParams, setSearchParams] = useSearchParams();
  const requestedId = Number(searchParams.get("chainId"));
  const selectedId =
    Number.isSafeInteger(requestedId) && requestedId > 0
      ? requestedId
      : undefined;
  const activeId = selectedId ?? chains.data?.[0]?.id;
  const detail = useBusinessProcessChain(activeId);
  const busy = createDraft.isPending || save.isPending || publish.isPending;
  async function run(operation: () => Promise<void>) {
    setError(undefined);
    setNotice(undefined);
    try {
      await operation();
    } catch (cause) {
      const message = axios.isAxiosError(cause)
        ? (cause.response?.data?.detail ?? cause.response?.data?.message)
        : undefined;
      setError(
        typeof message === "string"
          ? message
          : "Não foi possível salvar a cadeia. Confira o estado e tente novamente.",
      );
    }
  }
  function selectChain(id: number) {
    const next = new URLSearchParams(searchParams);
    next.set("chainId", String(id));
    setSearchParams(next, { replace: true });
    setEditing(false);
    setError(undefined);
  }
  const productId = Number(searchParams.get("productId")) || undefined;
  const cycleCatalog = useCycleCatalog(activeId, productId);

  return (
    <div className="business-process-chain-page">
      <header className="mb-4">
        <PageTitle>Cadeias de criação e entrega de valor</PageTitle>
        <p className="text-body-secondary mb-0">
          Visão simples dos processos que transformam uma oportunidade em valor
          entregue à cliente e receita para o Marketing Hub.
        </p>
      </header>

      <div className="business-process-chain-layout">
        <aside
          className="card business-process-chain-list"
          aria-label="Lista de cadeias de processos"
        >
          <div className="card-header fw-semibold">Cadeias</div>
          <div className="list-group list-group-flush">
            {(chains.data ?? []).map((chain) => (
              <button
                key={chain.id}
                type="button"
                className={`list-group-item list-group-item-action ${activeId === chain.id ? "active" : ""}`}
                disabled={busy}
                onClick={() => selectChain(chain.id)}
              >
                <span className="d-block fw-semibold">{chain.name}</span>
                <span className="small">
                  v{chain.versionNumber} · {chain.processCount} processos
                  {chain.status === "DRAFT" ? " · Rascunho" : ""}
                </span>
              </button>
            ))}
            {!chains.isLoading && chains.data?.length === 0 ? (
              <div className="p-3 small text-body-secondary">
                Nenhuma cadeia cadastrada.
              </div>
            ) : null}
          </div>
        </aside>

        <main>
          {error ? (
            <p className="alert alert-danger" role="alert">
              {error}
            </p>
          ) : null}
          {notice ? (
            <p className="alert alert-success" role="status">
              {notice}
            </p>
          ) : null}
          {chains.isError || detail.isError ? (
            <p className="alert alert-warning" role="alert">
              Não foi possível consultar a cadeia selecionada.{" "}
              <button
                type="button"
                onClick={() => {
                  chains.refetch();
                  detail.refetch();
                }}
              >
                Atualizar leitura
              </button>
            </p>
          ) : null}
          {detail.data ? (
            <>
              {editing && detail.data.status === "DRAFT" ? (
                <BusinessProcessChainEditor
                  key={detail.data.id}
                  chain={detail.data}
                  saving={save.isPending}
                  onCancel={() => setEditing(false)}
                  onSave={(value) =>
                    run(async () => {
                      await save.mutateAsync({ id: detail.data!.id, value });
                      setEditing(false);
                      setNotice(
                        "Rascunho salvo. Revise a cadeia antes de publicar.",
                      );
                    })
                  }
                />
              ) : null}
              <section className="card card-body mb-3">
                <div className="d-flex flex-wrap justify-content-between gap-2 align-items-start">
                  <div>
                    <span className="badge text-bg-success">
                      {detail.data.status}
                    </span>
                    <h2 className="h4 mt-2 mb-2">
                      {detail.data.name} · v{detail.data.versionNumber}
                    </h2>
                    <p className="mb-0">{detail.data.purpose}</p>
                  </div>
                  <div className="d-flex flex-wrap gap-2">
                    {detail.data.status === "DRAFT" ? (
                      <>
                        <button
                          type="button"
                          className="btn btn-outline-primary"
                          disabled={busy || editing}
                          onClick={() => setEditing(true)}
                        >
                          Editar rascunho da cadeia
                        </button>
                        <button
                          type="button"
                          className="btn btn-success"
                          disabled={busy || editing}
                          onClick={() =>
                            run(async () => {
                              if (
                                !window.confirm(
                                  "Publicar esta versão da cadeia? As versões e execuções anteriores serão preservadas.",
                                )
                              )
                                return;
                              await publish.mutateAsync(detail.data!.id);
                              setNotice(
                                "Cadeia publicada. As execuções anteriores mantêm suas versões.",
                              );
                            })
                          }
                        >
                          {publish.isPending
                            ? "Publicando..."
                            : "Publicar cadeia"}
                        </button>
                      </>
                    ) : (
                      <button
                        type="button"
                        className="btn btn-outline-primary"
                        disabled={busy}
                        onClick={() =>
                          run(async () => {
                            const draft = await createDraft.mutateAsync(
                              detail.data!.id,
                            );
                            selectChain(draft.id);
                            setEditing(true);
                          })
                        }
                      >
                        {createDraft.isPending
                          ? "Criando rascunho..."
                          : "Criar nova versão da cadeia"}
                      </button>
                    )}
                  </div>
                </div>
                <div className="business-process-chain-summary mt-3">
                  <div>
                    <strong>Resultado da cadeia</strong>
                    <span>{detail.data.outcomeDescription}</span>
                  </div>
                  <div>
                    <strong>Métrica principal</strong>
                    <span>{detail.data.primaryMetric}</span>
                  </div>
                  <div>
                    <strong>Processos</strong>
                    <span>{detail.data.processes.length} em sequência</span>
                  </div>
                </div>
              </section>

              <section className="card card-body">
                <h2 className="h5 mb-1">Processos da cadeia</h2>
                <p className="small text-body-secondary mb-3">
                  Cada processo termina com um resultado verificável antes de
                  entregar valor ao próximo. No processo de venda, o aprendizado
                  orienta o retorno ao responsável pelo ajuste e a próxima
                  homologação.
                </p>
                {cycleCatalog.isError ? (
                  <p className="alert alert-warning" role="alert">
                    A integração do ciclo está indisponível. Atualize a leitura
                    para consultar os vínculos oficiais.
                  </p>
                ) : null}
                <ol className="business-process-chain-processes">
                  {detail.data.processes.map((process) => (
                    <li key={process.processDefinitionId}>
                      <div className="business-process-chain-order" aria-hidden>
                        {process.sequenceNumber}
                      </div>
                      <article className="business-process-chain-process">
                        <div className="d-flex flex-wrap justify-content-between gap-2">
                          <div>
                            <h3 className="h5 mb-1">{process.name}</h3>
                            <div className="small text-body-secondary">
                              {process.ownerName} · v{process.versionNumber}
                            </div>
                          </div>
                          <div className="d-flex flex-wrap align-items-center gap-2">
                            <span className="badge text-bg-success">
                              {process.status}
                            </span>
                            <Link
                              className="btn btn-sm btn-outline-primary"
                              to={`${
                                process.status === "RETIRED"
                                  ? "/business-processes/retired"
                                  : "/business-processes"
                              }?processId=${process.processDefinitionId}&chainId=${detail.data.id}${productId ? `&productId=${productId}` : ""}`}
                              aria-label={`Abrir atividades de ${process.name} no diagrama BPM`}
                            >
                              Abrir BPM
                            </Link>
                          </div>
                        </div>
                        <p className="mt-3 mb-3">{process.purpose}</p>
                        <div className="business-process-chain-process-grid">
                          <div>
                            <strong>Contribuição de valor</strong>
                            <span>{process.valueContribution}</span>
                          </div>
                          <div>
                            <strong>Entrada</strong>
                            <span>{process.triggerDescription}</span>
                          </div>
                          <div>
                            <strong>Resultado final</strong>
                            <span>{process.outcomeDescription}</span>
                          </div>
                        </div>
                        {cycleCatalog.data?.entry?.parentProcessDefinitionId ===
                        process.processDefinitionId ? (
                          <LearningCycleBlueprint
                            entry={cycleCatalog.data.entry}
                            diagram={cycleCatalog.data.diagram}
                            version={cycleCatalog.data.version}
                          />
                        ) : null}
                      </article>
                    </li>
                  ))}
                </ol>
              </section>
            </>
          ) : (
            <div className="card card-body text-body-secondary">
              {chains.isLoading || detail.isLoading
                ? "Carregando cadeia de processos..."
                : "Selecione uma cadeia para ver seus processos."}
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
