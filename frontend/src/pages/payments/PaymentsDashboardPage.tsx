import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  AlertCircle,
  BadgeCheck,
  CalendarClock,
  CheckCircle2,
  Clock3,
  CreditCard,
  Loader2,
  Mail,
  Receipt,
  TrendingUp,
  User,
} from "lucide-react";
import PageTitle from "../../components/PageTitle";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import { useLeadPortalPayments } from "../../api/leadPortal/useLeadPortalPayments";
import {
  buildHistory,
  formatCurrency,
  formatDate,
  resolveCategory,
  statusBadgeTone,
  statusLabel,
} from "./paymentUtils";
import "./PaymentsDashboardPage.css";

type PaymentFilter = "all" | "pending" | "paid" | "failed";

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
        Acompanhe aqui o status geral de cada compra. Para ver histórico,
        webhooks e demais detalhes, abra a página dedicada de cada transação.
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
                    <div className="payments-card__row">
                      <p className="payments-card__amount">
                        {formatCurrency(payment.amount, payment.currency)}
                      </p>
                      <p className="payments-card__customer">
                        <User size={16} aria-hidden="true" />
                        <span>{payment.buyerName || payment.buyerEmail || "Sem comprador"}</span>
                      </p>
                    </div>

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
                    </div>
                  </div>

                  <footer className="payments-card__footer" aria-label="Contato e detalhes">
                    <div className="payments-card__meta-item">
                      <Mail size={15} aria-hidden="true" />
                      <span>{payment.buyerEmail || "Sem e-mail informado"}</span>
                    </div>
                    <Link
                      to={`/payments/${payment.id}`}
                      className="payments-card__link"
                      aria-label={`Ver detalhes da compra ${payment.id}`}
                    >
                      Ver detalhes
                    </Link>
                  </footer>
                </article>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}

