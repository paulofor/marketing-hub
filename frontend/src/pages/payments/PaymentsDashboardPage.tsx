import { useMemo, useState } from "react";
import {
  AlertCircle,
  BadgeCheck,
  CalendarClock,
  CheckCircle2,
  Clock3,
  CreditCard,
  History,
  Loader2,
  Mail,
  Receipt,
  TrendingUp,
  User,
} from "lucide-react";
import PageTitle from "../../components/PageTitle";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import { LeadPortalPayment, LeadPortalPaymentHistory, useLeadPortalPayments } from "../../api/leadPortal/useLeadPortalPayments";
import "./PaymentsDashboardPage.css";

type PaymentFilter = "all" | "pending" | "paid" | "failed";

const currencyFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
  minimumFractionDigits: 2,
});

function formatCurrency(value?: number | null, currency?: string | null) {
  if (value === null || value === undefined) {
    return "—";
  }
  if (currency && currency !== "BRL") {
    return `${currency} ${value.toFixed(2)}`;
  }
  return currencyFormatter.format(value);
}

function formatDate(value?: string | null) {
  if (!value) return "—";
  const date = new Date(value);
  return date.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

function normalizeStatus(status?: string | null) {
  return status?.toUpperCase() ?? "";
}

function statusLabel(status?: string | null) {
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

function statusBadgeTone(status?: string | null) {
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

function resolveCategory(payment: LeadPortalPayment): PaymentFilter {
  const normalized = normalizeStatus(payment.status);
  if (["APPROVED", "DELIVERED", "DELIVERING"].includes(normalized)) {
    return "paid";
  }
  if (["FAILED", "CANCELED", "CANCELLED"].includes(normalized)) {
    return "failed";
  }
  return "pending";
}

function buildHistory(payment: LeadPortalPayment) {
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

export default function PaymentsDashboardPage() {
  useBreadcrumbs([
    { label: "Financeiro" },
    { label: "Pagamentos" },
  ]);

  const [statusFilter, setStatusFilter] = useState<PaymentFilter>("all");
  const { data: payments = [], isLoading, error } = useLeadPortalPayments(80);

  const totals = useMemo(() => {
    return payments.reduce(
      (acc, payment) => {
        const category = resolveCategory(payment);
        const amount = payment.amount ?? 0;
        if (category === "paid") {
          acc.paid += 1;
          acc.paidAmount += amount;
        } else if (category === "failed") {
          acc.failed += 1;
        } else {
          acc.pending += 1;
          acc.pendingAmount += amount;
        }
        acc.totalAmount += amount;
        return acc;
      },
      { paid: 0, failed: 0, pending: 0, paidAmount: 0, pendingAmount: 0, totalAmount: 0 },
    );
  }, [payments]);

  const visiblePayments = useMemo(() => {
    if (statusFilter === "all") return payments;
    return payments.filter((payment) => resolveCategory(payment) === statusFilter);
  }, [payments, statusFilter]);

  return (
    <div className="payments-page">
      <PageTitle>Pagamentos</PageTitle>
      <p className="text-body-secondary">
        Acompanhe aqui tudo o que o lead-portal-payments está registrando: número
        da compra, status atual e o histórico vindo do Mercado Pago/webhook.
      </p>

      <div className="payments-toolbar" role="group" aria-label="Filtros de status">
        <div className="payments-toolbar__info">
          <div>
            <p className="payments-toolbar__title">Visão consolidada</p>
            <p className="payments-toolbar__hint">
              Dados reais do lead-portal-payments com o histórico de cada
              transação.
            </p>
          </div>
          <div className="payments-toolbar__legend" aria-label="Legenda de status">
            <span className="badge text-bg-warning">Pendente</span>
            <span className="badge text-bg-success">Pago/Entregue</span>
            <span className="badge text-bg-danger">Falha/Cancelado</span>
          </div>
        </div>
        <div className="payments-toolbar__actions">
          <button
            type="button"
            className={`payments-toolbar__pill${statusFilter === "all" ? " is-active" : ""}`}
            onClick={() => setStatusFilter("all")}
            aria-pressed={statusFilter === "all"}
          >
            <TrendingUp size={16} aria-hidden="true" />
            <span>Todos</span>
            <span className="payments-toolbar__counter">{payments.length}</span>
          </button>
          <button
            type="button"
            className={`payments-toolbar__pill${statusFilter === "pending" ? " is-active" : ""}`}
            onClick={() => setStatusFilter("pending")}
            aria-pressed={statusFilter === "pending"}
          >
            <Clock3 size={16} aria-hidden="true" />
            <span>Pendentes</span>
            <span className="payments-toolbar__counter">{totals.pending}</span>
          </button>
          <button
            type="button"
            className={`payments-toolbar__pill${statusFilter === "paid" ? " is-active" : ""}`}
            onClick={() => setStatusFilter("paid")}
            aria-pressed={statusFilter === "paid"}
          >
            <CheckCircle2 size={16} aria-hidden="true" />
            <span>Pagos/Entregues</span>
            <span className="payments-toolbar__counter">{totals.paid}</span>
          </button>
          <button
            type="button"
            className={`payments-toolbar__pill${statusFilter === "failed" ? " is-active" : ""}`}
            onClick={() => setStatusFilter("failed")}
            aria-pressed={statusFilter === "failed"}
          >
            <AlertCircle size={16} aria-hidden="true" />
            <span>Falha/Cancelado</span>
            <span className="payments-toolbar__counter">{totals.failed}</span>
          </button>
        </div>
      </div>

      <div className="payments-grid payments-grid--summary" role="list">
        <div className="payments-card payments-card--emphasis" role="listitem">
          <div className="payments-card__icon" aria-hidden="true">
            <Clock3 size={22} />
          </div>
          <div>
            <p className="payments-card__label">Pendentes</p>
            <p className="payments-card__value">{totals.pending}</p>
            <p className="payments-card__muted">
              {formatCurrency(totals.pendingAmount)} aguardando confirmação
            </p>
          </div>
        </div>
        <div className="payments-card" role="listitem">
          <div className="payments-card__icon" aria-hidden="true">
            <BadgeCheck size={22} />
          </div>
          <div>
            <p className="payments-card__label">Pagos/entregues</p>
            <p className="payments-card__value">{totals.paid}</p>
            <p className="payments-card__muted">
              {formatCurrency(totals.paidAmount)} já creditados
            </p>
          </div>
        </div>
        <div className="payments-card" role="listitem">
          <div className="payments-card__icon" aria-hidden="true">
            <CreditCard size={22} />
          </div>
          <div>
            <p className="payments-card__label">Volume processado</p>
            <p className="payments-card__value">{formatCurrency(totals.totalAmount)}</p>
            <p className="payments-card__muted">Inclui pendentes e confirmados</p>
          </div>
        </div>
        <div className="payments-card" role="listitem">
          <div className="payments-card__icon" aria-hidden="true">
            <Receipt size={22} />
          </div>
          <div>
            <p className="payments-card__label">Transações monitoradas</p>
            <p className="payments-card__value">{payments.length}</p>
            <p className="payments-card__muted">Atualiza a cada 30s</p>
          </div>
        </div>
      </div>

      <section aria-labelledby="payments-heading" className="payments-section">
        <div className="payments-section__header">
          <div>
            <p className="payments-section__eyebrow">Transações</p>
            <h2 id="payments-heading" className="payments-section__title">
              Últimos registros do lead-portal-payments
            </h2>
          </div>
          <span className="badge text-bg-secondary">{visiblePayments.length} encontrados</span>
        </div>

        {isLoading ? (
          <div className="payments-empty" role="status">
            <div className="payments-empty__icon" aria-hidden="true">
              <Loader2 size={24} className="spin" />
            </div>
            <div>
              <p className="payments-empty__title">Carregando pagamentos</p>
              <p className="payments-empty__hint">Buscando transações diretamente do serviço.</p>
            </div>
          </div>
        ) : error ? (
          <div className="payments-empty" role="alert">
            <div className="payments-empty__icon" aria-hidden="true">
              <AlertCircle size={24} />
            </div>
            <div>
              <p className="payments-empty__title">Não foi possível carregar</p>
              <p className="payments-empty__hint">{(error as Error).message}</p>
            </div>
          </div>
        ) : visiblePayments.length === 0 ? (
          <div className="payments-empty" role="status">
            <div className="payments-empty__icon" aria-hidden="true">
              <CalendarClock size={24} />
            </div>
            <div>
              <p className="payments-empty__title">Nenhuma transação neste filtro</p>
              <p className="payments-empty__hint">Aguarde webhooks ou altere o filtro.</p>
            </div>
          </div>
        ) : (
          <div className="payments-grid" role="list">
            {visiblePayments.map((payment) => {
              const category = resolveCategory(payment);
              const history = buildHistory(payment);
              const lastUpdate = history.length > 0 ? history[history.length - 1] : null;

              return (
                <article key={payment.id} className="payments-card" role="listitem">
                  <header className="payments-card__header">
                    <div
                      className={`payments-card__pill${category === "paid" ? " payments-card__pill--success" : ""}${category === "failed" ? " payments-card__pill--danger" : ""}`}
                    >
                      <Receipt size={16} aria-hidden="true" />
                      <span>Compra #{payment.id}</span>
                    </div>
                    <span className={`badge ${statusBadgeTone(payment.status)}`}>
                      {statusLabel(payment.status)}
                    </span>
                  </header>

                  <div className="payments-card__body">
                    <p className="payments-card__customer">
                      <User size={16} aria-hidden="true" />
                      <span>{payment.buyerName || payment.buyerEmail || "Sem comprador"}</span>
                    </p>
                    <p className="payments-card__amount">
                      {formatCurrency(payment.amount, payment.currency)}
                    </p>

                    <div className="payments-card__meta" aria-label="Detalhes principais">
                      <div className="payments-card__meta-item">
                        <CreditCard size={15} aria-hidden="true" />
                        <span>{payment.mercadoPagoStatus ? `MP: ${payment.mercadoPagoStatus}` : "Status pendente no MP"}</span>
                      </div>
                      <div className="payments-card__meta-item">
                        <CalendarClock size={15} aria-hidden="true" />
                        <span>Atualizado em {formatDate(lastUpdate?.at ?? payment.updatedAt)}</span>
                      </div>
                    </div>

                    <div className="payments-card__tags" aria-label="Referências">
                      <span className="badge text-bg-secondary">Pacote #{payment.packageId}</span>
                      {payment.submissionId && (
                        <span className="badge text-bg-light text-dark">Submission {payment.submissionId}</span>
                      )}
                      {payment.mercadoPagoPaymentId && (
                        <span className="badge text-bg-info">Payment ID {payment.mercadoPagoPaymentId}</span>
                      )}
                      {payment.mercadoPagoPreferenceId && (
                        <span className="badge text-bg-light text-dark">Pref {payment.mercadoPagoPreferenceId}</span>
                      )}
                    </div>
                  </div>

                  <footer className="payments-card__footer" aria-label="Contato e histórico">
                    <div className="payments-card__meta-item">
                      <Mail size={15} aria-hidden="true" />
                      <span>{payment.buyerEmail || "Sem e-mail informado"}</span>
                    </div>
                    <span className="payments-card__status-note">
                      Histórico de status abaixo
                    </span>
                  </footer>

                  <div className="payment-history" aria-label="Histórico do pagamento">
                    <div className="payment-history__title">
                      <History size={16} aria-hidden="true" />
                      <span>Histórico</span>
                    </div>
                    <ul className="payment-history__list">
                      {history.map((entry, index) => (
                        <PaymentHistoryItem key={`${entry.label}-${entry.at ?? index}`} entry={entry} />
                      ))}
                    </ul>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}

function PaymentHistoryItem({ entry }: { entry: LeadPortalPaymentHistory & { atDate?: Date | null } }) {
  return (
    <li className="payment-history__item">
      <span className="payment-history__dot" aria-hidden="true" />
      <div className="payment-history__content">
        <div className="payment-history__header">
          <span className="payment-history__label">{entry.label}</span>
          {entry.status && (
            <span className={`badge ${statusBadgeTone(entry.status)}`}>
              {statusLabel(entry.status)}
            </span>
          )}
        </div>
        <p className="payment-history__time">{formatDate(entry.at)}</p>
        {entry.detail && <p className="payment-history__detail">{entry.detail}</p>}
        <span className="payment-history__source">
          {entry.source === "webhook" ? "Webhook Mercado Pago" : "Sistema"}
        </span>
      </div>
    </li>
  );
}
