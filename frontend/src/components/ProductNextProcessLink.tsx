import { ArrowRight } from "lucide-react";
import { Link } from "react-router-dom";
import "./ProductNextProcessLink.css";

/** Abre o painel do processo oficial, preservando o contexto sem iniciar execução. */
export default function ProductNextProcessLink({
  processNumber,
  processName,
  activityNumber,
  activityName,
  responsible,
  state,
  reason,
  url,
  cycleProcess = false,
}: {
  processNumber?: string | number | null;
  processName: string;
  activityNumber?: number | null;
  activityName?: string;
  responsible?: string | null;
  state?: string;
  reason?: string | null;
  url: string;
  cycleProcess?: boolean;
}) {
  const blocked = state === "BLOCKED";
  const running = state === "IN_PROGRESS" || state === "PENDING";
  const number =
    processNumber != null && activityNumber != null
      ? `${processNumber}.${activityNumber}`
      : null;

  return (
    <div
      className={`product-next-process${blocked ? " product-next-process--blocked" : ""}`}
    >
      <span className="product-next-process__label">
        {cycleProcess ? "Processo do ciclo" : "Próximo processo"}
      </span>
      <strong className="product-next-process__title">
        Processo {processNumber != null ? `${processNumber} — ` : ""}
        {processName}
      </strong>
      <Link
        className="btn btn-primary product-next-process__link"
        to={`${url.split("#")[0]}#process-execution`}
      >
        {cycleProcess ? "Abrir processo do ciclo" : "Abrir próximo processo"}
        <ArrowRight size={18} aria-hidden="true" />
      </Link>
      <small>Execute ou acompanhe as atividades no painel do processo.</small>
      {activityName ? (
        <>
          <small>
            {blocked
              ? "Atividade com pendência"
              : running
                ? "Atividade em andamento"
                : "Próxima atividade"}
            : {number ? `${number} — ` : ""}
            {activityName}
          </small>
          <small>Responsável: {responsible || "Não informado"}</small>
        </>
      ) : null}
      {reason && blocked ? (
        <p>{reason}</p>
      ) : reason ? (
        <details className="product-next-process__reason">
          <summary>Situação da atividade</summary>
          <p>{reason}</p>
        </details>
      ) : null}
    </div>
  );
}
