import type {
  ProcessDiagram,
  ProcessNode,
} from "../../api/businessProcess/types";
import "./BusinessProcessesPage.css";

const label: Record<ProcessNode["type"], string> = {
  START: "Início",
  TASK: "Atividade",
  GATEWAY: "Decisão",
  END: "Fim",
};

export default function BusinessProcessDiagram({
  diagram,
}: {
  diagram: ProcessDiagram;
}) {
  const incoming = new Map<string, typeof diagram.flows>();
  diagram.flows.forEach((flow) =>
    incoming.set(flow.to, [...(incoming.get(flow.to) ?? []), flow]),
  );

  return (
    <div className="process-diagram" aria-label="Diagrama BPM do processo">
      {diagram.nodes.map((node, index) => (
        <div className="process-diagram__row" key={node.id}>
          {index > 0 ? (
            <div
              className="process-diagram__incoming"
              aria-label="Fluxos de entrada"
            >
              {(incoming.get(node.id) ?? []).map((flow) => (
                <span key={`${flow.from}-${flow.to}`}>
                  <span aria-hidden="true">↓</span> {flow.label || ""}
                </span>
              ))}
            </div>
          ) : null}
          <article
            className={`process-node process-node--${node.type.toLowerCase()}`}
          >
            <span className="process-node__type">{label[node.type]}</span>
            <h3>{node.label}</h3>
            {node.owner ? (
              <div className="process-node__owner">
                Responsável: {node.owner}
              </div>
            ) : null}
            {node.description ? <p>{node.description}</p> : null}
          </article>
        </div>
      ))}
    </div>
  );
}
