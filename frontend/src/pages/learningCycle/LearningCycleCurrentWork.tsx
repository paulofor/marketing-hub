import { Link } from "react-router-dom";
import type { LearningCycle } from "../../api/learningCycle/useLearningCycles";

/** Destaca a dependência real do ciclo sem transformar acompanhamento em nova decisão. */
export default function LearningCycleCurrentWork({
  cycle,
}: {
  cycle: LearningCycle;
}) {
  const work = cycle.delegatedWork;
  if (!work || cycle.status !== "OPEN") return null;
  return (
    <section className="alert alert-info" aria-label="Trabalho atual do ciclo">
      <h3 className="h5">
        {work.state === "IN_PROGRESS"
          ? "Atividade em andamento"
          : "Preparação pendente"}
      </h3>
      <p className="fw-bold">
        {work.processNumber}.{work.activityNumber} — {work.activityName}
      </p>
      <p>{work.reason}</p>
      <p>Responsável pela atividade: {work.responsible}</p>
      <p>
        A etapa será concluída quando esta atividade comprovar sua entrega.
        Acompanhe o processo para ver o bloqueio e as decisões necessárias.
      </p>
      <Link to={work.url} className="btn btn-primary">
        {work.state === "IN_PROGRESS"
          ? "Acompanhar atividade"
          : "Ver atividade e pendência"}
      </Link>
    </section>
  );
}
