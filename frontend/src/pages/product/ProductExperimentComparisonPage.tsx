import { Link, useParams } from "react-router-dom";
import { ArrowLeft, BarChart3, BookOpen, FlaskConical } from "lucide-react";
import { useProductExperimentComparison } from "../../api/product/useProductExperimentComparison";
import type { ProductExperimentComparisonExperiment } from "../../api/product/useProductExperimentComparison";
import PageTitle from "../../components/PageTitle";

const numberFormatter = new Intl.NumberFormat("pt-BR");
const moneyFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

function formatNumber(value?: number | null) {
  return numberFormatter.format(value ?? 0);
}

function formatMoney(value?: number | null) {
  return moneyFormatter.format(value ?? 0);
}

function formatDate(value?: string | null) {
  if (!value) return "Sem data";
  const date = new Date(`${value}T00:00:00Z`);
  if (Number.isNaN(date.getTime())) return "Sem data";
  return date.toLocaleDateString("pt-BR", { timeZone: "UTC" });
}

function ratio(numerator?: number | null, denominator?: number | null) {
  if (!denominator) return "0%";
  return `${(((numerator ?? 0) / denominator) * 100).toLocaleString("pt-BR", {
    maximumFractionDigits: 1,
  })}%`;
}

function findFunnelTotal(
  experiment: ProductExperimentComparisonExperiment,
  stageCode: string,
) {
  return (
    experiment.funnelStages.find((stage) => stage.stageCode === stageCode)
      ?.total ?? 0
  );
}

function statusLabel(value?: string | null) {
  return value ? value.replace(/_/g, " ") : "Sem status";
}

function isRunningExperiment(experiment: ProductExperimentComparisonExperiment) {
  return experiment.status?.toUpperCase() === "RUNNING";
}

export default function ProductExperimentComparisonPage() {
  const { productId } = useParams();
  const comparisonQuery = useProductExperimentComparison(productId);
  const comparison = comparisonQuery.data;

  if (comparisonQuery.isLoading) {
    return <p className="text-muted">Carregando painel comparativo...</p>;
  }

  if (comparisonQuery.isError || !comparison) {
    return (
      <div>
        <Link className="btn btn-outline-secondary mb-3" to="/products">
          <ArrowLeft size={16} aria-hidden="true" />
          Voltar para produtos
        </Link>
        <div className="alert alert-danger">
          Não foi possível carregar o painel comparativo do produto.
        </div>
      </div>
    );
  }

  const experiments = comparison.experiments ?? [];
  const sortedExperiments = [...experiments].sort((current, next) => {
    const currentRunning = isRunningExperiment(current);
    const nextRunning = isRunningExperiment(next);
    if (currentRunning === nextRunning) return 0;
    return currentRunning ? -1 : 1;
  });
  const totals = experiments.reduce(
    (acc, experiment) => ({
      impressions: acc.impressions + (experiment.impressions ?? 0),
      clicks: acc.clicks + (experiment.clicks ?? 0),
      spend: acc.spend + (experiment.spend ?? 0),
      purchases: acc.purchases + findFunnelTotal(experiment, "COMPRA"),
    }),
    { impressions: 0, clicks: 0, spend: 0, purchases: 0 },
  );

  return (
    <div>
      <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-4">
        <div>
          <PageTitle>Comparativo de experimentos</PageTitle>
          <p className="text-muted mb-0">
            {comparison.productName ||
              comparison.productSlug ||
              `Produto ${comparison.productId}`}{" "}
            · {statusLabel(comparison.commercialStatus)}
          </p>
        </div>
        <Link className="btn btn-outline-secondary" to="/products">
          <ArrowLeft size={16} aria-hidden="true" />
          Voltar para produtos
        </Link>
      </div>

      <section className="product-comparison-decision mb-3">
        <div>
          <span>Recomendação principal</span>
          <strong>{comparison.mainRecommendation}</strong>
        </div>
      </section>

      <div className="product-comparison-summary mb-3">
        <section>
          <FlaskConical size={18} aria-hidden="true" />
          <span>Experimentos</span>
          <strong>{formatNumber(experiments.length)}</strong>
        </section>
        <section>
          <BarChart3 size={18} aria-hidden="true" />
          <span>Impressões</span>
          <strong>{formatNumber(totals.impressions)}</strong>
        </section>
        <section>
          <BarChart3 size={18} aria-hidden="true" />
          <span>Cliques</span>
          <strong>{formatNumber(totals.clicks)}</strong>
        </section>
        <section>
          <BookOpen size={18} aria-hidden="true" />
          <span>Compras</span>
          <strong>{formatNumber(totals.purchases)}</strong>
        </section>
      </div>

      {experiments.length === 0 ? (
        <div className="alert alert-warning">
          Nenhum experimento vinculado ao produto para comparação.
        </div>
      ) : (
        <>
          <div className="card mb-3">
            <div className="card-body">
              <div className="table-responsive">
                <table className="table align-middle product-comparison-table">
                  <thead>
                    <tr>
                      <th>Experimento</th>
                      <th>Status</th>
                      <th>Mídia</th>
                      <th>Funil</th>
                      <th>Criativos</th>
                      <th>Decisão</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sortedExperiments.map((experiment) => {
                      const running = isRunningExperiment(experiment);
                      const formAccess = findFunnelTotal(
                        experiment,
                        "ACESSO_FORM_LEAD",
                      );
                      const purchases = findFunnelTotal(experiment, "COMPRA");
                      return (
                        <tr
                          className={
                            running
                              ? "product-comparison-table__row--running"
                              : undefined
                          }
                          key={experiment.experimentId}
                        >
                          <td>
                            <Link
                              className="fw-semibold"
                              to={`/experiments/${experiment.experimentId}`}
                            >
                              {experiment.name}
                            </Link>
                            <small className="d-block text-muted">
                              #{experiment.experimentId} ·{" "}
                              {formatDate(experiment.startDate)} a{" "}
                              {formatDate(experiment.endDate)}
                            </small>
                          </td>
                          <td>
                            <span
                              className={
                                running
                                  ? "badge product-comparison-status product-comparison-status--running"
                                  : "badge product-comparison-status text-bg-light border"
                              }
                            >
                              {statusLabel(experiment.status)}
                            </span>
                            <small className="d-block text-muted mt-1">
                              Campanha: {statusLabel(experiment.campaignStatus)}
                            </small>
                          </td>
                          <td>
                            <strong>{formatMoney(experiment.spend)}</strong>
                            <small className="d-block text-muted">
                              {formatNumber(experiment.impressions)} imp. ·{" "}
                              {formatNumber(experiment.clicks)} cliques
                            </small>
                            <small className="d-block text-muted">
                              CTR {ratio(experiment.clicks, experiment.impressions)}
                            </small>
                          </td>
                          <td>
                            <strong>{formatNumber(formAccess)}</strong>
                            <small className="d-block text-muted">
                              entradas · {formatNumber(purchases)} compras
                            </small>
                            <small className="d-block text-muted">
                              Leads Meta: {formatNumber(experiment.leads)}
                            </small>
                          </td>
                          <td>
                            <strong>
                              {formatNumber(experiment.approvedCreatives)} /{" "}
                              {formatNumber(experiment.totalCreatives)}
                            </strong>
                            <small className="d-block text-muted">
                              aprovados / total
                            </small>
                          </td>
                          <td className="product-comparison-table__decision">
                            {experiment.recommendedAction}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <div className="row g-3">
            {sortedExperiments.map((experiment) => (
              <div className="col-12 col-xl-6" key={experiment.experimentId}>
                <section className="product-comparison-learning">
                  <h2>{experiment.name}</h2>
                  <dl>
                    <div>
                      <dt>Hipótese</dt>
                      <dd>{experiment.hypothesis || "Não registrada"}</dd>
                    </div>
                    <div>
                      <dt>Promessa</dt>
                      <dd>{experiment.promise || "Não registrada"}</dd>
                    </div>
                    <div>
                      <dt>Lições aprendidas</dt>
                      <dd>
                        {experiment.learnedLessons ||
                          "Ainda sem lições aprendidas registradas."}
                      </dd>
                    </div>
                  </dl>
                </section>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
