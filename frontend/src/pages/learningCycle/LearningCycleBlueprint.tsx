import { Link } from "react-router-dom";
import type { LearningCycleEntry } from "../../api/learningCycle/useLearningCycles";
import type { ProcessDiagram } from "../../api/businessProcess/types";
import LearningCycleDiagram from "./LearningCycleDiagram";
import "./LearningCyclesPage.css";

export default function LearningCycleBlueprint({
  entry,
  diagram,
  version,
}: {
  entry: LearningCycleEntry;
  diagram?: ProcessDiagram;
  version?: number;
}) {
  return (
    <section
      className="card card-body mt-3 cycle-placement"
      aria-label="Ciclo dentro do processo de venda"
    >
      <span className="small text-body-secondary">
        Subprocesso do processo {entry.sequenceNumber} ·{" "}
        {entry.parentProcessName}
      </span>
      <h4 className="h5 mt-2">{entry.processName}</h4>
      <p>
        Cada ciclo é uma execução deste subprocesso, vinculada a um experimento.
        Os resultados orientam a próxima melhoria de produto, comunicação ou
        investimento.
      </p>
      <p className={entry.integrated ? "" : "alert alert-warning"}>
        {entry.guidance}
      </p>
      <Link
        className="btn btn-outline-primary align-self-start"
        to={entry.workspaceUrl}
      >
        {entry.actionLabel}
      </Link>
      {entry.returnRoutes.length > 0 ? (
        <details className="mt-3">
          <summary>Decisão comercial: para onde o fluxo retorna?</summary>
          <p className="small mt-2">
            Registre a decisão no ciclo. Os destinos abaixo explicam onde
            executar o ajuste; abrir um destino não autoriza publicação ou
            gasto.
          </p>
          <ul className="cycle-return-routes">
            {entry.returnRoutes.map((route) => (
              <li key={route.processDefinitionId}>
                <strong>{route.label}</strong>
                <p>{route.condition}</p>
                <Link to={route.url}>
                  {route.sequenceNumber}. {route.processName}
                </Link>
              </li>
            ))}
          </ul>
          <p className="small mb-0">
            Encerrar ou concluir como inconclusivo preserva o experimento e o
            aprendizado.
          </p>
        </details>
      ) : null}
      {diagram ? (
        <details className="mt-3">
          <summary>Ver BPM com decisões e retornos · v{version}</summary>
          <LearningCycleDiagram diagram={diagram} />
        </details>
      ) : null}
    </section>
  );
}
