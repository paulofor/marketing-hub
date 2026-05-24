import { useMemo } from "react";

type JsonValue =
  | null
  | boolean
  | number
  | string
  | JsonValue[]
  | { [key: string]: JsonValue };

type CollapsibleJsonViewerProps = {
  content?: string | null;
  emptyMessage?: string;
  initiallyCollapsed?: boolean;
  parseAsJson?: boolean;
};

function parseJson(content?: string | null): JsonValue | null {
  if (!content) return null;
  try {
    return JSON.parse(content) as JsonValue;
  } catch {
    return null;
  }
}

function decodeEscapedText(value: string): string {
  if (!value.includes("\\n") && !value.includes("\\r") && !value.includes("\\t")) {
    return value;
  }

  return value
    .replace(/\\r\\n/g, "\n")
    .replace(/\\n/g, "\n")
    .replace(/\\r/g, "\n")
    .replace(/\\t/g, "\t");
}

function JsonNode({
  value,
  label,
  depth = 0,
  initiallyCollapsed = false,
}: {
  value: JsonValue;
  label?: string;
  depth?: number;
  initiallyCollapsed?: boolean;
}) {
  const spacing = { marginLeft: depth === 0 ? 0 : 12 };

  if (value === null || typeof value !== "object") {
    const isString = typeof value === "string";

    return (
      <div style={spacing} className="small">
        {label ? <strong>{label}: </strong> : null}
        {isString ? (
          <pre
            className="mb-0 d-inline"
            style={{ whiteSpace: "pre-wrap", wordBreak: "break-word" }}
          >
            {decodeEscapedText(value)}
          </pre>
        ) : (
          <code>{JSON.stringify(value)}</code>
        )}
      </div>
    );
  }

  const isArray = Array.isArray(value);
  const entries = isArray
    ? value.map((item, index) => [String(index), item] as const)
    : Object.entries(value);

  return (
    <details open={initiallyCollapsed ? depth > 0 : depth < 1} style={spacing}>
      <summary className="small" style={{ cursor: "pointer" }}>
        {label ? (
          <strong>{label}</strong>
        ) : (
          <strong>{isArray ? "Array" : "Object"}</strong>
        )}{" "}
        ({entries.length})
      </summary>
      <div className="mt-1 d-flex flex-column gap-1">
        {entries.length === 0 ? (
          <div className="small text-muted">{isArray ? "[]" : "{}"}</div>
        ) : (
          entries.map(([key, child]) => (
            <JsonNode
              key={`${depth}-${key}`}
              value={child}
              label={key}
              depth={depth + 1}
              initiallyCollapsed={initiallyCollapsed}
            />
          ))
        )}
      </div>
    </details>
  );
}

export default function CollapsibleJsonViewer({
  content,
  emptyMessage = "Sem conteúdo registrado.",
  initiallyCollapsed = false,
  parseAsJson = true,
}: CollapsibleJsonViewerProps) {
  const parsed = useMemo(() => (parseAsJson ? parseJson(content) : null), [content, parseAsJson]);

  if (!content) {
    return <p className="text-muted small mb-0">{emptyMessage}</p>;
  }

  if (!parsed) {
    return (
      <pre
        className="bg-body-tertiary p-3 rounded small mb-0"
        style={{ whiteSpace: "pre-wrap", overflowWrap: "anywhere", wordBreak: "break-word" }}
      >
        {decodeEscapedText(content)}
      </pre>
    );
  }

  return (
    <div className="bg-body-tertiary rounded p-2" style={{ overflowX: "auto" }}>
      <JsonNode value={parsed} initiallyCollapsed={initiallyCollapsed} />
    </div>
  );
}
