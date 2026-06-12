import { AlertCircle } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import OprmModuleNavigation from "./OprmModuleNavigation";
import { useOprmRoutineWorkspaceData } from "../../api/oprm/useOprmRoutineWorkspaceData";
import { buildRoutineValueBlocks } from "./oprmRoutineValueBlocks";

function readStringArray(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.filter((item): item is string => typeof item === "string");
}

function signalLabel(signal: Record<string, unknown>): string {
  const keys = [
    "painLabel",
    "outcomeLabel",
    "mechanismLabel",
    "painSummary",
    "outcomeSummary",
    "mechanismSummary",
  ];
  const found = keys.find((key) => typeof signal[key] === "string");
  return found ? String(signal[found]) : "Sinal sem rótulo";
}

export default function OprmRoutinePage() {
  const { occupationSeedRef } = useParams();
  const routineQuery = useOprmRoutineWorkspaceData(occupationSeedRef);

  const routineCard =
    routineQuery.data?.routineCardPayload &&
    typeof routineQuery.data.routineCardPayload === "object"
      ? routineQuery.data.routineCardPayload
      : null;

  const routineValueBlocks = buildRoutineValueBlocks(routineCard, [
    ...(routineQuery.data?.painSignals ?? []),
    ...(routineQuery.data?.desiredOutcomeSignals ?? []),
    ...(routineQuery.data?.mechanismOpportunitySignals ?? []),
  ]);
  const blocksWithSpecificItems = routineValueBlocks.filter(
    (block) => block.items.length > 0,
  ).length;

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <PageTitle>OPRM · Rotina</PageTitle>
        <p className="text-secondary mb-0">
          Visualize a rotina operacional por blocos de valor para identificar
          rapidamente dores, riscos e mecanismos que podem virar produto digital
          vendável.
        </p>
      </header>

      <OprmModuleNavigation occupationSeedRef={occupationSeedRef} />

      {routineQuery.isLoading ? (
        <div className="d-flex justify-content-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Carregando rotina OPRM...</span>
          </div>
        </div>
      ) : null}

      {routineQuery.isError ? (
        <div className="alert alert-danger d-flex gap-2 mb-0" role="alert">
          <AlertCircle size={18} className="mt-1" aria-hidden="true" />
          <div>
            <strong>Não foi possível carregar os sinais da rotina.</strong>
            <p className="mb-0">
              Valide se existem artefatos publicados para a ocupação
              selecionada.
            </p>
          </div>
        </div>
      ) : null}

      {!routineQuery.isLoading && !routineQuery.isError && !routineCard ? (
        <div className="alert alert-secondary mb-0" role="status">
          Nenhum artifact `occupationPersonaRoutineCard` foi encontrado para a
          ocupação.
        </div>
      ) : null}

      {!routineQuery.isLoading && !routineQuery.isError && routineCard ? (
        <>
          <section className="card border-0 shadow-sm">
            <div className="card-body d-flex flex-column gap-3">
              <div className="d-flex flex-column flex-lg-row justify-content-between gap-2">
                <div>
                  <h2 className="h5 mb-1">Leitura comercial da rotina</h2>
                  <p className="mb-0 text-secondary">
                    {String(
                      routineCard.routineSummary ?? "Resumo indisponível",
                    )}
                  </p>
                </div>
                <span className="badge text-bg-primary-subtle border border-primary-subtle text-primary align-self-lg-start">
                  {blocksWithSpecificItems}/{routineValueBlocks.length} blocos
                  com sinais
                </span>
              </div>
              <div className="alert alert-info mb-0" role="status">
                A prioridade é separar momentos da rotina para enxergar onde o
                profissional perde tempo, receita ou recorrência — e não repetir
                apenas tarefas genéricas como “gerenciar rotina e agenda”.
              </div>
            </div>
          </section>

          <section className="row g-3">
            {routineValueBlocks.map((block) => (
              <div className="col-12 col-xl-6" key={block.id}>
                <article className="card h-100 border-0 shadow-sm">
                  <div className="card-body d-flex flex-column gap-3">
                    <div>
                      <h3 className="h6 mb-1">{block.title}</h3>
                      <p className="text-secondary small mb-0">
                        {block.businessQuestion}
                      </p>
                    </div>
                    <ul className="list-group list-group-flush border rounded">
                      {block.items.length > 0 ? (
                        block.items.map((item, index) => (
                          <li
                            className="list-group-item"
                            key={`${block.id}-${index}`}
                          >
                            {item}
                          </li>
                        ))
                      ) : (
                        <li className="list-group-item text-secondary">
                          {block.emptyLabel}
                        </li>
                      )}
                    </ul>
                    <p className="small text-primary mb-0">
                      <span className="fw-semibold">Uso para produto:</span>{" "}
                      {block.opportunityHint}
                    </p>
                  </div>
                </article>
              </div>
            ))}
          </section>

          <section className="card border-0 shadow-sm">
            <div className="card-body d-flex flex-column gap-3">
              <h2 className="h5 mb-0">Seleções para oferta</h2>
              <div className="row g-3">
                <div className="col-12 col-lg-4">
                  <h3 className="h6">Dores</h3>
                  <ul className="list-group">
                    {routineQuery.data?.painSignals.length ? (
                      routineQuery.data.painSignals.map((signal, index) => (
                        <li className="list-group-item" key={`pain-${index}`}>
                          {signalLabel(signal)}
                        </li>
                      ))
                    ) : (
                      <li className="list-group-item">Sem dores derivadas.</li>
                    )}
                  </ul>
                </div>
                <div className="col-12 col-lg-4">
                  <h3 className="h6">Resultados desejados</h3>
                  <ul className="list-group">
                    {routineQuery.data?.desiredOutcomeSignals.length
                      ? routineQuery.data.desiredOutcomeSignals.map(
                          (signal, index) => (
                            <li
                              className="list-group-item"
                              key={`outcome-${index}`}
                            >
                              {signalLabel(signal)}
                            </li>
                          ),
                        )
                      : readStringArray(routineCard.desiredOutcomeSignals).map(
                          (item, index) => (
                            <li
                              className="list-group-item"
                              key={`outcome-fallback-${index}`}
                            >
                              {item}
                            </li>
                          ),
                        )}
                  </ul>
                </div>
                <div className="col-12 col-lg-4">
                  <h3 className="h6">Mecanismos candidatos</h3>
                  <ul className="list-group">
                    {routineQuery.data?.mechanismOpportunitySignals.length
                      ? routineQuery.data.mechanismOpportunitySignals.map(
                          (signal, index) => (
                            <li
                              className="list-group-item"
                              key={`mechanism-${index}`}
                            >
                              {signalLabel(signal)}
                            </li>
                          ),
                        )
                      : readStringArray(
                          routineCard.mechanismOpportunitySignals,
                        ).map((item, index) => (
                          <li
                            className="list-group-item"
                            key={`mechanism-fallback-${index}`}
                          >
                            {item}
                          </li>
                        ))}
                  </ul>
                </div>
              </div>
            </div>
          </section>

          <div>
            <Link
              to={`/oprm/offer/${encodeURIComponent(occupationSeedRef ?? "")}`}
              className="btn btn-primary"
            >
              Ir para oferta
            </Link>
          </div>
        </>
      ) : null}
    </div>
  );
}
