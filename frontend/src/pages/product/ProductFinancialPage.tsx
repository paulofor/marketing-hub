import { Link, useParams } from "react-router-dom";
import { ArrowLeft, CircleDollarSign } from "lucide-react";
import ReactECharts from "echarts-for-react";
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
  const monthlyResultsForChart = [...summary.monthlyResults].reverse();
  const monthlyChartOption = {
    color: ["#2563eb", "#16a34a", "#dc2626"],
    grid: { left: 48, right: 24, top: 32, bottom: 40 },
    legend: { bottom: 0 },
    tooltip: {
      trigger: "axis",
      valueFormatter: (value: number) => formatBrl(value),
    },
    xAxis: {
      type: "category",
      data: monthlyResultsForChart.map((month) => month.monthLabel),
      axisLabel: { color: "#475569" },
    },
    yAxis: {
      type: "value",
      axisLabel: {
        color: "#475569",
        formatter: (value: number) => `R$ ${value}`,
      },
      splitLine: { lineStyle: { color: "#e2e8f0" } },
    },
    series: [
      {
        name: "Custo",
        type: "bar",
        data: monthlyResultsForChart.map((month) => month.cost.brl),
        barMaxWidth: 32,
      },
      {
        name: "Receita",
        type: "bar",
        data: monthlyResultsForChart.map((month) => month.revenue.brl),
        barMaxWidth: 32,
      },
      {
        name: "Lucro",
        type: "line",
        data: monthlyResultsForChart.map((month) => month.profit.brl),
        smooth: true,
        symbolSize: 8,
      },
    ],
  };
  const costCompositionData = summary.costs
    .filter((line) => line.monthly.brl > 0)
    .map((line) => ({
      name: line.label,
      value: line.monthly.brl,
    }));
  const costCompositionOption = {
    color: ["#0f766e", "#7c3aed", "#ea580c", "#0891b2", "#be123c"],
    legend: { bottom: 0 },
    tooltip: {
      trigger: "item",
      valueFormatter: (value: number) => formatBrl(value),
    },
    series: [
      {
        name: "Custo mensal",
        type: "pie",
        radius: ["42%", "68%"],
        center: ["50%", "44%"],
        avoidLabelOverlap: true,
        label: {
          formatter: "{b}",
          color: "#172033",
        },
        data:
          costCompositionData.length > 0
            ? costCompositionData
            : [{ name: "Sem custo informado", value: 0 }],
      },
    ],
  };

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
          <strong>{formatBrl(summary.revenue.monthly.brl)}</strong>
          <small>{formatUsd(summary.revenue.monthly.usd)}</small>
        </section>
        <section className="product-financial-summary__tile">
          <span>Custo mensal</span>
          <strong>
            {formatBrl(
              summary.costs.reduce(
                (total, line) => total + line.monthly.brl,
                0,
              ),
            )}
          </strong>
          <small>
            {formatUsd(
              summary.costs.reduce(
                (total, line) => total + line.monthly.usd,
                0,
              ),
            )}
          </small>
        </section>
        <section className="product-financial-summary__tile product-financial-summary__tile--profit">
          <span>Lucro anual</span>
          <strong>{formatBrl(summary.profit.annual.brl)}</strong>
          <small>{formatUsd(summary.profit.annual.usd)}</small>
        </section>
      </div>

      <div className="product-financial-charts mb-3">
        <section className="card product-financial-chart">
          <div className="card-body">
            <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
              <h2 className="h6 mb-0">Evolução financeira</h2>
              <span className="text-muted small">Valores em BRL</span>
            </div>
            <ReactECharts
              option={monthlyChartOption}
              style={{ height: 320 }}
              aria-label="Gráfico de evolução mensal de custo, receita e lucro"
            />
          </div>
        </section>
        <section className="card product-financial-chart">
          <div className="card-body">
            <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
              <h2 className="h6 mb-0">Composição do custo mensal</h2>
              <span className="text-muted small">Por origem de custo</span>
            </div>
            <ReactECharts
              option={costCompositionOption}
              style={{ height: 320 }}
              aria-label="Gráfico de composição dos custos mensais"
            />
          </div>
        </section>
      </div>

      <div className="card product-financial-months mb-3">
        <div className="card-body">
          <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
            <h2 className="h6 mb-0">Resultado dos últimos 4 meses</h2>
            <span className="text-muted small">
              Mês corrente e 3 anteriores
            </span>
          </div>
          <div className="table-responsive">
            <table className="table align-middle product-financial-table mb-0">
              <thead>
                <tr>
                  <th>Mês</th>
                  <th>Custo</th>
                  <th>Receita</th>
                  <th>Lucro</th>
                </tr>
              </thead>
              <tbody>
                {summary.monthlyResults.map((month) => (
                  <tr key={month.monthStart}>
                    <td>
                      <strong className="product-financial-months__label">
                        {month.monthLabel}
                      </strong>
                    </td>
                    <td>
                      <strong className="product-financial-months__brl">
                        {formatBrl(month.cost.brl)}
                      </strong>
                      <small>{formatUsd(month.cost.usd)}</small>
                    </td>
                    <td>
                      <strong className="product-financial-months__brl">
                        {formatBrl(month.revenue.brl)}
                      </strong>
                      <small>{formatUsd(month.revenue.usd)}</small>
                    </td>
                    <td>
                      <strong
                        className={
                          month.profit.brl >= 0
                            ? "product-financial-months__brl product-financial-months__profit--positive"
                            : "product-financial-months__brl product-financial-months__profit--negative"
                        }
                      >
                        {formatBrl(month.profit.brl)}
                      </strong>
                      <small>{formatUsd(month.profit.usd)}</small>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
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
                      <strong>{formatBrl(line.monthly.brl)}</strong>
                      <small>{formatUsd(line.monthly.usd)}</small>
                    </td>
                    <td>
                      <strong>{formatBrl(line.annual.brl)}</strong>
                      <small>{formatUsd(line.annual.usd)}</small>
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
