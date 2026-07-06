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
  className?: string;
  maxHeight?: string;
  plainTextVariant?: "default" | "reading";
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
  if (
    !value.includes("\\n") &&
    !value.includes("\\r") &&
    !value.includes("\\t")
  ) {
    return value;
  }

  return value
    .replace(/\\r\\n/g, "\n")
    .replace(/\\n/g, "\n")
    .replace(/\\r/g, "\n")
    .replace(/\\t/g, "\t");
}

function formatJsonPrimitive(value: JsonValue) {
  if (value === null) return "null";
  if (typeof value === "string")
    return JSON.stringify(decodeEscapedText(value));
  return String(value);
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
  const fieldLabel = label ? (
    <span className="text-primary">{label}: </span>
  ) : null;

  if (value === null || typeof value !== "object") {
    return (
      <div className="font-monospace small py-1">
        {fieldLabel}
        <span className="text-break">{formatJsonPrimitive(value)}</span>
      </div>
    );
  }

  const isArray = Array.isArray(value);
  const entries = isArray
    ? value.map((item, index) => [String(index), item] as const)
    : Object.entries(value);
  const itemLabel = isArray
    ? `${entries.length} item(ns)`
    : `${entries.length} campo(s)`;
  const bracketOpen = isArray ? "[" : "{";
  const bracketClose = isArray ? "]" : "}";

  return (
    <details
      className="json-tree-node"
      open={initiallyCollapsed ? depth > 0 : depth < 2}
    >
      <summary
        className="font-monospace small py-1"
        style={{ cursor: "pointer" }}
      >
        {fieldLabel}
        <span>{bracketOpen}</span>
        <span className="text-muted ms-1">{itemLabel}</span>
        <span className="ms-1">{bracketClose}</span>
      </summary>
      <div className="border-start ps-3 ms-2">
        {entries.length === 0 ? (
          <div className="text-muted small py-1">Sem conteúdo.</div>
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
  className = "",
  maxHeight = "28rem",
  plainTextVariant = parseAsJson ? "default" : "reading",
}: CollapsibleJsonViewerProps) {
  const parsed = useMemo(
    () => (parseAsJson ? parseJson(content) : null),
    [content, parseAsJson],
  );

  if (!content) {
    return <p className="text-muted small mb-0">{emptyMessage}</p>;
  }

  if (!parsed) {
    const isReadingVariant = plainTextVariant === "reading";
    return (
      <pre
        className={`bg-body-tertiary border rounded mb-0 ${isReadingVariant ? "p-4" : "p-3 small"} ${className}`}
        style={{
          whiteSpace: "pre-wrap",
          overflowWrap: "anywhere",
          wordBreak: "break-word",
          maxHeight,
          overflow: "auto",
          fontSize: isReadingVariant ? "0.95rem" : undefined,
          lineHeight: isReadingVariant ? 1.65 : undefined,
        }}
      >
        {decodeEscapedText(content)}
      </pre>
    );
  }

  return (
    <div
      className={`bg-body-tertiary border rounded p-2 ${className}`}
      style={{ maxHeight, overflow: "auto" }}
    >
      <JsonNode value={parsed} initiallyCollapsed={initiallyCollapsed} />
    </div>
  );
}
