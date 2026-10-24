import { Link } from "react-router-dom";
import {
  ArrowRight,
  CheckCircle2,
  HelpCircle,
  Clock3,
  Loader2,
  PauseCircle,
  Play,
  RefreshCw,
  ShieldAlert,
  User,
} from "lucide-react";
import PageTitle from "../../components/PageTitle";
import { useProductsInPlay } from "../../api/product/useProductsInPlay";
import "./ProductsInPlayPage.css";

const statusAppearance: Record<string, { tone: string; icon: typeof Play }> = {
  EXECUTING: { tone: "running", icon: Loader2 },
  QUEUED: { tone: "waiting", icon: Clock3 },
  WAITING: { tone: "waiting", icon: Clock3 },
  NOT_STARTED: { tone: "waiting", icon: Clock3 },
  AWAITING_DECISION: { tone: "decision", icon: User },
  BLOCKED: { tone: "blocked", icon: ShieldAlert },
  PAUSED: { tone: "paused", icon: PauseCircle },
  COMPLETED: { tone: "complete", icon: CheckCircle2 },
};

const dateTime = new Intl.DateTimeFormat("pt-BR", {
  day: "2-digit",
  month: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
});

/** Apresenta os produtos em PLAY e o trabalho corrente fornecido integralmente pelo backend. */
export default function ProductsInPlayPage() {
  const query = useProductsInPlay();
  const data = query.data;
  return (
    <div className="products-play-page">
      <header className="products-play-header">
        <div>
          <span className="products-play-eyebrow">
            <Play size={14} aria-hidden="true" /> ACOMPANHAMENTO
          </span>
          <PageTitle>Produtos em play</PageTitle>
          <p>O que está acontecendo com cada produto, em um só lugar.</p>
        </div>
        <button
          type="button"
          className="products-play-refresh"
          disabled={query.isFetching}
          onClick={() => void query.refetch()}
        >
          {query.isFetching ? (
            <Loader2
              size={17}
              className="products-play-spin"
              aria-hidden="true"
            />
          ) : (
            <RefreshCw size={17} aria-hidden="true" />
          )}
          {query.isFetching ? "Atualizando..." : "Atualizar"}
        </button>
      </header>

      {query.isError && (
        <div className="alert alert-warning" role="alert">
          {data
            ? "A atualização falhou. Os cartões mostram a última consulta disponível."
            : "Não foi possível consultar os produtos. Use Atualizar para tentar novamente."}
        </div>
      )}
      {!data && query.isPending ? (
        <div className="products-play-empty" role="status">
          <Loader2 className="products-play-spin" aria-hidden="true" />
          Consultando as atividades dos produtos...
        </div>
      ) : data ? (
        <>
          <section
            className="products-play-summary"
            aria-label="Resumo da operação"
          >
            <div>
              <strong>{data.total}</strong>
              <span>em play</span>
            </div>
            <div className="products-play-summary--running">
              <strong>{data.executing}</strong>
              <span>executando</span>
            </div>
            <div>
              <strong>{data.waiting}</strong>
              <span>aguardando</span>
            </div>
            <div className="products-play-summary--blocked">
              <strong>{data.blocked}</strong>
              <span>bloqueados</span>
            </div>
            <div>
              <strong>{data.paused}</strong>
              <span>pausados</span>
            </div>
          </section>
          <div className="products-play-updated">
            <span>
              Consulta de{" "}
              <time dateTime={data.observedAt}>
                {dateTime.format(new Date(data.observedAt))}
              </time>{" "}
              · atualização a cada 30 s
            </span>
            <span>
              PLAY permite automação; o status mostra a atividade real.
            </span>
          </div>
          {data.unavailable > 0 && (
            <p className="alert alert-warning" role="status">
              {data.unavailable} produto(s) sem atividade confirmada nesta
              consulta.
            </p>
          )}
          {data.total === 0 ? (
            <div className="products-play-empty">
              <PauseCircle size={32} aria-hidden="true" />
              <h2>Nenhum produto em play</h2>
              <p>Os produtos em STOP continuam disponíveis na gestão.</p>
              <Link to="/products/all">
                Ver todos os produtos{" "}
                <ArrowRight size={16} aria-hidden="true" />
              </Link>
            </div>
          ) : (
            <section
              className="products-play-grid"
              aria-label="Atividades dos produtos em play"
            >
              {data.products.map((product) => {
                const appearance = statusAppearance[product.status] || {
                  tone: "unknown",
                  icon: HelpCircle,
                };
                const Icon = appearance.icon;
                return (
                  <article
                    key={product.productId}
                    className={`products-play-card products-play-card--${appearance.tone}`}
                    aria-labelledby={`product-play-${product.productId}`}
                  >
                    <div className="products-play-card__top">
                      <span
                        className="products-play-card__monogram"
                        aria-hidden="true"
                      >
                        {product.productName.charAt(0)}
                      </span>
                      <span className="products-play-status">
                        <Icon size={15} aria-hidden="true" />
                        {product.statusLabel}
                      </span>
                    </div>
                    <h2 id={`product-play-${product.productId}`}>
                      {product.productName}
                    </h2>
                    {product.commercialName &&
                      product.commercialName !== product.productName && (
                        <p className="products-play-card__subtitle">
                          {product.commercialName}
                        </p>
                      )}
                    <div className="products-play-activity">
                      <span>
                        ATIVIDADE CORRENTE
                        {product.activityNumber
                          ? ` · ${product.activityNumber}`
                          : ""}
                      </span>
                      <h3>{product.activityName}</h3>
                      <p>
                        <User size={15} aria-hidden="true" />{" "}
                        {product.responsible || "Responsável não informado"}
                      </p>
                    </div>
                    {product.reason && (
                      <details className="products-play-details">
                        <summary>Entender o status</summary>
                        <p>{product.reason}</p>
                        {product.lastReconciledAt && (
                          <small>
                            Último acompanhamento:{" "}
                            {dateTime.format(
                              new Date(product.lastReconciledAt),
                            )}
                          </small>
                        )}
                      </details>
                    )}
                    <Link
                      className="products-play-card__link"
                      to={product.activityUrl}
                      aria-label={`Ver atividade de ${product.productName}`}
                    >
                      Ver atividade <ArrowRight size={18} aria-hidden="true" />
                    </Link>
                  </article>
                );
              })}
            </section>
          )}
        </>
      ) : null}
    </div>
  );
}
