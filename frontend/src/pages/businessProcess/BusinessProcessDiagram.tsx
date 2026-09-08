import type {
  BusinessProcessReference,
  BusinessProcessExecutionResource,
  ProcessDiagram,
  ProcessNode,
} from "../../api/businessProcess/types";
import BusinessProcessEntityName from "../../components/BusinessProcessEntityName";
import { Link, useSearchParams } from "react-router-dom";
import { useId } from "react";
import { Diamond } from "lucide-react";
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
  subprocesses,
}: {
  diagram: ProcessDiagram;
  executionResources: BusinessProcessExecutionResource[];
  processDefinitionId: number;
  documentActivityIds: string[];
  subprocesses: BusinessProcessReference[];
}) {
  const [params] = useSearchParams();
  const chainId = params.get("chainId");
  const productId = params.get("productId");
  const prefix = useId().replace(/:/g, "");
  const incoming = new Map<string, typeof diagram.flows>();
  diagram.flows.forEach((flow) =>
    incoming.set(flow.to, [...(incoming.get(flow.to) ?? []), flow]),
  );

  return (
    <div className="process-diagram" aria-label="Diagrama BPM do processo">
      {diagram.nodes.map((node, index) => {
        const subprocess = subprocesses.find(
          (item) => item.processCode === node.subprocessCode,
        );
        return (
          <div className="process-diagram__row" key={node.id}>
            {index > 0 ? (
              <div
                className="process-diagram__incoming"
                aria-label="Fluxos de entrada"
              >
                {(incoming.get(node.id) ?? []).map((flow, flowIndex) => (
                  <span key={`${flow.from}-${flow.to}-${flowIndex}`}>
                    <span aria-hidden="true">↓</span> {flow.label || ""}
                  </span>
                ))}
              </div>
            ) : null}
            <article
              id={`${prefix}-${node.id}`}
              className={`process-node process-node--${node.type.toLowerCase()}`}
            >
              <span className="process-node__type">{label[node.type]}</span>
              <h3>
                {node.type === "GATEWAY" ? (
                  <Diamond
                    size={30}
                    role="img"
                    aria-label="Losango de decisão"
                    className="me-2"
                  />
                ) : null}
                {node.type === "TASK" ? (
                  <BusinessProcessEntityName
                    kind="activity"
                    name={node.label}
                  />
                ) : (
                  node.label
                )}
              </h3>
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
              {node.subprocessCode ? (
                <div className="process-node__subprocess">
                  <span>Delega para o subprocesso</span>
                  {subprocess ? (
                    <Link
                      to={`/business-processes?processId=${subprocess.id}${chainId ? `&chainId=${encodeURIComponent(chainId)}` : ""}${productId ? `&productId=${encodeURIComponent(productId)}` : ""}`}
                      aria-label={`Abrir subprocesso ${subprocess.name}`}
                    >
                      <BusinessProcessEntityName
                        kind="process"
                        name={`${subprocess.name} · v${subprocess.versionNumber} →`}
                        iconSize={16}
                      />
                    </Link>
                  ) : (
                    <strong>{node.subprocessCode}</strong>
                  )}
                  <small>Código: {node.subprocessCode}</small>
                </div>
              ) : null}
              {node.description || node.type === "TASK" ? (
                <div className="process-node__objective">
                  {node.description ? (
                    <p>
                      <strong>Objetivo:</strong> {node.description}
                    </p>
                  ) : null}
                  {node.type === "TASK" ? (
                    <Link
                      className="process-node__executions-link"
                      to={`/business-processes/${processDefinitionId}/activities/${encodeURIComponent(node.id)}/executions`}
                    >
                      Ver as 10 execuções mais recentes
                    </Link>
                  ) : null}
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
              {node.type === "GATEWAY" ? (
                <nav
                  className="cycle-outgoing"
                  aria-label={`Decisões de ${node.label}`}
                >
                  {diagram.flows
                    .filter((flow) => flow.from === node.id)
                    .map((flow, flowIndex) => (
                      <a
                        key={`${flow.to}-${flowIndex}`}
                        href={`#${prefix}-${flow.to}`}
                      >
                        {flow.kind === "REWORK" ? "↩ " : "→ "}
                        {flow.label ||
                          diagram.nodes.find((target) => target.id === flow.to)
                            ?.label}
                      </a>
                    ))}
                </nav>
              ) : null}
            </article>
          </div>
        );
      })}
    </div>
  );
}
