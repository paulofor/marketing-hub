import { type ReactNode } from "react";

/** Formata a data vinda do backend sem alterar sua semântica operacional. */
export function formattedDateTime(value?: string) {
  if (!value) return "Não informado";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Não informado";
  return date.toLocaleString("pt-BR");
}

/** Calcula uma duração legível somente quando o backend informou os dois marcos da execução. */
export function formattedDuration(startedAt?: string, finishedAt?: string) {
  if (!startedAt || !finishedAt) return "Não informado";
  const durationMs =
    new Date(finishedAt).getTime() - new Date(startedAt).getTime();
  if (!Number.isFinite(durationMs) || durationMs < 0) return "Não informado";
  const seconds = Math.floor(durationMs / 1000);
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const remainingSeconds = seconds % 60;
  return [
    hours ? `${hours}h` : null,
    minutes ? `${minutes}min` : null,
    `${remainingSeconds}s`,
  ]
    .filter(Boolean)
    .join(" ");
}

/** Renderiza valores simples de um JSON na árvore expansível. */
function JsonValue({ value }: { value: unknown }) {
  if (value === null)
    return <span className="business-process-json-tree__value">nulo</span>;
  if (typeof value === "string") {
    return (
      <span className="business-process-json-tree__value">
        &quot;{value}&quot;
      </span>
    );
  }
  return (
    <span className="business-process-json-tree__value">{String(value)}</span>
  );
}

/** Exibe objetos e listas JSON de forma progressiva, sem esconder o conteúdo original. */
function JsonTree({
  value,
  level = 0,
}: {
  value: unknown;
  level?: number;
}): ReactNode {
  if (value === null || typeof value !== "object")
    return <JsonValue value={value} />;
  const entries = Array.isArray(value)
    ? value.map((item, index) => [String(index), item] as const)
    : Object.entries(value);
  return (
    <details className="business-process-json-tree" open={level === 0}>
      <summary>
        {Array.isArray(value)
          ? `Lista (${entries.length})`
          : `Objeto (${entries.length})`}
      </summary>
      <ul>
        {entries.map(([key, nestedValue]) => (
          <li key={key}>
            <strong>{key}:</strong>{" "}
            <JsonTree value={nestedValue} level={level + 1} />
          </li>
        ))}
      </ul>
    </details>
  );
}

/** Mostra JSON como árvore sob demanda e preserva texto puro das execuções legadas. */
export function StructuredExecutionContent({
  value,
  emptyText = "Não informado.",
}: {
  value?: string;
  emptyText?: string;
}) {
  if (!value) return <span className="text-body-secondary">{emptyText}</span>;
  try {
    const parsed = JSON.parse(value);
    return (
      <details className="business-process-json-viewer">
        <summary>Visualizar JSON em árvore</summary>
        <div className="business-process-json-viewer__tree">
          <JsonTree value={parsed} />
        </div>
      </details>
    );
  } catch {
    return <pre className="business-process-document__content">{value}</pre>;
  }
}
