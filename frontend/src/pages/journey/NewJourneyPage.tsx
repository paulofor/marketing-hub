import { useNavigate } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import JourneyForm from "./JourneyForm";
import { useCreateJourney } from "../../api/journey/useCreateJourney";
import type { JourneyRequestPayload } from "../../api/journey/types";
import "./JourneyPageShell.css";

export default function NewJourneyPage() {
  const navigate = useNavigate();
  const createJourney = useCreateJourney();

  const handleSubmit = (payload: JourneyRequestPayload) => {
    createJourney.mutate(payload, {
      onSuccess: (journey) => {
        navigate(`/journeys/${journey.id}`);
      },
    });
  };

  return (
    <div className="journey-page-shell">
      <header className="journey-page-shell__header">
        <PageTitle>Criar nova jornada</PageTitle>
        <p>
          Estruture uma jornada personalizada a partir de um template existente,
          definindo público, janela de ativação e metadados operacionais.
        </p>
      </header>
      <JourneyForm
        submitLabel="Criar jornada"
        onSubmit={handleSubmit}
        isSubmitting={createJourney.isPending}
      />
    </div>
  );
}
