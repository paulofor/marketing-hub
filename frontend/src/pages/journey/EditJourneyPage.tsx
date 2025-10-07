import { Link, useNavigate, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import JourneyForm from "./JourneyForm";
import { useJourney } from "../../api/journey/useJourney";
import { useUpdateJourney } from "../../api/journey/useUpdateJourney";
import type { JourneyRequestPayload } from "../../api/journey/types";
import "./JourneyPageShell.css";
import "./JourneyDetailPage.css";

export default function EditJourneyPage() {
  const params = useParams<{ id: string }>();
  const journeyId = Number(params.id);
  const navigate = useNavigate();
  const { data: journey, isLoading } = useJourney(Number.isNaN(journeyId) ? undefined : journeyId);
  const updateJourney = useUpdateJourney(journeyId);

  const handleSubmit = (payload: JourneyRequestPayload) => {
    updateJourney.mutate(payload, {
      onSuccess: () => {
        navigate(`/journeys/${journeyId}`);
      },
    });
  };

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

  return (
    <div className="journey-page-shell">
      <header className="journey-page-shell__header">
        <PageTitle>Editar jornada</PageTitle>
        <p>Atualize metas, metadados e segmentação para manter a jornada alinhada ao plano atual.</p>
      </header>
      <JourneyForm
        initialJourney={journey}
        submitLabel="Salvar alterações"
        onSubmit={handleSubmit}
        isSubmitting={updateJourney.isPending}
        onCancel={() => navigate(`/journeys/${journeyId}`)}
      />
    </div>
  );
}
