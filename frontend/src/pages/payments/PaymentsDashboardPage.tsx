import { useMemo, useState } from "react";
import {
  BadgeCheck,
  CalendarClock,
  CheckCircle2,
  Clock3,
  CreditCard,
  Mail,
  Receipt,
  TrendingUp,
  User,
} from "lucide-react";
import PageTitle from "../../components/PageTitle";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import "./PaymentsDashboardPage.css";

type PaymentStatus = "pending" | "completed";

type Payment = {
  id: string;
  customer: string;
  amount: number;
  status: PaymentStatus;
  method: string;
  dueDate: string;
  paidAt?: string;
  description: string;
  reference: string;
  contact: string;
  lastUpdate: string;
};

const PAYMENTS: Payment[] = [
  {
    id: "PAG-10234",
    customer: "Estúdio Norte",
    amount: 1420,
    status: "pending",
    method: "Cartão de crédito",
    dueDate: "2024-08-12",
    description: "Plano Performance — ciclo mensal",
    reference: "Funil Black Friday",
    contact: "financeiro@estudionorte.com",
    lastUpdate: "2024-08-08 às 10:20",
  },
  {
    id: "PAG-10235",
    customer: "Mercato Café",
    amount: 890,
    status: "pending",
    method: "Boleto bancário",
    dueDate: "2024-08-10",
    description: "Pacote de criativos premium",
    reference: "Journey - Lançamento cold audience",
    contact: "pagamentos@mercatocafe.com",
    lastUpdate: "2024-08-08 às 09:12",
  },
  {
    id: "PAG-10236",
    customer: "Academia Pulse",
    amount: 1320,
    status: "completed",
    method: "Pix",
    dueDate: "2024-08-05",
    paidAt: "2024-08-04",
    description: "Gestão de anúncios — trimestre",
    reference: "CPA Otimizado",
    contact: "financeiro@pulsestudio.fit",
    lastUpdate: "2024-08-05 às 18:45",
  },
  {
    id: "PAG-10237",
    customer: "AgroVale",
    amount: 640,
    status: "completed",
    method: "Cartão de crédito",
    dueDate: "2024-08-03",
    paidAt: "2024-08-02",
    description: "Implantação de chatbot",
    reference: "Journey - Retenção pós-captura",
    contact: "contato@agrovale.com.br",
    lastUpdate: "2024-08-02 às 16:10",
  },
  {
    id: "PAG-10238",
    customer: "Clínica Vitta",
    amount: 980,
    status: "pending",
    method: "Pix",
    dueDate: "2024-08-11",
    description: "Setup de automações e onboarding",
    reference: "Portal de leads — Série Welcome",
    contact: "financeiro@clinicavitta.com",
    lastUpdate: "2024-08-08 às 08:55",
  },
];

const currencyFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
  minimumFractionDigits: 2,
});

function formatCurrency(value: number) {
  return currencyFormatter.format(value);
}

function formatDateLabel(payment: Payment) {
  if (payment.status === "completed" && payment.paidAt) {
    return `Pago em ${payment.paidAt}`;
  }

  return `Vence em ${payment.dueDate}`;
}

export default function PaymentsDashboardPage() {
  useBreadcrumbs([
    { label: "Financeiro" },
    { label: "Pagamentos" },
  ]);

  const [statusFilter, setStatusFilter] = useState<PaymentStatus | "all">("all");

  const totals = useMemo(() => {
    const pending = PAYMENTS.filter((payment) => payment.status === "pending");
    const completed = PAYMENTS.filter((payment) => payment.status === "completed");
    const received = completed.reduce((sum, payment) => sum + payment.amount, 0);

    return {
      pendingCount: pending.length,
      completedCount: completed.length,
      pendingTotal: pending.reduce((sum, payment) => sum + payment.amount, 0),
      received,
      overall: PAYMENTS.reduce((sum, payment) => sum + payment.amount, 0),
    };
  }, []);

  const visiblePayments = useMemo(() => {
    if (statusFilter === "all") {
      return PAYMENTS;
    }

    return PAYMENTS.filter((payment) => payment.status === statusFilter);
  }, [statusFilter]);

  const pendingPayments = visiblePayments.filter(
    (payment) => payment.status === "pending",
  );
  const completedPayments = visiblePayments.filter(
    (payment) => payment.status === "completed",
  );

  return (
    <div className="payments-page">
      <PageTitle>Pagamentos</PageTitle>
      <p className="text-body-secondary">
        Identifique rapidamente o que já foi recebido e o que ainda depende de
        ação. Use este painel para alinhar com o financeiro e priorizar
        cobranças antes que impactem as campanhas ativas.
      </p>

      <div className="payments-toolbar" role="group" aria-label="Filtros de status">
        <div className="payments-toolbar__info">
          <div>
            <p className="payments-toolbar__title">Visão consolidada</p>
            <p className="payments-toolbar__hint">
              Totais agrupados por status e referências chave das campanhas.
            </p>
          </div>
          <div className="payments-toolbar__legend" aria-label="Legenda de status">
            <span className="badge text-bg-warning">Pendente</span>
            <span className="badge text-bg-success">Pago</span>
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
          </button>
          <button
            type="button"
            className={`payments-toolbar__pill${statusFilter === "pending" ? " is-active" : ""}`}
            onClick={() => setStatusFilter("pending")}
            aria-pressed={statusFilter === "pending"}
          >
            <Clock3 size={16} aria-hidden="true" />
            <span>Somente pendentes</span>
            <span className="payments-toolbar__counter">{totals.pendingCount}</span>
          </button>
          <button
            type="button"
            className={`payments-toolbar__pill${statusFilter === "completed" ? " is-active" : ""}`}
            onClick={() => setStatusFilter("completed")}
            aria-pressed={statusFilter === "completed"}
          >
            <CheckCircle2 size={16} aria-hidden="true" />
            <span>Somente pagos</span>
            <span className="payments-toolbar__counter">{totals.completedCount}</span>
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
            <p className="payments-card__value">{totals.pendingCount}</p>
            <p className="payments-card__muted">
              {formatCurrency(totals.pendingTotal)} aguardando confirmação
            </p>
          </div>
        </div>
        <div className="payments-card" role="listitem">
          <div className="payments-card__icon" aria-hidden="true">
            <BadgeCheck size={22} />
          </div>
          <div>
            <p className="payments-card__label">Pagos</p>
            <p className="payments-card__value">{totals.completedCount}</p>
            <p className="payments-card__muted">
              {formatCurrency(totals.received)} já creditados
            </p>
          </div>
        </div>
        <div className="payments-card" role="listitem">
          <div className="payments-card__icon" aria-hidden="true">
            <CreditCard size={22} />
          </div>
          <div>
            <p className="payments-card__label">Volume processado</p>
            <p className="payments-card__value">{formatCurrency(totals.overall)}</p>
            <p className="payments-card__muted">Inclui pendentes e confirmados</p>
          </div>
        </div>
        <div className="payments-card" role="listitem">
          <div className="payments-card__icon" aria-hidden="true">
            <Receipt size={22} />
          </div>
          <div>
            <p className="payments-card__label">Prioridade</p>
            <p className="payments-card__value">{totals.pendingCount} cobranças</p>
            <p className="payments-card__muted">Reforce contato até a data limite</p>
          </div>
        </div>
      </div>

      <section aria-labelledby="payments-pending-heading" className="payments-section">
        <div className="payments-section__header">
          <div>
            <p className="payments-section__eyebrow">Fila de cobrança</p>
            <h2 id="payments-pending-heading" className="payments-section__title">
              Pagamentos pendentes
            </h2>
          </div>
          <span className="badge text-bg-warning">
            {pendingPayments.length} pendente(s)
          </span>
        </div>

        {pendingPayments.length === 0 ? (
          <div className="payments-empty" role="status">
            <div className="payments-empty__icon" aria-hidden="true">
              <CalendarClock size={24} />
            </div>
            <div>
              <p className="payments-empty__title">Sem pendências agora</p>
              <p className="payments-empty__hint">
                Ótimo trabalho! Continue monitorando para evitar atrasos nas
                campanhas.
              </p>
            </div>
          </div>
        ) : (
          <div className="payments-grid" role="list">
            {pendingPayments.map((payment) => (
              <article key={payment.id} className="payments-card" role="listitem">
                <header className="payments-card__header">
                  <div className="payments-card__pill">
                    <Clock3 size={16} aria-hidden="true" />
                    <span>{payment.id}</span>
                  </div>
                  <span className="badge text-bg-warning">Pendente</span>
                </header>
                <div className="payments-card__body">
                  <p className="payments-card__customer">
                    <User size={16} aria-hidden="true" />
                    <span>{payment.customer}</span>
                  </p>
                  <p className="payments-card__amount">{formatCurrency(payment.amount)}</p>
                  <p className="payments-card__muted">{payment.description}</p>
                  <div className="payments-card__meta">
                    <div className="payments-card__meta-item">
                      <CreditCard size={15} aria-hidden="true" />
                      <span>{payment.method}</span>
                    </div>
                    <div className="payments-card__meta-item">
                      <CalendarClock size={15} aria-hidden="true" />
                      <span>{formatDateLabel(payment)}</span>
                    </div>
                  </div>
                  <div className="payments-card__tags" aria-label="Referências">
                    <span className="badge text-bg-secondary">{payment.reference}</span>
                    <span className="badge text-bg-light text-dark">
                      Última atualização: {payment.lastUpdate}
                    </span>
                  </div>
                </div>
                <footer className="payments-card__footer" aria-label="Contato sugerido">
                  <div className="payments-card__meta-item">
                    <Mail size={15} aria-hidden="true" />
                    <span>{payment.contact}</span>
                  </div>
                  <button type="button" className="btn btn-outline-primary btn-sm">
                    Enviar lembrete
                  </button>
                </footer>
              </article>
            ))}
          </div>
        )}
      </section>

      <section
        aria-labelledby="payments-completed-heading"
        className="payments-section"
      >
        <div className="payments-section__header">
          <div>
            <p className="payments-section__eyebrow">Confirmados</p>
            <h2
              id="payments-completed-heading"
              className="payments-section__title"
            >
              Pagamentos realizados
            </h2>
          </div>
          <span className="badge text-bg-success">
            {completedPayments.length} pagamento(s)
          </span>
        </div>

        {completedPayments.length === 0 ? (
          <div className="payments-empty" role="status">
            <div className="payments-empty__icon" aria-hidden="true">
              <CheckCircle2 size={24} />
            </div>
            <div>
              <p className="payments-empty__title">Nenhum pagamento confirmado</p>
              <p className="payments-empty__hint">
                Ainda não há registros de pagamentos finalizados neste filtro.
              </p>
            </div>
          </div>
        ) : (
          <div className="payments-grid" role="list">
            {completedPayments.map((payment) => (
              <article key={payment.id} className="payments-card" role="listitem">
                <header className="payments-card__header">
                  <div className="payments-card__pill payments-card__pill--success">
                    <CheckCircle2 size={16} aria-hidden="true" />
                    <span>{payment.id}</span>
                  </div>
                  <span className="badge text-bg-success">Pago</span>
                </header>
                <div className="payments-card__body">
                  <p className="payments-card__customer">
                    <User size={16} aria-hidden="true" />
                    <span>{payment.customer}</span>
                  </p>
                  <p className="payments-card__amount">{formatCurrency(payment.amount)}</p>
                  <p className="payments-card__muted">{payment.description}</p>
                  <div className="payments-card__meta">
                    <div className="payments-card__meta-item">
                      <CreditCard size={15} aria-hidden="true" />
                      <span>{payment.method}</span>
                    </div>
                    <div className="payments-card__meta-item">
                      <CalendarClock size={15} aria-hidden="true" />
                      <span>{formatDateLabel(payment)}</span>
                    </div>
                  </div>
                  <div className="payments-card__tags" aria-label="Referências">
                    <span className="badge text-bg-secondary">{payment.reference}</span>
                    <span className="badge text-bg-light text-dark">
                      Última atualização: {payment.lastUpdate}
                    </span>
                  </div>
                </div>
                <footer className="payments-card__footer" aria-label="Contato sugerido">
                  <div className="payments-card__meta-item">
                    <Mail size={15} aria-hidden="true" />
                    <span>{payment.contact}</span>
                  </div>
                  <span className="payments-card__status-note">Pago em {payment.paidAt}</span>
                </footer>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
