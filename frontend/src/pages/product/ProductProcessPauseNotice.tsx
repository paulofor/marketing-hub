import { Link } from "react-router-dom";
import type { ProcessAutomation } from "../../api/businessProcess/useProcessAutomation";

/** Repete na atividade a pausa oficial do processo e a ação necessária, sem deduzir sua causa. */
export default function ProductProcessPauseNotice({
  execution,
  unavailable = false,
}: {
  execution?: ProcessAutomation;
  unavailable?: boolean;
}) {
  if (unavailable)
    return (
      <p className="alert alert-warning" role="alert">
        Não foi possível atualizar o motivo da pausa. A execução não foi
        retomada; atualize a consulta no início da página.
      </p>
    );
  if (!execution || !["PAUSED", "PAUSING"].includes(execution.status))
    return null;
  return (
    <section
      className="alert alert-warning"
      aria-label="Por que esta atividade não continua"
    >
      <strong>
        {execution.userAction?.title ?? "O processo está pausado"}
      </strong>
      <p className="mt-2 mb-2">
        {execution.userAction?.reason ?? execution.reason}
      </p>
      {execution.userAction ? (
        <>
          <p className="mb-2">
            <strong>Sua ação:</strong> {execution.userAction.actionLabel}.
          </p>
          <p className="mb-2">
            Responsável: {execution.userAction.responsible}
          </p>
          <Link
            className="btn btn-outline-primary text-wrap"
            to={execution.userAction.actionUrl}
          >
            {execution.userAction.actionLabel}
          </Link>
          <p className="mt-2 mb-0">{execution.userAction.afterAction}</p>
        </>
      ) : (
        <p className="mb-0">
          A atividade aguarda a retomada do processo no início desta página.
          Nenhuma nova tarefa será iniciada enquanto ele estiver pausado.
        </p>
      )}
    </section>
  );
}
