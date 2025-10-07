import { useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useJourney } from "../../api/journey/useJourney";
import JourneyStatusBadge from "./JourneyStatusBadge";
import { useDeleteJourney } from "../../api/journey/useDeleteJourney";
import "./JourneyDetailPage.css";

function formatDateTime(value?: string | null) {
  if (!value) {
    return "—";
  }
  try {
    return new Intl.DateTimeFormat("pt-BR", {
      dateStyle: "long",
      timeStyle: "short",
    }).format(new Date(value));
  } catch (error) {
    return value;
  }
}

export default function JourneyDetailPage() {
  const params = useParams<{ id: string }>();
  const navigate = useNavigate();
  const journeyId = Number(params.id);
  const { data: journey, isLoading } = useJourney(Number.isNaN(journeyId) ? undefined : journeyId);
  const deleteJourney = useDeleteJourney(journeyId);
  const [isConfirmOpen, setIsConfirmOpen] = useState(false);

  const metadataEntries = useMemo(
    () => Object.entries(journey?.metadata ?? {}),
    [journey?.metadata],
  );

  if (isLoading) {
    return <div className="journey-detail__loading">Carregando jornada...</div>;
  }

  if (!journey) {
    return (
      <div className="journey-detail__loading">
        Jornada não encontrada.
        <div>
          <Link to="/journeys" className="btn btn-link">
            Voltar para jornadas
          </Link>
        </div>
      </div>
    );
  }

  const handleDelete = async () => {
    await deleteJourney.mutateAsync();
    navigate("/journeys");
  };

  return (
    <div className="journey-detail">
      <header className="journey-detail__header">
        <div>
          <PageTitle>{journey.name}</PageTitle>
          {journey.description ? (
            <p className="journey-detail__subtitle">{journey.description}</p>
          ) : null}
          <div className="journey-detail__status">
            <JourneyStatusBadge status={journey.status} />
            <span className="journey-detail__status-meta">
              Atualizada em {formatDateTime(journey.updatedAt)}
            </span>
          </div>
        </div>
        <div className="journey-detail__actions">
          <Link className="btn btn-secondary" to={`/journeys/${journey.id}/edit`}>
            Editar jornada
          </Link>
          <button
            type="button"
            className="btn btn-outline-danger"
            onClick={() => setIsConfirmOpen(true)}
            disabled={deleteJourney.isPending}
          >
            {deleteJourney.isPending ? (
              <>
                <span
                  className="spinner-border spinner-border-sm me-2"
                  role="status"
                  aria-hidden="true"
                />
                Removendo...
              </>
            ) : (
              "Excluir"
            )}
          </button>
        </div>
      </header>

      <section className="journey-detail__grid">
        <article className="journey-detail__card">
          <h2>Resumo</h2>
          <dl>
            <div>
              <dt>Template</dt>
              <dd>{journey.templateName}</dd>
            </div>
            <div>
              <dt>Janela</dt>
              <dd>
                {formatDateTime(journey.startAt)}
                <span className="journey-detail__arrow">→</span>
                {formatDateTime(journey.endAt)}
              </dd>
            </div>
            <div>
              <dt>Criado em</dt>
              <dd>{formatDateTime(journey.createdAt)}</dd>
            </div>
          </dl>
        </article>

        <article className="journey-detail__card">
          <h2>Segmentação</h2>
          <dl>
            <div>
              <dt>Referência externa</dt>
              <dd>{journey.segmentReference ?? "—"}</dd>
            </div>
            <div>
              <dt>Filtro</dt>
              <dd>{journey.segmentFilter ?? "—"}</dd>
            </div>
            <div>
              <dt>Nicho de mercado</dt>
              <dd>{journey.marketNicheId ?? "—"}</dd>
            </div>
            <div>
              <dt>Experimento</dt>
              <dd>{journey.experimentId ?? "—"}</dd>
            </div>
          </dl>
        </article>

        <article className="journey-detail__card journey-detail__card--full">
          <h2>Metadados</h2>
          {metadataEntries.length ? (
            <ul className="journey-detail__metadata">
              {metadataEntries.map(([key, value]) => (
                <li key={key}>
                  <span className="journey-detail__metadata-key">{key}</span>
                  <span className="journey-detail__metadata-value">{value || "—"}</span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="journey-detail__empty">Nenhum metadado cadastrado.</p>
          )}
        </article>
      </section>

      {isConfirmOpen ? (
        <div className="journey-detail__modal" role="dialog" aria-modal="true">
          <div className="journey-detail__modal-content">
            <h3>Confirmar exclusão</h3>
            <p>
              Esta ação removerá a jornada "{journey.name}" e todo o histórico associado.
              Tem certeza de que deseja continuar?
            </p>
            <div className="journey-detail__modal-actions">
              <button
                type="button"
                className="btn btn-outline-secondary"
                onClick={() => setIsConfirmOpen(false)}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="btn btn-danger"
                onClick={handleDelete}
                disabled={deleteJourney.isPending}
              >
                {deleteJourney.isPending ? (
                  <>
                    <span
                      className="spinner-border spinner-border-sm me-2"
                      role="status"
                      aria-hidden="true"
                    />
                    Removendo...
                  </>
                ) : (
                  "Excluir"
                )}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
