import type { JourneyStatus } from "../../api/journey/types";
import "./JourneyListPage.css";

const STATUS_LABELS: Record<JourneyStatus, string> = {
  DRAFT: "Rascunho",
  ACTIVE: "Ativa",
  PAUSED: "Pausada",
  COMPLETED: "Concluída",
  ARCHIVED: "Arquivada",
};

interface JourneyStatusBadgeProps {
  status: JourneyStatus;
}

export default function JourneyStatusBadge({ status }: JourneyStatusBadgeProps) {
  return (
    <span className={`journey-status-badge journey-status-badge--${status.toLowerCase()}`}>
      {STATUS_LABELS[status] ?? status}
    </span>
  );
}
