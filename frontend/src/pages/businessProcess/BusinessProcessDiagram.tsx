import type {
  BusinessProcessExecutionResource,
  ProcessDiagram,
  ProcessNode,
} from "../../api/businessProcess/types";
import { Link } from "react-router-dom";
import "./BusinessProcessesPage.css";

const label: Record<ProcessNode["type"], string> = {
  START: "Início",
  TASK: "Atividade",
  GATEWAY: "Decisão",
  END: "Fim",
};

export default function BusinessProcessDiagram({
  diagram,
  executionResources,
  processDefinitionId,
  documentActivityIds,
}: {
  diagram: ProcessDiagram;
  executionResources: BusinessProcessExecutionResource[];
  processDefinitionId: number;
  documentActivityIds: string[];
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
            {node.executionResourceCode ? (
              <div className="process-node__resource">
                Recurso obrigatório:{" "}
                {executionResources.find(
                  (item) => item.resourceCode === node.executionResourceCode,
                )?.name ?? node.executionResourceCode}
                {executionResources.find(
                  (item) => item.resourceCode === node.executionResourceCode,
                )?.executorReference
                  ? ` · executor ${
                      executionResources.find(
                        (item) =>
                          item.resourceCode === node.executionResourceCode,
                      )?.executorReference
                    }`
                  : ""}
              </div>
            ) : null}
            {node.description ? (
              <div className="process-node__objective">
                <p>
                  <strong>Objetivo:</strong> {node.description}
                </p>
                {node.type === "TASK" &&
                (node.documentOutput ||
                  documentActivityIds.includes(node.id)) ? (
                  <Link
                    className="process-node__documents-link"
                    to={`/business-processes/${processDefinitionId}/activities/${encodeURIComponent(node.id)}/documents`}
                  >
                    Ver os 10 últimos documentos
                    {node.documentOutput?.label
                      ? ` · ${node.documentOutput.label}`
                      : ""}
                  </Link>
                ) : null}
              </div>
            ) : null}
          </article>
        </div>
      ))}
    </div>
  );
}
