import { format } from "node:util";

const MAX_LINES = 2000;
const lines = [];

function write(level, ...values) {
  const line = `${new Date().toISOString()} level=${level} ${format(...values)}`;
  lines.push(line);
  if (lines.length > MAX_LINES) lines.splice(0, lines.length - MAX_LINES);
  const target =
    level === "ERROR"
      ? console.error
      : level === "WARN"
        ? console.warn
        : console.log;
  target(line);
}

/** Logger operacional em memória, sanitizado pelo chamador antes de registrar payloads. */
export const operationalLogger = {
  info: (...values) => write("INFO", ...values),
  warn: (...values) => write("WARN", ...values),
  error: (...values) => write("ERROR", ...values),
};

/** Retorna uma cópia estável das linhas recentes para o endpoint operacional. */
export function recentOperationalLogLines() {
  return [...lines];
}
