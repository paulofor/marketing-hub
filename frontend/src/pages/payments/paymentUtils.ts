import { LeadPortalPayment, LeadPortalPaymentHistory } from "../../api/leadPortal/useLeadPortalPayments";

const currencyFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
  minimumFractionDigits: 2,
});

export function formatCurrency(value?: number | null, currency?: string | null) {
  if (value === null || value === undefined) {
    return "—";
  }
  if (currency && currency !== "BRL") {
    return `${currency} ${value.toFixed(2)}`;
  }
  return currencyFormatter.format(value);
}

export function formatDate(value?: string | null) {
  if (!value) return "—";
  const date = new Date(value);
  return date.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

export function normalizeStatus(status?: string | null) {
  return status?.toUpperCase() ?? "";
}

export function statusLabel(status?: string | null) {
  switch (normalizeStatus(status)) {
    case "PREFERENCE_CREATED":
      return "Preferência criada";
    case "PENDING_PAYMENT":
      return "Pagamento pendente";
    case "APPROVED":
      return "Aprovado";
    case "DELIVERING":
      return "Entregando";
    case "DELIVERED":
      return "Entregue";
    case "FAILED":
      return "Falhou";
    case "CANCELED":
    case "CANCELLED":
      return "Cancelado";
    default:
      return status ?? "—";
  }
}

export function statusBadgeTone(status?: string | null) {
  const normalized = normalizeStatus(status);
  if (["APPROVED", "DELIVERED", "DELIVERING"].includes(normalized)) {
    return "text-bg-success";
  }
  if (["FAILED", "CANCELED", "CANCELLED"].includes(normalized)) {
    return "text-bg-danger";
  }
  if (["PREFERENCE_CREATED", "PENDING", "PENDING_PAYMENT"].includes(normalized)) {
    return "text-bg-warning";
  }
  return "text-bg-secondary";
}

export function resolveCategory(payment: LeadPortalPayment) {
  const normalized = normalizeStatus(payment.status);
  if (["APPROVED", "DELIVERED", "DELIVERING"].includes(normalized)) {
    return "paid" as const;
  }
  if (["FAILED", "CANCELED", "CANCELLED"].includes(normalized)) {
    return "failed" as const;
  }
  return "pending" as const;
}

export type PaymentHistoryEntry = LeadPortalPaymentHistory & { atDate?: Date | null };

export function buildHistory(payment: LeadPortalPayment): PaymentHistoryEntry[] {
  const history = [...(payment.history || [])];
  return history
    .map((item) => ({
      ...item,
      atDate: item.at ? new Date(item.at) : null,
    }))
    .sort((a, b) => {
      if (a.atDate && b.atDate) return a.atDate.getTime() - b.atDate.getTime();
      if (a.atDate) return -1;
      if (b.atDate) return 1;
      return 0;
    });
}
