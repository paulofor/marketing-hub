import { AlertTriangle, CheckCircle2, Clock3, Loader2 } from "lucide-react";
import { useEffect, useRef } from "react";
import type { ProductProcessRecoveryTask } from "../../api/businessProcess/types";
import { formattedDateTime } from "../businessProcess/BusinessProcessExecutionPresentation";

export type ActivityExecutionFeedback = {
  error?: string;
  message?: string;
  taskIds?: number[];
};

const taskLabels: Record<string, string> = {
  PENDING: "Aguardando o agente",
  IN_PROGRESS: "Em execução",
  RUNNING: "Em execução",
  COMPLETED: "Concluída",
  BLOCKED: "Bloqueada",
  CANCELLED: "Cancelada",
  FAILED: "Falhou",
};

/** Mostra confirmação e andamento no local do clique, sempre a partir do retorno persistido. */
export default function ProductProcessTaskTracking({
  task,
  feedback,
  trackingError,
}: {
  task?: ProductProcessRecoveryTask | null;
  feedback?: ActivityExecutionFeedback;
  trackingError?: boolean;
}) {
  const confirmedTasks = feedback?.taskIds ?? [];
  const feedbackAnchor = useRef<HTMLDivElement>(null);
  const confirmationKey = confirmedTasks.join(",");
  useEffect(() => {
    if (feedback?.error || feedback?.message || confirmationKey) {
      feedbackAnchor.current?.scrollIntoView?.({ block: "nearest" });
    }
  }, [feedback?.error, feedback?.message, confirmationKey]);
  const awaitingRead =
    confirmedTasks.length > 0 && !confirmedTasks.includes(task?.taskId ?? -1);
  const active =
    task && ["PENDING", "IN_PROGRESS", "RUNNING"].includes(task.status);
  return (
    <div
      ref={feedbackAnchor}
      className="product-process-task-tracking"
      aria-label="Acompanhamento da tarefa"
    >
      {feedback?.error ? (
        <div className="alert alert-danger mb-2" role="alert">
          {feedback.error}
        </div>
      ) : null}
      {awaitingRead ? (
        <p className="alert alert-info mb-2" role="status">
          Tarefa{confirmedTasks.length > 1 ? "s" : ""}{" "}
          {confirmedTasks.map((id) => `#${id}`).join(", ")} registrada
          {confirmedTasks.length > 1 ? "s" : ""}. Consultando o andamento…
        </p>
      ) : feedback?.message && !task ? (
        <p className="alert alert-info mb-2" role="status">
          {feedback.message}
        </p>
      ) : null}
      {task && !awaitingRead ? (
        <div className="product-process-task-tracking__task">
          <p
            className="mb-1 d-flex align-items-center gap-2"
            role="status"
            aria-live="polite"
          >
            {active ? (
              <Loader2
                size={18}
                className="spinner-border spinner-border-sm"
                aria-hidden="true"
              />
            ) : task.status === "COMPLETED" ? (
              <CheckCircle2 size={18} aria-hidden="true" />
            ) : task.status === "BLOCKED" || task.status === "FAILED" ? (
              <AlertTriangle size={18} aria-hidden="true" />
            ) : (
              <Clock3 size={18} aria-hidden="true" />
            )}
            <strong>
              Tarefa #{task.taskId} · {taskLabels[task.status] ?? task.status}
            </strong>
          </p>
          <p className="mb-1">
            {task.agentName}
            {active ? " · O andamento será atualizado automaticamente." : ""}
          </p>
          <small className="d-block text-body-secondary">
            Criada: {formattedDateTime(task.createdAt)}
            {task.startedAt
              ? ` · Início: ${formattedDateTime(task.startedAt)}`
              : ""}
            {task.finishedAt
              ? ` · Fim: ${formattedDateTime(task.finishedAt)}`
              : ""}
          </small>
          {task.executionError || task.recommendedAction ? (
            <div className="alert alert-warning mt-2 mb-0">
              <strong>A atividade ainda não foi concluída.</strong>
              {task.recommendedAction ? (
                <p className="mb-1">{task.recommendedAction}</p>
              ) : null}
              {task.executionError ? (
                <details>
                  <summary>Ver motivo registrado pelo agente</summary>
                  <p className="mt-2 mb-0 text-break">{task.executionError}</p>
                </details>
              ) : null}
            </div>
          ) : null}
        </div>
      ) : null}
      {trackingError ? (
        <p className="alert alert-warning mt-2 mb-0" role="alert">
          Não foi possível atualizar o andamento. A última situação conhecida
          foi preservada; a consulta será repetida automaticamente.
        </p>
      ) : null}
    </div>
  );
}
