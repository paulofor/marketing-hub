import { useMemo } from "react";
import { Link, useSearchParams } from "react-router-dom";
import {
  useBusinessProcessChain,
  useBusinessProcessChains,
} from "../../api/businessProcessChain/useBusinessProcessChains";
import PageTitle from "../../components/PageTitle";
import "./BusinessProcessChainsPage.css";

export default function BusinessProcessChainsPage() {
  const chains = useBusinessProcessChains();
  const [searchParams, setSearchParams] = useSearchParams();
  const requestedId = Number(searchParams.get("chainId"));
  const selectedId =
    Number.isSafeInteger(requestedId) && requestedId > 0
      ? requestedId
      : undefined;
  const activeId = useMemo(
    () =>
      selectedId ??
      chains.data?.find((item) => item.status === "PUBLISHED")?.id ??
      chains.data?.[0]?.id,
    [chains.data, selectedId],
  );
  const detail = useBusinessProcessChain(activeId);

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
                onClick={() => {
                  const next = new URLSearchParams(searchParams);
                  next.set("chainId", String(chain.id));
                  setSearchParams(next, { replace: true });
                }}
              >
                <span className="d-block fw-semibold">{chain.name}</span>
                <span className="small">
                  v{chain.versionNumber} · {chain.processCount} processos
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
          {detail.data ? (
            <>
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
                  entregar valor ao próximo.
                </p>
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
                              to={`/business-processes?processId=${process.processDefinitionId}`}
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
