import { useNavigate } from "react-router-dom";
import type { InteractionJourney } from "../../api/interactionJourney/types";
import { useSaveInteractionJourney } from "../../api/interactionJourney/useSaveInteractionJourney";
import InteractionJourneyBuilder from "../../components/InteractionJourneyBuilder";
import PageTitle from "../../components/PageTitle";
import "./InteractionJourneyPage.css";

export default function NewInteractionJourneyPage() {
  const navigate = useNavigate();
  const saveJourney = useSaveInteractionJourney();

  const handleSubmit = (payload: InteractionJourney) => {
    saveJourney.mutate(payload, {
      onSuccess: () => navigate("/interaction-journeys"),
    });
  };

  return (
    <div className="interaction-journey-page">
      <div className="d-flex flex-column flex-lg-row justify-content-between align-items-start gap-3 mb-3">
        <div>
          <PageTitle>Nova jornada de interação</PageTitle>
          <p className="text-muted mb-0">
            Construa os passos, elementos e subelementos que compõem o fluxo.
          </p>
        </div>
      </div>
      <InteractionJourneyBuilder
        onSubmit={handleSubmit}
        isSubmitting={saveJourney.isPending}
      />
    </div>
  );
}
