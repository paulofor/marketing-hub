const LEAD_PORTAL_TIME_ZONE = "America/Sao_Paulo";

const leadPortalDateTimeFormatter = new Intl.DateTimeFormat("pt-BR", {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
  hour12: false,
  timeZone: LEAD_PORTAL_TIME_ZONE,
  timeZoneName: "short",
});

function normalizeDateInput(value?: string | null) {
  if (!value) return null;
  const trimmed = value.trim();

  // Detect ISO-like strings that omit the timezone (e.g., "2025-05-23 10:30:00").
  const isIsoWithoutTimezone =
    /^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}/.test(trimmed) &&
    !/[zZ]|[+-]\d{2}:?\d{2}$/.test(trimmed);

  if (isIsoWithoutTimezone) {
    // Ensure we treat the value as UTC to avoid discrepancies when the browser assumes local time.
    const withTSeparator = trimmed.replace(" ", "T");
    return `${withTSeparator}Z`;
  }

  return trimmed;
}

export function parseLeadPortalDateTime(value?: string | null): number | null {
  const normalized = normalizeDateInput(value);
  if (!normalized) return null;
  const timestamp = Date.parse(normalized);
  return Number.isNaN(timestamp) ? null : timestamp;
}

export function formatLeadPortalDateTime(value?: string | null) {
  const timestamp = parseLeadPortalDateTime(value);
  if (timestamp === null) return "--";

  try {
    return leadPortalDateTimeFormatter.format(new Date(timestamp));
  } catch {
    return "--";
  }
}

export { LEAD_PORTAL_TIME_ZONE };
