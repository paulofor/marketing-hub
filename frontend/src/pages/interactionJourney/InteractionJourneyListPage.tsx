import { Link } from "react-router-dom";
import { useDeleteInteractionJourney } from "../../api/interactionJourney/useDeleteInteractionJourney";
import { useInteractionJourneys } from "../../api/interactionJourney/useInteractionJourneys";
import PageTitle from "../../components/PageTitle";
import "./InteractionJourneyPage.css";

export default function InteractionJourneyListPage() {
  const { data, isLoading, isError } = useInteractionJourneys();
  const deleteJourney = useDeleteInteractionJourney();

  const onDelete = (id: number, name: string) => {
    if (!window.confirm(`Remover a jornada "${name}"?`)) return;
    deleteJourney.mutate(id);
  };

  return (
    <div className="interaction-journey-page">
      <div className="d-flex flex-column flex-lg-row justify-content-between align-items-start gap-3 mb-3">
        <div>
          <PageTitle>Jornadas de interação</PageTitle>
          <p className="text-muted mb-0">
            Organize passos com elementos e subelementos em um fluxo visual, separado da jornada tradicional.
          </p>
        </div>
        <Link to="/interaction-journeys/new" className="btn btn-primary">
          Nova jornada de interação
        </Link>
      </div>

      <section className="card shadow-sm border-0">
        <div className="card-body">
          {isLoading ? <p>Carregando jornadas...</p> : null}
          {isError ? (
            <p className="text-danger">Não foi possível carregar as jornadas.</p>
          ) : null}

          {!isLoading && !data?.length ? (
            <div className="interaction-empty">
              <p className="fw-semibold mb-1">Nenhuma jornada cadastrada</p>
              <p className="text-muted mb-0">Crie a primeira jornada de interação para visualizar aqui.</p>
            </div>
          ) : null}

          <div className="row g-3">
            {data?.map((journey) => {
              const updated = journey.updatedAt || journey.createdAt;
              const formattedDate = updated
                ? new Date(updated).toLocaleString("pt-BR")
                : "-";
              return (
                <div className="col-12 col-md-6 col-xl-4" key={journey.id ?? journey.name}>
                  <div className="interaction-card h-100">
                    <div className="d-flex align-items-start justify-content-between gap-2 mb-2">
                      <h2 className="h6 mb-0">{journey.name}</h2>
                      <span className="badge bg-info-subtle text-info-emphasis">
                        {journey.steps.length} passo{journey.steps.length === 1 ? "" : "s"}
                      </span>
                    </div>
                    <p className="text-muted small mb-2">
                      {journey.description || "Sem descrição"}
                    </p>
                    <div className="interaction-card__footer">
                      <small className="text-muted">Atualizado: {formattedDate}</small>
                      <div className="d-flex gap-2">
                        <Link
                          to={`/interaction-journeys/${journey.id}/edit`}
                          className="btn btn-outline-primary btn-sm"
                        >
                          Editar
                        </Link>
                        <button
                          type="button"
                          className="btn btn-outline-danger btn-sm"
                          onClick={() => journey.id && onDelete(Number(journey.id), journey.name)}
                          disabled={deleteJourney.isPending}
                        >
                          Excluir
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </section>
    </div>
  );
}
