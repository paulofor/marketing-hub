import { Link, useParams } from "react-router-dom";
import { ArrowLeft, CircleDollarSign } from "lucide-react";
import { useProductFinancialSummary } from "../../api/product/useProductFinancialSummary";
import PageTitle from "../../components/PageTitle";

const brlFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

const usdFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
});

function formatBrl(value?: number) {
  return brlFormatter.format(value ?? 0);
}

function formatUsd(value?: number) {
  return usdFormatter.format(value ?? 0);
}

function formatPeriodStart(value?: string) {
  if (!value) return "período atual";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "período atual";
  return date.toLocaleDateString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    timeZone: "UTC",
  });
}

export default function ProductFinancialPage() {
  const { productId } = useParams();
  const financialQuery = useProductFinancialSummary(productId);
  const summary = financialQuery.data;

  if (financialQuery.isLoading) {
    return <p className="text-muted">Carregando financeiro do produto...</p>;
  }

  if (financialQuery.isError || !summary) {
    return (
      <div>
        <Link className="btn btn-outline-secondary mb-3" to="/products">
          <ArrowLeft size={16} aria-hidden="true" />
          Voltar para produtos
        </Link>
        <div className="alert alert-danger">
          Não foi possível carregar o financeiro do produto.
        </div>
      </div>
    );
  }

  const allLines = [...summary.costs, summary.revenue, summary.profit];

  return (
    <div>
      <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-4">
        <div>
          <PageTitle>Financeiro do produto</PageTitle>
          <p className="text-muted mb-0">
            {summary.productName ||
              summary.productSlug ||
              `Produto ${summary.productId}`}{" "}
            · valores em BRL e USD com câmbio fixo de R${" "}
            {summary.exchangeRateBrlPerUsd} por US$ 1.
          </p>
        </div>
        <Link className="btn btn-outline-secondary" to="/products">
          <ArrowLeft size={16} aria-hidden="true" />
          Voltar para produtos
        </Link>
      </div>

      <div className="product-financial-summary mb-3">
        <section className="product-financial-summary__tile">
          <span>Receita mensal</span>
          <strong>{formatUsd(summary.revenue.monthly.usd)}</strong>
          <small>{formatBrl(summary.revenue.monthly.brl)}</small>
        </section>
        <section className="product-financial-summary__tile">
          <span>Custo mensal</span>
          <strong>
            {formatUsd(
              summary.costs.reduce(
                (total, line) => total + line.monthly.usd,
                0,
              ),
            )}
          </strong>
          <small>
            {formatBrl(
              summary.costs.reduce(
                (total, line) => total + line.monthly.brl,
                0,
              ),
            )}
          </small>
        </section>
        <section className="product-financial-summary__tile product-financial-summary__tile--profit">
          <span>Lucro anual</span>
          <strong>{formatUsd(summary.profit.annual.usd)}</strong>
          <small>{formatBrl(summary.profit.annual.brl)}</small>
        </section>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
            <h2 className="h6 mb-0">Custos, receitas e lucro</h2>
            <span className="text-muted small">
              Mês desde {formatPeriodStart(summary.monthStart)} · ano desde{" "}
              {formatPeriodStart(summary.yearStart)}
            </span>
          </div>
          <div className="table-responsive">
            <table className="table align-middle product-financial-table">
              <thead>
                <tr>
                  <th>Tipo</th>
                  <th>Mensal</th>
                  <th>Anual</th>
                  <th>Origem</th>
                </tr>
              </thead>
              <tbody>
                {allLines.map((line) => (
                  <tr key={line.type}>
                    <td>
                      <span className="product-financial-table__type">
                        <CircleDollarSign size={16} aria-hidden="true" />
                        {line.label}
                      </span>
                    </td>
                    <td>
                      <strong>{formatUsd(line.monthly.usd)}</strong>
                      <small>{formatBrl(line.monthly.brl)}</small>
                    </td>
                    <td>
                      <strong>{formatUsd(line.annual.usd)}</strong>
                      <small>{formatBrl(line.annual.brl)}</small>
                    </td>
                    <td className="text-muted small">{line.source}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
