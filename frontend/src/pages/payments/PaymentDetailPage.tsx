import { useMemo } from "react";
import { Link, useParams } from "react-router-dom";
import {
  AlertCircle,
  ArrowLeft,
  CreditCard,
  History,
  Loader2,
  Mail,
  User,
} from "lucide-react";
import PageTitle from "../../components/PageTitle";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import { useLeadPortalPayments } from "../../api/leadPortal/useLeadPortalPayments";
import {
  PaymentHistoryEntry,
  buildHistory,
  formatCurrency,
  formatDate,
  resolveCategory,
  resolvePaymentTypeLabel,
  resolveRejectionLabel,
  statusBadgeTone,
  statusLabel,
} from "./paymentUtils";
import "./PaymentsDashboardPage.css";
import "./PaymentDetailPage.css";

export default function PaymentDetailPage() {
  const { id } = useParams();
  const paymentId = Number(id);
  const { data: payments = [], isLoading, error } = useLeadPortalPayments(200);

  const payment = useMemo(
    () => payments.find((item) => item.id === paymentId),
    [paymentId, payments],
  );

  const breadcrumbLabel = payment
    ? `Compra #${payment.id}`
    : Number.isNaN(paymentId)
      ? "Detalhe do pagamento"
      : `Compra #${paymentId}`;

  useBreadcrumbs([
    { label: "Financeiro" },
    { label: "Pagamentos", to: "/payments" },
    { label: breadcrumbLabel },
  ]);

  if (Number.isNaN(paymentId)) {
    return (
      <div className="payment-detail-page">
        <PageTitle>Pagamento não encontrado</PageTitle>
        <p className="text-body-secondary">
          O identificador informado não é válido. Volte para a lista de pagamentos
          para selecionar uma compra.
        </p>
        <Link className="payment-detail__back" to="/payments">
          <ArrowLeft size={16} aria-hidden="true" />
          <span>Ir para pagamentos</span>
        </Link>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="payment-detail-page" role="status">
        <div className="payment-detail__loading">
          <Loader2 className="spin" size={24} aria-hidden="true" />
          <div>
            <PageTitle>Carregando compra #{paymentId}</PageTitle>
            <p className="text-body-secondary mb-0">Buscando dados atualizados.</p>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="payment-detail-page" role="alert">
        <div className="payment-detail__empty">
          <AlertCircle size={22} aria-hidden="true" />
          <div>
            <PageTitle>Não foi possível carregar</PageTitle>
            <p className="text-body-secondary mb-0">{(error as Error).message}</p>
          </div>
        </div>
      </div>
    );
  }

  if (!payment) {
    return (
      <div className="payment-detail-page" role="status">
        <div className="payment-detail__empty">
          <AlertCircle size={22} aria-hidden="true" />
          <div>
            <PageTitle>Pagamento não localizado</PageTitle>
            <p className="text-body-secondary mb-2">
              Não encontramos a compra solicitada nesta lista recente. Volte para a
              página principal e selecione uma transação.
            </p>
            <Link className="payment-detail__back" to="/payments">
              <ArrowLeft size={16} aria-hidden="true" />
              <span>Voltar para pagamentos</span>
            </Link>
          </div>
        </div>
      </div>
    );
  }

  const history = buildHistory(payment);
  const paymentType = resolvePaymentTypeLabel(payment.paymentType, payment.paymentMethod);
  const rejectionLabel = resolveRejectionLabel(payment.rejectionReason);
  const isRejected = resolveCategory(payment) === "failed" || payment.mercadoPagoStatus?.toLowerCase() === "rejected";

  return (
    <div className="payment-detail-page">
      <div className="payment-detail__toolbar">
        <Link className="payment-detail__back" to="/payments">
          <ArrowLeft size={16} aria-hidden="true" />
          <span>Pagamentos</span>
        </Link>
        <span className={`badge ${statusBadgeTone(payment.status)}`}>
          {statusLabel(payment.status)}
        </span>
      </div>

      <PageTitle>Compra #{payment.id}</PageTitle>
      <p className="text-body-secondary">
        Visualize os detalhes completos da transação, incluindo dados do comprador,
        referências do Mercado Pago e o histórico de eventos recebidos.
      </p>

      <div className="payments-grid payments-grid--summary" role="list">
          <div className="payments-card payments-card--emphasis" role="listitem">
            <div className="payments-card__icon" aria-hidden="true">
              <CreditCard size={22} />
            </div>
            <div>
              <p className="payments-card__label">Valor da compra</p>
              <p className="payments-card__value">{formatCurrency(payment.amount, payment.currency)}</p>
              <p className="payments-card__muted">
                Status no Mercado Pago: {payment.mercadoPagoStatus || "pendente"}
              </p>
              <p className="payments-card__muted mb-0">
                {paymentType ? `Pagamento via ${paymentType}` : "Forma de pagamento não informada"}
              </p>
              {isRejected && rejectionLabel && (
                <p className="payments-card__muted text-danger fw-semibold mb-0">
                  Motivo da rejeição: {rejectionLabel}
                </p>
              )}
            </div>
          </div>
        <div className="payments-card" role="listitem">
          <div className="payments-card__icon" aria-hidden="true">
            <User size={22} />
          </div>
          <div>
            <p className="payments-card__label">Comprador</p>
            <p className="payments-card__value payments-card__value--small">
              {payment.buyerName || payment.buyerEmail || "Sem comprador"}
            </p>
            <p className="payments-card__muted">{payment.buyerEmail || "Sem e-mail informado"}</p>
          </div>
        </div>
      </div>

      <section className="payments-section payment-detail__section" aria-labelledby="payment-info-heading">
        <div className="payments-section__header">
          <div>
            <p className="payments-section__eyebrow">Resumo</p>
            <h2 id="payment-info-heading" className="payments-section__title">
              Informações principais
            </h2>
          </div>
          <span className="badge text-bg-secondary">Atualizado em {formatDate(payment.updatedAt)}</span>
        </div>

        <div className="payment-detail__info-grid">
          <div className="payment-detail__info-item">
            <span className="payment-detail__info-label">Compra</span>
            <span className="payment-detail__info-value">#{payment.id}</span>
          </div>
          <div className="payment-detail__info-item">
            <span className="payment-detail__info-label">Pacote</span>
            <span className="payment-detail__info-value">#{payment.packageId}</span>
          </div>
          {payment.submissionId && (
            <div className="payment-detail__info-item">
              <span className="payment-detail__info-label">Submission</span>
              <span className="payment-detail__info-value">{payment.submissionId}</span>
            </div>
          )}
          {payment.mercadoPagoPaymentId && (
            <div className="payment-detail__info-item">
              <span className="payment-detail__info-label">Payment ID</span>
              <span className="payment-detail__info-value">{payment.mercadoPagoPaymentId}</span>
            </div>
          )}
          {payment.mercadoPagoPreferenceId && (
            <div className="payment-detail__info-item">
              <span className="payment-detail__info-label">Preferência</span>
              <span className="payment-detail__info-value">{payment.mercadoPagoPreferenceId}</span>
            </div>
          )}
          {paymentType && (
            <div className="payment-detail__info-item">
              <span className="payment-detail__info-label">Tipo de pagamento</span>
              <span className="payment-detail__info-value">{paymentType}</span>
            </div>
          )}
          {isRejected && rejectionLabel && (
            <div className="payment-detail__info-item payment-detail__info-item--alert">
              <span className="payment-detail__info-label">Motivo da rejeição</span>
              <span className="payment-detail__info-value">{rejectionLabel}</span>
            </div>
          )}
          {payment.checkoutExpiresAt && (
            <div className="payment-detail__info-item">
              <span className="payment-detail__info-label">Checkout expira em</span>
              <span className="payment-detail__info-value">{formatDate(payment.checkoutExpiresAt)}</span>
            </div>
          )}
          {payment.paymentApprovedAt && (
            <div className="payment-detail__info-item">
              <span className="payment-detail__info-label">Pagamento aprovado</span>
              <span className="payment-detail__info-value">{formatDate(payment.paymentApprovedAt)}</span>
            </div>
          )}
          {payment.deliveredAt && (
            <div className="payment-detail__info-item">
              <span className="payment-detail__info-label">Entrega</span>
              <span className="payment-detail__info-value">{formatDate(payment.deliveredAt)}</span>
            </div>
          )}
          <div className="payment-detail__info-item">
            <span className="payment-detail__info-label">Criado em</span>
            <span className="payment-detail__info-value">{formatDate(payment.createdAt)}</span>
          </div>
        </div>
      </section>

      <section className="payments-section payment-detail__section" aria-labelledby="payment-history-heading">
        <div className="payments-section__header">
          <div>
            <p className="payments-section__eyebrow">Linha do tempo</p>
            <h2 id="payment-history-heading" className="payments-section__title">
              Histórico de eventos
            </h2>
          </div>
          <div className="payment-detail__history-meta">
            <Mail size={15} aria-hidden="true" />
            <span>{payment.buyerEmail || "Sem e-mail informado"}</span>
          </div>
        </div>

        {history.length === 0 ? (
          <div className="payment-detail__empty" role="status">
            <History size={20} aria-hidden="true" />
            <div>
              <p className="mb-0 fw-bold">Nenhum histórico recebido</p>
              <p className="text-body-secondary mb-0">O serviço ainda não registrou eventos para esta compra.</p>
            </div>
          </div>
        ) : (
          <ul className="payment-history__list">
            {history.map((entry, index) => (
              <PaymentHistoryItem key={`${entry.label}-${entry.at ?? index}`} entry={entry} />
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

function PaymentHistoryItem({ entry }: { entry: PaymentHistoryEntry }) {
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
