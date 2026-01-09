import { Link, useNavigate, useParams } from "react-router-dom";
import type { InteractionJourney } from "../../api/interactionJourney/types";
import { useInteractionJourney } from "../../api/interactionJourney/useInteractionJourney";
import { useSaveInteractionJourney } from "../../api/interactionJourney/useSaveInteractionJourney";
import InteractionJourneyBuilder from "../../components/InteractionJourneyBuilder";
import PageTitle from "../../components/PageTitle";
import "./InteractionJourneyPage.css";

export default function EditInteractionJourneyPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data, isLoading, isError } = useInteractionJourney(id);
  const saveJourney = useSaveInteractionJourney();

  const handleSubmit = (payload: InteractionJourney) => {
    saveJourney.mutate({ ...payload, id: Number(id) }, {
      onSuccess: () => navigate("/interaction-journeys"),
    });
  };

  return (
    <div className="interaction-journey-page">
      <div className="d-flex flex-column flex-lg-row justify-content-between align-items-start gap-3 mb-3">
        <div>
          <PageTitle>Editar jornada de interação</PageTitle>
          <p className="text-muted mb-0">
            Ajuste passos, elementos e visualização gráfica conforme o fluxo evolui.
          </p>
        </div>
        <Link to="/interaction-journeys" className="btn btn-outline-secondary">
          Voltar
        </Link>
      </div>

      {isLoading ? <p>Carregando jornada...</p> : null}
      {isError ? <p className="text-danger">Não foi possível carregar a jornada.</p> : null}
      {!data && !isLoading ? <p className="text-muted">Jornada não encontrada.</p> : null}

      {data ? (
        <InteractionJourneyBuilder
          initialJourney={data}
          onSubmit={handleSubmit}
          isSubmitting={saveJourney.isPending}
        />
      ) : null}
    </div>
  );
}
