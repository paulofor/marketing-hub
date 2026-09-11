import { ArrowRight } from "lucide-react";
import { Link } from "react-router-dom";
import "./ProductNextActivityLink.css";

/** Destaca o acesso à atividade escolhida pelo backend, sem iniciar sua execução. */
export default function ProductNextActivityLink({
  processNumber,
  processName,
  activityNumber,
  activityName,
  responsible,
  state,
  reason,
  url,
}: {
  processNumber?: string | number | null;
  processName: string;
  activityNumber?: number | null;
  activityName: string;
  responsible?: string | null;
  state: string;
  reason?: string | null;
  url: string;
}) {
  const blocked = state === "BLOCKED";
  const running = state === "IN_PROGRESS" || state === "PENDING";
  const number =
    processNumber != null && activityNumber != null
      ? `${processNumber}.${activityNumber}`
      : null;

  return (
    <div
      className={`product-next-activity${blocked ? " product-next-activity--blocked" : ""}`}
    >
      <span className="product-next-activity__label">
        {blocked
          ? "Atividade com pendência"
          : running
            ? "Atividade em andamento"
            : "Próxima atividade"}
      </span>
      <strong className="product-next-activity__title">
        {number ? `${number} — ` : ""}
        {activityName}
      </strong>
      <small>
        Processo {processNumber != null ? `${processNumber} — ` : ""}
        {processName}
      </small>
      <small>Responsável: {responsible || "Não informado"}</small>
      <Link className="btn btn-primary product-next-activity__link" to={url}>
        {blocked
          ? "Ver atividade e pendência"
          : running
            ? "Acompanhar atividade"
            : "Abrir próxima atividade"}
        <ArrowRight size={18} aria-hidden="true" />
      </Link>
      {reason && blocked ? (
        <p>{reason}</p>
      ) : reason ? (
        <details className="product-next-activity__reason">
          <summary>
            {blocked ? "Motivo da pendência" : "Situação da atividade"}
          </summary>
          <p>{reason}</p>
        </details>
      ) : null}
    </div>
  );
}
