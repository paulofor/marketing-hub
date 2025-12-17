const LEAD_PORTAL_TIME_ZONE = "America/Sao_Paulo";

const leadPortalDateTimeFormatter = new Intl.DateTimeFormat("pt-BR", {
  dateStyle: "short",
  timeStyle: "medium",
  timeZone: LEAD_PORTAL_TIME_ZONE,
});

export function formatLeadPortalDateTime(value?: string | null) {
  if (!value) return "--";
  try {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return "--";
    }
    return leadPortalDateTimeFormatter.format(date);
  } catch {
    return "--";
  }
}

export { LEAD_PORTAL_TIME_ZONE };
