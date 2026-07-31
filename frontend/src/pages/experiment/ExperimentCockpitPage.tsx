import {
  AlertTriangle,
  ArrowLeft,
  BarChart3,
  CheckCircle2,
  DollarSign,
  Lightbulb,
  MousePointerClick,
  ShoppingCart,
  Target,
  TrendingUp,
  Workflow,
} from "lucide-react";
import type { ReactNode } from "react";
import { Link, useParams } from "react-router-dom";
import { useExperimentCockpit } from "../../api/experiment/useExperimentCockpit";
import PageTitle from "../../components/PageTitle";
import experimentIcon from "../../assets/icons/experiment-icon.svg";

const currencyFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
  minimumFractionDigits: 2,
});

function formatCurrency(value?: number | null) {
  return value === null || value === undefined
    ? "—"
    : currencyFormatter.format(value);
}

function formatNumber(value?: number | null) {
  return value === null || value === undefined
    ? "—"
    : new Intl.NumberFormat("pt-BR").format(value);
}

function formatPercent(value?: number | null) {
  return value === null || value === undefined ? "—" : `${value.toFixed(2)}%`;
}

function formatDecimal(value?: number | null) {
  return value === null || value === undefined ? "—" : value.toFixed(2);
}

function severityClass(severity?: string | null) {
  if (severity === "success") return "success";
  if (severity === "danger") return "danger";
  if (severity === "warning") return "warning";
  return "secondary";
}

function valueOrDash(value?: string | null) {
  return value?.trim() ? value : "—";
}

export default function ExperimentCockpitPage() {
  const { id } = useParams();
  const { data, isLoading, isError } = useExperimentCockpit(id);

  if (isLoading) {
    return (
      <div className="d-flex justify-content-center py-5">
        <div className="spinner-border" role="status">
          <span className="visually-hidden">Carregando cockpit...</span>
        </div>
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div className="alert alert-danger" role="alert">
        Não foi possível carregar o cockpit do experimento agora.
      </div>
    );
  }

  const { scoreboard, bottleneck } = data;
  const variant = severityClass(bottleneck.severity);

  return (
    <div className="d-flex flex-column gap-4">
      <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
        <div>
          <PageTitle icon={experimentIcon}>Cockpit do Experimento</PageTitle>
          <div className="text-muted">
            #{data.experimentId} · {data.experimentName}
          </div>
        </div>
        <div className="d-flex flex-wrap gap-2">
          <Link className="btn btn-outline-secondary btn-sm" to="/experiments">
            <ArrowLeft size={16} className="me-1" />
            Experimentos
          </Link>
          <Link
            className="btn btn-outline-primary btn-sm"
            to={`/experiments/${data.experimentId}`}
          >
            Ver detalhes
          </Link>
        </div>
      </div>

      <section className="row g-3">
        <MetricCard
          icon={<DollarSign size={20} />}
          label="Receita"
          value={formatCurrency(scoreboard.revenue)}
          tone="success"
        />
        <MetricCard
          icon={<BarChart3 size={20} />}
          label="Gasto"
          value={formatCurrency(scoreboard.spend)}
          tone="primary"
        />
        <MetricCard
          icon={<TrendingUp size={20} />}
          label="Margem"
          value={formatCurrency(scoreboard.margin)}
          tone={
            scoreboard.margin && scoreboard.margin > 0 ? "success" : "secondary"
          }
        />
        <MetricCard
          icon={<ShoppingCart size={20} />}
          label="Compras"
          value={formatNumber(scoreboard.purchases)}
          tone={scoreboard.purchases > 0 ? "success" : "secondary"}
        />
      </section>

      <section className={`border-start border-4 border-${variant} ps-3 py-2`}>
        <div className={`badge text-bg-${variant} mb-2`}>Gargalo principal</div>
        <h2 className="h4 mb-2">{bottleneck.title}</h2>
        <p className="mb-1">{bottleneck.diagnosis}</p>
        <p className="mb-1 text-muted">{bottleneck.commercialImpact}</p>
        <p className="mb-0 fw-semibold">{bottleneck.recommendedFocus}</p>
      </section>

      <section className="row g-3">
        <div className="col-lg-7">
          <div className="border rounded-2 p-3 h-100">
            <h2 className="h5 d-flex align-items-center gap-2">
              <Target size={18} />
              Pergunta do experimento
            </h2>
            <dl className="row mb-0 small">
              <dt className="col-sm-4">Dor</dt>
              <dd className="col-sm-8">{valueOrDash(data.question.pain)}</dd>
              <dt className="col-sm-4">Promessa</dt>
              <dd className="col-sm-8">{valueOrDash(data.question.promise)}</dd>
              <dt className="col-sm-4">Mecanismo PDE</dt>
              <dd className="col-sm-8">
                {valueOrDash(data.question.mechanism)}
              </dd>
              <dt className="col-sm-4">Oferta</dt>
              <dd className="col-sm-8">{valueOrDash(data.question.offer)}</dd>
              <dt className="col-sm-4">CTA</dt>
              <dd className="col-sm-8">
                {valueOrDash(data.question.primaryCta)}
              </dd>
              <dt className="col-sm-4">Variável primária</dt>
              <dd className="col-sm-8">
                {valueOrDash(data.question.primaryVariable)}
              </dd>
              <dt className="col-sm-4">Métrica primária</dt>
              <dd className="col-sm-8">
                {valueOrDash(data.question.primaryMetric)}
              </dd>
            </dl>
          </div>
        </div>
        <div className="col-lg-5">
          <div className="border rounded-2 p-3 h-100">
            <h2 className="h5 d-flex align-items-center gap-2">
              {data.health.status === "READY" ? (
                <CheckCircle2 size={18} />
              ) : (
                <AlertTriangle size={18} />
              )}
              Chegou validamente ao mercado?
            </h2>
            <p className="mb-1 fw-semibold">{data.health.headline}</p>
            <p className="text-muted small">{data.health.description}</p>
            {data.health.blockers.length > 0 ? (
              <ul className="mb-0 small">
                {data.health.blockers.map((blocker, index) => (
                  <li key={`${blocker}-${index}`}>{blocker}</li>
                ))}
              </ul>
            ) : (
              <span className="badge text-bg-success">
                Sem bloqueio conhecido
              </span>
            )}
          </div>
        </div>
      </section>

      <section className="row g-3">
        <MetricCard
          icon={<MousePointerClick size={20} />}
          label="Impressões"
          value={formatNumber(scoreboard.impressions)}
          tone="secondary"
        />
        <MetricCard
          icon={<MousePointerClick size={20} />}
          label="Cliques"
          value={formatNumber(scoreboard.clicks)}
          tone="secondary"
        />
        <MetricCard
          icon={<TrendingUp size={20} />}
          label="CTR"
          value={formatPercent(scoreboard.ctr)}
          tone="secondary"
        />
        <MetricCard
          icon={<DollarSign size={20} />}
          label="ROAS"
          value={formatDecimal(scoreboard.roas)}
          tone="secondary"
        />
      </section>

      <section className="border rounded-2 p-3">
        <h2 className="h5 d-flex align-items-center gap-2">
          <Workflow size={18} />
          Funil comercial
        </h2>
        <div className="table-responsive">
          <table className="table table-sm align-middle mb-0">
            <thead>
              <tr>
                <th>Etapa</th>
                <th className="text-end">Total</th>
                <th className="text-end">Únicos</th>
                <th>Fonte</th>
              </tr>
            </thead>
            <tbody>
              {data.funnel.map((stage) => (
                <tr key={stage.stage}>
                  <td>{stage.label}</td>
                  <td className="text-end">{formatNumber(stage.totalCount)}</td>
                  <td className="text-end">
                    {formatNumber(stage.uniqueCount)}
                  </td>
                  <td>{valueOrDash(stage.source)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="row g-3">
        <div className="col-lg-6">
          <div className="border rounded-2 p-3 h-100">
            <h2 className="h5 d-flex align-items-center gap-2">
              <Lightbulb size={18} />O que aprendemos
            </h2>
            <ul className="mb-0 small">
              {data.learnings.map((learning, index) => (
                <li key={`${learning}-${index}`}>{learning}</li>
              ))}
            </ul>
          </div>
        </div>
        <div className="col-lg-6">
          <div className="border rounded-2 p-3 h-100">
            <h2 className="h5">Vender agora</h2>
            <div className="d-flex flex-column gap-2">
              {data.nextActions.map((action) => (
                <Link
                  key={action.code}
                  className="btn btn-outline-primary text-start"
                  to={action.targetRoute}
                >
                  <span className="fw-semibold d-block">{action.label}</span>
                  <span className="small text-muted">{action.rationale}</span>
                </Link>
              ))}
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

function MetricCard({
  icon,
  label,
  value,
  tone,
}: {
  icon: ReactNode;
  label: string;
  value: string;
  tone: "primary" | "success" | "secondary";
}) {
  return (
    <div className="col-6 col-xl-3">
      <div className="border rounded-2 p-3 h-100">
        <div className={`text-${tone} mb-2`}>{icon}</div>
        <div className="small text-muted">{label}</div>
        <div className="h4 mb-0">{value}</div>
      </div>
    </div>
  );
}
