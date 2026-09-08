import { useId } from "react";
import type { ProcessDiagram } from "../../api/businessProcess/types";
import "./LearningCyclesPage.css";

export default function LearningCycleDiagram({
  diagram,
  currentStage,
}: {
  diagram: ProcessDiagram;
  currentStage?: string;
}) {
  const prefix = useId().replace(/:/g, "");
  return (
    <ol
      className="learning-cycle-diagram"
      aria-label="BPM de ciclos de aprendizado e vendas"
    >
      {diagram.nodes.map((node) => (
        <li
          id={`${prefix}-${node.id}`}
          key={node.id}
          className={`cycle-node cycle-node--${node.type.toLowerCase()}`}
          aria-current={node.id === currentStage ? "step" : undefined}
        >
          <div className="cycle-node-heading">
            {node.type === "GATEWAY" ? (
              <svg
                className="cycle-diamond"
                viewBox="0 0 42 42"
                role="img"
                aria-label="Losango de decisão"
              >
                <path d="M21 1 L41 21 L21 41 L1 21 Z" />
              </svg>
            ) : null}
            <strong>{node.label}</strong>
            {node.id === currentStage ? (
              <span className="badge text-bg-primary">Etapa atual</span>
            ) : null}
          </div>
          {node.owner ? <small>{node.owner}</small> : null}
          {node.description ? <p>{node.description}</p> : null}
          <div
            className="cycle-outgoing"
            aria-label={`Saídas de ${node.label}`}
          >
            {diagram.flows
              .filter((flow) => flow.from === node.id)
              .map((flow, index) => (
                <a
                  key={`${flow.to}-${index}`}
                  className={flow.kind === "REWORK" ? "cycle-return" : ""}
                  href={`#${prefix}-${flow.to}`}
                >
                  <span aria-hidden>{flow.kind === "REWORK" ? "↩" : "↓"}</span>{" "}
                  {flow.label ||
                    diagram.nodes.find((target) => target.id === flow.to)
                      ?.label}
                </a>
              ))}
          </div>
        </li>
      ))}
    </ol>
  );
}
