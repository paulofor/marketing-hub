const leadPortalDateTimeFormatter = new Intl.DateTimeFormat("pt-BR", {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
  hour12: false,
});

export function parseLeadPortalDateTime(value?: string | null): number | null {
  if (!value) return null;
  const timestamp = Date.parse(value.trim());
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
